package com.hmp.sales.feature.auth.service;

import com.hmp.sales.common.exception.UnauthorizedOperationException;
import com.hmp.sales.config.AppProperties;
import com.hmp.sales.feature.auth.entity.RefreshToken;
import com.hmp.sales.feature.auth.repository.RefreshTokenRepository;
import com.hmp.sales.feature.auth.util.HashUtils;
import com.hmp.sales.feature.user.entity.Role;
import com.hmp.sales.feature.user.entity.RoleCode;
import com.hmp.sales.feature.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test cho {@link RefreshTokenServiceImpl}.
 * Xác minh logic tạo, xoay vòng và phát hiện replay attack của Refresh Token.
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenServiceImpl refreshTokenService;
    private AppProperties appProperties;
    private User testUser;

    @BeforeEach
    void setUp() {
        AppProperties.JwtProperties jwtProps = new AppProperties.JwtProperties(
                "sales-backend",
                "secret-key-for-testing-only-not-real",
                60L,
                30L
        );
        appProperties = new AppProperties(jwtProps, null, null, null);
        refreshTokenService = new RefreshTokenServiceImpl(refreshTokenRepository, appProperties);

        Role role = new Role();
        role.setRoleCode(RoleCode.CUSTOMER);
        testUser = User.builder()
                .fullName("Test User")
                .email("test@example.com")
                .role(role)
                .build();
        testUser.setId(1L);
    }

    @Test
    @DisplayName("createRefreshToken - Tạo token mới, lưu hash SHA-256 vào DB")
    void createRefreshToken_success_savesHashToDb() {
        UUID accessJti = UUID.randomUUID();
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RefreshTokenService.RefreshTokenPair result = refreshTokenService.createRefreshToken(testUser, accessJti);

        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();

        assertThat(result.rawToken()).isNotBlank();
        // hash trong DB phải khớp với SHA-256 của raw token
        assertThat(saved.getTokenHash()).isEqualTo(HashUtils.sha256Hex(result.rawToken()));
        assertThat(saved.getTokenFamilyId()).isNotNull();
        assertThat(saved.getIsRevoked()).isFalse();
        assertThat(saved.getAccessTokenJti()).isEqualTo(accessJti);
    }

    @Test
    @DisplayName("rotateRefreshToken - Xoay vòng thành công: revoke cũ, tạo mới cùng family")
    void rotateRefreshToken_validToken_revokesOldAndCreatesNew() {
        String rawToken = UUID.randomUUID().toString();
        String hash = HashUtils.sha256Hex(rawToken);
        UUID familyId = UUID.randomUUID();
        UUID newAccessJti = UUID.randomUUID();

        RefreshToken existing = RefreshToken.builder()
                .id(1L)
                .user(testUser)
                .tokenFamilyId(familyId)
                .tokenHash(hash)
                .accessTokenJti(UUID.randomUUID())
                .expiresAt(Instant.now().plusSeconds(86400))
                .isRevoked(false)
                .isDeleted(false)
                .build();

        when(refreshTokenRepository.findByTokenHashAndIsDeletedFalse(hash)).thenReturn(Optional.of(existing));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RefreshTokenService.RefreshTokenPair result = refreshTokenService.rotateRefreshToken(rawToken, newAccessJti);

        // Token cũ phải bị revoke
        assertThat(existing.getIsRevoked()).isTrue();
        // Token mới phải cùng family
        assertThat(result.savedToken().getTokenFamilyId()).isEqualTo(familyId);
        assertThat(result.rawToken()).isNotEqualTo(rawToken);
        assertThat(result.savedToken().getAccessTokenJti()).isEqualTo(newAccessJti);
    }

    @Test
    @DisplayName("rotateRefreshToken - Token đã hết hạn: ném UnauthorizedOperationException")
    void rotateRefreshToken_expiredToken_throwsException() {
        String rawToken = UUID.randomUUID().toString();
        String hash = HashUtils.sha256Hex(rawToken);

        RefreshToken expired = RefreshToken.builder()
                .tokenHash(hash)
                .tokenFamilyId(UUID.randomUUID())
                .expiresAt(Instant.now().minusSeconds(1))
                .isRevoked(false)
                .isDeleted(false)
                .build();

        when(refreshTokenRepository.findByTokenHashAndIsDeletedFalse(hash)).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(rawToken, UUID.randomUUID()))
                .isInstanceOf(UnauthorizedOperationException.class)
                .hasMessageContaining("hết hạn");
    }

    @Test
    @DisplayName("rotateRefreshToken - Phát hiện Replay Attack: token đã revoked → thu hồi toàn bộ family")
    void rotateRefreshToken_replayAttack_revokesEntireFamily() {
        String rawToken = UUID.randomUUID().toString();
        String hash = HashUtils.sha256Hex(rawToken);
        UUID familyId = UUID.randomUUID();

        RefreshToken revokedToken = RefreshToken.builder()
                .tokenHash(hash)
                .tokenFamilyId(familyId)
                .expiresAt(Instant.now().plusSeconds(86400))
                .isRevoked(true)  // Đã bị revoke trước đó
                .isDeleted(false)
                .build();

        when(refreshTokenRepository.findByTokenHashAndIsDeletedFalse(hash)).thenReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(rawToken, UUID.randomUUID()))
                .isInstanceOf(UnauthorizedOperationException.class)
                .hasMessageContaining("tái sử dụng");

        verify(refreshTokenRepository).revokeAllByTokenFamilyId(familyId);
    }

    @Test
    @DisplayName("rotateRefreshToken - Token không tồn tại: ném UnauthorizedOperationException")
    void rotateRefreshToken_tokenNotFound_throwsException() {
        String rawToken = UUID.randomUUID().toString();
        when(refreshTokenRepository.findByTokenHashAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(rawToken, UUID.randomUUID()))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    @DisplayName("revokeRefreshToken - Token tồn tại: đánh dấu isRevoked = true")
    void revokeRefreshToken_existingToken_setsRevokedTrue() {
        String rawToken = UUID.randomUUID().toString();
        String hash = HashUtils.sha256Hex(rawToken);

        RefreshToken token = RefreshToken.builder()
                .tokenHash(hash)
                .isRevoked(false)
                .isDeleted(false)
                .build();

        when(refreshTokenRepository.findByTokenHashAndIsDeletedFalse(hash)).thenReturn(Optional.of(token));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        refreshTokenService.revokeRefreshToken(rawToken);

        assertThat(token.getIsRevoked()).isTrue();
        verify(refreshTokenRepository).save(token);
    }
}

package com.hmp.sales.feature.auth.service;

import com.hmp.sales.common.exception.UnauthorizedOperationException;
import com.hmp.sales.config.AppProperties;
import com.hmp.sales.feature.auth.dto.AuthResponse;
import com.hmp.sales.feature.auth.entity.RefreshToken;
import com.hmp.sales.feature.auth.service.GoogleAuthService.GoogleUserPayload;
import com.hmp.sales.feature.auth.service.RefreshTokenService.RefreshTokenPair;
import com.hmp.sales.feature.user.entity.AuthProvider;
import com.hmp.sales.feature.user.entity.Role;
import com.hmp.sales.feature.user.entity.RoleCode;
import com.hmp.sales.feature.user.entity.User;
import com.hmp.sales.feature.user.entity.UserStatus;
import com.hmp.sales.feature.user.repository.RoleRepository;
import com.hmp.sales.feature.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit test cho {@link AuthServiceImpl}.
 * Xác minh luồng đăng nhập Google và kiểm soát tài khoản.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private GoogleAuthService googleAuthService;
    @Mock private JwtTokenService jwtTokenService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private HttpServletResponse httpResponse;

    private AuthServiceImpl authService;
    private AppProperties appProperties;

    private Role customerRole;

    @BeforeEach
    void setUp() {
        AppProperties.JwtProperties jwtProps = new AppProperties.JwtProperties(
                "sales-backend", "secret", 60L, 30L);
        appProperties = new AppProperties(jwtProps, null, null, null, null, null);
        authService = new AuthServiceImpl(
                googleAuthService, jwtTokenService, refreshTokenService,
                userRepository, roleRepository, appProperties);

        customerRole = new Role();
        customerRole.setRoleCode(RoleCode.CUSTOMER);
    }

    @Test
    @DisplayName("loginWithGoogle - Tài khoản chưa tồn tại: tạo mới CUSTOMER, cấp token")
    void loginWithGoogle_newUser_createsCustomerAndIssuesTokens() {
        GoogleUserPayload payload = new GoogleUserPayload("google-sub-123", "new@example.com", "Nguyễn Mới");
        when(googleAuthService.verifyGoogleToken(anyString())).thenReturn(payload);
        when(userRepository.findByAuthProviderAndProviderSubjectAndIsDeletedFalse(AuthProvider.GOOGLE, "google-sub-123"))
                .thenReturn(Optional.empty());
        when(roleRepository.findByRoleCodeAndIsDeletedFalse(RoleCode.CUSTOMER))
                .thenReturn(Optional.of(customerRole));

        User savedUser = User.builder()
                .fullName("Nguyễn Mới").email("new@example.com")
                .authProvider(AuthProvider.GOOGLE).providerSubject("google-sub-123")
                .role(customerRole).status(UserStatus.ACTIVE).build();
        savedUser.setId(99L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        when(jwtTokenService.generateAccessToken(any())).thenReturn("access-token-jwt");
        when(jwtTokenService.extractJti(anyString())).thenReturn(UUID.randomUUID());

        RefreshToken savedRt = RefreshToken.builder()
                .user(savedUser).tokenFamilyId(UUID.randomUUID())
                .tokenHash("hash").accessTokenJti(UUID.randomUUID())
                .expiresAt(Instant.now().plusSeconds(86400)).build();
        when(refreshTokenService.createRefreshToken(any(), any()))
                .thenReturn(new RefreshTokenPair("raw-refresh-token", savedRt));

        AuthResponse result = authService.loginWithGoogle("google-credential", httpResponse);

        assertThat(result.accessToken()).isEqualTo("access-token-jwt");
        assertThat(result.tokenType()).isEqualTo("Bearer");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("loginWithGoogle - Tài khoản đã tồn tại: đăng nhập bình thường, không tạo mới")
    void loginWithGoogle_existingUser_doesNotCreateDuplicate() {
        GoogleUserPayload payload = new GoogleUserPayload("sub-456", "existing@example.com", "Người Dùng Cũ");
        User existingUser = User.builder()
                .fullName("Người Dùng Cũ").email("existing@example.com")
                .authProvider(AuthProvider.GOOGLE).providerSubject("sub-456")
                .role(customerRole).status(UserStatus.ACTIVE).build();
        existingUser.setId(5L);

        when(googleAuthService.verifyGoogleToken(anyString())).thenReturn(payload);
        when(userRepository.findByAuthProviderAndProviderSubjectAndIsDeletedFalse(AuthProvider.GOOGLE, "sub-456"))
                .thenReturn(Optional.of(existingUser));
        when(jwtTokenService.generateAccessToken(any())).thenReturn("existing-access-token");
        when(jwtTokenService.extractJti(anyString())).thenReturn(UUID.randomUUID());

        RefreshToken savedRt = RefreshToken.builder()
                .user(existingUser).tokenFamilyId(UUID.randomUUID())
                .tokenHash("hash2").accessTokenJti(UUID.randomUUID())
                .expiresAt(Instant.now().plusSeconds(86400)).build();
        when(refreshTokenService.createRefreshToken(any(), any()))
                .thenReturn(new RefreshTokenPair("raw-rt-2", savedRt));

        AuthResponse result = authService.loginWithGoogle("google-credential", httpResponse);

        assertThat(result.accessToken()).isEqualTo("existing-access-token");
        // Không tạo user mới
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("loginWithGoogle - Tài khoản bị khóa (LOCKED): ném UnauthorizedOperationException")
    void loginWithGoogle_lockedAccount_throwsUnauthorized() {
        GoogleUserPayload payload = new GoogleUserPayload("sub-locked", "locked@example.com", "Bị Khóa");
        User lockedUser = User.builder()
                .fullName("Bị Khóa").email("locked@example.com")
                .role(customerRole).status(UserStatus.LOCKED).build();
        lockedUser.setId(10L);

        when(googleAuthService.verifyGoogleToken(anyString())).thenReturn(payload);
        when(userRepository.findByAuthProviderAndProviderSubjectAndIsDeletedFalse(AuthProvider.GOOGLE, "sub-locked"))
                .thenReturn(Optional.of(lockedUser));

        assertThatThrownBy(() -> authService.loginWithGoogle("credential", httpResponse))
                .isInstanceOf(UnauthorizedOperationException.class)
                .hasMessageContaining("bị khóa");
    }

    @Test
    @DisplayName("loginWithGoogle - Google token không hợp lệ: ném UnauthorizedOperationException")
    void loginWithGoogle_invalidGoogleToken_throwsUnauthorized() {
        when(googleAuthService.verifyGoogleToken(anyString()))
                .thenThrow(new UnauthorizedOperationException("Google token không hợp lệ"));

        assertThatThrownBy(() -> authService.loginWithGoogle("bad-credential", httpResponse))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    @DisplayName("loginWithGoogle - Tài khoản Manager đã được provision (sub = null): liên kết Google sub và cấp token")
    void loginWithGoogle_pendingManager_linksGoogleSubAndIssuesTokens() {
        Role managerRole = new Role();
        managerRole.setRoleCode(RoleCode.MANAGER);

        User pendingManager = User.builder()
                .fullName("Quản Lý Kho")
                .email("manager@example.com")
                .authProvider(AuthProvider.GOOGLE)
                .providerSubject(null)
                .role(managerRole)
                .status(UserStatus.ACTIVE)
                .build();
        pendingManager.setId(77L);

        GoogleUserPayload payload = new GoogleUserPayload("google-sub-manager-999", "manager@example.com", "Quản Lý Kho");
        when(googleAuthService.verifyGoogleToken(anyString())).thenReturn(payload);
        when(userRepository.findByEmailAndIsDeletedFalse("manager@example.com"))
                .thenReturn(Optional.of(pendingManager));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        when(jwtTokenService.generateAccessToken(any())).thenReturn("manager-access-token");
        when(jwtTokenService.extractJti(anyString())).thenReturn(UUID.randomUUID());

        RefreshToken savedRt = RefreshToken.builder()
                .user(pendingManager)
                .tokenFamilyId(UUID.randomUUID())
                .tokenHash("hash-manager")
                .accessTokenJti(UUID.randomUUID())
                .expiresAt(Instant.now().plusSeconds(86400))
                .build();
        when(refreshTokenService.createRefreshToken(any(), any()))
                .thenReturn(new RefreshTokenPair("raw-rt-manager", savedRt));

        AuthResponse response = authService.loginWithGoogle("google-credential", httpResponse);

        assertThat(response.accessToken()).isEqualTo("manager-access-token");
        assertThat(pendingManager.getProviderSubject()).isEqualTo("google-sub-manager-999");
        verify(userRepository).save(pendingManager);
        verify(roleRepository, never()).findByRoleCodeAndIsDeletedFalse(any());
    }
}

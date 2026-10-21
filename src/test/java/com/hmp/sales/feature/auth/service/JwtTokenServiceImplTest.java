package com.hmp.sales.feature.auth.service;

import org.mockito.quality.Strictness;

import com.hmp.sales.config.AppProperties;
import com.hmp.sales.feature.user.entity.Role;
import com.hmp.sales.feature.user.entity.RoleCode;
import com.hmp.sales.feature.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test cho {@link JwtTokenServiceImpl}.
 * Xác minh claims JWT, tích hợp Redis và cơ chế thu hồi token.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtTokenServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private JwtTokenServiceImpl jwtTokenService;
    private AppProperties appProperties;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Secret đủ dài (>= 256 bit) cho HMAC-SHA256
        AppProperties.JwtProperties jwtProps = new AppProperties.JwtProperties(
                "sales-backend",
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
                60L,
                30L
        );
        appProperties = new AppProperties(jwtProps, null, null, null);

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        jwtTokenService = new JwtTokenServiceImpl(appProperties, redisTemplate);

        Role role = new Role();
        role.setRoleCode(RoleCode.CUSTOMER);

        testUser = User.builder()
                .fullName("Nguyễn Văn A")
                .email("test@example.com")
                .role(role)
                .build();
        // Simulate DB assigned ID
        testUser.setId(1L);
    }

    @Test
    @DisplayName("generateAccessToken - Sinh JWT thành công và đăng ký vào Redis")
    void generateAccessToken_success_registersToRedis() {
        String token = jwtTokenService.generateAccessToken(testUser);

        assertThat(token).isNotBlank();
        // Xác minh Redis được gọi để lưu token với TTL
        verify(valueOps).set(
                startsWith(JwtTokenServiceImpl.REDIS_ACCESS_KEY_PREFIX),
                anyString(),
                eq(60L),
                eq(TimeUnit.MINUTES)
        );
    }

    @Test
    @DisplayName("generateAccessToken - JWT không chứa claim permissions (phân quyền động)")
    void generateAccessToken_noPermissionsClaim_inJwtPayload() throws Exception {
        String token = jwtTokenService.generateAccessToken(testUser);

        // Decode JWT payload (base64) để kiểm tra không có "permissions"
        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3);
        String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));

        assertThat(payload).doesNotContain("permissions");
        assertThat(payload).contains("\"roles\"");
        assertThat(payload).contains("ROLE_CUSTOMER");
        assertThat(payload).contains("\"sub\"");
        assertThat(payload).contains("\"email\"");
        assertThat(payload).contains("\"jti\"");
    }

    @Test
    @DisplayName("generateAccessToken - JWT chứa roles với prefix ROLE_")
    void generateAccessToken_containsRoleWithPrefix() {
        String token = jwtTokenService.generateAccessToken(testUser);
        String[] parts = token.split("\\.");
        String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));

        assertThat(payload).contains("ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("extractJti - Đọc JTI từ JWT hợp lệ thành công")
    void extractJti_validToken_returnsJti() {
        String token = jwtTokenService.generateAccessToken(testUser);
        UUID jti = jwtTokenService.extractJti(token);

        assertThat(jti).isNotNull();
    }

    @Test
    @DisplayName("revokeAccessToken - Xóa đúng key auth:access:{jti} trên Redis")
    void revokeAccessToken_deletesCorrectRedisKey() {
        UUID jti = UUID.randomUUID();
        when(redisTemplate.delete(JwtTokenServiceImpl.REDIS_ACCESS_KEY_PREFIX + jti)).thenReturn(true);

        jwtTokenService.revokeAccessToken(jti);

        verify(redisTemplate).delete(JwtTokenServiceImpl.REDIS_ACCESS_KEY_PREFIX + jti);
    }

    @Test
    @DisplayName("generateJti - Mỗi lần sinh UUID mới và duy nhất")
    void generateJti_returnsUniqueValues() {
        UUID jti1 = jwtTokenService.generateJti();
        UUID jti2 = jwtTokenService.generateJti();

        assertThat(jti1).isNotNull();
        assertThat(jti2).isNotNull();
        assertThat(jti1).isNotEqualTo(jti2);
    }
}

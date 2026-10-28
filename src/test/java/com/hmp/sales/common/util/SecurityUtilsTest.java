package com.hmp.sales.common.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Kiểm thử đơn vị cho {@link SecurityUtils}.
 */
class SecurityUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Hàm khởi tạo private phải ném UnsupportedOperationException khi cố gắng khởi tạo")
    void constructor_throwsException() throws Exception {
        Constructor<SecurityUtils> constructor = SecurityUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("extractUserId(Jwt) trả về ID khi JWT hợp lệ")
    void extractUserId_withValidJwt_returnsId() {
        Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .subject("123")
                .build();

        Long userId = SecurityUtils.extractUserId(jwt);
        assertThat(userId).isEqualTo(123L);
    }

    @Test
    @DisplayName("extractUserId(Jwt) trả về null khi JWT null hoặc subject không hợp lệ")
    void extractUserId_withInvalidJwt_returnsNull() {
        assertThat(SecurityUtils.extractUserId((Jwt) null)).isNull();

        Jwt jwtNullSubject = Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .claim("custom", "val")
                .build();
        assertThat(SecurityUtils.extractUserId(jwtNullSubject)).isNull();

        Jwt jwtInvalidSubject = Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .subject("abc")
                .build();
        assertThat(SecurityUtils.extractUserId(jwtInvalidSubject)).isNull();
    }

    @Test
    @DisplayName("extractUserId(Authentication) trả về ID khi principal là Jwt")
    void extractUserId_withJwtAuthentication_returnsId() {
        Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .subject("456")
                .build();
        Authentication auth = new TestingAuthenticationToken(jwt, null);

        Long userId = SecurityUtils.extractUserId(auth);
        assertThat(userId).isEqualTo(456L);
    }

    @Test
    @DisplayName("extractUserId(Authentication) trả về ID khi principal là username số")
    void extractUserId_withNameAuthentication_returnsId() {
        Authentication auth = new TestingAuthenticationToken("789", null);

        Long userId = SecurityUtils.extractUserId(auth);
        assertThat(userId).isEqualTo(789L);
    }

    @Test
    @DisplayName("extractUserId(Authentication) trả về null khi auth null hoặc tên không phải số")
    void extractUserId_withInvalidAuth_returnsNull() {
        assertThat(SecurityUtils.extractUserId((Authentication) null)).isNull();

        Authentication nonNumericAuth = new TestingAuthenticationToken("admin@example.com", null);
        assertThat(SecurityUtils.extractUserId(nonNumericAuth)).isNull();
    }

    @Test
    @DisplayName("extractCurrentUserId lấy ID từ SecurityContextHolder")
    void extractCurrentUserId_fromSecurityContext_returnsId() {
        assertThat(SecurityUtils.extractCurrentUserId()).isNull();

        Authentication auth = new TestingAuthenticationToken("999", null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(SecurityUtils.extractCurrentUserId()).isEqualTo(999L);
    }
}


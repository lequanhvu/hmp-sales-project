package com.hmp.sales.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PermissionEvaluationService Tests")
class PermissionEvaluationServiceTest {

    private PermissionEvaluationService permissionEvaluationService;

    @BeforeEach
    void setUp() {
        permissionEvaluationService = new PermissionEvaluationServiceImpl();
    }

    @Test
    @DisplayName("hasPermission: trả về false khi authentication là null")
    void hasPermission_NullAuth_ReturnsFalse() {
        boolean result = permissionEvaluationService.hasPermission(null, "PRODUCT_PRICE_VIEW");
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("hasPermission: trả về false khi người dùng là anonymous / chưa xác thực")
    void hasPermission_Unauthenticated_ReturnsFalse() {
        Authentication unauth = new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
        unauth.setAuthenticated(false);

        boolean result = permissionEvaluationService.hasPermission(unauth, "PRODUCT_PRICE_VIEW");
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("hasPermission: trả về true khi JWT authorities chứa quyền tương ứng")
    void hasPermission_ContainsPermission_ReturnsTrue() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user-1", "token",
                List.of(
                        new SimpleGrantedAuthority("ROLE_CUSTOMER"),
                        new SimpleGrantedAuthority("PRODUCT_PRICE_VIEW"),
                        new SimpleGrantedAuthority("ORDER_CREATE")
                )
        );

        boolean result = permissionEvaluationService.hasPermission(auth, "PRODUCT_PRICE_VIEW");
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("hasPermission: trả về false khi authorities không chứa quyền cần kiểm tra")
    void hasPermission_DoesNotContainPermission_ReturnsFalse() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user-1", "token",
                List.of(
                        new SimpleGrantedAuthority("ROLE_CUSTOMER"),
                        new SimpleGrantedAuthority("ORDER_CREATE")
                )
        );

        boolean result = permissionEvaluationService.hasPermission(auth, "PRODUCT_PRICE_VIEW");
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("canViewProductPrice: trả về true khi caller có quyền PRODUCT_PRICE_VIEW")
    void canViewProductPrice_HasPermission_ReturnsTrue() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user-1", "token",
                List.of(new SimpleGrantedAuthority("PRODUCT_PRICE_VIEW"))
        );

        boolean result = permissionEvaluationService.canViewProductPrice(auth);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("canViewProductPrice: trả về false khi caller không có quyền PRODUCT_PRICE_VIEW")
    void canViewProductPrice_NoPermission_ReturnsFalse() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user-1", "token",
                Collections.emptyList()
        );

        boolean result = permissionEvaluationService.canViewProductPrice(auth);
        assertThat(result).isFalse();
    }
}

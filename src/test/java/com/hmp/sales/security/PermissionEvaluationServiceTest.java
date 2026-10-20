package com.hmp.sales.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionEvaluationService Tests")
class PermissionEvaluationServiceTest {

    @Mock
    private PermissionAuthorizationManager permissionAuthorizationManager;

    private PermissionEvaluationService permissionEvaluationService;

    @BeforeEach
    void setUp() {
        permissionEvaluationService = new PermissionEvaluationServiceImpl(permissionAuthorizationManager);
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
    @DisplayName("hasPermission: trả về true khi vai trò của user có quyền trong cache RAM")
    void hasPermission_RoleHasPermissionInCache_ReturnsTrue() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user-1", "token",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );

        when(permissionAuthorizationManager.hasPermission(auth, "PRODUCT_PRICE_VIEW")).thenReturn(true);

        boolean result = permissionEvaluationService.hasPermission(auth, "PRODUCT_PRICE_VIEW");
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("hasPermission: trả về false khi vai trò của user không có quyền trong cache RAM")
    void hasPermission_RoleDoesNotHavePermissionInCache_ReturnsFalse() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user-1", "token",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );

        when(permissionAuthorizationManager.hasPermission(auth, "PRODUCT_PRICE_VIEW")).thenReturn(false);

        boolean result = permissionEvaluationService.hasPermission(auth, "PRODUCT_PRICE_VIEW");
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("canViewProductPrice: trả về true khi caller có quyền PRODUCT_PRICE_VIEW trong cache")
    void canViewProductPrice_HasPermission_ReturnsTrue() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user-1", "token",
                List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))
        );

        when(permissionAuthorizationManager.hasPermission(auth, "PRODUCT_PRICE_VIEW")).thenReturn(true);

        boolean result = permissionEvaluationService.canViewProductPrice(auth);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("canViewProductPrice: trả về false khi caller không có quyền PRODUCT_PRICE_VIEW trong cache")
    void canViewProductPrice_NoPermission_ReturnsFalse() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user-1", "token",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );

        when(permissionAuthorizationManager.hasPermission(auth, "PRODUCT_PRICE_VIEW")).thenReturn(false);

        boolean result = permissionEvaluationService.canViewProductPrice(auth);
        assertThat(result).isFalse();
    }
}

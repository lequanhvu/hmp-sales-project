package com.hmp.sales.security;

import com.hmp.sales.feature.user.entity.Permission;
import com.hmp.sales.feature.user.entity.Role;
import com.hmp.sales.feature.user.entity.RoleCode;
import com.hmp.sales.feature.user.entity.RolePermission;
import com.hmp.sales.feature.user.repository.RolePermissionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionAuthorizationManager Unit Tests")
class PermissionAuthorizationManagerTest {

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    private PermissionAuthorizationManager authorizationManager;

    private Role customerRole;
    private Role managerRole;
    private Permission orderCreatePerm;
    private Permission orderCancelPerm;
    private Permission orderCalculatePerm;
    private Permission priceViewPerm;

    @BeforeEach
    void setUp() {
        customerRole = Role.builder().id(3).roleCode(RoleCode.CUSTOMER).build();
        managerRole = Role.builder().id(2).roleCode(RoleCode.MANAGER).build();

        orderCreatePerm = Permission.builder()
                .id(1)
                .permissionName("ORDER_CREATE")
                .apiPath("/api/v1/orders")
                .method("POST")
                .build();

        orderCancelPerm = Permission.builder()
                .id(2)
                .permissionName("ORDER_CANCEL_OWN")
                .apiPath("/api/v1/orders/{id}/cancel")
                .method("POST")
                .build();

        orderCalculatePerm = Permission.builder()
                .id(3)
                .permissionName("ORDER_CALCULATE")
                .apiPath("/api/v1/manager/orders/{id}/calculate")
                .method("POST")
                .build();

        priceViewPerm = Permission.builder()
                .id(4)
                .permissionName("PRODUCT_PRICE_VIEW")
                .apiPath("/api/v1/products/price")
                .method("GET")
                .build();

        List<RolePermission> activeLinks = List.of(
                new RolePermission(customerRole, orderCreatePerm),
                new RolePermission(customerRole, orderCancelPerm),
                new RolePermission(managerRole, orderCalculatePerm),
                new RolePermission(managerRole, priceViewPerm)
        );

        when(rolePermissionRepository.findAllActiveRolePermissions()).thenReturn(activeLinks);

        authorizationManager = new PermissionAuthorizationManager(rolePermissionRepository);
        authorizationManager.loadCache();
    }

    @Test
    @DisplayName("loadCache: Nạp danh sách quyền và ánh xạ cả tiền tố ROLE_ và không tiền tố")
    void loadCache_MapsRolesCorrectly() {
        var cache = authorizationManager.getRolePermissionsCache();

        assertThat(cache).containsKey("CUSTOMER");
        assertThat(cache).containsKey("ROLE_CUSTOMER");
        assertThat(cache.get("ROLE_CUSTOMER")).hasSize(2);

        assertThat(cache).containsKey("MANAGER");
        assertThat(cache).containsKey("ROLE_MANAGER");
        assertThat(cache.get("ROLE_MANAGER")).hasSize(2);
    }

    @Test
    @DisplayName("check: Trả về false khi authentication là null")
    void check_NullAuth_ReturnsFalse() {
        RequestAuthorizationContext context = mock(RequestAuthorizationContext.class);

        AuthorizationDecision decision = authorizationManager.authorize(() -> null, context);

        assertThat(decision).isNotNull();
        assertThat(decision.isGranted()).isFalse();
    }

    @Test
    @DisplayName("check: Trả về false khi authentication là Anonymous")
    void check_AnonymousAuth_ReturnsFalse() {
        Authentication anonymous = new AnonymousAuthenticationToken(
                "key", "anon", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
        RequestAuthorizationContext context = mock(RequestAuthorizationContext.class);

        AuthorizationDecision decision = authorizationManager.authorize(() -> anonymous, context);

        assertThat(decision.isGranted()).isFalse();
    }

    @Test
    @DisplayName("check: Cấp quyền khi vai trò có quyền khớp chính xác URI và HTTP method")
    void check_ExactMatch_ReturnsTrue() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "cust-1", "token", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/orders");
        when(request.getMethod()).thenReturn("POST");

        RequestAuthorizationContext context = new RequestAuthorizationContext(request);

        AuthorizationDecision decision = authorizationManager.authorize(() -> auth, context);

        assertThat(decision.isGranted()).isTrue();
    }

    @Test
    @DisplayName("check: Cấp quyền khi URI chứa path variable (ví dụ: /api/v1/orders/123/cancel)")
    void check_PathVariableMatch_ReturnsTrue() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "cust-1", "token", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/orders/123/cancel");
        when(request.getMethod()).thenReturn("POST");

        RequestAuthorizationContext context = new RequestAuthorizationContext(request);

        AuthorizationDecision decision = authorizationManager.authorize(() -> auth, context);

        assertThat(decision.isGranted()).isTrue();
    }

    @Test
    @DisplayName("check: Từ chối truy cập khi URI khớp nhưng sai HTTP method (ví dụ GET thay vì POST)")
    void check_WrongMethod_ReturnsFalse() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "cust-1", "token", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/orders");
        when(request.getMethod()).thenReturn("GET"); // Cần POST nhưng gọi GET

        RequestAuthorizationContext context = new RequestAuthorizationContext(request);

        AuthorizationDecision decision = authorizationManager.authorize(() -> auth, context);

        assertThat(decision.isGranted()).isFalse();
    }

    @Test
    @DisplayName("check: Từ chối truy cập khi vai trò không có quyền truy cập endpoint này")
    void check_NoMatchingPermission_ReturnsFalse() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "cust-1", "token", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/manager/orders/10/calculate");
        when(request.getMethod()).thenReturn("POST");

        RequestAuthorizationContext context = new RequestAuthorizationContext(request);

        // CUSTOMER không có quyền ORDER_CALCULATE
        AuthorizationDecision decision = authorizationManager.authorize(() -> auth, context);

        assertThat(decision.isGranted()).isFalse();
    }

    @Test
    @DisplayName("hasPermission: Trả về true khi vai trò trong cache có tên quyền tương ứng")
    void hasPermission_HasPermission_ReturnsTrue() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "mgr-1", "token", List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))
        );

        boolean hasPriceView = authorizationManager.hasPermission(auth, "PRODUCT_PRICE_VIEW");
        assertThat(hasPriceView).isTrue();
    }

    @Test
    @DisplayName("hasPermission: Trả về false khi vai trò không có quyền tương ứng trong cache")
    void hasPermission_DoesNotHavePermission_ReturnsFalse() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "cust-1", "token", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );

        boolean hasPriceView = authorizationManager.hasPermission(auth, "PRODUCT_PRICE_VIEW");
        assertThat(hasPriceView).isFalse();
    }
}

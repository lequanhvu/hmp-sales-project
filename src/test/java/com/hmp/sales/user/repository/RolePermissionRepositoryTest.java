package com.hmp.sales.user.repository;

import com.hmp.sales.user.entity.Permission;
import com.hmp.sales.user.entity.Role;
import com.hmp.sales.user.entity.RoleCode;
import com.hmp.sales.user.entity.RolePermission;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("RolePermissionRepository Integration Tests")
class RolePermissionRepositoryTest {

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Gán quyền cho vai trò và kiểm tra findPermissionsByRoleId cùng hasPermission")
    void assignPermissionToRole_andQuerySuccessfully() {
        Role customerRole = Role.builder()
                .roleCode(RoleCode.CUSTOMER)
                .roleName("Khách hàng")
                .build();
        entityManager.persist(customerRole);

        Permission priceViewPerm = Permission.builder()
                .permissionName("PRODUCT_PRICE_VIEW")
                .apiPath("/api/v1/products/**")
                .method("GET")
                .module("PRODUCT")
                .build();
        entityManager.persist(priceViewPerm);

        Permission orderCreatePerm = Permission.builder()
                .permissionName("ORDER_CREATE")
                .apiPath("/api/v1/orders")
                .method("POST")
                .module("ORDER")
                .build();
        entityManager.persist(orderCreatePerm);
        entityManager.flush();

        RolePermission rp1 = new RolePermission(customerRole, priceViewPerm);
        RolePermission rp2 = new RolePermission(customerRole, orderCreatePerm);
        entityManager.persist(rp1);
        entityManager.persist(rp2);
        entityManager.flush();
        entityManager.clear();

        List<Permission> permissions = rolePermissionRepository.findPermissionsByRoleId(customerRole.getId());
        assertThat(permissions).hasSize(2)
                .extracting(Permission::getPermissionName)
                .containsExactlyInAnyOrder("PRODUCT_PRICE_VIEW", "ORDER_CREATE");

        boolean hasPriceView = rolePermissionRepository.hasPermission(customerRole.getId(), "PRODUCT_PRICE_VIEW");
        boolean hasAdminPerm = rolePermissionRepository.hasPermission(customerRole.getId(), "USER_LOCK");

        assertThat(hasPriceView).isTrue();
        assertThat(hasAdminPerm).isFalse();
    }

    @Test
    @DisplayName("hasPermission trả về false nếu RolePermission bị xóa mềm")
    void hasPermission_whenSoftDeleted_returnsFalse() {
        Role role = Role.builder().roleCode(RoleCode.CUSTOMER).roleName("Khách hàng").build();
        entityManager.persist(role);

        Permission perm = Permission.builder()
                .permissionName("PRODUCT_PRICE_VIEW")
                .apiPath("/api/v1/products/**")
                .method("GET")
                .module("PRODUCT")
                .build();
        entityManager.persist(perm);
        entityManager.flush();

        RolePermission rp = new RolePermission(role, perm);
        rp.setIsDeleted(true);
        entityManager.persist(rp);
        entityManager.flush();
        entityManager.clear();

        boolean hasPriceView = rolePermissionRepository.hasPermission(role.getId(), "PRODUCT_PRICE_VIEW");
        assertThat(hasPriceView).isFalse();
    }
}

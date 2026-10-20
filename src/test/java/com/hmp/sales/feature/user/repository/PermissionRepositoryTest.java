package com.hmp.sales.feature.user.repository;

import  com.hmp.sales.feature.user.entity.Permission;
import com.hmp.sales.feature.user.repository.PermissionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("PermissionRepository Integration Tests")
class PermissionRepositoryTest {

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Lưu và tìm kiếm Permission theo PermissionName thành công")
    void findByPermissionName_whenExists_returnsPermission() {
        Permission perm = Permission.builder()
                .permissionName("PRODUCT_PRICE_VIEW")
                .apiPath("/api/v1/products/**")
                .method("GET")
                .module("PRODUCT")
                .description("Quyền xem giá sản phẩm trên website")
                .build();
        entityManager.persist(perm);
        entityManager.flush();

        Optional<Permission> found = permissionRepository.findByPermissionName("PRODUCT_PRICE_VIEW");

        assertThat(found).isPresent();
        assertThat(found.get().getModule()).isEqualTo("PRODUCT");
        assertThat(found.get().getMethod()).isEqualTo("GET");
        assertThat(found.get().getIsDeleted()).isFalse();
    }

    @Test
    @DisplayName("Tìm kiếm theo apiPath và method chưa bị xóa mềm")
    void findByApiPathAndMethodAndIsDeletedFalse_returnsMatch() {
        Permission p1 = Permission.builder()
                .permissionName("ORDER_CREATE")
                .apiPath("/api/v1/orders")
                .method("POST")
                .module("ORDER")
                .build();
        entityManager.persist(p1);
        entityManager.flush();

        Optional<Permission> found = permissionRepository.findByApiPathAndMethodAndIsDeletedFalse(
                "/api/v1/orders", "POST");

        assertThat(found).isPresent();
        assertThat(found.get().getPermissionName()).isEqualTo("ORDER_CREATE");
    }

    @Test
    @DisplayName("Tìm kiếm danh sách theo danh sách tên mã quyền (In clause)")
    void findByPermissionNameInAndIsDeletedFalse_returnsMatchedList() {
        Permission p1 = Permission.builder().permissionName("PERM_A").apiPath("/a").method("GET").module("A").build();
        Permission p2 = Permission.builder().permissionName("PERM_B").apiPath("/b").method("GET").module("B").build();
        Permission p3 = Permission.builder().permissionName("PERM_C").apiPath("/c").method("GET").module("C").isDeleted(true).build();
        entityManager.persist(p1);
        entityManager.persist(p2);
        entityManager.persist(p3);
        entityManager.flush();

        List<Permission> result = permissionRepository.findByPermissionNameInAndIsDeletedFalse(
                List.of("PERM_A", "PERM_C"));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getPermissionName()).isEqualTo("PERM_A");
    }
}

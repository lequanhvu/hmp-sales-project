package com.hmp.sales.feature.user.repository;

import com.hmp.sales.feature.user.entity.Role;
import com.hmp.sales.feature.user.entity.RoleCode;
import com.hmp.sales.feature.user.repository.RoleRepository;
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
@DisplayName("RoleRepository Integration Tests")
class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Lưu và tìm kiếm vai trò theo RoleCode thành công")
    void findByRoleCode_whenRoleExists_returnsRole() {
        Role role = Role.builder()
                .roleCode(RoleCode.ADMIN)
                .roleName("Quản trị viên")
                .description("Toàn quyền hệ thống")
                .isDeleted(false)
                .build();
        entityManager.persist(role);
        entityManager.flush();

        Optional<Role> found = roleRepository.findByRoleCode(RoleCode.ADMIN);

        assertThat(found).isPresent();
        assertThat(found.get().getRoleName()).isEqualTo("Quản trị viên");
        assertThat(found.get().getCreatedAt()).isNotNull();
        assertThat(found.get().getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findByRoleCodeAndIsDeletedFalse bỏ qua các bản ghi bị xóa mềm")
    void findByRoleCodeAndIsDeletedFalse_whenDeleted_returnsEmpty() {
        Role deletedRole = Role.builder()
                .roleCode(RoleCode.MANAGER)
                .roleName("Quản lý")
                .isDeleted(true)
                .build();
        entityManager.persist(deletedRole);
        entityManager.flush();

        Optional<Role> active = roleRepository.findByRoleCodeAndIsDeletedFalse(RoleCode.MANAGER);
        Optional<Role> any = roleRepository.findByRoleCode(RoleCode.MANAGER);

        assertThat(active).isEmpty();
        assertThat(any).isPresent();
    }

    @Test
    @DisplayName("findAllByIsDeletedFalse chỉ trả về các vai trò đang hoạt động")
    void findAllByIsDeletedFalse_returnsOnlyActiveRoles() {
        Role active = Role.builder().roleCode(RoleCode.CUSTOMER).roleName("Khách hàng").isDeleted(false).build();
        Role deleted = Role.builder().roleCode(RoleCode.MANAGER).roleName("Quản lý").isDeleted(true).build();
        entityManager.persist(active);
        entityManager.persist(deleted);
        entityManager.flush();

        List<Role> activeRoles = roleRepository.findAllByIsDeletedFalse();

        assertThat(activeRoles).hasSize(1);
        assertThat(activeRoles.getFirst().getRoleCode()).isEqualTo(RoleCode.CUSTOMER);
    }
}

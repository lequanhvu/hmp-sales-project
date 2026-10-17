package com.hmp.sales.user.repository;

import com.hmp.sales.user.entity.AuthProvider;
import com.hmp.sales.user.entity.Role;
import com.hmp.sales.user.entity.RoleCode;
import com.hmp.sales.user.entity.User;
import com.hmp.sales.user.entity.UserStatus;
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
@DisplayName("UserRepository Integration Tests")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Lưu và tìm kiếm người dùng theo email và idWithRole thành công")
    void saveAndFindUser_byEmailAndIdWithRole_success() {
        Role role = Role.builder()
                .roleCode(RoleCode.CUSTOMER)
                .roleName("Khách hàng")
                .build();
        entityManager.persist(role);

        User customer = User.builder()
                .fullName("Nguyễn Văn A")
                .email("nguyenvana@gmail.com")
                .phone("0901234567")
                .authProvider(AuthProvider.GOOGLE)
                .providerSubject("google-sub-12345")
                .status(UserStatus.ACTIVE)
                .role(role)
                .build();
        entityManager.persist(customer);
        entityManager.flush();
        entityManager.clear();

        Optional<User> byEmail = userRepository.findByEmailAndIsDeletedFalse("nguyenvana@gmail.com");
        assertThat(byEmail).isPresent();
        assertThat(byEmail.get().getFullName()).isEqualTo("Nguyễn Văn A");
        assertThat(byEmail.get().getAuthProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(byEmail.get().getStatus()).isEqualTo(UserStatus.ACTIVE);

        Optional<User> withRole = userRepository.findByIdWithRole(customer.getId());
        assertThat(withRole).isPresent();
        assertThat(withRole.get().getRole().getRoleCode()).isEqualTo(RoleCode.CUSTOMER);
    }

    @Test
    @DisplayName("Tìm kiếm người dùng theo Google Provider và ProviderSubject thành công")
    void findByAuthProviderAndProviderSubject_success() {
        Role role = Role.builder().roleCode(RoleCode.CUSTOMER).roleName("Khách hàng").build();
        entityManager.persist(role);

        User user = User.builder()
                .fullName("Trần Thị B")
                .email("tranthib@gmail.com")
                .authProvider(AuthProvider.GOOGLE)
                .providerSubject("google-sub-999")
                .role(role)
                .build();
        entityManager.persist(user);
        entityManager.flush();

        Optional<User> found = userRepository.findByAuthProviderAndProviderSubjectAndIsDeletedFalse(
                AuthProvider.GOOGLE, "google-sub-999");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("tranthib@gmail.com");
    }

    @Test
    @DisplayName("existsByEmailAndIsDeletedFalse và existsByPhoneAndIsDeletedFalse hoạt động chính xác")
    void existsByEmailAndPhone_checksCorrectly() {
        Role role = Role.builder().roleCode(RoleCode.MANAGER).roleName("Quản lý").build();
        entityManager.persist(role);

        User manager = User.builder()
                .fullName("Lê Văn C")
                .email("levanc@hmp.com")
                .phone("0912345678")
                .passwordHash("$2a$10$hashedpassword...")
                .authProvider(AuthProvider.LOCAL)
                .role(role)
                .build();
        entityManager.persist(manager);
        entityManager.flush();

        assertThat(userRepository.existsByEmailAndIsDeletedFalse("levanc@hmp.com")).isTrue();
        assertThat(userRepository.existsByEmailAndIsDeletedFalse("other@hmp.com")).isFalse();
        assertThat(userRepository.existsByPhoneAndIsDeletedFalse("0912345678")).isTrue();
        assertThat(userRepository.existsByPhoneAndIsDeletedFalse("0999999999")).isFalse();
    }

    @Test
    @DisplayName("findByRoleCode lọc đúng danh sách người dùng theo vai trò")
    void findByRoleCode_returnsMatchingUsers() {
        Role adminRole = Role.builder().roleCode(RoleCode.ADMIN).roleName("Admin").build();
        Role custRole = Role.builder().roleCode(RoleCode.CUSTOMER).roleName("Customer").build();
        entityManager.persist(adminRole);
        entityManager.persist(custRole);

        User u1 = User.builder().fullName("Admin 1").email("admin1@hmp.com").authProvider(AuthProvider.LOCAL).role(adminRole).build();
        User u2 = User.builder().fullName("Cust 1").email("cust1@gmail.com").authProvider(AuthProvider.GOOGLE).role(custRole).build();
        User u3 = User.builder().fullName("Cust 2").email("cust2@gmail.com").authProvider(AuthProvider.GOOGLE).role(custRole).build();
        entityManager.persist(u1);
        entityManager.persist(u2);
        entityManager.persist(u3);
        entityManager.flush();

        List<User> customers = userRepository.findByRoleCode(RoleCode.CUSTOMER);
        assertThat(customers).hasSize(2)
                .extracting(User::getEmail)
                .containsExactlyInAnyOrder("cust1@gmail.com", "cust2@gmail.com");
    }
}

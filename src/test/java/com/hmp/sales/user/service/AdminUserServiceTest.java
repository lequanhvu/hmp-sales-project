package com.hmp.sales.user.service;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.common.exception.ResourceNotFoundException;
import com.hmp.sales.systemlog.SystemLogService;
import com.hmp.sales.user.dto.CreateManagerRequest;
import com.hmp.sales.user.dto.UserStatusResponse;
import com.hmp.sales.user.dto.UserSummaryResponse;
import com.hmp.sales.user.entity.AuthProvider;
import com.hmp.sales.user.entity.Role;
import com.hmp.sales.user.entity.RoleCode;
import com.hmp.sales.user.entity.User;
import com.hmp.sales.user.entity.UserStatus;
import com.hmp.sales.user.repository.RoleRepository;
import com.hmp.sales.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUserService Unit Tests")
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private SystemLogService systemLogService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    private AdminUserServiceImpl adminUserService;

    private User activeUser;
    private User lockedUser;
    private Role customerRole;
    private Role managerRole;

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserServiceImpl(
                userRepository,
                roleRepository,
                systemLogService,
                stringRedisTemplate
        );
        customerRole = Role.builder()
                .id(3)
                .roleCode(RoleCode.CUSTOMER)
                .roleName("Khách hàng")
                .isDeleted(false)
                .build();

        managerRole = Role.builder()
                .id(2)
                .roleCode(RoleCode.MANAGER)
                .roleName("Quản lý")
                .isDeleted(false)
                .build();

        activeUser = User.builder()
                .id(10L)
                .fullName("Nguyễn Văn A")
                .email("vana@example.com")
                .status(UserStatus.ACTIVE)
                .role(customerRole)
                .isDeleted(false)
                .build();

        lockedUser = User.builder()
                .id(20L)
                .fullName("Trần Thị B")
                .email("thib@example.com")
                .status(UserStatus.LOCKED)
                .role(customerRole)
                .isDeleted(false)
                .build();
    }

    @Nested
    @DisplayName("Nghiệp vụ khóa tài khoản (lockUser)")
    class LockUserTests {

        @Test
        @DisplayName("Khóa tài khoản thành công khi user đang ACTIVE")
        void lockUser_Success() {
            when(userRepository.findById(10L)).thenReturn(Optional.of(activeUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UserStatusResponse response = adminUserService.lockUser(10L, 999L);

            assertThat(response.userId()).isEqualTo(10L);
            assertThat(response.status()).isEqualTo(UserStatus.LOCKED);
            assertThat(activeUser.getStatus()).isEqualTo(UserStatus.LOCKED);
            verify(systemLogService).log(eq(999L), eq("USER_LOCKED"), eq("USER"), eq(10L), any());
        }

        @Test
        @DisplayName("Ném lỗi khi Admin cố tình tự khóa tài khoản của chính mình")
        void lockUser_SelfLock_ThrowsException() {
            when(userRepository.findById(10L)).thenReturn(Optional.of(activeUser));

            assertThatThrownBy(() -> adminUserService.lockUser(10L, 10L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Admin không thể tự khóa tài khoản của chính mình");
        }

        @Test
        @DisplayName("Ném lỗi khi khóa user đã ở trạng thái LOCKED")
        void lockUser_AlreadyLocked_ThrowsException() {
            when(userRepository.findById(20L)).thenReturn(Optional.of(lockedUser));

            assertThatThrownBy(() -> adminUserService.lockUser(20L, 999L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("trạng thái khóa");
        }

        @Test
        @DisplayName("Ném lỗi khi không tìm thấy tài khoản người dùng")
        void lockUser_NotFound_ThrowsException() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminUserService.lockUser(99L, 999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ mở khóa tài khoản (unlockUser)")
    class UnlockUserTests {

        @Test
        @DisplayName("Mở khóa thành công khi user đang LOCKED")
        void unlockUser_Success() {
            when(userRepository.findById(20L)).thenReturn(Optional.of(lockedUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UserStatusResponse response = adminUserService.unlockUser(20L, 999L);

            assertThat(response.userId()).isEqualTo(20L);
            assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
            assertThat(lockedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
            verify(systemLogService).log(eq(999L), eq("USER_UNLOCKED"), eq("USER"), eq(20L), any());
        }

        @Test
        @DisplayName("Ném lỗi khi mở khóa user đang ACTIVE")
        void unlockUser_AlreadyActive_ThrowsException() {
            when(userRepository.findById(10L)).thenReturn(Optional.of(activeUser));

            assertThatThrownBy(() -> adminUserService.unlockUser(10L, 999L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("hoạt động");
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ xóa mềm người dùng (softDeleteUser)")
    class SoftDeleteUserTests {

        @Test
        @DisplayName("Xóa mềm thành công: đánh dấu isDeleted = true và status = LOCKED")
        void softDeleteUser_Success() {
            when(userRepository.findById(10L)).thenReturn(Optional.of(activeUser));

            adminUserService.softDeleteUser(10L, 999L);

            assertThat(activeUser.getIsDeleted()).isTrue();
            assertThat(activeUser.getStatus()).isEqualTo(UserStatus.LOCKED);
            verify(userRepository).save(activeUser);
            verify(systemLogService).log(eq(999L), eq("USER_SOFT_DELETED"), eq("USER"), eq(10L), any());
        }

        @Test
        @DisplayName("Ném lỗi khi Admin tự xóa tài khoản của chính mình")
        void softDeleteUser_SelfDelete_ThrowsException() {
            when(userRepository.findById(10L)).thenReturn(Optional.of(activeUser));

            assertThatThrownBy(() -> adminUserService.softDeleteUser(10L, 10L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Admin không thể tự xóa tài khoản của chính mình");
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ tạo tài khoản Manager (createManager)")
    class CreateManagerTests {

        @Test
        @DisplayName("Tạo Manager thành công khi email chưa tồn tại")
        void createManager_Success() {
            CreateManagerRequest request = new CreateManagerRequest("manager@company.com", "Quản lý kho");

            when(userRepository.existsByEmailAndIsDeletedFalse("manager@company.com")).thenReturn(false);
            when(roleRepository.findByRoleCodeAndIsDeletedFalse(RoleCode.MANAGER)).thenReturn(Optional.of(managerRole));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(100L);
                return u;
            });

            UserSummaryResponse response = adminUserService.createManager(request, 999L);

            assertThat(response.userId()).isEqualTo(100L);
            assertThat(response.email()).isEqualTo("manager@company.com");
            assertThat(response.fullName()).isEqualTo("Quản lý kho");
            assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
            assertThat(response.role()).isEqualTo("MANAGER");
            verify(systemLogService).log(eq(999L), eq("MANAGER_CREATED"), eq("USER"), eq(100L), any());
        }

        @Test
        @DisplayName("Ném lỗi khi email đã được sử dụng")
        void createManager_DuplicateEmail_ThrowsException() {
            CreateManagerRequest request = new CreateManagerRequest("existing@company.com", "Quản lý");
            when(userRepository.existsByEmailAndIsDeletedFalse("existing@company.com")).thenReturn(true);

            assertThatThrownBy(() -> adminUserService.createManager(request, 999L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("đã được sử dụng");
        }
    }
}

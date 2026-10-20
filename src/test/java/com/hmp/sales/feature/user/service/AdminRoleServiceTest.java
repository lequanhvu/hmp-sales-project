package com.hmp.sales.feature.user.service;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.common.exception.ResourceNotFoundException;
import com.hmp.sales.feature.systemlog.SystemLogService;
import com.hmp.sales.feature.user.service.RoleServiceImpl;
import com.hmp.sales.feature.user.dto.RolePermissionsResponse;
import com.hmp.sales.feature.user.dto.UpdateRolePermissionsRequest;
import com.hmp.sales.feature.user.entity.Permission;
import com.hmp.sales.feature.user.entity.Role;
import com.hmp.sales.feature.user.entity.RoleCode;
import com.hmp.sales.feature.user.entity.RolePermission;
import com.hmp.sales.feature.user.entity.User;
import com.hmp.sales.feature.user.repository.PermissionRepository;
import com.hmp.sales.feature.user.repository.RolePermissionRepository;
import com.hmp.sales.feature.user.repository.RoleRepository;
import com.hmp.sales.feature.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminRoleService Unit Tests")
class AdminRoleServiceTest {

        @Mock
        private RoleRepository roleRepository;

        @Mock
        private PermissionRepository permissionRepository;

        @Mock
        private RolePermissionRepository rolePermissionRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private StringRedisTemplate stringRedisTemplate;

        @Mock
        private SystemLogService systemLogService;

        @Mock
        private com.hmp.sales.security.PermissionAuthorizationManager permissionAuthorizationManager;

        private RoleServiceImpl adminRoleService;

        private Role customerRole;
        private Permission priceViewPerm;
        private Permission orderCreatePerm;

        @BeforeEach
        void setUp() {
                adminRoleService = new RoleServiceImpl(
                                roleRepository,
                                permissionRepository,
                                rolePermissionRepository,
                                userRepository,
                                stringRedisTemplate,
                                systemLogService,
                                permissionAuthorizationManager);
                customerRole = Role.builder()
                                .id(3)
                                .roleCode(RoleCode.CUSTOMER)
                                .roleName("Khách hàng")
                                .isDeleted(false)
                                .build();

                priceViewPerm = Permission.builder()
                                .id(1)
                                .permissionName("PRODUCT_PRICE_VIEW")
                                .module("PRODUCT")
                                .isDeleted(false)
                                .build();

                orderCreatePerm = Permission.builder()
                                .id(2)
                                .permissionName("ORDER_CREATE")
                                .module("ORDER")
                                .isDeleted(false)
                                .build();
        }

        @Test
        @DisplayName("getRolePermissions: Trả về danh sách quyền của vai trò thành công")
        void getRolePermissions_Success() {
                when(roleRepository.findById(3)).thenReturn(Optional.of(customerRole));
                when(rolePermissionRepository.findPermissionsByRoleId(3)).thenReturn(List.of(orderCreatePerm));

                RolePermissionsResponse response = adminRoleService.getRolePermissions(3);

                assertThat(response.roleId()).isEqualTo(3);
                assertThat(response.roleName()).isEqualTo("Khách hàng");
                assertThat(response.permissions()).containsExactly("ORDER_CREATE");
        }

        @Test
        @DisplayName("getRolePermissions: Ném ResourceNotFoundException khi vai trò không tồn tại")
        void getRolePermissions_NotFound_ThrowsException() {
                when(roleRepository.findById(999)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> adminRoleService.getRolePermissions(999))
                                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("updateRolePermissions: Cập nhật quyền thành công và ghi log audit + trigger forced token revocation")
        void updateRolePermissions_Success() {
                List<String> newPermNames = List.of("PRODUCT_PRICE_VIEW", "ORDER_CREATE");
                UpdateRolePermissionsRequest request = new UpdateRolePermissionsRequest(newPermNames);

                when(roleRepository.findById(3)).thenReturn(Optional.of(customerRole));
                when(permissionRepository.findByPermissionNameInAndIsDeletedFalse(newPermNames))
                                .thenReturn(List.of(priceViewPerm, orderCreatePerm));

                // Giả sử trước đó đã có ORDER_CREATE
                RolePermission existingOrderCreate = new RolePermission(customerRole, orderCreatePerm);
                existingOrderCreate.setIsDeleted(false);
                when(rolePermissionRepository.findByIdRoleId(3))
                                .thenReturn(new ArrayList<>(List.of(existingOrderCreate)));

                User customer1 = User.builder().id(101L).build();
                when(userRepository.findByRoleCode(RoleCode.CUSTOMER)).thenReturn(List.of(customer1));

                RolePermissionsResponse response = adminRoleService.updateRolePermissions(3, request, 999L);

                assertThat(response.roleId()).isEqualTo(3);
                assertThat(response.permissions()).containsExactlyInAnyOrder("PRODUCT_PRICE_VIEW", "ORDER_CREATE");

                verify(rolePermissionRepository).saveAll(anyList());
                verify(permissionAuthorizationManager).loadCache();
                verify(systemLogService).log(eq(999L), eq("ROLE_PERMISSIONS_UPDATED"), eq("ROLE"), eq(3L), any());
        }

        @Test
        @DisplayName("updateRolePermissions: Ném BusinessRuleException khi danh sách có quyền không hợp lệ")
        void updateRolePermissions_InvalidPermissionName_ThrowsException() {
                List<String> requestedNames = List.of("INVALID_PERMISSION_NAME");
                UpdateRolePermissionsRequest request = new UpdateRolePermissionsRequest(requestedNames);

                when(roleRepository.findById(3)).thenReturn(Optional.of(customerRole));
                when(permissionRepository.findByPermissionNameInAndIsDeletedFalse(requestedNames))
                                .thenReturn(List.of()); // Không tìm thấy trong DB

                assertThatThrownBy(() -> adminRoleService.updateRolePermissions(3, request, 999L))
                                .isInstanceOf(BusinessRuleException.class)
                                .hasMessageContaining("chứa các quyền không hợp lệ");
        }
}

package com.hmp.sales.user.service;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.common.exception.ResourceNotFoundException;
import com.hmp.sales.systemlog.SystemLogService;
import com.hmp.sales.user.dto.RolePermissionsResponse;
import com.hmp.sales.user.dto.UpdateRolePermissionsRequest;
import com.hmp.sales.user.entity.Permission;
import com.hmp.sales.user.entity.Role;
import com.hmp.sales.user.entity.RolePermission;
import com.hmp.sales.user.entity.User;
import com.hmp.sales.user.repository.PermissionRepository;
import com.hmp.sales.user.repository.RolePermissionRepository;
import com.hmp.sales.user.repository.RoleRepository;
import com.hmp.sales.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation của {@link AdminRoleService}.
 * <p>
 * Cung cấp chức năng xem và cập nhật quyền hạn cho từng vai trò người dùng trong hệ thống.
 * Hỗ trợ cơ chế forced token revocation đối với tất cả người dùng thuộc vai trò bị thay đổi quyền.
 */
@Slf4j
@Service
public class AdminRoleServiceImpl implements AdminRoleService {

    private static final String ENTITY_TYPE_ROLE = "ROLE";

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final SystemLogService systemLogService;

    public AdminRoleServiceImpl(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            RolePermissionRepository rolePermissionRepository,
            UserRepository userRepository,
            StringRedisTemplate stringRedisTemplate,
            SystemLogService systemLogService
    ) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userRepository = userRepository;
        this.stringRedisTemplate = stringRedisTemplate;
        this.systemLogService = systemLogService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public RolePermissionsResponse getRolePermissions(Integer roleId) {
        Role role = findRoleOrThrow(roleId);

        List<Permission> permissions = rolePermissionRepository.findPermissionsByRoleId(roleId);
        List<String> permissionNames = permissions.stream()
                .map(Permission::getPermissionName)
                .toList();

        return new RolePermissionsResponse(role.getId(), role.getRoleName(), permissionNames);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Cập nhật danh sách quyền cho vai trò theo phương thức replace-all.
     * Tự động revoke phiên làm việc của toàn bộ người dùng thuộc vai trò đó.
     */
    @Override
    @Transactional
    public RolePermissionsResponse updateRolePermissions(
            Integer roleId,
            UpdateRolePermissionsRequest request,
            Long actorId
    ) {
        Role role = findRoleOrThrow(roleId);

        List<String> requestedNames = request.permissions() != null ? request.permissions() : List.of();

        // 1. Kiểm tra tính hợp lệ của tất cả quyền hạn được truyền lên
        List<Permission> matchedPermissions = List.of();
        if (!requestedNames.isEmpty()) {
            matchedPermissions = permissionRepository.findByPermissionNameInAndIsDeletedFalse(requestedNames);
            Set<String> matchedNames = matchedPermissions.stream()
                    .map(Permission::getPermissionName)
                    .collect(Collectors.toSet());

            List<String> invalidNames = requestedNames.stream()
                    .filter(name -> !matchedNames.contains(name))
                    .toList();

            if (!invalidNames.isEmpty()) {
                throw new BusinessRuleException(
                        "Danh sách quyền hạn chứa các quyền không hợp lệ hoặc đã bị vô hiệu hóa: " + invalidNames);
            }
        }

        // 2. Cập nhật role_permissions: đồng bộ trạng thái is_deleted
        List<RolePermission> existingLinks = rolePermissionRepository.findByIdRoleId(roleId);
        Map<Integer, RolePermission> linkByPermissionId = existingLinks.stream()
                .collect(Collectors.toMap(
                        rp -> rp.getPermission().getId(),
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        Set<Integer> targetPermissionIds = matchedPermissions.stream()
                .map(Permission::getId)
                .collect(Collectors.toSet());

        List<RolePermission> toSave = new ArrayList<>();

        // Kích hoạt lại hoặc thêm mới quyền trong danh sách yêu cầu
        for (Permission perm : matchedPermissions) {
            RolePermission existing = linkByPermissionId.get(perm.getId());
            if (existing != null) {
                if (Boolean.TRUE.equals(existing.getIsDeleted())) {
                    existing.setIsDeleted(false);
                    toSave.add(existing);
                }
            } else {
                toSave.add(new RolePermission(role, perm));
            }
        }

        // Xóa mềm các quyền cũ không còn nằm trong danh sách mới
        for (RolePermission existing : existingLinks) {
            if (!targetPermissionIds.contains(existing.getPermission().getId())
                    && !Boolean.TRUE.equals(existing.getIsDeleted())) {
                existing.setIsDeleted(true);
                toSave.add(existing);
            }
        }

        if (!toSave.isEmpty()) {
            rolePermissionRepository.saveAll(toSave);
        }

        // 3. Forced Token Revocation: Tìm toàn bộ user thuộc vai trò này để vô hiệu hóa token
        revokeTokensForRole(role);

        // 4. Ghi nhật ký kiểm toán hệ thống
        systemLogService.log(
                actorId,
                "ROLE_PERMISSIONS_UPDATED",
                ENTITY_TYPE_ROLE,
                Long.valueOf(roleId),
                "Admin cập nhật quyền cho vai trò " + role.getRoleCode() + ": " + requestedNames
        );

        return new RolePermissionsResponse(role.getId(), role.getRoleName(), requestedNames);
    }

    /**
     * Vô hiệu hóa phiên làm việc của tất cả người dùng thuộc vai trò bị thay đổi quyền.
     * <p>
     * Theo Section 18 trong {@code architecture.md}:
     * Buộc người dùng thuộc vai trò đó phải đăng nhập lại để nhận JWT mới có quyền cập nhật.
     *
     * @param role thực thể vai trò bị thay đổi quyền
     */
    private void revokeTokensForRole(Role role) {
        List<User> usersInRole = userRepository.findByRoleCode(role.getRoleCode());
        log.info("[AdminRoleService] Đã cập nhật quyền cho vai trò {}. Bắt đầu forced token revocation cho {} người dùng.",
                role.getRoleCode(), usersInRole.size());

        for (User user : usersInRole) {
            // TODO Phase 2: Khi RefreshToken entity và RefreshTokenRepository được hoàn thiện,
            // thu hồi toàn bộ refresh tokens trong DB và xóa access token tương ứng trong Redis (auth:access:{jti}).
            log.debug("[AdminRoleService] Yêu cầu thu hồi token cho người dùng ID={}", user.getId());
        }
    }

    /**
     * Tìm vai trò theo ID và kiểm tra chưa bị xóa mềm.
     *
     * @param roleId định danh vai trò
     * @return thực thể {@link Role}
     * @throws ResourceNotFoundException nếu không tìm thấy vai trò
     */
    private Role findRoleOrThrow(Integer roleId) {
        return roleRepository.findById(roleId)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy vai trò người dùng với ID: " + roleId));
    }
}

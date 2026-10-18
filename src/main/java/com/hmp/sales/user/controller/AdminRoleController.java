package com.hmp.sales.user.controller;

import com.hmp.sales.common.response.ApiSuccessResponse;
import com.hmp.sales.user.dto.RolePermissionsResponse;
import com.hmp.sales.user.dto.UpdateRolePermissionsRequest;
import com.hmp.sales.user.service.AdminRoleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller quản lý phân quyền vai trò (Role & Permission) dành cho Admin.
 * <p>
 * Cho phép xem và cập nhật động các quyền hạn được gán cho từng vai trò người dùng trong hệ thống.
 * Mọi endpoint đều yêu cầu xác thực JWT và quyền {@code ROLE_PERMISSION_UPDATE}.
 */
@RestController
@RequestMapping("/api/v1/admin/roles")
public class AdminRoleController {

    private final AdminRoleService adminRoleService;

    /**
     * Khởi tạo AdminRoleController với dependency AdminRoleService.
     *
     * @param adminRoleService service xử lý nghiệp vụ phân quyền vai trò
     */
    public AdminRoleController(AdminRoleService adminRoleService) {
        this.adminRoleService = adminRoleService;
    }

    /**
     * Lấy danh sách toàn bộ quyền hạn đang được gán cho một vai trò.
     * <p>
     * Yêu cầu quyền {@code ROLE_PERMISSION_UPDATE}.
     *
     * @param roleId định danh vai trò cần xem quyền
     * @return danh sách tên các quyền hạn của vai trò
     */
    @GetMapping("/{roleId}/permissions")
    @PreAuthorize("hasAuthority('ROLE_PERMISSION_UPDATE')")
    public ResponseEntity<ApiSuccessResponse<RolePermissionsResponse>> getRolePermissions(
            @PathVariable Integer roleId
    ) {
        RolePermissionsResponse response = adminRoleService.getRolePermissions(roleId);
        return ResponseEntity.ok(ApiSuccessResponse.success(response));
    }

    /**
     * Cập nhật toàn bộ quyền hạn cho một vai trò (replace-all).
     * <p>
     * Yêu cầu quyền {@code ROLE_PERMISSION_UPDATE}.
     * Khi cập nhật thành công, tất cả người dùng thuộc vai trò này sẽ bị thu hồi token
     * và buộc phải đăng nhập lại để nhận quyền mới (Section 18, architecture.md).
     *
     * @param roleId  định danh vai trò cần cập nhật quyền
     * @param request danh sách quyền hạn mới
     * @param jwt     JWT của Admin đang thực hiện yêu cầu
     * @return danh sách quyền hạn mới cập nhật của vai trò
     */
    @PutMapping("/{roleId}/permissions")
    @PreAuthorize("hasAuthority('ROLE_PERMISSION_UPDATE')")
    public ResponseEntity<ApiSuccessResponse<RolePermissionsResponse>> updateRolePermissions(
            @PathVariable Integer roleId,
            @RequestBody @Valid UpdateRolePermissionsRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long actorId = extractActorId(jwt);
        RolePermissionsResponse response = adminRoleService.updateRolePermissions(roleId, request, actorId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Cập nhật quyền cho vai trò thành công.", response));
    }

    /**
     * Trích xuất ID người dùng từ JWT subject claim.
     *
     * @param jwt JWT của người dùng đang xác thực
     * @return ID người dùng
     */
    private Long extractActorId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}

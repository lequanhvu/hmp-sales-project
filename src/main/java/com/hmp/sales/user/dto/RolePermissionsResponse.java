package com.hmp.sales.user.dto;

import java.util.List;

/**
 * Response DTO trả về danh sách các quyền hạn hiện có của một vai trò người dùng.
 *
 * @param roleId định danh vai trò (Role ID)
 * @param roleName tên hiển thị hoặc mã vai trò (ví dụ: "ADMIN", "MANAGER", "CUSTOMER")
 * @param permissions danh sách tên các quyền hạn đang được gán cho vai trò này
 */
public record RolePermissionsResponse(
        Integer roleId,
        String roleName,
        List<String> permissions
) {
}

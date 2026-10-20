package com.hmp.sales.feature.user.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Request DTO chứa danh sách quyền hạn mới được Admin cập nhật cho một vai trò.
 * <p>
 * Danh sách này sẽ thay thế toàn bộ quyền hạn hiện tại của vai trò được chỉ
 * định.
 * Tất cả các quyền hạn trong danh sách phải tồn tại trong bảng permissions.
 *
 * @param permissions danh sách tên các quyền hạn được gán (không được null, có
 *                    thể rỗng nếu muốn tước toàn bộ quyền)
 */
public record UpdateRolePermissionsRequest(
                @NotNull(message = "Danh sách quyền hạn không được để trống (null)") 
                List<String> permissions) {
}

package com.hmp.sales.user.service;

import com.hmp.sales.user.dto.RolePermissionsResponse;
import com.hmp.sales.user.dto.UpdateRolePermissionsRequest;

/**
 * Service quản lý vai trò và phân quyền (Role & Permission) dành cho Admin.
 * <p>
 * Cung cấp các thao tác xem quyền hạn của vai trò và cập nhật quyền hạn động
 * cho từng vai trò (đặc biệt là quyền xem giá {@code PRODUCT_PRICE_VIEW} của Customer).
 * Khi quyền của một vai trò thay đổi, hệ thống sẽ tự động thực hiện forced token revocation
 * đối với tất cả người dùng thuộc vai trò đó để buộc họ đăng nhập lại và nhận quyền mới.
 */
public interface AdminRoleService {

    /**
     * Lấy danh sách các quyền hạn đang hoạt động của một vai trò theo ID.
     *
     * @param roleId định danh vai trò
     * @return {@link RolePermissionsResponse} chứa thông tin vai trò và danh sách tên quyền
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tìm thấy vai trò
     */
    RolePermissionsResponse getRolePermissions(Integer roleId);

    /**
     * Cập nhật toàn bộ quyền hạn cho một vai trò (replace-all).
     * <p>
     * Thao tác này diễn ra trong một giao dịch cơ sở dữ liệu:
     * <ol>
     *     <li>Kiểm tra tính hợp lệ của tất cả quyền hạn trong danh sách yêu cầu.</li>
     *     <li>Cập nhật liên kết trong bảng {@code role_permissions}.</li>
     *     <li>Sau khi commit thành công, kích hoạt cơ chế forced token revocation:
     *         xóa access token (Redis) và đánh dấu thu hồi refresh token (PostgreSQL)
     *         của toàn bộ người dùng thuộc vai trò này.</li>
     *     <li>Ghi nhật ký kiểm toán hệ thống qua {@code SystemLogService}.</li>
     * </ol>
     *
     * @param roleId định danh vai trò cần cập nhật
     * @param request danh sách quyền hạn mới
     * @param actorId định danh người dùng thực hiện thao tác (Admin)
     * @return {@link RolePermissionsResponse} chứa thông tin vai trò và danh sách tên quyền mới cập nhật
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tìm thấy vai trò
     * @throws com.hmp.sales.common.exception.BusinessRuleException nếu danh sách quyền chứa quyền không hợp lệ
     */
    RolePermissionsResponse updateRolePermissions(Integer roleId, UpdateRolePermissionsRequest request, Long actorId);
}

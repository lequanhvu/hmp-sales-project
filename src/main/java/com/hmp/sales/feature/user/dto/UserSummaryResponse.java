package com.hmp.sales.feature.user.dto;

import com.hmp.sales.feature.user.entity.User;
import com.hmp.sales.feature.user.entity.UserStatus;

/**
 * Response DTO tóm tắt thông tin người dùng dành cho Admin xem danh sách.
 * <p>
 * Không bao giờ expose: passwordHash, providerSubject, token hay thông tin nhạy
 * cảm khác.
 *
 * @param userId   ID định danh người dùng
 * @param fullName họ và tên đầy đủ
 * @param email    địa chỉ email
 * @param phone    số điện thoại (có thể null)
 * @param role     tên mã vai trò (ADMIN, MANAGER, CUSTOMER)
 * @param status   trạng thái tài khoản (ACTIVE, LOCKED)
 */
public record UserSummaryResponse(
        Long userId,
        String fullName,
        String email,
        String phone,
        String role,
        UserStatus status) {
    /**
     * Chuyển đổi từ {@link User} entity sang DTO.
     *
     * @param user thực thể người dùng (cần eager fetch role)
     * @return DTO tóm tắt người dùng
     */
    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole() != null ? user.getRole().getRoleCode().name() : null,
                user.getStatus());
    }
}

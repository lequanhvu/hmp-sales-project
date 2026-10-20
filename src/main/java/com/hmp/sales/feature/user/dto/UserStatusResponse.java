package com.hmp.sales.feature.user.dto;

import com.hmp.sales.feature.user.entity.UserStatus;

/**
 * Response DTO trả về trạng thái mới của tài khoản sau thao tác lock/unlock.
 *
 * @param userId ID người dùng
 * @param status trạng thái mới của tài khoản (ACTIVE, LOCKED)
 */
public record UserStatusResponse(
                Long userId,
                UserStatus status) {
}

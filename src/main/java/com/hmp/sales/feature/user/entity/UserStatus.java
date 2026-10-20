package com.hmp.sales.feature.user.entity;

/**
 * Trạng thái hoạt động của tài khoản người dùng trong hệ thống.
 * <ul>
 *     <li>{@link #ACTIVE}: Tài khoản đang hoạt động bình thường và được phép đăng nhập.</li>
 *     <li>{@link #LOCKED}: Tài khoản đã bị Admin khóa quyền truy cập.</li>
 * </ul>
 */
public enum UserStatus {
    /**
     * Tài khoản đang hoạt động bình thường.
     */
    ACTIVE,

    /**
     * Tài khoản bị khóa quyền truy cập.
     */
    LOCKED
}

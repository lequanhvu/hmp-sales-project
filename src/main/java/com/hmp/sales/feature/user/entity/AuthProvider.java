package com.hmp.sales.feature.user.entity;

/**
 * Định danh nhà cung cấp phương thức xác thực cho tài khoản người dùng.
 * <ul>
 *     <li>{@link #GOOGLE}: Xác thực qua Google OAuth (dành cho Khách hàng).</li>
 *     <li>{@link #LOCAL}: Xác thực nội bộ qua username/password (dành cho Admin / Manager).</li>
 * </ul>
 */
public enum AuthProvider {
    /**
     * Xác thực thông qua tài khoản Google OAuth.
     */
    GOOGLE,

    /**
     * Xác thực cục bộ bằng mật khẩu mã hóa trong hệ thống.
     */
    LOCAL
}

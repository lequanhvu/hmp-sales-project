package com.hmp.sales.feature.auth.service;

import com.hmp.sales.feature.auth.dto.AuthResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Hợp đồng dịch vụ xác thực người dùng.
 * <p>
 * Điều phối toàn bộ luồng xác thực: xác minh Google Token, đồng bộ tài khoản,
 * cấp phát JWT Access Token và Refresh Token, xoay vòng token và đăng xuất.
 */
public interface AuthService {

    /**
     * Đăng nhập bằng Google ID Token.
     * <p>
     * Nếu người dùng chưa tồn tại → tạo tài khoản CUSTOMER mới (idempotent).
     * Nếu tài khoản bị khóa ({@code LOCKED}) → từ chối với 403.
     * Ghi Refresh Token vào cookie HttpOnly thông qua {@code response}.
     *
     * @param credential   Google ID Token từ frontend
     * @param httpResponse servlet response để gắn cookie refresh token
     * @return {@link AuthResponse} chứa Access Token JWT
     * @throws com.hmp.sales.common.exception.UnauthorizedOperationException nếu token Google không hợp lệ
     *         hoặc tài khoản bị khóa
     */
    AuthResponse loginWithGoogle(String credential, HttpServletResponse httpResponse);

    /**
     * Làm mới Access Token bằng Refresh Token từ cookie.
     * <p>
     * Xoay vòng Refresh Token: revoke token cũ, phát hành token mới trong cùng family.
     * Ghi Refresh Token mới vào cookie HttpOnly.
     *
     * @param httpRequest  servlet request để đọc cookie refresh token
     * @param httpResponse servlet response để cập nhật cookie
     * @return {@link AuthResponse} chứa Access Token mới
     * @throws com.hmp.sales.common.exception.UnauthorizedOperationException nếu refresh token không hợp lệ,
     *         hết hạn, hoặc phát hiện replay attack
     */
    AuthResponse refreshToken(HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    /**
     * Đăng xuất: thu hồi Access Token trên Redis và Refresh Token trong PostgreSQL,
     * đồng thời xóa cookie.
     *
     * @param authorizationHeader giá trị header Authorization (Bearer {jwt})
     * @param httpRequest         servlet request để đọc cookie refresh token
     * @param httpResponse        servlet response để xóa cookie
     */
    void logout(String authorizationHeader, HttpServletRequest httpRequest, HttpServletResponse httpResponse);
}

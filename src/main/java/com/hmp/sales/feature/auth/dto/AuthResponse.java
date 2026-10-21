package com.hmp.sales.feature.auth.dto;

/**
 * DTO phản hồi sau khi đăng nhập hoặc làm mới token thành công.
 * <p>
 * Access Token được trả về trong JSON body để frontend lưu trong memory.
 * Refresh Token được gửi riêng qua HttpOnly cookie (không xuất hiện trong DTO này).
 *
 * @param accessToken chuỗi JWT Access Token đã ký bằng HMAC-SHA256
 * @param tokenType   kiểu token (luôn là {@code "Bearer"})
 */
public record AuthResponse(
    String accessToken,
    String tokenType
) {
    /**
     * Tạo phản hồi chuẩn Bearer Token.
     *
     * @param accessToken JWT access token
     * @return {@link AuthResponse} với tokenType = "Bearer"
     */
    public static AuthResponse bearer(String accessToken) {
        return new AuthResponse(accessToken, "Bearer");
    }
}

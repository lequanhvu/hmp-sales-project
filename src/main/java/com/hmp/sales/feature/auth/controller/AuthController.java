package com.hmp.sales.feature.auth.controller;

import com.hmp.sales.common.response.ApiSuccessResponse;
import com.hmp.sales.feature.auth.dto.AuthResponse;
import com.hmp.sales.feature.auth.dto.GoogleLoginRequest;
import com.hmp.sales.feature.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller xử lý các endpoint xác thực người dùng.
 * <p>
 * Cung cấp 3 endpoint công khai (WHITELIST trong SecurityConfig):
 * <ul>
 *   <li>{@code POST /api/v1/auth/google} — Đăng nhập bằng Google ID Token.</li>
 *   <li>{@code POST /api/v1/auth/refresh} — Làm mới Access Token bằng Refresh Token từ cookie.</li>
 *   <li>{@code POST /api/v1/auth/logout} — Đăng xuất, thu hồi token.</li>
 * </ul>
 * Controller này hoàn toàn mỏng (thin controller): không chứa business logic,
 * chỉ nhận request → gọi {@link AuthService} → trả về response chuẩn.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * Khởi tạo controller với interface {@link AuthService}.
     *
     * @param authService interface dịch vụ xác thực (không inject concrete class)
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Đăng nhập bằng Google ID Token.
     * <p>
     * Nhận Google Credential (JWT) từ frontend sau khi người dùng hoàn tất
     * luồng Google Sign-In. Nếu tài khoản chưa tồn tại, tự động tạo mới với vai trò CUSTOMER.
     *
     * @param request      request body chứa Google ID Token
     * @param httpResponse servlet response để gắn cookie Refresh Token (HttpOnly)
     * @return {@link ApiSuccessResponse} chứa {@link AuthResponse} với Access Token
     */
    @PostMapping("/google")
    public ResponseEntity<ApiSuccessResponse<AuthResponse>> loginWithGoogle(
            @Valid @RequestBody GoogleLoginRequest request,
            HttpServletResponse httpResponse) {
        AuthResponse authResponse = authService.loginWithGoogle(request.credential(), httpResponse);
        return ResponseEntity.ok(ApiSuccessResponse.success("Đăng nhập Google thành công", authResponse));
    }

    /**
     * Làm mới Access Token bằng Refresh Token đang lưu trong cookie HttpOnly.
     * <p>
     * Token cũ bị thu hồi, token mới được cấp trong cùng Token Family.
     *
     * @param httpRequest  servlet request để đọc cookie Refresh Token
     * @param httpResponse servlet response để cập nhật cookie Refresh Token mới
     * @return {@link ApiSuccessResponse} chứa {@link AuthResponse} với Access Token mới
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiSuccessResponse<AuthResponse>> refreshToken(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        AuthResponse authResponse = authService.refreshToken(httpRequest, httpResponse);
        return ResponseEntity.ok(ApiSuccessResponse.success("Làm mới token thành công", authResponse));
    }

    /**
     * Đăng xuất: thu hồi Access Token (xóa khỏi Redis) và Refresh Token (đánh dấu revoke trong DB),
     * xóa cookie Refresh Token.
     *
     * @param authorizationHeader giá trị header Authorization ({@code Bearer <JWT>}) để lấy JTI thu hồi
     * @param httpRequest         servlet request để đọc cookie Refresh Token
     * @param httpResponse        servlet response để xóa cookie
     * @return {@link ApiSuccessResponse} rỗng báo đăng xuất thành công
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiSuccessResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        authService.logout(authorizationHeader, httpRequest, httpResponse);
        return ResponseEntity.ok(ApiSuccessResponse.success("Đăng xuất thành công", null));
    }
}

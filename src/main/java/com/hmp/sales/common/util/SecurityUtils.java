package com.hmp.sales.common.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Lớp tiện ích hỗ trợ trích xuất thông tin định danh người dùng (User ID) từ SecurityContext và Token xác thực.
 * <p>
 * Được sử dụng tập trung tại tầng Controller và các tầng nghiệp vụ thay thế việc lặp lại
 * phương thức {@code extractUserId} ở từng controller riêng biệt.
 */
public final class SecurityUtils {

    /**
     * Hàm khởi tạo riêng tư để ngăn chặn việc khởi tạo đối tượng tiện ích.
     *
     * @throws UnsupportedOperationException luôn luôn được ném nếu cố gắng khởi tạo
     */
    private SecurityUtils() {
        throw new UnsupportedOperationException("Lớp tiện ích không thể khởi tạo instance.");
    }

    /**
     * Trích xuất ID người dùng từ đối tượng {@link Jwt}.
     * <p>
     * Lấy giá trị từ {@code subject} claim của JWT và chuyển đổi sang dạng {@link Long}.
     *
     * @param jwt đối tượng JWT xác thực
     * @return ID người dùng dạng {@link Long}, hoặc {@code null} nếu {@code jwt} rỗng hoặc subject không hợp lệ
     */
    public static Long extractUserId(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Trích xuất ID người dùng từ đối tượng {@link Authentication}.
     * <p>
     * Hỗ trợ cả trường hợp Principal là {@link Jwt} hoặc lấy từ {@link Authentication#getName()}.
     *
     * @param authentication đối tượng xác thực người dùng trong Spring Security
     * @return ID người dùng dạng {@link Long}, hoặc {@code null} nếu chưa xác thực hoặc không hợp lệ
     */
    public static Long extractUserId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }

        // Trường hợp 1: Principal là Jwt token
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return extractUserId(jwt);
        }

        // Trường hợp 2: Lấy từ Authentication Name
        String name = authentication.getName();
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(name);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Trích xuất ID người dùng hiện tại đang đăng nhập từ {@link SecurityContextHolder}.
     *
     * @return ID người dùng dạng {@link Long}, hoặc {@code null} nếu không tìm thấy thông tin xác thực
     */
    public static Long extractCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return extractUserId(authentication);
    }
}


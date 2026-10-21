package com.hmp.sales.feature.auth.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Tiện ích băm mật mã dùng trong hệ thống xác thực.
 * <p>
 * Cung cấp phương thức băm SHA-256 để lưu trữ an toàn Refresh Token:
 * chỉ lưu giá trị băm (hash) vào PostgreSQL, không bao giờ lưu plain text.
 * <p>
 * Lưu ý: Không dùng lớp này để băm mật khẩu (dùng BCrypt thay thế).
 * SHA-256 chỉ phù hợp cho các giá trị ngẫu nhiên đủ entropy như raw refresh token
 * (UUID hoặc SecureRandom bytes).
 */
public final class HashUtils {

    private HashUtils() {
        // Utility class — không khởi tạo
    }

    /**
     * Băm chuỗi đầu vào bằng thuật toán SHA-256 và trả về kết quả dạng hex lowercase.
     * <p>
     * Kết quả là chuỗi hex 64 ký tự, phù hợp để lưu vào cột {@code token_hash VARCHAR(64)}.
     *
     * @param input chuỗi cần băm (ví dụ: raw refresh token UUID string)
     * @return chuỗi hex 64 ký tự của SHA-256 hash
     * @throws IllegalStateException nếu thuật toán SHA-256 không khả dụng (không xảy ra trên JVM chuẩn)
     */
    public static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 không khả dụng trên JVM hiện tại", e);
        }
    }
}

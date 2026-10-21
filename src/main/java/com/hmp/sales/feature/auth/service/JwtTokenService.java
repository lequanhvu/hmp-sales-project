package com.hmp.sales.feature.auth.service;

import com.hmp.sales.feature.user.entity.User;

import java.util.UUID;

/**
 * Hợp đồng dịch vụ cấp phát và quản lý vòng đời JWT Access Token.
 * <p>
 * Access Token là JWT chứa các claim tối giản: {@code sub} (userId), {@code email},
 * {@code roles}, {@code jti} (UUID), {@code iss}, {@code exp}.
 * TUYỆT ĐỐI không nhúng claim {@code permissions} vào token để tránh stale permissions
 * và giảm kích thước token — phân quyền chi tiết được xử lý động tại FilterChain.
 * <p>
 * Mỗi Access Token khi được cấp phát sẽ được đăng ký vào Redis với key
 * {@code auth:access:{jti}} và TTL bằng thời hạn token. Khi bị thu hồi,
 * bản ghi Redis sẽ bị xóa.
 */
public interface JwtTokenService {

    /**
     * Cấp phát JWT Access Token cho người dùng và đăng ký {@code auth:access:{jti}} vào Redis.
     *
     * @param user thực thể người dùng chứa thông tin cần nhúng vào token
     * @return chuỗi JWT đã ký bằng HMAC-SHA256
     */
    String generateAccessToken(User user);

    /**
     * Sinh UUID ngẫu nhiên để dùng làm JTI (JWT ID) cho token mới.
     * JTI đảm bảo mỗi token là duy nhất và có thể tra cứu riêng biệt trên Redis.
     *
     * @return UUID mới ngẫu nhiên
     */
    UUID generateJti();

    /**
     * Thu hồi Access Token bằng cách xóa bản ghi {@code auth:access:{jti}} trên Redis.
     * Thường được gọi khi người dùng đăng xuất hoặc phát hiện replay attack.
     *
     * @param jti UUID định danh JWT cần thu hồi
     */
    void revokeAccessToken(UUID jti);

    /**
     * Đọc claim {@code jti} từ chuỗi JWT đã được ký.
     * Dùng để lấy JTI từ Authorization header khi xử lý logout hoặc revoke.
     *
     * @param rawJwt chuỗi JWT Bearer Token
     * @return UUID của JTI claim
     * @throws com.hmp.sales.common.exception.UnauthorizedOperationException nếu JWT không hợp lệ hoặc thiếu JTI
     */
    UUID extractJti(String rawJwt);
}

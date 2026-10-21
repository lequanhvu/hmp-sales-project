package com.hmp.sales.feature.auth.service;

import com.hmp.sales.common.exception.UnauthorizedOperationException;
import com.hmp.sales.config.AppProperties;
import com.hmp.sales.feature.user.entity.User;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Triển khai dịch vụ JWT Access Token sử dụng Nimbus JOSE với thuật toán HS256.
 * <p>
 * Mỗi token chứa các claim tối giản: {@code sub}, {@code email}, {@code roles},
 * {@code jti}, {@code iss}, {@code iat}, {@code exp}.
 * TUYỆT ĐỐI không nhúng {@code permissions} vào token.
 * Sau khi cấp phát, token được đăng ký vào Redis với key {@code auth:access:{jti}}
 * và TTL bằng thời hạn token để hỗ trợ server-side revocation.
 */
@Service
public class JwtTokenServiceImpl implements JwtTokenService {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenServiceImpl.class);

    /** Tiền tố key Redis cho access token đang hoạt động. */
    public static final String REDIS_ACCESS_KEY_PREFIX = "auth:access:";

    private final AppProperties appProperties;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Khởi tạo service với cấu hình ứng dụng và Redis template.
     *
     * @param appProperties       cấu hình JWT (secret, issuer, expirationMinutes)
     * @param stringRedisTemplate Redis template dùng lưu access token JTI
     */
    public JwtTokenServiceImpl(AppProperties appProperties, StringRedisTemplate stringRedisTemplate) {
        this.appProperties = appProperties;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Claims được nhúng (tối giản, không có permissions):
     * <ul>
     *   <li>{@code sub}: userId dạng string</li>
     *   <li>{@code email}: email người dùng</li>
     *   <li>{@code roles}: danh sách vai trò (ví dụ: {@code ["ROLE_CUSTOMER"]})</li>
     *   <li>{@code jti}: UUID ngẫu nhiên</li>
     *   <li>{@code iss}: tên hệ thống phát hành</li>
     *   <li>{@code iat}: thời điểm phát hành</li>
     *   <li>{@code exp}: thời điểm hết hạn</li>
     * </ul>
     */
    @Override
    public String generateAccessToken(User user) {
        UUID jti = generateJti();
        Instant now = Instant.now();
        long expirationMinutes = appProperties.jwt().expirationMinutes();
        Instant expiresAt = now.plus(expirationMinutes, ChronoUnit.MINUTES);

        String rolesClaim = "ROLE_" + user.getRole().getRoleCode().name();

        try {
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(String.valueOf(user.getId()))
                    .claim("email", user.getEmail())
                    .claim("roles", List.of(rolesClaim))
                    .jwtID(jti.toString())
                    .issuer(appProperties.jwt().issuer())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expiresAt))
                    .build();

            byte[] secretBytes = appProperties.jwt().secret().getBytes(StandardCharsets.UTF_8);
            JWSSigner signer = new MACSigner(secretBytes);
            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
            signedJWT.sign(signer);

            String token = signedJWT.serialize();

            // Đăng ký vào Redis với TTL bằng thời hạn token
            String redisKey = REDIS_ACCESS_KEY_PREFIX + jti;
            stringRedisTemplate.opsForValue().set(redisKey, token, expirationMinutes, TimeUnit.MINUTES);

            return token;
        } catch (Exception e) {
            throw new UnauthorizedOperationException("Không thể tạo JWT access token: " + e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UUID generateJti() {
        return UUID.randomUUID();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void revokeAccessToken(UUID jti) {
        String redisKey = REDIS_ACCESS_KEY_PREFIX + jti;
        Boolean deleted = stringRedisTemplate.delete(redisKey);
        if (Boolean.TRUE.equals(deleted)) {
            log.debug("Đã thu hồi access token JTI={} trên Redis", jti);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UUID extractJti(String rawJwt) {
        try {
            byte[] secretBytes = appProperties.jwt().secret().getBytes(StandardCharsets.UTF_8);
            SignedJWT signedJWT = SignedJWT.parse(rawJwt);
            JWSVerifier verifier = new MACVerifier(secretBytes);
            if (!signedJWT.verify(verifier)) {
                throw new UnauthorizedOperationException("Chữ ký JWT không hợp lệ");
            }
            String jtiStr = signedJWT.getJWTClaimsSet().getJWTID();
            if (jtiStr == null || jtiStr.isBlank()) {
                throw new UnauthorizedOperationException("JWT thiếu claim jti");
            }
            return UUID.fromString(jtiStr);
        } catch (UnauthorizedOperationException e) {
            throw e;
        } catch (Exception e) {
            throw new UnauthorizedOperationException("Không thể đọc JTI từ JWT: " + e.getMessage(), e);
        }
    }
}

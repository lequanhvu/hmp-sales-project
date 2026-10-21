package com.hmp.sales.feature.auth.service;

import com.hmp.sales.common.exception.UnauthorizedOperationException;
import com.hmp.sales.config.AppProperties;
import com.hmp.sales.feature.auth.entity.RefreshToken;
import com.hmp.sales.feature.auth.repository.RefreshTokenRepository;
import com.hmp.sales.feature.auth.util.HashUtils;
import com.hmp.sales.feature.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Triển khai dịch vụ quản lý vòng đời Refresh Token.
 * <p>
 * Raw token là UUID ngẫu nhiên được gửi cho client qua cookie HttpOnly.
 * Hash SHA-256 của raw token được lưu vào PostgreSQL.
 * Xoay vòng nguyên tử: revoke token cũ trước, sau đó mới tạo token mới
 * trong cùng một transaction để đảm bảo tính nhất quán.
 */
@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenServiceImpl.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final AppProperties appProperties;

    /**
     * Khởi tạo service với các phụ thuộc cần thiết.
     *
     * @param refreshTokenRepository repository lưu trữ refresh token trong PostgreSQL
     * @param appProperties          cấu hình ứng dụng (refreshExpirationDays)
     */
    public RefreshTokenServiceImpl(
            RefreshTokenRepository refreshTokenRepository,
            AppProperties appProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.appProperties = appProperties;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Raw token = UUID ngẫu nhiên dạng string.
     * Sinh family ID mới cho phiên đăng nhập mới.
     */
    @Override
    @Transactional
    public RefreshTokenPair createRefreshToken(User user, UUID accessTokenJti) {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = HashUtils.sha256Hex(rawToken);
        UUID familyId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(appProperties.jwt().refreshExpirationDays(), ChronoUnit.DAYS);

        RefreshToken entity = RefreshToken.builder()
                .user(user)
                .tokenFamilyId(familyId)
                .tokenHash(tokenHash)
                .accessTokenJti(accessTokenJti)
                .expiresAt(expiresAt)
                .isRevoked(false)
                .isDeleted(false)
                .build();

        RefreshToken saved = refreshTokenRepository.save(entity);
        return new RefreshTokenPair(rawToken, saved);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Thứ tự thực hiện trong cùng transaction:
     * <ol>
     *   <li>Tìm token theo hash SHA-256.</li>
     *   <li>Kiểm tra hết hạn.</li>
     *   <li>Nếu đã revoke → phát hiện Replay Attack → thu hồi toàn bộ family → throw exception.</li>
     *   <li>Revoke token hiện tại.</li>
     *   <li>Tạo token mới với cùng {@code tokenFamilyId}.</li>
     * </ol>
     */
    @Override
    @Transactional
    public RefreshTokenPair rotateRefreshToken(String rawRefreshToken, UUID newAccessJti) {
        String tokenHash = HashUtils.sha256Hex(rawRefreshToken);

        RefreshToken existing = refreshTokenRepository.findByTokenHashAndIsDeletedFalse(tokenHash)
                .orElseThrow(() -> new UnauthorizedOperationException("Refresh token không hợp lệ hoặc không tồn tại"));

        // Kiểm tra hết hạn
        if (existing.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedOperationException("Refresh token đã hết hạn");
        }

        // Phát hiện Replay Attack: token đã bị revoke trước đó
        if (Boolean.TRUE.equals(existing.getIsRevoked())) {
            log.warn("Phát hiện Replay Attack! TokenFamilyId={} bị thu hồi toàn bộ.", existing.getTokenFamilyId());
            refreshTokenRepository.revokeAllByTokenFamilyId(existing.getTokenFamilyId());
            throw new UnauthorizedOperationException(
                    "Phát hiện tấn công tái sử dụng token. Toàn bộ phiên đăng nhập đã bị thu hồi.");
        }

        // Revoke token hiện tại
        existing.setIsRevoked(true);
        refreshTokenRepository.save(existing);

        // Tạo token mới trong cùng family
        String newRawToken = UUID.randomUUID().toString();
        String newHash = HashUtils.sha256Hex(newRawToken);
        Instant newExpiresAt = Instant.now().plus(appProperties.jwt().refreshExpirationDays(), ChronoUnit.DAYS);

        RefreshToken newToken = RefreshToken.builder()
                .user(existing.getUser())
                .tokenFamilyId(existing.getTokenFamilyId())
                .tokenHash(newHash)
                .accessTokenJti(newAccessJti)
                .expiresAt(newExpiresAt)
                .isRevoked(false)
                .isDeleted(false)
                .build();

        RefreshToken saved = refreshTokenRepository.save(newToken);
        return new RefreshTokenPair(newRawToken, saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void revokeRefreshToken(String rawRefreshToken) {
        String tokenHash = HashUtils.sha256Hex(rawRefreshToken);
        refreshTokenRepository.findByTokenHashAndIsDeletedFalse(tokenHash).ifPresent(token -> {
            token.setIsRevoked(true);
            refreshTokenRepository.save(token);
        });
    }
}

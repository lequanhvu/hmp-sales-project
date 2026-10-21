package com.hmp.sales.feature.auth.service;

import com.hmp.sales.feature.auth.entity.RefreshToken;
import com.hmp.sales.feature.user.entity.User;

import java.util.UUID;

/**
 * Hợp đồng dịch vụ quản lý vòng đời Refresh Token trong PostgreSQL.
 * <p>
 * Refresh Token là giá trị ngẫu nhiên opaque được lưu dưới dạng hash SHA-256.
 * Mỗi token thuộc một Token Family ({@code tokenFamilyId}) để phát hiện
 * tấn công tái sử dụng (Replay Attack): nếu token đã bị thu hồi được sử dụng lại,
 * toàn bộ family sẽ bị thu hồi ngay lập tức.
 */
public interface RefreshTokenService {

    /**
     * Tạo Refresh Token mới cho người dùng và lưu hash SHA-256 vào PostgreSQL.
     * <p>
     * Phát sinh một Token Family mới ({@code tokenFamilyId} mới) cho phiên đăng nhập mới.
     *
     * @param user             người dùng cần cấp Refresh Token
     * @param accessTokenJti   JTI của Access Token được cấp cùng lúc (để liên kết)
     * @return cặp giá trị gồm {@link RefreshTokenPair} chứa raw token và entity đã lưu
     */
    RefreshTokenPair createRefreshToken(User user, UUID accessTokenJti);

    /**
     * Xoay vòng (rotate) Refresh Token: thu hồi token hiện tại, tạo token mới
     * trong cùng Token Family và cấp Access Token mới.
     * <p>
     * Nếu token đã bị thu hồi trước đó (phát hiện Replay Attack), toàn bộ Family
     * sẽ bị thu hồi và ném {@link com.hmp.sales.common.exception.UnauthorizedOperationException}.
     *
     * @param rawRefreshToken  giá trị plain text của refresh token (từ cookie)
     * @param newAccessJti     JTI của Access Token mới được cấp trong lần refresh này
     * @return {@link RefreshTokenPair} chứa raw token mới và entity đã lưu
     * @throws com.hmp.sales.common.exception.UnauthorizedOperationException nếu token không hợp lệ,
     *         đã hết hạn, hoặc phát hiện replay attack
     */
    RefreshTokenPair rotateRefreshToken(String rawRefreshToken, UUID newAccessJti);

    /**
     * Thu hồi một Refresh Token cụ thể khi người dùng đăng xuất.
     *
     * @param rawRefreshToken giá trị plain text của refresh token cần thu hồi
     */
    void revokeRefreshToken(String rawRefreshToken);

    /**
     * Cặp kết quả trả về sau khi tạo hoặc xoay vòng Refresh Token.
     *
     * @param rawToken   giá trị plain text của refresh token (gửi về client qua cookie HttpOnly)
     * @param savedToken thực thể {@link RefreshToken} đã được lưu vào PostgreSQL
     */
    record RefreshTokenPair(String rawToken, RefreshToken savedToken) {}
}

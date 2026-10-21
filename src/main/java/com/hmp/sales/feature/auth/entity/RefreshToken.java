package com.hmp.sales.feature.auth.entity;

import com.hmp.sales.feature.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Thực thể đại diện cho Refresh Token được lưu trữ bền vững trong PostgreSQL.
 * <p>
 * Chỉ lưu chuỗi băm SHA-256 (hex 64 ký tự) của raw token — KHÔNG BAO GIỜ lưu giá trị
 * plain text của token vào cơ sở dữ liệu để đảm bảo an toàn khi bị lộ dữ liệu.
 * <p>
 * Hỗ trợ cơ chế Token Family để phát hiện tấn công tái sử dụng token (Replay Attack):
 * khi một token đã bị thu hồi ({@code isRevoked = true}) lại được sử dụng, toàn bộ
 * chuỗi token trong cùng {@code tokenFamilyId} sẽ bị thu hồi ngay lập tức.
 * <p>
 * Refresh token xoay vòng (rotation) theo mẫu:
 * <pre>
 *   R1 (active) &rarr; Revoke R1 &rarr; Tạo R2 (cùng tokenFamilyId, accessTokenJti mới)
 * </pre>
 */
@Entity
@Table(
    name = "refresh_tokens",
    indexes = {
        @Index(name = "idx_refresh_tokens_hash", columnList = "token_hash"),
        @Index(name = "idx_refresh_tokens_family", columnList = "token_family_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    /**
     * Khóa chính định danh Refresh Token (BIGINT IDENTITY).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refresh_token_id")
    private Long id;

    /**
     * Người dùng sở hữu Refresh Token này. Lazy fetch để tránh N+1.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Định danh chuỗi xoay vòng token (Token Family).
     * Tất cả các token trong một phiên đăng nhập dùng chung một UUID này.
     * Khi phát hiện replay attack, toàn bộ family bị thu hồi.
     */
    @Column(name = "token_family_id", nullable = false, columnDefinition = "uuid")
    private UUID tokenFamilyId;

    /**
     * Chuỗi băm SHA-256 (định dạng hex, 64 ký tự) của plain refresh token.
     * Không bao giờ lưu plain text token vào đây.
     */
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    /**
     * JTI (JWT ID) của access token được cấp phát cùng lúc với refresh token này.
     * Dùng để thu hồi access token trên Redis khi phát hiện replay attack.
     */
    @Column(name = "access_token_jti", nullable = false, columnDefinition = "uuid")
    private UUID accessTokenJti;

    /**
     * Thời điểm refresh token hết hạn (UTC).
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * Trạng thái thu hồi của token.
     * {@code false} = đang hoạt động; {@code true} = đã bị xoay hoặc bị hủy.
     */
    @Column(name = "is_revoked", nullable = false)
    @Builder.Default
    private Boolean isRevoked = false;

    /**
     * Thời điểm Refresh Token được tạo trong hệ thống (UTC).
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Cờ xóa mềm. Mặc định {@code false}.
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        if (this.isRevoked == null) this.isRevoked = false;
        if (this.isDeleted == null) this.isDeleted = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RefreshToken that = (RefreshToken) o;
        return Objects.equals(tokenHash, that.tokenHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tokenHash);
    }
}

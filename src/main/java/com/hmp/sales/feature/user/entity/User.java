package com.hmp.sales.feature.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;

/**
 * Thực thể đại diện cho người dùng trong toàn bộ hệ thống (User).
 * <p>
 * Lưu trữ tại bảng {@code users}. Hỗ trợ cả hai phương thức đăng nhập:
 * <ul>
 *     <li>Khách hàng đăng nhập qua Google OAuth (lưu {@code providerSubject}, không cần mật khẩu).</li>
 *     <li>Quản trị viên / Quản lý đăng nhập nội bộ (lưu mật khẩu băm {@code passwordHash}).</li>
 * </ul>
 */
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email", columnNames = {"email"}),
        @UniqueConstraint(name = "uk_users_auth_provider_subject", columnNames = {"auth_provider", "provider_subject"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /**
     * Khóa chính định danh tài khoản người dùng (BIGINT IDENTITY).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    /**
     * Họ và tên đầy đủ hiển thị của người dùng.
     */
    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    /**
     * Địa chỉ email của người dùng (duy nhất trong toàn hệ thống).
     */
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    /**
     * Số điện thoại liên hệ (có thể bổ sung khi khách hàng đặt hàng lần đầu).
     * Chiều dài 100 ký tự để chứa được hậu tố UUID khi tài khoản bị xóa mềm.
     */
    @Column(name = "phone", length = 100)
    private String phone;

    /**
     * Chuỗi mật khẩu đã được băm an toàn (BCrypt).
     * Để trống (null) đối với tài khoản khách hàng đăng nhập qua Google.
     */
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    /**
     * Nhà cung cấp phương thức xác thực (GOOGLE hoặc LOCAL).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false, length = 30)
    private AuthProvider authProvider;

    /**
     * Khóa định danh duy nhất của người dùng từ nhà cung cấp OAuth (Google Subject / 'sub' claim).
     */
    @Column(name = "provider_subject", length = 255)
    private String providerSubject;

    /**
     * Trạng thái hoạt động của tài khoản (ACTIVE, LOCKED).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    /**
     * Vai trò của người dùng trong hệ thống (ADMIN, MANAGER, CUSTOMER).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    /**
     * Cờ đánh dấu xóa mềm (soft delete). Mặc định là false.
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    /**
     * Thời điểm tài khoản được tạo trong hệ thống.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Thời điểm tài khoản được cập nhật lần cuối.
     */
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
        if (this.status == null) {
            this.status = UserStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email);
    }
}

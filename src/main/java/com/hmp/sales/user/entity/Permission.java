package com.hmp.sales.user.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Thực thể đại diện cho quyền hạn trong hệ thống (Permission).
 * <p>
 * Lưu trữ tại bảng {@code permissions}. Quản lý quyền hạn chi tiết theo đường dẫn API, phương thức HTTP,
 * và các quyền nghiệp vụ đặc thù (tiêu biểu như {@code PRODUCT_PRICE_VIEW}).
 */
@Entity
@Table(name = "permissions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_permissions_name", columnNames = {"permission_name"}),
        @UniqueConstraint(name = "uk_permissions_api_method", columnNames = {"api_path", "method"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    /**
     * Khóa chính định danh quyền hạn (INT IDENTITY).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Integer id;

    /**
     * Tên mã quyền hạn nghiệp vụ (ví dụ: "PRODUCT_PRICE_VIEW", "PRODUCT_CREATE", "ORDER_VIEW_OWN").
     */
    @Column(name = "permission_name", nullable = false, unique = true, length = 150)
    private String permissionName;

    /**
     * Đường dẫn API áp dụng quyền (ví dụ: "/api/v1/products/**").
     */
    @Column(name = "api_path", nullable = false, length = 255)
    private String apiPath;

    /**
     * Phương thức HTTP (GET, POST, PUT, DELETE, PATCH).
     */
    @Column(name = "method", nullable = false, length = 10)
    private String method;

    /**
     * Phân hệ nghiệp vụ tương ứng (ví dụ: "PRODUCT", "ORDER", "USER", "ROLE").
     */
    @Column(name = "module", nullable = false, length = 100)
    private String module;

    /**
     * Mô tả chi tiết ý nghĩa và phạm vi của quyền hạn.
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Cờ đánh dấu xóa mềm (soft delete). Mặc định là false.
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    /**
     * Thời điểm quyền hạn được tạo trong cơ sở dữ liệu.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Thời điểm quyền hạn được cập nhật lần cuối.
     */
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Danh sách liên kết phân quyền của vai trò có chứa quyền này.
     */
    @OneToMany(mappedBy = "permission", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<RolePermission> rolePermissions = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.isDeleted == null) {
            this.isDeleted = false;
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
        Permission that = (Permission) o;
        return Objects.equals(permissionName, that.permissionName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(permissionName);
    }
}

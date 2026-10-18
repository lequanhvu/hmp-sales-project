package com.hmp.sales.user.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Thực thể đại diện cho vai trò người dùng trong hệ thống (Role).
 * <p>
 * Lưu trữ tại bảng {@code roles}, chứa các vai trò chính như ADMIN, MANAGER,
 * CUSTOMER.
 */
@Entity
@Table(name = "roles", uniqueConstraints = {
        @UniqueConstraint(name = "uk_roles_role_code", columnNames = { "role_code" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    /**
     * Khóa chính định danh vai trò (INT IDENTITY).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Integer id;

    /**
     * Mã định danh vai trò (ADMIN, MANAGER, CUSTOMER).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role_code", nullable = false, unique = true, length = 30)
    private RoleCode roleCode;

    /**
     * Tên hiển thị của vai trò (ví dụ: "Quản trị viên", "Khách hàng").
     */
    @Column(name = "role_name", nullable = false, length = 100)
    private String roleName;

    /**
     * Mô tả chi tiết chức năng hoặc phạm vi của vai trò.
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
     * Thời điểm bản ghi vai trò được khởi tạo trong hệ thống.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Thời điểm bản ghi vai trò được cập nhật lần cuối.
     */
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Danh sách liên kết phân quyền gán cho vai trò này.
     */
    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
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
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Role role = (Role) o;
        return roleCode == role.roleCode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleCode);
    }
}

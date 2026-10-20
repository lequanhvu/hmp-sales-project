package com.hmp.sales.feature.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;

/**
 * Thực thể liên kết phân quyền giữa {@link Role} và {@link Permission}.
 * <p>
 * Lưu trữ tại bảng {@code role_permissions}. Cho phép Admin cấu hình linh hoạt
 * quyền hạn
 * của từng vai trò (đặc biệt là quyền xem giá {@code PRODUCT_PRICE_VIEW} của
 * Customer).
 */
@Entity
@Table(name = "role_permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermission {

    /**
     * Khóa chính kết hợp (RoleID, PermissionID).
     */
    @EmbeddedId
    @Builder.Default
    private RolePermissionId id = new RolePermissionId();

    /**
     * Vai trò được gán quyền.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roleId")
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    /**
     * Quyền hạn được gán cho vai trò.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("permissionId")
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

    /**
     * Cờ đánh dấu xóa mềm (soft delete). Mặc định là false.
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    /**
     * Thời điểm bản ghi phân quyền được tạo.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Thời điểm bản ghi phân quyền được cập nhật lần cuối.
     */
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Khởi tạo liên kết phân quyền giữa Role và Permission.
     *
     * @param role       vai trò cần gán quyền
     * @param permission quyền hạn được gán
     */
    public RolePermission(Role role, Permission permission) {
        this.role = role;
        this.permission = permission;
        this.id = new RolePermissionId(
                role != null ? role.getId() : null,
                permission != null ? permission.getId() : null);
        this.isDeleted = false;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
        if (this.id == null && this.role != null && this.permission != null) {
            this.id = new RolePermissionId(this.role.getId(), this.permission.getId());
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
        RolePermission that = (RolePermission) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

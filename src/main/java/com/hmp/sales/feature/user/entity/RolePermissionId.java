package com.hmp.sales.feature.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite Primary Key cho bảng liên kết {@link RolePermission} (RoleID, PermissionID).
 */
@Embeddable
@Getter
@Setter
public class RolePermissionId implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Khóa ngoại tham chiếu đến vai trò (Role).
     */
    @Column(name = "role_id")
    private Integer roleId;

    /**
     * Khóa ngoại tham chiếu đến quyền hạn (Permission).
     */
    @Column(name = "permission_id")
    private Integer permissionId;

    /**
     * Constructor mặc định không tham số cho JPA.
     */
    public RolePermissionId() {
    }

    /**
     * Constructor đầy đủ tham số cho RolePermissionId.
     *
     * @param roleId       ID của vai trò
     * @param permissionId ID của quyền hạn
     */
    public RolePermissionId(Integer roleId, Integer permissionId) {
        this.roleId = roleId;
        this.permissionId = permissionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RolePermissionId that = (RolePermissionId) o;
        return Objects.equals(roleId, that.roleId) && Objects.equals(permissionId, that.permissionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleId, permissionId);
    }
}

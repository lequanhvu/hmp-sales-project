package com.hmp.sales.feature.user.repository;

import com.hmp.sales.feature.user.entity.Permission;
import com.hmp.sales.feature.user.entity.Role;
import com.hmp.sales.feature.user.entity.RolePermission;
import com.hmp.sales.feature.user.entity.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository truy xuất dữ liệu thực thể liên kết phân quyền {@link RolePermission}.
 */
@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

    /**
     * Lấy danh sách liên kết phân quyền đang hoạt động của một vai trò.
     *
     * @param role thực thể vai trò
     * @return danh sách liên kết phân quyền chưa bị xóa
     */
    List<RolePermission> findByRoleAndIsDeletedFalse(Role role);

    /**
     * Lấy danh sách liên kết phân quyền đang hoạt động theo ID vai trò.
     *
     * @param roleId định danh vai trò
     * @return danh sách liên kết phân quyền chưa bị xóa
     */
    List<RolePermission> findByIdRoleIdAndIsDeletedFalse(Integer roleId);

    /**
     * Lấy toàn bộ danh sách liên kết phân quyền của một vai trò theo ID (bao gồm cả bản ghi bị xóa mềm).
     *
     * @param roleId định danh vai trò
     * @return danh sách liên kết phân quyền
     */
    List<RolePermission> findByIdRoleId(Integer roleId);

    /**
     * Tìm liên kết phân quyền cụ thể theo ID vai trò và ID quyền hạn.
     *
     * @param roleId định danh vai trò
     * @param permissionId định danh quyền hạn
     * @return {@link Optional} chứa bản ghi phân quyền nếu tìm thấy
     */
    Optional<RolePermission> findByIdRoleIdAndIdPermissionId(Integer roleId, Integer permissionId);

    /**
     * Truy vấn tất cả danh sách các quyền hạn được gán cho một vai trò cụ thể.
     *
     * @param roleId định danh vai trò
     * @return danh sách các thực thể quyền hạn đang hoạt động gán cho vai trò
     */
    @Query("SELECT rp.permission FROM RolePermission rp " +
            "WHERE rp.role.id = :roleId " +
            "AND rp.isDeleted = false " +
            "AND rp.permission.isDeleted = false")
    List<Permission> findPermissionsByRoleId(@Param("roleId") Integer roleId);

    /**
     * Kiểm tra xem vai trò có được gán quyền hạn cụ thể (theo tên quyền) hay không.
     *
     * @param roleId định danh vai trò
     * @param permissionName tên quyền hạn cần kiểm tra (ví dụ: "PRODUCT_PRICE_VIEW")
     * @return true nếu vai trò có quyền này và cả hai đều chưa bị xóa mềm
     */
    @Query("SELECT COUNT(rp) > 0 FROM RolePermission rp " +
            "WHERE rp.role.id = :roleId " +
            "AND rp.permission.permissionName = :permissionName " +
            "AND rp.isDeleted = false " +
            "AND rp.permission.isDeleted = false")
    boolean hasPermission(@Param("roleId") Integer roleId, @Param("permissionName") String permissionName);

    /**
     * Lấy toàn bộ danh sách liên kết phân quyền đang hoạt động kèm theo Role và Permission.
     * Sử dụng JOIN FETCH để eager load các thực thể liên quan, dùng nạp vào bộ nhớ RAM khi ứng dụng khởi động.
     *
     * @return danh sách các liên kết phân quyền hợp lệ chưa bị xóa mềm
     */
    @Query("SELECT rp FROM RolePermission rp " +
            "JOIN FETCH rp.role r " +
            "JOIN FETCH rp.permission p " +
            "WHERE rp.isDeleted = false " +
            "AND r.isDeleted = false " +
            "AND p.isDeleted = false")
    List<RolePermission> findAllActiveRolePermissions();
}

package com.hmp.sales.feature.user.repository;

import com.hmp.sales.feature.user.entity.Role;
import com.hmp.sales.feature.user.entity.RoleCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository truy xuất dữ liệu thực thể {@link Role}.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {

    /**
     * Tìm vai trò theo mã định danh vai trò (ADMIN, MANAGER, CUSTOMER).
     *
     * @param roleCode mã định danh vai trò
     * @return {@link Optional} chứa vai trò nếu tìm thấy
     */
    Optional<Role> findByRoleCode(RoleCode roleCode);

    /**
     * Tìm vai trò theo mã định danh vai trò và chưa bị xóa mềm.
     *
     * @param roleCode mã định danh vai trò
     * @return {@link Optional} chứa vai trò nếu tìm thấy và chưa bị xóa
     */
    Optional<Role> findByRoleCodeAndIsDeletedFalse(RoleCode roleCode);

    /**
     * Kiểm tra sự tồn tại của vai trò theo mã định danh và chưa bị xóa mềm.
     *
     * @param roleCode mã định danh vai trò
     * @return true nếu tồn tại, ngược lại false
     */
    boolean existsByRoleCodeAndIsDeletedFalse(RoleCode roleCode);

    /**
     * Lấy danh sách tất cả các vai trò chưa bị xóa mềm.
     *
     * @return danh sách vai trò khả dụng
     */
    List<Role> findAllByIsDeletedFalse();
}

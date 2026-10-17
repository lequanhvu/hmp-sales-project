package com.hmp.sales.user.repository;

import com.hmp.sales.user.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository truy xuất dữ liệu thực thể {@link Permission}.
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Integer> {

    /**
     * Tìm quyền hạn theo tên mã quyền (ví dụ: "PRODUCT_PRICE_VIEW").
     *
     * @param permissionName tên mã quyền hạn
     * @return {@link Optional} chứa quyền hạn nếu tìm thấy
     */
    Optional<Permission> findByPermissionName(String permissionName);

    /**
     * Tìm quyền hạn theo tên mã quyền và chưa bị xóa mềm.
     *
     * @param permissionName tên mã quyền hạn
     * @return {@link Optional} chứa quyền hạn nếu tìm thấy và chưa bị xóa
     */
    Optional<Permission> findByPermissionNameAndIsDeletedFalse(String permissionName);

    /**
     * Tìm quyền hạn theo đường dẫn API, phương thức HTTP và chưa bị xóa mềm.
     *
     * @param apiPath đường dẫn API
     * @param method phương thức HTTP (GET, POST, ...)
     * @return {@link Optional} chứa quyền hạn nếu tìm thấy
     */
    Optional<Permission> findByApiPathAndMethodAndIsDeletedFalse(String apiPath, String method);

    /**
     * Lấy danh sách tất cả các quyền hạn theo phân hệ nghiệp vụ (module).
     *
     * @param module tên phân hệ nghiệp vụ
     * @return danh sách quyền hạn thuộc phân hệ
     */
    List<Permission> findByModuleAndIsDeletedFalse(String module);

    /**
     * Lấy danh sách tất cả các quyền hạn chưa bị xóa mềm.
     *
     * @return danh sách quyền hạn khả dụng
     */
    List<Permission> findAllByIsDeletedFalse();

    /**
     * Lấy danh sách quyền hạn theo tập hợp tên mã quyền và chưa bị xóa mềm.
     *
     * @param permissionNames danh sách tên mã quyền cần tìm
     * @return danh sách quyền hạn tương ứng
     */
    List<Permission> findByPermissionNameInAndIsDeletedFalse(Collection<String> permissionNames);
}

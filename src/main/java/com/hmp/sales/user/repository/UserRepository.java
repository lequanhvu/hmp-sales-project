package com.hmp.sales.user.repository;

import com.hmp.sales.user.entity.AuthProvider;
import com.hmp.sales.user.entity.RoleCode;
import com.hmp.sales.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository truy xuất dữ liệu thực thể người dùng {@link User}.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    /**
     * Tìm người dùng theo địa chỉ email.
     *
     * @param email địa chỉ email người dùng
     * @return {@link Optional} chứa người dùng nếu tìm thấy
     */
    Optional<User> findByEmail(String email);

    /**
     * Tìm người dùng theo địa chỉ email và chưa bị xóa mềm.
     *
     * @param email địa chỉ email người dùng
     * @return {@link Optional} chứa người dùng nếu tìm thấy và chưa bị xóa
     */
    Optional<User> findByEmailAndIsDeletedFalse(String email);

    /**
     * Tìm người dùng theo nhà cung cấp OAuth và định danh Subject của nhà cung cấp.
     *
     * @param authProvider nhà cung cấp xác thực (GOOGLE)
     * @param providerSubject định danh người dùng từ Google
     * @return {@link Optional} chứa người dùng nếu tìm thấy và chưa bị xóa
     */
    Optional<User> findByAuthProviderAndProviderSubjectAndIsDeletedFalse(
            AuthProvider authProvider,
            String providerSubject
    );

    /**
     * Kiểm tra xem địa chỉ email đã tồn tại trong hệ thống hay chưa (chưa bị xóa mềm).
     *
     * @param email địa chỉ email cần kiểm tra
     * @return true nếu email đã tồn tại, ngược lại false
     */
    boolean existsByEmailAndIsDeletedFalse(String email);

    /**
     * Kiểm tra xem số điện thoại đã được đăng ký và chưa bị xóa mềm hay chưa.
     *
     * @param phone số điện thoại liên hệ
     * @return true nếu số điện thoại đã tồn tại, ngược lại false
     */
    boolean existsByPhoneAndIsDeletedFalse(String phone);

    /**
     * Lấy thông tin người dùng kèm theo vai trò (Eager fetch vai trò để tránh N+1 query).
     *
     * @param id định danh người dùng
     * @return {@link Optional} chứa người dùng kèm vai trò
     */
    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.id = :id AND u.isDeleted = false")
    Optional<User> findByIdWithRole(@Param("id") Long id);

    /**
     * Lấy thông tin người dùng kèm theo vai trò theo email (phục vụ đăng nhập/xác thực).
     *
     * @param email địa chỉ email người dùng
     * @return {@link Optional} chứa người dùng kèm vai trò
     */
    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.email = :email AND u.isDeleted = false")
    Optional<User> findByEmailWithRole(@Param("email") String email);

    /**
     * Lấy danh sách người dùng theo vai trò.
     *
     * @param roleCode mã vai trò (ADMIN, MANAGER, CUSTOMER)
     * @return danh sách người dùng thuộc vai trò đó
     */
    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.role.roleCode = :roleCode AND u.isDeleted = false")
    List<User> findByRoleCode(@Param("roleCode") RoleCode roleCode);
}

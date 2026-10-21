package com.hmp.sales.feature.auth.repository;

import com.hmp.sales.feature.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository truy xuất dữ liệu Refresh Token trong PostgreSQL.
 * <p>
 * Hỗ trợ tra cứu token theo hash SHA-256, thu hồi theo family ID phục vụ
 * cơ chế xoay vòng và phát hiện replay attack.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Tìm Refresh Token theo giá trị hash SHA-256 và chưa bị xóa mềm.
     *
     * @param tokenHash chuỗi hex SHA-256 (64 ký tự) của raw refresh token
     * @return {@link Optional} chứa token nếu tìm thấy
     */
    Optional<RefreshToken> findByTokenHashAndIsDeletedFalse(String tokenHash);

    /**
     * Thu hồi (revoke) toàn bộ Refresh Token trong cùng một Token Family.
     * Được gọi khi phát hiện tấn công replay attack (token đã revoke bị tái sử dụng).
     *
     * @param tokenFamilyId UUID định danh chuỗi token family cần thu hồi
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.isRevoked = true WHERE rt.tokenFamilyId = :familyId AND rt.isRevoked = false")
    void revokeAllByTokenFamilyId(@Param("familyId") UUID tokenFamilyId);

    /**
     * Đếm số lượng token chưa bị thu hồi trong một family.
     * Dùng để kiểm tra trạng thái family còn hoạt động hay không.
     *
     * @param tokenFamilyId UUID của token family
     * @return số lượng token còn active trong family
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.tokenFamilyId = :familyId AND rt.isRevoked = false AND rt.isDeleted = false")
    long countActiveByTokenFamilyId(@Param("familyId") UUID tokenFamilyId);
}

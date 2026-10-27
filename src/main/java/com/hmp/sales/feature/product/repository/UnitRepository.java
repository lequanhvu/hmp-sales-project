package com.hmp.sales.feature.product.repository;

import com.hmp.sales.feature.product.entity.Unit;
import com.hmp.sales.feature.product.entity.UnitStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository truy vấn dữ liệu cho thực thể {@link Unit}.
 * Cung cấp các phương thức tìm kiếm, lọc trạng thái và kiểm tra ràng buộc phụ thuộc bảng giá.
 */
public interface UnitRepository extends JpaRepository<Unit, Long> {

    /**
     * Tìm đơn vị tính theo ID và chưa bị xóa mềm.
     *
     * @param id ID đơn vị tính cần tìm
     * @return Optional chứa đơn vị tính nếu tồn tại và chưa xóa
     */
    Optional<Unit> findByIdAndIsDeletedFalse(Long id);

    /**
     * Tìm đơn vị tính theo tên và chưa bị xóa mềm.
     *
     * @param unitName tên đơn vị tính
     * @return Optional chứa đơn vị tính nếu tìm thấy
     */
    Optional<Unit> findByUnitNameAndIsDeletedFalse(String unitName);

    /**
     * Kiểm tra tên đơn vị tính đã tồn tại hay chưa.
     *
     * @param unitName tên đơn vị tính cần kiểm tra
     * @return true nếu tên đã tồn tại và chưa bị xóa
     */
    boolean existsByUnitNameAndIsDeletedFalse(String unitName);

    /**
     * Kiểm tra tên đơn vị tính đã tồn tại ngoại trừ một ID cụ thể (dùng khi update).
     *
     * @param unitName tên đơn vị tính cần kiểm tra
     * @param id       ID đơn vị tính cần loại trừ
     * @return true nếu tên đã tồn tại ở đơn vị khác
     */
    boolean existsByUnitNameAndIsDeletedFalseAndIdNot(String unitName, Long id);

    /**
     * Tìm kiếm đơn vị tính theo tên (không phân biệt hoa thường) với phân trang.
     *
     * @param keyword  từ khóa tìm theo tên (có thể null để lấy tất cả)
     * @param status   lọc theo trạng thái (có thể null để lấy tất cả)
     * @param pageable thông tin phân trang
     * @return trang đơn vị tính phù hợp
     */
    @Query("""
            SELECT u FROM Unit u
            WHERE u.isDeleted = false
              AND (:keyword IS NULL OR LOWER(u.unitName) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:status IS NULL OR u.status = :status)
            """)
    Page<Unit> searchUnits(@Param("keyword") String keyword,
                           @Param("status") UnitStatus status,
                           Pageable pageable);

    /**
     * Lấy danh sách đơn vị tính đang ACTIVE để hiển thị dropdown.
     *
     * @return danh sách đơn vị tính đang hoạt động
     */
    List<Unit> findByStatusAndIsDeletedFalseOrderByUnitNameAsc(UnitStatus status);

    /**
     * Kiểm tra còn bảng giá sản phẩm nào đang sử dụng đơn vị tính này hay không.
     *
     * @param unitId ID đơn vị tính cần kiểm tra
     * @return true nếu còn bảng giá liên kết
     */
    @Query("SELECT COUNT(pp) > 0 FROM ProductPrice pp WHERE pp.unit.id = :unitId AND pp.isDeleted = false")
    boolean hasActivePrices(@Param("unitId") Long unitId);
}

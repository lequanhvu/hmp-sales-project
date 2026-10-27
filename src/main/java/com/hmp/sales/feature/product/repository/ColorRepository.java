package com.hmp.sales.feature.product.repository;

import com.hmp.sales.feature.product.entity.Color;
import com.hmp.sales.feature.product.entity.ColorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository truy vấn dữ liệu cho thực thể {@link Color}.
 * Cung cấp các phương thức tìm kiếm, lọc trạng thái và kiểm tra ràng buộc phụ thuộc biến thể sản phẩm.
 */
public interface ColorRepository extends JpaRepository<Color, Long> {

    /**
     * Tìm màu sắc theo ID và chưa bị xóa mềm.
     *
     * @param id ID màu sắc cần tìm
     * @return Optional chứa màu sắc nếu tồn tại và chưa xóa
     */
    Optional<Color> findByIdAndIsDeletedFalse(Long id);

    /**
     * Tìm màu sắc theo tên và chưa bị xóa mềm.
     *
     * @param colorName tên màu sắc
     * @return Optional chứa màu sắc nếu tìm thấy
     */
    Optional<Color> findByColorNameAndIsDeletedFalse(String colorName);

    /**
     * Kiểm tra tên màu sắc đã tồn tại hay chưa.
     *
     * @param colorName tên màu sắc cần kiểm tra
     * @return true nếu tên đã tồn tại và chưa bị xóa
     */
    boolean existsByColorNameAndIsDeletedFalse(String colorName);

    /**
     * Kiểm tra tên màu sắc đã tồn tại ngoại trừ một ID cụ thể (dùng khi update).
     *
     * @param colorName tên màu sắc cần kiểm tra
     * @param id        ID màu sắc cần loại trừ
     * @return true nếu tên đã tồn tại ở màu khác
     */
    boolean existsByColorNameAndIsDeletedFalseAndIdNot(String colorName, Long id);

    /**
     * Tìm kiếm màu sắc theo tên (không phân biệt hoa thường) với phân trang.
     *
     * @param keyword  từ khóa tìm theo tên (có thể null để lấy tất cả)
     * @param status   lọc theo trạng thái (có thể null để lấy tất cả)
     * @param pageable thông tin phân trang
     * @return trang màu sắc phù hợp
     */
    @Query("""
            SELECT c FROM Color c
            WHERE c.isDeleted = false
              AND (:keyword IS NULL OR LOWER(c.colorName) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:status IS NULL OR c.status = :status)
            """)
    Page<Color> searchColors(@Param("keyword") String keyword,
                             @Param("status") ColorStatus status,
                             Pageable pageable);

    /**
     * Lấy danh sách màu sắc đang ACTIVE để hiển thị dropdown.
     *
     * @return danh sách màu sắc đang hoạt động
     */
    List<Color> findByStatusAndIsDeletedFalseOrderByColorNameAsc(ColorStatus status);

    /**
     * Kiểm tra còn biến thể sản phẩm nào đang liên kết với màu sắc này hay không.
     *
     * @param colorId ID màu sắc cần kiểm tra
     * @return true nếu còn biến thể liên kết
     */
    @Query("SELECT COUNT(pc) > 0 FROM ProductColor pc WHERE pc.color.id = :colorId AND pc.isDeleted = false")
    boolean hasActiveVariants(@Param("colorId") Long colorId);
}

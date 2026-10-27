package com.hmp.sales.feature.product.repository;

import com.hmp.sales.feature.product.entity.Brand;
import com.hmp.sales.feature.product.entity.BrandStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository truy vấn dữ liệu cho thực thể {@link Brand}.
 * Cung cấp các phương thức tìm kiếm theo tên, lọc trạng thái, và kiểm tra tên trùng.
 */
public interface BrandRepository extends JpaRepository<Brand, Long> {

    /**
     * Tìm thương hiệu theo ID và chưa bị xóa mềm.
     *
     * @param id ID thương hiệu cần tìm
     * @return Optional chứa thương hiệu nếu tồn tại và chưa xóa
     */
    Optional<Brand> findByIdAndIsDeletedFalse(Long id);

    /**
     * Tìm thương hiệu theo tên và chưa bị xóa mềm.
     *
     * @param brandName tên thương hiệu
     * @return Optional chứa thương hiệu nếu tìm thấy
     */
    Optional<Brand> findByBrandNameAndIsDeletedFalse(String brandName);

    /**
     * Kiểm tra tên thương hiệu đã tồn tại hay chưa (case-sensitive).
     *
     * @param brandName tên thương hiệu cần kiểm tra
     * @return true nếu tên đã tồn tại và chưa bị xóa
     */
    boolean existsByBrandNameAndIsDeletedFalse(String brandName);

    /**
     * Kiểm tra tên thương hiệu đã tồn tại ngoại trừ một ID cụ thể (dùng khi update).
     *
     * @param brandName tên thương hiệu cần kiểm tra
     * @param id        ID thương hiệu cần loại trừ
     * @return true nếu tên đã tồn tại ở thương hiệu khác
     */
    boolean existsByBrandNameAndIsDeletedFalseAndIdNot(String brandName, Long id);

    /**
     * Tìm kiếm thương hiệu theo tên (không phân biệt hoa thường) với phân trang.
     * Chỉ trả về thương hiệu chưa bị xóa.
     *
     * @param keyword  từ khóa tìm theo tên (có thể null để lấy tất cả)
     * @param status   lọc theo trạng thái (có thể null để lấy tất cả)
     * @param pageable thông tin phân trang và sắp xếp
     * @return trang thương hiệu phù hợp
     */
    @Query("""
            SELECT b FROM Brand b
            WHERE b.isDeleted = false
              AND (:keyword IS NULL OR LOWER(b.brandName) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:status IS NULL OR b.status = :status)
            """)
    Page<Brand> searchBrands(@Param("keyword") String keyword,
                             @Param("status") BrandStatus status,
                             Pageable pageable);

    /**
     * Lấy danh sách thương hiệu đang ACTIVE để hiển thị dropdown công khai.
     *
     * @return danh sách thương hiệu đang hoạt động
     */
    List<Brand> findByStatusAndIsDeletedFalseOrderByBrandNameAsc(BrandStatus status);

    /**
     * Kiểm tra còn sản phẩm nào đang liên kết với thương hiệu này hay không.
     *
     * @param brandId ID thương hiệu cần kiểm tra
     * @return true nếu còn sản phẩm liên kết (chưa bị xóa)
     */
    @Query("SELECT COUNT(p) > 0 FROM Product p WHERE p.brand.id = :brandId AND p.isDeleted = false")
    boolean hasActiveProducts(@Param("brandId") Long brandId);
}

package com.hmp.sales.feature.product.repository;

import com.hmp.sales.feature.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Repository truy vấn dữ liệu cho thực thể {@link ProductImage}.
 * Hỗ trợ kiểm tra ràng buộc tối đa 11 ảnh (1 chính + 10 phụ) cho mỗi sản phẩm.
 */
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    /**
     * Tìm ảnh theo ID và chưa bị xóa mềm.
     *
     * @param id ID ảnh cần tìm
     * @return Optional chứa ảnh nếu tồn tại và chưa xóa
     */
    Optional<ProductImage> findByIdAndIsDeletedFalse(Long id);

    /**
     * Lấy danh sách ảnh chưa xóa của một sản phẩm, sắp xếp theo thứ tự hiển thị.
     *
     * @param productId ID sản phẩm
     * @return danh sách ảnh sắp xếp theo displayOrder
     */
    List<ProductImage> findByProductIdAndIsDeletedFalseOrderByDisplayOrderAsc(Long productId);

    /**
     * Đếm tổng số ảnh chưa xóa của một sản phẩm.
     *
     * @param productId ID sản phẩm
     * @return tổng số ảnh
     */
    long countByProductIdAndIsDeletedFalse(Long productId);

    /**
     * Đếm số ảnh chính (isPrimary=true) chưa xóa của một sản phẩm.
     *
     * @param productId ID sản phẩm
     * @return số ảnh chính (nên là 0 hoặc 1)
     */
    long countByProductIdAndIsPrimaryTrueAndIsDeletedFalse(Long productId);

    /**
     * Đếm số ảnh phụ (isPrimary=false) chưa xóa của một sản phẩm.
     *
     * @param productId ID sản phẩm
     * @return số ảnh phụ (tối đa 10)
     */
    long countByProductIdAndIsPrimaryFalseAndIsDeletedFalse(Long productId);

    /**
     * Soft-delete tất cả ảnh của một sản phẩm (dùng khi xóa mềm sản phẩm).
     *
     * @param productId ID sản phẩm cần xóa mềm toàn bộ ảnh
     */
    @Modifying
    @Transactional
    @Query("UPDATE ProductImage pi SET pi.isDeleted = true WHERE pi.product.id = :productId")
    void softDeleteByProductId(@Param("productId") Long productId);
}

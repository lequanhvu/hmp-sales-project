package com.hmp.sales.feature.product.repository;

import com.hmp.sales.feature.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Repository truy vấn dữ liệu cho thực thể {@link ProductVariant}.
 */
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    /**
     * Tìm biến thể theo ID và chưa bị xóa mềm.
     *
     * @param id ID biến thể cần tìm
     * @return Optional chứa biến thể nếu tồn tại và chưa xóa
     */
    Optional<ProductVariant> findByIdAndIsDeletedFalse(Long id);

    /**
     * Lấy danh sách biến thể chưa xóa của một sản phẩm.
     *
     * @param productId ID sản phẩm
     * @return danh sách biến thể
     */
    List<ProductVariant> findByProductIdAndIsDeletedFalse(Long productId);

    /**
     * Soft-delete tất cả biến thể của một sản phẩm (dùng khi xóa mềm sản phẩm).
     *
     * @param productId ID sản phẩm cần xóa mềm toàn bộ biến thể
     */
    @Modifying
    @Transactional
    @Query("UPDATE ProductVariant pv SET pv.isDeleted = true WHERE pv.product.id = :productId")
    void softDeleteByProductId(@Param("productId") Long productId);
}

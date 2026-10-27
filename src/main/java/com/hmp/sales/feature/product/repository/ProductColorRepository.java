package com.hmp.sales.feature.product.repository;

import com.hmp.sales.feature.product.entity.ProductColor;
import com.hmp.sales.feature.product.entity.ProductColorId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Repository truy vấn dữ liệu cho thực thể {@link ProductColor}.
 * Khóa chính phức hợp {@code (variant_id, color_id)}.
 */
public interface ProductColorRepository extends JpaRepository<ProductColor, ProductColorId> {

    /**
     * Lấy danh sách màu sắc chưa xóa của một biến thể sản phẩm.
     *
     * @param variantId ID biến thể sản phẩm
     * @return danh sách liên kết màu sắc của biến thể
     */
    List<ProductColor> findByVariantIdAndIsDeletedFalse(Long variantId);

    /**
     * Soft-delete tất cả màu sắc liên kết của một biến thể (dùng khi xóa mềm biến thể).
     *
     * @param variantId ID biến thể cần xóa mềm toàn bộ màu
     */
    @Modifying
    @Transactional
    @Query("UPDATE ProductColor pc SET pc.isDeleted = true WHERE pc.variant.id = :variantId")
    void softDeleteByVariantId(@Param("variantId") Long variantId);
}

package com.hmp.sales.feature.product.dto;

import com.hmp.sales.feature.product.entity.ProductImage;

/**
 * Response DTO chứa thông tin ảnh của sản phẩm.
 *
 * @param id           ID ảnh
 * @param imageUrl     URL ảnh trên cloud/object storage
 * @param isPrimary    true nếu đây là ảnh đại diện (primary)
 * @param displayOrder thứ tự hiển thị
 */
public record ProductImageResponse(
        Long id,
        String imageUrl,
        Boolean isPrimary,
        Integer displayOrder
) {

    /**
     * Chuyển đổi từ entity {@link ProductImage} sang DTO {@link ProductImageResponse}.
     *
     * @param image entity ảnh cần chuyển đổi
     * @return ProductImageResponse đã được điền dữ liệu
     */
    public static ProductImageResponse fromEntity(ProductImage image) {
        return new ProductImageResponse(
                image.getId(),
                image.getImageUrl(),
                image.getIsPrimary(),
                image.getDisplayOrder()
        );
    }
}

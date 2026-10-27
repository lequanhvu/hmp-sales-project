package com.hmp.sales.feature.product.dto;

import com.hmp.sales.feature.product.entity.Product;
import com.hmp.sales.feature.product.entity.ProductStatus;

/**
 * Response DTO gọn dùng cho danh sách tìm kiếm sản phẩm.
 * Không bao gồm danh sách biến thể và bảng giá chi tiết để giảm kích thước response.
 *
 * @param id             ID sản phẩm
 * @param productName    tên sản phẩm
 * @param status         trạng thái sản phẩm
 * @param categoryId     ID danh mục
 * @param categoryName   tên danh mục
 * @param brandId        ID thương hiệu (null nếu chưa gán)
 * @param brandName      tên thương hiệu (null nếu chưa gán)
 * @param primaryImageUrl URL ảnh đại diện (null nếu chưa có ảnh)
 * @param priceVisible   true nếu caller có quyền PRODUCT_PRICE_VIEW
 */
public record ProductSummaryResponse(
        Long id,
        String productName,
        ProductStatus status,
        Long categoryId,
        String categoryName,
        Long brandId,
        String brandName,
        String primaryImageUrl,
        boolean priceVisible
) {

    /**
     * Chuyển đổi từ entity {@link Product} sang DTO tóm tắt.
     *
     * @param product      entity sản phẩm
     * @param priceVisible true nếu caller có quyền xem giá
     * @return ProductSummaryResponse đã được điền dữ liệu
     */
    public static ProductSummaryResponse fromEntity(Product product, boolean priceVisible) {
        String primaryImageUrl = product.getImages().stream()
                .filter(img -> !img.getIsDeleted() && img.getIsPrimary())
                .map(img -> img.getImageUrl())
                .findFirst()
                .orElse(null);

        return new ProductSummaryResponse(
                product.getId(),
                product.getProductName(),
                product.getStatus(),
                product.getCategory() != null ? product.getCategory().getId() : null,
                product.getCategory() != null ? product.getCategory().getCategoryName() : null,
                product.getBrand() != null ? product.getBrand().getId() : null,
                product.getBrand() != null ? product.getBrand().getBrandName() : null,
                primaryImageUrl,
                priceVisible
        );
    }
}

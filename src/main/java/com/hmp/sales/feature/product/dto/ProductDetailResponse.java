package com.hmp.sales.feature.product.dto;

import com.hmp.sales.feature.product.entity.Product;
import com.hmp.sales.feature.product.entity.ProductStatus;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Response DTO chi tiết sản phẩm kèm đầy đủ ảnh và biến thể.
 * Giá trong biến thể sẽ bị che giấu (null) nếu caller thiếu quyền {@code PRODUCT_PRICE_VIEW}.
 *
 * @param id           ID sản phẩm
 * @param productName  tên sản phẩm
 * @param description  mô tả chi tiết
 * @param status       trạng thái sản phẩm
 * @param categoryId   ID danh mục
 * @param categoryName tên danh mục
 * @param brandId      ID thương hiệu
 * @param brandName    tên thương hiệu
 * @param priceVisible true nếu caller có quyền PRODUCT_PRICE_VIEW
 * @param images       danh sách ảnh sản phẩm
 * @param variants     danh sách biến thể kèm giá (có thể null nếu thiếu quyền)
 * @param createdAt    thời điểm tạo
 * @param updatedAt    thời điểm cập nhật lần cuối
 */
public record ProductDetailResponse(
        Long id,
        String productName,
        String description,
        ProductStatus status,
        Long categoryId,
        String categoryName,
        Long brandId,
        String brandName,
        boolean priceVisible,
        List<ProductImageResponse> images,
        List<ProductVariantResponse> variants,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Chuyển đổi từ entity {@link Product} sang DTO chi tiết đầy đủ.
     *
     * @param product      entity sản phẩm (với images và variants đã load)
     * @param priceVisible true nếu caller có quyền xem giá
     * @return ProductDetailResponse đã được điền dữ liệu
     */
    public static ProductDetailResponse fromEntity(Product product, boolean priceVisible) {
        List<ProductImageResponse> images = product.getImages().stream()
                .filter(img -> !img.getIsDeleted())
                .map(ProductImageResponse::fromEntity)
                .collect(Collectors.toList());

        List<ProductVariantResponse> variants = product.getVariants().stream()
                .filter(v -> !v.getIsDeleted())
                .map(v -> ProductVariantResponse.fromEntity(v, priceVisible))
                .collect(Collectors.toList());

        return new ProductDetailResponse(
                product.getId(),
                product.getProductName(),
                product.getDescription(),
                product.getStatus(),
                product.getCategory() != null ? product.getCategory().getId() : null,
                product.getCategory() != null ? product.getCategory().getCategoryName() : null,
                product.getBrand() != null ? product.getBrand().getId() : null,
                product.getBrand() != null ? product.getBrand().getBrandName() : null,
                priceVisible,
                images,
                variants,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}

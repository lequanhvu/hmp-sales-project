package com.hmp.sales.feature.product.dto;

import com.hmp.sales.feature.product.entity.ProductVariant;
import com.hmp.sales.feature.product.entity.VariantStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Response DTO chứa thông tin chi tiết một biến thể sản phẩm kèm danh sách màu sắc
 * và bảng giá theo từng đơn vị tính (có thể null nếu caller thiếu quyền {@code PRODUCT_PRICE_VIEW}).
 *
 * @param id          ID biến thể
 * @param variantCode mã biến thể nội bộ
 * @param variantName tên biến thể hiển thị
 * @param typeValue   loại sóng/loại sản phẩm
 * @param thickness   độ dày tôn (mm)
 * @param status      trạng thái biến thể
 * @param colors      danh sách tên màu sắc liên kết
 * @param prices      bảng giá theo đơn vị tính (null khi thiếu quyền PRODUCT_PRICE_VIEW)
 */
public record ProductVariantResponse(
        Long id,
        String variantCode,
        String variantName,
        String typeValue,
        BigDecimal thickness,
        VariantStatus status,
        List<String> colors,
        Map<String, Integer> prices
) {

    /**
     * Chuyển đổi từ entity {@link ProductVariant} sang DTO với giá đã được kiểm soát quyền.
     *
     * @param variant      entity biến thể
     * @param priceVisible true nếu caller có quyền PRODUCT_PRICE_VIEW
     * @return ProductVariantResponse với giá được che giấu nếu thiếu quyền
     */
    public static ProductVariantResponse fromEntity(ProductVariant variant, boolean priceVisible) {
        List<String> colorNames = variant.getColors().stream()
                .filter(pc -> !pc.getIsDeleted())
                .map(pc -> pc.getColor().getColorName())
                .collect(Collectors.toList());

        Map<String, Integer> priceMap = null;
        if (priceVisible) {
            priceMap = variant.getPrices().stream()
                    .filter(pp -> !pp.getIsDeleted())
                    .collect(Collectors.toMap(
                            pp -> pp.getUnit().getUnitName(),
                            pp -> pp.getPrice()
                    ));
        }

        return new ProductVariantResponse(
                variant.getId(),
                variant.getVariantCode(),
                variant.getVariantName(),
                variant.getTypeValue(),
                variant.getThickness(),
                variant.getStatus(),
                colorNames,
                priceMap
        );
    }
}

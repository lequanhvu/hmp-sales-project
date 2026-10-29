package com.hmp.sales.feature.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Request DTO để tạo mới hoặc cập nhật biến thể sản phẩm.
 *
 * @param variantCode mã biến thể nội bộ (tùy chọn, tối đa 80 ký tự)
 * @param variantName tên biến thể hiển thị (bắt buộc, tối đa 150 ký tự)
 * @param typeValue   loại sóng/loại tôn (tùy chọn)
 * @param thickness   độ dày tôn tính theo mm (tùy chọn)
 * @param colorIds    danh sách ID màu sắc liên kết (tùy chọn)
 * @param prices      bảng giá theo đơn vị tính: Map từ unitId → price (VND integer)
 */
public record ProductVariantRequest(

        @Size(max = 80, message = "Mã biến thể không được vượt quá 80 ký tự.")
        String variantCode,

        @NotBlank(message = "Tên biến thể không được để trống.")
        @Size(max = 150, message = "Tên biến thể không được vượt quá 150 ký tự.")
        String variantName,

        String typeValue,

        // độ dày
        BigDecimal thickness, 

        List<Long> colorIds,

        Map<Long, Integer> prices
) {}

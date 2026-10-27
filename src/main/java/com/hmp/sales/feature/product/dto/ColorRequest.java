package com.hmp.sales.feature.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO để tạo mới hoặc cập nhật màu sắc tôn sản xuất.
 *
 * @param colorName tên màu sắc, bắt buộc, tối đa 100 ký tự (vd: Xanh ngọc, Đỏ đậm)
 */
public record ColorRequest(

        @NotBlank(message = "Tên màu sắc không được để trống.")
        @Size(max = 100, message = "Tên màu sắc không được vượt quá 100 ký tự.")
        String colorName
) {}

package com.hmp.sales.feature.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO để tạo mới hoặc cập nhật thương hiệu sản phẩm.
 *
 * @param brandName tên thương hiệu, bắt buộc, tối đa 150 ký tự
 */
public record BrandRequest(

        @NotBlank(message = "Tên thương hiệu không được để trống.")
        @Size(max = 150, message = "Tên thương hiệu không được vượt quá 150 ký tự.")
        String brandName
) {}

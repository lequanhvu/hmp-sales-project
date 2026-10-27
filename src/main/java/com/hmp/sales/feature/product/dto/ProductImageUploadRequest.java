package com.hmp.sales.feature.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Request DTO để thêm ảnh cho sản phẩm.
 *
 * @param imageUrl     URL ảnh trên cloud/object storage (bắt buộc, tối đa 1000 ký tự)
 * @param isPrimary    true nếu đây là ảnh đại diện (primary)
 * @param displayOrder thứ tự hiển thị (mặc định 0)
 */
public record ProductImageUploadRequest(

        @NotBlank(message = "URL ảnh không được để trống.")
        @Size(max = 1000, message = "URL ảnh không được vượt quá 1000 ký tự.")
        String imageUrl,

        @NotNull(message = "Phải chỉ định loại ảnh (isPrimary).")
        Boolean isPrimary,

        @Min(value = 0, message = "Thứ tự hiển thị phải >= 0.")
        Integer displayOrder
) {}

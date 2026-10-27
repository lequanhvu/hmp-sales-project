package com.hmp.sales.feature.product.dto;

import jakarta.validation.constraints.Size;

/**
 * Request DTO để cập nhật thông tin cơ bản của sản phẩm.
 * Tất cả trường đều là tùy chọn (null = không thay đổi).
 *
 * @param productName tên sản phẩm mới (tùy chọn, tối đa 250 ký tự)
 * @param description mô tả mới (tùy chọn)
 * @param brandId     ID thương hiệu mới (tùy chọn, null để giữ nguyên)
 * @param categoryId  ID danh mục mới để chuyển sản phẩm sang (tùy chọn)
 */
public record ProductUpdateRequest(

        @Size(max = 250, message = "Tên sản phẩm không được vượt quá 250 ký tự.")
        String productName,

        String description,

        Long brandId,

        Long categoryId
) {}

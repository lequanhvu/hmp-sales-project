package com.hmp.sales.feature.category.dto;

import jakarta.validation.constraints.Size;

/**
 * Request DTO để cập nhật thông tin danh mục sản phẩm.
 * Tất cả trường đều là tùy chọn (null = không thay đổi).
 *
 * @param categoryName tên danh mục mới (tùy chọn, tối đa 150 ký tự)
 */
public record CategoryUpdateRequest(

        @Size(max = 150, message = "Tên danh mục không được vượt quá 150 ký tự.")
        String categoryName
) {}

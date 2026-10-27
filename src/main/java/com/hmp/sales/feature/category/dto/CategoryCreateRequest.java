package com.hmp.sales.feature.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO để tạo mới một danh mục sản phẩm.
 * Dùng cho cả tạo danh mục root ({@code POST /api/v1/manager/categories})
 * và tạo danh mục con ({@code POST /api/v1/manager/categories/{parentId}/children}).
 *
 * @param categoryName tên danh mục, bắt buộc, tối đa 150 ký tự
 */
public record CategoryCreateRequest(

        @NotBlank(message = "Tên danh mục không được để trống.")
        @Size(max = 150, message = "Tên danh mục không được vượt quá 150 ký tự.")
        String categoryName
) {}

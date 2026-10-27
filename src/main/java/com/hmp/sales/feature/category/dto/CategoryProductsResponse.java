package com.hmp.sales.feature.category.dto;

import com.hmp.sales.feature.product.dto.ProductSummaryResponse;

import java.util.List;

/**
 * Response DTO cho danh sách sản phẩm thuộc một danh mục cụ thể.
 *
 * @param categoryId   ID danh mục
 * @param categoryName tên danh mục
 * @param products     danh sách sản phẩm tóm tắt trực thuộc danh mục
 */
public record CategoryProductsResponse(
        Long categoryId,
        String categoryName,
        List<ProductSummaryResponse> products
) {
}

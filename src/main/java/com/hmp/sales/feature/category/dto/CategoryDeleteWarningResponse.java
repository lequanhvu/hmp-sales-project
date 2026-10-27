package com.hmp.sales.feature.category.dto;

/**
 * Response DTO trả về khi thực hiện xóa danh mục chứa danh mục con hoặc sản phẩm
 * mà chưa có cờ {@code force=true}.
 * <p>
 * Frontend sẽ dùng thông tin này để hiển thị popup xác nhận cảnh báo với số lượng bị ảnh hưởng.
 *
 * @param categoryId        ID danh mục muốn xóa
 * @param categoryName      tên danh mục muốn xóa
 * @param childCategoryCount số lượng danh mục con bị ảnh hưởng (đệ quy)
 * @param productCount      số lượng sản phẩm bị ảnh hưởng (trong cây con)
 * @param totalImpactCount  tổng số lượng danh mục + sản phẩm bị ảnh hưởng
 */
public record CategoryDeleteWarningResponse(
        Long categoryId,
        String categoryName,
        long childCategoryCount,
        long productCount,
        long totalImpactCount
) {}

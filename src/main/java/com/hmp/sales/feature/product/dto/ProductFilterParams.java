package com.hmp.sales.feature.product.dto;

import com.hmp.sales.feature.product.entity.ProductStatus;

/**
 * Các tham số lọc, sắp xếp và phân trang cho endpoint tìm kiếm sản phẩm công khai.
 *
 * @param productName   lọc theo tên sản phẩm (tìm kiếm mờ, có thể null)
 * @param categoryName  lọc theo tên danh mục (tìm kiếm mờ, có thể null)
 * @param status        lọc theo trạng thái sản phẩm (có thể null để lấy tất cả)
 * @param sortBy        trường sắp xếp (productName, createdAt; không cho phép sortBy=price khi thiếu quyền)
 * @param sortDirection hướng sắp xếp: ASC hoặc DESC (mặc định ASC)
 * @param page          chỉ số trang (bắt đầu từ 0)
 * @param size          số bản ghi trên một trang (mặc định 20)
 */
public record ProductFilterParams(
        String productName,
        String categoryName,
        ProductStatus status,
        String sortBy,
        String sortDirection,
        int page,
        int size
) {}

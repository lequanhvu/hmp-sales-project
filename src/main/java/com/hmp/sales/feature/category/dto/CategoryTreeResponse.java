package com.hmp.sales.feature.category.dto;

import com.hmp.sales.feature.category.entity.Category;
import com.hmp.sales.feature.category.entity.CategoryStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO đại diện cho một node trong cây danh mục phân cấp n-cấp.
 * Mỗi node chứa danh sách các node con ({@code children}) dạng lồng nhau.
 *
 * @param id           ID danh mục
 * @param categoryName tên danh mục
 * @param status       trạng thái hoạt động
 * @param children     danh sách danh mục con trực tiếp (lồng nhau)
 */
public record CategoryTreeResponse(
        Long id,
        String categoryName,
        CategoryStatus status,
        List<CategoryTreeResponse> children
) {

    /**
     * Chuyển đổi từ entity {@link Category} sang node cây với danh sách con rỗng.
     * Children sẽ được thêm vào sau khi dựng cây in-memory.
     *
     * @param category entity danh mục cần chuyển đổi
     * @return CategoryTreeResponse với children rỗng
     */
    public static CategoryTreeResponse fromEntity(Category category) {
        return new CategoryTreeResponse(
                category.getId(),
                category.getCategoryName(),
                category.getStatus(),
                new ArrayList<>()
        );
    }
}

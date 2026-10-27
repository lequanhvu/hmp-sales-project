package com.hmp.sales.feature.category.dto;

import com.hmp.sales.feature.category.entity.Category;
import com.hmp.sales.feature.category.entity.CategoryStatus;

import java.time.Instant;

/**
 * Response DTO chứa thông tin cơ bản của một danh mục sản phẩm.
 *
 * @param id             ID danh mục
 * @param categoryName   tên danh mục
 * @param status         trạng thái hoạt động
 * @param parentId       ID danh mục cha (null nếu là root)
 * @param createdAt      thời điểm tạo
 * @param updatedAt      thời điểm cập nhật lần cuối
 */
public record CategoryResponse(
        Long id,
        String categoryName,
        CategoryStatus status,
        Long parentId,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Chuyển đổi từ entity {@link Category} sang DTO {@link CategoryResponse}.
     *
     * @param category entity danh mục cần chuyển đổi
     * @return DTO CategoryResponse đã được điền dữ liệu
     */
    public static CategoryResponse fromEntity(Category category) {
        Long parentId = category.getParentCategory() != null
                ? category.getParentCategory().getId()
                : null;
        return new CategoryResponse(
                category.getId(),
                category.getCategoryName(),
                category.getStatus(),
                parentId,
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}

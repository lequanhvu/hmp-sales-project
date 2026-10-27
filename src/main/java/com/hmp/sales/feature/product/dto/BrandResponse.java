package com.hmp.sales.feature.product.dto;

import com.hmp.sales.feature.product.entity.Brand;
import com.hmp.sales.feature.product.entity.BrandStatus;

import java.time.Instant;

/**
 * Response DTO chứa thông tin thương hiệu sản phẩm.
 *
 * @param id        ID thương hiệu
 * @param brandName tên thương hiệu
 * @param status    trạng thái hoạt động (ACTIVE/LOCKED)
 * @param createdAt thời điểm tạo
 * @param updatedAt thời điểm cập nhật lần cuối
 */
public record BrandResponse(
        Long id,
        String brandName,
        BrandStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Chuyển đổi từ entity {@link Brand} sang DTO {@link BrandResponse}.
     *
     * @param brand entity thương hiệu cần chuyển đổi
     * @return DTO BrandResponse đã được điền dữ liệu
     */
    public static BrandResponse fromEntity(Brand brand) {
        return new BrandResponse(
                brand.getId(),
                brand.getBrandName(),
                brand.getStatus(),
                brand.getCreatedAt(),
                brand.getUpdatedAt()
        );
    }
}

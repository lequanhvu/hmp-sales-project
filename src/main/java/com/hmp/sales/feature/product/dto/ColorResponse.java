package com.hmp.sales.feature.product.dto;

import com.hmp.sales.feature.product.entity.Color;
import com.hmp.sales.feature.product.entity.ColorStatus;

import java.time.Instant;

/**
 * Response DTO chứa thông tin màu sắc tôn sản xuất.
 *
 * @param id        ID màu sắc
 * @param colorName tên màu sắc (vd: Xanh ngọc, Đỏ đậm, Xanh rêu)
 * @param status    trạng thái hoạt động (ACTIVE/LOCKED)
 * @param createdAt thời điểm tạo
 * @param updatedAt thời điểm cập nhật lần cuối
 */
public record ColorResponse(
        Long id,
        String colorName,
        ColorStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Chuyển đổi từ entity {@link Color} sang DTO {@link ColorResponse}.
     *
     * @param color entity màu sắc cần chuyển đổi
     * @return DTO ColorResponse đã được điền dữ liệu
     */
    public static ColorResponse fromEntity(Color color) {
        return new ColorResponse(
                color.getId(),
                color.getColorName(),
                color.getStatus(),
                color.getCreatedAt(),
                color.getUpdatedAt()
        );
    }
}

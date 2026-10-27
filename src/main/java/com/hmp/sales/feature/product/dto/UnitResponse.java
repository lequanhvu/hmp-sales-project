package com.hmp.sales.feature.product.dto;

import com.hmp.sales.feature.product.entity.Unit;
import com.hmp.sales.feature.product.entity.UnitStatus;

import java.time.Instant;

/**
 * Response DTO chứa thông tin đơn vị tính bán hàng.
 *
 * @param id        ID đơn vị tính
 * @param unitName  tên đơn vị tính (vd: m, cái, tấm)
 * @param status    trạng thái hoạt động (ACTIVE/LOCKED)
 * @param createdAt thời điểm tạo
 * @param updatedAt thời điểm cập nhật lần cuối
 */
public record UnitResponse(
        Long id,
        String unitName,
        UnitStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Chuyển đổi từ entity {@link Unit} sang DTO {@link UnitResponse}.
     *
     * @param unit entity đơn vị tính cần chuyển đổi
     * @return DTO UnitResponse đã được điền dữ liệu
     */
    public static UnitResponse fromEntity(Unit unit) {
        return new UnitResponse(
                unit.getId(),
                unit.getUnitName(),
                unit.getStatus(),
                unit.getCreatedAt(),
                unit.getUpdatedAt()
        );
    }
}

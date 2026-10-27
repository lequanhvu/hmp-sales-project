package com.hmp.sales.feature.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO để tạo mới hoặc cập nhật đơn vị tính bán hàng.
 *
 * @param unitName tên đơn vị tính, bắt buộc, tối đa 50 ký tự (vd: m, cái, tấm, cuộn)
 */
public record UnitRequest(

        @NotBlank(message = "Tên đơn vị tính không được để trống.")
        @Size(max = 50, message = "Tên đơn vị tính không được vượt quá 50 ký tự.")
        String unitName
) {}

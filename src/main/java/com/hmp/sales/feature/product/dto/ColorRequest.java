package com.hmp.sales.feature.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO để tạo mới hoặc cập nhật màu sắc tôn sản xuất.
 *
 * @param colorName tên màu sắc, bắt buộc, tối đa 100 ký tự (vd: Xanh ngọc, Đỏ đậm)
 * @param imageUrl  đường dẫn ảnh mẫu màu sắc (tùy chọn, tối đa 500 ký tự)
 */
public record ColorRequest(

        @NotBlank(message = "Tên màu sắc không được để trống.")
        @Size(max = 100, message = "Tên màu sắc không được vượt quá 100 ký tự.")
        String colorName,

        @Size(max = 500, message = "Đường dẫn ảnh màu sắc không được vượt quá 500 ký tự.")
        String imageUrl
) {
    /**
     * Constructor thuận tiện khi chỉ truyền tên màu sắc.
     *
     * @param colorName tên màu sắc
     */
    public ColorRequest(String colorName) {
        this(colorName, null);
    }
}

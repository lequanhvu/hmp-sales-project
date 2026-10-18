package com.hmp.sales.user.dto;

import com.hmp.sales.user.entity.UserStatus;
import jakarta.validation.constraints.Size;

/**
 * Request DTO để tìm kiếm và lọc danh sách người dùng.
 * <p>
 * Tất cả các tham số đều là tùy chọn — nếu để trống sẽ trả về toàn bộ danh sách.
 *
 * @param name   lọc theo họ tên (tìm kiếm gần đúng)
 * @param phone  lọc theo số điện thoại (tìm kiếm gần đúng)
 * @param email  lọc theo địa chỉ email (tìm kiếm gần đúng)
 * @param status lọc theo trạng thái tài khoản (ACTIVE, LOCKED)
 * @param page   trang hiện tại (mặc định 0)
 * @param size   kích thước trang (mặc định 20)
 */
public record UserSearchRequest(
        @Size(max = 150, message = "Tên tìm kiếm không được vượt quá 150 ký tự")
        String name,

        @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự")
        String phone,

        @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
        String email,

        UserStatus status,

        Integer page,
        Integer size
) {
    /** Trang mặc định là 0 nếu không truyền vào. */
    public int pageOrDefault() {
        return page != null && page >= 0 ? page : 0;
    }

    /** Kích thước trang mặc định là 20 nếu không truyền vào. */
    public int sizeOrDefault() {
        return size != null && size > 0 ? size : 20;
    }
}

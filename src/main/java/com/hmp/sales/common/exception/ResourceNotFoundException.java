package com.hmp.sales.common.exception;

/**
 * Ngoại lệ ném ra khi không tìm thấy tài nguyên yêu cầu trong cơ sở dữ liệu
 * (ví dụ: sản phẩm, danh mục, đơn hàng, người dùng không tồn tại với ID chỉ định).
 * Tương ứng với mã phản hồi HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Khởi tạo ngoại lệ với thông điệp tùy chỉnh.
     *
     * @param message Thông điệp mô tả chi tiết lỗi.
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Khởi tạo ngoại lệ với tên loại tài nguyên và định danh không tìm thấy.
     * Tự động sinh thông điệp bằng tiếng Việt: "Không tìm thấy {resourceName} với mã: {id}".
     *
     * @param resourceName Tên loại tài nguyên (ví dụ: "Sản phẩm", "Danh mục", "Đơn hàng").
     * @param id           Định danh (ID hoặc mã) của tài nguyên tìm kiếm.
     */
    public ResourceNotFoundException(String resourceName, Object id) {
        super(String.format("Không tìm thấy %s với mã: %s", resourceName, id));
    }
}

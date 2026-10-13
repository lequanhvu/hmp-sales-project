package com.hmp.sales.common.exception;

/**
 * Ngoại lệ ném ra khi người dùng thực hiện hành động bị từ chối hoặc không được phép
 * (ví dụ: khách hàng cố gắng hủy đơn hàng đã qua thời hạn cho phép hủy, hoặc sửa đổi dữ liệu của người khác).
 * Tương ứng với mã phản hồi HTTP 403 Forbidden.
 */
public class UnauthorizedOperationException extends RuntimeException {

    /**
     * Khởi tạo ngoại lệ với thông điệp mô tả hành vi không được phép.
     *
     * @param message Thông điệp mô tả chi tiết lỗi.
     */
    public UnauthorizedOperationException(String message) {
        super(message);
    }

    /**
     * Khởi tạo ngoại lệ với thông điệp và nguyên nhân gốc rễ.
     *
     * @param message Thông điệp mô tả chi tiết lỗi.
     * @param cause   Ngoại lệ nguyên nhân gây ra lỗi này.
     */
    public UnauthorizedOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}

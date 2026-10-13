package com.hmp.sales.common.exception;

/**
 * Ngoại lệ ném ra khi một thao tác vi phạm quy tắc nghiệp vụ của hệ thống
 * (ví dụ: số lượng không hợp lệ, sản phẩm đang ở trạng thái ngừng kinh doanh, v.v.).
 */
public class BusinessRuleException extends RuntimeException {

    /**
     * Khởi tạo ngoại lệ với thông điệp mô tả lỗi vi phạm nghiệp vụ.
     *
     * @param message Thông điệp mô tả chi tiết lỗi.
     */
    public BusinessRuleException(String message) {
        super(message);
    }

    /**
     * Khởi tạo ngoại lệ với thông điệp và nguyên nhân gốc rễ.
     *
     * @param message Thông điệp mô tả chi tiết lỗi.
     * @param cause   Ngoại lệ nguyên nhân gây ra lỗi này.
     */
    public BusinessRuleException(String message, Throwable cause) {
        super(message, cause);
    }
}

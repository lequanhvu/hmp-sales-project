package com.hmp.sales.common.exception;

/**
 * Ngoại lệ ném ra khi xác thực mã OTP thất bại
 * (ví dụ: mã OTP sai, mã đã hết hạn, hoặc đã được sử dụng trước đó).
 */
public class OtpVerificationException extends BusinessRuleException {

    /**
     * Khởi tạo ngoại lệ với thông điệp mô tả lỗi xác thực OTP.
     *
     * @param message Thông điệp mô tả chi tiết lỗi.
     */
    public OtpVerificationException(String message) {
        super(message);
    }
}

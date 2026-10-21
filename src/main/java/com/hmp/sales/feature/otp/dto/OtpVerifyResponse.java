package com.hmp.sales.feature.otp.dto;

/**
 * DTO phản hồi sau khi xác minh OTP thành công.
 *
 * @param verified kết quả xác minh (luôn là {@code true} khi trả về response này)
 * @param phone    số điện thoại đã được xác thực
 */
public record OtpVerifyResponse(
    boolean verified,
    String phone
) {}

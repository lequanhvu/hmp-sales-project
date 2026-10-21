package com.hmp.sales.feature.otp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO yêu cầu xác minh mã OTP đã nhận.
 *
 * @param phone số điện thoại cần xác minh
 * @param otp   mã OTP 6 chữ số nhận được
 */
public record OtpVerifyRequest(
    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại không đúng định dạng")
    String phone,

    @NotBlank(message = "Mã OTP không được để trống")
    @Pattern(regexp = "^[0-9]{6}$", message = "Mã OTP phải là 6 chữ số")
    String otp
) {}

package com.hmp.sales.feature.otp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO yêu cầu gửi mã OTP đến số điện thoại.
 *
 * @param phone số điện thoại Việt Nam cần xác thực (10 chữ số, bắt đầu bằng 0)
 */
public record OtpRequest(
    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại không đúng định dạng (10 chữ số, bắt đầu bằng 0)")
    String phone
) {}

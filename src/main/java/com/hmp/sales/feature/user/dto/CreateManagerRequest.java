package com.hmp.sales.feature.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO để Admin tạo tài khoản Manager mới.
 * <p>
 * Manager sử dụng Google authentication — không cần mật khẩu.
 * Admin chỉ cần cung cấp email Google và họ tên của Manager.
 *
 * @param email    địa chỉ email Google của Manager (phải duy nhất trong hệ
 *                 thống)
 * @param fullName họ và tên đầy đủ của Manager
 */
public record CreateManagerRequest(
                @NotBlank(message = "Email không được để trống")
                @Email(message = "Định dạng email không hợp lệ") 
                @Size(max = 255, message = "Email không được vượt quá 255 ký tự") 
                String email,

                @NotBlank(message = "Họ tên không được để trống") 
                @Size(min = 2, max = 150, message = "Họ tên phải từ 2 đến 150 ký tự") 
                String fullName) {
}

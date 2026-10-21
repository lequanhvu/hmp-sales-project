package com.hmp.sales.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO yêu cầu đăng nhập bằng Google (nhận Google ID Token từ frontend).
 *
 * @param credential Google ID Token (JWT) nhận được từ Google Sign-In trên frontend.
 *                   Không được trống hoặc null.
 */
public record GoogleLoginRequest(
    @NotBlank(message = "Google credential không được để trống")
    String credential
) {}

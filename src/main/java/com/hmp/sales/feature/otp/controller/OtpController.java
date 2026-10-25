package com.hmp.sales.feature.otp.controller;

import com.hmp.sales.common.response.ApiSuccessResponse;
import com.hmp.sales.feature.otp.dto.OtpRequest;
import com.hmp.sales.feature.otp.dto.OtpVerifyRequest;
import com.hmp.sales.feature.otp.dto.OtpVerifyResponse;
import com.hmp.sales.feature.otp.service.PhoneOtpService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.security.core.Authentication;

/**
 * Controller xử lý các endpoint OTP xác thực số điện thoại.
 * <p>
 * Cung cấp 2 endpoint yêu cầu đăng nhập (Customer role):
 * <ul>
 *   <li>{@code POST /api/v1/orders/otp/request} — Gửi mã OTP đến số điện thoại.</li>
 *   <li>{@code POST /api/v1/orders/otp/verify} — Xác minh mã OTP và cập nhật số điện thoại vào tài khoản.</li>
 * </ul>
 * Controller này hoàn toàn mỏng (thin controller): không chứa business logic.
 */
@RestController
@RequestMapping({"/api/v1/me/otp", "/api/v1/orders/otp"})
public class OtpController {

    private final PhoneOtpService phoneOtpService;

    /**
     * Khởi tạo controller với interface {@link PhoneOtpService}.
     *
     * @param phoneOtpService interface dịch vụ OTP (không inject concrete class)
     */
    public OtpController(PhoneOtpService phoneOtpService) {
        this.phoneOtpService = phoneOtpService;
    }

    /**
     * Gửi mã OTP 6 chữ số đến số điện thoại.
     * <p>
     * Áp dụng rate-limit: không gửi lại trong vòng 60 giây kể từ lần gửi trước.
     * OTP có hiệu lực trong 5 phút.
     *
     * @param request body chứa số điện thoại cần gửi OTP
     * @return {@link ApiSuccessResponse} rỗng với thông báo đã gửi thành công
     */
    @PostMapping("/request")
    public ResponseEntity<ApiSuccessResponse<Void>> requestOtp(
            @Valid @RequestBody OtpRequest request) {
        phoneOtpService.sendOtp(request.phone());
        return ResponseEntity.ok(ApiSuccessResponse.success("Mã xác thực OTP đã được gửi thành công", null));
    }

    /**
     * Xác minh mã OTP người dùng nhập và lưu số điện thoại vào hồ sơ người dùng trong DB.
     * <p>
     * Nếu đúng, đánh dấu số điện thoại đã xác thực (bảo chứng 10 phút) để dùng khi tạo đơn hàng,
     * đồng thời lưu trực tiếp số điện thoại vào tài khoản người dùng trong cơ sở dữ liệu.
     * OTP bị tiêu thụ ngay sau khi xác minh thành công (không thể dùng lại).
     *
     * @param request        body chứa số điện thoại và mã OTP 6 chữ số
     * @param authentication đối tượng xác thực của người dùng hiện tại
     * @return {@link ApiSuccessResponse} chứa {@link OtpVerifyResponse} xác nhận thành công
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiSuccessResponse<OtpVerifyResponse>> verifyOtp(
            @Valid @RequestBody OtpVerifyRequest request,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        phoneOtpService.verifyOtp(request.phone(), request.otp(), userId);
        OtpVerifyResponse response = new OtpVerifyResponse(true, request.phone());
        return ResponseEntity.ok(ApiSuccessResponse.success("Xác thực số điện thoại thành công", response));
    }

    /**
     * Trích xuất User ID từ thông tin xác thực.
     *
     * @param authentication thông tin xác thực người dùng
     * @return ID người dùng dạng Long, hoặc null nếu chưa xác thực hoặc không hợp lệ
     */
    private Long extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

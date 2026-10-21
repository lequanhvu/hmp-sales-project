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

/**
 * Controller xử lý các endpoint OTP xác thực số điện thoại.
 * <p>
 * Cung cấp 2 endpoint yêu cầu đăng nhập (Customer role):
 * <ul>
 *   <li>{@code POST /api/v1/orders/otp/request} — Gửi mã OTP đến số điện thoại.</li>
 *   <li>{@code POST /api/v1/orders/otp/verify} — Xác minh mã OTP.</li>
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
     * Xác minh mã OTP người dùng nhập.
     * <p>
     * Nếu đúng, đánh dấu số điện thoại đã xác thực (bảo chứng 10 phút) để dùng khi tạo đơn hàng.
     * OTP bị tiêu thụ ngay sau khi xác minh thành công (không thể dùng lại).
     *
     * @param request body chứa số điện thoại và mã OTP 6 chữ số
     * @return {@link ApiSuccessResponse} chứa {@link OtpVerifyResponse} xác nhận thành công
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiSuccessResponse<OtpVerifyResponse>> verifyOtp(
            @Valid @RequestBody OtpVerifyRequest request) {
        phoneOtpService.verifyOtp(request.phone(), request.otp());
        OtpVerifyResponse response = new OtpVerifyResponse(true, request.phone());
        return ResponseEntity.ok(ApiSuccessResponse.success("Xác thực số điện thoại thành công", response));
    }
}

package com.hmp.sales.feature.otp.controller;

import com.hmp.sales.common.exception.GlobalExceptionHandler;
import com.hmp.sales.common.exception.OtpVerificationException;
import com.hmp.sales.feature.otp.service.PhoneOtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit test cho {@link OtpController}.
 * Kiểm thử các endpoint yêu cầu và xác thực mã OTP số điện thoại.
 */
@ExtendWith(MockitoExtension.class)
class OtpControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PhoneOtpService phoneOtpService;

    @InjectMocks
    private OtpController otpController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(otpController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("requestOtp - Yêu cầu OTP thành công trả về 200")
    void requestOtp_validPhone_returns200() throws Exception {
        String jsonPayload = """
                {
                    "phone": "0912345678"
                }
                """;
        doNothing().when(phoneOtpService).sendOtp("0912345678");

        mockMvc.perform(post("/api/v1/orders/otp/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Mã xác thực OTP đã được gửi thành công"));

        verify(phoneOtpService).sendOtp("0912345678");
    }

    @Test
    @DisplayName("requestOtp - Số điện thoại không đúng định dạng trả về 400 Bad Request")
    void requestOtp_invalidPhone_returns400() throws Exception {
        String jsonPayload = """
                {
                    "phone": "12345"
                }
                """;

        mockMvc.perform(post("/api/v1/orders/otp/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(phoneOtpService);
    }

    @Test
    @DisplayName("verifyOtp - Xác minh OTP thành công trả về 200 và response xác nhận")
    void verifyOtp_validOtp_returns200AndToken() throws Exception {
        String jsonPayload = """
                {
                    "phone": "0912345678",
                    "otp": "123456"
                }
                """;
        doNothing().when(phoneOtpService).verifyOtp("0912345678", "123456");

        mockMvc.perform(post("/api/v1/orders/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.phone").value("0912345678"))
                .andExpect(jsonPath("$.data.verified").value(true));

        verify(phoneOtpService).verifyOtp("0912345678", "123456");
    }

    @Test
    @DisplayName("verifyOtp - OTP sai hoặc hết hạn ném ngoại lệ trả về 400")
    void verifyOtp_wrongOrExpiredOtp_returns400() throws Exception {
        String jsonPayload = """
                {
                    "phone": "0912345678",
                    "otp": "999999"
                }
                """;
        doThrow(new OtpVerificationException("Mã OTP không chính xác. Bạn còn 2 lần thử."))
                .when(phoneOtpService).verifyOtp("0912345678", "999999");

        mockMvc.perform(post("/api/v1/orders/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("Mã OTP không chính xác. Bạn còn 2 lần thử."));

        verify(phoneOtpService).verifyOtp("0912345678", "999999");
    }

    @Test
    @DisplayName("verifyOtp - OTP không đủ 6 chữ số trả về 400 Bad Request")
    void verifyOtp_invalidOtpFormat_returns400() throws Exception {
        String jsonPayload = """
                {
                    "phone": "0912345678",
                    "otp": "12"
                }
                """;

        mockMvc.perform(post("/api/v1/orders/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(phoneOtpService);
    }
}

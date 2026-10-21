package com.hmp.sales.feature.auth.controller;

import com.hmp.sales.common.exception.GlobalExceptionHandler;
import com.hmp.sales.common.exception.UnauthorizedOperationException;
import com.hmp.sales.feature.auth.dto.AuthResponse;
import com.hmp.sales.feature.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit test cho {@link AuthController}.
 * Kiểm thử các endpoint đăng nhập Google, refresh token và logout.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("loginWithGoogle - Đăng nhập Google thành công trả về 200 và access token")
    void loginWithGoogle_validCredential_returns200AndToken() throws Exception {
        String jsonPayload = """
                {
                    "credential": "google-id-token-valid"
                }
                """;
        AuthResponse response = AuthResponse.bearer("mocked.jwt.token");

        when(authService.loginWithGoogle(eq("google-id-token-valid"), any(HttpServletResponse.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.accessToken").value("mocked.jwt.token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));

        verify(authService).loginWithGoogle(eq("google-id-token-valid"), any(HttpServletResponse.class));
    }

    @Test
    @DisplayName("loginWithGoogle - Body thiếu credential trả về 400 Bad Request")
    void loginWithGoogle_blankCredential_returns400() throws Exception {
        String jsonPayload = """
                {
                    "credential": ""
                }
                """;

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("refreshToken - Làm mới token thành công trả về 200 và token mới")
    void refreshToken_validCookie_returns200AndNewToken() throws Exception {
        AuthResponse response = AuthResponse.bearer("new.mocked.jwt.token");

        when(authService.refreshToken(any(HttpServletRequest.class), any(HttpServletResponse.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refresh_token", "raw-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.accessToken").value("new.mocked.jwt.token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));

        verify(authService).refreshToken(any(HttpServletRequest.class), any(HttpServletResponse.class));
    }

    @Test
    @DisplayName("refreshToken - Không có cookie refresh_token trả về 403 Forbidden")
    void refreshToken_missingCookie_returns403() throws Exception {
        when(authService.refreshToken(any(HttpServletRequest.class), any(HttpServletResponse.class)))
                .thenThrow(new UnauthorizedOperationException("Không tìm thấy Refresh Token trong cookie"));

        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value(403));

        verify(authService).refreshToken(any(HttpServletRequest.class), any(HttpServletResponse.class));
    }

    @Test
    @DisplayName("logout - Đăng xuất thành công trả về 200")
    void logout_success_returns200() throws Exception {
        doNothing().when(authService).logout(any(), any(HttpServletRequest.class), any(HttpServletResponse.class));

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer valid.jwt.token")
                        .cookie(new Cookie("refresh_token", "raw-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200));

        verify(authService).logout(eq("Bearer valid.jwt.token"), any(HttpServletRequest.class), any(HttpServletResponse.class));
    }
}

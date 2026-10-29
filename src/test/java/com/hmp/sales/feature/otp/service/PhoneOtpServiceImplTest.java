package com.hmp.sales.feature.otp.service;

import org.mockito.quality.Strictness;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.common.exception.OtpVerificationException;
import com.hmp.sales.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.hmp.sales.feature.user.entity.User;
import com.hmp.sales.feature.user.repository.UserRepository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test cho {@link PhoneOtpServiceImpl}.
 * Xác minh toàn bộ luồng OTP: gửi, rate-limit, xác minh, single-use, replay prevention.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PhoneOtpServiceImplTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private UserRepository userRepository;

    private PhoneOtpServiceImpl otpService;
    private static final String PHONE = "0987654321";

    @BeforeEach
    void setUp() {
        AppProperties.OtpProperties otpProps = new AppProperties.OtpProperties(300, 5, 60);
        AppProperties appProperties = new AppProperties(null, otpProps, null, null, null, null);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        otpService = new PhoneOtpServiceImpl(redisTemplate, appProperties, userRepository);
    }

    @Test
    @DisplayName("sendOtp - Gửi thành công: lưu OTP và rate-limit vào Redis")
    void sendOtp_success_savesOtpAndRateLimitToRedis() {
        when(redisTemplate.hasKey("otp:rate:" + PHONE)).thenReturn(false);

        otpService.sendOtp(PHONE);

        verify(valueOps).set(eq("otp:phone:" + PHONE), matches("[0-9]{6}"), eq(300L), eq(TimeUnit.SECONDS));
        verify(valueOps).set(eq("otp:rate:" + PHONE), eq("1"), eq(60L), eq(TimeUnit.SECONDS));
        verify(redisTemplate).delete("otp:attempts:" + PHONE);
    }

    @Test
    @DisplayName("sendOtp - Rate limit: gửi lại quá nhanh → ném BusinessRuleException")
    void sendOtp_rateLimitActive_throwsBusinessRuleException() {
        when(redisTemplate.hasKey("otp:rate:" + PHONE)).thenReturn(true);

        assertThatThrownBy(() -> otpService.sendOtp(PHONE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("giây");

        verify(valueOps, never()).set(eq("otp:phone:" + PHONE), anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("verifyOtp - Mã đúng: xóa OTP, đánh dấu verified")
    void verifyOtp_correctOtp_deletesOtpAndSetsVerified() {
        when(valueOps.get("otp:attempts:" + PHONE)).thenReturn(null);
        when(valueOps.get("otp:phone:" + PHONE)).thenReturn("482915");

        otpService.verifyOtp(PHONE, "482915");

        verify(redisTemplate).delete("otp:phone:" + PHONE);
        verify(redisTemplate).delete("otp:attempts:" + PHONE);
        verify(valueOps).set(eq("otp:verified:" + PHONE), eq("1"), eq(600L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("verifyOtp - Mã đúng kèm userId: lưu số điện thoại vào DB cho User")
    void verifyOtp_correctOtp_withUserId_savesPhoneToUserDb() {
        when(valueOps.get("otp:attempts:" + PHONE)).thenReturn(null);
        when(valueOps.get("otp:phone:" + PHONE)).thenReturn("482915");
        User mockUser = User.builder().id(10L).fullName("Test Customer").build();
        when(userRepository.findById(10L)).thenReturn(Optional.of(mockUser));

        otpService.verifyOtp(PHONE, "482915", 10L);

        assertThat(mockUser.getPhone()).isEqualTo(PHONE);
        verify(userRepository).save(mockUser);
        verify(redisTemplate).delete("otp:phone:" + PHONE);
        verify(valueOps).set(eq("otp:verified:" + PHONE), eq("1"), eq(600L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("verifyOtp - Mã sai: tăng bộ đếm attempts")
    void verifyOtp_wrongOtp_incrementsAttemptCounter() {
        when(valueOps.get("otp:attempts:" + PHONE)).thenReturn(null);
        when(valueOps.get("otp:phone:" + PHONE)).thenReturn("111111");

        assertThatThrownBy(() -> otpService.verifyOtp(PHONE, "999999"))
                .isInstanceOf(OtpVerificationException.class)
                .hasMessageContaining("không đúng");

        verify(valueOps).increment("otp:attempts:" + PHONE);
    }

    @Test
    @DisplayName("verifyOtp - Vượt số lần thử tối đa: xóa OTP và ném OtpVerificationException")
    void verifyOtp_maxAttemptsExceeded_deletesOtpAndThrows() {
        when(valueOps.get("otp:attempts:" + PHONE)).thenReturn("5"); // = maxAttempts

        assertThatThrownBy(() -> otpService.verifyOtp(PHONE, "123456"))
                .isInstanceOf(OtpVerificationException.class)
                .hasMessageContaining("quá");

        verify(redisTemplate).delete("otp:phone:" + PHONE);
        verify(redisTemplate).delete("otp:attempts:" + PHONE);
    }

    @Test
    @DisplayName("verifyOtp - OTP đã hết hạn (key không còn tồn tại): ném OtpVerificationException")
    void verifyOtp_otpExpired_throwsException() {
        when(valueOps.get("otp:attempts:" + PHONE)).thenReturn(null);
        when(valueOps.get("otp:phone:" + PHONE)).thenReturn(null);

        assertThatThrownBy(() -> otpService.verifyOtp(PHONE, "123456"))
                .isInstanceOf(OtpVerificationException.class)
                .hasMessageContaining("hết hạn");
    }

    @Test
    @DisplayName("isVerified - Cờ tồn tại trong Redis: trả về true")
    void isVerified_verifiedKeyExists_returnsTrue() {
        when(redisTemplate.hasKey("otp:verified:" + PHONE)).thenReturn(true);
        assertThat(otpService.isVerified(PHONE)).isTrue();
    }

    @Test
    @DisplayName("isVerified - Cờ không tồn tại: trả về false")
    void isVerified_verifiedKeyAbsent_returnsFalse() {
        when(redisTemplate.hasKey("otp:verified:" + PHONE)).thenReturn(false);
        assertThat(otpService.isVerified(PHONE)).isFalse();
    }

    @Test
    @DisplayName("consumeVerification - Xóa cờ verified khỏi Redis")
    void consumeVerification_deletesVerifiedKey() {
        otpService.consumeVerification(PHONE);
        verify(redisTemplate).delete("otp:verified:" + PHONE);
    }
}

package com.hmp.sales.feature.otp.service;

/**
 * Hợp đồng dịch vụ xác thực số điện thoại qua OTP (One-Time Password).
 * <p>
 * OTP là chuỗi 6 chữ số ngẫu nhiên, được lưu tạm thời trên Redis với TTL 5 phút.
 * Hỗ trợ rate-limiting (cooldown 60s giữa 2 lần gửi) và giới hạn số lần nhập sai.
 * OTP bị tiêu thụ ngay sau khi xác minh thành công (single-use).
 * <p>
 * Redis key schema:
 * <ul>
 *   <li>{@code otp:phone:{phone}} — mã OTP (TTL = OTP_EXPIRY_SECONDS)</li>
 *   <li>{@code otp:rate:{phone}} — cooldown chống spam gửi OTP (TTL = RESEND_INTERVAL_SECONDS)</li>
 *   <li>{@code otp:attempts:{phone}} — đếm số lần nhập sai (TTL = OTP_EXPIRY_SECONDS)</li>
 *   <li>{@code otp:verified:{phone}} — bảo chứng đã xác thực thành công (TTL = 10 phút)</li>
 * </ul>
 */
public interface PhoneOtpService {

    /**
     * Sinh mã OTP 6 chữ số ngẫu nhiên và lưu vào Redis với TTL.
     * <p>
     * Kiểm tra rate-limit: nếu gửi quá nhanh (chưa hết cooldown), ném {@link com.hmp.sales.common.exception.BusinessRuleException}.
     * Khi gửi thành công, reset bộ đếm lần nhập sai.
     * <p>
     * Trong môi trường V1, OTP được log ở DEBUG level (thay thế cho việc gửi SMS thật).
     *
     * @param phone số điện thoại nhận OTP (định dạng: 10 chữ số Việt Nam)
     * @throws com.hmp.sales.common.exception.BusinessRuleException nếu gửi lại quá nhanh (rate limit)
     */
    void sendOtp(String phone);

    /**
     * Xác minh mã OTP do người dùng nhập.
     * <p>
     * Kiểm tra số lần nhập sai: nếu vượt giới hạn, OTP bị hủy và ném {@link com.hmp.sales.common.exception.OtpVerificationException}.
     * Nếu OTP đúng: xóa khỏi Redis ngay (single-use), đặt cờ {@code otp:verified:{phone}} với TTL 10 phút.
     * Nếu OTP sai: tăng bộ đếm lần nhập sai.
     *
     * @param phone số điện thoại cần xác minh
     * @param otp   mã OTP 6 chữ số người dùng nhập vào
     * @throws com.hmp.sales.common.exception.OtpVerificationException nếu OTP sai, đã hết hạn,
     *         hoặc vượt số lần thử cho phép
     */
    void verifyOtp(String phone, String otp);

    /**
     * Kiểm tra xem số điện thoại đã có bảo chứng xác thực OTP còn hiệu lực hay không.
     * Dùng để validate trước khi tạo đơn hàng mà không tiêu thụ OTP lần thứ hai.
     *
     * @param phone số điện thoại cần kiểm tra
     * @return {@code true} nếu cờ {@code otp:verified:{phone}} tồn tại trong Redis
     */
    boolean isVerified(String phone);

    /**
     * Xóa bảo chứng xác thực {@code otp:verified:{phone}} sau khi đã hoàn tất đặt hàng.
     * Đảm bảo mỗi OTP verification chỉ được dùng cho một lần đặt hàng.
     *
     * @param phone số điện thoại cần xóa bảo chứng
     */
    void consumeVerification(String phone);
}

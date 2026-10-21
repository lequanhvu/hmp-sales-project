package com.hmp.sales.feature.auth.service;

/**
 * Hợp đồng dịch vụ xác minh Google ID Token.
 * <p>
 * Nhận Google Credential (ID Token) từ frontend (sau khi Google Sign-In thành công),
 * gọi Google TokenInfo endpoint để xác thực chữ ký và lấy thông tin người dùng.
 * Không lưu trữ hay cache Google token — chỉ dùng để lấy payload.
 */
public interface GoogleAuthService {

    /**
     * Xác minh Google ID Token và trả về thông tin payload người dùng.
     * <p>
     * Gọi {@code https://oauth2.googleapis.com/tokeninfo?id_token={credential}} để xác thực.
     * Nếu token không hợp lệ hoặc Google trả về lỗi, ném {@link com.hmp.sales.common.exception.UnauthorizedOperationException}.
     *
     * @param idToken chuỗi Google ID Token (JWT) từ frontend
     * @return {@link GoogleUserPayload} chứa thông tin xác thực từ Google
     * @throws com.hmp.sales.common.exception.UnauthorizedOperationException nếu token không hợp lệ
     */
    GoogleUserPayload verifyGoogleToken(String idToken);

    /**
     * Payload thông tin người dùng trả về sau khi xác minh Google ID Token thành công.
     *
     * @param sub  Google Subject — định danh người dùng duy nhất từ Google (stable identifier)
     * @param email địa chỉ email của tài khoản Google
     * @param name  tên hiển thị đầy đủ của người dùng
     */
    record GoogleUserPayload(String sub, String email, String name) {}
}

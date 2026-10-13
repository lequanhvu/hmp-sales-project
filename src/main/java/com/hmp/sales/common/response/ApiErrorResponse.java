package com.hmp.sales.common.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Record đại diện cho cấu trúc phản hồi lỗi chuẩn của toàn bộ hệ thống REST
 * API.
 * Đảm bảo tính nhất quán theo quy chuẩn API specification.
 *
 * @param statusCode Mã trạng thái HTTP tương ứng (ví dụ: 400, 401, 403, 404,
 *                   409, 500).
 * @param message    Thông điệp mô tả lỗi hiển thị bằng tiếng Việt.
 * @param data       Dữ liệu tùy chọn đính kèm khi có lỗi (sử dụng generic type
 *                   T).
 * @param errors     Danh sách chi tiết các lỗi validation trường dữ liệu (nếu
 *                   có).
 * @param timestamp  Thời điểm xảy ra lỗi theo định dạng chuẩn
 *                   yyyy-MM-dd'T'HH:mm:ss.
 * @param <T>        Kiểu dữ liệu của payload đính kèm trong trường data.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse<T>(
        int statusCode,
        String message,
        T data,
        List<ValidationErrorResponse> errors,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime timestamp) {

    /**
     * Khởi tạo đối tượng ApiErrorResponse với mã lỗi và thông điệp tùy chỉnh.
     */
    public static <T> ApiErrorResponse<T> of(int statusCode, String message) {
        return new ApiErrorResponse<>(statusCode, message, null, null, LocalDateTime.now());
    }

    /**
     * Khởi tạo đối tượng ApiErrorResponse với mã lỗi, thông điệp và payload dữ liệu
     * đính kèm.
     */
    public static <T> ApiErrorResponse<T> of(int statusCode, String message, T data) {
        return new ApiErrorResponse<>(statusCode, message, data, null, LocalDateTime.now());
    }

    /**
     * Khởi tạo đối tượng ApiErrorResponse với mã lỗi, thông điệp và danh sách chi
     * tiết lỗi validation.
     */
    public static <T> ApiErrorResponse<T> of(int statusCode, String message, List<ValidationErrorResponse> errors) {
        return new ApiErrorResponse<>(statusCode, message, null, errors, LocalDateTime.now());
    }

    /**
     * Khởi tạo đối tượng ApiErrorResponse với mã lỗi, ưu tiên thông điệp message,
     * nếu message trống hoặc null thì dự phòng sử dụng error.
     */
    public static <T> ApiErrorResponse<T> ofError(int statusCode, String message, String error) {
        String finalMessage = (message != null && !message.isBlank()) ? message : error;
        return new ApiErrorResponse<>(statusCode, finalMessage, null, null, LocalDateTime.now());
    }

    /**
     * Tạo phản hồi lỗi 404 Not Found (Không tìm thấy tài nguyên).
     */
    public static <T> ApiErrorResponse<T> notFound(String message) {
        return new ApiErrorResponse<>(404, message, null, null, LocalDateTime.now());
    }

    /**
     * Tạo phản hồi lỗi 400 Bad Request (Yêu cầu không hợp lệ).
     */
    public static <T> ApiErrorResponse<T> badRequest(String message) {
        return new ApiErrorResponse<>(400, message, null, null, LocalDateTime.now());
    }

    /**
     * Tạo phản hồi lỗi 400 Bad Request kèm danh sách các lỗi validation chi tiết
     * theo từng trường.
     */
    public static <T> ApiErrorResponse<T> badRequest(String message, List<ValidationErrorResponse> errors) {
        return new ApiErrorResponse<>(400, message, null, errors, LocalDateTime.now());
    }

    /**
     * Tạo phản hồi lỗi 400 Bad Request kèm danh sách chuỗi mô tả chi tiết lỗi,
     * tự động chuyển đổi sang định dạng ValidationErrorResponse.
     *
     * @param message Thông điệp mô tả lỗi chung.
     * @param details Danh sách các thông điệp lỗi chi tiết dạng chuỗi.
     * @param <T>     Kiểu dữ liệu của data.
     * @return Đối tượng ApiErrorResponse với status 400 kèm errors đã được ánh xạ.
     */
    public static <T> ApiErrorResponse<T> badRequestWithDetails(String message, List<String> details) {
        List<ValidationErrorResponse> validationErrors = details == null ? null
                : details.stream()
                        .map(detail -> new ValidationErrorResponse(null, detail))
                        .toList();
        return new ApiErrorResponse<>(400, message, null, validationErrors, LocalDateTime.now());
    }

    /**
     * Tạo phản hồi lỗi 409 Conflict (Xung đột trạng thái hoặc dữ liệu đã tồn tại).
     */
    public static <T> ApiErrorResponse<T> conflict(String message) {
        return new ApiErrorResponse<>(409, message, null, null, LocalDateTime.now());
    }

    /**
     * Tạo phản hồi lỗi 401 Unauthorized (Chưa xác thực hoặc token không hợp lệ).
     */
    public static <T> ApiErrorResponse<T> unauthorized(String message) {
        return new ApiErrorResponse<>(401, message, null, null, LocalDateTime.now());
    }

    /**
     * Tạo phản hồi lỗi 403 Forbidden (Bị từ chối truy cập / không đủ quyền hạn).
     */
    public static <T> ApiErrorResponse<T> forbidden(String message) {
        return new ApiErrorResponse<>(403, message, null, null, LocalDateTime.now());
    }

    /**
     * Tạo phản hồi lỗi 500 Internal Server Error (Lỗi nội bộ hệ thống không mong
     * muốn).
     */
    public static <T> ApiErrorResponse<T> error(String message) {
        return new ApiErrorResponse<>(500, message, null, null, LocalDateTime.now());
    }
}

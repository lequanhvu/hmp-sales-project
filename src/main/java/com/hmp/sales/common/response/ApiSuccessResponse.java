package com.hmp.sales.common.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * Record đại diện cho cấu trúc phản hồi API thành công của toàn bộ hệ thống REST API.
 * Đảm bảo tính nhất quán theo quy chuẩn API specification (status 200, 201,...).
 *
 * @param statusCode Mã trạng thái HTTP thành công (ví dụ: 200, 201).
 * @param message    Thông điệp phản hồi bằng tiếng Việt.
 * @param data       Dữ liệu trả về cho client (sử dụng generic type T).
 * @param timestamp  Thời điểm phản hồi theo định dạng chuẩn yyyy-MM-dd'T'HH:mm:ss.
 * @param <T>        Kiểu dữ liệu của payload trả về.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiSuccessResponse<T>(
    int statusCode,
    String message,
    T data,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime timestamp
) {

    /**
     * Tạo phản hồi thành công 200 OK với dữ liệu và thông điệp mặc định "Thành công".
     *
     * @param data Dữ liệu trả về.
     * @param <T>  Kiểu dữ liệu của data.
     * @return Đối tượng ApiSuccessResponse với status 200.
     */
    public static <T> ApiSuccessResponse<T> success(T data) {
        return new ApiSuccessResponse<>(200, "Thành công", data, LocalDateTime.now());
    }

    /**
     * Tạo phản hồi thành công 200 OK với thông điệp tùy chỉnh và dữ liệu.
     *
     * @param message Thông điệp mô tả kết quả thành công.
     * @param data    Dữ liệu trả về.
     * @param <T>     Kiểu dữ liệu của data.
     * @return Đối tượng ApiSuccessResponse với status 200.
     */
    public static <T> ApiSuccessResponse<T> success(String message, T data) {
        return new ApiSuccessResponse<>(200, message, data, LocalDateTime.now());
    }

    /**
     * Tạo phản hồi 201 Created với dữ liệu và thông điệp mặc định "Tạo mới thành công".
     *
     * @param data Dữ liệu của tài nguyên vừa tạo.
     * @param <T>  Kiểu dữ liệu của data.
     * @return Đối tượng ApiSuccessResponse với status 201.
     */
    public static <T> ApiSuccessResponse<T> created(T data) {
        return new ApiSuccessResponse<>(201, "Tạo mới thành công", data, LocalDateTime.now());
    }

    /**
     * Tạo phản hồi 201 Created khi tạo mới tài nguyên thành công.
     *
     * @param message Thông điệp mô tả kết quả tạo mới.
     * @param data    Dữ liệu của tài nguyên vừa được tạo.
     * @param <T>     Kiểu dữ liệu của data.
     * @return Đối tượng ApiSuccessResponse với status 201.
     */
    public static <T> ApiSuccessResponse<T> created(String message, T data) {
        return new ApiSuccessResponse<>(201, message, data, LocalDateTime.now());
    }

    /**
     * Tạo phản hồi thành công với mã trạng thái HTTP, thông điệp và dữ liệu tùy chỉnh.
     *
     * @param statusCode Mã trạng thái HTTP thành công.
     * @param message    Thông điệp phản hồi.
     * @param data       Dữ liệu trả về.
     * @param <T>        Kiểu dữ liệu của data.
     * @return Đối tượng ApiSuccessResponse đã cấu hình.
     */
    public static <T> ApiSuccessResponse<T> of(int statusCode, String message, T data) {
        return new ApiSuccessResponse<>(statusCode, message, data, LocalDateTime.now());
    }
}

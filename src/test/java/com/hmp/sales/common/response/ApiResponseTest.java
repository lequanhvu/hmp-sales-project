package com.hmp.sales.common.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    @Test
    @DisplayName("success(data) should set statusCode 200 and default Vietnamese message")
    void success_withData_returns200() {
        String data = "test data";
        ApiSuccessResponse<String> response = ApiSuccessResponse.success(data);

        assertEquals(200, response.statusCode());
        assertEquals("Thành công", response.message());
        assertEquals(data, response.data());
        assertNotNull(response.timestamp());
    }

    @Test
    @DisplayName("success(message, data) should set custom message and statusCode 200")
    void success_withCustomMessage_returns200() {
        ApiSuccessResponse<Integer> response = ApiSuccessResponse.success("Đã tìm thấy thông tin", 123);

        assertEquals(200, response.statusCode());
        assertEquals("Đã tìm thấy thông tin", response.message());
        assertEquals(123, response.data());
        assertNotNull(response.timestamp());
    }

    @Test
    @DisplayName("created(data) should set statusCode 201 and default message")
    void created_withData_returns201() {
        ApiSuccessResponse<String> response = ApiSuccessResponse.created("new-id");

        assertEquals(201, response.statusCode());
        assertEquals("Tạo mới thành công", response.message());
        assertEquals("new-id", response.data());
        assertNotNull(response.timestamp());
    }

    @Test
    @DisplayName("created(message, data) should set statusCode 201")
    void created_returns201() {
        ApiSuccessResponse<String> response = ApiSuccessResponse.created("Tạo thành công", "new-id");

        assertEquals(201, response.statusCode());
        assertEquals("Tạo thành công", response.message());
        assertEquals("new-id", response.data());
        assertNotNull(response.timestamp());
    }

    @Test
    @DisplayName("of(statusCode, message, data) should set custom values")
    void of_returnsCustomSuccessResponse() {
        ApiSuccessResponse<String> response = ApiSuccessResponse.of(200, "Tùy chỉnh", "payload");

        assertEquals(200, response.statusCode());
        assertEquals("Tùy chỉnh", response.message());
        assertEquals("payload", response.data());
        assertNotNull(response.timestamp());
    }

    @Test
    @DisplayName("ApiErrorResponse.of should set status code and null data")
    void apiErrorResponse_of_returnsErrorStructure() {
        ApiErrorResponse<?> response = ApiErrorResponse.of(404, "Không tìm thấy dữ liệu.");

        assertEquals(404, response.statusCode());
        assertEquals("Không tìm thấy dữ liệu.", response.message());
        assertNull(response.data());
        assertNull(response.errors());
        assertNotNull(response.timestamp());
    }

    @Test
    @DisplayName("ApiErrorResponse.of with errors should include validation details")
    void apiErrorResponse_withValidationErrors_includesDetails() {
        List<ValidationErrorResponse> errors = List.of(
            new ValidationErrorResponse("phone", "Số điện thoại không hợp lệ")
        );
        ApiErrorResponse<?> response = ApiErrorResponse.of(400, "Dữ liệu không hợp lệ", errors);

        assertEquals(400, response.statusCode());
        assertEquals("Dữ liệu không hợp lệ", response.message());
        assertNotNull(response.errors());
        assertEquals(1, response.errors().size());
        assertEquals("phone", response.errors().get(0).field());
    }

    @Test
    @DisplayName("ApiErrorResponse helper methods return expected status codes and messages")
    void apiErrorResponse_convenienceMethods_returnExpectedValues() {
        ApiErrorResponse<Void> notFound = ApiErrorResponse.notFound("Tài nguyên không tìm thấy");
        assertEquals(404, notFound.statusCode());
        assertEquals("Tài nguyên không tìm thấy", notFound.message());
        assertNull(notFound.data());

        ApiErrorResponse<Void> badRequest = ApiErrorResponse.badRequest("Yêu cầu không hợp lệ");
        assertEquals(400, badRequest.statusCode());
        assertEquals("Yêu cầu không hợp lệ", badRequest.message());

        ApiErrorResponse<Void> conflict = ApiErrorResponse.conflict("Xung đột dữ liệu");
        assertEquals(409, conflict.statusCode());
        assertEquals("Xung đột dữ liệu", conflict.message());

        ApiErrorResponse<Void> unauthorized = ApiErrorResponse.unauthorized("Chưa đăng nhập");
        assertEquals(401, unauthorized.statusCode());
        assertEquals("Chưa đăng nhập", unauthorized.message());

        ApiErrorResponse<Void> forbidden = ApiErrorResponse.forbidden("Không có quyền");
        assertEquals(403, forbidden.statusCode());
        assertEquals("Không có quyền", forbidden.message());

        ApiErrorResponse<Void> error = ApiErrorResponse.error("Lỗi hệ thống");
        assertEquals(500, error.statusCode());
        assertEquals("Lỗi hệ thống", error.message());
    }

    @Test
    @DisplayName("ApiErrorResponse.of with generic data should retain typed data")
    void apiErrorResponse_withGenericData_retainsTypedData() {
        Integer errorPayload = 999;
        ApiErrorResponse<Integer> response = ApiErrorResponse.of(422, "Lỗi xử lý", errorPayload);

        assertEquals(422, response.statusCode());
        assertEquals("Lỗi xử lý", response.message());
        assertEquals(999, response.data());
        assertNull(response.errors());
    }

    @Test
    @DisplayName("ApiErrorResponse.ofError should set message or fallback error")
    void apiErrorResponse_ofError_handlesMessageAndFallback() {
        ApiErrorResponse<Void> res1 = ApiErrorResponse.ofError(400, "Thông báo lỗi", "Bad Request");
        assertEquals(400, res1.statusCode());
        assertEquals("Thông báo lỗi", res1.message());

        ApiErrorResponse<Void> res2 = ApiErrorResponse.ofError(404, "", "Not Found");
        assertEquals(404, res2.statusCode());
        assertEquals("Not Found", res2.message());
    }

    @Test
    @DisplayName("ApiErrorResponse.badRequest with details list maps to errors")
    void apiErrorResponse_badRequestWithDetails_mapsToValidationErrors() {
        List<String> details = List.of("Trường A sai", "Trường B sai");
        ApiErrorResponse<Void> response = ApiErrorResponse.badRequestWithDetails("Lỗi xác thực", details);

        assertEquals(400, response.statusCode());
        assertNotNull(response.errors());
        assertEquals(2, response.errors().size());
        assertEquals("Trường A sai", response.errors().get(0).message());
    }
}


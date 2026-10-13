package com.hmp.sales.common.exception;

import com.hmp.sales.common.response.ApiErrorResponse;
import com.hmp.sales.common.response.ValidationErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * Bộ xử lý ngoại lệ tập trung (Centralized Exception Handler) cho toàn bộ ứng dụng REST API.
 * Đảm bảo mọi lỗi đều được bắt và định dạng nhất quán theo chuẩn ApiErrorResponse
 * với thông điệp bằng tiếng Việt rõ ràng, mã trạng thái HTTP chuẩn xác và không để lộ thông tin nhạy cảm.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Xử lý ngoại lệ khi không tìm thấy tài nguyên trong hệ thống (404 Not Found).
     *
     * @param ex Ngoại lệ ResourceNotFoundException.
     * @return Phản hồi lỗi với mã HTTP 404 và thông điệp lỗi.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiErrorResponse.of(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    /**
     * Xử lý ngoại lệ khi chuyển đổi trạng thái đơn hàng không hợp lệ (409 Conflict).
     *
     * @param ex Ngoại lệ InvalidOrderStateException.
     * @return Phản hồi lỗi với mã HTTP 409 và thông điệp lỗi.
     */
    @ExceptionHandler(InvalidOrderStateException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidOrderState(InvalidOrderStateException ex) {
        log.warn("Invalid order state transition: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiErrorResponse.of(HttpStatus.CONFLICT.value(), ex.getMessage()));
    }

    /**
     * Xử lý ngoại lệ khi người dùng thực hiện thao tác không được phép (403 Forbidden).
     *
     * @param ex Ngoại lệ UnauthorizedOperationException.
     * @return Phản hồi lỗi với mã HTTP 403 và thông điệp lỗi.
     */
    @ExceptionHandler(UnauthorizedOperationException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorizedOperation(UnauthorizedOperationException ex) {
        log.warn("Unauthorized operation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiErrorResponse.of(HttpStatus.FORBIDDEN.value(), ex.getMessage()));
    }

    /**
     * Xử lý ngoại lệ khi xác thực mã OTP thất bại hoặc mã đã hết hạn (400 Bad Request).
     *
     * @param ex Ngoại lệ OtpVerificationException.
     * @return Phản hồi lỗi với mã HTTP 400 và thông điệp lỗi.
     */
    @ExceptionHandler(OtpVerificationException.class)
    public ResponseEntity<ApiErrorResponse> handleOtpVerification(OtpVerificationException ex) {
        log.warn("OTP verification failure: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiErrorResponse.of(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    /**
     * Xử lý ngoại lệ vi phạm quy tắc nghiệp vụ chung (400 Bad Request).
     *
     * @param ex Ngoại lệ BusinessRuleException.
     * @return Phản hồi lỗi với mã HTTP 400 và thông điệp lỗi.
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessRule(BusinessRuleException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiErrorResponse.of(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    /**
     * Xử lý lỗi validation dữ liệu khi kiểm tra @Valid trên Request Body DTO (400 Bad Request).
     * Thu thập danh sách chi tiết các trường vi phạm (field name và message).
     *
     * @param ex Ngoại lệ MethodArgumentNotValidException do Spring ném ra khi validation DTO thất bại.
     * @return Phản hồi lỗi với mã HTTP 400 kèm danh sách ValidationErrorResponse.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<ValidationErrorResponse> validationErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> new ValidationErrorResponse(error.getField(), error.getDefaultMessage()))
            .toList();

        log.warn("Validation failed: {} field errors", validationErrors.size());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Dữ liệu yêu cầu không hợp lệ.",
                validationErrors
            ));
    }

    /**
     * Xử lý lỗi vi phạm ràng buộc validation trên tham số query/path param (400 Bad Request).
     *
     * @param ex Ngoại lệ ConstraintViolationException do Hibernate Validator ném ra.
     * @return Phản hồi lỗi với mã HTTP 400 kèm danh sách ValidationErrorResponse.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<ValidationErrorResponse> validationErrors = ex.getConstraintViolations()
            .stream()
            .map(violation -> new ValidationErrorResponse(
                violation.getPropertyPath().toString(),
                violation.getMessage()
            ))
            .toList();

        log.warn("Constraint violation: {} errors", validationErrors.size());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Dữ liệu tham số không hợp lệ.",
                validationErrors
            ));
    }

    /**
     * Xử lý lỗi khi người dùng không đủ quyền truy cập tài nguyên bảo vệ bởi Spring Security (403 Forbidden).
     *
     * @param ex Ngoại lệ AccessDeniedException của Spring Security.
     * @return Phản hồi lỗi với mã HTTP 403 và thông điệp chuẩn tiếng Việt.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                "Bạn không có quyền thực hiện hành động này."
            ));
    }

    /**
     * Xử lý lỗi khi người dùng chưa xác thực hoặc thông tin đăng nhập không hợp lệ (401 Unauthorized).
     *
     * @param ex Ngoại lệ AuthenticationException của Spring Security.
     * @return Phản hồi lỗi với mã HTTP 401 và thông điệp chuẩn tiếng Việt.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(AuthenticationException ex) {
        log.warn("Authentication failure: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                "Yêu cầu chưa được xác thực hoặc thông tin xác thực không hợp lệ."
            ));
    }

    /**
     * Xử lý lỗi khi client truy cập vào đường dẫn endpoint không tồn tại trên server (404 Not Found).
     *
     * @param ex Ngoại lệ NoResourceFoundException do Spring 6+ ném ra khi không tìm thấy route/handler.
     * @return Phản hồi lỗi với mã HTTP 404 và thông điệp chuẩn tiếng Việt.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                "Đường dẫn tài nguyên không tồn tại."
            ));
    }

    /**
     * Xử lý lỗi khi client gọi sai phương thức HTTP (ví dụ: POST vào route chỉ hỗ trợ GET) (405 Method Not Allowed).
     *
     * @param ex Ngoại lệ HttpRequestMethodNotSupportedException do Spring ném ra.
     * @return Phản hồi lỗi với mã HTTP 405 và thông điệp chuẩn tiếng Việt.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(ApiErrorResponse.of(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                "Phương thức HTTP không được hỗ trợ cho tài nguyên này."
            ));
    }

    /**
     * Xử lý tất cả các ngoại lệ không mong muốn chưa được xử lý cụ thể (500 Internal Server Error).
     * Ghi log đầy đủ stacktrace phục vụ điều tra nhưng trả về thông điệp chung an toàn cho client,
     * đảm bảo không để lộ thông tin nhạy cảm của hệ thống.
     *
     * @param ex Ngoại lệ không lường trước.
     * @return Phản hồi lỗi với mã HTTP 500 và thông điệp an toàn chuẩn tiếng Việt.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception ex) {
        log.error("Unhandled internal server error: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Đã xảy ra lỗi không mong muốn trên hệ thống. Vui lòng thử lại sau."
            ));
    }
}

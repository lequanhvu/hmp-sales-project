package com.hmp.sales.security;

import tools.jackson.databind.ObjectMapper;
import com.hmp.sales.common.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Xử lý ngoại lệ phân quyền (Authorization Failure) ở tầng Spring Security Filter Chain.
 * <p>
 * Khi người dùng đã xác thực danh tính thành công (đã có token hợp lệ) nhưng không có
 * đủ quyền hạn hoặc vai trò để truy cập tài nguyên được bảo vệ (ném ra {@link AccessDeniedException}),
 * handler này chặn lại và trả về phản hồi lỗi định dạng JSON {@link ApiErrorResponse}
 * với mã trạng thái HTTP 403 Forbidden thay vì trả về trang lỗi HTML mặc định của servlet.
 * </p>
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    /**
     * Khởi tạo handler với phụ thuộc ObjectMapper để tuần tự hóa dữ liệu JSON.
     *
     * @param objectMapper đối tượng chuyển đổi Java object sang chuỗi JSON
     */
    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Can thiệp và ghi phản hồi lỗi 403 Forbidden về client khi xảy ra ngoại lệ từ chối truy cập.
     *
     * @param request               đối tượng yêu cầu HTTP từ client
     * @param response              đối tượng phản hồi HTTP gửi về client
     * @param accessDeniedException ngoại lệ từ chối truy cập do Spring Security ném ra
     * @throws IOException nếu xảy ra lỗi ghi dữ liệu vào luồng phản hồi
     */
    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException accessDeniedException
    ) throws IOException {
        // Thiết lập mã trạng thái HTTP là 403 Forbidden
        response.setStatus(HttpStatus.FORBIDDEN.value());
        // Đặt kiểu nội dung trả về là JSON và hỗ trợ bảng mã tiếng Việt UTF-8
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // Khởi tạo đối tượng ApiErrorResponse chuẩn theo hợp đồng API
        ApiErrorResponse errorResponse = ApiErrorResponse.of(
            HttpStatus.FORBIDDEN.value(),
            "Bạn không có quyền thực hiện hành động này."
        );

        // Tuần tự hóa đối tượng lỗi thành chuỗi JSON và ghi thẳng ra response stream
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}

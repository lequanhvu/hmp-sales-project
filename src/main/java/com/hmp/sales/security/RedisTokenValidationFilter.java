package com.hmp.sales.security;

import tools.jackson.databind.ObjectMapper;
import com.hmp.sales.common.response.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Bộ lọc bảo mật xác thực trạng thái hoạt động của Access Token qua Redis.
 * <p>
 * Sau khi Spring Security giải mã JWT và kiểm tra tính hợp lệ về mặt chữ ký số,
 * bộ lọc này can thiệp để kiểm tra xem định danh {@code jti} của token còn tồn tại
 * trong Redis hay không (với tiền tố {@code auth:access:{jti}}).
 * </p>
 * <p>
 * Cơ chế này giúp hệ thống hỗ trợ thu hồi token tức thì (Token Revocation / Logout)
 * mà không phải chuyển đổi hoàn toàn sang mô hình Stateful Session truyền thống.
 * </p>
 */
@Component
public class RedisTokenValidationFilter extends OncePerRequestFilter {

    /**
     * Tiền tố key Redis lưu trữ Access Token còn hiệu lực theo đặc tả thiết kế.
     */
    public static final String REDIS_ACCESS_TOKEN_PREFIX = "auth:access:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Khởi tạo bộ lọc với các phụ thuộc cần thiết.
     *
     * @param stringRedisTemplate đối tượng thao tác với Redis dạng chuỗi
     * @param objectMapper        đối tượng chuyển đổi dữ liệu phản hồi sang định dạng JSON
     */
    public RedisTokenValidationFilter(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Thực hiện kiểm tra sự tồn tại của token trong Redis trên từng HTTP request.
     * <p>
     * - Nếu request không có thông tin xác thực JWT (request công khai): Cho phép đi tiếp.<br>
     * - Nếu request chứa JWT: Kiểm tra mã {@code jti} trên Redis. Nếu không tồn tại hoặc đã hết hạn,
     *   lập tức xóa thông tin ngữ cảnh bảo mật và trả về mã lỗi 401 Unauthorized kèm thông báo chuẩn.<br>
     * - Nếu token còn hiệu lực trong Redis: Cho phép request tiếp tục vào filter chain tiếp theo.
     * </p>
     *
     * @param request     đối tượng yêu cầu HTTP từ client
     * @param response    đối tượng phản hồi HTTP gửi về client
     * @param filterChain chuỗi các bộ lọc tiếp theo cần thực thi
     * @throws ServletException nếu xảy ra lỗi trong quá trình xử lý của servlet filter
     * @throws IOException      nếu xảy ra lỗi đọc/ghi dữ liệu phản hồi
     */
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        // Lấy thông tin xác thực hiện tại đã được thiết lập bởi Spring Security
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Chỉ kiểm tra khi request đã được xác thực bằng JWT Bearer Token
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof Jwt jwt) {
            String jti = jwt.getId();
            if (jti == null) {
                jti = jwt.getClaimAsString("jti");
            }

            // Kiểm tra xem token có chứa jti hợp lệ và key tương ứng có còn tồn tại trên Redis không
            if (jti == null || Boolean.FALSE.equals(stringRedisTemplate.hasKey(REDIS_ACCESS_TOKEN_PREFIX + jti))) {
                // Xóa thông tin xác thực khỏi thread hiện tại
                SecurityContextHolder.clearContext();

                // Thiết lập phản hồi lỗi 401 với định dạng JSON chuẩn ApiErrorResponse
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");

                ApiErrorResponse errorResponse = ApiErrorResponse.of(
                    HttpStatus.UNAUTHORIZED.value(),
                    "Phiên đăng nhập đã hết hạn hoặc token đã bị thu hồi."
                );

                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                // Dừng filter chain tại đây, không cho request đi tiếp vào Controller
                return;
            }
        }

        // Cho phép request tiếp tục đi tiếp nếu không dùng JWT hoặc token còn active trong Redis
        filterChain.doFilter(request, response);
    }
}

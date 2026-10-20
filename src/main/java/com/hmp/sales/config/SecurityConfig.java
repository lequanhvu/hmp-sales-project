package com.hmp.sales.config;

import com.hmp.sales.security.PermissionAuthorizationManager;
import com.hmp.sales.security.RedisTokenValidationFilter;
import com.hmp.sales.security.RestAccessDeniedHandler;
import com.hmp.sales.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Cấu hình bảo mật ứng dụng với Spring Security 7 và OAuth2 Resource Server.
 * Thiết lập các quy tắc phân quyền, vô hiệu hóa session (stateless), cấu hình
 * CORS,
 * xử lý ngoại lệ xác thực/phân quyền và tích hợp RedisTokenValidationFilter.
 */
@Configuration
@EnableWebSecurity
// @EnableMethodSecurity
public class SecurityConfig {

    /**
     * Danh sách các đường dẫn API công khai cho mọi HTTP method, không yêu cầu xác
     * thực.
     */
    public static final String[] WHITELIST = {
            "/",
            "/error",
            "/favicon.ico",
            "/api/v1/auth/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    /**
     * Danh sách các đường dẫn API công khai chỉ áp dụng cho phương thức GET (xem
     * danh mục, sản phẩm).
     */
    public static final String[] PUBLIC_GET_WHITELIST = {
            "/api/v1/categories/**",
            "/api/v1/products/**"
    };

    private final AppProperties appProperties;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final RedisTokenValidationFilter redisTokenValidationFilter;
    private final PermissionAuthorizationManager permissionAuthorizationManager;

    /**
     * Khởi tạo cấu hình bảo mật với các phụ thuộc cần thiết.
     *
     * @param appProperties                  chứa các tham số cấu hình hệ thống (JWT, CORS, OTP)
     * @param authenticationEntryPoint       xử lý phản hồi khi chưa xác thực (401 Unauthorized)
     * @param accessDeniedHandler            xử lý phản hồi khi không đủ quyền hạn (403 Forbidden)
     * @param redisTokenValidationFilter     bộ lọc kiểm tra trạng thái hoạt động của token trên Redis
     * @param permissionAuthorizationManager bộ quản lý phân quyền động phía server từ cache RAM
     */
    public SecurityConfig(
            AppProperties appProperties,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler,
            RedisTokenValidationFilter redisTokenValidationFilter,
            PermissionAuthorizationManager permissionAuthorizationManager) {
        this.appProperties = appProperties;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.redisTokenValidationFilter = redisTokenValidationFilter;
        this.permissionAuthorizationManager = permissionAuthorizationManager;
    }

    /**
     * Thiết lập chuỗi các bộ lọc bảo mật (Security Filter Chain) chính của ứng dụng.
     * <p>
     * Cấu hình bao gồm:
     * <ul>
     * <li>Vô hiệu hóa CSRF do hệ thống xây dựng theo mô hình REST API stateless sử dụng JWT.</li>
     * <li>Kích hoạt CORS theo chính sách cấu hình trong file {@code application.yaml}.</li>
     * <li>Thiết lập Session Policy là {@code STATELESS} (không lưu phiên đăng nhập trong HttpSession).</li>
     * <li>Cấu hình xử lý ngoại lệ tập trung cho 401 và 403 bằng các REST handler tùy biến.</li>
     * <li>Phân quyền URL: Mở công khai các endpoint tra cứu trong whitelist, tất cả request còn lại
     * kiểm tra quyền động qua {@link PermissionAuthorizationManager}.</li>
     * <li>Đăng ký OAuth2 Resource Server để giải mã và xác minh chữ ký Bearer JWT.</li>
     * <li>Gắn bộ lọc {@link RedisTokenValidationFilter} ngay sau bộ lọc xác thực token của Spring
     * để kiểm tra xem token có bị thu hồi trên Redis hay không.</li>
     * </ul>
     * </p>
     *
     * @param http đối tượng cấu hình bảo mật {@link HttpSecurity} của Spring Security
     * @return chuỗi lọc bảo mật {@link SecurityFilterChain} đã được đóng gói hoàn chỉnh
     * @throws Exception nếu xảy ra lỗi trong quá trình khởi tạo cấu hình bảo mật
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Tắt CSRF vì REST API giao tiếp qua Header Authorization (stateless), không dùng Cookie session
                .csrf(AbstractHttpConfigurer::disable)
                // Kích hoạt cấu hình CORS từ bean corsConfigurationSource()
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Không duy trì HTTP Session phía server (Stateless)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Xử lý lỗi trả về JSON chuẩn khi vi phạm xác thực hoặc ủy quyền ở tầng Filter
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                // Cấu hình quy tắc phân quyền cho các endpoint HTTP
                .authorizeHttpRequests(auth -> auth
                        // Cho phép tất cả request OPTIONS (Pre-flight request của trình duyệt khi gọi CORS)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Cho phép truy cập không cần token vào các endpoint trong danh sách WHITELIST
                        .requestMatchers(WHITELIST).permitAll()
                        // Cho phép người dùng chưa đăng nhập chỉ xem danh mục và sản phẩm (phương thức GET)
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET_WHITELIST).permitAll()
                        // Tất cả các request còn lại được kiểm tra quyền động qua PermissionAuthorizationManager
                        .anyRequest().access(permissionAuthorizationManager))
                // Cấu hình Spring Security đóng vai trò OAuth2 Resource Server nhận Bearer
                // Token (JWT)
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                // Thêm bộ lọc kiểm tra trạng thái token trong Redis ngay sau bộ lọc giải mã JWT
                .addFilterAfter(redisTokenValidationFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Khởi tạo bean {@link JwtDecoder} để giải mã và xác thực chữ ký JWT bằng thuật
     * toán HMAC-SHA256.
     * Khóa bí mật được lấy động từ cấu hình {@code app.jwt.secret}.
     *
     * @return đối tượng {@link JwtDecoder} của Nimbus dùng cho việc kiểm tra tính
     *         toàn vẹn của token
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        byte[] secretBytes = appProperties.jwt().secret().getBytes(StandardCharsets.UTF_8);
        SecretKey secretKey = new SecretKeySpec(secretBytes, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }

    /**
     * Thiết lập cấu hình CORS (Cross-Origin Resource Sharing) cho toàn bộ ứng dụng.
     * Đọc các thông số về Origins, Methods, Headers và Allow-Credentials từ file
     * cấu hình.
     *
     * @return đối tượng {@link CorsConfigurationSource} chứa các luật CORS theo
     *         từng pattern URL
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        AppProperties.CorsProperties corsProps = appProperties.cors();

        if (corsProps != null) {
            config.setAllowedOrigins(corsProps.allowedOrigins());
            config.setAllowedMethods(corsProps.allowedMethods());
            if (corsProps.allowedHeaders() != null && corsProps.allowedHeaders().contains("*")) {
                config.addAllowedHeader("*");
            } else {
                config.setAllowedHeaders(corsProps.allowedHeaders());
            }
            config.setAllowCredentials(corsProps.allowCredentials());
        } else {
            config.addAllowedOriginPattern("*");
            config.addAllowedMethod("*");
            config.addAllowedHeader("*");
            config.setAllowCredentials(true);
        }

        // Áp dụng chính sách CORS này cho toàn bộ đường dẫn trên server
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

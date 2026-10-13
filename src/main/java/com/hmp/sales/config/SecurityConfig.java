package com.hmp.sales.config;

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
 * Thiết lập các quy tắc phân quyền, vô hiệu hóa session (stateless), cấu hình CORS,
 * xử lý ngoại lệ xác thực/phân quyền và tích hợp RedisTokenValidationFilter.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Danh sách các đường dẫn API công khai cho mọi HTTP method, không yêu cầu xác thực.
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
     * Danh sách các đường dẫn API công khai chỉ áp dụng cho phương thức GET (xem danh mục, sản phẩm).
     */
    public static final String[] PUBLIC_GET_WHITELIST = {
        "/api/v1/categories/**",
        "/api/v1/products/**"
    };

    private final AppProperties appProperties;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final RedisTokenValidationFilter redisTokenValidationFilter;

    public SecurityConfig(
        AppProperties appProperties,
        RestAuthenticationEntryPoint authenticationEntryPoint,
        RestAccessDeniedHandler accessDeniedHandler,
        RedisTokenValidationFilter redisTokenValidationFilter
    ) {
        this.appProperties = appProperties;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.redisTokenValidationFilter = redisTokenValidationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(WHITELIST).permitAll()
                .requestMatchers(HttpMethod.GET, PUBLIC_GET_WHITELIST).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults())
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .addFilterAfter(redisTokenValidationFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        byte[] secretBytes = appProperties.jwt().secret().getBytes(StandardCharsets.UTF_8);
        SecretKey secretKey = new SecretKeySpec(secretBytes, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }

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

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

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

@Component
public class RedisTokenValidationFilter extends OncePerRequestFilter {

    public static final String REDIS_ACCESS_TOKEN_PREFIX = "auth:access:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisTokenValidationFilter(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof Jwt jwt) {
            String jti = jwt.getId();
            if (jti == null) {
                jti = jwt.getClaimAsString("jti");
            }

            if (jti == null || Boolean.FALSE.equals(stringRedisTemplate.hasKey(REDIS_ACCESS_TOKEN_PREFIX + jti))) {
                SecurityContextHolder.clearContext();

                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");

                ApiErrorResponse errorResponse = ApiErrorResponse.of(
                    HttpStatus.UNAUTHORIZED.value(),
                    "Phiên đăng nhập đã hết hạn hoặc token đã bị thu hồi."
                );

                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}

package com.hmp.sales.security;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisTokenValidationFilterTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private FilterChain filterChain;

    private RedisTokenValidationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        filter = new RedisTokenValidationFilter(stringRedisTemplate, objectMapper);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Unauthenticated request should proceed down filter chain")
    void unauthenticatedRequest_proceeds() throws ServletException, IOException {
        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Active token in Redis should proceed down filter chain")
    void activeTokenInRedis_proceeds() throws ServletException, IOException {
        String jti = "test-jti-123";
        Jwt jwt = new Jwt(
            "mock-token-value",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Map.of("alg", "HS256"),
            Map.of("jti", jti, "sub", "test-user")
        );
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(stringRedisTemplate.hasKey("auth:access:" + jti)).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Revoked or missing token in Redis should return 401 and clear context")
    void revokedTokenInRedis_returns401() throws ServletException, IOException {
        String jti = "revoked-jti-456";
        Jwt jwt = new Jwt(
            "mock-token-value",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Map.of("alg", "HS256"),
            Map.of("jti", jti, "sub", "test-user")
        );
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(stringRedisTemplate.hasKey("auth:access:" + jti)).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Phiên đăng nhập đã hết hạn hoặc token đã bị thu hồi"));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}

package com.hmp.sales.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
    JwtProperties jwt,
    OtpProperties otp,
    OrderProperties order,
    CorsProperties cors
) {
    public record JwtProperties(
        String issuer,
        String secret,
        long expirationMinutes,
        long refreshExpirationDays
    ) {}

    public record OtpProperties(
        int expirySeconds,
        int maxAttempts,
        int resendIntervalSeconds
    ) {}

    public record OrderProperties(
        int cancellationWindowMinutes
    ) {}

    public record CorsProperties(
        List<String> allowedOrigins,
        List<String> allowedMethods,
        List<String> allowedHeaders,
        boolean allowCredentials
    ) {}
}

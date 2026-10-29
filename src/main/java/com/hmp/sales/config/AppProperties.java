package com.hmp.sales.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
    JwtProperties jwt,
    OtpProperties otp,
    OrderProperties order,
    CorsProperties cors,
    PaginationProperties pagination,
    ProductProperties product
) {
    public AppProperties {
        if (pagination == null) {
            pagination = new PaginationProperties(0, 20, 100);
        }
        if (product == null) {
            product = new ProductProperties(1, 10);
        }
    }

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

    public record PaginationProperties(
        int defaultPage,
        int defaultPageSize,
        int maxPageSize
    ) {
        public PaginationProperties {
            if (defaultPageSize <= 0) defaultPageSize = 20;
            if (maxPageSize <= 0) maxPageSize = 100;
        }
    }

    public record ProductProperties(
        int maxPrimaryImages,
        int maxSecondaryImages
    ) {
        public ProductProperties {
            if (maxPrimaryImages <= 0) maxPrimaryImages = 1;
            if (maxSecondaryImages <= 0) maxSecondaryImages = 10;
        }

        public int maxTotalImages() {
            return maxPrimaryImages + maxSecondaryImages;
        }
    }
}

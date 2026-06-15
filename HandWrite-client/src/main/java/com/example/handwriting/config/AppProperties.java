package com.example.handwriting.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 应用级配置（JWT / CORS / Storage / RateLimit）
 */
@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private Storage storage = new Storage();
    private RateLimit ratelimit = new RateLimit();

    @Data
    public static class Jwt {
        /** Base64 编码后的密钥 */
        private String secret;
        private String issuer = "handwriting-client";
        private long accessTokenTtlSeconds = 900L;
        private long refreshTokenTtlSeconds = 604800L;
        private String header = "Authorization";
        private String prefix = "Bearer ";
    }

    @Data
    public static class Cors {
        private String allowedOrigins = "*";
        private String allowedMethods = "GET,POST,PUT,DELETE,OPTIONS";
        private String allowedHeaders = "*";
        private boolean allowCredentials = true;
        private long maxAge = 3600L;
    }

    @Data
    public static class Storage {
        /** minio / s3 */
        private String provider = "minio";
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String region = "us-east-1";
        private String bucketCollect;
        private String bucketAudit;
        private String bucketBackup;
        private long presignExpireSeconds = 600L;
        private String publicBaseUrl;
    }

    @Data
    public static class RateLimit {
        private int authPerMinute = 20;
        private int uploadPerMinute = 60;
    }
}

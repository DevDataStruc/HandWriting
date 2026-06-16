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
        /** minio / s3 / azure */
        private String provider = "minio";

        // ---- 通用 ----
        /** 通用 Bucket/Container 名称（按 purpose 区分：采集/审核/备份） */
        private String bucketCollect;
        private String bucketAudit;
        private String bucketBackup;
        /** 签名过期时间（秒） */
        private long presignExpireSeconds = 600L;
        /** 公开访问的 CDN 基地址（可选） */
        private String publicBaseUrl;

        // ---- MinIO / S3 ----
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String region = "us-east-1";

        // ---- Azure Blob Storage ----
        /** Azure 存储账号名（e.g. myhandwriting） */
        private String azureAccountName;
        /** Azure 存储账号访问密钥（在 Azure Portal → 存储账户 → 访问密钥 中获取） */
        private String azureAccountKey;
        /** SAS 令牌（可选，代替账号密钥） */
        private String azureSasToken;
        /** 完整连接字符串（可选，最高优先级） */
        private String azureConnectionString;
        /** Azure 云环境：china（中国）/ global（国际）/ usgov（美国政府） */
        private String azureCloud = "china";
    }

    @Data
    public static class RateLimit {
        private int authPerMinute = 20;
        private int uploadPerMinute = 60;
    }
}

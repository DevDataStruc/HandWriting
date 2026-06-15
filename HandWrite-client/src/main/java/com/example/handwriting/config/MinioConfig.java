package com.example.handwriting.config;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO / S3 客户端配置
 */
@Configuration
public class MinioConfig {

    private final AppProperties appProperties;

    public MinioConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Bean
    public MinioClient minioClient() {
        AppProperties.Storage s = appProperties.getStorage();
        return MinioClient.builder()
                .endpoint(s.getEndpoint())
                .credentials(s.getAccessKey(), s.getSecretKey())
                .region(s.getRegion())
                .build();
    }
}

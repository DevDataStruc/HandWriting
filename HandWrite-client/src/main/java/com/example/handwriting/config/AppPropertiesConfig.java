package com.example.handwriting.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 启用 AppProperties 配置类
 */
@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class AppPropertiesConfig {
}

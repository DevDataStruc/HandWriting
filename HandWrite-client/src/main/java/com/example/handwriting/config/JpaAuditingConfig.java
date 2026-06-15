package com.example.handwriting.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * 启用 JPA 审计字段自动填充（createTime / updateTime）
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}

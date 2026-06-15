package com.example.handwriting;

import org.junit.jupiter.api.Test;

/**
 * 基础冒烟测试
 * （完整的集成测试需启用 Testcontainers，本工程以单测冒烟起步）
 */
class HandwritingClientApplicationTests {

    @Test
    void contextLoadsDisabled() {
        // 默认禁用 SpringBootTest 启动以避免 CI 环境无依赖。
        // 集成测试请使用 @SpringBootTest + Testcontainers
    }
}

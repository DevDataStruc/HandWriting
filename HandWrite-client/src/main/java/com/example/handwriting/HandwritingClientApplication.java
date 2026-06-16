package com.example.handwriting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Map;

/**
 * 手写体收集和管理系统 - 客户端 API 启动类
 *
 * @author handwriting-team
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class HandwritingClientApplication {

    static {
        // 兜底：把 System.getenv() 中的 HANDWRITE_* 复制到 System properties
        // 解决 IntelliJ 启动时缓存 env vars 导致后续 setx 不可见的问题
        try {
            String prefix = "HANDWRITE_";
            int count = 0;
            for (Map.Entry<String, String> e : System.getenv().entrySet()) {
                if (e.getKey().startsWith(prefix) && e.getValue() != null) {
                    if (System.getProperty(e.getKey()) == null) {
                        System.setProperty(e.getKey(), e.getValue());
                        count++;
                    }
                }
            }
            if (count > 0) {
                System.out.println("[HandwriteBootstrap] injected " + count + " HANDWRITE_* system properties from System.getenv()");
            }
        } catch (SecurityException ignored) {
            // 安全管理器拒绝
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(HandwritingClientApplication.class, args);
    }
}

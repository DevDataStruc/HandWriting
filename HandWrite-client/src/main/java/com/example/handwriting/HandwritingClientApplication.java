package com.example.handwriting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 手写体收集和管理系统 - 客户端 API 启动类
 *
 * @author handwriting-team
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class HandwritingClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(HandwritingClientApplication.class, args);
    }
}

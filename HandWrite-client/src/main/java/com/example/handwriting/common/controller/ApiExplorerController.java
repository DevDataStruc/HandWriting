package com.example.handwriting.common.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * API 端点浏览器：访问 /endpoints 返回一个静态 HTML 页面，
 * 用于浏览后端暴露的全部 REST 端点。
 */
@RestController
public class ApiExplorerController {

    @GetMapping(value = "/endpoints", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> endpoints() throws Exception {
        ClassPathResource resource = new ClassPathResource("static/endpoints.html");
        String html = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }
}

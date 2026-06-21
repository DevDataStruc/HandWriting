package com.example.handwriting.file.controller;

import com.example.handwriting.file.service.LocalFileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 本地文件访问接口（公开）
 *
 * <p>{@code GET /v1/file/local/{userId}/{charId}/{filename}}
 *
 * <p>由 SecurityConfig 放行 GET 访问。安全性依赖 UUID 文件名的不可猜测性，
 * 与原对象存储的"公开 CDN"语义一致，便于前端 &lt;img src&gt; 直接加载。
 */
@Slf4j
@RestController
@RequestMapping("/v1/file/local")
@RequiredArgsConstructor
public class LocalFileController {

    private final LocalFileStorageService storageService;

    @GetMapping("/{userId}/{charId}/{filename:.+}")
    public ResponseEntity<Resource> serve(
            @PathVariable Long userId,
            @PathVariable Long charId,
            @PathVariable String filename
    ) {
        String relativeKey = userId + "/" + charId + "/" + filename;
        Path file = storageService.resolveSafe(relativeKey);

        Resource resource = new PathResource(file);
        MediaType mediaType = probeMediaType(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, mediaType.toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .body(resource);
    }

    private MediaType probeMediaType(Path file) {
        try {
            String ct = Files.probeContentType(file);
            if (ct != null) return MediaType.parseMediaType(ct);
        } catch (IOException ignored) {
        }
        String name = file.getFileName().toString().toLowerCase();
        if (name.endsWith(".png"))  return MediaType.IMAGE_PNG;
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (name.endsWith(".svg"))  return MediaType.valueOf("image/svg+xml");
        if (name.endsWith(".webp")) return MediaType.valueOf("image/webp");
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}

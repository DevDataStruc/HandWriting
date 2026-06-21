package com.example.handwriting.file.service;

import com.example.handwriting.common.exception.BizException;
import com.example.handwriting.common.result.ErrorCode;
import com.example.handwriting.config.AppProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

/**
 * 本地文件系统存储服务
 *
 * <p>目录结构：{@code <basePath>/<userId>/<charId>/<timestamp>_<uuid>.<ext>}
 *
 * <p>同一字符（charId）允许存在多份样本（多文件），文件名以时间戳 + UUID 保证唯一。
 *
 * <p>激活条件：当 {@code app.storage.provider=local}（默认）时启用，由 {@code SampleService} 调用，
 * 不再走对象存储签名/直传链路。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalFileStorageService {

    private final AppProperties appProperties;

    private Path basePath;

    @PostConstruct
    void init() throws IOException {
        String configured = appProperties.getStorage().getLocal().getBasePath();
        Path p = Paths.get(configured);
        this.basePath = p.isAbsolute() ? p : Paths.get(System.getProperty("user.dir"), configured).toAbsolutePath().normalize();
        Files.createDirectories(this.basePath);
        log.info("[LocalFileStorage] 存储根目录: {}", this.basePath);
    }

    /**
     * 保存上传文件。
     *
     * @param userId 当前用户 ID（用于隔离目录）
     * @param charId 字符 ID（用于按字符聚合文件）
     * @param file   上传文件
     * @return 文件相对 key，格式 {@code <userId>/<charId>/<filename>}
     */
    public StoredFile store(Long userId, Long charId, MultipartFile file) {
        validate(file);
        String ext = resolveExt(file);
        validateExt(ext);

        String filename = buildFilename(ext);
        Path target = userDir(userId).resolve(String.valueOf(charId)).resolve(filename);
        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("[LocalFileStorage] 写入文件失败 userId={} charId={}", userId, charId, e);
            throw new BizException(ErrorCode.FILE_UPLOAD_FAILED, e.getMessage());
        }

        String relativeKey = userId + "/" + charId + "/" + filename;
        String accessUrl = buildAccessUrl(relativeKey);
        log.debug("[LocalFileStorage] 已保存 key={} size={}", relativeKey, file.getSize());
        return new StoredFile(relativeKey, accessUrl, file.getSize(), ext);
    }

    /**
     * 把相对 key 解析为安全绝对路径（防止目录穿越）
     */
    public Path resolveSafe(String relativeKey) {
        if (relativeKey == null || relativeKey.isBlank()) {
            throw new BizException(ErrorCode.NOT_FOUND, "文件 key 为空");
        }
        Path resolved = basePath.resolve(relativeKey).normalize();
        if (!resolved.startsWith(basePath)) {
            throw new BizException(ErrorCode.FORBIDDEN, "非法的文件路径");
        }
        if (!Files.exists(resolved) || !Files.isRegularFile(resolved)) {
            throw new BizException(ErrorCode.NOT_FOUND, "文件不存在");
        }
        return resolved;
    }

    /**
     * 删除文件（忽略不存在的情况，避免影响主流程）
     */
    public void deleteQuietly(String relativeKey) {
        if (relativeKey == null || relativeKey.isBlank()) return;
        try {
            Path p = basePath.resolve(relativeKey).normalize();
            if (p.startsWith(basePath)) Files.deleteIfExists(p);
        } catch (IOException e) {
            log.warn("[LocalFileStorage] 删除文件失败 key={}: {}", relativeKey, e.getMessage());
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.FILE_EMPTY);
        }
        long max = appProperties.getStorage().getLocal().getMaxFileSize();
        if (file.getSize() > max) {
            throw new BizException(ErrorCode.FILE_SIZE_EXCEED,
                    "文件大小 " + file.getSize() + " 超过限制 " + max);
        }
    }

    private String resolveExt(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            return original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        }
        String ct = file.getContentType();
        if (ct != null && ct.contains("/")) {
            return ct.substring(ct.indexOf('/') + 1).toLowerCase(Locale.ROOT);
        }
        return "png";
    }

    private void validateExt(String ext) {
        if (!appProperties.getStorage().getLocal().getAllowedExt().contains(ext)) {
            throw new BizException(ErrorCode.FILE_TYPE_INVALID, ext);
        }
    }

    private String buildFilename(String ext) {
        return System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "." + ext;
    }

    private Path userDir(Long userId) {
        return basePath.resolve(String.valueOf(userId));
    }

    private String buildAccessUrl(String relativeKey) {
        String prefix = appProperties.getStorage().getLocal().getUrlPrefix();
        if (!prefix.startsWith("/")) prefix = "/" + prefix;
        if (prefix.endsWith("/")) prefix = prefix.substring(0, prefix.length() - 1);
        return prefix + "/" + relativeKey;
    }

    /** 保存结果 */
    public record StoredFile(String fileKey, String accessUrl, long size, String ext) {}
}

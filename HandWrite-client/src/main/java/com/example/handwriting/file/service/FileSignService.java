package com.example.handwriting.file.service;

import com.example.handwriting.common.exception.BizException;
import com.example.handwriting.common.result.ErrorCode;
import com.example.handwriting.config.AppProperties;
import com.example.handwriting.file.dto.FileSignDTO;
import com.example.handwriting.file.dto.FileSignVO;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 对象存储直传签名服务（MinIO 兼容 S3 协议）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileSignService {

    private static final java.util.Set<String> ALLOWED_EXT = java.util.Set.of("png", "jpg", "jpeg", "svg", "webp");

    private final MinioClient minioClient;
    private final AppProperties appProperties;

    public FileSignVO sign(Long userId, FileSignDTO dto) {
        String ext = dto.getExt().toLowerCase();
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BizException(ErrorCode.FILE_TYPE_INVALID, ext);
        }
        String bucket = pickBucket(dto.getPurpose());
        String objectKey = buildObjectKey(userId, dto.getPurpose(), ext);

        long expire = appProperties.getStorage().getPresignExpireSeconds();
        try {
            String url = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(bucket)
                    .object(objectKey)
                    .expiry((int) expire)
                    .build());
            return FileSignVO.builder()
                    .bucket(bucket)
                    .objectKey(objectKey)
                    .uploadUrl(url)
                    .method("PUT")
                    .expireSeconds(expire)
                    .build();
        } catch (Exception e) {
            log.error("生成直传签名失败", e);
            throw new BizException(ErrorCode.REMOTE_ERROR, e.getMessage());
        }
    }

    private String pickBucket(String purpose) {
        return switch (purpose) {
            case "SAMPLE_FILE" -> appProperties.getStorage().getBucketCollect();
            case "SAMPLE_AUDIT_BACKUP" -> appProperties.getStorage().getBucketAudit();
            default -> appProperties.getStorage().getBucketCollect();
        };
    }

    private String buildObjectKey(Long userId, String purpose, String ext) {
        LocalDate now = LocalDate.now();
        return "sample/%s/%02d/%d/%s/%s.%s".formatted(
                now.getYear(), now.getMonthValue(), userId, purpose,
                UUID.randomUUID().toString().replace("-", ""), ext);
    }
}

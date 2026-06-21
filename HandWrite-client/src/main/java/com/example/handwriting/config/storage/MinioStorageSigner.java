package com.example.handwriting.config.storage;

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
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * MinIO / S3 直传签名实现（默认实现）
 *
 * <p>激活规则：当 {@code app.storage.provider} 为 {@code minio}/{@code s3}/未设置时启用。
 * 当 {@code provider=azure} 时，Spring 会优先注入 {@link AzureStorageSigner}（@Primary）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinioStorageSigner implements StorageSigner {

    private static final Set<String> ALLOWED_EXT = Set.of("png", "jpg", "jpeg", "svg", "webp");

    private final MinioClient minioClient;
    private final AppProperties appProperties;

    @Override
    public FileSignVO generateUploadUrl(Long userId, FileSignDTO dto) {
        String ext = dto.getExt().toLowerCase();
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BizException(ErrorCode.FILE_TYPE_INVALID, ext);
        }
        String bucket    = pickBucket(dto.getPurpose());
        String objectKey = buildObjectKey(userId, dto.getPurpose(), ext);
        long expire      = appProperties.getStorage().getPresignExpireSeconds();

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
                    .accessUrl(url.split("\\?")[0])
                    .method("PUT")
                    .expireSeconds(expire)
                    .requiredHeader(null)
                    .build();
        } catch (Exception e) {
            log.error("[MinIO] 生成直传签名失败", e);
            throw new BizException(ErrorCode.REMOTE_ERROR, e.getMessage());
        }
    }

    private String pickBucket(String purpose) {
        return switch (purpose) {
            case "SAMPLE_FILE"            -> appProperties.getStorage().getBucketCollect();
            case "SAMPLE_AUDIT_BACKUP"    -> appProperties.getStorage().getBucketAudit();
            case "SAMPLE_BACKUP"          -> appProperties.getStorage().getBucketBackup();
            default                      -> appProperties.getStorage().getBucketCollect();
        };
    }

    private String buildObjectKey(Long userId, String purpose, String ext) {
        LocalDate now = LocalDate.now();
        return "sample/%s/%02d/%d/%s/%s.%s".formatted(
                now.getYear(), now.getMonthValue(), userId, purpose,
                UUID.randomUUID().toString().replace("-", ""), ext);
    }
}

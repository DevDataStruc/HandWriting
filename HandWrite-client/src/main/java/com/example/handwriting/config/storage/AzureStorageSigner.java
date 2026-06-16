package com.example.handwriting.config.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.example.handwriting.common.exception.BizException;
import com.example.handwriting.common.result.ErrorCode;
import com.example.handwriting.config.AppProperties;
import com.example.handwriting.file.dto.FileSignDTO;
import com.example.handwriting.file.dto.FileSignVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

/**
 * Azure Blob Storage 直传签名实现
 *
 * <p>使用账号密钥在服务端生成 Service SAS URL，
 * 前端拿到 URL 后直接 PUT 上传到 Azure Blob（无需中转服务器带宽）。
 *
 * <p>激活条件：{@code app.storage.provider=azure}
 *
 * <p>参考：
 * <a href="https://docs.azure.cn/zh-cn/storage/blobs/storage-blob-java-get-started">
 *   Azure Blob Storage Java 入门</a>
 * <a href="https://docs.azure.cn/zh-cn/storage/blobs/sas-service-create-java">
 *   使用 Java 创建服务 SAS</a>
 */
@Slf4j
@Primary
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "azure")
public class AzureStorageSigner implements StorageSigner {

    private static final Set<String> ALLOWED_EXT = Set.of("png", "jpg", "jpeg", "svg", "webp");

    /** Azure Block Blob 上传必带的 Header（用于标识 blob 类型） */
    private static final String X_MS_BLOB_TYPE = "BlockBlob";
    private static final String HEADER_X_MS_BLOB_TYPE = "x-ms-blob-type";

    private final BlobServiceClient blobServiceClient;
    private final AppProperties appProperties;

    @Override
    public FileSignVO generateUploadUrl(Long userId, FileSignDTO dto) {
        String ext = dto.getExt().toLowerCase();
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BizException(ErrorCode.FILE_TYPE_INVALID, ext);
        }

        String container = pickContainer(dto.getPurpose());
        String blobName  = buildBlobName(userId, dto.getPurpose(), ext);
        long expire      = appProperties.getStorage().getPresignExpireSeconds();

        try {
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(container);
            BlobClient blobClient = containerClient.getBlobClient(blobName);

            // 构造 SAS：写权限 + 创建权限，作用在 blob 资源上
            OffsetDateTime expiryTime = OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(expire);
            BlobSasPermission perms = new BlobSasPermission()
                    .setWritePermission(true)
                    .setCreatePermission(true);
            BlobServiceSasSignatureValues sasValues =
                    new BlobServiceSasSignatureValues(expiryTime, perms)
                            .setStartTime(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));

            String sasToken = blobClient.generateSas(sasValues);
            String uploadUrl = blobClient.getBlobUrl() + "?" + sasToken;

            log.info("[Azure] 生成上传签名 container={} blob={} 过期={}s",
                    container, blobName, expire);

            return FileSignVO.builder()
                    .bucket(container)   // Azure 中 bucket ≡ container
                    .objectKey(blobName)
                    .uploadUrl(uploadUrl)
                    .method("PUT")
                    .expireSeconds(expire)
                    // 前端 PUT 时必须携带此头，否则 Azure 拒绝
                    .requiredHeader(HEADER_X_MS_BLOB_TYPE + ":" + X_MS_BLOB_TYPE)
                    .build();
        } catch (Exception e) {
            log.error("[Azure] 生成直传签名失败", e);
            throw new BizException(ErrorCode.REMOTE_ERROR, e.getMessage());
        }
    }

    /** purpose → container 映射（Azure 中没有"桶"，只有 container） */
    private String pickContainer(String purpose) {
        return switch (purpose) {
            case "SAMPLE_FILE"            -> appProperties.getStorage().getBucketCollect();
            case "SAMPLE_AUDIT_BACKUP"    -> appProperties.getStorage().getBucketAudit();
            case "SAMPLE_BACKUP"          -> appProperties.getStorage().getBucketBackup();
            default                      -> appProperties.getStorage().getBucketCollect();
        };
    }

    private String buildBlobName(Long userId, String purpose, String ext) {
        LocalDate now = LocalDate.now();
        return "sample/%s/%02d/%d/%s/%s.%s".formatted(
                now.getYear(), now.getMonthValue(), userId, purpose,
                UUID.randomUUID().toString().replace("-", ""), ext);
    }
}

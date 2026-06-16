package com.example.handwriting.config.storage;

import com.example.handwriting.file.dto.FileSignDTO;
import com.example.handwriting.file.dto.FileSignVO;

/**
 * 对象存储直传签名服务抽象接口
 *
 * <p>实现类需根据 app.storage.provider 配置项条件加载：
 * <ul>
 *   <li>{@code minio} / {@code s3} → {@link MinioStorageSigner}</li>
 *   <li>{@code azure} → {@link AzureStorageSigner}</li>
 * </ul>
 */
public interface StorageSigner {

    /**
     * 生成对象直传上传 URL（含临时签名），前端拿到后可直接 PUT 文件。
     *
     * @param userId 当前用户 ID（用于按用户隔离目录）
     * @param dto    业务参数（purpose / ext）
     * @return 包含 bucket/container 名、objectKey、uploadUrl、method、expireSeconds、requiredHeader
     */
    FileSignVO generateUploadUrl(Long userId, FileSignDTO dto);
}

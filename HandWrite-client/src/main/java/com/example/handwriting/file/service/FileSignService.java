package com.example.handwriting.file.service;

import com.example.handwriting.config.storage.StorageSigner;
import com.example.handwriting.file.dto.FileSignDTO;
import com.example.handwriting.file.dto.FileSignVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 文件直传签名服务（Facade）
 *
 * <p>实际签名由 {@link StorageSigner} 实现类完成，根据
 * {@code app.storage.provider} 配置自动注入 MinIO 或 Azure 实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileSignService {

    private final StorageSigner storageSigner;

    public FileSignVO sign(Long userId, FileSignDTO dto) {
        log.debug("用户 {} 申请 {} 文件直传签名", userId, dto.getPurpose());
        return storageSigner.generateUploadUrl(userId, dto);
    }
}

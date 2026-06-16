package com.example.handwriting.config;

import com.azure.core.util.ClientOptions;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.example.handwriting.common.exception.BizException;
import com.example.handwriting.common.result.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Azure Blob Storage 客户端配置
 *
 * <p>仅当 {@code app.storage.provider=azure} 时激活。
 *
 * <p>支持的鉴权方式（按优先级）：
 * <ol>
 *   <li>连接字符串 ({@code app.storage.azure-connection-string})</li>
 *   <li>SAS 令牌 ({@code app.storage.azure-sas-token})</li>
 *   <li>账号密钥 ({@code app.storage.azure-account-name} + {@code azure-account-key})</li>
 *   <li>DefaultAzureCredential（最后兜底，需要本机或托管身份已配置）</li>
 * </ol>
 *
 * <p>Endpoint 解析：
 * <ul>
 *   <li>若显式提供 {@code endpoint}，使用之</li>
 *   <li>否则按 {@code azure-cloud} 自动生成：
 *     <ul>
 *       <li>china  → {@code https://<account>.blob.core.chinacloudapi.cn}</li>
 *       <li>global → {@code https://<account>.blob.core.windows.net}</li>
 *     </ul>
 *   </li>
 * </ul>
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "azure")
public class AzureBlobConfig {

    private final AppProperties appProperties;

    public AzureBlobConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Bean
    public BlobServiceClient blobServiceClient() {
        AppProperties.Storage s = appProperties.getStorage();
        String endpoint = resolveEndpoint(s);

        BlobServiceClientBuilder builder = new BlobServiceClientBuilder()
                .endpoint(endpoint)
                .clientOptions(new ClientOptions().setApplicationId("handwriting-client"));

        // 1) 连接字符串（最高优先级）
        String connStr = s.getAzureConnectionString();
        if (connStr != null && !connStr.isBlank()) {
            log.info("[Azure] 使用 ConnectionString 连接 Blob Storage");
            return builder.connectionString(connStr).buildClient();
        }

        // 2) SAS 令牌
        String sas = s.getAzureSasToken();
        if (sas != null && !sas.isBlank()) {
            log.info("[Azure] 使用 SAS Token 连接 Blob Storage");
            // SAS 需去掉前导 '?'，Azure SDK 会自动拼接
            String normalized = sas.startsWith("?") ? sas.substring(1) : sas;
            return builder.sasToken(normalized).buildClient();
        }

        // 3) 账号密钥
        String accountName = s.getAzureAccountName();
        String accountKey  = s.getAzureAccountKey();
        if (accountName != null && !accountName.isBlank()
                && accountKey != null && !accountKey.isBlank()) {
            log.info("[Azure] 使用 AccountKey 连接 Blob Storage, account={}", accountName);
            StorageSharedKeyCredential credential = new StorageSharedKeyCredential(accountName, accountKey);
            return builder.credential(credential).buildClient();
        }

        // 4) DefaultAzureCredential（Entra ID / 托管身份 / 本地 az login）
        log.warn("[Azure] 未配置账号密钥/SAS/连接串，回退到 DefaultAzureCredential (Entra ID)");
        try {
            return builder.credential(new DefaultAzureCredentialBuilder().build()).buildClient();
        } catch (Exception e) {
            throw new BizException(ErrorCode.CONFIG_ERROR,
                    "Azure Blob Storage 鉴权失败：必须配置 connection-string / sas-token / account-key 之一");
        }
    }

    private String resolveEndpoint(AppProperties.Storage s) {
        if (s.getEndpoint() != null && !s.getEndpoint().isBlank()) {
            return s.getEndpoint();
        }
        String accountName = s.getAzureAccountName();
        if (accountName == null || accountName.isBlank()) {
            throw new BizException(ErrorCode.CONFIG_ERROR,
                    "Azure Storage Account 名称未配置 (AZURE_STORAGE_ACCOUNT)");
        }
        String suffix = switch (String.valueOf(s.getAzureCloud()).toLowerCase()) {
            case "global", "azure", "public" -> "blob.core.windows.net";
            case "china", "cn", "mooncake"   -> "blob.core.chinacloudapi.cn";
            case "usgov", "us-government"    -> "blob.core.usgovcloudapi.net";
            default                          -> "blob.core.chinacloudapi.cn";
        };
        return "https://" + accountName + "." + suffix;
    }
}

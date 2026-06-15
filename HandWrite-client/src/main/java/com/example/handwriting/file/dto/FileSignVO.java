package com.example.handwriting.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "对象存储直传签名")
public class FileSignVO implements Serializable {

    @Schema(description = "对象存储 Bucket")
    private String bucket;

    @Schema(description = "对象 Key（最终路径）")
    private String objectKey;

    @Schema(description = "PUT 方式的上传 URL，前端可直接 PUT 文件")
    private String uploadUrl;

    @Schema(description = "上传方式：PUT")
    private String method = "PUT";

    @Schema(description = "签名过期时间（秒）")
    private long expireSeconds;

    @Schema(description = "请求头限制（如不需要可为空）")
    private String requiredHeader;
}

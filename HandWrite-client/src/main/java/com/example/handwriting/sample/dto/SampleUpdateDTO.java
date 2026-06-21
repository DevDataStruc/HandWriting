package com.example.handwriting.sample.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.io.Serializable;

/**
 * 样本更新 DTO：重新上传笔迹后更新文件元数据
 */
@Data
public class SampleUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 可选：允许在编辑时调整所属字符（通常保持不变） */
    private Long charId;

    @NotBlank
    private String fileKey;

    private String fileUrl;

    @NotNull
    @Positive
    private Integer fileSize;

    private String sha256;

    private String device;
}

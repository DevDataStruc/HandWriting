package com.example.handwriting.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class FileSignDTO implements Serializable {

    /** 业务用途：SAMPLE_AVATAR / SAMPLE_FILE */
    @NotBlank
    private String purpose;

    /** 文件后缀，如 png、jpg、svg */
    @NotBlank
    private String ext;

    /** 业务 id，可选（如 charId） */
    private Long bizId;
}

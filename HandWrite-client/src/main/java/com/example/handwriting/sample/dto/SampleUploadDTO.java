package com.example.handwriting.sample.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.io.Serializable;

@Data
public class SampleUploadDTO implements Serializable {

    @NotNull
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

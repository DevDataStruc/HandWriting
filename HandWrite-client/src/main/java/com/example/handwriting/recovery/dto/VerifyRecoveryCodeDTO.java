package com.example.handwriting.recovery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 使用恢复码验证（一次性 8 位 code，绑定时一次下发 10 个）
 */
@Data
public class VerifyRecoveryCodeDTO implements Serializable {

    @NotBlank
    @Size(min = 8, max = 32)
    private String recoveryCode;
}

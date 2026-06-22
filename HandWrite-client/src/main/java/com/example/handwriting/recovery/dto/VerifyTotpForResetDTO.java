package com.example.handwriting.recovery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 找回密码第二步：使用 TOTP 动态口令验证身份
 */
@Data
public class VerifyTotpForResetDTO implements Serializable {

    @NotBlank
    private String challengeId;

    /** 6 位动态口令（与恢复码二选一） */
    private String code;

    /** 一次性恢复码（与 code 二选一） */
    private String recoveryCode;

    @NotNull
    private Boolean useRecoveryCode = false;
}

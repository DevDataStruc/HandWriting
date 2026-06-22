package com.example.handwriting.recovery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

/**
 * 验证动态口令（绑定确认 / 恢复登录 通用）
 */
@Data
public class VerifyTotpDTO implements Serializable {

    /** 6 位动态口令 */
    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "动态口令为 6 位数字")
    private String code;
}

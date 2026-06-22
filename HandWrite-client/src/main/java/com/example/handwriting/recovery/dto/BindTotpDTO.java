package com.example.handwriting.recovery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

/**
 * 确认 TOTP 绑定（用户输入认证器中显示的 6 位动态口令）
 */
@Data
public class BindTotpDTO implements Serializable {

    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "动态口令为 6 位数字")
    private String code;
}

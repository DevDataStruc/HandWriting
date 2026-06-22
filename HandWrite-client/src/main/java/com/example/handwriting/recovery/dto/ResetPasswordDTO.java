package com.example.handwriting.recovery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 找回密码第三步：使用短期 token 重置密码
 */
@Data
public class ResetPasswordDTO implements Serializable {

    @NotBlank
    private String resetToken;

    @NotBlank
    @Size(min = 6, max = 64)
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?~`]{6,64}$",
            message = "密码需 6-64 位，且至少包含字母和数字")
    private String newPassword;
}

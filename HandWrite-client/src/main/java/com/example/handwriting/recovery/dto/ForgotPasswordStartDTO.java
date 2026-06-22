package com.example.handwriting.recovery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 找回密码第一步：用户输入用户名
 */
@Data
public class ForgotPasswordStartDTO implements Serializable {

    @NotBlank
    @Size(min = 3, max = 64)
    @Pattern(regexp = "^[A-Za-z0-9_.-]+$")
    private String username;
}

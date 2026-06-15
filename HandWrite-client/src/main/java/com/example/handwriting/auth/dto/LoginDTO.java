package com.example.handwriting.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class LoginDTO implements Serializable {

    @NotBlank
    @Size(min = 3, max = 64)
    private String username;

    @NotBlank
    @Size(min = 6, max = 64)
    private String password;

    /** 图形验证码 token */
    @NotBlank
    private String captchaKey;

    @NotBlank
    @Size(min = 4, max = 6)
    private String captchaCode;
}

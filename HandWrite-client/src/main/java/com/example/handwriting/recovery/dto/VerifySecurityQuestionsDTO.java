package com.example.handwriting.recovery.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 找回密码第二步：使用密保问题验证身份
 */
@Data
public class VerifySecurityQuestionsDTO implements Serializable {

    /** 第一步返回的 challengeId */
    @NotBlank
    private String challengeId;

    @NotEmpty
    @Size(min = 3, max = 3)
    @Valid
    private List<SecurityAnswerItem> answers;
}

package com.example.handwriting.recovery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户密保问题条目（设置时使用）
 */
@Data
public class SecurityQuestionItem implements Serializable {

    @NotBlank
    @Size(max = 64)
    private String questionKey;

    @NotBlank
    @Size(max = 255)
    private String questionText;

    @NotBlank
    @Size(min = 1, max = 64)
    private String answer;
}

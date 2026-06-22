package com.example.handwriting.recovery.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 单题密保答案
 */
@Data
public class SecurityAnswerItem implements Serializable {

    @NotNull
    @Min(1)
    private Integer questionIndex;

    @NotBlank
    @Size(min = 1, max = 64)
    private String answer;
}

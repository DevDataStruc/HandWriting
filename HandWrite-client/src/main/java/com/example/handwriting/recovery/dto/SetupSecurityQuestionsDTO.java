package com.example.handwriting.recovery.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 设置密保问题（一次性提交 3 题）
 */
@Data
public class SetupSecurityQuestionsDTO implements Serializable {

    @NotEmpty
    @Size(min = 3, max = 3, message = "必须设置 3 道密保问题")
    @Valid
    private List<SecurityQuestionItem> questions;
}

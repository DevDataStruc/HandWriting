package com.example.handwriting.recovery.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 密保问题摘要（仅暴露索引 + 问题正文，不暴露答案）
 */
@Data
public class SecurityQuestionVO implements Serializable {

    private Integer questionIndex;
    private String questionKey;
    private String questionText;
}

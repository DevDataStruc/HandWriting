package com.example.handwriting.recovery.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 找回密码第一步响应：
 *  - 不直接告知用户存在与否（统一返回"若账号存在则已发送"风格）
 *  - 告知前端当前账号**可用的恢复方式**
 *  - 若 SECURITY_QUESTION 可用，一并返回 3 道问题正文（不含答案）
 */
@Data
public class ForgotPasswordStartVO implements Serializable {

    /** 临时 challenge 标识（前端在后续请求中需要带回） */
    private String challengeId;

    /** 屏蔽后的账号标识（用于前端展示）：test → t****t */
    private String maskedUsername;

    /** 可用恢复方式：TOTP / SECURITY_QUESTION */
    private List<String> methods;

    /** 密保问题（仅 SECURITY_QUESTION 可用时返回，元素仅含 questionIndex/Key/Text，无答案） */
    private List<SecurityQuestionVO> securityQuestions;
}

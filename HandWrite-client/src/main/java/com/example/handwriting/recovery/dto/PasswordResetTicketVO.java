package com.example.handwriting.recovery.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 找回密码第二步响应：通过身份验证后下发的短期 token
 *  - 前端在重置密码时必须带回
 *  - 5 分钟内有效，一次性消费
 */
@Data
public class PasswordResetTicketVO implements Serializable {

    /** 短期 token（前端在重置密码时回传） */
    private String resetToken;

    /** 过期秒数（前端用于倒计时） */
    private Long expiresIn;
}

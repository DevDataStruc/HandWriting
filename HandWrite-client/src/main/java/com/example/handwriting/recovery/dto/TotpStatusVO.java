package com.example.handwriting.recovery.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 2FA 状态摘要
 */
@Data
public class TotpStatusVO implements Serializable {

    /** 是否已绑定 */
    private Boolean bound;

    /** 发行者 */
    private String issuer;

    /** 账户标签 */
    private String accountLabel;

    /** 剩余恢复码数量 */
    private Integer remainingRecoveryCodes;

    /** 绑定时间 */
    private java.time.LocalDateTime confirmedAt;
}

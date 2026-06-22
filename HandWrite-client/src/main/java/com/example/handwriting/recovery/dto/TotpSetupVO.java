package com.example.handwriting.recovery.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 2FA 初始化响应：携带 secret / otpauth URL / 二维码 base64 / 恢复码
 * <p>此 secret 仅在「未确认」状态下返回给前端一次，确认绑定后 secret 不再下发</p>
 */
@Data
public class TotpSetupVO implements Serializable {

    /** Base32 编码的 TOTP 密钥（明文，仅在未确认时返回） */
    private String secret;

    /** 认证器需要扫描的 otpauth URL */
    private String otpauthUrl;

    /** 二维码图片（PNG，base64） */
    private String qrCodeBase64;

    /** 10 个一次性恢复码（明文，仅显示一次） */
    private List<String> recoveryCodes;

    /** 发行者 */
    private String issuer;

    /** 账户标签 */
    private String accountLabel;
}

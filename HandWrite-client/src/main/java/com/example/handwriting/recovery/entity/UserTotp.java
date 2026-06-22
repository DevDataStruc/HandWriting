package com.example.handwriting.recovery.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户 2FA（TOTP）绑定
 */
@Data
@Entity
@Table(name = "t_user_totp", indexes = {
        @Index(name = "idx_totp_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
public class UserTotp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /** Base32 编码的 TOTP 密钥 */
    @Column(nullable = false, length = 64)
    private String secret;

    @Column(nullable = false, length = 64)
    private String issuer = "HandWrite";

    @Column(name = "account_label", nullable = false, length = 128)
    private String accountLabel;

    /** JSON 数组：每项为恢复码的 BCrypt 密文 */
    @Column(name = "recovery_codes", nullable = false, columnDefinition = "TEXT")
    private String recoveryCodes;

    /** 0 启用 / 1 已停用 */
    @Column(nullable = false)
    private Integer status = 0;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    /** 防重放：上次成功验证的 TOTP step */
    @Column(name = "last_used_step", nullable = false)
    private Long lastUsedStep = 0L;

    @CreatedDate
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @LastModifiedDate
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}

package com.example.handwriting.recovery.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 密码恢复短期 token（一次性）
 */
@Data
@Entity
@Table(name = "t_password_reset_token", indexes = {
        @Index(name = "idx_pr_user", columnList = "user_id"),
        @Index(name = "idx_pr_expire", columnList = "expired_at")
})
@EntityListeners(AuditingEntityListener.class)
public class PasswordResetToken implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128, unique = true)
    private String token;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** TOTP / SECURITY_QUESTION */
    @Column(nullable = false, length = 32)
    private String method;

    /** JSON：方法特有上下文 */
    @Column(name = "challenge_payload", columnDefinition = "TEXT")
    private String challengePayload;

    /** 0 未使用 / 1 已使用 */
    @Column(nullable = false)
    private Integer used = 0;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    @CreatedDate
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}

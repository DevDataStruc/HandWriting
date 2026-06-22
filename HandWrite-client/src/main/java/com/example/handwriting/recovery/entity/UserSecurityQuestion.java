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
 * 用户密保问题（每个用户最多 3 题）
 */
@Data
@Entity
@Table(name = "t_user_security_question", indexes = {
        @Index(name = "idx_sq_user", columnList = "user_id")
})
@EntityListeners(AuditingEntityListener.class)
public class UserSecurityQuestion implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 问题序号 1/2/3 */
    @Column(name = "question_index", nullable = false)
    private Integer questionIndex;

    @Column(name = "question_key", nullable = false, length = 64)
    private String questionKey;

    @Column(name = "question_text", nullable = false, length = 255)
    private String questionText;

    @Column(name = "answer_hash", nullable = false, length = 255)
    private String answerHash;

    @Column(nullable = false, length = 64)
    private String salt;

    @CreatedDate
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @LastModifiedDate
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}

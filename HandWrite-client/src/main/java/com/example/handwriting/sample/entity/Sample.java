package com.example.handwriting.sample.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_sample", indexes = {
        @Index(name = "idx_sample_user_status", columnList = "user_id,status"),
        @Index(name = "idx_sample_status_time", columnList = "status,create_time"),
        @Index(name = "idx_sample_char", columnList = "char_id")
})
@EntityListeners(AuditingEntityListener.class)
public class Sample implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "char_id", nullable = false)
    private Long charId;

    @Column(name = "file_key", nullable = false, length = 255)
    private String fileKey;

    @Column(name = "file_url", length = 512)
    private String fileUrl;

    @Column(name = "file_size", nullable = false)
    private Integer fileSize;

    @Column(length = 64)
    private String sha256;

    @Column(length = 64)
    private String device;

    /** 0 待审 / 1 通过 / 2 驳回 */
    @Column(nullable = false)
    private Integer status = 0;

    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    @Column(name = "audited_by")
    private Long auditedBy;

    @Column(name = "audited_time")
    private LocalDateTime auditedTime;

    @CreatedDate
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @LastModifiedDate
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(nullable = false)
    private Integer deleted = 0;
}

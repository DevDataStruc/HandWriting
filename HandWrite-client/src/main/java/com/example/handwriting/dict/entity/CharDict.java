package com.example.handwriting.dict.entity;

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
@Table(name = "t_char_dict", indexes = {
        @Index(name = "idx_char_category", columnList = "category"),
        @Index(name = "idx_char_difficulty", columnList = "difficulty")
})
@EntityListeners(AuditingEntityListener.class)
public class CharDict implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 字符 */
    @Column(name = "char_value", nullable = false, length = 8)
    private String charValue;

    /** HANZI / DIGIT / LETTER / SYMBOL */
    @Column(nullable = false, length = 32)
    private String category;

    /** 1-5 */
    @Column(nullable = false)
    private Integer difficulty = 1;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private Integer enabled = 1;

    @CreatedDate
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @LastModifiedDate
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}

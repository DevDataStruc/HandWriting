package com.example.handwriting.sample.dto;

import com.example.handwriting.sample.entity.Sample;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "样本视图")
public class SampleVO implements Serializable {

    private Long id;
    private Long userId;
    private Long charId;
    /**
     * 字符（汉字/数字/字母/符号），由 CharDict.charValue 映射。
     * 字段名 {@code charValue} 避免与 Java 关键字 {@code char} 冲突；
     * 显式 {@code @JsonProperty("char")} 锁定 JSON 键以匹配前端 {@code SampleVO.char}。
     */
    @Schema(description = "字符（汉字/数字/字母/符号）")
    @JsonProperty("char")
    private String charValue;
    private String fileKey;
    private String fileUrl;
    private Integer fileSize;
    private String device;
    private Integer status;
    private String rejectReason;
    private Long auditedBy;
    private LocalDateTime auditedTime;
    private LocalDateTime createTime;

    public static SampleVO from(Sample s) {
        SampleVO vo = new SampleVO();
        BeanUtils.copyProperties(s, vo);
        return vo;
    }
}

package com.example.handwriting.sample.dto;

import com.example.handwriting.sample.entity.Sample;
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

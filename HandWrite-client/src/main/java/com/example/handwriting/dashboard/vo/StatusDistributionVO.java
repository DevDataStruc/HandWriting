package com.example.handwriting.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 样本状态分布（GET /v1/stats/status-distribution）
 * <p>字段与前端 {@code StatusDistribution} 对齐</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "样本状态分布")
public class StatusDistributionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 状态：PENDING / APPROVED / REJECTED（也支持数字 0/1/2） */
    @Schema(description = "状态名")
    private String status;

    @Schema(description = "数量")
    private Long count;
}

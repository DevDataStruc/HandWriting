package com.example.handwriting.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统计总览（GET /v1/stats/overview）
 * <p>字段与前端 {@code StatsOverviewVO} 对齐，详见 HandWrite-web api/contracts/stats.d.ts</p>
 */
@Data
@Schema(description = "统计总览")
public class StatsOverviewVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "累计样本数")
    private Long totalSamples;

    @Schema(description = "注册用户数")
    private Long totalUsers;

    @Schema(description = "字符字典总数（启用）")
    private Long totalChars;

    @Schema(description = "今日新增样本数")
    private Long todaySamples;

    @Schema(description = "今日新增用户数")
    private Long todayUsers;

    @Schema(description = "待审核样本数")
    private Long pendingAudits;

    @Schema(description = "已通过样本数")
    private Long approvedSamples;

    @Schema(description = "已驳回样本数")
    private Long rejectedSamples;

    @Schema(description = "样本增长率（较昨日，百分比）")
    private Double growthRate;
}

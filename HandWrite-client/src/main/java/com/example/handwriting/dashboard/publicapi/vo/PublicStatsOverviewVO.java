package com.example.handwriting.dashboard.publicapi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 首页公开展示统计（GET /v1/public/stats/overview）
 * <p>面向未登录用户，仅暴露累计样本 / 参与用户 / 覆盖字符三个脱敏数字，
 * 与登录态下的 {@link com.example.handwriting.dashboard.vo.StatsOverviewVO} 解耦。</p>
 */
@Data
@Schema(description = "首页公开展示统计")
public class PublicStatsOverviewVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "累计样本数")
    private Long totalSamples;

    @Schema(description = "参与用户数")
    private Long totalUsers;

    @Schema(description = "覆盖字符数（已启用字典条数）")
    private Long totalChars;
}

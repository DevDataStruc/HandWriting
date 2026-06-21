package com.example.handwriting.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 样本/用户趋势（GET /v1/stats/trend?days=N）
 * <p>字段与前端 {@code SampleTrendVO} 对齐</p>
 */
@Data
@Schema(description = "样本趋势")
public class SampleTrendVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "日期序列（yyyy-MM-dd）")
    private List<String> dates;

    @Schema(description = "每日新增样本数")
    private List<Long> samples;

    @Schema(description = "每日新增用户数")
    private List<Long> users;
}

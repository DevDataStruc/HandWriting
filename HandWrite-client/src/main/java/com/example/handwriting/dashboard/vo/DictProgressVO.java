package com.example.handwriting.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 字符采集进度（GET /v1/stats/dict-progress）
 * <p>字段与前端 {@code DictProgress} 对齐</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "字符采集进度")
public class DictProgressVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "字符 ID")
    private Long charId;

    @Schema(description = "字符（汉字/数字/字母/符号）")
    private String charValue;

    @Schema(description = "已通过采集数")
    private Long collected;

    @Schema(description = "目标采集数（默认 100，可在配置中调整）")
    private Long target;

    @Schema(description = "进度（百分比，0-100）")
    private Long progress;
}

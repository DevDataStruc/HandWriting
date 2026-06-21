package com.example.handwriting.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 贡献者排行（GET /v1/stats/top-contributors）
 * <p>字段与前端 {@code TopContributor} 对齐</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "贡献者排行")
public class TopContributorVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户 ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像 URL")
    private String avatar;

    @Schema(description = "提交样本总数")
    private Long sampleCount;

    @Schema(description = "已通过样本数")
    private Long approvedCount;
}

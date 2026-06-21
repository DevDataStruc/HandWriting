package com.example.handwriting.dashboard.publicapi.controller;

import com.example.handwriting.common.result.R;
import com.example.handwriting.dashboard.publicapi.service.PublicStatsService;
import com.example.handwriting.dashboard.publicapi.vo.PublicStatsOverviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公共统计接口（无需登录）
 * <p>服务于门户首页等公开页面，仅暴露脱敏后的累计数字。</p>
 */
@Tag(name = "公共统计")
@RestController
@RequestMapping("/v1/public/stats")
@RequiredArgsConstructor
public class PublicStatsController {

    private final PublicStatsService publicStatsService;

    @Operation(summary = "首页公开展示统计（累计样本 / 参与用户 / 覆盖字符）")
    @GetMapping("/overview")
    public R<PublicStatsOverviewVO> overview() {
        return R.ok(publicStatsService.overview());
    }
}

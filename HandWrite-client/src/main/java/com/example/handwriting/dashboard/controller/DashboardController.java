package com.example.handwriting.dashboard.controller;

import com.example.handwriting.common.result.R;
import com.example.handwriting.dashboard.service.DashboardService;
import com.example.handwriting.dashboard.vo.DictProgressVO;
import com.example.handwriting.dashboard.vo.SampleTrendVO;
import com.example.handwriting.dashboard.vo.StatsOverviewVO;
import com.example.handwriting.dashboard.vo.StatusDistributionVO;
import com.example.handwriting.dashboard.vo.TopContributorVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 控制台 Dashboard 统计接口。
 * <p>与前端 {@code src/api/stats.ts} 中的扩展接口一一对应；</p>
 * <p>同时接管原 {@code /v1/stats/overview} 与 {@code /v1/stats/trend} 的返回结构，
 * 以匹配前端 {@code StatsOverviewVO} / {@code SampleTrendVO} 字段。</p>
 */
@Tag(name = "控制台 Dashboard")
@RestController
@RequestMapping("/v1/stats")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "总览数据")
    @GetMapping("/overview")
    public R<StatsOverviewVO> overview() {
        return R.ok(dashboardService.overview());
    }

    @Operation(summary = "样本/用户趋势（最近 N 天）")
    @GetMapping("/trend")
    public R<SampleTrendVO> trend(@RequestParam(defaultValue = "30") int days) {
        return R.ok(dashboardService.trend(days));
    }

    @Operation(summary = "样本状态分布（按 PENDING/APPROVED/REJECTED）")
    @GetMapping("/status-distribution")
    public R<List<StatusDistributionVO>> statusDistribution() {
        return R.ok(dashboardService.statusDistribution());
    }

    @Operation(summary = "贡献者排行 TOP N（默认 10）")
    @GetMapping("/top-contributors")
    public R<List<TopContributorVO>> topContributors(
            @RequestParam(defaultValue = "10") int limit) {
        return R.ok(dashboardService.topContributors(limit));
    }

    @Operation(summary = "字符采集进度（按进度降序，便于 TOP 20 展示）")
    @GetMapping("/dict-progress")
    public R<List<DictProgressVO>> dictProgress() {
        return R.ok(dashboardService.dictProgress());
    }
}

package com.example.handwriting.stats.controller;

import com.example.handwriting.common.result.R;
import com.example.handwriting.stats.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "统计")
@RestController
@RequestMapping("/v1/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @Operation(summary = "总览数据")
    @GetMapping("/overview")
    public R<Map<String, Object>> overview() {
        return R.ok(statsService.overview());
    }

    @Operation(summary = "样本趋势（最近 N 天）")
    @GetMapping("/trend")
    public R<Map<String, Object>> trend(@RequestParam(defaultValue = "30") int days) {
        return R.ok(statsService.trend(days));
    }
}

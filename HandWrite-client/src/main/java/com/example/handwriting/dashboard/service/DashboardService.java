package com.example.handwriting.dashboard.service;

import com.example.handwriting.common.constant.CommonConstants;
import com.example.handwriting.dashboard.vo.DictProgressVO;
import com.example.handwriting.dashboard.vo.SampleTrendVO;
import com.example.handwriting.dashboard.vo.StatsOverviewVO;
import com.example.handwriting.dashboard.vo.StatusDistributionVO;
import com.example.handwriting.dashboard.vo.TopContributorVO;
import com.example.handwriting.dict.entity.CharDict;
import com.example.handwriting.dict.repository.CharDictRepository;
import com.example.handwriting.sample.repository.SampleRepository;
import com.example.handwriting.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 控制台 Dashboard 统计服务。
 * <p>对应前端 {@code /v1/stats/*} 扩展接口（status-distribution / top-contributors / dict-progress），
 * 同时也覆写 /v1/stats/overview 与 /v1/stats/trend 的返回结构，
 * 以匹配前端 {@code api/contracts/stats.d.ts} 的字段定义。</p>
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    /** 单字符默认目标采集数（可通过 {@code app.dashboard.dict-target} 覆盖） */
    private static final long DEFAULT_DICT_TARGET = 100L;

    private final UserRepository userRepository;
    private final SampleRepository sampleRepository;
    private final CharDictRepository charDictRepository;

    // ============================================================
    // 总览（GET /v1/stats/overview）
    // ============================================================

    @Transactional(readOnly = true)
    public StatsOverviewVO overview() {
        StatsOverviewVO vo = new StatsOverviewVO();
        vo.setTotalSamples(sampleRepository.count());
        vo.setTotalUsers(userRepository.count());
        vo.setTotalChars(charDictRepository.countByEnabled(1));

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        vo.setTodaySamples(sampleRepository.countInRange(startOfDay, endOfDay));
        vo.setTodayUsers(userRepository.countInRange(startOfDay, endOfDay));

        vo.setPendingAudits(sampleRepository.countByStatus(CommonConstants.SAMPLE_STATUS_PENDING));
        vo.setApprovedSamples(sampleRepository.countByStatus(CommonConstants.SAMPLE_STATUS_APPROVED));
        vo.setRejectedSamples(sampleRepository.countByStatus(CommonConstants.SAMPLE_STATUS_REJECTED));

        // 增长率：今日样本数 / 昨日样本数 - 1（昨日 0 则返回 0）
        LocalDateTime startOfYesterday = startOfDay.minusDays(1);
        long yesterday = sampleRepository.countInRange(startOfYesterday, startOfDay);
        long today = vo.getTodaySamples() == null ? 0L : vo.getTodaySamples();
        if (yesterday <= 0L) {
            vo.setGrowthRate(today > 0L ? 100.0D : 0.0D);
        } else {
            vo.setGrowthRate(Math.round((today - yesterday) * 10000.0D / yesterday) / 100.0D);
        }
        return vo;
    }

    // ============================================================
    // 趋势（GET /v1/stats/trend?days=N）
    // ============================================================

    @Transactional(readOnly = true)
    public SampleTrendVO trend(int days) {
        if (days <= 0 || days > 90) days = 30;

        List<String> dates = new ArrayList<>(days);
        List<Long> samples = new ArrayList<>(days);
        List<Long> users = new ArrayList<>(days);

        // 在 DB 中按天聚合；区间为 [start, end) 闭开
        LocalDate end = LocalDate.now().plusDays(1);
        LocalDate start = end.minusDays(days);
        Map<LocalDate, Long> sampleByDate = aggregateSampleByDate(start, end);
        Map<LocalDate, Long> userByDate = aggregateUserByDate(start, end);

        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = LocalDate.now().minusDays(i);
            dates.add(d.format(DateTimeFormatter.ISO_DATE));
            samples.add(sampleByDate.getOrDefault(d, 0L));
            users.add(userByDate.getOrDefault(d, 0L));
        }

        SampleTrendVO vo = new SampleTrendVO();
        vo.setDates(dates);
        vo.setSamples(samples);
        vo.setUsers(users);
        return vo;
    }

    /**
     * 按天聚合样本创建量。使用原生 SQL 适配 MySQL 的 DATE() 函数。
     * 返回 [date, count] 形式，调用方转 Map。
     */
    @Transactional(readOnly = true)
    protected Map<LocalDate, Long> aggregateSampleByDate(LocalDate start, LocalDate end) {
        List<Object[]> rows = sampleRepository.countDailyCreated(start, end);
        Map<LocalDate, Long> map = new HashMap<>();
        for (Object[] r : rows) {
            if (r == null || r.length < 2 || r[0] == null) continue;
            map.put(((java.sql.Date) r[0]).toLocalDate(), ((Number) r[1]).longValue());
        }
        return map;
    }

    @Transactional(readOnly = true)
    protected Map<LocalDate, Long> aggregateUserByDate(LocalDate start, LocalDate end) {
        List<Object[]> rows = userRepository.countDailyCreated(start, end);
        Map<LocalDate, Long> map = new HashMap<>();
        for (Object[] r : rows) {
            if (r == null || r.length < 2 || r[0] == null) continue;
            map.put(((java.sql.Date) r[0]).toLocalDate(), ((Number) r[1]).longValue());
        }
        return map;
    }

    // ============================================================
    // 状态分布（GET /v1/stats/status-distribution）
    // ============================================================

    @Transactional(readOnly = true)
    public List<StatusDistributionVO> statusDistribution() {
        List<Object[]> rows = sampleRepository.countGroupByStatus();
        Map<Integer, Long> map = new HashMap<>();
        for (Object[] r : rows) {
            if (r == null || r.length < 2 || r[0] == null) continue;
            map.put(((Number) r[0]).intValue(), ((Number) r[1]).longValue());
        }
        // 固定顺序：已通过 / 待审核 / 已驳回（前端饼图期望的颜色对应）
        List<StatusDistributionVO> list = new ArrayList<>(3);
        list.add(StatusDistributionVO.builder()
                .status("APPROVED")
                .count(map.getOrDefault(CommonConstants.SAMPLE_STATUS_APPROVED, 0L))
                .build());
        list.add(StatusDistributionVO.builder()
                .status("PENDING")
                .count(map.getOrDefault(CommonConstants.SAMPLE_STATUS_PENDING, 0L))
                .build());
        list.add(StatusDistributionVO.builder()
                .status("REJECTED")
                .count(map.getOrDefault(CommonConstants.SAMPLE_STATUS_REJECTED, 0L))
                .build());
        return list;
    }

    // ============================================================
    // 贡献者排行（GET /v1/stats/top-contributors?limit=10）
    // ============================================================

    @Transactional(readOnly = true)
    public List<TopContributorVO> topContributors(int limit) {
        if (limit <= 0 || limit > 100) limit = 10;
        List<Object[]> rows = sampleRepository.aggregateByUser(PageRequest.of(0, limit));
        if (rows.isEmpty()) return Collections.emptyList();

        // 拉取用户基本信息
        Set<Long> userIds = rows.stream()
                .map(r -> ((Number) r[0]).longValue())
                .collect(Collectors.toSet());
        Map<Long, Object[]> userMap = userRepository.findBriefByIds(new ArrayList<>(userIds)).stream()
                .collect(Collectors.toMap(
                        r -> ((Number) r[0]).longValue(),
                        r -> r,
                        (a, b) -> a));

        List<TopContributorVO> result = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            Long userId = ((Number) r[0]).longValue();
            Long total = ((Number) r[1]).longValue();
            Long approved = r[2] == null ? 0L : ((Number) r[2]).longValue();
            Object[] brief = userMap.get(userId);
            String username = brief == null ? null : (String) brief[1];
            String nickname = brief == null ? null : (String) brief[2];
            String avatar = brief == null ? null : (String) brief[3];
            result.add(TopContributorVO.builder()
                    .userId(userId)
                    .username(username)
                    .nickname(nickname)
                    .avatar(avatar)
                    .sampleCount(total)
                    .approvedCount(approved)
                    .build());
        }
        return result;
    }

    // ============================================================
    // 字符采集进度（GET /v1/stats/dict-progress）
    // ============================================================

    @Transactional(readOnly = true)
    public List<DictProgressVO> dictProgress() {
        Map<Long, Long> approvedMap = sampleRepository.countApprovedGroupByCharId().stream()
                .filter(Objects::nonNull)
                .filter(r -> r.length >= 2 && r[0] != null)
                .collect(Collectors.toMap(
                        r -> ((Number) r[0]).longValue(),
                        r -> ((Number) r[1]).longValue(),
                        (a, b) -> a));

        // 仅取启用的字符，避免返回 disabled 项
        List<CharDict> enabledChars = charDictRepository.findByEnabled(1,
                PageRequest.of(0, 5000)).getContent();
        List<DictProgressVO> list = new ArrayList<>(enabledChars.size());
        for (CharDict c : enabledChars) {
            long collected = approvedMap.getOrDefault(c.getId(), 0L);
            long target = DEFAULT_DICT_TARGET;
            long progress = Math.min(100L, target <= 0 ? 0L : collected * 100L / target);
            list.add(DictProgressVO.builder()
                    .charId(c.getId())
                    .charValue(c.getCharValue())
                    .collected(collected)
                    .target(target)
                    .progress(progress)
                    .build());
        }
        // 按进度降序、采集数降序，便于前端直接取 TOP 20
        list.sort((a, b) -> {
            int cmp = Long.compare(b.getProgress(), a.getProgress());
            return cmp != 0 ? cmp : Long.compare(b.getCollected(), a.getCollected());
        });
        return list;
    }
}

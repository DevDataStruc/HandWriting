package com.example.handwriting.dashboard.publicapi.service;

import com.example.handwriting.dashboard.publicapi.vo.PublicStatsOverviewVO;
import com.example.handwriting.dict.repository.CharDictRepository;
import com.example.handwriting.sample.repository.SampleRepository;
import com.example.handwriting.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 首页公开展示统计服务。
 * <p>与 DashboardService 解耦，仅做最小化的 count 查询，避免暴露管理后台细节。</p>
 */
@Service
@RequiredArgsConstructor
public class PublicStatsService {

    private final SampleRepository sampleRepository;
    private final UserRepository userRepository;
    private final CharDictRepository charDictRepository;

    @Transactional(readOnly = true)
    public PublicStatsOverviewVO overview() {
        PublicStatsOverviewVO vo = new PublicStatsOverviewVO();
        vo.setTotalSamples(sampleRepository.count());
        vo.setTotalUsers(userRepository.count());
        vo.setTotalChars(charDictRepository.countByEnabled(1));
        return vo;
    }
}

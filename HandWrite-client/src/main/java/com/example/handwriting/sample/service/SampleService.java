package com.example.handwriting.sample.service;

import com.example.handwriting.common.constant.CommonConstants;
import com.example.handwriting.common.exception.BizException;
import com.example.handwriting.common.result.ErrorCode;
import com.example.handwriting.common.result.PageQuery;
import com.example.handwriting.common.result.PageResult;
import com.example.handwriting.dict.entity.CharDict;
import com.example.handwriting.dict.repository.CharDictRepository;
import com.example.handwriting.sample.dto.SampleUploadDTO;
import com.example.handwriting.sample.dto.SampleVO;
import com.example.handwriting.sample.entity.Sample;
import com.example.handwriting.sample.repository.SampleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SampleService {

    private final SampleRepository sampleRepository;
    private final CharDictRepository charDictRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional
    public SampleVO upload(Long userId, SampleUploadDTO dto) {
        CharDict dict = charDictRepository.findById(dto.getCharId())
                .orElseThrow(() -> new BizException(ErrorCode.CHAR_DICT_NOT_EXISTS));
        if (dict.getEnabled() == 0) {
            throw new BizException(ErrorCode.CHAR_DICT_NOT_EXISTS);
        }
        Sample s = new Sample();
        s.setUserId(userId);
        s.setCharId(dto.getCharId());
        s.setFileKey(dto.getFileKey());
        s.setFileUrl(dto.getFileUrl());
        s.setFileSize(dto.getFileSize());
        s.setSha256(dto.getSha256());
        s.setDevice(dto.getDevice());
        s.setStatus(CommonConstants.SAMPLE_STATUS_PENDING);
        sampleRepository.save(s);

        // 实时计数
        String key = String.format(CommonConstants.REDIS_KEY_STATS_DAILY,
                LocalDate.now().format(DateTimeFormatter.ISO_DATE));
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofDays(40));

        return SampleVO.from(s);
    }

    public PageResult<SampleVO> mySamples(Long userId, Integer status, PageQuery query) {
        PageRequest pr = PageRequest.of((int) (query.getPageNum() - 1), (int) query.getPageSize(),
                Sort.by(Sort.Direction.DESC, "id"));
        Page<Sample> page = (status == null)
                ? sampleRepository.findByUserId(userId, pr)
                : sampleRepository.findByUserIdAndStatus(userId, status, pr);
        return toPageResult(page, query);
    }

    public SampleVO detail(Long id, Long userId, boolean isAdminOrAuditor) {
        Sample s = sampleRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.SAMPLE_NOT_EXISTS));
        if (!isAdminOrAuditor && !s.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.SAMPLE_OWNER_FORBIDDEN);
        }
        return SampleVO.from(s);
    }

    @Transactional
    public void delete(Long id, Long userId, boolean isAdmin) {
        Sample s = sampleRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.SAMPLE_NOT_EXISTS));
        if (!isAdmin && !s.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.SAMPLE_OWNER_FORBIDDEN);
        }
        if (s.getStatus() == CommonConstants.SAMPLE_STATUS_APPROVED) {
            throw new BizException(ErrorCode.SAMPLE_STATUS_INVALID, "已通过样本不可删除");
        }
        sampleRepository.delete(s);
    }

    private PageResult<SampleVO> toPageResult(Page<Sample> page, PageQuery query) {
        List<SampleVO> records = page.getContent().stream().map(SampleVO::from).collect(Collectors.toList());
        return PageResult.of(page.getTotalElements(), query.getPageNum(), query.getPageSize(), records);
    }
}

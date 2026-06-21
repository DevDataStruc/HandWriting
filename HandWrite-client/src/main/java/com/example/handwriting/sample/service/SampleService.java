package com.example.handwriting.sample.service;

import com.example.handwriting.common.constant.CommonConstants;
import com.example.handwriting.common.exception.BizException;
import com.example.handwriting.common.result.ErrorCode;
import com.example.handwriting.common.result.PageQuery;
import com.example.handwriting.common.result.PageResult;
import com.example.handwriting.dict.entity.CharDict;
import com.example.handwriting.dict.repository.CharDictRepository;
import com.example.handwriting.file.service.LocalFileStorageService;
import com.example.handwriting.sample.dto.SampleVO;
import com.example.handwriting.sample.entity.Sample;
import com.example.handwriting.sample.repository.SampleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SampleService {

    /** 匹配 t_sample.device VARCHAR(64) */
    private static final int MAX_DEVICE_LEN = 64;

    private final SampleRepository sampleRepository;
    private final CharDictRepository charDictRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final LocalFileStorageService localFileStorage;

    /**
     * 新建样本：接收 multipart 文件，落地本地 storage/ 目录后落库。
     * 同字符可重复上传，每个文件一条 Sample 记录。
     */
    @Transactional
    public SampleVO uploadWithFile(Long userId, Long charId, MultipartFile file, String device) {
        CharDict dict = requireEnabledChar(charId);

        LocalFileStorageService.StoredFile stored = localFileStorage.store(userId, dict.getId(), file);

        Sample s = new Sample();
        s.setUserId(userId);
        s.setCharId(dict.getId());
        s.setFileKey(stored.fileKey());
        s.setFileUrl(stored.accessUrl());
        s.setFileSize((int) stored.size());
        s.setDevice(safeDevice(device));
        s.setStatus(CommonConstants.SAMPLE_STATUS_PENDING);
        sampleRepository.save(s);

        bumpDailyCounter();

        log.info("[Sample] 用户 {} 上传样本 charId={} fileKey={} size={}",
                userId, dict.getId(), stored.fileKey(), stored.size());
        return SampleVO.from(s);
    }

    /**
     * 重新上传笔迹：原文件删除，保存新文件，状态重置为待审核。
     */
    @Transactional
    public SampleVO updateWithFile(Long id, Long userId, MultipartFile file, Long charId) {
        Sample s = sampleRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.SAMPLE_NOT_EXISTS));
        if (!s.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.SAMPLE_OWNER_FORBIDDEN);
        }
        if (s.getStatus() == CommonConstants.SAMPLE_STATUS_APPROVED) {
            throw new BizException(ErrorCode.SAMPLE_STATUS_INVALID, "已通过样本不可修改");
        }

        Long targetCharId = charId != null ? charId : s.getCharId();
        if (charId != null && !charId.equals(s.getCharId())) {
            // 校验新字符有效
            requireEnabledChar(charId);
        }

        LocalFileStorageService.StoredFile stored = localFileStorage.store(userId, targetCharId, file);

        // 删除旧文件（不抛错）
        localFileStorage.deleteQuietly(s.getFileKey());

        s.setCharId(targetCharId);
        s.setFileKey(stored.fileKey());
        s.setFileUrl(stored.accessUrl());
        s.setFileSize((int) stored.size());
        s.setStatus(CommonConstants.SAMPLE_STATUS_PENDING);
        s.setRejectReason(null);
        s.setAuditedBy(null);
        s.setAuditedTime(null);

        sampleRepository.save(s);
        log.info("[Sample] 用户 {} 重新上传样本 id={} charId={} fileKey={}",
                userId, s.getId(), targetCharId, stored.fileKey());
        return SampleVO.from(s);
    }

    private CharDict requireEnabledChar(Long charId) {
        CharDict dict = charDictRepository.findById(charId)
                .orElseThrow(() -> new BizException(ErrorCode.CHAR_DICT_NOT_EXISTS));
        if (dict.getEnabled() == 0) {
            throw new BizException(ErrorCode.CHAR_DICT_NOT_EXISTS);
        }
        return dict;
    }

    /**
     * 设备指纹归一化：截断到 MAX_DEVICE_LEN（匹配 t_sample.device 列长），
     * 避免 navigator.userAgent 之类的长字符串触发 MySQL Data truncation。
     */
    private String safeDevice(String device) {
        if (device == null) return "";
        if (device.length() <= MAX_DEVICE_LEN) return device;
        return device.substring(0, MAX_DEVICE_LEN);
    }

    private void bumpDailyCounter() {
        String key = String.format(CommonConstants.REDIS_KEY_STATS_DAILY,
                LocalDate.now().format(DateTimeFormatter.ISO_DATE));
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofDays(40));
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
        SampleVO vo = SampleVO.from(s);
        vo.setCharValue(resolveCharValue(s.getCharId()));
        return vo;
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
        // 先删文件再删记录（顺序可调，文件不存在不影响主流程）
        localFileStorage.deleteQuietly(s.getFileKey());
        sampleRepository.delete(s);
    }

    private PageResult<SampleVO> toPageResult(Page<Sample> page, PageQuery query) {
        List<SampleVO> records = page.getContent().stream().map(SampleVO::from).collect(Collectors.toList());
        Set<Long> charIds = records.stream()
                .map(SampleVO::getCharId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!charIds.isEmpty()) {
            Map<Long, String> charMap = charDictRepository.findAllById(charIds).stream()
                    .collect(Collectors.toMap(CharDict::getId, CharDict::getCharValue));
            records.forEach(vo -> vo.setCharValue(charMap.get(vo.getCharId())));
        }
        return PageResult.of(page.getTotalElements(), query.getPageNum(), query.getPageSize(), records);
    }

    private String resolveCharValue(Long charId) {
        if (charId == null) return null;
        return charDictRepository.findById(charId)
                .map(CharDict::getCharValue)
                .orElse(null);
    }
}

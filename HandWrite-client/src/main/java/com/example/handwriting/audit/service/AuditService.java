package com.example.handwriting.audit.service;

import com.example.handwriting.audit.dto.AuditDecisionDTO;
import com.example.handwriting.common.constant.CommonConstants;
import com.example.handwriting.common.exception.BizException;
import com.example.handwriting.common.result.ErrorCode;
import com.example.handwriting.common.result.PageQuery;
import com.example.handwriting.common.result.PageResult;
import com.example.handwriting.dict.entity.CharDict;
import com.example.handwriting.dict.repository.CharDictRepository;
import com.example.handwriting.sample.dto.SampleVO;
import com.example.handwriting.sample.entity.Sample;
import com.example.handwriting.sample.repository.SampleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final SampleRepository sampleRepository;
    private final CharDictRepository charDictRepository;

    public PageResult<SampleVO> pending(PageQuery query) {
        PageRequest pr = PageRequest.of((int) (query.getPageNum() - 1), (int) query.getPageSize(),
                Sort.by(Sort.Direction.ASC, "createTime"));
        Page<Sample> page = sampleRepository.findByStatus(CommonConstants.SAMPLE_STATUS_PENDING, pr);
        return toPageResult(page, query);
    }

    public PageResult<SampleVO> history(Integer status, PageQuery query) {
        PageRequest pr = PageRequest.of((int) (query.getPageNum() - 1), (int) query.getPageSize(),
                Sort.by(Sort.Direction.DESC, "auditedTime"));
        Page<Sample> page = sampleRepository.findByStatus(
                status == null ? CommonConstants.SAMPLE_STATUS_APPROVED : status, pr);
        return toPageResult(page, query);
    }

    @Transactional
    public void approve(Long auditorId, Long id) {
        Sample s = sampleRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.SAMPLE_NOT_EXISTS));
        if (s.getStatus() != CommonConstants.SAMPLE_STATUS_PENDING) {
            throw new BizException(ErrorCode.AUDIT_ALREADY_DONE);
        }
        s.setStatus(CommonConstants.SAMPLE_STATUS_APPROVED);
        s.setAuditedBy(auditorId);
        s.setAuditedTime(LocalDateTime.now());
        sampleRepository.save(s);
    }

    @Transactional
    public void reject(Long auditorId, Long id, AuditDecisionDTO dto) {
        Sample s = sampleRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.SAMPLE_NOT_EXISTS));
        if (s.getStatus() != CommonConstants.SAMPLE_STATUS_PENDING) {
            throw new BizException(ErrorCode.AUDIT_ALREADY_DONE);
        }
        s.setStatus(CommonConstants.SAMPLE_STATUS_REJECTED);
        s.setAuditedBy(auditorId);
        s.setAuditedTime(LocalDateTime.now());
        s.setRejectReason(dto == null ? null : dto.getReason());
        sampleRepository.save(s);
    }

    /**
     * 将 {@link Sample} 列表转换为带 {@code char} 字段的 {@link SampleVO}。
     * <p>批量查 CharDict 避免 N+1。</p>
     */
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
}

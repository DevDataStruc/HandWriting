package com.example.handwriting.dict.service;

import com.example.handwriting.common.exception.BizException;
import com.example.handwriting.common.result.ErrorCode;
import com.example.handwriting.common.result.PageQuery;
import com.example.handwriting.common.result.PageResult;
import com.example.handwriting.dict.dto.CharDictUpsertDTO;
import com.example.handwriting.dict.entity.CharDict;
import com.example.handwriting.dict.repository.CharDictRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static com.example.handwriting.common.constant.CommonConstants.REDIS_KEY_DICT_CACHE;

@Service
@RequiredArgsConstructor
public class CharDictService {

    private final CharDictRepository repository;
    private final org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    public PageResult<CharDict> page(String category, PageQuery query) {
        PageRequest pr = PageRequest.of((int) (query.getPageNum() - 1), (int) query.getPageSize(),
                Sort.by(Sort.Direction.ASC, "id"));
        Page<CharDict> page = (category == null || category.isEmpty())
                ? repository.findByEnabled(1, pr)
                : repository.findByCategoryAndEnabled(category, 1, pr);
        return PageResult.of(page.getTotalElements(), query.getPageNum(), query.getPageSize(),
                page.getContent());
    }

    public List<CharDict> listByCategory(String category) {
        String key = String.format(REDIS_KEY_DICT_CACHE, category == null ? "ALL" : category);
        @SuppressWarnings("unchecked")
        List<CharDict> cached = (List<CharDict>) redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached;
        }
        List<CharDict> list = (category == null || category.isEmpty())
                ? repository.findAll()
                : repository.findByCategoryAndEnabled(category, 1,
                        PageRequest.of(0, 1000, Sort.by(Sort.Direction.ASC, "id"))).getContent();
        if (list == null) list = Collections.emptyList();
        redisTemplate.opsForValue().set(key, list, Duration.ofMinutes(10));
        return list;
    }

    public CharDict detail(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.SAMPLE_NOT_EXISTS, "字符不存在"));
    }

    @Transactional
    public CharDict create(CharDictUpsertDTO dto) {
        if (repository.existsByCharValue(dto.getCharValue())) {
            throw new BizException(ErrorCode.USER_ALREADY_EXISTS, "字符已存在: " + dto.getCharValue());
        }
        CharDict entity = new CharDict();
        entity.setCharValue(dto.getCharValue());
        entity.setCategory(dto.getCategory());
        entity.setDifficulty(dto.getDifficulty());
        entity.setDescription(dto.getDescription());
        entity.setEnabled(dto.getEnabled());
        CharDict saved = repository.save(entity);
        clearDictCache();
        return saved;
    }

    @Transactional
    public CharDict update(Long id, CharDictUpsertDTO dto) {
        CharDict entity = repository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.SAMPLE_NOT_EXISTS, "字符不存在"));
        repository.findByCharValue(dto.getCharValue())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new BizException(ErrorCode.USER_ALREADY_EXISTS, "字符已存在: " + dto.getCharValue());
                });
        entity.setCharValue(dto.getCharValue());
        entity.setCategory(dto.getCategory());
        entity.setDifficulty(dto.getDifficulty());
        entity.setDescription(dto.getDescription());
        entity.setEnabled(dto.getEnabled());
        CharDict saved = repository.save(entity);
        clearDictCache();
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        CharDict entity = repository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.SAMPLE_NOT_EXISTS, "字符不存在"));
        repository.delete(entity);
        clearDictCache();
    }

    /**
     * 批量导入字符
     * <p>
     * 处理策略：
     * <ul>
     *   <li>每条记录尝试入库；若 charValue 已存在则视为跳过而非失败（导入友好）</li>
     *   <li>条目级字段缺省时使用 dto 顶层默认值（defaultCategory / defaultDifficulty ...）</li>
     *   <li>整批作为一个事务，任一行脏数据可导致整体回滚（保守做法，保留一致性）</li>
     * </ul>
     *
     * @return 包含 inserted/skipped/failed 的 VO
     */
    @Transactional
    public com.example.handwriting.dict.dto.CharDictImportResultVO batchCreateWithDefaults(
            com.example.handwriting.dict.dto.CharDictBatchCreateDTO dto,
            List<com.example.handwriting.dict.dto.CharDictBatchCreateDTO.Item> rawItems) {
        if (rawItems == null || rawItems.isEmpty()) {
            com.example.handwriting.dict.dto.CharDictImportResultVO empty =
                    new com.example.handwriting.dict.dto.CharDictImportResultVO();
            empty.setMessage("未提供字符条目");
            return empty;
        }

        String defaultCategory = (dto == null || dto.getDefaultCategory() == null
                || dto.getDefaultCategory().isBlank()) ? "HANZI" : dto.getDefaultCategory();
        Integer defaultDifficulty = (dto == null || dto.getDefaultDifficulty() == null)
                ? 1 : dto.getDefaultDifficulty();
        String defaultDescription = (dto == null) ? null : dto.getDefaultDescription();
        Integer defaultEnabled = (dto == null || dto.getDefaultEnabled() == null) ? 1 : dto.getDefaultEnabled();

        com.example.handwriting.dict.dto.CharDictImportResultVO result =
                new com.example.handwriting.dict.dto.CharDictImportResultVO();
        java.util.List<CharDict> toSave = new java.util.ArrayList<>();
        java.util.List<String> failedSamples = new java.util.ArrayList<>();

        for (com.example.handwriting.dict.dto.CharDictBatchCreateDTO.Item it : rawItems) {
            if (it == null || it.getCharValue() == null || it.getCharValue().isBlank()) {
                continue;
            }
            String cv = it.getCharValue().trim();
            // 单字符长度上限 8（与 DTO 注解保持一致）
            if (cv.length() > 8) {
                failedSamples.add(cv);
                continue;
            }
            if (repository.existsByCharValue(cv)) {
                result.setSkipped(result.getSkipped() + 1);
                continue;
            }
            try {
                CharDict entity = new CharDict();
                entity.setCharValue(cv);
                entity.setCategory(it.getCategory() == null || it.getCategory().isBlank()
                        ? defaultCategory : it.getCategory());
                entity.setDifficulty(it.getDifficulty() == null ? defaultDifficulty : it.getDifficulty());
                entity.setDescription(it.getDescription() == null ? defaultDescription : it.getDescription());
                entity.setEnabled(it.getEnabled() == null ? defaultEnabled : it.getEnabled());
                toSave.add(entity);
            } catch (Exception ex) {
                failedSamples.add(cv);
            }
        }

        if (!toSave.isEmpty()) {
            repository.saveAll(toSave);
            result.setInserted(toSave.size());
        }
        result.setFailed(failedSamples.size());
        // 截断样本
        if (failedSamples.size() > 50) {
            result.setFailedSamples(failedSamples.subList(0, 50));
        } else {
            result.setFailedSamples(failedSamples);
        }
        result.setMessage(String.format("新增 %d，跳过 %d，失败 %d", result.getInserted(), result.getSkipped(), result.getFailed()));
        if (result.getInserted() > 0) {
            clearDictCache();
        }
        return result;
    }

    private void clearDictCache() {
        for (String category : List.of("ALL", "HANZI", "DIGIT", "LETTER", "SYMBOL")) {
            redisTemplate.delete(String.format(REDIS_KEY_DICT_CACHE, category));
        }
    }
}

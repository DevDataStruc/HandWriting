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

    private void clearDictCache() {
        for (String category : List.of("ALL", "HANZI", "DIGIT", "LETTER", "SYMBOL")) {
            redisTemplate.delete(String.format(REDIS_KEY_DICT_CACHE, category));
        }
    }
}

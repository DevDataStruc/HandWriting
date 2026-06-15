package com.example.handwriting.dict.service;

import com.example.handwriting.common.result.PageQuery;
import com.example.handwriting.common.result.PageResult;
import com.example.handwriting.dict.entity.CharDict;
import com.example.handwriting.dict.repository.CharDictRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

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
}

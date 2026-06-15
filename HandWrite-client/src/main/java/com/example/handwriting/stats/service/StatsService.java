package com.example.handwriting.stats.service;

import com.example.handwriting.sample.repository.SampleRepository;
import com.example.handwriting.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.example.handwriting.common.constant.CommonConstants.REDIS_KEY_STATS_DAILY;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final UserRepository userRepository;
    private final SampleRepository sampleRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public Map<String, Object> overview() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userTotal", userRepository.count());
        result.put("sampleTotal", sampleRepository.count());

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        long todaySamples = sampleRepository.countInRange(startOfDay, endOfDay);
        result.put("sampleToday", todaySamples);
        return result;
    }

    public Map<String, Object> trend(int days) {
        if (days <= 0 || days > 90) days = 30;
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Long> samples = new LinkedHashMap<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = LocalDate.now().minusDays(i);
            String key = String.format(REDIS_KEY_STATS_DAILY, d.format(DateTimeFormatter.ISO_DATE));
            Object v = redisTemplate.opsForValue().get(key);
            samples.put(d.toString(), v == null ? 0L : Long.parseLong(v.toString()));
        }
        result.put("days", days);
        result.put("samples", samples);
        return result;
    }
}

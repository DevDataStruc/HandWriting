package com.example.handwriting.auth.service;

import com.wf.captcha.SpecCaptcha;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.example.handwriting.common.constant.CommonConstants.REDIS_KEY_CAPTCHA;

/**
 * 图形验证码服务
 */
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private static final long CAPTCHA_TTL_SECONDS = 300L;

    private final RedisTemplate<String, Object> redisTemplate;

    public Map<String, Object> generate() {
        SpecCaptcha captcha = new SpecCaptcha(120, 40, 4);
        captcha.setCharType(SpecCaptcha.TYPE_DEFAULT);
        String code = captcha.text().toLowerCase();
        String key = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(
                String.format(REDIS_KEY_CAPTCHA, key),
                code,
                Duration.ofSeconds(CAPTCHA_TTL_SECONDS));

        byte[] pngBytes;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            captcha.out(baos);
            pngBytes = baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("生成图形验证码失败", e);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("captchaKey", key);
        result.put("imageBase64", "data:image/png;base64," + Base64.getEncoder().encodeToString(pngBytes));
        result.put("expireSeconds", CAPTCHA_TTL_SECONDS);
        return result;
    }
}

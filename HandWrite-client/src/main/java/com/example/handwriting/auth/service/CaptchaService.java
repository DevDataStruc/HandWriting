package com.example.handwriting.auth.service;

import com.wf.captcha.SpecCaptcha;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 图形验证码服务（内存版）。
 * <p>用 {@link ConcurrentHashMap} 存储 captchaKey -> (code, expireAt)，
 * 验证即删除（一次性使用），过期项在访问时惰性清理。</p>
 * <p>注意：内存版仅适合单实例部署；多实例需替换为集中式存储（Redis/DB）。</p>
 */
@Slf4j
@Service
public class CaptchaService {

    private static final long CAPTCHA_TTL_SECONDS = 300L;
    private static final long CAPTCHA_TTL_MILLIS = CAPTCHA_TTL_SECONDS * 1000L;

    /** 验证码条目：存原文 + 过期时间戳 */
    private record CaptchaEntry(String code, long expireAtMs) {
        boolean isExpired() {
            return System.currentTimeMillis() > expireAtMs;
        }
    }

    private final ConcurrentMap<String, CaptchaEntry> store = new ConcurrentHashMap<>();

    public Map<String, Object> generate() {
        SpecCaptcha captcha = new SpecCaptcha(120, 40, 4);
        captcha.setCharType(SpecCaptcha.TYPE_DEFAULT);
        String code = captcha.text().toLowerCase();
        String key = UUID.randomUUID().toString().replace("-", "");

        store.put(key, new CaptchaEntry(code, System.currentTimeMillis() + CAPTCHA_TTL_MILLIS));

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

    /**
     * 校验并消费验证码。一次性使用：校验通过后立刻从存储中删除。
     * <p>使用 {@link ConcurrentMap#compute} 保证"取-校验-删"三步原子，
     * 避免两个并发登录使用同一 captchaKey 时都被放行。</p>
     *
     * @param key  captchaKey
     * @param code 用户输入的验证码（不区分大小写）
     * @return true 通过；false 失败（不存在 / 已过期 / 错误）
     */
    public boolean verify(String key, String code) {
        if (key == null || code == null) return false;
        String lower = code.toLowerCase();
        boolean[] ok = new boolean[1];
        store.compute(key, (k, entry) -> {
            if (entry == null) {
                // 不存在或已过期被清理
                ok[0] = false;
                return null;
            }
            if (entry.isExpired()) {
                // 过期项：清理
                ok[0] = false;
                return null;
            }
            if (entry.code.equals(lower)) {
                // 校验通过：消费
                ok[0] = true;
                return null;
            }
            // 校验失败：保留项，给用户重试
            ok[0] = false;
            return entry;
        });
        return ok[0];
    }

    /**
     * 兜底：登录失败时强制清理某个 captchaKey，防止暴力枚举。
     * 校验失败本身不删除条目（允许重试），但显式调用此方法可立即作废。
     */
    public void invalidate(String key) {
        if (key == null) return;
        store.remove(key);
    }
}

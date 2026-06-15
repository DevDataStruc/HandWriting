package com.example.handwriting.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * Web 工具：获取当前请求、IP、User-Agent 等
 */
public final class WebUtils {

    private WebUtils() {}

    public static HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }

    public static String clientIp() {
        HttpServletRequest req = currentRequest();
        if (req == null) return "unknown";
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
        for (String h : headers) {
            String ip = req.getHeader(h);
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                int idx = ip.indexOf(',');
                return idx > 0 ? ip.substring(0, idx).trim() : ip.trim();
            }
        }
        return Optional.ofNullable(req.getRemoteAddr()).orElse("unknown");
    }

    public static String userAgent() {
        HttpServletRequest req = currentRequest();
        return req == null ? "" : Optional.ofNullable(req.getHeader("User-Agent")).orElse("");
    }
}

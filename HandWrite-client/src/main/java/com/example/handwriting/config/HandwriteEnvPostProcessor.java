package com.example.handwriting.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * HANDWRITE_* 环境变量强制注入器
 * <p>
 * 解决问题：IntelliJ 启动时缓存了 env vars，启动后修改注册表 / setx 设的 vars 不会影响
 * 已启动的进程，导致 Spring 占位符 ${HANDWRITE_*} 解析失败。
 * <p>
 * 本类在 Spring 启动最早阶段（任何 Bean 之前）执行：
 *   1. 优先读 System.getenv() 中的 HANDWRITE_*
 *   2. 回退读 user.dir/.env  /  user.dir/tools/.env
 *   3. 回退读 Windows 注册表 (HKCU\Environment)
 * <p>
 * 把所有收集到的 vars 包装为 PropertySource 注入到 Spring Environment，
 * 优先级高于 application.yml（因此 application.yml 中的默认值作 fallback）。
 * <p>
 * 注册方式：META-INF/spring.factories 或 META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports
 */
public class HandwriteEnvPostProcessor implements EnvironmentPostProcessor {

    private static final String SOURCE_NAME = "handwrite-env-injector@" + UUID.randomUUID();
    private static final String PREFIX = "HANDWRITE_";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
        Map<String, Object> props = new LinkedHashMap<>();

        // 1) System.getenv()
        collectFromSystemEnv(props);

        // 2) .env 文件
        collectFromDotEnv(props, Paths.get("").toAbsolutePath().resolve(".env"));
        collectFromDotEnv(props, Paths.get("").toAbsolutePath().resolve("tools").resolve(".env"));

        // 3) Windows 注册表
        if (isWindows()) {
            collectFromWindowsRegistry(props);
        }

        if (props.isEmpty()) {
            return;
        }

        // 4) 注入到 Spring Environment（addFirst 确保最高优先级）
        MapPropertySource source = new MapPropertySource(SOURCE_NAME, props);
        env.getPropertySources().addFirst(source);
    }

    private void collectFromSystemEnv(Map<String, Object> props) {
        try {
            System.getenv().forEach((k, v) -> {
                if (k.startsWith(PREFIX) && v != null) {
                    props.putIfAbsent(k, v);
                }
            });
        } catch (SecurityException ignored) {
            // 安全管理器拒绝
        }
    }

    private void collectFromDotEnv(Map<String, Object> props, Path path) {
        if (!Files.exists(path)) {
            return;
        }
        try {
            Files.lines(path).forEach(line -> {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    return;
                }
                int eq = trimmed.indexOf('=');
                if (eq <= 0) {
                    return;
                }
                String key = trimmed.substring(0, eq).trim();
                String val = trimmed.substring(eq + 1).trim();
                if (key.startsWith(PREFIX) && !val.isEmpty()) {
                    // 去掉首尾引号
                    if ((val.startsWith("\"") && val.endsWith("\"")) ||
                            (val.startsWith("'") && val.endsWith("'"))) {
                        val = val.substring(1, val.length() - 1);
                    }
                    props.putIfAbsent(key, val);
                }
            });
        } catch (IOException ignored) {
        }
    }

    private void collectFromWindowsRegistry(Map<String, Object> props) {
        try {
            Process p = new ProcessBuilder("reg", "query",
                    "HKCU\\Environment", "/v", PREFIX + "DB_HOST").redirectErrorStream(true).start();
            // 简化处理：批量 dump 所有 HANDWRITE_*
            Process all = new ProcessBuilder("cmd", "/c",
                    "for /f \"tokens=1,* delims==\" %a in ('reg query \"HKCU\\Environment\" ^| findstr /B /I \"HANDWRITE_\"') do @echo %a=%b"
            ).redirectErrorStream(true).start();
            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(all.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                String key = line.substring(0, eq).trim();
                String val = line.substring(eq + 1).trim();
                if (key.startsWith(PREFIX) && !val.isEmpty()) {
                    props.putIfAbsent(key, val);
                }
            }
            br.close();
            all.waitFor();
        } catch (Exception ignored) {
            // 跨平台 / 权限不足时静默失败
        }
    }

    private boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win");
    }
}

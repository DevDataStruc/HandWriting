package com.example.handwriting.config;

import com.example.handwriting.config.AppProperties.Cors;
import com.example.handwriting.config.AppProperties.Jwt;
import com.example.handwriting.config.AppProperties.RateLimit;
import com.example.handwriting.config.AppProperties.Storage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 启动配置审计 / 自检
 * 打印：JVM 信息 / 激活的 profile / 已加载的配置源 / HANDWRITE_* 环境变量
 *      / 解析后的 AppProperties / 关键 Spring 配置（DB/Redis/Storage/JWT）
 * 用途：排查 "占位符没解析" / "环境变量没读到" 等玄学问题
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigAuditRunner implements ApplicationRunner {

    private static final String PREFIX = "HANDWRITE_";
    private static final String DEV_KEY = System.lineSeparator() + "    │ ";

    private final AppProperties appProperties;
    private final Environment env;

    @Override
    public void run(ApplicationArguments args) {
        try {
            printHeader();
            printJvm();
            printProfiles();
            printPropertySources();
            printSystemEnv();
            printSpringPlaceholders();
            printAppProperties();
            printWarnings();
        } catch (Exception e) {
            log.error("[ConfigAudit] 自检失败: {}", e.getMessage(), e);
        }
    }

    // ============================================================
    //  1) 头部
    // ============================================================
    private void printHeader() {
        log.info("");
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║            HandWrite-client 启动配置审计 (Config Audit)         ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");
    }

    // ============================================================
    //  2) JVM / 主机
    // ============================================================
    private void printJvm() throws UnknownHostException {
        log.info("");
        log.info("┌── [1/6] JVM & Host ──────────────────────────────────────────");
        log.info("│  java.version        = {}", System.getProperty("java.version"));
        log.info("│  java.home           = {}", System.getProperty("java.home"));
        log.info("│  user.dir            = {}", System.getProperty("user.dir"));
        log.info("│  user.name           = {}", System.getProperty("user.name"));
        log.info("│  os.name             = {} {}", System.getProperty("os.name"), System.getProperty("os.arch"));
        log.info("│  hostname            = {}", InetAddress.getLocalHost().getHostName());
        log.info("│  PID                 = {}", ProcessHandle.current().pid());
    }

    // ============================================================
    //  3) Profile
    // ============================================================
    private void printProfiles() {
        log.info("");
        log.info("┌── [2/6] Spring Profile ─────────────────────────────────────");
        String[] profiles = env.getActiveProfiles();
        if (profiles.length == 0) {
            log.info("│  active profiles     = (default)");
        } else {
            log.info("│  active profiles     = {}", String.join(", ", profiles));
        }
        log.info("│  default profiles    = {}", Arrays.toString(env.getDefaultProfiles()));
    }

    // ============================================================
    //  4) 已加载的配置源
    // ============================================================
    private void printPropertySources() {
        log.info("");
        log.info("┌── [3/6] PropertySource 加载顺序 ──────────────────────────────");
        if (env instanceof ConfigurableEnvironment cfgEnv) {
            MutablePropertySources sources = cfgEnv.getPropertySources();
            int i = 1;
            for (PropertySource<?> ps : sources) {
                String name = ps.getName();
                String origin = ps.getClass().getSimpleName();
                log.info("│  {:>2}. {}  ({})", i++, name, origin);
            }
        } else {
            log.info("│  (非 ConfigurableEnvironment，无法枚举)");
        }
    }

    // ============================================================
    //  5) System.getenv() 里的 HANDWRITE_*
    // ============================================================
    private void printSystemEnv() {
        log.info("");
        log.info("┌── [4/6] 系统环境变量 HANDWRITE_* ────────────────────────────");
        Map<String, String> env = System.getenv();
        Map<String, String> matches = env.entrySet().stream()
                .filter(e -> e.getKey().startsWith(PREFIX))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a, TreeMap::new));
        if (matches.isEmpty()) {
            log.warn("│  ⚠️  未在 System.getenv() 中检测到任何 HANDWRITE_* 变量");
            log.warn("│  → 你的 .env 不会被自动加载（除非用 spring.config.import 或 IDE 配置）");
        } else {
            log.info("│  count = {}", matches.size());
            matches.forEach((k, v) -> log.info("│  {} = {}", k, mask(k, v)));
        }
    }

    // ============================================================
    //  6) Spring 占位符解析情况（关键）
    // ============================================================
    private void printSpringPlaceholders() {
        log.info("");
        log.info("┌── [5/6] Spring 解析后的运行时配置（占位符已替换） ──────────────");

        // Datasource
        String dbUrl = env.getProperty("spring.datasource.url", "");
        String dbUser = env.getProperty("spring.datasource.username", "");
        String dbPass = env.getProperty("spring.datasource.password", "");
        log.info("│  datasource.url       = {}", dbUrl);
        log.info("│  datasource.username  = {}", dbUser);
        log.info("│  datasource.password  = {}", mask("datasource.password", dbPass));
        if (containsPlaceholder(dbUrl)) {
            log.error("│  ❌ datasource.url 仍含未解析占位符：{}", dbUrl);
        }

        // Redis
        String redisHost = env.getProperty("spring.data.redis.host", "");
        String redisPort = env.getProperty("spring.data.redis.port", "");
        String redisPwd  = env.getProperty("spring.data.redis.password", "");
        log.info("│  redis.host           = {}", redisHost);
        log.info("│  redis.port           = {}", redisPort);
        log.info("│  redis.password       = {}", mask("redis.password", redisPwd));

        // Mail
        log.info("│  mail.host            = {}", env.getProperty("spring.mail.host"));
        log.info("│  mail.port            = {}", env.getProperty("spring.mail.port"));
        log.info("│  mail.username        = {}", env.getProperty("spring.mail.username"));

        // Server
        log.info("│  server.port          = {}", env.getProperty("server.port", "8080"));
        log.info("│  spring.profiles.active = {}", env.getProperty("spring.profiles.active", "default"));

        // Flyway / JPA
        log.info("│  flyway.enabled       = {}", env.getProperty("spring.flyway.enabled", "true"));
        log.info("│  jpa.ddl-auto         = {}", env.getProperty("spring.jpa.hibernate.ddl-auto", "none"));

        // Logging
        log.info("│  logging.file.name    = {}", env.getProperty("logging.file.name", "(console only)"));
        log.info("│  app log level        = {}", env.getProperty("logging.level.com.example.handwriting"));
    }

    // ============================================================
    //  7) AppProperties 解析结果
    // ============================================================
    private void printAppProperties() {
        log.info("");
        log.info("┌── [6/6] AppProperties 解析结果 ──────────────────────────────");

        // JWT
        Jwt jwt = appProperties.getJwt();
        log.info("│  app.jwt");
        log.info("│      .issuer                = {}", jwt.getIssuer());
        log.info("│      .accessTtlSeconds      = {}", jwt.getAccessTokenTtlSeconds());
        log.info("│      .refreshTtlSeconds     = {}", jwt.getRefreshTokenTtlSeconds());
        log.info("│      .header                = {}", jwt.getHeader());
        log.info("│      .prefix                = '{}'", jwt.getPrefix());
        log.info("│      .secret (length)       = {}", jwt.getSecret() == null ? "NULL ❌" : (jwt.getSecret().length() + " chars (masked)"));
        if (jwt.getSecret() != null && jwt.getSecret().length() < 32) {
            log.warn("│  ⚠️  JWT secret 长度 < 32 字符，生产环境建议至少 32 字符");
        }

        // CORS
        Cors cors = appProperties.getCors();
        log.info("│  app.cors");
        log.info("│      .allowedOrigins        = {}", cors.getAllowedOrigins());
        log.info("│      .allowedMethods        = {}", cors.getAllowedMethods());
        log.info("│      .allowCredentials     = {}", cors.isAllowCredentials());

        // Storage
        Storage st = appProperties.getStorage();
        log.info("│  app.storage");
        log.info("│      .provider              = {}", st.getProvider());
        log.info("│      .bucketCollect         = {}", st.getBucketCollect());
        log.info("│      .bucketAudit           = {}", st.getBucketAudit());
        log.info("│      .bucketBackup          = {}", st.getBucketBackup());
        log.info("│      .presignExpireSeconds  = {}", st.getPresignExpireSeconds());
        log.info("│      .publicBaseUrl         = {}", st.getPublicBaseUrl());
        switch (String.valueOf(st.getProvider()).toLowerCase()) {
            case "minio", "s3" -> {
                log.info("│      .endpoint              = {}", st.getEndpoint());
                log.info("│      .accessKey (masked)    = {}", mask("accessKey", st.getAccessKey()));
                log.info("│      .secretKey (masked)    = {}", mask("secretKey", st.getSecretKey()));
                log.info("│      .region                = {}", st.getRegion());
            }
            case "azure" -> {
                log.info("│      .azure.accountName     = {}", st.getAzureAccountName());
                log.info("│      .azure.cloud           = {}", st.getAzureCloud());
                boolean hasConn = st.getAzureConnectionString() != null && !st.getAzureConnectionString().isEmpty();
                boolean hasSas  = st.getAzureSasToken() != null && !st.getAzureSasToken().isEmpty();
                boolean hasKey  = st.getAzureAccountKey() != null && !st.getAzureAccountKey().isEmpty();
                log.info("│      .azure.credential      = {}",
                        hasConn ? "ConnectionString ✓" :
                        hasSas  ? "SAS Token ✓" :
                        hasKey  ? "AccountKey ✓" : "❌ NONE (Azure 将无法认证)");
            }
            default -> log.warn("│  ⚠️  未知 storage.provider={}", st.getProvider());
        }

        // RateLimit
        RateLimit rl = appProperties.getRatelimit();
        log.info("│  app.ratelimit");
        log.info("│      .authPerMinute         = {}", rl.getAuthPerMinute());
        log.info("│      .uploadPerMinute       = {}", rl.getUploadPerMinute());
    }

    // ============================================================
    //  8) 警告 / 错误
    // ============================================================
    private void printWarnings() {
        log.info("");
        log.info("┌── 启动自检总结 ─────────────────────────────────────────────");
        Set<String> warns = new LinkedHashSet<>();

        // 检查 1：System.getenv 中 HANDWRITE_ 数 vs Spring 解析后数
        long sysCount = System.getenv().keySet().stream().filter(k -> k.startsWith(PREFIX)).count();
        long resolvedCount = Arrays.stream(env.getActiveProfiles()).count() == 0 ? 0 : 0; // 占位
        if (sysCount == 0) {
            warns.add("System.getenv() 无 HANDWRITE_* 变量（依赖 spring.config.import 或 IDE env）");
        }

        // 检查 2：datasource.url 包含字面 ${}
        String dbUrl = env.getProperty("spring.datasource.url", "");
        if (containsPlaceholder(dbUrl)) {
            warns.add("datasource.url 仍含未解析占位符 → " + dbUrl);
        }

        // 检查 3：storage 凭证
        Storage st = appProperties.getStorage();
        if ("minio".equalsIgnoreCase(st.getProvider()) || "s3".equalsIgnoreCase(st.getProvider())) {
            if (isBlank(st.getEndpoint()) || isBlank(st.getAccessKey()) || isBlank(st.getSecretKey())) {
                warns.add("MinIO/S3 模式凭证缺失（endpoint/accessKey/secretKey 不能为空）");
            }
        } else if ("azure".equalsIgnoreCase(st.getProvider())) {
            if (isBlank(st.getAzureAccountName())
                    || (isBlank(st.getAzureAccountKey())
                    && isBlank(st.getAzureSasToken())
                    && isBlank(st.getAzureConnectionString()))) {
                warns.add("Azure 模式凭证缺失（需要 accountName + 任意一种 key/sas/conn）");
            }
        }

        // 检查 4：JWT secret
        if (appProperties.getJwt().getSecret() == null) {
            warns.add("JWT secret 为 null（应用将启动失败）");
        }

        if (warns.isEmpty()) {
            log.info("│  ✅ 所有关键配置已正确解析");
        } else {
            log.warn("│  ⚠️  发现 {} 个问题：", warns.size());
            warns.forEach(w -> log.warn("│     - {}", w));
        }
        log.info("└─────────────────────────────────────────────────────────────");
        log.info("");
    }

    // ============================================================
    //  工具方法
    // ============================================================
    private static String mask(String key, String value) {
        if (value == null) return "<null>";
        if (value.isEmpty()) return "<empty>";
        if (key == null) return "***";
        String k = key.toLowerCase();
        if (k.contains("password") || k.contains("secret") || k.contains("key")
                || k.contains("token") || k.contains("connectionstring")) {
            if (value.length() <= 8) return "***";
            return value.substring(0, 4) + "***" + value.substring(value.length() - 4) + " (len=" + value.length() + ")";
        }
        return value;
    }

    private static boolean containsPlaceholder(String s) {
        return s != null && s.contains("${");
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}

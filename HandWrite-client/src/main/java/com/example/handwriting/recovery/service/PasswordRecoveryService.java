package com.example.handwriting.recovery.service;

import com.example.handwriting.common.constant.CommonConstants;
import com.example.handwriting.common.exception.BizException;
import com.example.handwriting.common.result.ErrorCode;
import com.example.handwriting.recovery.dto.ForgotPasswordStartVO;
import com.example.handwriting.recovery.dto.PasswordResetTicketVO;
import com.example.handwriting.recovery.dto.SecurityAnswerItem;
import com.example.handwriting.recovery.dto.SecurityQuestionVO;
import com.example.handwriting.recovery.dto.VerifySecurityQuestionsDTO;
import com.example.handwriting.recovery.dto.VerifyTotpForResetDTO;
import com.example.handwriting.recovery.entity.PasswordResetToken;
import com.example.handwriting.recovery.entity.UserTotp;
import com.example.handwriting.recovery.repository.PasswordResetTokenRepository;
import com.example.handwriting.recovery.repository.UserSecurityQuestionRepository;
import com.example.handwriting.recovery.repository.UserTotpRepository;
import com.example.handwriting.user.entity.User;
import com.example.handwriting.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 密码恢复编排服务（同时驱动 2FA / 密保两种方案）
 *
 * <p>关键安全设计 — 防用户枚举 (User Enumeration)：
 * <ul>
 *   <li>start() 对"用户存在/不存在"返回 <b>完全一致</b> 的结构（maskedUsername + challengeId + 空 methods）</li>
 *   <li>IP 限流 + 用户名限流同时生效，<b>无论用户是否存在</b></li>
 *   <li>verifySecurityQuestions / verifyTotp 在用户不存在或答案错误时返回<b>同一错误</b></li>
 *   <li>密保问题正文只在"用户存在 且 选择 SQ"时返回；不存在则统一返回 3 个占位</li>
 * </ul>
 *
 * <pre>
 * 流程：
 *   1) start(username)        -> challengeId + 静态方法清单
 *   2a) getSecurityQuestions(challengeId) -> 返回 3 道问题（用户不存在时也返回占位）
 *   2b) verifySecurityQuestions / verifyTotp -> 通过后颁发 resetToken (5min)
 *   3) resetPassword(resetToken, newPwd) -> 一次性使用
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordRecoveryService {

    private static final SecureRandom RANDOM = new SecureRandom();

    public static final String METHOD_TOTP = "TOTP";
    public static final String METHOD_SECURITY_QUESTION = "SECURITY_QUESTION";

    public static final long CHALLENGE_TTL_SECONDS = 10 * 60L;     // challenge 10 分钟
    public static final long RESET_TOKEN_TTL_SECONDS = 5 * 60L;    // resetToken 5 分钟

    /** 用户名维度限流：1 次 / 1 分钟（无论用户存在与否） */
    public static final long RATE_LIMIT_USERNAME_TTL_SECONDS = 60L;
    public static final int  RATE_LIMIT_USERNAME_MAX = 1;

    /** IP 维度限流：10 次 / 5 分钟（防枚举） */
    public static final long RATE_LIMIT_IP_TTL_SECONDS = 5 * 60L;
    public static final int  RATE_LIMIT_IP_MAX = 10;

    /** 校验步骤（密保 / TOTP）的 IP 限流：20 次 / 5 分钟（防爆破） */
    public static final int  RATE_LIMIT_VERIFY_IP_MAX = 20;

    private final UserRepository userRepository;
    private final UserTotpRepository userTotpRepository;
    private final UserSecurityQuestionRepository userSecurityQuestionRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final SecurityQuestionService securityQuestionService;
    private final TotpService totpService;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;

    /* =============================================================
     *  Step 1：发起找回（关键入口，不暴露账号是否存在）
     * ============================================================= */

    /**
     * 发起找回
     * <p>对"用户存在/不存在"完全等价地处理：
     * <ol>
     *   <li>IP 限流（防单 IP 大量枚举）</li>
     *   <li>用户名限流（防单用户名被频繁骚扰）</li>
     *   <li>颁发 challengeId（用户不存在时也颁发，但内部记 -1）</li>
     *   <li>返回 maskedUsername + challengeId + 空 methods</li>
     * </ol>
     */
    public ForgotPasswordStartVO start(String username) {
        String clientIp = currentClientIp();

        // 1) IP 限流（任何输入都触发）
        enforceRateLimit(
                "recovery:ip", clientIp,
                RATE_LIMIT_IP_MAX, RATE_LIMIT_IP_TTL_SECONDS);

        // 2) 用户名限流（任何输入都触发，使用归一化的 username 作 key）
        String normalized = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        enforceRateLimit(
                "recovery:user", normalized,
                RATE_LIMIT_USERNAME_MAX, RATE_LIMIT_USERNAME_TTL_SECONDS);

        // 3) 颁发 challengeId（统一行为，不因用户是否存在而不同）
        String challengeId = newChallengeId();
        User user = userRepository.findByUsername(username).orElse(null);
        String userIdForChallenge = (user != null) ? user.getId().toString() : "-1";

        redisTemplate.opsForValue().set(
                String.format(CommonConstants.REDIS_KEY_RECOVERY_CHALLENGE, challengeId),
                userIdForChallenge,
                Duration.ofSeconds(CHALLENGE_TTL_SECONDS));

        // 4) 响应统一为空 methods（不暴露账号是否存在 / 启用了哪些方式）
        ForgotPasswordStartVO vo = new ForgotPasswordStartVO();
        vo.setMaskedUsername(maskUsername(username));
        vo.setChallengeId(challengeId);
        vo.setMethods(new ArrayList<>());
        return vo;
    }

    /* =============================================================
     *  Step 2a-0：获取密保问题（选 SQ 后调用）
     * ============================================================= */

    /**
     * 返回该 challengeId 对应的密保问题。
     * <p>用户存在 → 返回真实问题正文；用户不存在 → 返回 3 个占位（"第 1/2/3 道问题"）。
     * 这样前端 UI 完全一致，无法通过 API 响应区分账号是否存在。
     */
    public List<SecurityQuestionVO> getSecurityQuestions(String challengeId) {
        Long userId = peekChallengeUserId(challengeId);
        if (userId == null || userId <= 0) {
            // 用户不存在：返回 3 个占位
            return List.of(
                    placeholder(1), placeholder(2), placeholder(3)
            );
        }
        List<SecurityQuestionVO> list = securityQuestionService.listByUser(userId);
        // 真实问题不足 3 道时也补齐占位，保持前端渲染一致
        while (list.size() < 3) {
            list.add(placeholder(list.size() + 1));
        }
        return list;
    }

    /* =============================================================
     *  Step 2a：密保校验（统一错误信息）
     * ============================================================= */

    public PasswordResetTicketVO verifySecurityQuestions(VerifySecurityQuestionsDTO dto) {
        String clientIp = currentClientIp();
        enforceRateLimit("recovery:verify:ip", clientIp,
                RATE_LIMIT_VERIFY_IP_MAX, RATE_LIMIT_IP_TTL_SECONDS);

        // 取出 userId，<b>不区分"challenge 失效" 与 "用户不存在"</b>
        Long userId = consumeChallengeUserId(dto.getChallengeId());
        if (userId == null) {
            // 用户不存在（challenge 内 userId = -1）或 challenge 已被消费
            throw new BizException(ErrorCode.SECURITY_QUESTION_ANSWER_INVALID);
        }
        // 用户存在但未设置密保 / 答案错误 一律走同一错误
        try {
            securityQuestionService.verifyAll(userId, dto.getAnswers());
        } catch (BizException ex) {
            // 归一化所有密保相关错误为"答案错误"，不暴露具体原因
            throw new BizException(ErrorCode.SECURITY_QUESTION_ANSWER_INVALID);
        }
        return issueResetToken(userId, METHOD_SECURITY_QUESTION);
    }

    /* =============================================================
     *  Step 2b：TOTP 校验（统一错误信息）
     * ============================================================= */

    public PasswordResetTicketVO verifyTotp(VerifyTotpForResetDTO dto) {
        String clientIp = currentClientIp();
        enforceRateLimit("recovery:verify:ip", clientIp,
                RATE_LIMIT_VERIFY_IP_MAX, RATE_LIMIT_IP_TTL_SECONDS);

        Long userId = consumeChallengeUserId(dto.getChallengeId());
        if (userId == null) {
            // 用户不存在 或 challenge 已被消费
            throw new BizException(ErrorCode.TOTP_CODE_INVALID);
        }
        UserTotp totp = userTotpRepository.findByUserId(userId)
                .filter(t -> t.getConfirmedAt() != null && t.getStatus() == 0)
                .orElse(null);
        if (totp == null) {
            // 未绑定 2FA 也归一化为"口令错误"，不暴露
            throw new BizException(ErrorCode.TOTP_CODE_INVALID);
        }
        try {
            if (Boolean.TRUE.equals(dto.getUseRecoveryCode())) {
                totpService.verifyAndConsumeRecoveryCode(userId, dto.getRecoveryCode());
            } else {
                totpService.verifyCode(userId, dto.getCode());
            }
        } catch (BizException ex) {
            // 归一化所有 TOTP 相关错误为"口令错误"
            throw new BizException(ErrorCode.TOTP_CODE_INVALID);
        }
        return issueResetToken(userId, METHOD_TOTP);
    }

    /* =============================================================
     *  Step 3：重置密码
     * ============================================================= */

    @Transactional
    public void resetPassword(String resetToken, String newPassword) {
        if (resetToken == null || resetToken.isBlank()) {
            throw new BizException(ErrorCode.RECOVERY_TOKEN_INVALID);
        }
        PasswordResetToken entity = passwordResetTokenRepository.findByToken(resetToken)
                .orElseThrow(() -> new BizException(ErrorCode.RECOVERY_TOKEN_INVALID));
        if (entity.getUsed() != null && entity.getUsed() == 1) {
            throw new BizException(ErrorCode.RECOVERY_TOKEN_USED);
        }
        if (entity.getExpiredAt() != null && entity.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new BizException(ErrorCode.RECOVERY_TOKEN_INVALID);
        }

        User user = userRepository.findById(entity.getUserId())
                .orElseThrow(() -> new BizException(ErrorCode.RECOVERY_TOKEN_INVALID));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        entity.setUsed(1);
        passwordResetTokenRepository.save(entity);
        // 清空该用户所有 reset token，防止重放
        passwordResetTokenRepository.deleteByUserId(entity.getUserId());
    }

    /* =============================================================
     *  内部辅助
     * ============================================================= */

    /**
     * 通用限流（基于 Redis INCR + EXPIRE）
     * @param scope   限流维度（如 "recovery:ip" / "recovery:user"）
     * @param key     限流 key（IP 或用户名）
     * @param max     时间窗内允许的最大次数
     * @param ttlSec  时间窗秒数
     */
    private void enforceRateLimit(String scope, String key, int max, long ttlSec) {
        if (key == null || key.isBlank()) {
            return; // 兜底：拿不到 IP 时不阻断
        }
        String redisKey = String.format(CommonConstants.REDIS_KEY_RATELIMIT, scope, key);
        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count != null && count == 1L) {
            redisTemplate.expire(redisKey, Duration.ofSeconds(ttlSec));
        }
        if (count != null && count > max) {
            throw new BizException(ErrorCode.RATE_LIMIT);
        }
    }

    /**
     * 只读取 challengeId 对应的 userId，不删除 challenge。
     * 用于 step2a-0 拉取密保问题（可能多次访问）。
     * @return userId; 若 challenge 不存在 / 已过期 / 用户不存在 则返回 null
     */
    private Long peekChallengeUserId(String challengeId) {
        if (challengeId == null || challengeId.isBlank()) {
            return null;
        }
        Object v = redisTemplate.opsForValue().get(
                String.format(CommonConstants.REDIS_KEY_RECOVERY_CHALLENGE, challengeId));
        if (v == null) {
            return null;
        }
        try {
            long uid = Long.parseLong(v.toString());
            return uid > 0 ? uid : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * 消费 challengeId（一次性）。读取后立即删除。
     * @return userId; 若 challenge 不存在 / 已过期 / 用户不存在 则返回 null
     */
    private Long consumeChallengeUserId(String challengeId) {
        if (challengeId == null || challengeId.isBlank()) {
            return null;
        }
        String key = String.format(CommonConstants.REDIS_KEY_RECOVERY_CHALLENGE, challengeId);
        Object v = redisTemplate.opsForValue().get(key);
        if (v == null) {
            return null;
        }
        redisTemplate.delete(key);
        try {
            long uid = Long.parseLong(v.toString());
            return uid > 0 ? uid : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private PasswordResetTicketVO issueResetToken(Long userId, String method) {
        String token = generateResetToken();
        PasswordResetToken entity = new PasswordResetToken();
        entity.setToken(token);
        entity.setUserId(userId);
        entity.setMethod(method);
        entity.setUsed(0);
        entity.setExpiredAt(LocalDateTime.now().plusSeconds(RESET_TOKEN_TTL_SECONDS));
        passwordResetTokenRepository.save(entity);

        PasswordResetTicketVO vo = new PasswordResetTicketVO();
        vo.setResetToken(token);
        vo.setExpiresIn(RESET_TOKEN_TTL_SECONDS);
        return vo;
    }

    /**
     * 占位问题（用户不存在 / 未设置密保 时返回）
     */
    private static SecurityQuestionVO placeholder(int index) {
        SecurityQuestionVO vo = new SecurityQuestionVO();
        vo.setQuestionIndex(index);
        vo.setQuestionKey("");
        vo.setQuestionText("你的第 " + index + " 道密保问题");
        return vo;
    }

    /**
     * 取客户端 IP（兼容反向代理头）
     */
    private static String currentClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes)
                    RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "unknown";
            HttpServletRequest req = attrs.getRequest();
            String ip = req.getHeader("X-Forwarded-For");
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
            ip = req.getHeader("X-Real-IP");
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.trim();
            }
            return req.getRemoteAddr();
        } catch (Exception ex) {
            return "unknown";
        }
    }

    private static String newChallengeId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String generateResetToken() {
        byte[] buf = new byte[32];
        RANDOM.nextBytes(buf);
        StringBuilder sb = new StringBuilder();
        for (byte b : buf) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String maskUsername(String raw) {
        if (raw == null) return "";
        if (raw.length() <= 2) {
            return raw.charAt(0) + "***";
        }
        return raw.charAt(0) + "***" + raw.charAt(raw.length() - 1);
    }
}

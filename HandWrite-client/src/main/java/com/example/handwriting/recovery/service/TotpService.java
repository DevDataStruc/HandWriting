package com.example.handwriting.recovery.service;

import com.example.handwriting.common.exception.BizException;
import com.example.handwriting.common.result.ErrorCode;
import com.example.handwriting.recovery.dto.TotpSetupVO;
import com.example.handwriting.recovery.dto.TotpStatusVO;
import com.example.handwriting.recovery.entity.UserTotp;
import com.example.handwriting.recovery.repository.UserTotpRepository;
import com.example.handwriting.recovery.util.QrCodeUtil;
import com.example.handwriting.recovery.util.TotpUtil;
import com.example.handwriting.user.entity.User;
import com.example.handwriting.user.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 2FA（TOTP）服务
 *  - 初始化（生成 secret / 恢复码 / 二维码）
 *  - 确认绑定（必须用认证器产生的 6 位 code 校验通过才完成绑定）
 *  - 校验动态口令（含 lastUsedStep 防重放）
 *  - 校验一次性恢复码（用过的即作废）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TotpService {

    private static final int TOTP_WINDOW = 1;             // 校验 ±1 步
    private static final int RECOVERY_CODE_COUNT = 10;     // 一次性恢复码数量
    private static final int RECOVERY_CODE_LENGTH = 10;    // 5 字节 => 10 字符 Base32
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final UserRepository userRepository;
    private final UserTotpRepository userTotpRepository;
    private final PasswordEncoder passwordEncoder;

    /** 获取当前用户 2FA 状态 */
    public TotpStatusVO status(Long userId) {
        Optional<UserTotp> opt = userTotpRepository.findByUserId(userId);
        TotpStatusVO vo = new TotpStatusVO();
        if (opt.isEmpty() || opt.get().getStatus() != 0) {
            vo.setBound(false);
            return vo;
        }
        UserTotp t = opt.get();
        vo.setBound(true);
        vo.setIssuer(t.getIssuer());
        vo.setAccountLabel(t.getAccountLabel());
        vo.setConfirmedAt(t.getConfirmedAt());
        try {
            List<String> codes = MAPPER.readValue(t.getRecoveryCodes(), new TypeReference<>() {});
            vo.setRemainingRecoveryCodes(codes == null ? 0 : codes.size());
        } catch (Exception ex) {
            vo.setRemainingRecoveryCodes(0);
        }
        return vo;
    }

    /**
     * 初始化 2FA：
     *  - 若用户已绑定且 confirmed -> 抛 TOTP_ALREADY_BOUND
     *  - 若已有未确认的记录（confirmed_at 为空）-> 重新生成 secret / 恢复码
     *  - 恢复码以 BCrypt 密文入库，明文仅返回一次
     */
    @Transactional
    public TotpSetupVO setup(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_EXISTS));

        Optional<UserTotp> existing = userTotpRepository.findByUserId(userId);
        if (existing.isPresent() && existing.get().getConfirmedAt() != null) {
            throw new BizException(ErrorCode.TOTP_ALREADY_BOUND);
        }

        String secret = TotpUtil.generateSecret();
        List<String> plainCodes = generateRecoveryCodes();
        List<String> hashedCodes = plainCodes.stream()
                .map(passwordEncoder::encode)
                .toList();

        UserTotp entity = existing.orElseGet(UserTotp::new);
        entity.setUserId(userId);
        entity.setSecret(secret);
        entity.setIssuer("HandWrite");
        entity.setAccountLabel(user.getUsername());
        try {
            entity.setRecoveryCodes(MAPPER.writeValueAsString(hashedCodes));
        } catch (Exception ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR);
        }
        entity.setStatus(0);
        entity.setLastUsedStep(0L);
        userTotpRepository.save(entity);

        String otpauth = TotpUtil.buildOtpAuthUrl(entity.getIssuer(), entity.getAccountLabel(), secret);
        String qrBase64 = QrCodeUtil.toBase64Png(otpauth, 280);

        TotpSetupVO vo = new TotpSetupVO();
        vo.setSecret(secret);
        vo.setOtpauthUrl(otpauth);
        vo.setQrCodeBase64(qrBase64);
        vo.setRecoveryCodes(plainCodes);
        vo.setIssuer(entity.getIssuer());
        vo.setAccountLabel(entity.getAccountLabel());
        return vo;
    }

    /**
     * 确认绑定：用户必须用认证器中的 6 位 code 通过校验，才把 confirmedAt 写入
     */
    @Transactional
    public void confirmBinding(Long userId, String code) {
        UserTotp entity = userTotpRepository.findByUserId(userId)
                .orElseThrow(() -> new BizException(ErrorCode.TOTP_NOT_BOUND));
        if (entity.getConfirmedAt() != null) {
            throw new BizException(ErrorCode.TOTP_ALREADY_BOUND);
        }
        long step = TotpUtil.matchedStep(entity.getSecret(), code, TOTP_WINDOW);
        if (step < 0) {
            throw new BizException(ErrorCode.TOTP_CODE_INVALID);
        }
        entity.setConfirmedAt(LocalDateTime.now());
        entity.setLastUsedStep(step);
        userTotpRepository.save(entity);
    }

    /**
     * 校验动态口令（含防重放）：
     *  - 校验通过后立即推进 lastUsedStep，相同 step 不再通过
     *  - 用于「已登录用户」二次验证 / 「忘记密码」流程
     */
    @Transactional
    public void verifyCode(Long userId, String code) {
        UserTotp entity = userTotpRepository.findByUserId(userId)
                .orElseThrow(() -> new BizException(ErrorCode.TOTP_NOT_BOUND));
        if (entity.getConfirmedAt() == null) {
            throw new BizException(ErrorCode.TOTP_NOT_BOUND);
        }
        long step = TotpUtil.matchedStep(entity.getSecret(), code, TOTP_WINDOW);
        if (step < 0) {
            throw new BizException(ErrorCode.TOTP_CODE_INVALID);
        }
        if (step <= entity.getLastUsedStep()) {
            // 防重放：相同 / 更早的 step 一律拒绝
            throw new BizException(ErrorCode.TOTP_CODE_INVALID);
        }
        entity.setLastUsedStep(step);
        userTotpRepository.save(entity);
    }

    /** 用户解绑 2FA（必须先通过密码 / 已登录态校验，由 Controller 层把关） */
    @Transactional
    public void unbind(Long userId) {
        if (!userTotpRepository.existsByUserId(userId)) {
            throw new BizException(ErrorCode.TOTP_NOT_BOUND);
        }
        userTotpRepository.deleteByUserId(userId);
    }

    /**
     * 校验一次性恢复码：传入明文，命中后立即将该密文从列表中剔除（防止重复使用）
     * @return 命中的明文（仅日志用）
     */
    @Transactional
    public String verifyAndConsumeRecoveryCode(Long userId, String plainCode) {
        if (plainCode == null) {
            throw new BizException(ErrorCode.TOTP_RECOVERY_CODE_INVALID);
        }
        UserTotp entity = userTotpRepository.findByUserId(userId)
                .orElseThrow(() -> new BizException(ErrorCode.TOTP_NOT_BOUND));
        if (entity.getConfirmedAt() == null) {
            throw new BizException(ErrorCode.TOTP_NOT_BOUND);
        }
        try {
            List<String> hashed = MAPPER.readValue(entity.getRecoveryCodes(), new TypeReference<>() {});
            if (hashed == null || hashed.isEmpty()) {
                throw new BizException(ErrorCode.TOTP_RECOVERY_CODE_INVALID);
            }
            String normalized = plainCode.trim().toUpperCase().replaceAll("\\s+", "");
            List<String> remaining = new ArrayList<>(hashed.size());
            boolean matched = false;
            for (String h : hashed) {
                if (!matched && passwordEncoder.matches(normalized, h)) {
                    matched = true;
                } else {
                    remaining.add(h);
                }
            }
            if (!matched) {
                throw new BizException(ErrorCode.TOTP_RECOVERY_CODE_INVALID);
            }
            entity.setRecoveryCodes(MAPPER.writeValueAsString(remaining));
            userTotpRepository.save(entity);
            return normalized;
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("verify recovery code failed", ex);
            throw new BizException(ErrorCode.INTERNAL_ERROR);
        }
    }

    /** 生成 10 个 5 字节随机恢复码，Base32 编码后大写 */
    private List<String> generateRecoveryCodes() {
        // 选用 Base32 字符表（无 0/O/1/I 歧义）
        char[] alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < RECOVERY_CODE_COUNT; i++) {
            StringBuilder sb = new StringBuilder(RECOVERY_CODE_LENGTH);
            for (int j = 0; j < RECOVERY_CODE_LENGTH; j++) {
                sb.append(alphabet[RANDOM.nextInt(alphabet.length)]);
            }
            // 5 个一组，加横线便于阅读：ABCDE-FGHJK
            String raw = sb.toString();
            codes.add(raw.substring(0, 5) + "-" + raw.substring(5));
        }
        return Collections.unmodifiableList(codes);
    }
}

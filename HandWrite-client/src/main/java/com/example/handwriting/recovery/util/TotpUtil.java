package com.example.handwriting.recovery.util;

import org.apache.commons.codec.binary.Base32;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;

/**
 * TOTP（RFC 6238）核心算法：仅依赖 JDK 与 commons-codec。
 *  - 算法：HmacSHA1，6 位数字，30 秒步长（与 Google Authenticator / Microsoft Authenticator 一致）
 *  - 窗口：默认校验 ±1 步（容差 30s）
 */
public final class TotpUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base32 BASE32 = new Base32();

    private TotpUtil() {}

    /** 生成 20 字节（160 bit）随机密钥，返回 Base32 编码（无 padding） */
    public static String generateSecret() {
        byte[] buf = new byte[20];
        RANDOM.nextBytes(buf);
        return BASE32.encodeAsString(buf).replace("=", "");
    }

    /**
     * 计算指定时间步的 TOTP 6 位口令
     * @param base32Secret Base32 编码的密钥
     * @param timeStep     Unix 时间 / 30
     */
    public static String generateCode(String base32Secret, long timeStep) throws Exception {
        byte[] keyBytes = BASE32.decode(base32Secret);
        byte[] data = ByteBuffer.allocate(8).putLong(timeStep).array();
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(keyBytes, "HmacSHA1"));
        byte[] hash = mac.doFinal(data);
        int offset = hash[hash.length - 1] & 0x0F;
        int binary = ((hash[offset] & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);
        int otp = binary % 1_000_000;
        return String.format("%06d", otp);
    }

    /**
     * 校验用户输入的 6 位动态口令
     * @param windowSize  容忍的步数（±windowSize）
     */
    public static boolean verifyCode(String base32Secret, String userCode, int windowSize) {
        if (userCode == null || !userCode.matches("^\\d{6}$")) {
            return false;
        }
        long now = System.currentTimeMillis() / 1000L / 30L;
        try {
            for (long i = -windowSize; i <= windowSize; i++) {
                String code = generateCode(base32Secret, now + i);
                if (constantTimeEquals(code, userCode)) {
                    return true;
                }
            }
        } catch (Exception ex) {
            return false;
        }
        return false;
    }

    /** 返回匹配到的 step（用于防重放：服务端应记录 lastUsedStep） */
    public static long matchedStep(String base32Secret, String userCode, int windowSize) {
        if (userCode == null || !userCode.matches("^\\d{6}$")) {
            return -1L;
        }
        long now = System.currentTimeMillis() / 1000L / 30L;
        try {
            for (long i = -windowSize; i <= windowSize; i++) {
                long step = now + i;
                String code = generateCode(base32Secret, step);
                if (constantTimeEquals(code, userCode)) {
                    return step;
                }
            }
        } catch (Exception ex) {
            return -1L;
        }
        return -1L;
    }

    /** 构造 otpauth:// URL（Google Authenticator 标准格式） */
    public static String buildOtpAuthUrl(String issuer, String accountLabel, String secret) {
        // label 需要 URL 编码，issuer 同样的处理
        String encIssuer = urlEncode(issuer);
        String encLabel = urlEncode(issuer + ":" + accountLabel);
        return "otpauth://totp/" + encLabel
                + "?secret=" + secret
                + "&issuer=" + encIssuer
                + "&algorithm=SHA1&digits=6&period=30";
    }

    private static String urlEncode(String s) {
        StringBuilder sb = new StringBuilder();
        byte[] bytes = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        for (byte b : bytes) {
            char c = (char) (b & 0xFF);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '_' || c == '.' || c == '~') {
                sb.append(c);
            } else {
                sb.append('%').append(String.format("%02X", (int) c));
            }
        }
        return sb.toString();
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}

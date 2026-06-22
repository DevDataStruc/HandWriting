package com.example.handwriting.common.constant;

/**
 * 业务常量
 */
public final class CommonConstants {

    private CommonConstants() {}

    /** 角色编码 */
    public static final String ROLE_USER = "USER";
    public static final String ROLE_AUDITOR = "AUDITOR";
    public static final String ROLE_ADMIN = "ADMIN";

    /** 权限点编码 */
    public static final String PERM_SAMPLE_UPLOAD = "sample:upload";
    public static final String PERM_SAMPLE_DELETE = "sample:delete";
    public static final String PERM_AUDIT_APPROVE = "audit:approve";
    public static final String PERM_AUDIT_REJECT = "audit:reject";
    public static final String PERM_ADMIN_USER = "admin:user";
    public static final String PERM_ADMIN_ROLE = "admin:role";

    /** 样本状态 */
    public static final int SAMPLE_STATUS_PENDING = 0;
    public static final int SAMPLE_STATUS_APPROVED = 1;
    public static final int SAMPLE_STATUS_REJECTED = 2;

    /** 用户状态 */
    public static final int USER_STATUS_NORMAL = 0;
    public static final int USER_STATUS_DISABLED = 1;

    /** 字符分类 */
    public static final String CHAR_CATEGORY_HANZI = "HANZI";
    public static final String CHAR_CATEGORY_DIGIT = "DIGIT";
    public static final String CHAR_CATEGORY_LETTER = "LETTER";
    public static final String CHAR_CATEGORY_SYMBOL = "SYMBOL";

    /** Redis Key */
    public static final String REDIS_KEY_REFRESH = "auth:refresh:%s";
    public static final String REDIS_KEY_CAPTCHA = "auth:captcha:%s";
    public static final String REDIS_KEY_BLACKLIST = "auth:blacklist:%s";
    public static final String REDIS_KEY_RATELIMIT = "ratelimit:%s:%s";
    public static final String REDIS_KEY_DICT_CACHE = "dict:chars:%s";
    public static final String REDIS_KEY_STATS_DAILY = "stats:daily:%s";
    /** 密码恢复 challenge -> userId 映射（短 TTL） */
    public static final String REDIS_KEY_RECOVERY_CHALLENGE = "auth:recovery:challenge:%s";
    /** 密码恢复 token -> userId 映射（短 TTL） */
    public static final String REDIS_KEY_RECOVERY_TOKEN = "auth:recovery:token:%s";

    /** 文件存储桶 */
    public static final String BUCKET_COLLECT = "COLLECT";
    public static final String BUCKET_AUDIT = "AUDIT";
    public static final String BUCKET_BACKUP = "BACKUP";
}

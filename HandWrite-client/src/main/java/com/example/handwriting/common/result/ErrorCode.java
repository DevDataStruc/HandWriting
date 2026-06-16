package com.example.handwriting.common.result;

import lombok.Getter;

/**
 * 全局错误码枚举。
 * <p>
 * 编码规则：
 * <ul>
 *   <li>0 成功</li>
 *   <li>1xxx 通用客户端错误（参数、未登录、无权限）</li>
 *   <li>2xxx 业务异常（资源不存在、状态非法、冲突等）</li>
 *   <li>5xxx 服务端异常（数据库、第三方、未知）</li>
 * </ul>
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "ok"),

    // 1xxx 通用
    BAD_REQUEST(1000, "请求参数不合法"),
    UNAUTHORIZED(1001, "未登录或登录已过期"),
    TOKEN_INVALID(1002, "令牌无效"),
    FORBIDDEN(1003, "无访问权限"),
    NOT_FOUND(1004, "资源不存在"),
    METHOD_NOT_ALLOWED(1005, "请求方法不被允许"),
    RATE_LIMIT(1006, "请求过于频繁，请稍后再试"),
    CAPTCHA_INVALID(1007, "验证码错误或已过期"),
    IDEMPOTENT_REJECT(1008, "重复请求"),

    // 2xxx 业务
    USER_NOT_EXISTS(2001, "用户不存在"),
    USER_PASSWORD_INVALID(2002, "用户名或密码错误"),
    USER_DISABLED(2003, "账号已被禁用"),
    USER_ALREADY_EXISTS(2004, "用户名已存在"),
    SAMPLE_NOT_EXISTS(2101, "样本不存在"),
    SAMPLE_STATUS_INVALID(2102, "样本状态不允许该操作"),
    SAMPLE_OWNER_FORBIDDEN(2103, "非样本所有者禁止操作"),
    CHAR_DICT_NOT_EXISTS(2201, "字符字典项不存在"),
    AUDIT_ALREADY_DONE(2301, "样本已审核"),
    FILE_EMPTY(2401, "上传文件为空"),
    FILE_TYPE_INVALID(2402, "文件类型不被允许"),
    FILE_SIZE_EXCEED(2403, "文件大小超出限制"),
    FILE_UPLOAD_FAILED(2404, "文件上传失败"),
    SIGN_INVALID(2501, "签名已过期或无效"),
    PERMISSION_DENIED(2601, "权限不足"),

    // 5xxx 服务端
    INTERNAL_ERROR(5000, "服务异常，请稍后再试"),
    DB_ERROR(5100, "数据库异常"),
    REMOTE_ERROR(5200, "下游服务异常"),
    CONFIG_ERROR(5300, "服务端配置错误"),
    UNKNOWN_ERROR(5999, "未知错误");

    private final int code;
    private final String msg;

    ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}

/**
 * 通用 API 契约类型（与 docs/API.md §2-§4 一致）
 */

/** 统一响应结构 R<T> */
export interface ApiResponse<T = unknown> {
  code: number
  msg: string
  data: T
  ts: number
  success: boolean
}

/** 分页查询参数（Spring MVC `@ModelAttribute`） */
export interface PageQuery {
  pageNum?: number
  pageSize?: number
  keyword?: string
  orderBy?: string
  order?: 'asc' | 'desc'
  [key: string]: unknown
}

/** 分页结果（API.md §4.5） */
export interface PageResult<T = unknown> {
  total: number
  pageNum: number
  pageSize: number
  records: T[]
}

/**
 * 业务异常码（与后端 ErrorCode.java 严格对齐）
 * <p>注意：必须与后端 {@code com.example.handwriting.common.result.ErrorCode} 保持一致。
 * 修改时请同步双端，并在 PR 描述中标注"业务码变更"。</p>
 * <ul>
 *   <li>0      成功</li>
 *   <li>1xxx   通用（参数/认证/权限/限流/验证码）</li>
 *   <li>2xxx   业务校验（用户/样本/文件/密码恢复/2FA/密保）</li>
 *   <li>5xxx   服务端异常</li>
 * </ul>
 */
export const BusinessCode = {
  SUCCESS: 0,

  // 1xxx 通用
  BAD_REQUEST: 1000,
  UNAUTHORIZED: 1001, // 未登录或登录已过期
  TOKEN_INVALID: 1002, // 令牌无效（access/refresh 均用此码）
  FORBIDDEN: 1003, // 无访问权限
  NOT_FOUND: 1004, // 资源不存在
  METHOD_NOT_ALLOWED: 1005, // 请求方法不被允许
  RATE_LIMIT: 1006, // 请求过于频繁
  CAPTCHA_INVALID: 1007, // 验证码错误或已过期
  IDEMPOTENT_REJECT: 1008, // 重复请求

  // 2xxx 业务 - 用户
  USER_NOT_EXISTS: 2001,
  USER_PASSWORD_INVALID: 2002, // 用户名或密码错误
  USER_DISABLED: 2003, // 账号已被禁用
  USER_ALREADY_EXISTS: 2004, // 用户名已存在

  // 2xxx 业务 - 样本 / 字符
  SAMPLE_NOT_EXISTS: 2101,
  SAMPLE_STATUS_INVALID: 2102,
  SAMPLE_OWNER_FORBIDDEN: 2103,
  CHAR_DICT_NOT_EXISTS: 2201,
  AUDIT_ALREADY_DONE: 2301,

  // 2xxx 业务 - 文件
  FILE_EMPTY: 2401,
  FILE_TYPE_INVALID: 2402,
  FILE_SIZE_EXCEED: 2403,
  FILE_UPLOAD_FAILED: 2404,

  // 2xxx 业务 - 其他
  SIGN_INVALID: 2501,
  PERMISSION_DENIED: 2601,

  // 2xxx 业务 - 密码恢复 / 2FA / 密保
  RECOVERY_TOKEN_INVALID: 2700,
  RECOVERY_TOKEN_USED: 2701,
  RECOVERY_METHOD_UNAVAILABLE: 2702,
  TOTP_NOT_BOUND: 2703,
  TOTP_ALREADY_BOUND: 2704,
  TOTP_CODE_INVALID: 2705,
  TOTP_RECOVERY_CODE_INVALID: 2706,
  SECURITY_QUESTION_NOT_SET: 2707,
  SECURITY_QUESTION_ANSWER_INVALID: 2708,
  SECURITY_QUESTION_ALREADY_SET: 2709,

  // 5xxx 服务端
  INTERNAL_ERROR: 5000,
  DB_ERROR: 5100,
  REMOTE_ERROR: 5200, // 后端无 REDIS_ERROR，统一归到下游异常
  CONFIG_ERROR: 5300,
  UNKNOWN_ERROR: 5999,
} as const

export type BusinessCodeValue = (typeof BusinessCode)[keyof typeof BusinessCode]

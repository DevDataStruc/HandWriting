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
 * 业务异常码（与 ErrorCode.java §12 对齐）
 * - 0    成功
 * - 1xxx 用户/认证
 * - 2xxx 业务校验
 * - 4xxx 限流
 * - 5xxx 系统异常
 */
export const BusinessCode = {
  SUCCESS: 0,
  USER_PASSWORD_INVALID: 1001,
  CAPTCHA_INVALID: 1002,
  USER_DISABLED: 1003,
  USER_NOT_FOUND: 1004,
  USERNAME_EXISTS: 1005,
  TOKEN_INVALID: 1101,
  REFRESH_TOKEN_INVALID: 1102,
  TOKEN_MISSING: 1103,
  FORBIDDEN: 1104,
  SAMPLE_NOT_FOUND: 2001,
  SAMPLE_AUDITED: 2002,
  CHAR_NOT_FOUND: 2003,
  FILE_TOO_LARGE: 2004,
  FILE_TYPE_INVALID: 2005,
  PARAM_INVALID: 3001,
  RATE_LIMIT: 4001,
  INTERNAL_ERROR: 5000,
  DB_ERROR: 5100,
  REDIS_ERROR: 5200,
  CONFIG_ERROR: 5300,
  UNKNOWN: 9999,
} as const

export type BusinessCodeValue = (typeof BusinessCode)[keyof typeof BusinessCode]

/**
 * 通用 API 契约类型
 */

/**
 * 统一响应结构
 */
export interface ApiResponse<T = unknown> {
  code: number
  msg: string
  data: T
}

/**
 * 分页查询参数
 */
export interface PageQuery {
  pageNum?: number
  pageSize?: number
  keyword?: string
  orderBy?: string
  order?: 'asc' | 'desc'
  [key: string]: unknown
}

/**
 * 分页结果
 */
export interface PageResult<T = unknown> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
  pages?: number
}

/**
 * 业务异常码
 */
export const BusinessCode = {
  SUCCESS: 0,
  UNAUTHORIZED: 1001,
  FORBIDDEN: 1003,
  BUSINESS_ERROR: 2001,
  VALIDATION_ERROR: 2002,
  SERVER_ERROR: 5000,
} as const

export type BusinessCodeValue = (typeof BusinessCode)[keyof typeof BusinessCode]

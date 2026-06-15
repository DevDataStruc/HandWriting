import type { Sample, SampleQuery, SampleStatus } from './sample'
import type { PageResult } from './common'

/**
 * 审核操作请求
 */
export interface AuditActionRequest {
  reason?: string
}

/**
 * 审核结果
 */
export interface AuditResult {
  id: number | string
  status: SampleStatus
  reviewedAt: string
  reviewerId: number | string
}

/**
 * 批量审核请求
 */
export interface BatchAuditRequest {
  ids: (number | string)[]
  action: 'APPROVED' | 'REJECTED'
  reason?: string
}

/**
 * 批量审核结果
 */
export interface BatchAuditResult {
  successCount: number
  failedCount: number
  failedIds: (number | string)[]
}

/**
 * 待审核查询参数
 */
export interface AuditPendingQuery extends SampleQuery {
  charId?: number | string
}

/**
 * 待审核分页结果
 */
export type AuditPageResult = PageResult<Sample>

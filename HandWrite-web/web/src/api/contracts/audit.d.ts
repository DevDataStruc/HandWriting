import type { PageResult, PageQuery } from './common'
import type { SampleVO } from './sample'

/** 审核操作请求（POST /v1/audit/{id}/approve、/v1/audit/{id}/reject） */
export interface AuditDecisionDTO {
  reason?: string
}

/** 审核结果（POST /v1/audit/{id}/approve & /v1/audit/{id}/reject） */
export interface AuditResultVO {
  id: number
  status: number
  reviewedAt: string
  reviewerId: number
}

/** 待审核查询参数（GET /v1/audit/pending） */
export interface AuditPendingQuery extends PageQuery {
  charId?: number
  category?: string
}

/** 审核历史查询参数（GET /v1/audit/history） */
export interface AuditHistoryQuery extends PageQuery {
  /** 0 待审核 1 通过 2 驳回 */
  status?: number
  reviewerId?: number
  startDate?: string
  endDate?: string
}

/** 待审核 / 审核历史分页结果 */
export type AuditPageResult = PageResult<SampleVO>

/** 批量审核请求（前端扩展） */
export interface BatchAuditRequest {
  ids: (number | string)[]
  action: 'APPROVED' | 'REJECTED'
  reason?: string
}

/** 批量审核结果 */
export interface BatchAuditResult {
  successCount: number
  failedCount: number
  failedIds: number[]
}

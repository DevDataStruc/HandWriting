import request from '@/utils/request'
import type {
  AuditActionRequest,
  AuditPageResult,
  AuditPendingQuery,
  BatchAuditRequest,
  BatchAuditResult,
} from './contracts/audit'
import type { Sample } from './contracts/sample'

/** 待审核列表 */
export function fetchPendingAudits(params?: AuditPendingQuery): Promise<AuditPageResult> {
  return request.get<AuditPageResult>('/audit/pending', params)
}

/** 通过 */
export function approveSample(id: number | string, data?: AuditActionRequest): Promise<Sample> {
  return request.post<Sample>(`/audit/${id}/approve`, data || {})
}

/** 驳回 */
export function rejectSample(id: number | string, data: AuditActionRequest): Promise<Sample> {
  return request.post<Sample>(`/audit/${id}/reject`, data)
}

/** 批量审核 */
export function batchAudit(data: BatchAuditRequest): Promise<BatchAuditResult> {
  return request.post<BatchAuditResult>('/audit/batch', data)
}

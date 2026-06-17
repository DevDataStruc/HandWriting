import request from '@/utils/request'
import type {
  AuditDecisionDTO,
  AuditHistoryQuery,
  AuditPageResult,
  AuditPendingQuery,
  AuditResultVO,
  BatchAuditResult,
} from './contracts/audit'

/**
 * 审核模块（管理员）- 与 API.md §1.2 / §7.4 一致
 * 基础路径：/v1/audit
 */

/** GET /v1/audit/pending - 待审核列表 */
export function pendingAudits(params?: AuditPendingQuery): Promise<AuditPageResult> {
  return request.get<AuditPageResult>('/audit/pending', {
    ...params,
  })
}

/** GET /v1/audit/history - 审核历史 */
export function auditHistory(params?: AuditHistoryQuery): Promise<AuditPageResult> {
  return request.get<AuditPageResult>('/audit/history', {
    ...params,
  })
}

/** POST /v1/audit/{id}/approve - 审核通过 */
export function approve(id: number, data?: AuditDecisionDTO): Promise<AuditResultVO> {
  return request.post<AuditResultVO>(`/audit/${id}/approve`, data || {})
}

/** POST /v1/audit/{id}/reject - 审核驳回 */
export function reject(id: number, data: AuditDecisionDTO): Promise<AuditResultVO> {
  return request.post<AuditResultVO>(`/audit/${id}/reject`, data)
}

/**
 * 批量审核（前端扩展：API.md 未定义批量接口，前端循环单条调用）
 * - 通过 Promise.allSettled 并发执行
 * - 返回成功/失败数及失败 ID 列表
 */
export async function batchAudit(
  ids: (number | string)[],
  action: 'APPROVED' | 'REJECTED',
  reason?: string
): Promise<BatchAuditResult> {
  // ids 全部转换为 number 形式（后端 /v1/audit/{id}/* 路径参数只接受数字）
  const numericIds = ids.map((id) => (typeof id === 'string' ? Number(id) : id))
  const results = await Promise.allSettled(
    numericIds.map((id) => {
      const data: AuditDecisionDTO = action === 'REJECTED' ? { reason } : {}
      return action === 'APPROVED' ? approve(id, data) : reject(id, data)
    })
  )
  const failedIds: number[] = []
  let successCount = 0
  results.forEach((r, idx) => {
    if (r.status === 'fulfilled') {
      successCount += 1
    } else {
      failedIds.push(numericIds[idx])
    }
  })
  return {
    successCount,
    failedCount: ids.length - successCount,
    failedIds,
  }
}

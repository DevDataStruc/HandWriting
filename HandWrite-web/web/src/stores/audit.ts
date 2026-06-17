import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as auditApi from '@/api/audit'
import type {
  AuditDecisionDTO,
  AuditHistoryQuery,
  AuditPageResult,
  AuditPendingQuery,
  AuditResultVO,
  BatchAuditResult,
} from '@/api/contracts/audit'
import type { SampleVO } from '@/api/contracts/sample'

/**
 * 审核 store
 * 字段与 API.md §1.2 / §7.4 对齐
 *  - GET /v1/audit/pending
 *  - GET /v1/audit/history
 *  - POST /v1/audit/{id}/approve
 *  - POST /v1/audit/{id}/reject
 */
export const useAuditStore = defineStore('audit', () => {
  const pendingList = ref<SampleVO[]>([])
  const historyList = ref<SampleVO[]>([])
  const total = ref(0)
  const historyTotal = ref(0)
  const pageNum = ref(1)
  const pageSize = ref(10)
  const loading = ref(false)

  /** 待审核列表 */
  async function fetchPending(params?: AuditPendingQuery): Promise<AuditPageResult> {
    loading.value = true
    try {
      const query: AuditPendingQuery = {
        pageNum: params?.pageNum ?? pageNum.value,
        pageSize: params?.pageSize ?? pageSize.value,
        ...params,
      }
      const res = await auditApi.pendingAudits(query)
      pendingList.value = res.records
      total.value = res.total
      pageNum.value = res.pageNum
      pageSize.value = res.pageSize
      return res
    } finally {
      loading.value = false
    }
  }

  /** 审核历史 */
  async function fetchHistory(params?: AuditHistoryQuery): Promise<AuditPageResult> {
    loading.value = true
    try {
      const query: AuditHistoryQuery = {
        pageNum: params?.pageNum ?? pageNum.value,
        pageSize: params?.pageSize ?? pageSize.value,
        ...params,
      }
      const res = await auditApi.auditHistory(query)
      historyList.value = res.records
      historyTotal.value = res.total
      pageNum.value = res.pageNum
      pageSize.value = res.pageSize
      return res
    } finally {
      loading.value = false
    }
  }

  /** POST /v1/audit/{id}/approve */
  async function approve(id: number, data?: AuditDecisionDTO): Promise<AuditResultVO> {
    const res = await auditApi.approve(id, data)
    pendingList.value = pendingList.value.filter((s) => Number(s.id) !== Number(id))
    total.value = Math.max(0, total.value - 1)
    return res
  }

  /** POST /v1/audit/{id}/reject */
  async function reject(id: number, data: AuditDecisionDTO): Promise<AuditResultVO> {
    const res = await auditApi.reject(id, data)
    pendingList.value = pendingList.value.filter((s) => Number(s.id) !== Number(id))
    total.value = Math.max(0, total.value - 1)
    return res
  }

  /**
   * 批量审核（前端循环单条）
   *  - 成功/失败的 id 列表
   *  - 失败不会回滚已成功的
   */
  async function batchAudit(
    ids: (number | string)[],
    action: 'APPROVED' | 'REJECTED',
    reason?: string
  ): Promise<BatchAuditResult> {
    const res = await auditApi.batchAudit(ids, action, reason)
    // 成功项移出待审核列表
    if (res.successCount > 0) {
      const failedSet = new Set(res.failedIds.map((x) => String(x)))
      pendingList.value = pendingList.value.filter((s) => failedSet.has(String(s.id)))
      total.value = Math.max(0, total.value - res.successCount)
    }
    return res
  }

  function reset(): void {
    pendingList.value = []
    historyList.value = []
    total.value = 0
    historyTotal.value = 0
    pageNum.value = 1
    pageSize.value = 10
  }

  return {
    pendingList,
    historyList,
    total,
    historyTotal,
    pageNum,
    pageSize,
    loading,
    fetchPending,
    fetchHistory,
    approve,
    reject,
    batchAudit,
    reset,
  }
})

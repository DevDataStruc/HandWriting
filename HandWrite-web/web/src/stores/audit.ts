import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as auditApi from '@/api/audit'
import type {
  AuditPageResult,
  AuditPendingQuery,
  BatchAuditRequest,
  BatchAuditResult,
} from '@/api/contracts/audit'
import type { Sample } from '@/api/contracts/sample'

export const useAuditStore = defineStore('audit', () => {
  const pendingList = ref<Sample[]>([])
  const total = ref(0)
  const pageNum = ref(1)
  const pageSize = ref(10)
  const loading = ref(false)

  async function fetchPending(params?: AuditPendingQuery): Promise<AuditPageResult> {
    loading.value = true
    try {
      const query: AuditPendingQuery = {
        pageNum: params?.pageNum ?? pageNum.value,
        pageSize: params?.pageSize ?? pageSize.value,
        ...params,
      }
      const res = await auditApi.fetchPendingAudits(query)
      pendingList.value = res.list
      total.value = res.total
      pageNum.value = res.pageNum
      pageSize.value = res.pageSize
      return res
    } finally {
      loading.value = false
    }
  }

  async function approve(id: number | string): Promise<Sample> {
    const res = await auditApi.approveSample(id, {})
    pendingList.value = pendingList.value.filter((s) => s.id !== id)
    total.value = Math.max(0, total.value - 1)
    return res
  }

  async function reject(id: number | string, reason: string): Promise<Sample> {
    const res = await auditApi.rejectSample(id, { reason })
    pendingList.value = pendingList.value.filter((s) => s.id !== id)
    total.value = Math.max(0, total.value - 1)
    return res
  }

  async function batchAudit(data: BatchAuditRequest): Promise<BatchAuditResult> {
    const res = await auditApi.batchAudit(data)
    if (res.successCount > 0) {
      await fetchPending()
    }
    return res
  }

  function reset(): void {
    pendingList.value = []
    total.value = 0
    pageNum.value = 1
    pageSize.value = 10
  }

  return {
    pendingList,
    total,
    pageNum,
    pageSize,
    loading,
    fetchPending,
    approve,
    reject,
    batchAudit,
    reset,
  }
})

import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as sampleApi from '@/api/sample'
import type { Sample, SamplePageResult, SampleQuery } from '@/api/contracts/sample'
import { storage } from '@/utils/storage'

export interface LocalDraft extends Sample {
  local: true
}

const LOCAL_DRAFTS_KEY = 'local-drafts'

function readLocalDrafts(): LocalDraft[] {
  return storage.get<LocalDraft[]>(LOCAL_DRAFTS_KEY, []) ?? []
}

function writeLocalDrafts(list: LocalDraft[]): void {
  storage.set(LOCAL_DRAFTS_KEY, list.slice(0, 200))
}

export const useSampleStore = defineStore('sample', () => {
  const list = ref<Sample[]>([])
  const total = ref(0)
  const pageNum = ref(1)
  const pageSize = ref(10)
  const loading = ref(false)
  const uploadProgress = ref(0)
  const uploading = ref(false)
  const currentDetail = ref<Sample | null>(null)
  /** 本地草稿（离线/接口失败兜底） */
  const localDrafts = ref<LocalDraft[]>(readLocalDrafts())

  async function fetchPage(params?: SampleQuery): Promise<SamplePageResult> {
    loading.value = true
    try {
      const query: SampleQuery = {
        pageNum: params?.pageNum ?? pageNum.value,
        pageSize: params?.pageSize ?? pageSize.value,
        ...params,
      }
      const res = await sampleApi.fetchMySamples(query)
      // 合并本地草稿，确保用户能看到"离线存稿"
      const locals = readLocalDrafts()
      const merged: Sample[] = [...locals, ...res.list]
      list.value = merged
      total.value = res.total + locals.length
      pageNum.value = res.pageNum
      pageSize.value = res.pageSize
      return res
    } finally {
      loading.value = false
    }
  }

  function addLocalDraft(draft: LocalDraft): void {
    const list = readLocalDrafts()
    list.unshift(draft)
    writeLocalDrafts(list)
    localDrafts.value = list
    // 立即同步到当前 list（无需等下一次 fetchPage）
    if (pageSize.value > 0) {
      const current = list.value.slice()
      current.unshift(draft)
      list.value = current.slice(0, pageSize.value)
    }
    total.value += 1
  }

  function removeLocalDraft(id: number | string): void {
    const filtered = readLocalDrafts().filter((d) => String(d.id) !== String(id))
    writeLocalDrafts(filtered)
    localDrafts.value = filtered
    list.value = list.value.filter((s) => String(s.id) !== String(id))
    total.value = Math.max(0, total.value - 1)
    if (currentDetail.value?.id === id) currentDetail.value = null
  }

  async function upload(
    charId: number | string,
    blob: Blob,
    meta?: { duration?: number; strokeCount?: number; remark?: string }
  ): Promise<Sample> {
    uploading.value = true
    uploadProgress.value = 0
    try {
      const res = await sampleApi.uploadSample(
        { charId, deviceInfo: navigator.userAgent, ...meta },
        blob,
        (p) => (uploadProgress.value = p)
      )
      // 刷新列表第一项
      if (list.value.length >= pageSize.value) {
        list.value.pop()
      }
      return res as unknown as Sample
    } finally {
      uploading.value = false
      uploadProgress.value = 100
      setTimeout(() => (uploadProgress.value = 0), 800)
    }
  }

  async function fetchDetail(id: number | string): Promise<Sample> {
    // 本地草稿优先
    if (String(id).startsWith('local-')) {
      const found = readLocalDrafts().find((d) => d.id === id)
      if (found) {
        currentDetail.value = found
        return found
      }
    }
    const data = await sampleApi.fetchSampleDetail(id)
    currentDetail.value = data
    return data
  }

  async function remove(id: number | string): Promise<void> {
    if (String(id).startsWith('local-')) {
      removeLocalDraft(id)
      return
    }
    await sampleApi.deleteSample(id)
    list.value = list.value.filter((s) => s.id !== id)
    total.value = Math.max(0, total.value - 1)
    if (currentDetail.value?.id === id) currentDetail.value = null
  }

  async function removeMany(ids: (number | string)[]): Promise<void> {
    if (ids.length === 0) return
    const set = new Set(ids.map(String))
    // 本地草稿先行
    const locals = readLocalDrafts().filter((d) => !set.has(String(d.id)))
    const remoteIds = ids.filter((id) => !String(id).startsWith('local-'))
    writeLocalDrafts(locals)
    localDrafts.value = locals
    if (remoteIds.length > 0) {
      await sampleApi.batchDeleteSamples(remoteIds)
    }
    list.value = list.value.filter((s) => !set.has(String(s.id)))
    total.value = Math.max(0, total.value - ids.length)
    if (currentDetail.value && set.has(String(currentDetail.value.id))) {
      currentDetail.value = null
    }
  }

  function reset(): void {
    list.value = []
    total.value = 0
    pageNum.value = 1
    pageSize.value = 10
    currentDetail.value = null
  }

  return {
    list,
    total,
    pageNum,
    pageSize,
    loading,
    uploadProgress,
    uploading,
    currentDetail,
    localDrafts,
    fetchPage,
    upload,
    fetchDetail,
    remove,
    removeMany,
    addLocalDraft,
    removeLocalDraft,
    reset,
  }
})

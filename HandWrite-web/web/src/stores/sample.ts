import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as sampleApi from '@/api/sample'
import type { Sample, SamplePageResult, SampleQuery } from '@/api/contracts/sample'

export const useSampleStore = defineStore('sample', () => {
  const list = ref<Sample[]>([])
  const total = ref(0)
  const pageNum = ref(1)
  const pageSize = ref(10)
  const loading = ref(false)
  const uploadProgress = ref(0)
  const uploading = ref(false)
  const currentDetail = ref<Sample | null>(null)

  async function fetchPage(params?: SampleQuery): Promise<SamplePageResult> {
    loading.value = true
    try {
      const query: SampleQuery = {
        pageNum: params?.pageNum ?? pageNum.value,
        pageSize: params?.pageSize ?? pageSize.value,
        ...params,
      }
      const res = await sampleApi.fetchMySamples(query)
      list.value = res.list
      total.value = res.total
      pageNum.value = res.pageNum
      pageSize.value = res.pageSize
      return res
    } finally {
      loading.value = false
    }
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
    const data = await sampleApi.fetchSampleDetail(id)
    currentDetail.value = data
    return data
  }

  async function remove(id: number | string): Promise<void> {
    await sampleApi.deleteSample(id)
    list.value = list.value.filter((s) => s.id !== id)
    total.value = Math.max(0, total.value - 1)
    if (currentDetail.value?.id === id) currentDetail.value = null
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
    fetchPage,
    upload,
    fetchDetail,
    remove,
    reset,
  }
})

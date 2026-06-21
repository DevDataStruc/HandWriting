import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as sampleApi from '@/api/sample'
import type {
  LocalDraft,
  SamplePageQuery,
  SamplePageResult,
  SampleVO,
} from '@/api/contracts/sample'
import { storage } from '@/utils/storage'

/**
 * 本地草稿（离线/上传失败兜底）
 * - id 以 "local-" 开头，便于 MySamples 视图识别
 * 类型定义见 api/contracts/sample.d.ts
 */

const LOCAL_DRAFTS_KEY = 'local-drafts'

function readLocalDrafts(): LocalDraft[] {
  return storage.get<LocalDraft[]>(LOCAL_DRAFTS_KEY, []) ?? []
}

function writeLocalDrafts(list: LocalDraft[]): void {
  storage.set(LOCAL_DRAFTS_KEY, list.slice(0, 200))
}

function asNumberId(id: number | string): number | string {
  if (typeof id === 'number') return id
  if (typeof id === 'string' && id.startsWith('local-')) return id
  const n = Number(id)
  return Number.isFinite(n) ? n : id
}

export const useSampleStore = defineStore('sample', () => {
  const list = ref<SampleVO[]>([])
  const total = ref(0)
  const pageNum = ref(1)
  const pageSize = ref(10)
  const loading = ref(false)
  const uploadProgress = ref(0)
  const uploading = ref(false)
  const currentDetail = ref<SampleVO | null>(null)
  /** 本地草稿 */
  const localDrafts = ref<LocalDraft[]>(readLocalDrafts())

  /**
   * 分页查询我的样本（GET /v1/sample/page）
   * 后端返回的 records 已经是当前用户的样本，本地草稿合并到列表头部
   */
  async function fetchPage(params?: SamplePageQuery): Promise<SamplePageResult> {
    loading.value = true
    try {
      const query: SamplePageQuery = {
        pageNum: params?.pageNum ?? pageNum.value,
        pageSize: params?.pageSize ?? pageSize.value,
        ...params,
      }
      const res = await sampleApi.pageMySamples(query)
      const locals = readLocalDrafts()
      const merged: SampleVO[] = [...locals, ...res.records]
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
    const cur = readLocalDrafts()
    cur.unshift(draft)
    writeLocalDrafts(cur)
    localDrafts.value = cur
    if (pageSize.value > 0) {
      const merged = list.value.slice()
      merged.unshift(draft)
      list.value = merged.slice(0, pageSize.value)
    }
    total.value += 1
  }

  function removeLocalDraft(id: number | string): void {
    const filtered = readLocalDrafts().filter((d) => String(d.id) !== String(id))
    writeLocalDrafts(filtered)
    localDrafts.value = filtered
    list.value = list.value.filter((s) => String(s.id) !== String(id))
    total.value = Math.max(0, total.value - 1)
    if (currentDetail.value && String(currentDetail.value.id) === String(id)) {
      currentDetail.value = null
    }
  }

  /**
   * 完整上传流程（改造后，本地存储单调用）：
   *   1) POST /v1/sample/upload（multipart） 一次性把文件 + 元数据发给后端
   *      - 后端将文件落到 storage/<userId>/<charId>/<timestamp>_<uuid>.<ext>
   *      - 同字符可重复上传，每份文件对应一条 Sample 记录
   *
   * 失败兜底：把 blob 持久化到 localStorage，标记为 local-draft
   */
  async function upload(
    charId: number,
    blob: Blob,
    meta?: { duration?: number; strokeCount?: number; remark?: string }
  ): Promise<SampleVO> {
    uploading.value = true
    uploadProgress.value = 0
    try {
      const device = typeof navigator !== 'undefined' ? navigator.userAgent : ''
      const vo = await sampleApi.uploadSampleFile(
        blob,
        charId,
        device,
        (p) => (uploadProgress.value = p)
      )
      uploadProgress.value = 100

      const sample: SampleVO = {
        id: vo.id,
        userId: 0,
        charId: vo.charId,
        fileKey: '',
        fileUrl: vo.fileUrl,
        fileSize: blob.size,
        device,
        status: vo.status,
        createTime: vo.createTime,
        remark: meta?.remark,
      }
      if (list.value.length >= pageSize.value) list.value.pop()
      list.value = [sample, ...list.value]
      total.value += 1
      return sample
    } finally {
      uploading.value = false
      setTimeout(() => (uploadProgress.value = 0), 800)
    }
  }

  /**
   * 重新上传笔迹：multipart 单调用。
   */
  async function update(
    id: number | string,
    charId: number,
    blob: Blob,
    meta?: { duration?: number; strokeCount?: number; remark?: string }
  ): Promise<SampleVO> {
    uploading.value = true
    uploadProgress.value = 0
    try {
      const updated = await sampleApi.updateSampleFile(
        id,
        blob,
        charId,
        (p) => (uploadProgress.value = p)
      )
      uploadProgress.value = 100

      const idx = list.value.findIndex((s) => String(s.id) === String(id))
      const next: SampleVO = {
        ...updated,
        duration: meta?.duration,
        strokeCount: meta?.strokeCount,
        remark: meta?.remark,
      }
      if (idx >= 0) {
        list.value[idx] = next
      } else if (currentDetail.value && String(currentDetail.value.id) === String(id)) {
        currentDetail.value = next
      }
      return next
    } finally {
      uploading.value = false
      setTimeout(() => (uploadProgress.value = 0), 800)
    }
  }

  /** GET /v1/sample/{id} */
  async function fetchDetail(id: number | string): Promise<SampleVO> {
    if (String(id).startsWith('local-')) {
      const found = readLocalDrafts().find((d) => d.id === id)
      if (found) {
        currentDetail.value = found
        return found
      }
      throw new Error('本地草稿不存在')
    }
    const data = await sampleApi.getSample(asNumberId(id))
    currentDetail.value = data
    return data
  }

  /** DELETE /v1/sample/{id} */
  async function remove(id: number | string): Promise<void> {
    if (String(id).startsWith('local-')) {
      removeLocalDraft(id)
      return
    }
    await sampleApi.deleteSample(asNumberId(id))
    list.value = list.value.filter((s) => String(s.id) !== String(id))
    total.value = Math.max(0, total.value - 1)
    if (currentDetail.value && String(currentDetail.value.id) === String(id)) {
      currentDetail.value = null
    }
  }

  /**
   * 批量删除：
   * - 本地草稿走本地存储
   * - 服务端样本调后端（API.md 未定义批量删除，逐条调）
   */
  async function removeMany(ids: (number | string)[]): Promise<void> {
    if (ids.length === 0) return
    const set = new Set(ids.map(String))
    const locals = readLocalDrafts().filter((d) => !set.has(String(d.id)))
    writeLocalDrafts(locals)
    localDrafts.value = locals

    const remoteIds = ids.filter((id) => !String(id).startsWith('local-')).map(asNumberId)
    for (const id of remoteIds) {
      try {
        await sampleApi.deleteSample(id)
      } catch (err) {
        console.warn('[sample] removeMany partial failure', id, err)
      }
    }
    list.value = list.value.filter((s) => !set.has(String(s.id)))
    total.value = Math.max(0, total.value - ids.length)
    if (currentDetail.value && set.has(String(currentDetail.value.id))) {
      currentDetail.value = null
    }
  }

  /** 把当前 blob 落盘到本地草稿箱 */
  function persistDraft(
    charId: number,
    blob: Blob,
    dataUrl: string,
    meta: { strokeCount: number; duration: number; remark: string }
  ): LocalDraft {
    const id = `local-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`
    const draft: LocalDraft = {
      id,
      userId: 0,
      charId,
      fileKey: id,
      fileUrl: dataUrl,
      imageUrl: dataUrl,
      fileSize: blob.size,
      device: typeof navigator !== 'undefined' ? navigator.userAgent : '',
      status: 0, // 待审核
      createTime: new Date().toISOString(),
      duration: meta.duration,
      strokeCount: meta.strokeCount,
      remark: meta.remark,
      local: true,
    }
    addLocalDraft(draft)
    return draft
  }

  /**
   * 同步本地草稿到服务端（在网络恢复时调用）
   * 遍历所有 local-draft，签名 → 直传 → 落库 → 删除本地
   */
  async function flushLocalDrafts(): Promise<{ success: number; failed: number }> {
    const list = readLocalDrafts()
    let success = 0
    let failed = 0
    for (const d of list) {
      try {
        // 这里需要一个 dataURL → Blob 的转换
        const blob = await (await fetch(d.imageUrl)).blob()
        await upload(d.charId, blob, {
          duration: d.duration,
          strokeCount: d.strokeCount,
          remark: d.remark,
        })
        removeLocalDraft(d.id)
        success++
      } catch (err) {
        console.warn('[sample] flushLocalDrafts failed', d.id, err)
        failed++
      }
    }
    return { success, failed }
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
    update,
    fetchDetail,
    remove,
    removeMany,
    addLocalDraft,
    removeLocalDraft,
    persistDraft,
    flushLocalDrafts,
    reset,
  }
})

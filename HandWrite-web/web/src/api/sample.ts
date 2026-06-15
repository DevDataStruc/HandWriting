import request from '@/utils/request'
import type {
  Sample,
  SamplePageResult,
  SampleQuery,
  SampleUploadMeta,
  SampleUploadResponse,
} from './contracts/sample'

/** 上传手写体样本 */
export function uploadSample(
  meta: SampleUploadMeta,
  blob: Blob,
  onProgress?: (p: number) => void
): Promise<SampleUploadResponse> {
  const form = new FormData()
  form.append('file', blob, `sample_${Date.now()}.png`)
  form.append('charId', String(meta.charId))
  if (meta.deviceInfo) form.append('deviceInfo', meta.deviceInfo)
  if (meta.duration != null) form.append('duration', String(meta.duration))
  if (meta.strokeCount != null) form.append('strokeCount', String(meta.strokeCount))
  if (meta.remark) form.append('remark', meta.remark)
  return request.upload<SampleUploadResponse>('/sample/upload', form, onProgress)
}

/** 分页查询我的样本 */
export function fetchMySamples(params?: SampleQuery): Promise<SamplePageResult> {
  return request.get<SamplePageResult>('/sample/page', params)
}

/** 样本详情 */
export function fetchSampleDetail(id: number | string): Promise<Sample> {
  return request.get<Sample>(`/sample/${id}`)
}

/** 删除样本 */
export function deleteSample(id: number | string): Promise<void> {
  return request.delete<void>(`/sample/${id}`)
}

/** 批量删除 */
export function batchDeleteSamples(ids: (number | string)[]): Promise<void> {
  return request.post<void>('/sample/batch-delete', { ids })
}

import request from '@/utils/request'
import type {
  SamplePageQuery,
  SamplePageResult,
  SampleUpdateDTO,
  SampleUploadDTO,
  SampleUploadVO,
  SampleVO,
} from './contracts/sample'

/**
 * 样本模块 - 与 API.md §1.2 / §7 一致
 * 基础路径：/v1/sample
 *
 * 上传流程（API.md §8 改造后，本地存储版）：
 *   1) POST /v1/sample/upload multipart(file, charId[, device]) 直接落盘后端 storage/ 目录
 *   - 旧链路（签名 → 对象存储直传 → 落库）已弃用，本地存储走单次 multipart
 */

/** POST /v1/sample/upload - multipart 上传（文件 + charId + 可选 device） */
export function uploadSampleFile(
  file: Blob,
  charId: number,
  device?: string,
  onProgress?: (percent: number) => void
): Promise<SampleUploadVO> {
  const form = new FormData()
  form.append('file', file)
  form.append('charId', String(charId))
  if (device) form.append('device', device)
  return request.upload<SampleUploadVO>('/sample/upload', form, onProgress)
}

/** PUT /v1/sample/{id} - multipart 重新上传笔迹（替换旧文件） */
export function updateSampleFile(
  id: number | string,
  file: Blob,
  charId?: number,
  onProgress?: (percent: number) => void
): Promise<SampleVO> {
  const form = new FormData()
  form.append('file', file)
  if (charId != null) form.append('charId', String(charId))
  return request.upload<SampleVO>(`/sample/${id}`, form, onProgress)
}

/**
 * 旧版：JSON 落库（已弃用，保留仅为类型/调用方兼容性兜底）。
 * 新链路请改用 uploadSampleFile。
 * @deprecated since 2025; use {@link uploadSampleFile}
 */
export function uploadSample(data: SampleUploadDTO): Promise<SampleUploadVO> {
  return request.post<SampleUploadVO>('/sample/upload', data)
}

/** GET /v1/sample/page - 分页查询我的样本 */
export function pageMySamples(params?: SamplePageQuery): Promise<SamplePageResult> {
  return request.get<SamplePageResult>('/sample/page', {
    ...params,
  })
}

/** GET /v1/sample/{id} - 样本详情 */
export function getSample(id: number | string): Promise<SampleVO> {
  return request.get<SampleVO>(`/sample/${id}`)
}

/** DELETE /v1/sample/{id} - 删除样本 */
export function deleteSample(id: number | string): Promise<void> {
  return request.delete<void>(`/sample/${id}`)
}

/** PUT /v1/sample/{id} - JSON 版（已弃用）。新链路请用 updateSampleFile。 */
export function updateSample(
  id: number | string,
  data: SampleUpdateDTO
): Promise<SampleVO> {
  return request.put<SampleVO>(`/sample/${id}`, data)
}

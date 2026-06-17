import request from '@/utils/request'
import type {
  SamplePageQuery,
  SamplePageResult,
  SampleUploadDTO,
  SampleUploadVO,
  SampleVO,
} from './contracts/sample'

/**
 * 样本模块 - 与 API.md §1.2 / §7 一致
 * 基础路径：/v1/sample
 *
 * 上传流程（API.md §8）：
 *   1) POST /v1/file/sign 拿直传签名  （见 file.service）
 *   2) PUT 直传到对象存储
 *   3) POST /v1/sample/upload 落库（仅元数据）
 */

/** POST /v1/sample/upload - 上传样本（元数据） */
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

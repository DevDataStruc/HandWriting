import request from '@/utils/request'
import type { FileSignDTO, FileSignVO } from './contracts/file'

/**
 * 文件模块 - 与 API.md §1.2 / §8 一致
 * 基础路径：/v1/file
 *
 * 直传流程：
 *   1) POST /v1/file/sign 拿到 uploadUrl / objectKey / requiredHeader?
 *   2) 客户端 PUT 直传到对象存储（不经过后端）
 *   3) 调 sample.upload(meta) 落库
 */

/** POST /v1/file/sign - 获取对象存储直传签名 */
export function getUploadSign(data: FileSignDTO): Promise<FileSignVO> {
  return request.post<FileSignVO>('/file/sign', data)
}

/**
 * 通用对象存储直传（无业务元数据），返回 { url, key }
 * - 进度回调 percent（0-100）
 * - 内部串通 getUploadSign + XHR PUT
 */
export async function directUpload(
  file: File | Blob,
  onProgress?: (percent: number) => void,
  purpose: 'AVATAR' | 'SAMPLE_FILE' | 'AUDIT_FILE' = 'SAMPLE_FILE',
  bizId?: number
): Promise<{ url: string; key: string }> {
  const ext =
    file instanceof File
      ? (file.name.split('.').pop() || 'png').toLowerCase()
      : (file.type.split('/')[1] || 'png').toLowerCase()

  const sign = await getUploadSign({ purpose, ext, bizId })

  const headers: Record<string, string> = {}
  if (sign.requiredHeader) {
    const [k, v] = sign.requiredHeader.split(':')
    if (k && v) headers[k.trim()] = v.trim()
  }

  await new Promise<void>((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    xhr.open(sign.method, sign.uploadUrl, true)
    Object.entries(headers).forEach(([k, v]) => xhr.setRequestHeader(k, v))
    xhr.upload.onprogress = (e) => {
      if (onProgress && e.lengthComputable) {
        onProgress(Math.round((e.loaded * 100) / e.total))
      }
    }
    xhr.onload = () =>
      xhr.status >= 200 && xhr.status < 300 ? resolve() : reject(new Error(`HTTP ${xhr.status}`))
    xhr.onerror = () => reject(new Error('network error'))
    xhr.send(file)
  })

  return {
    url: sign.accessUrl || sign.uploadUrl.split('?')[0],
    key: sign.objectKey,
  }
}

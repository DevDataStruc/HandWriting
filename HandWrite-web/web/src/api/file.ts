import request from '@/utils/request'
import type { FileSignResponse, FileUploadResponse } from './contracts/file'

/** 获取对象存储直传签名 */
export function fetchUploadSign(filename: string, mimeType: string): Promise<FileSignResponse> {
  return request.get<FileSignResponse>('/file/sign', { filename, mimeType })
}

/** 通用文件上传（直传 OSS） */
export async function directUpload(
  file: File,
  onProgress?: (p: number) => void
): Promise<FileUploadResponse> {
  const sign = await fetchUploadSign(file.name, file.type)
  await new Promise<void>((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    xhr.open(sign.method, sign.uploadUrl, true)
    if (sign.headers) {
      Object.entries(sign.headers).forEach(([k, v]) => xhr.setRequestHeader(k, String(v)))
    }
    xhr.upload.onprogress = (e) => {
      if (onProgress && e.lengthComputable) onProgress(Math.round((e.loaded * 100) / e.total))
    }
    xhr.onload = () => (xhr.status >= 200 && xhr.status < 300 ? resolve() : reject(new Error('upload failed')))
    xhr.onerror = () => reject(new Error('network error'))
    xhr.send(file)
  })
  return {
    url: sign.accessUrl || sign.uploadUrl,
    key: sign.key,
    size: file.size,
    mimeType: file.type,
  }
}

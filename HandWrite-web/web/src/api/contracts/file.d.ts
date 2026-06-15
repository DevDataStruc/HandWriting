/**
 * 文件直传签名
 */
export interface FileSignResponse {
  uploadUrl: string
  method: 'PUT' | 'POST'
  key: string
  headers?: Record<string, string>
  expiresAt?: string
  accessUrl?: string
  bucket?: string
}

/**
 * 文件上传响应
 */
export interface FileUploadResponse {
  url: string
  key: string
  size: number
  mimeType: string
}

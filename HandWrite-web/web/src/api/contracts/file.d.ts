/**
 * 文件直传签名（POST /v1/file/sign）
 * 字段与 API.md §9 FileSignDTO/FileSignVO 对齐
 */

/** 直传签名请求体 */
export interface FileSignDTO {
  purpose: 'AVATAR' | 'SAMPLE_FILE' | 'AUDIT_FILE'
  ext: string
  bizId?: number
}

/** 直传签名响应 */
export interface FileSignVO {
  bucket: string
  objectKey: string
  uploadUrl: string
  method: 'PUT' | 'POST'
  expireSeconds: number
  /** "x-ms-blob-type:BlockBlob"（Azure 必须） */
  requiredHeader?: string
  /** 拼装好的访问 URL（可选，便于直接拼接） */
  accessUrl?: string
}

/** 文件上传响应（直传完成后，落库前可使用） */
export interface FileUploadResponse {
  url: string
  key: string
  size: number
  mimeType: string
}

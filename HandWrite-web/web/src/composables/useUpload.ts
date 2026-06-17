import { ref } from 'vue'
import * as fileApi from '@/api/file'
import { ElMessage } from 'element-plus'
import { ALLOWED_IMAGE_TYPES, UPLOAD_MAX_SIZE } from '@/utils/constants'

export interface UseUploadOptions {
  accept?: string[]
  maxSize?: number
  direct?: boolean
}

/**
 * 通用上传 composable，支持进度与重试
 * - direct = true：走 fileApi.directUpload（签名 → 直传对象存储）
 */
export function useUpload(options: UseUploadOptions = {}) {
  const accept = options.accept ?? ALLOWED_IMAGE_TYPES
  const maxSize = options.maxSize ?? UPLOAD_MAX_SIZE

  const progress = ref(0)
  const uploading = ref(false)
  const error = ref<string | null>(null)

  function validate(file: File): string | null {
    if (!accept.includes(file.type)) {
      return `不支持的文件类型: ${file.type || '未知'}`
    }
    if (file.size > maxSize) {
      return `文件大小超过限制 (${(maxSize / 1024 / 1024).toFixed(0)}MB)`
    }
    return null
  }

  async function upload(file: File, retry = 2): Promise<{ url: string; key: string }> {
    error.value = null
    const v = validate(file)
    if (v) {
      error.value = v
      ElMessage.error(v)
      throw new Error(v)
    }
    uploading.value = true
    progress.value = 0
    try {
      const res = await fileApi.directUpload(file, (p: number) => (progress.value = p))
      return { url: res.url, key: res.key }
    } catch (err) {
      if (retry > 0) {
        console.warn('[useUpload] retry', retry)
        return upload(file, retry - 1)
      }
      error.value = (err as Error).message || '上传失败'
      throw err
    } finally {
      uploading.value = false
      setTimeout(() => (progress.value = 0), 600)
    }
  }

  return {
    progress,
    uploading,
    error,
    validate,
    upload,
  }
}

import request from '@/utils/request'

/**
 * 字符字典管理（管理员）接口
 * 基础路径：/v1/admin/dict
 *
 * 与后端 CharDictAdminController + CharDictFileController 一一对应
 */

/** 单条字符条目（与后端 CharDictBatchCreateDTO.Item 对齐） */
export interface CharDictItem {
  charValue: string
  category?: string
  difficulty?: number
  description?: string
  enabled?: number
  lineNo?: number
}

/** 批量新增请求体 */
export interface CharDictBatchPayload {
  defaultCategory?: string
  defaultDifficulty?: number
  defaultDescription?: string
  defaultEnabled?: number
  items: CharDictItem[]
}

/** 导入结果 */
export interface CharDictImportResult {
  inserted: number
  skipped: number
  failed: number
  message: string
  failedSamples?: string[]
}

/** POST /v1/admin/dict/chars/batch - 批量新增字符（JSON 形式） */
export function batchCreateChars(payload: CharDictBatchPayload): Promise<CharDictImportResult> {
  return request.post<CharDictImportResult>('/admin/dict/chars/batch', payload)
}

/**
 * POST /v1/admin/dict/chars/import/json - 通过 JSON 文本导入
 * @param jsonText 完整 JSON 字符串（数组）
 */
export function importJsonChars(
  jsonText: string,
  defaults?: Omit<CharDictBatchPayload, 'items'>
): Promise<CharDictImportResult> {
  // 约定：把 JSON 文本塞进 items[0].charValue 字段
  return request.post<CharDictImportResult>('/admin/dict/chars/import/json', {
    ...(defaults || {}),
    items: [{ charValue: jsonText }],
  })
}

/**
 * POST /v1/admin/dict/chars/import/file - 文件导入（txt/json/xls/xlsx/docx）
 * @param file 浏览器 File 对象
 * @param defaults 默认分类 / 难度 / 描述 / 启用
 * @param onProgress 进度回调（0-100）
 */
export function importFileChars(
  file: File,
  defaults?: Omit<CharDictBatchPayload, 'items'>,
  onProgress?: (percent: number) => void
): Promise<CharDictImportResult> {
  const form = new FormData()
  form.append('file', file)
  if (defaults?.defaultCategory) form.append('defaultCategory', defaults.defaultCategory)
  if (defaults?.defaultDifficulty != null)
    form.append('defaultDifficulty', String(defaults.defaultDifficulty))
  if (defaults?.defaultDescription) form.append('defaultDescription', defaults.defaultDescription)
  if (defaults?.defaultEnabled != null)
    form.append('defaultEnabled', String(defaults.defaultEnabled))
  return request.upload<CharDictImportResult>('/admin/dict/chars/import/file', form, onProgress)
}

/**
 * POST /v1/admin/dict/chars/import/preview - 解析预览（不入库）
 */
export function previewImportFile(
  file: File,
  limit = 20
): Promise<{ ok: boolean; total?: number; preview?: CharDictItem[]; message?: string }> {
  const form = new FormData()
  form.append('file', file)
  form.append('limit', String(limit))
  return request.upload('/admin/dict/chars/import/preview', form)
}

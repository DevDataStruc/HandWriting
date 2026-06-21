import type { PageQuery, PageResult } from './common'

/**
 * 字符字典项（GET /v1/dict/chars、/v1/dict/chars/list）
 * 字段与 API.md §9 CharDict 对齐
 *
 * 后端核心字段为 charValue，前端为兼容既有组件（CharPrompt 等），
 * 在 store 层把 charValue 镜像为 char；其他扩展字段为可选，便于后端按需补齐。
 */
export interface CharDict {
  id: number
  charValue: string
  category: string
  difficulty: number
  description: string
  enabled: number
  createTime: string
  updateTime: string
  /** 前端别名，等价于 charValue */
  char?: string
  /** 拼音（可选，后端可补齐） */
  pinyin?: string
  /** 部首（可选） */
  radical?: string
  /** 笔画数（可选） */
  strokeCount?: number
  /** 已采集样本数（前端扩展） */
  sampleCount?: number
  /** 目标采集数（前端扩展） */
  targetCount?: number
}

/** 字典分页查询参数 */
export interface DictPageQuery extends PageQuery {
  category?: string
  difficulty?: number
  enabled?: number
}

/** 字典按分类查询参数 */
export interface DictListQuery {
  category?: string
}

/**
 * 样本状态（数字 0/1/2，与 API.md §9 SampleVO.status 对齐）
 *  - 0 待审核
 *  - 1 通过
 *  - 2 驳回
 *
 * 注意：常量值（SampleStatusCode）已迁移到 ./sample.ts，
 * 避免 .d.ts 文件被 Vite/Rollup 错误地作为运行时模块加载
 */

/** 样本状态（字符串别名，便于前端使用） */
export type SampleStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

/**
 * 样本实体（GET /v1/sample/{id}、GET /v1/sample/page、GET /v1/audit/pending、GET /v1/audit/history）
 * 字段与 API.md §9 SampleVO 对齐
 */
export interface SampleVO {
  id: number | string
  userId: number
  charId: number
  char?: string
  category?: string
  pinyin?: string
  fileKey: string
  fileUrl: string
  thumbUrl?: string
  /** 兼容旧字段：本地草稿 dataURL */
  imageUrl?: string
  fileSize: number
  device: string
  /** 0 待审核 1 通过 2 驳回 */
  status: number
  rejectReason?: string
  auditedBy?: number
  auditedTime?: string
  remark?: string
  /** 写入耗时（毫秒，前端扩展） */
  duration?: number
  /** 笔画数（前端扩展，采集阶段记录） */
  strokeCount?: number
  createTime: string
  /** 前端扩展：本地草稿标记 */
  local?: boolean
}

/**
 * 本地草稿（离线/上传失败兜底）
 * - id 以 "local-" 开头，便于 MySamples 视图识别
 */
export interface LocalDraft extends Omit<SampleVO, 'id'> {
  id: string
  local: true
  imageUrl: string
  status: number
  duration?: number
  strokeCount?: number
}

/** 样本上传元数据（POST /v1/sample/upload） */
export interface SampleUploadDTO {
  charId: number
  fileKey: string
  fileUrl?: string
  fileSize: number
  sha256?: string
  device?: string
}

/** 样本更新元数据（PUT /v1/sample/{id}） */
export interface SampleUpdateDTO {
  charId?: number
  fileKey: string
  fileUrl?: string
  fileSize: number
  sha256?: string
  device?: string
}

/** 样本上传响应 */
export interface SampleUploadVO {
  id: number
  fileUrl: string
  charId: number
  status: number
  createTime: string
}

/** 样本分页查询参数 */
export interface SamplePageQuery extends PageQuery {
  status?: number
  charId?: number
  userId?: number
  startDate?: string
  endDate?: string
}

/** 样本分页结果 */
export type SamplePageResult = PageResult<SampleVO>

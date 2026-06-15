import type { PageQuery, PageResult } from './common'

/**
 * 字符字典项
 */
export interface DictChar {
  id: number | string
  char: string
  pinyin?: string
  radical?: string
  strokeCount?: number
  category?: string
  difficulty?: 1 | 2 | 3 | 4 | 5
  imageUrl?: string
  description?: string
  sampleCount?: number
  targetCount?: number
  isCollected?: boolean
}

/**
 * 字典查询参数
 */
export interface DictQuery extends PageQuery {
  category?: string
  difficulty?: number
  isCollected?: boolean
}

/**
 * 上传样本元数据
 */
export interface SampleUploadMeta {
  charId: number | string
  deviceInfo?: string
  duration?: number
  strokeCount?: number
  remark?: string
}

/**
 * 上传样本响应
 */
export interface SampleUploadResponse {
  id: number | string
  url: string
  charId: number | string
  createdAt: string
}

/**
 * 样本状态
 */
export type SampleStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

/**
 * 样本实体
 */
export interface Sample {
  id: number | string
  charId: number | string
  char?: string
  imageUrl: string
  thumbUrl?: string
  userId: number | string
  username?: string
  status: SampleStatus
  rejectReason?: string
  deviceInfo?: string
  duration?: number
  strokeCount?: number
  remark?: string
  createdAt: string
  reviewedAt?: string
  reviewerId?: number | string
  reviewerName?: string
}

/**
 * 样本查询参数
 */
export interface SampleQuery extends PageQuery {
  status?: SampleStatus
  charId?: number | string
  userId?: number | string
  startDate?: string
  endDate?: string
}

/**
 * 样本分页结果
 */
export type SamplePageResult = PageResult<Sample>

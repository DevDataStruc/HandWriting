import type { PageResult } from './common'

/**
 * 统计总览（GET /v1/stats/overview）
 * 字段与 API.md §1.2 stats 模块对应
 */
export interface StatsOverviewVO {
  totalSamples: number
  totalUsers: number
  totalChars: number
  todaySamples: number
  todayUsers: number
  pendingAudits: number
  approvedSamples: number
  rejectedSamples: number
  growthRate?: number
}

/**
 * 样本趋势（GET /v1/stats/trend）
 * 后端可返回任意结构，前端按需读取
 */
export interface SampleTrendVO {
  dates: string[]
  samples: number[]
  users: number[]
}

/** 时间序列数据点 */
export interface TrendPoint {
  date: string
  value: number
}

/** 状态分布 */
export interface StatusDistribution {
  status: string
  count: number
}

/** 贡献者排行项 */
export interface TopContributor {
  userId: number
  username: string
  nickname?: string
  avatar?: string
  sampleCount: number
  approvedCount: number
}

/** 字典采集进度 */
export interface DictProgress {
  charId: number
  char: string
  collected: number
  target: number
  progress: number
}

/**
 * 首页公开展示统计（GET /v1/public/stats/overview）
 * 面向未登录用户，仅返回三个脱敏数字
 */
export interface PublicStatsOverviewVO {
  totalSamples: number
  totalUsers: number
  totalChars: number
}

/** 审核历史分页 */
export type AuditHistoryPageResult = PageResult<unknown>

/**
 * 统计概览
 */
export interface StatsOverview {
  totalSamples: number
  totalUsers: number
  todaySamples: number
  todayUsers: number
  pendingAudits: number
  approvedSamples: number
  rejectedSamples: number
  activeChars: number
  growthRate?: number
}

/**
 * 时间序列数据点
 */
export interface TrendPoint {
  date: string
  value: number
}

/**
 * 样本增长趋势
 */
export interface SampleTrend {
  dates: string[]
  samples: number[]
  users: number[]
}

/**
 * 状态分布
 */
export interface StatusDistribution {
  status: string
  count: number
}

/**
 * 贡献者排行项
 */
export interface TopContributor {
  userId: number | string
  username: string
  nickname?: string
  avatar?: string
  sampleCount: number
  approvedCount: number
}

/**
 * 字典采集进度
 */
export interface DictProgress {
  charId: number | string
  char: string
  collected: number
  target: number
  progress: number
}

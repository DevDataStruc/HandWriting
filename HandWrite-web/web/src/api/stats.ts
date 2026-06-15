import request from '@/utils/request'
import type {
  DictProgress,
  SampleTrend,
  StatsOverview,
  StatusDistribution,
  TopContributor,
} from './contracts/stats'

/** 总览数据 */
export function fetchOverview(): Promise<StatsOverview> {
  return request.get<StatsOverview>('/stats/overview')
}

/** 样本增长趋势 */
export function fetchSampleTrend(days = 30): Promise<SampleTrend> {
  return request.get<SampleTrend>('/stats/trend', { days })
}

/** 状态分布 */
export function fetchStatusDistribution(): Promise<StatusDistribution[]> {
  return request.get<StatusDistribution[]>('/stats/status-distribution')
}

/** 贡献者排行 */
export function fetchTopContributors(limit = 10): Promise<TopContributor[]> {
  return request.get<TopContributor[]>('/stats/top-contributors', { limit })
}

/** 字典采集进度 */
export function fetchDictProgress(): Promise<DictProgress[]> {
  return request.get<DictProgress[]>('/stats/dict-progress')
}

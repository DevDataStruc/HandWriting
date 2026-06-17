import request from '@/utils/request'
import type {
  DictProgress,
  SampleTrendVO,
  StatsOverviewVO,
  StatusDistribution,
  TopContributor,
} from './contracts/stats'

/**
 * 统计模块 - 与 API.md §1.2 / §7.5 一致
 * 基础路径：/v1/stats
 *
 * 注：API.md §1.2 只定义了 overview 和 trend 两个接口，
 * statusDistribution / topContributors / dictProgress 属于前端扩展，
 * 后端可按需提供。若未提供，调用方需做兜底（空数组 / 默认值）。
 */

/** GET /v1/stats/overview - 总览数据 */
export function overview(): Promise<StatsOverviewVO> {
  return request.get<StatsOverviewVO>('/stats/overview')
}

/** 别名：与 DashboardView 中的 fetchOverview 对齐 */
export const fetchOverview = overview

/** GET /v1/stats/trend - 样本趋势（默认近 30 天） */
export function trend(days = 30): Promise<SampleTrendVO> {
  return request.get<SampleTrendVO>('/stats/trend', { days })
}

/** 别名：与 DashboardView / StatsView 中的 fetchSampleTrend 对齐 */
export const fetchSampleTrend = trend

/**
 * 样本状态分布（前端扩展：后端可提供 /v1/stats/status-distribution）
 * 不存在时返回空数组
 */
export function fetchStatusDistribution(): Promise<StatusDistribution[]> {
  return request
    .get<StatusDistribution[]>('/stats/status-distribution')
    .catch(() => [] as StatusDistribution[])
}

/**
 * 贡献者排行（前端扩展：后端可提供 /v1/stats/top-contributors）
 * 不存在时返回空数组
 */
export function fetchTopContributors(limit = 10): Promise<TopContributor[]> {
  return request
    .get<TopContributor[]>('/stats/top-contributors', { limit })
    .catch(() => [] as TopContributor[])
}

/**
 * 字符采集进度（前端扩展：后端可提供 /v1/stats/dict-progress）
 * 不存在时返回空数组
 */
export function fetchDictProgress(): Promise<DictProgress[]> {
  return request.get<DictProgress[]>('/stats/dict-progress').catch(() => [] as DictProgress[])
}

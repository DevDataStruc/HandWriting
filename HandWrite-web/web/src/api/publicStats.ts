import request from '@/utils/request'
import type { PublicStatsOverviewVO } from './contracts/stats'

/**
 * 公共统计接口 - 无需登录
 * 基础路径：/v1/public/stats
 * 面向门户首页等公开页面，仅暴露脱敏后的累计数字
 */

/** GET /v1/public/stats/overview - 首页累计样本/参与用户/覆盖字符 */
export function fetchPublicOverview(): Promise<PublicStatsOverviewVO> {
  return request.get<PublicStatsOverviewVO>('/public/stats/overview')
}

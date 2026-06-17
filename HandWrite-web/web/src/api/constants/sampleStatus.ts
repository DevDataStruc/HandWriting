/**
 * 样本状态码常量（与 API.md §9 SampleVO.status 对齐）
 *  - 0 待审核
 *  - 1 通过
 *  - 2 驳回
 *
 * 该文件作为运行时模块存在，避免 .d.ts 文件被 Vite/Rollup 错误加载
 */
export const SampleStatusCode = {
  PENDING: 0,
  APPROVED: 1,
  REJECTED: 2,
} as const

export type SampleStatusCodeValue = (typeof SampleStatusCode)[keyof typeof SampleStatusCode]

/** 状态码 → 字符串映射 */
export const SampleStatusMap: Record<SampleStatusCodeValue, 'PENDING' | 'APPROVED' | 'REJECTED'> = {
  0: 'PENDING',
  1: 'APPROVED',
  2: 'REJECTED',
}

/** 字符串 → 状态码映射 */
export const SampleStatusReverse: Record<
  'PENDING' | 'APPROVED' | 'REJECTED',
  SampleStatusCodeValue
> = {
  PENDING: 0,
  APPROVED: 1,
  REJECTED: 2,
}

/** 把后端数字状态码转字符串 */
export function parseSampleStatus(code: number | undefined | null): 'PENDING' | 'APPROVED' | 'REJECTED' {
  if (code === 1) return 'APPROVED'
  if (code === 2) return 'REJECTED'
  return 'PENDING'
}

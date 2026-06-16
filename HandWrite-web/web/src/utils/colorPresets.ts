/**
 * 手写板颜色系统
 *  - 24 色调色板（基础/炫彩）
 *  - 动态颜色：渐变（沿笔画方向取两端色）与 呼吸（按时间在两端色间循环）
 *  - 自定义：RGB / HSB 精确选择（通过 el-color-picker）
 */

/** 24 色调色板（基础 8 + 中间色 8 + 强调 8） */
export const COLOR_PALETTE: string[] = [
  // —— 基础中性 ——
  '#0D9488',
  '#134E4A',
  '#1F2937',
  '#475569',
  // —— 暖色 ——
  '#F97316',
  '#FB923C',
  '#F59E0B',
  '#EF4444',
  // —— 冷色 ——
  '#3B82F6',
  '#0EA5E9',
  '#6366F1',
  '#8B5CF6',
  // —— 绿/植物 ——
  '#10B981',
  '#22C55E',
  '#84CC16',
  '#65A30D',
  // —— 粉/品红 ——
  '#EC4899',
  '#F472B6',
  '#D946EF',
  '#A855F7',
  // —— 高对比 ——
  '#FFFFFF',
  '#FACC15',
  '#06B6D4',
  '#0F172A',
]

/** 动态颜色模式 */
export type DynamicColorMode = 'solid' | 'gradient' | 'breathing'

export interface DynamicColor {
  mode: DynamicColorMode
  /** 主色（必填） */
  primary: string
  /** 副色（用于 gradient / breathing 的两端） */
  secondary?: string
  /** 呼吸节奏（毫秒/周期） */
  durationMs?: number
}

/** 解析"动态颜色"在某个时间点（ms）上的最终色值 */
export function resolveDynamicColor(color: DynamicColor, t: number): string {
  if (color.mode === 'solid' || !color.secondary) return color.primary
  if (color.mode === 'gradient') {
    // 静态渐变：取主色（在画笔端点之间已通过 primary 控制）
    return color.primary
  }
  // breathing: 在 primary ↔ secondary 之间按 sin 曲线插值
  const dur = Math.max(200, color.durationMs ?? 1500)
  const phase = (t % dur) / dur // 0..1
  const k = 0.5 - 0.5 * Math.cos(phase * Math.PI * 2) // 0..1..0 平滑
  return mixHex(color.primary, color.secondary, k)
}

/** 解析用户传入的"动态色"配置（向后兼容：传字符串当 solid） */
export function toDynamicColor(input: string | DynamicColor): DynamicColor {
  if (typeof input === 'string') return { mode: 'solid', primary: input }
  return input
}

/** 简单 hex → rgb 解析 */
function hexToRgb(hex: string): [number, number, number] {
  const m = /^#?([0-9a-f]{3}|[0-9a-f]{6})$/i.exec(hex.trim())
  if (!m) return [0, 0, 0]
  let h = m[1]
  if (h.length === 3)
    h = h
      .split('')
      .map((c) => c + c)
      .join('')
  const n = parseInt(h, 16)
  return [(n >> 16) & 0xff, (n >> 8) & 0xff, n & 0xff]
}

/** rgb → hex */
function rgbToHex(r: number, g: number, b: number): string {
  const c = (v: number) =>
    Math.round(clamp(v, 0, 255))
      .toString(16)
      .padStart(2, '0')
  return `#${c(r)}${c(g)}${c(b)}`
}

/** 颜色插值（t=0 → a，t=1 → b） */
export function mixHex(a: string, b: string, t: number): string {
  const k = clamp(t, 0, 1)
  const [r1, g1, b1] = hexToRgb(a)
  const [r2, g2, b2] = hexToRgb(b)
  return rgbToHex(r1 + (r2 - r1) * k, g1 + (g2 - g1) * k, b1 + (b2 - b1) * k)
}

function clamp(v: number, lo: number, hi: number): number {
  return Math.max(lo, Math.min(hi, v))
}

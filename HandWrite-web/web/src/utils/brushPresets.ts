/**
 * 笔刷配置 — 柳叶笔（willow leaf）核心预设
 *
 * 基于 perfect-freehand 的笔锋还原能力，通过 size / thinning / smoothing /
 * streamline / taper 组合实现：
 *  - 钢笔类：低 thinning，均匀线宽
 *  - 毛笔类：中等 thinning，尾端渐变
 *  - 柳叶笔类：高 thinning + 双向 taper，模拟柳叶尖头-中宽-收尾的笔锋
 */

/** 数值限位 */
export function clamp(v: number, min: number, max: number): number {
  return Math.max(min, Math.min(max, v))
}

export type BrushCategory = 'pen' | 'brush' | 'willow'

export interface BrushPreset {
  /** 唯一 id */
  id: string
  /** 展示名称（zh-CN） */
  name: string
  /** 英文展示名称 */
  nameEn: string
  /** 分类 */
  category: BrushCategory
  /** 基础笔尖宽度（px） */
  size: number
  /** 0~1，越大越随速度变细 */
  thinning: number
  /** 0~1，越大越平滑 */
  smoothing: number
  /** 0~1，越大越简化采样点 */
  streamline: number
  /** 起笔 taper 长度（0~1） */
  taperStart: number
  /** 收笔 taper 长度（0~1） */
  taperEnd: number
  /** Element Plus 图标组件名 */
  icon: string
}

export const BRUSH_PRESETS: BrushPreset[] = [
  // ----- 钢笔（pen）-----
  {
    id: 'pen-fine',
    name: '钢笔(细)',
    nameEn: 'Pen (Fine)',
    category: 'pen',
    size: 2,
    thinning: 0.1,
    smoothing: 0.5,
    streamline: 0.5,
    taperStart: 0,
    taperEnd: 0,
    icon: 'EditPen',
  },
  {
    id: 'pen-medium',
    name: '钢笔(中)',
    nameEn: 'Pen (Medium)',
    category: 'pen',
    size: 4,
    thinning: 0.15,
    smoothing: 0.5,
    streamline: 0.5,
    taperStart: 0,
    taperEnd: 0,
    icon: 'EditPen',
  },
  // ----- 毛笔（brush）-----
  {
    id: 'brush-soft',
    name: '毛笔(软)',
    nameEn: 'Brush (Soft)',
    category: 'brush',
    size: 18,
    thinning: 0.5,
    smoothing: 0.55,
    streamline: 0.5,
    taperStart: 0.4,
    taperEnd: 0.4,
    icon: 'Brush',
  },
  {
    id: 'brush-hard',
    name: '毛笔(硬)',
    nameEn: 'Brush (Hard)',
    category: 'brush',
    size: 14,
    thinning: 0.35,
    smoothing: 0.6,
    streamline: 0.5,
    taperStart: 0.2,
    taperEnd: 0.2,
    icon: 'Brush',
  },
  // ----- 柳叶笔（willow leaf）四种变体 -----
  {
    id: 'willow-thin',
    name: '细柳',
    nameEn: 'Willow (Thin)',
    category: 'willow',
    size: 8,
    thinning: 0.6,
    smoothing: 0.6,
    streamline: 0.5,
    taperStart: 0.5,
    taperEnd: 0.5,
    icon: 'MagicStick',
  },
  {
    id: 'willow-thick',
    name: '粗柳',
    nameEn: 'Willow (Thick)',
    category: 'willow',
    size: 20,
    thinning: 0.55,
    smoothing: 0.5,
    streamline: 0.5,
    taperStart: 0.4,
    taperEnd: 0.4,
    icon: 'MagicStick',
  },
  {
    id: 'willow-sharp',
    name: '锐利柳',
    nameEn: 'Willow (Sharp)',
    category: 'willow',
    size: 12,
    thinning: 0.75,
    smoothing: 0.7,
    streamline: 0.5,
    taperStart: 0.8,
    taperEnd: 0.8,
    icon: 'MagicStick',
  },
  {
    id: 'willow-flowing',
    name: '飘途柳',
    nameEn: 'Willow (Flowing)',
    category: 'willow',
    size: 16,
    thinning: 0.5,
    smoothing: 0.85,
    streamline: 0.7,
    taperStart: 0.6,
    taperEnd: 0.7,
    icon: 'MagicStick',
  },
]

export const BRUSH_PRESETS_BY_ID: Record<string, BrushPreset> = BRUSH_PRESETS.reduce(
  (acc, b) => {
    acc[b.id] = b
    return acc
  },
  {} as Record<string, BrushPreset>
)

/** 默认笔刷：细柳 */
export const DEFAULT_BRUSH_ID = 'willow-thin'

/** 角度限位（0-180 度，超出对称翻转） */
export const ANGLE_MIN = 0
export const ANGLE_MAX = 180

/** 大小限位（1-100px，覆盖绝大多数书写场景） */
export const SIZE_MIN = 1
export const SIZE_MAX = 100
export const DEFAULT_SIZE = 8

/** 压感灵敏度：50=标准，<50=更迟钝，>50=更灵敏 */
export const PRESSURE_SENSITIVITY_MIN = 0
export const PRESSURE_SENSITIVITY_MAX = 200
export const DEFAULT_PRESSURE_SENSITIVITY = 100

/** 压感曲线：把原始 0-1 压力重映射成最终用于渲染的 0-1 值 */
export type PressureCurve = 'linear' | 'soft' | 'hard'

/** 压感曲线函数（曲线越陡，轻压力也能产生明显粗细） */
export const PRESSURE_CURVES: Record<PressureCurve, (p: number) => number> = {
  // 线性：1:1
  linear: (p) => p,
  // 柔和：低压力变化更平滑（立方曲线，让轻微压力即可展现笔锋）
  soft: (p) => Math.pow(p, 0.5),
  // 刚烈：低压力时几乎不显，高压力时爆发（更接近真实毛笔）
  hard: (p) => Math.pow(p, 2),
}

/** 压感曲线对输入压力应用灵敏度（0~2 倍乘） */
export function applyPressure(
  pressure: number,
  sensitivityPct: number,
  curve: PressureCurve
): number {
  const sens = clamp(sensitivityPct / 100, 0, 2)
  const remapped = PRESSURE_CURVES[curve](pressure) * sens
  return clamp(remapped, 0, 1)
}

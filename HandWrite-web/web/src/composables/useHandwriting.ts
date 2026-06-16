import { computed, ref } from 'vue'
import { getStroke } from 'perfect-freehand'
import {
  applyPressure,
  BRUSH_PRESETS_BY_ID,
  clamp,
  DEFAULT_BRUSH_ID,
  SIZE_MAX,
  SIZE_MIN,
  type BrushPreset,
  type PressureCurve,
} from '@/utils/brushPresets'
import { STROKE_DEFAULT_SIZE } from '@/utils/constants'

export interface StrokePoint {
  x: number
  y: number
  pressure?: number
  time?: number
}

export interface UseHandwritingOptions {
  /** 基础笔尖宽度 */
  size?: number
  /** 0~1 速度变细系数 */
  thinning?: number
  /** 0~1 平滑系数 */
  smoothing?: number
  /** 0~1 简化系数 */
  streamline?: number
  /** 颜色 */
  color?: string
  /** 笔刷预设 id（不传则使用细柳） */
  brush?: string
  /** 笔尖旋转角度（度） */
  angle?: number
  /** 是否响应压感 */
  pressure?: boolean
  /** 压感灵敏度（0~200，100=标准） */
  pressureSensitivity?: number
  /** 压感曲线：linear / soft / hard */
  pressureCurve?: PressureCurve
  minSize?: number
  maxSize?: number
}

/** 单条已渲染笔画的快照（d / color），保证撤销/重做不丢失设置 */
export interface RenderedStroke {
  id: string
  d: string
  color: string
}

function outlineToSvgPath(outline: number[][]): string {
  if (outline.length < 2) return ''
  const parts: string[] = []
  let prevX = NaN
  let prevY = NaN
  for (let i = 0; i < outline.length; i++) {
    const [x, y] = outline[i]
    const fx = x.toFixed(2)
    const fy = y.toFixed(2)
    if (fx === prevX.toFixed(2) && fy === prevY.toFixed(2)) continue
    parts.push(`${i === 0 ? 'M' : 'L'}${fx},${fy}`)
    prevX = x
    prevY = y
  }
  if (parts.length === 0) return ''
  return parts.join(' ') + ' Z'
}

function rotateAroundCentroid(outline: number[][], angleDeg: number): number[][] {
  if (!outline.length || Math.abs(angleDeg) < 0.01) return outline
  let cx = 0
  let cy = 0
  for (const [x, y] of outline) {
    cx += x
    cy += y
  }
  cx /= outline.length
  cy /= outline.length
  const rad = (angleDeg * Math.PI) / 180
  const cos = Math.cos(rad)
  const sin = Math.sin(rad)
  return outline.map(([x, y]) => {
    const dx = x - cx
    const dy = y - cy
    return [cx + dx * cos - dy * sin, cy + dx * sin + dy * cos]
  })
}

/**
 * 手写板 composable，基于 perfect-freehand 实现笔锋还原。
 *
 * 压感管线：原始 pressure → applyPressure(sens, curve) → 渲染输入。
 * 这样不同灵敏度/曲线能在同一组采样点上产生不同视觉效果。
 */
export function useHandwriting(options: UseHandwritingOptions = {}) {
  const strokes = ref<RenderedStroke[]>([])
  const redoStack = ref<RenderedStroke[]>([])
  const currentStroke = ref<StrokePoint[]>([])

  const size = ref<number>(options.size ?? STROKE_DEFAULT_SIZE)
  const color = ref<string>(options.color ?? '#0D9488')
  const brushId = ref<string>(options.brush ?? DEFAULT_BRUSH_ID)
  const angle = ref<number>(options.angle ?? 0)
  const pressureEnabled = ref<boolean>(options.pressure ?? true)
  const pressureSensitivity = ref<number>(options.pressureSensitivity ?? 100)
  const pressureCurve = ref<PressureCurve>(options.pressureCurve ?? 'linear')
  const minSize = options.minSize ?? SIZE_MIN
  const maxSize = options.maxSize ?? SIZE_MAX

  const currentBrush = computed<BrushPreset>(
    () => BRUSH_PRESETS_BY_ID[brushId.value] ?? BRUSH_PRESETS_BY_ID[DEFAULT_BRUSH_ID]
  )

  function setBrush(id: string): void {
    if (BRUSH_PRESETS_BY_ID[id]) brushId.value = id
  }

  function setSize(v: number): void {
    size.value = clamp(v, minSize, maxSize)
  }

  function setColor(c: string): void {
    color.value = c
  }

  function setAngle(a: number): void {
    angle.value = clamp(a, 0, 180)
  }

  function setPressure(p: boolean): void {
    pressureEnabled.value = !!p
  }

  function setPressureSensitivity(p: number): void {
    pressureSensitivity.value = clamp(p, 0, 200)
  }

  function setPressureCurve(c: PressureCurve): void {
    pressureCurve.value = c
  }

  /** 把原始点转成 [x, y, pressure]（pressure 走压感管线） */
  function toFreehandInput(points: StrokePoint[]): number[][] {
    return points.map((p) => {
      const raw = p.pressure ?? 0.5
      const out = pressureEnabled.value
        ? applyPressure(raw, pressureSensitivity.value, pressureCurve.value)
        : 0.5
      return [p.x, p.y, out]
    })
  }

  function buildStrokePath(points: StrokePoint[]): string {
    if (points.length < 2) return ''
    const input = toFreehandInput(points)
    const outline = getStroke(input as unknown as number[][], {
      size: size.value,
      thinning: currentBrush.value.thinning,
      smoothing: currentBrush.value.smoothing,
      streamline: currentBrush.value.streamline,
      easing: (t: number) => t,
      last: true,
      start: { taper: currentBrush.value.taperStart, easing: (t: number) => t },
      end: { taper: currentBrush.value.taperEnd, easing: (t: number) => t },
    }) as unknown as number[][]
    return outlineToSvgPath(rotateAroundCentroid(outline, angle.value))
  }

  function startStroke(point: StrokePoint): void {
    currentStroke.value = [point]
    redoStack.value = []
  }

  function extendStroke(point: StrokePoint): void {
    currentStroke.value.push(point)
  }

  function endStroke(): RenderedStroke | null {
    const d = buildStrokePath(currentStroke.value)
    currentStroke.value = []
    if (!d) return null
    const snap: RenderedStroke = {
      id: `s-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
      d,
      color: color.value,
    }
    strokes.value.push(snap)
    return snap
  }

  function previewPath(points?: StrokePoint[]): string {
    const pts = points ?? currentStroke.value
    return buildStrokePath(pts)
  }

  function clear(): void {
    strokes.value = []
    redoStack.value = []
    currentStroke.value = []
  }

  function undo(): void {
    if (strokes.value.length === 0) return
    redoStack.value.push(strokes.value.pop() as RenderedStroke)
  }

  function redo(): void {
    if (redoStack.value.length === 0) return
    strokes.value.push(redoStack.value.pop() as RenderedStroke)
  }

  function canUndo(): boolean {
    return strokes.value.length > 0
  }

  function canRedo(): boolean {
    return redoStack.value.length > 0
  }

  function toSVG(width: number, height: number): string {
    const paths = strokes.value
      .map((s) => `<path d="${s.d}" fill="${s.color}" fill-rule="nonzero" />`)
      .join('')
    return `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}">${paths}</svg>`
  }

  return {
    // 状态
    strokes,
    redoStack,
    currentStroke,
    size,
    color,
    brushId,
    angle,
    pressureEnabled,
    pressureSensitivity,
    pressureCurve,
    currentBrush,
    // 操作
    startStroke,
    extendStroke,
    endStroke,
    previewPath,
    clear,
    undo,
    redo,
    canUndo,
    canRedo,
    setBrush,
    setSize,
    setColor,
    setAngle,
    setPressure,
    setPressureSensitivity,
    setPressureCurve,
    // 导出
    toSVG,
  }
}

export type HandwritingController = ReturnType<typeof useHandwriting>

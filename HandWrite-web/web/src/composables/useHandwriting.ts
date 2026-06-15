import { ref } from 'vue'
import { getStroke } from 'perfect-freehand'
import { STROKE_DEFAULT_SIZE, STROKE_MAX_SIZE, STROKE_MIN_SIZE } from '@/utils/constants'

export interface StrokePoint {
  x: number
  y: number
  pressure?: number
  time?: number
}

export interface UseHandwritingOptions {
  size?: number
  thinning?: number
  smoothing?: number
  streamline?: number
  color?: string
  minSize?: number
  maxSize?: number
}

/**
 * 手写板 composable，基于 perfect-freehand 实现笔锋还原
 */
export function useHandwriting(options: UseHandwritingOptions = {}) {
  const strokes = ref<StrokePoint[][]>([])
  const currentStroke = ref<StrokePoint[]>([])
  const redoStack = ref<StrokePoint[][]>([])

  const strokeSize = ref<number>(options.size ?? STROKE_DEFAULT_SIZE)
  const strokeColor = ref<string>(options.color ?? '#0D9488')
  const thinning = ref<number>(options.thinning ?? 0.5)
  const smoothing = ref<number>(options.smoothing ?? 0.5)
  const streamline = ref<number>(options.streamline ?? 0.5)

  function startStroke(point: StrokePoint): void {
    currentStroke.value = [point]
    redoStack.value = []
  }

  function extendStroke(point: StrokePoint): void {
    currentStroke.value.push(point)
  }

  function endStroke(): void {
    if (currentStroke.value.length > 0) {
      strokes.value.push(currentStroke.value)
      currentStroke.value = []
    }
  }

  function clear(): void {
    strokes.value = []
    currentStroke.value = []
    redoStack.value = []
  }

  function undo(): void {
    if (strokes.value.length === 0) return
    redoStack.value.push(strokes.value.pop() as StrokePoint[][])
  }

  function redo(): void {
    if (redoStack.value.length === 0) return
    strokes.value.push(redoStack.value.pop() as StrokePoint[][])
  }

  function setSize(size: number): void {
    strokeSize.value = Math.max(STROKE_MIN_SIZE, Math.min(STROKE_MAX_SIZE, size))
  }

  function setColor(color: string): void {
    strokeColor.value = color
  }

  function canUndo(): boolean {
    return strokes.value.length > 0
  }

  function canRedo(): boolean {
    return redoStack.value.length > 0
  }

  /** 获取 perfect-freehand 笔锋点 */
  function getFreehandPoints(points: StrokePoint[]): number[][] {
    return getStroke(points as unknown as number[][], {
      size: strokeSize.value,
      thinning: thinning.value,
      smoothing: smoothing.value,
      streamline: streamline.value,
      easing: (t: number) => t,
      last: true,
    }) as unknown as number[][]
  }

  /** 获取单条笔画的 SVG path d */
  function strokeToSvgPath(points: StrokePoint[]): string {
    const freehand = getFreehandPoints(points)
    if (!freehand.length) return ''
    const d = freehand
      .map((point, i) => {
        const [x, y] = point
        return `${i === 0 ? 'M' : 'L'}${x.toFixed(2)},${y.toFixed(2)}`
      })
      .join(' ')
    return d
  }

  /** 获取当前所有笔画的 SVG 字符串 */
  function toSVG(width: number, height: number): string {
    const paths = strokes.value
      .map((s) => `<path d="${strokeToSvgPath(s)}" fill="${strokeColor.value}" />`)
      .join('')
    return `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}">${paths}</svg>`
  }

  return {
    strokes,
    currentStroke,
    redoStack,
    strokeSize,
    strokeColor,
    thinning,
    smoothing,
    streamline,
    startStroke,
    extendStroke,
    endStroke,
    clear,
    undo,
    redo,
    setSize,
    setColor,
    canUndo,
    canRedo,
    getFreehandPoints,
    strokeToSvgPath,
    toSVG,
  }
}

export type HandwritingController = ReturnType<typeof useHandwriting>

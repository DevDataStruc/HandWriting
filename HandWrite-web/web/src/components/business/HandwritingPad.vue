<template>
  <div class="handwriting-pad" :class="{ 'is-drawing': isDrawing, 'is-readonly': readonly }">
    <div ref="containerRef" class="handwriting-pad__canvas-wrap" :style="wrapStyle">
      <svg
        ref="svgRef"
        class="handwriting-pad__svg"
        :viewBox="`0 0 ${width} ${height}`"
        :width="width"
        :height="height"
        xmlns="http://www.w3.org/2000/svg"
        @pointerdown="onPointerDown"
        @pointermove="onPointerMove"
        @pointerup="onPointerUp"
        @pointercancel="onPointerUp"
        @pointerleave="onPointerUp"
      >
        <!-- 田字格背景 -->
        <g class="grid" aria-hidden="true">
          <rect :x="0" :y="0" :width="width" :height="height" fill="#FFFFFF" />
          <line
            v-for="i in 3"
            :key="`h-${i}`"
            :x1="0"
            :x2="width"
            :y1="(height / 4) * i"
            :y2="(height / 4) * i"
            stroke="#E0F2F1"
            stroke-width="1"
            stroke-dasharray="2 4"
          />
          <line
            v-for="i in 3"
            :key="`v-${i}`"
            :y1="0"
            :y2="height"
            :x1="(width / 4) * i"
            :x2="(width / 4) * i"
            stroke="#E0F2F1"
            stroke-width="1"
            stroke-dasharray="2 4"
          />
          <rect
            :x="width / 4"
            :y="height / 4"
            :width="width / 2"
            :height="height / 2"
            fill="none"
            stroke="#5EEAD4"
            stroke-width="1.5"
          />
        </g>

        <!-- 已保存的笔画：fill 闭合路径，per-stroke 设置保留 -->
        <path
          v-for="stroke in committedStrokes"
          :key="stroke.id"
          :d="stroke.d"
          :fill="stroke.color"
          fill-rule="nonzero"
        />

        <!-- 当前进行中的笔画 -->
        <path v-if="currentPath" :d="currentPath" :fill="currentColor" fill-rule="nonzero" />
      </svg>

      <div v-if="!readonly" class="handwriting-pad__hint" aria-hidden="true">
        {{ hintText }}
      </div>
    </div>

    <!-- 工具栏 -->
    <div v-if="!readonly" class="handwriting-pad__toolbar">
      <!-- 历史操作 -->
      <div class="toolbar-group">
        <el-tooltip :content="t('collect.undo') + ' (Ctrl+Z)'">
          <el-button :disabled="!canUndo" link @click="undo">
            <el-icon><Back /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip :content="t('collect.redo') + ' (Ctrl+Y)'">
          <el-button :disabled="!canRedo" link @click="redo">
            <el-icon><RefreshRight /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip :content="t('collect.clear')">
          <el-button :disabled="strokesCount === 0" link @click="clear">
            <el-icon><Delete /></el-icon>
          </el-button>
        </el-tooltip>
      </div>

      <!-- 笔刷选择 -->
      <div class="toolbar-group toolbar-group--brush">
        <span class="label">{{ t('collect.brush') }}</span>
        <div class="brush-list">
          <button
            v-for="preset in brushPresets"
            :key="preset.id"
            type="button"
            class="brush-item"
            :class="{
              'is-active': preset.id === activeBrushId,
              [`brush-item--${preset.category}`]: true,
            }"
            :title="preset.name"
            @click="setBrush(preset.id)"
          >
            <span class="brush-item__icon">
              <el-icon><component :is="preset.icon" /></el-icon>
            </span>
            <span class="brush-item__name">{{ preset.name }}</span>
          </button>
        </div>
      </div>

      <!-- 笔尖尺寸 -->
      <div class="toolbar-group toolbar-group--slider">
        <span class="label">{{ t('collect.size') }}</span>
        <el-slider
          v-model="strokeSize"
          :min="sizeMin"
          :max="sizeMax"
          :step="1"
          style="width: 120px"
          aria-label="brush-size"
        />
        <span class="value">{{ strokeSize }}px</span>
      </div>

      <!-- 笔尖角度 -->
      <div class="toolbar-group toolbar-group--slider">
        <span class="label">{{ t('collect.angle') }}</span>
        <el-slider
          v-model="brushAngle"
          :min="angleMin"
          :max="angleMax"
          :step="1"
          style="width: 100px"
          aria-label="brush-angle"
        />
        <span class="value">{{ brushAngle }}°</span>
      </div>

      <!-- 压感开关 -->
      <div class="toolbar-group">
        <el-tooltip :content="t('collect.pressureTip')">
          <el-switch
            v-model="pressureEnabled"
            :active-text="t('collect.pressure')"
            inline-prompt
            size="small"
          />
        </el-tooltip>
      </div>

      <!-- 颜色 -->
      <div class="toolbar-group">
        <span class="label">{{ t('collect.color') }}</span>
        <div class="color-list">
          <span
            v-for="c in colorPalette"
            :key="c"
            class="color-dot"
            :class="{ 'is-active': c === strokeColor }"
            :style="{ background: c }"
            @click="setColor(c)"
          />
          <el-color-picker
            v-model="customColor"
            size="small"
            class="color-picker"
            :predefine="colorPalette"
            @change="onCustomColorChange"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { getStroke } from 'perfect-freehand'
import { useEventListener, useResizeObserver } from '@vueuse/core'
import {
  ANGLE_MAX,
  ANGLE_MIN,
  BRUSH_PRESETS,
  BRUSH_PRESETS_BY_ID,
  DEFAULT_BRUSH_ID,
  SIZE_MAX,
  SIZE_MIN,
  type BrushPreset,
} from '@/utils/brushPresets'
import { CANVAS_DEFAULT_HEIGHT, CANVAS_DEFAULT_WIDTH } from '@/utils/constants'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

// -------------------- 类型定义 --------------------
interface RawPoint {
  x: number
  y: number
  pressure: number
  /** 毫秒时间戳（用于速度估算） */
  t: number
}

interface StrokeSnapshot {
  id: string
  /** 完整 SVG path d（含 Z 闭合） */
  d: string
  color: string
}

// -------------------- Props / Emits --------------------
const props = withDefaults(
  defineProps<{
    modelValue?: string
    width?: number
    height?: number
    initialColor?: string
    initialBrush?: string
    initialSize?: number
    initialAngle?: number
    initialPressure?: boolean
    readonly?: boolean
  }>(),
  {
    width: CANVAS_DEFAULT_WIDTH,
    height: CANVAS_DEFAULT_HEIGHT,
    modelValue: '',
    initialColor: '#0D9488',
    initialBrush: DEFAULT_BRUSH_ID,
    initialSize: 8,
    initialAngle: 0,
    initialPressure: true,
    readonly: false,
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
  change: [strokes: number]
  start: []
  end: []
}>()

// -------------------- 状态 --------------------
const containerRef = ref<HTMLDivElement | null>(null)
const svgRef = ref<SVGSVGElement | null>(null)

/** 原始输入点（带压感） */
const rawPoints = ref<RawPoint[]>([])

/** 已提交笔画：每条保存自己的 size/color/brush/angle，确保撤销/重做/导出与原始设置一致 */
const committedStrokes = ref<StrokeSnapshot[]>([])

/** 重做栈：保存最近被撤销的完整笔画对象 */
const redoStack = ref<StrokeSnapshot[]>([])

const isDrawing = ref(false)

const strokeSize = ref<number>(clamp(props.initialSize, SIZE_MIN, SIZE_MAX))
const brushAngle = ref<number>(clamp(props.initialAngle, ANGLE_MIN, ANGLE_MAX))
const pressureEnabled = ref<boolean>(props.initialPressure)
const strokeColor = ref<string>(props.initialColor)
const customColor = ref<string>(props.initialColor)

const activeBrushId = ref<string>(props.initialBrush)
const brushPresets: BrushPreset[] = BRUSH_PRESETS

const colorPalette = ['#0D9488', '#134E4A', '#F97316', '#10B981', '#EF4444', '#3B82F6', '#111827']

const sizeMin = SIZE_MIN
const sizeMax = SIZE_MAX
const angleMin = ANGLE_MIN
const angleMax = ANGLE_MAX

const strokesCount = computed(() => committedStrokes.value.length)
const canUndo = computed(() => committedStrokes.value.length > 0)
const canRedo = computed(() => redoStack.value.length > 0)

const wrapStyle = computed(() => ({
  width: '100%',
  maxWidth: `${props.width}px`,
  aspectRatio: `${props.width} / ${props.height}`,
}))

const hintText = computed(() => {
  const cat = currentBrush.value?.category
  if (cat === 'willow') return t('collect.hintWillow')
  if (cat === 'brush') return t('collect.hintBrush')
  return t('collect.hintPen')
})

const currentColor = computed(() => strokeColor.value)

// -------------------- 笔刷/工具函数 --------------------
function clamp(v: number, min: number, max: number): number {
  return Math.max(min, Math.min(max, v))
}

const currentBrush = computed<BrushPreset>(
  () => BRUSH_PRESETS_BY_ID[activeBrushId.value] ?? BRUSH_PRESETS_BY_ID[DEFAULT_BRUSH_ID]
)

/** 把原始点转成 perfect-freehand 期望的 [x, y, pressure] */
function toFreehandInput(points: RawPoint[]): number[][] {
  return points.map((p) => [p.x, p.y, pressureEnabled.value ? p.pressure : 0.5])
}

/** 围绕质心旋转点集（角度制） */
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

/** perfect-freehand 轮廓点 → 闭合 SVG path d（带 Z 闭合，可被 fill 正确填充） */
function outlineToSvgPath(outline: number[][]): string {
  if (outline.length < 2) return ''
  // 量化到两位小数，去除完全重复的连续点
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

/** 实时预览 path：只使用 base size，不做旋转（旋转需完整轮廓） */
const currentPath = computed(() => {
  if (rawPoints.value.length < 2) return ''
  const outline = getStroke(toFreehandInput(rawPoints.value), {
    size: strokeSize.value,
    thinning: currentBrush.value.thinning,
    smoothing: currentBrush.value.smoothing,
    streamline: currentBrush.value.streamline,
    easing: (t: number) => t,
    last: true,
    start: { taper: currentBrush.value.taperStart, easing: (t: number) => t },
    end: { taper: currentBrush.value.taperEnd, easing: (t: number) => t },
  }) as unknown as number[][]
  return outlineToSvgPath(outline)
})

// -------------------- 事件处理 --------------------
function getSvgPoint(e: PointerEvent): RawPoint | null {
  const svg = svgRef.value
  if (!svg) return null
  const rect = svg.getBoundingClientRect()
  if (rect.width === 0 || rect.height === 0) return null
  const x = ((e.clientX - rect.left) / rect.width) * props.width
  const y = ((e.clientY - rect.top) / rect.height) * props.height
  if (x < 0 || y < 0 || x > props.width || y > props.height) return null
  // 鼠标无压感时，模拟 0.5；压感笔/触屏使用真实值
  const rawPressure = e.pressure
  const pressure = rawPressure && rawPressure > 0 ? rawPressure : 0.5
  return { x, y, pressure, t: e.timeStamp || performance.now() }
}

function onPointerDown(e: PointerEvent) {
  if (props.readonly) return
  // 仅响应左键 / 触屏 / 笔
  if (e.pointerType === 'mouse' && e.button !== 0) return
  e.preventDefault()
  const p = getSvgPoint(e)
  if (!p) return
  isDrawing.value = true
  rawPoints.value = [p]
  try {
    ;(e.currentTarget as Element).setPointerCapture?.(e.pointerId)
  } catch {
    /* 旧浏览器忽略 */
  }
  emit('start')
}

/** rAF 节流：高频 pointermove 不会触发多余渲染 */
let rafId: number | null = null
function onPointerMove(e: PointerEvent) {
  if (!isDrawing.value) return
  const p = getSvgPoint(e)
  if (!p) return
  rawPoints.value.push(p)
  if (rafId === null) {
    rafId = requestAnimationFrame(() => {
      // 触发 currentPath computed 重算
      rawPoints.value = rawPoints.value.slice()
      rafId = null
    })
  }
}

function onPointerUp() {
  if (!isDrawing.value) return
  isDrawing.value = false
  if (rafId !== null) {
    cancelAnimationFrame(rafId)
    rafId = null
  }
  if (rawPoints.value.length > 1) {
    const outline = getStroke(toFreehandInput(rawPoints.value), {
      size: strokeSize.value,
      thinning: currentBrush.value.thinning,
      smoothing: currentBrush.value.smoothing,
      streamline: currentBrush.value.streamline,
      easing: (t: number) => t,
      last: true,
      start: { taper: currentBrush.value.taperStart, easing: (t: number) => t },
      end: { taper: currentBrush.value.taperEnd, easing: (t: number) => t },
    }) as unknown as number[][]
    const rotated = rotateAroundCentroid(outline, brushAngle.value)
    const d = outlineToSvgPath(rotated)
    if (d) {
      committedStrokes.value.push({
        id: `s-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
        d,
        color: strokeColor.value,
      })
      redoStack.value = []
      emit('change', committedStrokes.value.length)
    }
  }
  rawPoints.value = []
  emit('end')
}

// -------------------- 撤销 / 重做 / 清空 --------------------
function undo() {
  if (committedStrokes.value.length === 0) return
  const popped = committedStrokes.value.pop()!
  redoStack.value.push(popped)
  emit('change', committedStrokes.value.length)
  scheduleExport()
}

function redo() {
  if (redoStack.value.length === 0) return
  const popped = redoStack.value.pop()!
  committedStrokes.value.push(popped)
  emit('change', committedStrokes.value.length)
  scheduleExport()
}

function clear() {
  committedStrokes.value = []
  redoStack.value = []
  rawPoints.value = []
  emit('change', 0)
  scheduleExport()
}

function setColor(c: string) {
  strokeColor.value = c
  customColor.value = c
}

function onCustomColorChange(c: string | null) {
  if (c) strokeColor.value = c
}

function setBrush(id: string) {
  if (BRUSH_PRESETS_BY_ID[id]) activeBrushId.value = id
}

// -------------------- 导出（修复：使用 fill 闭合路径） --------------------
/** 把所有笔画渲染到指定画布 */
function renderToCanvas(): HTMLCanvasElement {
  const dpr = Math.max(1, window.devicePixelRatio || 1)
  const canvas = document.createElement('canvas')
  canvas.width = props.width * dpr
  canvas.height = props.height * dpr
  canvas.style.width = `${props.width}px`
  canvas.style.height = `${props.height}px`
  const ctx = canvas.getContext('2d')
  if (!ctx) return canvas
  ctx.scale(dpr, dpr)
  ctx.fillStyle = '#FFFFFF'
  ctx.fillRect(0, 0, props.width, props.height)
  for (const s of committedStrokes.value) {
    const path = new Path2D(s.d)
    ctx.fillStyle = s.color
    ctx.fill(path)
  }
  return canvas
}

function toDataURL(mime = 'image/png', quality = 1): string {
  return renderToCanvas().toDataURL(mime, quality)
}

function toBlob(mime = 'image/png', quality = 1): Promise<Blob | null> {
  return new Promise((resolve) => renderToCanvas().toBlob((b) => resolve(b), mime, quality))
}

function toSVGString(): string {
  const paths = committedStrokes.value
    .map((s) => `<path d="${s.d}" fill="${s.color}" fill-rule="nonzero" />`)
    .join('')
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${props.width}" height="${props.height}" viewBox="0 0 ${props.width} ${props.height}">${paths}</svg>`
}

// -------------------- modelValue 同步（防抖） --------------------
let exportTimer: number | null = null
function scheduleExport() {
  if (exportTimer !== null) window.clearTimeout(exportTimer)
  exportTimer = window.setTimeout(() => {
    if (committedStrokes.value.length === 0) {
      emit('update:modelValue', '')
    } else {
      try {
        emit('update:modelValue', toDataURL())
      } catch (err) {
        console.warn('[HandwritingPad] export failed', err)
      }
    }
  }, 80)
}

watch(committedStrokes, scheduleExport, { deep: true })

// -------------------- 键盘快捷键 --------------------
useEventListener(window, 'keydown', (e: KeyboardEvent) => {
  if (props.readonly) return
  // 在输入框中不拦截
  const tag = (e.target as HTMLElement | null)?.tagName
  if (tag === 'INPUT' || tag === 'TEXTAREA' || (e.target as HTMLElement | null)?.isContentEditable)
    return
  const ctrl = e.ctrlKey || e.metaKey
  if (ctrl && (e.key === 'z' || e.key === 'Z') && !e.shiftKey) {
    e.preventDefault()
    undo()
  } else if (
    ctrl &&
    (e.key === 'y' || e.key === 'Y' || (e.shiftKey && (e.key === 'z' || e.key === 'Z')))
  ) {
    e.preventDefault()
    redo()
  }
})

useResizeObserver(containerRef, () => {
  /* viewBox 已自适应，无需手动重绘 */
})

onMounted(() => {
  emit('change', 0)
})

onBeforeUnmount(() => {
  if (rafId !== null) cancelAnimationFrame(rafId)
  if (exportTimer !== null) window.clearTimeout(exportTimer)
})

defineExpose({
  clear,
  undo,
  redo,
  toDataURL,
  toBlob,
  toSVGString,
  getStrokeCount: () => strokesCount.value,
  isEmpty: () => strokesCount.value === 0,
  setBrush,
  setColor,
  setSize: (v: number) => (strokeSize.value = clamp(v, SIZE_MIN, SIZE_MAX)),
  setAngle: (v: number) => (brushAngle.value = clamp(v, ANGLE_MIN, ANGLE_MAX)),
  setPressure: (v: boolean) => (pressureEnabled.value = !!v),
})
</script>

<style lang="scss" scoped>
.handwriting-pad {
  @include flex-column;
  gap: $spacing-md;
  width: 100%;

  &__canvas-wrap {
    position: relative;
    width: 100%;
    background: $bg-elevated;
    border-radius: $radius-lg;
    border: 2px solid $border-base;
    overflow: hidden;
    box-shadow: $shadow-sm;
    transition: border-color $transition-base;

    &.is-drawing,
    &:hover {
      border-color: $color-primary-lighter;
    }
  }

  &__svg {
    display: block;
    width: 100%;
    height: 100%;
    touch-action: none;
    user-select: none;
    -webkit-user-select: none;
    cursor: crosshair;
  }

  &.is-drawing &__svg {
    cursor: grabbing;
  }

  &.is-readonly &__svg {
    cursor: default;
  }

  &__hint {
    position: absolute;
    top: $spacing-sm;
    left: $spacing-sm;
    padding: 2px $spacing-sm;
    background: rgba(13, 78, 74, 0.6);
    color: #fff;
    font-size: $font-size-xs;
    border-radius: $radius-sm;
    pointer-events: none;
    user-select: none;
  }

  &__toolbar {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: $spacing-md;
    padding: $spacing-sm $spacing-md;
    background: $bg-elevated;
    border-radius: $radius-md;
    border: 1px solid $border-light;
  }
}

.toolbar-group {
  @include flex-start;
  gap: $spacing-sm;

  &--slider {
    min-width: 200px;
  }

  &--brush {
    flex: 1 1 320px;
    min-width: 280px;
  }
}

.label {
  font-size: $font-size-sm;
  color: $text-secondary;
  white-space: nowrap;
}

.value {
  font-family: $font-family-mono;
  font-size: $font-size-sm;
  color: $text-primary;
  min-width: 42px;
  text-align: right;
}

.brush-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.brush-item {
  @include button-reset;
  @include flex-center;
  flex-direction: column;
  gap: 2px;
  width: 60px;
  height: 52px;
  padding: 4px 6px;
  border-radius: $radius-sm;
  background: $bg-muted;
  border: 1.5px solid transparent;
  transition: all $transition-fast;

  &__icon {
    font-size: 18px;
    color: $text-regular;
    line-height: 1;
  }

  &__name {
    font-size: 11px;
    color: $text-secondary;
    line-height: 1.2;
    text-align: center;
  }

  &:hover {
    background: $bg-overlay;
    transform: translateY(-1px);
  }

  &.is-active {
    background: $color-primary-lighter;
    border-color: $color-primary;
    box-shadow: 0 0 0 2px rgba(13, 148, 136, 0.18);

    .brush-item__icon,
    .brush-item__name {
      color: $color-primary-darker;
      font-weight: 600;
    }
  }

  &--pen {
    .brush-item__icon {
      color: #475569;
    }
  }
  &--brush {
    .brush-item__icon {
      color: #b45309;
    }
  }
  &--willow {
    .brush-item__icon {
      color: $color-primary;
    }
  }
}

.color-list {
  display: flex;
  align-items: center;
  gap: 6px;
}

.color-dot {
  display: inline-block;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  cursor: pointer;
  border: 2px solid transparent;
  transition:
    transform $transition-fast,
    border-color $transition-fast;

  &:hover {
    transform: scale(1.1);
  }
  &.is-active {
    border-color: $text-primary;
    transform: scale(1.1);
  }
}

.color-picker {
  :deep(.el-color-picker__trigger) {
    width: 26px;
    height: 26px;
  }
}
</style>

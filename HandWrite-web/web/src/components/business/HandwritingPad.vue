<template>
  <div class="hw-pad">
    <!-- ============== 画布区 ============== -->
    <div ref="containerRef" class="hw-pad__canvas" :style="wrapStyle">
      <div class="hw-pad__frame">
        <svg
          ref="svgRef"
          class="hw-pad__svg"
          :viewBox="`0 0 ${width} ${height}`"
          :width="width"
          :height="height"
          xmlns="http://www.w3.org/2000/svg"
          @pointerdown="onPointerDown"
          @pointermove="onPointerMove"
          @pointerup="onPointerUp"
          @pointercancel="onPointerUp"
          @pointerleave="onPointerUp"
          @mousedown="onPointerDown"
          @mousemove="onPointerMove"
          @mouseup="onPointerUp"
          @mouseleave="onPointerUp"
          @touchstart.passive="onPointerDown"
          @touchmove.passive="onPointerMove"
          @touchend="onPointerUp"
          @touchcancel="onPointerUp"
        >
          <!-- 画稿笺纸背景（带格线） -->
          <g class="hw-pad__paper" aria-hidden="true">
            <image
              :xlink:href="GRID_PAPER_TEXTURE_SVG"
              :href="GRID_PAPER_TEXTURE_SVG"
              :x="0"
              :y="0"
              :width="width"
              :height="height"
              preserveAspectRatio="none"
            />
            <rect :x="0" :y="0" :width="width" :height="height" fill="transparent" />
          </g>

          <!-- 笔画 -->
          <path
            v-for="stroke in committedStrokes"
            :key="stroke.id"
            :d="stroke.d"
            :fill="stroke.color"
            fill-rule="nonzero"
          />
          <path v-if="currentPath" :d="currentPath" :fill="currentColor" fill-rule="nonzero" />
        </svg>

        <!-- 朱砂印章 · 存稿 -->
        <button
          v-if="!readonly"
          type="button"
          class="hw-pad__seal"
          :class="{ 'is-ready': strokesCount > 0 }"
          :disabled="strokesCount === 0"
          :aria-label="t('collect.save')"
          @click="onSave"
        >
          <span class="hw-pad__seal-text">存稿</span>
          <span class="hw-pad__seal-sub">{{ t('collect.saveSub') }}</span>
        </button>

        <!-- 落款提示 -->
        <div v-if="!readonly" class="hw-pad__hint" aria-hidden="true">
          {{ hintText }}
        </div>
      </div>
    </div>

    <!-- ============== 第一行：题名 + 笔刷 ============== -->
    <div class="hw-pad__row hw-pad__row--title">
      <div class="hw-pad__title">
        <span class="hw-pad__label">{{ t('collect.artworkTitle') }}</span>
        <div class="hw-pad__input-frame">
          <input
            v-model="artworkTitle"
            type="text"
            class="hw-pad__input"
            :placeholder="t('collect.artworkTitlePh')"
            maxlength="20"
            @input="onTitleInput"
          />
        </div>
      </div>

      <div class="hw-pad__brushes">
        <span class="hw-pad__label">{{ t('collect.brush') }}</span>
        <div class="hw-pad__brush-list">
          <button
            v-for="preset in brushPresets"
            :key="preset.id"
            type="button"
            class="hw-pad__brush"
            :class="{ 'is-active': preset.id === activeBrushId }"
            :title="preset.name"
            @click="setBrush(preset.id)"
          >
            <span class="hw-pad__brush-name">{{ preset.name }}</span>
          </button>
        </div>
      </div>
    </div>

    <!-- ============== 笔尖参数区 ============== -->
    <div class="hw-pad__row hw-pad__row--params">
      <div class="hw-pad__param">
        <span class="hw-pad__label"
          >{{ t('collect.size') }} <em>{{ strokeSize }}px</em></span
        >
        <el-slider
          v-model="strokeSize"
          :min="sizeMin"
          :max="sizeMax"
          :step="1"
          aria-label="brush-size"
          class="hw-slider"
        />
      </div>
      <div class="hw-pad__divider" />
      <div class="hw-pad__param">
        <span class="hw-pad__label"
          >{{ t('collect.angle') }} <em>{{ brushAngle }}°</em></span
        >
        <el-slider
          v-model="brushAngle"
          :min="angleMin"
          :max="angleMax"
          :step="1"
          aria-label="brush-angle"
          class="hw-slider"
        />
      </div>
      <div class="hw-pad__divider" />
    </div>

    <!-- ============== 压感设置 ============== -->
    <div class="hw-pad__row hw-pad__row--pressure">
      <div class="hw-pad__pressure">
        <button
          type="button"
          class="hw-pad__seal-check"
          :class="{ 'is-checked': pressureEnabled }"
          :aria-label="t('collect.pressure')"
          @click="pressureEnabled = !pressureEnabled"
        >
          <span v-if="pressureEnabled" class="hw-pad__seal-tick" />
        </button>
        <span class="hw-pad__label">{{ t('collect.pressure') }}</span>
      </div>
      <div class="hw-pad__divider" />
      <div class="hw-pad__sensitivity">
        <span class="hw-pad__label"
          >{{ t('collect.sensitivity') }} <em>{{ pressureSensitivity }}%</em></span
        >
        <el-slider
          v-model="pressureSensitivity"
          :min="0"
          :max="200"
          :step="5"
          :disabled="!pressureEnabled"
          aria-label="pressure-sensitivity"
          class="hw-slider hw-slider--ruler"
        />
      </div>
    </div>

    <!-- ============== 色卡区 ============== -->
    <div class="hw-pad__row hw-pad__row--palette">
      <div class="hw-pad__palette-title">
        <span class="hw-pad__label">{{ t('collect.pigmentTitle') }}</span>
        <div class="hw-pad__input-frame hw-pad__input-frame--small">
          <input :value="activeColorMeta.name" type="text" class="hw-pad__input" readonly />
        </div>
        <el-color-picker
          v-model="customColor"
          size="small"
          class="hw-pad__picker"
          :predefine="INK_PALETTE.map((c) => c.hex)"
          @change="onCustomColorChange"
        />
      </div>
      <div class="hw-pad__divider hw-pad__divider--branch" />
      <div class="hw-pad__palette">
        <button
          v-for="c in INK_PALETTE"
          :key="c.id"
          type="button"
          class="hw-pad__disc"
          :class="{ 'is-active': strokeColor.toLowerCase() === c.hex.toLowerCase() }"
          :style="{ '--disc-color': c.hex }"
          :title="c.name"
          @click="setColor(c.hex)"
        >
          <span class="hw-pad__disc-name">{{ c.short }}</span>
        </button>
      </div>
    </div>

    <!-- ============== 底部状态栏 ============== -->
    <div class="hw-pad__row hw-pad__row--status">
      <div class="hw-pad__status">
        <span class="hw-pad__status-item">
          {{ t('collect.strokeCount') }} <em>{{ strokesCount }}</em>
        </span>
        <span class="hw-pad__status-divider">·</span>
        <span class="hw-pad__status-item">
          {{ t('collect.duration') }} <em>{{ durationLabel }}</em>
        </span>
        <span v-if="dynamicMode !== 'solid'" class="hw-pad__status-divider">·</span>
        <el-select
          v-if="dynamicMode !== 'solid'"
          v-model="dynamicMode"
          size="small"
          class="hw-select hw-select--mini"
        >
          <el-option label="纯色" value="solid" />
          <el-option label="渐变" value="gradient" />
          <el-option label="呼吸" value="breathing" />
        </el-select>
      </div>
      <div class="hw-pad__actions">
        <button
          type="button"
          class="hw-pad__chip"
          :disabled="!canUndo"
          :title="t('collect.undo')"
          @click="undo"
        >
          撤销
        </button>
        <button
          type="button"
          class="hw-pad__chip"
          :disabled="!canRedo"
          :title="t('collect.redo')"
          @click="redo"
        >
          翻页
        </button>
        <button
          type="button"
          class="hw-pad__chip hw-pad__chip--danger"
          :disabled="strokesCount === 0"
          :title="t('collect.clear')"
          @click="clear"
        >
          删除
        </button>
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
  applyPressure,
  BRUSH_PRESETS,
  BRUSH_PRESETS_BY_ID,
  clamp,
  DEFAULT_BRUSH_ID,
  DEFAULT_PRESSURE_SENSITIVITY,
  SIZE_MAX,
  SIZE_MIN,
  type BrushPreset,
  type PressureCurve,
} from '@/utils/brushPresets'
import { CANVAS_DEFAULT_HEIGHT, CANVAS_DEFAULT_WIDTH } from '@/utils/constants'
import { GRID_PAPER_TEXTURE_SVG, INK_PALETTE, closestInk } from '@/utils/inkPalette'
import { useI18n } from 'vue-i18n'
import '@/styles/ink.scss'

const { t } = useI18n()

// -------------------- 类型 --------------------
interface RawPoint {
  x: number
  y: number
  pressure: number
  t: number
}
interface StrokeSnapshot {
  id: string
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
    initialPressureSensitivity?: number
    initialPressureCurve?: PressureCurve
    initialArtworkTitle?: string
    readonly?: boolean
  }>(),
  {
    width: CANVAS_DEFAULT_WIDTH,
    height: CANVAS_DEFAULT_HEIGHT,
    modelValue: '',
    initialColor: '#2D2A26',
    initialBrush: DEFAULT_BRUSH_ID,
    initialSize: 8,
    initialAngle: 0,
    initialPressure: true,
    initialPressureSensitivity: DEFAULT_PRESSURE_SENSITIVITY,
    initialPressureCurve: 'linear',
    initialArtworkTitle: '',
    readonly: false,
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'update:artworkTitle': [value: string]
  change: [strokes: number]
  start: []
  end: []
  save: [
    payload: {
      blob: Blob
      svg: string
      dataUrl: string
      strokeCount: number
      durationMs: number
      title: string
    },
  ]
}>()

// -------------------- Refs --------------------
const containerRef = ref<HTMLDivElement | null>(null)
const svgRef = ref<SVGSVGElement | null>(null)
const rawPoints = ref<RawPoint[]>([])
const committedStrokes = ref<StrokeSnapshot[]>([])
const redoStack = ref<StrokeSnapshot[]>([])
const isDrawing = ref(false)
const sessionStart = ref<number>(0)
const lastTickAt = ref<number>(performance.now())

// 工具状态
const strokeSize = ref<number>(clamp(props.initialSize, SIZE_MIN, SIZE_MAX))
const brushAngle = ref<number>(clamp(props.initialAngle, ANGLE_MIN, ANGLE_MAX))
const pressureEnabled = ref<boolean>(props.initialPressure)
const pressureSensitivity = ref<number>(clamp(props.initialPressureSensitivity, 0, 200))
const pressureCurve = ref<PressureCurve>(props.initialPressureCurve)
const strokeColor = ref<string>(props.initialColor)
const customColor = ref<string>(props.initialColor)
const secondaryColor = ref<string>('#A65E44')
const dynamicMode = ref<'solid' | 'gradient' | 'breathing'>('solid')
const artworkTitle = ref<string>(props.initialArtworkTitle)

// 笔刷
const activeBrushId = ref<string>(props.initialBrush)
const brushPresets: BrushPreset[] = BRUSH_PRESETS
const currentBrush = computed<BrushPreset>(
  () => BRUSH_PRESETS_BY_ID[activeBrushId.value] ?? BRUSH_PRESETS_BY_ID[DEFAULT_BRUSH_ID]
)

const sizeMin = SIZE_MIN
const sizeMax = SIZE_MAX
const angleMin = ANGLE_MIN
const angleMax = ANGLE_MAX

const strokesCount = computed(() => committedStrokes.value.length)
const canUndo = computed(() => committedStrokes.value.length > 0)
const canRedo = computed(() => redoStack.value.length > 0)
const dynamicEnabled = computed(() => dynamicMode.value !== 'solid')

const wrapStyle = computed(() => ({
  width: '100%',
  maxWidth: `${props.width}px`,
  aspectRatio: `${props.width} / ${props.height}`,
}))

const activeColorMeta = computed(() => closestInk(strokeColor.value))

/** 用时计时 */
const durationLabel = computed(() => {
  const ms = sessionStart.value > 0 ? performance.now() - sessionStart.value : 0
  const s = Math.floor(ms / 1000)
  const m = Math.floor(s / 60)
  return `${String(m).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`
})

const hintText = computed(() => {
  const cat = currentBrush.value.category
  if (cat === 'willow') return t('collect.hintWillow')
  if (cat === 'brush') return t('collect.hintBrush')
  return t('collect.hintPen')
})

function resolveColorAt(t: number): string {
  if (!dynamicEnabled.value) return strokeColor.value
  const a = hexToRgb(strokeColor.value)
  const b = hexToRgb(secondaryColor.value)
  if (dynamicMode.value === 'gradient') return strokeColor.value
  // breathing
  const phase = ((t % 1500) / 1500) * Math.PI * 2
  const k = 0.5 - 0.5 * Math.cos(phase)
  return rgbToHex(a[0] + (b[0] - a[0]) * k, a[1] + (b[1] - a[1]) * k, a[2] + (b[2] - a[2]) * k)
}

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
function rgbToHex(r: number, g: number, b: number): string {
  const c = (v: number) => clamp(Math.round(v), 0, 255).toString(16).padStart(2, '0')
  return `#${c(r)}${c(g)}${c(b)}`
}

const currentColor = computed(() =>
  dynamicEnabled.value ? resolveColorAt(performance.now()) : strokeColor.value
)

// -------------------- 工具函数 --------------------
function toFreehandInput(points: RawPoint[]): number[][] {
  return points.map((p) => {
    const out = pressureEnabled.value
      ? applyPressure(p.pressure, pressureSensitivity.value, pressureCurve.value)
      : 0.5
    return [p.x, p.y, out]
  })
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
  return outlineToSvgPath(rotateAroundCentroid(outline, brushAngle.value))
})

// -------------------- 事件 --------------------
type InputEvent = PointerEvent | MouseEvent | TouchEvent

function extractCoords(
  e: InputEvent
): { x: number; y: number; pressure: number; pointerId: number; t: number } | null {
  if (e instanceof TouchEvent) {
    const t = e.touches[0] || e.changedTouches[0]
    if (!t) return null
    return {
      x: t.clientX,
      y: t.clientY,
      pressure: t.force || 0.5,
      pointerId: t.identifier,
      t: Date.now(),
    }
  }
  // PointerEvent / MouseEvent 都具备 clientX/Y/pressure
  const pe = e as PointerEvent & MouseEvent
  return {
    x: pe.clientX,
    y: pe.clientY,
    pressure: typeof pe.pressure === 'number' && pe.pressure > 0 ? pe.pressure : 0.5,
    pointerId: (pe as PointerEvent).pointerId ?? 0,
    t: pe.timeStamp || performance.now(),
  }
}

function getSvgPoint(coords: {
  x: number
  y: number
  pressure: number
  t: number
}): RawPoint | null {
  const svg = svgRef.value
  if (!svg) return null
  const rect = svg.getBoundingClientRect()
  if (rect.width === 0 || rect.height === 0) return null
  const x = ((coords.x - rect.left) / rect.width) * props.width
  const y = ((coords.y - rect.top) / rect.height) * props.height
  if (x < 0 || y < 0 || x > props.width || y > props.height) return null
  return { x, y, pressure: coords.pressure, t: coords.t }
}

function isLeftInput(e: InputEvent): boolean {
  if (e instanceof TouchEvent) return true
  const me = e as MouseEvent
  // mouse: 仅响应左键 (button 0)；中/右键直接 return
  // pen/tablet: 在悬停/移动时 button 可能是 -1，仅当 button === 0 视为有效按下
  if (me.button !== undefined && me.button > 0) return false
  return true
}

let activePointerId: number | null = null

function onPointerDown(e: InputEvent) {
  if (props.readonly) return
  if (!isLeftInput(e)) return
  // 避免与鼠标右键菜单冲突
  if (e.cancelable) e.preventDefault()
  const coords = extractCoords(e)
  if (!coords) return
  const p = getSvgPoint(coords)
  if (!p) return
  isDrawing.value = true
  activePointerId = coords.pointerId
  rawPoints.value = [p]
  if (sessionStart.value === 0) sessionStart.value = performance.now()
  // 仅 PointerEvent 可调用 setPointerCapture；旧浏览器/鼠标事件降级到 window 监听
  const target = e.currentTarget as Element | null
  if (e instanceof PointerEvent) {
    try {
      target?.setPointerCapture?.(e.pointerId)
    } catch {
      /* 旧浏览器忽略 */
    }
  }
  emit('start')
}

let rafId: number | null = null
function onPointerMove(e: InputEvent) {
  if (!isDrawing.value) return
  const coords = extractCoords(e)
  if (!coords) return
  const p = getSvgPoint(coords)
  if (!p) return
  rawPoints.value.push(p)
  lastTickAt.value = performance.now()
  if (rafId === null) {
    rafId = requestAnimationFrame(() => {
      rawPoints.value = rawPoints.value.slice()
      rafId = null
    })
  }
}

function onPointerUp() {
  if (!isDrawing.value) return
  isDrawing.value = false
  activePointerId = null
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
    const finalColor = dynamicEnabled.value ? resolveColorAt(performance.now()) : strokeColor.value
    const d = outlineToSvgPath(rotated)
    if (d) {
      committedStrokes.value.push({
        id: `s-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
        d,
        color: finalColor,
      })
      redoStack.value = []
      emit('change', committedStrokes.value.length)
    }
  }
  rawPoints.value = []
  emit('end')
}

// -------------------- 历史 / 清空 --------------------
function undo() {
  if (committedStrokes.value.length === 0) return
  redoStack.value.push(committedStrokes.value.pop() as StrokeSnapshot)
  emit('change', committedStrokes.value.length)
  scheduleExport()
}
function redo() {
  if (redoStack.value.length === 0) return
  committedStrokes.value.push(redoStack.value.pop() as StrokeSnapshot)
  emit('change', committedStrokes.value.length)
  scheduleExport()
}
function clear() {
  committedStrokes.value = []
  redoStack.value = []
  rawPoints.value = []
  sessionStart.value = 0
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
function onTitleInput(e: Event) {
  const v = (e.target as HTMLInputElement).value
  emit('update:artworkTitle', v)
}

// -------------------- 导出 --------------------
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
  ctx.fillStyle = '#F5F0E6'
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
  const bg = `<rect x="0" y="0" width="${props.width}" height="${props.height}" fill="#F5F0E6" />`
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${props.width}" height="${props.height}" viewBox="0 0 ${props.width} ${props.height}">${bg}${paths}</svg>`
}

async function onSave() {
  if (strokesCount.value === 0) return
  const blob = await toBlob()
  if (!blob) return
  const dataUrl = toDataURL()
  const svg = toSVGString()
  const durationMs = sessionStart.value > 0 ? performance.now() - sessionStart.value : 0
  emit('save', {
    blob,
    svg,
    dataUrl,
    strokeCount: strokesCount.value,
    durationMs,
    title: artworkTitle.value,
  })
  scheduleExport()
}

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

// 快捷键
useEventListener(window, 'keydown', (e: KeyboardEvent) => {
  if (props.readonly) return
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
  } else if (ctrl && (e.key === 's' || e.key === 'S')) {
    e.preventDefault()
    onSave()
  }
})

useResizeObserver(containerRef, () => {
  /* viewBox 已自适应 */
})

// 手写板兜底：当设备/驱动不触发 PointerEvent 但能触发 MouseEvent 时，
// 在 window 上挂一份 move/up 监听，确保拖出 SVG 范围仍能继续绘制并正常落笔。
useEventListener(window, 'mousemove', (e) => {
  if (!isDrawing.value) return
  if (activePointerId !== null) return // PointerEvent 模式已接管
  onPointerMove(e)
})
useEventListener(window, 'mouseup', () => {
  if (activePointerId !== null) return
  onPointerUp()
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
  setPressureSensitivity: (v: number) => (pressureSensitivity.value = clamp(v, 0, 200)),
  setPressureCurve: (v: PressureCurve) => (pressureCurve.value = v),
  setArtworkTitle: (v: string) => {
    artworkTitle.value = v
    emit('update:artworkTitle', v)
  },
  save: onSave,
})
</script>

<style lang="scss" scoped>
@use '@/styles/ink.scss' as ink;

.hw-pad {
  font-family: ink.$font-body;
  color: ink.$ink-black;
  @include flex-column;
  gap: ink.$spacing-block;
  width: 100%;
  @include ink.paper-bg;
  padding: ink.$spacing-block;
  border-radius: ink.$radius-lg;
  min-height: 720px;

  /* ============== 画布区 ============== */
  &__canvas {
    position: relative;
    width: 100%;
  }
  &__frame {
    position: relative;
    width: 100%;
    border: ink.$border-strong solid ink.$ochre;
    border-radius: ink.$radius;
    overflow: hidden;
    box-shadow:
      inset 0 0 0 1px rgba(166, 124, 82, 0.18),
      0 6px 24px rgba(45, 42, 38, 0.08);
    background-color: ink.$rice-paper;
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
  &__hint {
    position: absolute;
    bottom: ink.$spacing-inline;
    left: ink.$spacing-inline;
    padding: 2px 10px;
    background-color: rgba(45, 42, 38, 0.72);
    color: ink.$rice-paper;
    font-family: ink.$font-fang;
    font-size: 10pt;
    letter-spacing: 0.1em;
    border-radius: ink.$radius-sm;
    pointer-events: none;
  }

  /* 朱砂印章 */
  &__seal {
    position: absolute;
    top: ink.$spacing-inline;
    right: ink.$spacing-inline;
    width: 64px;
    height: 64px;
    border: 2px solid ink.$cinnabar;
    border-radius: 4px;
    background-color: rgba(168, 60, 50, 0.88);
    color: ink.$rice-paper;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 2px;
    cursor: pointer;
    font-family: ink.$font-zhuan;
    box-shadow: 0 4px 14px rgba(168, 60, 50, 0.25);
    transform: rotate(-2deg);
    transition:
      transform 0.2s,
      box-shadow 0.2s,
      opacity 0.2s;
    z-index: 10;

    &-text {
      font-size: 22pt;
      line-height: 1;
      letter-spacing: 0.05em;
      font-weight: 700;
      color: ink.$rice-paper;
    }
    &-sub {
      font-size: 7pt;
      letter-spacing: 0.2em;
      color: ink.$rice-paper;
      opacity: 0.85;
    }

    &:hover:not(:disabled) {
      transform: rotate(-2deg) scale(1.04);
      box-shadow: 0 6px 22px rgba(168, 60, 50, 0.4);
    }
    &:disabled {
      opacity: ink.$disabled-opacity;
      cursor: not-allowed;
    }
    &.is-ready {
      animation: seal-ink 3s ease-in-out infinite;
    }
  }

  /* ============== 行布局 ============== */
  &__row {
    @include flex-start;
    gap: ink.$spacing-block;
    align-items: stretch;
    @media (max-width: 1024px) {
      flex-direction: column;
    }

    &--title {
      align-items: flex-end;
    }
    &--params {
      gap: 16px;
    }
    &--pressure {
      align-items: center;
    }
    &--palette {
      align-items: stretch;
    }
    &--status {
      align-items: center;
      border-top: 1px dashed ink.$ink-grid;
      padding-top: ink.$spacing-inline;
      justify-content: space-between;
    }
  }
  &__divider {
    width: 1px;
    align-self: stretch;
    background: linear-gradient(
      to bottom,
      transparent,
      ink.$ink-grid 30%,
      ink.$ink-grid 70%,
      transparent
    );
    &--branch {
      width: 80px;
      height: 1px;
      background-image: url("data:image/svg+xml;utf8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 80 12' fill='none'%3E%3Cpath d='M0 6 C 20 4, 40 8, 80 5' stroke='%23C8C0B0' stroke-width='0.8'/%3E%3Ccircle cx='22' cy='5.5' r='1' fill='%23A83C32' fill-opacity='0.75'/%3E%3Ccircle cx='48' cy='6.5' r='1' fill='%23A83C32' fill-opacity='0.75'/%3E%3C/svg%3E");
      background-repeat: no-repeat;
      background-size: contain;
      background-color: transparent;
    }
  }

  /* 标签（仿古籍批注） */
  &__label {
    @include ink.label-caption;
    display: inline-flex;
    align-items: center;
    gap: 4px;
    em {
      font-style: normal;
      color: ink.$ink-black;
      font-weight: 600;
    }
  }

  /* 仿古笺纸输入框 */
  &__input-frame {
    @include ink.ink-frame(ink.$ink-deep);
    padding: 4px 10px;
    background-color: ink.$rice-paper-light;
    transition:
      border-color 0.2s,
      box-shadow 0.2s;
    &:focus-within {
      border-color: ink.$ochre;
      box-shadow: 0 0 0 2px rgba(166, 94, 68, 0.18);
    }
    &--small {
      max-width: 110px;
    }
  }
  &__input {
    border: 0;
    outline: 0;
    background: transparent;
    font-family: ink.$font-kai;
    font-size: 14pt;
    color: ink.$ink-black;
    width: 200px;
    padding: 6px 0;
  }

  /* 画稿题名 */
  &__title {
    @include flex-column;
    gap: 6px;
    align-items: flex-start;
    min-width: 240px;
  }

  /* 笔刷列 */
  &__brushes {
    @include flex-column;
    gap: 8px;
    flex: 1;
  }
  &__brush-list {
    display: grid;
    grid-template-columns: repeat(4, minmax(72px, 1fr));
    gap: 8px;
    @media (max-width: 600px) {
      grid-template-columns: repeat(2, 1fr);
    }
  }
  &__brush {
    @include ink.ink-frame(ink.$ink-deep);
    background-color: ink.$rice-paper-light;
    padding: 8px 6px;
    cursor: pointer;
    font-family: ink.$font-kai;
    transition:
      border-color 0.2s,
      background-color 0.2s,
      transform 0.15s;
    min-height: 44px;
    display: flex;
    align-items: center;
    justify-content: center;

    &-name {
      font-size: 11pt;
      color: ink.$ink-black;
      letter-spacing: 0.05em;
    }

    &:hover {
      background-color: ink.$rice-paper-deep;
      transform: translateY(-1px);
    }
    &.is-active {
      @include ink.selected-glow;
      background-color: ink.$jade;
    }
  }

  /* 笔尖参数区 */
  &__param {
    @include flex-column;
    gap: 6px;
    flex: 1;
    min-width: 180px;
  }
  &__sensitivity {
    @include flex-column;
    gap: 6px;
    flex: 1;
    min-width: 240px;
  }

  /* 压感设置 */
  &__pressure {
    @include flex-start;
    gap: 10px;
    align-items: center;
  }
  &__seal-check {
    width: 18px;
    height: 18px;
    border: 1.5px solid ink.$cinnabar;
    border-radius: 2px;
    background-color: ink.$rice-paper;
    cursor: pointer;
    position: relative;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    transition:
      background-color 0.2s,
      border-color 0.2s;
    padding: 0;
    &.is-checked {
      background-color: rgba(168, 60, 50, 0.18);
      border-color: ink.$cinnabar;
    }
  }
  &__seal-tick {
    display: inline-block;
    width: 14px;
    height: 14px;
    background-image: url("data:image/svg+xml;utf8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none'%3E%3Cpath d='M12 2 C 6 8, 6 16, 12 22' stroke='%23A83C32' stroke-width='1.5' stroke-linecap='round'/%3E%3Cpath d='M12 7 C 16 8, 18 9, 20 7' stroke='%23A83C32' stroke-width='1.5' stroke-linecap='round'/%3E%3Cpath d='M12 12 C 8 13, 6 14, 4 12' stroke='%23A83C32' stroke-width='1.5' stroke-linecap='round'/%3E%3C/svg%3E");
    background-repeat: no-repeat;
    background-position: center;
    background-size: contain;
  }

  /* 色卡 */
  &__palette-title {
    @include flex-start;
    gap: 12px;
    align-items: center;
    flex-wrap: wrap;
  }
  &__palette {
    flex: 1;
    display: grid;
    grid-template-columns: repeat(12, 1fr);
    gap: 8px;
    align-content: start;
    @media (max-width: 1024px) {
      grid-template-columns: repeat(8, 1fr);
    }
    @media (max-width: 600px) {
      grid-template-columns: repeat(6, 1fr);
    }
  }
  &__disc {
    --disc-color: #{ink.$ink-black};
    width: 24px;
    height: 24px;
    border-radius: 50%;
    border: 1px solid ink.$ink-black;
    background-color: var(--disc-color);
    cursor: pointer;
    position: relative;
    transition:
      transform 0.15s,
      box-shadow 0.2s;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    margin: 4px;

    &-name {
      font-family: ink.$font-kai;
      font-size: 8pt;
      color: ink.$rice-paper;
      mix-blend-mode: difference;
      opacity: 0;
      transition: opacity 0.2s;
      letter-spacing: 0;
    }

    &:hover {
      transform: scale(1.12);
      box-shadow: 0 2px 8px rgba(45, 42, 38, 0.22);
      .hw-pad__disc-name {
        opacity: 1;
      }
    }
    &.is-active {
      transform: scale(1.18);
      box-shadow:
        0 0 0 2px ink.$rice-paper,
        0 0 0 4px ink.$ochre,
        0 4px 12px rgba(45, 42, 38, 0.2);
    }
  }
  &__picker {
    :deep(.el-color-picker__trigger) {
      width: 28px;
      height: 28px;
      border: 1px solid ink.$ink-deep;
      border-radius: 4px;
      background-color: ink.$rice-paper-light;
    }
  }

  /* 状态栏 */
  &__status {
    @include flex-start;
    gap: 12px;
    font-family: ink.$font-fang;
    font-size: 10pt;
    color: ink.$ink-deep;
    .em,
    em {
      font-style: normal;
      color: ink.$ink-black;
      font-weight: 600;
      margin: 0 4px;
    }
  }
  &__status-divider {
    color: ink.$ink-grid;
  }
  &__actions {
    @include flex-end;
    gap: 10px;
  }
  &__chip {
    @include ink.ink-frame(ink.$ink-deep);
    background-color: ink.$wood;
    color: ink.$ink-black;
    font-family: ink.$font-kai;
    font-size: 12pt;
    padding: 6px 16px;
    min-width: 60px;
    min-height: 32px;
    cursor: pointer;
    transition:
      background-color 0.2s,
      border-color 0.2s,
      opacity 0.2s;
    letter-spacing: 0.15em;

    &:hover:not(:disabled) {
      background-color: ink.$wood-deep;
      color: ink.$rice-paper;
    }
    &:disabled {
      opacity: ink.$disabled-opacity;
      cursor: not-allowed;
    }
    &--danger {
      border-color: ink.$cinnabar;
      color: ink.$cinnabar;
      &:hover:not(:disabled) {
        background-color: ink.$cinnabar;
        color: ink.$rice-paper;
      }
    }
  }
}

/* Element Plus 控件国风化 */
:deep(.hw-slider) {
  --el-slider-runway-bg-color: #{ink.$ink-faint};
  --el-slider-main-bg-color: #{ink.$ink-black};
  --el-slider-button-size: 12px;
  --el-slider-height: 4px;
  --el-slider-border-radius: 2px;
  padding: 8px 0;
}
:deep(.hw-slider--ruler) {
  --el-slider-runway-bg-color: #{ink.$wood};
  background-image: repeating-linear-gradient(
    to right,
    transparent 0,
    transparent 9px,
    rgba(45, 42, 38, 0.35) 9px,
    rgba(45, 42, 38, 0.35) 10px
  );
  background-position: 0 50%;
  background-size: 100% 2px;
  background-repeat: no-repeat;
}
:deep(.hw-select) {
  --el-select-border-color: #{ink.$ink-deep};
  --el-select-bg-color: #{ink.$rice-paper-light};
  --el-select-text-color: #{ink.$ink-black};
  --el-select-border-color-hover: #{ink.$ochre};
  --el-select-input-focus-border-color: #{ink.$ochre};
  font-family: ink.$font-kai;
}
:deep(.hw-select--mini) {
  width: 80px;
}
:deep(.el-select__wrapper) {
  background-color: ink.$rice-paper-light;
  box-shadow: 0 0 0 1px ink.$ink-deep inset !important;
  border-radius: ink.$radius;
}
:deep(.el-select.is-focused .el-select__wrapper) {
  box-shadow:
    0 0 0 1px ink.$ochre inset,
    0 0 0 3px rgba(166, 94, 68, 0.18) !important;
}

@keyframes seal-ink {
  0%,
  100% {
    box-shadow: 0 4px 14px rgba(168, 60, 50, 0.25);
  }
  50% {
    box-shadow: 0 4px 24px rgba(168, 60, 50, 0.5);
  }
}
</style>

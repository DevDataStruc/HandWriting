<template>
  <div class="handwriting-pad" :class="{ 'is-drawing': isDrawing }">
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
        <g class="grid">
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

        <!-- 已保存的笔画 -->
        <path
          v-for="(stroke, i) in renderedStrokes"
          :key="`s-${i}`"
          :d="stroke"
          :fill="strokeColor"
        />

        <!-- 当前笔画 -->
        <path v-if="currentPath" :d="currentPath" :fill="strokeColor" />
      </svg>
    </div>

    <div class="handwriting-pad__toolbar">
      <div class="toolbar-group">
        <el-tooltip content="撤销 (Ctrl+Z)">
          <el-button :disabled="!canUndo" link @click="undo">
            <el-icon><Back /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip content="重做 (Ctrl+Y)">
          <el-button :disabled="!canRedo" link @click="redo">
            <el-icon><RefreshRight /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip content="清空">
          <el-button :disabled="strokesCount === 0" link @click="clear">
            <el-icon><Delete /></el-icon>
          </el-button>
        </el-tooltip>
      </div>

      <div class="toolbar-group toolbar-group--slider">
        <span class="label">笔画粗细</span>
        <el-slider
          v-model="strokeSize"
          :min="2"
          :max="32"
          :step="1"
          style="width: 120px"
          @change="onSizeChange"
        />
        <span class="value">{{ strokeSize }}px</span>
      </div>

      <div class="toolbar-group">
        <span class="label">颜色</span>
        <div class="color-list">
          <span
            v-for="c in colorPalette"
            :key="c"
            class="color-dot"
            :class="{ 'is-active': c === strokeColor }"
            :style="{ background: c }"
            @click="setColor(c)"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { getStroke } from 'perfect-freehand'
import { useEventListener, useResizeObserver } from '@vueuse/core'
import { CANVAS_DEFAULT_HEIGHT, CANVAS_DEFAULT_WIDTH, STROKE_DEFAULT_SIZE } from '@/utils/constants'

const props = withDefaults(
  defineProps<{
    modelValue?: string
    width?: number
    height?: number
    initialColor?: string
    readonly?: boolean
  }>(),
  {
    width: CANVAS_DEFAULT_WIDTH,
    height: CANVAS_DEFAULT_HEIGHT,
    initialColor: '#0D9488',
    readonly: false,
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
  change: [strokes: number]
  start: []
  end: []
}>()

const containerRef = ref<HTMLDivElement | null>(null)
const svgRef = ref<SVGSVGElement | null>(null)

const strokes = ref<number[][][]>([]) // 笔画数组：每个笔画是 perfect-freehand points
const redoStack = ref<number[][][]>([])
const currentPoints = ref<number[][]>([])
const isDrawing = ref(false)
const strokeSize = ref<number>(STROKE_DEFAULT_SIZE)
const strokeColor = ref<string>(props.initialColor)
const colorPalette = ['#0D9488', '#134E4A', '#F97316', '#10B981', '#EF4444', '#3B82F6', '#111827']

const strokesCount = computed(() => strokes.value.length)
const canUndo = computed(() => strokes.value.length > 0)
const canRedo = computed(() => redoStack.value.length > 0)

const wrapStyle = computed(() => ({
  width: '100%',
  maxWidth: `${props.width}px`,
  aspectRatio: `${props.width} / ${props.height}`,
}))

function toSvgPath(points: number[][]): string {
  if (points.length < 2) return ''
  const d = points.map(([x, y], i) => `${i === 0 ? 'M' : 'L'}${x.toFixed(2)},${y.toFixed(2)}`).join(' ')
  return d
}

const renderedStrokes = computed(() => strokes.value.map((s) => toSvgPath(s)))
const currentPath = computed(() => toSvgPath(currentPoints.value))

function getSvgPoint(e: PointerEvent): number[] | null {
  const svg = svgRef.value
  if (!svg) return null
  const rect = svg.getBoundingClientRect()
  const x = ((e.clientX - rect.left) / rect.width) * props.width
  const y = ((e.clientY - rect.top) / rect.height) * props.height
  if (x < 0 || y < 0 || x > props.width || y > props.height) return null
  return [x, y, e.pressure || 0.5]
}

function onPointerDown(e: PointerEvent) {
  if (props.readonly) return
  e.preventDefault()
  const p = getSvgPoint(e)
  if (!p) return
  isDrawing.value = true
  currentPoints.value = [p]
  ;(e.target as Element).setPointerCapture?.(e.pointerId)
  emit('start')
}

function onPointerMove(e: PointerEvent) {
  if (!isDrawing.value) return
  const p = getSvgPoint(e)
  if (!p) return
  currentPoints.value.push(p)
}

function onPointerUp() {
  if (!isDrawing.value) return
  isDrawing.value = false
  if (currentPoints.value.length > 1) {
    const freehandPoints = getStroke(currentPoints.value, {
      size: strokeSize.value * 2.2,
      thinning: 0.5,
      smoothing: 0.5,
      streamline: 0.5,
      easing: (t: number) => t,
      last: true,
    }) as unknown as number[][]
    strokes.value.push(freehandPoints)
    redoStack.value = []
    emit('change', strokes.value.length)
  }
  currentPoints.value = []
  emit('end')
}

function undo() {
  if (strokes.value.length === 0) return
  redoStack.value.push(strokes.value.pop() as number[][][])
  emit('change', strokes.value.length)
}

function redo() {
  if (redoStack.value.length === 0) return
  strokes.value.push(redoStack.value.pop() as number[][][])
  emit('change', strokes.value.length)
}

function clear() {
  strokes.value = []
  redoStack.value = []
  currentPoints.value = []
  emit('change', 0)
}

function setColor(c: string) {
  strokeColor.value = c
}

function onSizeChange(v: number | number[]) {
  strokeSize.value = typeof v === 'number' ? v : v[0]
}

/** 导出 PNG dataURL */
function toDataURL(mime = 'image/png', quality = 1): string {
  return renderToCanvas().toDataURL(mime, quality)
}

function toBlob(mime = 'image/png', quality = 1): Promise<Blob | null> {
  return new Promise((resolve) => renderToCanvas().toBlob((b) => resolve(b), mime, quality))
}

function toSVGString(): string {
  const paths = renderedStrokes.value.map((d) => `<path d="${d}" fill="${strokeColor.value}" />`).join('')
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${props.width}" height="${props.height}" viewBox="0 0 ${props.width} ${props.height}">${paths}</svg>`
}

function renderToCanvas(): HTMLCanvasElement {
  const canvas = document.createElement('canvas')
  canvas.width = props.width
  canvas.height = props.height
  const ctx = canvas.getContext('2d')
  if (!ctx) return canvas
  ctx.fillStyle = '#FFFFFF'
  ctx.fillRect(0, 0, props.width, props.height)
  ctx.fillStyle = strokeColor.value
  strokes.value.forEach((s) => {
    if (s.length < 2) return
    ctx.beginPath()
    s.forEach(([x, y], i) => {
      if (i === 0) ctx.moveTo(x, y)
      else ctx.lineTo(x, y)
    })
    ctx.closePath()
    ctx.fill()
  })
  return canvas
}

watch(strokes, () => {
  emit('update:modelValue', toDataURL())
}, { deep: true })

useEventListener(window, 'keydown', (e: KeyboardEvent) => {
  if (props.readonly) return
  const ctrl = e.ctrlKey || e.metaKey
  if (ctrl && e.key === 'z' && !e.shiftKey) {
    e.preventDefault()
    undo()
  } else if ((ctrl && e.key === 'y') || (ctrl && e.shiftKey && e.key === 'Z')) {
    e.preventDefault()
    redo()
  }
})

useResizeObserver(containerRef, () => {
  // 容器尺寸变化无需重绘 SVG，viewBox 已自适应
})

onMounted(() => {
  emit('change', 0)
})

onUnmounted(() => {
  // pointer capture 会被自动释放
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
    cursor: crosshair;
  }

  &.is-drawing &__svg {
    cursor: grabbing;
  }

  &__toolbar {
    @include flex-between;
    flex-wrap: wrap;
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
    min-width: 220px;
  }
}

.label {
  font-size: $font-size-sm;
  color: $text-secondary;
}

.value {
  font-family: $font-family-mono;
  font-size: $font-size-sm;
  color: $text-primary;
  min-width: 36px;
}

.color-list {
  display: flex;
  gap: 6px;
}

.color-dot {
  display: inline-block;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  cursor: pointer;
  border: 2px solid transparent;
  transition: transform $transition-fast, border-color $transition-fast;

  &:hover {
    transform: scale(1.1);
  }
  &.is-active {
    border-color: $text-primary;
    transform: scale(1.1);
  }
}
</style>

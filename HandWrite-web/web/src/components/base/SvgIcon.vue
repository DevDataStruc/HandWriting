<template>
  <svg
    :class="['svg-icon', $attrs.class]"
    :style="iconStyle"
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    aria-hidden="true"
    focusable="false"
  >
    <use :href="`#icon-${iconName}`" />
  </svg>
</template>

<script setup lang="ts">
import { computed, useAttrs } from 'vue'

defineOptions({ inheritAttrs: false })

const props = withDefaults(
  defineProps<{
    /** 图标名（不含扩展名），对应 src/assets/icons 下的 svg 文件名 */
    iconName: string
    /** 图标尺寸（px），默认 18 */
    size?: number | string
    /** 颜色，未传则继承 currentColor */
    color?: string
  }>(),
  { size: 18, color: '' }
)

defineEmits<{ click: [evt: MouseEvent] }>()

const attrs = useAttrs()
const iconStyle = computed(() => {
  const style: Record<string, string> = { ...(attrs.style as Record<string, string> | undefined) }
  if (props.color) style.color = props.color
  return style
})
</script>

<style scoped>
.svg-icon {
  display: inline-block;
  vertical-align: middle;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
  flex-shrink: 0;
}
</style>

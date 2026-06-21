<template>
  <span
    class="svg-icon"
    :style="{
      width: sizePx,
      height: sizePx,
      color,
    }"
    role="img"
    aria-hidden="true"
    v-html="path"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { getSvgIconPath, type SvgIconName } from '@/assets/icons/svg-icons'

const props = withDefaults(
  defineProps<{
    name: SvgIconName
    size?: number | string
    color?: string
  }>(),
  {
    size: 18,
    color: 'currentColor',
  }
)

const sizePx = computed(() => (typeof props.size === 'number' ? `${props.size}px` : props.size))
const path = computed(() => getSvgIconPath(props.name))
</script>

<style lang="scss" scoped>
.svg-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  line-height: 0;
  vertical-align: middle;

  :deep(svg) {
    width: 100%;
    height: 100%;
    display: block;
    fill: none;
    stroke: currentColor;
    stroke-width: 1.8;
    stroke-linecap: round;
    stroke-linejoin: round;
  }
}
</style>

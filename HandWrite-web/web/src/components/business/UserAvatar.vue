<template>
  <div class="user-avatar" :class="[`user-avatar--${size}`]" :style="customStyle">
    <img v-if="displaySrc" :src="displaySrc" :alt="displayName" @error="onError" />
    <span v-else class="user-avatar__fallback">{{ initial }}</span>
    <span v-if="status" class="user-avatar__status" :class="`is-${status}`" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { UserProfile } from '@/api/contracts/user'

const props = withDefaults(
  defineProps<{
    user?: UserProfile | null
    src?: string
    name?: string
    size?: 'small' | 'default' | 'large' | number
    status?: 'online' | 'offline' | 'busy'
  }>(),
  { size: 'default' }
)

const px = computed(() => {
  if (typeof props.size === 'number') return `${props.size}px`
  if (props.size === 'small') return '24px'
  if (props.size === 'large') return '64px'
  return '40px'
})

const customStyle = computed(() => ({
  width: px.value,
  height: px.value,
  fontSize: `calc(${px.value} * 0.4)`,
}))

const displaySrc = computed<string | undefined>(() => props.src || props.user?.avatar)
const displayName = computed<string>(() => props.name || props.user?.nickname || props.user?.username || '?')
const initial = computed(() => displayName.value.slice(0, 1).toUpperCase())

function onError(e: Event) {
  const t = e.target as HTMLImageElement
  t.style.display = 'none'
}
</script>

<style lang="scss" scoped>
.user-avatar {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: linear-gradient(135deg, $color-primary, $color-primary-light);
  color: $text-inverse;
  font-weight: 600;
  overflow: hidden;
  flex-shrink: 0;
  user-select: none;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  &__fallback {
    line-height: 1;
  }

  &__status {
    position: absolute;
    right: 0;
    bottom: 0;
    width: 25%;
    height: 25%;
    min-width: 8px;
    min-height: 8px;
    border-radius: 50%;
    border: 2px solid $bg-elevated;

    &.is-online {
      background: $color-success;
    }
    &.is-offline {
      background: $text-placeholder;
    }
    &.is-busy {
      background: $color-warning;
    }
  }
}
</style>

<template>
  <div class="base-card" :class="[`base-card--${variant}`, { 'is-hoverable': hoverable, 'is-flat': flat }]">
    <div v-if="title || $slots.header" class="base-card__header">
      <slot name="header">
        <div class="header-inner">
          <h3 class="title">{{ title }}</h3>
          <div v-if="subtitle" class="subtitle">{{ subtitle }}</div>
        </div>
      </slot>
      <div v-if="$slots.extra" class="extra">
        <slot name="extra" />
      </div>
    </div>
    <div class="base-card__body" :style="bodyStyle">
      <slot />
    </div>
    <div v-if="$slots.footer" class="base-card__footer">
      <slot name="footer" />
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    title?: string
    subtitle?: string
    variant?: 'default' | 'primary' | 'success' | 'warning' | 'danger'
    hoverable?: boolean
    flat?: boolean
    bodyStyle?: Record<string, string>
  }>(),
  { variant: 'default', hoverable: false, flat: false }
)
</script>

<style lang="scss" scoped>
.base-card {
  background: $bg-elevated;
  border-radius: $radius-lg;
  border: 1px solid $border-light;
  overflow: hidden;
  transition: box-shadow $transition-base, transform $transition-base, border-color $transition-base;

  &--default {
  }
  &--primary {
    border-color: $color-primary-lighter;
  }
  &--success {
    border-color: rgba(16, 185, 129, 0.3);
  }
  &--warning {
    border-color: rgba(245, 158, 11, 0.3);
  }
  &--danger {
    border-color: rgba(239, 68, 68, 0.3);
  }

  &.is-hoverable:hover {
    transform: translateY(-2px);
    box-shadow: $shadow-md;
    border-color: $color-primary-lighter;
  }

  &.is-flat {
    box-shadow: none;
  }

  &__header {
    @include flex-between;
    padding: $spacing-md $spacing-lg;
    border-bottom: 1px solid $border-light;

    .header-inner {
      @include flex-column;
      gap: 2px;
    }
    .title {
      font-size: $font-size-md;
      font-weight: 600;
      color: $text-primary;
    }
    .subtitle {
      font-size: $font-size-xs;
      color: $text-secondary;
    }
    .extra {
      @include flex-end;
      gap: $spacing-sm;
    }
  }

  &__body {
    padding: $spacing-lg;
  }

  &__footer {
    padding: $spacing-md $spacing-lg;
    border-top: 1px solid $border-light;
    background: $bg-muted;
  }
}
</style>

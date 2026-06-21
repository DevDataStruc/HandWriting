<template>
  <button
    class="base-button"
    :class="[
      `base-button--${type}`,
      `base-button--${size}`,
      { 'is-loading': loading, 'is-block': block, 'is-rounded': rounded },
    ]"
    :disabled="disabled || loading"
    :type="nativeType"
  >
    <span v-if="loading" class="base-button__spinner"></span>
    <el-icon v-else-if="icon" class="base-button__icon"><component :is="icon" /></el-icon>
    <span class="base-button__content">
      <slot />
    </span>
  </button>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    type?: 'primary' | 'secondary' | 'cta' | 'ghost' | 'danger' | 'text'
    size?: 'small' | 'default' | 'large'
    disabled?: boolean
    loading?: boolean
    block?: boolean
    rounded?: boolean
    icon?: string
    nativeType?: 'button' | 'submit' | 'reset'
  }>(),
  {
    type: 'primary',
    size: 'default',
    disabled: false,
    loading: false,
    block: false,
    rounded: false,
    nativeType: 'button',
  }
)
</script>

<style lang="scss" scoped>
.base-button {
  @include flex-center;
  gap: $spacing-sm;
  height: 44px;
  padding: 0 $spacing-lg;
  border-radius: $radius-lg;
  font-size: $font-size-md;
  font-weight: 500;
  letter-spacing: 0.4px;
  cursor: pointer;
  transition:
    background-color $transition-fast,
    border-color $transition-fast,
    color $transition-fast,
    box-shadow $transition-fast,
    transform $transition-fast;
  white-space: nowrap;
  user-select: none;
  position: relative;
  border: 1px solid transparent;

  // 尺寸变体
  &--small {
    height: 32px;
    padding: 0 $spacing-md;
    font-size: $font-size-sm;
    border-radius: $radius-md;
    gap: $spacing-xs;
  }
  &--large {
    height: 52px;
    padding: 0 $spacing-xl;
    font-size: $font-size-lg;
    border-radius: $radius-xl;
    gap: $spacing-sm;
  }

  // 全圆角
  &.is-rounded {
    border-radius: $radius-full;
    padding-left: $spacing-xl;
    padding-right: $spacing-xl;
  }
  &--small.is-rounded {
    padding-left: $spacing-md;
    padding-right: $spacing-md;
  }
  &--large.is-rounded {
    padding-left: $spacing-2xl;
    padding-right: $spacing-2xl;
  }

  &:focus-visible {
    outline: 2px solid $color-primary-light;
    outline-offset: 2px;
  }

  &:active:not(:disabled):not(.is-loading) {
    transform: translateY(1px) scale(0.98);
  }

  &:disabled {
    opacity: 0.55;
    cursor: not-allowed;
    box-shadow: none !important;
    transform: none !important;
  }

  &--primary {
    background: linear-gradient(135deg, $color-primary 0%, $color-primary-dark 100%);
    color: $text-inverse;
    box-shadow: 0 2px 6px -1px rgba(13, 148, 136, 0.4);
    &:hover:not(:disabled) {
      background: linear-gradient(135deg, $color-primary-light 0%, $color-primary 100%);
      box-shadow: 0 8px 18px -3px rgba(13, 148, 136, 0.5);
      transform: translateY(-2px);
    }
  }

  &--secondary {
    background: $bg-elevated;
    color: $color-primary;
    border: 1px solid $color-primary;
    &:hover:not(:disabled) {
      background: $bg-muted;
      transform: translateY(-1px);
      box-shadow: 0 4px 10px -2px rgba(13, 148, 136, 0.2);
    }
  }

  &--cta {
    background: linear-gradient(135deg, $color-cta 0%, $color-cta-dark 100%);
    color: $text-inverse;
    box-shadow: 0 2px 6px -1px rgba(249, 115, 22, 0.4);
    &:hover:not(:disabled) {
      background: linear-gradient(135deg, $color-cta-light 0%, $color-cta 100%);
      box-shadow: 0 8px 18px -3px rgba(249, 115, 22, 0.5);
      transform: translateY(-2px);
    }
  }

  &--ghost {
    background: transparent;
    color: $text-regular;
    border: 1px solid $border-base;
    &:hover:not(:disabled) {
      background: $bg-muted;
      color: $color-primary;
      border-color: $color-primary-light;
      transform: translateY(-1px);
    }
  }

  &--danger {
    background: linear-gradient(135deg, #F87171 0%, $color-danger 100%);
    color: $text-inverse;
    box-shadow: 0 2px 6px -1px rgba(239, 68, 68, 0.4);
    &:hover:not(:disabled) {
      background: linear-gradient(135deg, $color-danger 0%, #B91C1C 100%);
      box-shadow: 0 8px 18px -3px rgba(239, 68, 68, 0.5);
      transform: translateY(-2px);
    }
  }

  &--text {
    background: transparent;
    color: $color-primary;
    height: 36px;
    padding: 0 $spacing-sm;
    &:hover:not(:disabled) {
      background: $bg-muted;
    }
  }

  &.is-block {
    width: 100%;
  }
}

// ============================
// 关键：图标在按钮中不能溢出
// 1) .el-icon 必须有显式 width/height（不被 svg 撑爆）
// 2) .el-icon 内部的 svg 用 1em 跟随父容器
// ============================
.base-button__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 1em;
  height: 1em;
  font-size: inherit;

  // 覆盖 Element Plus icon 组件默认的尺寸
  --size: 1em;

  // 内部 svg 跟随父容器
  svg {
    width: 100% !important;
    height: 100% !important;
    display: block;
  }
}

.base-button--small .base-button__icon {
  font-size: 14px;
}
.base-button--default .base-button__icon {
  font-size: 18px;
}
.base-button--large .base-button__icon {
  font-size: 22px;
}

.base-button__spinner {
  width: 16px;
  height: 16px;
  border: 2px solid currentColor;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  flex-shrink: 0;
}

.base-button__content {
  display: inline-flex;
  align-items: center;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>

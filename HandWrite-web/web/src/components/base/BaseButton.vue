<template>
  <button
    class="base-button"
    :class="[`base-button--${type}`, { 'is-loading': loading, 'is-block': block }]"
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
    disabled?: boolean
    loading?: boolean
    block?: boolean
    icon?: string
    nativeType?: 'button' | 'submit' | 'reset'
  }>(),
  {
    type: 'primary',
    disabled: false,
    loading: false,
    block: false,
    nativeType: 'button',
  }
)
</script>

<style lang="scss" scoped>
.base-button {
  @include flex-center;
  gap: $spacing-sm;
  height: 38px;
  padding: 0 $spacing-md;
  border-radius: $radius-md;
  font-size: $font-size-base;
  font-weight: 500;
  cursor: pointer;
  transition: all $transition-fast;
  white-space: nowrap;
  user-select: none;

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  &--primary {
    background: $color-primary;
    color: $text-inverse;
    border: 1px solid $color-primary;
    &:hover:not(:disabled) {
      background: $color-primary-dark;
      border-color: $color-primary-dark;
      transform: translateY(-1px);
      box-shadow: $shadow-md;
    }
    &:active:not(:disabled) {
      transform: translateY(0);
    }
  }

  &--secondary {
    background: $bg-elevated;
    color: $color-primary;
    border: 1px solid $color-primary;
    &:hover:not(:disabled) {
      background: $bg-muted;
    }
  }

  &--cta {
    background: $color-cta;
    color: $text-inverse;
    border: 1px solid $color-cta;
    &:hover:not(:disabled) {
      background: $color-cta-dark;
      border-color: $color-cta-dark;
      transform: translateY(-1px);
      box-shadow: $shadow-md;
    }
  }

  &--ghost {
    background: transparent;
    color: $text-regular;
    border: 1px solid $border-base;
    &:hover:not(:disabled) {
      background: $bg-muted;
      color: $color-primary;
    }
  }

  &--danger {
    background: $color-danger;
    color: $text-inverse;
    border: 1px solid $color-danger;
    &:hover:not(:disabled) {
      background: darken($color-danger, 8%);
    }
  }

  &--text {
    background: transparent;
    color: $color-primary;
    border: 1px solid transparent;
    height: 32px;
    padding: 0 $spacing-sm;
    &:hover:not(:disabled) {
      background: $bg-muted;
    }
  }

  &.is-block {
    width: 100%;
  }
}

.base-button__spinner {
  width: 14px;
  height: 14px;
  border: 2px solid currentColor;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

.base-button__icon {
  font-size: 16px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>

<template>
  <BaseCard hoverable class="stats-card" :variant="variant">
    <div class="stats-card__inner">
      <div class="stats-card__icon" :style="{ background: iconBg }">
        <el-icon :size="24" :color="iconColor">
          <component :is="iconName" />
        </el-icon>
      </div>
      <div class="stats-card__body">
        <div class="stats-card__label">{{ label }}</div>
        <div class="stats-card__value">
          {{ formattedValue }}
          <span v-if="suffix" class="suffix">{{ suffix }}</span>
        </div>
        <div v-if="trend != null" class="stats-card__trend" :class="trendClass">
          <el-icon :size="12">
            <component :is="trend >= 0 ? 'CaretTop' : 'CaretBottom'" />
          </el-icon>
          <span>{{ Math.abs(trend) }}%</span>
          <span v-if="trendLabel" class="trend-label">{{ trendLabel }}</span>
        </div>
      </div>
    </div>
  </BaseCard>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import BaseCard from '@/components/base/BaseCard.vue'
import { formatNumber } from '@/utils/format'

const props = withDefaults(
  defineProps<{
    label: string
    value: number | string
    suffix?: string
    icon?: string
    trend?: number
    trendLabel?: string
    variant?: 'default' | 'primary' | 'success' | 'warning' | 'danger'
    precision?: number
  }>(),
  { variant: 'default', precision: 0 }
)

const iconName = computed<string>(() => props.icon || 'DataLine')

const formattedValue = computed(() => {
  if (typeof props.value === 'string') return props.value
  return formatNumber(props.value, props.precision)
})

const iconColor = computed(() => {
  switch (props.variant) {
    case 'primary':
      return '#0D9488'
    case 'success':
      return '#10B981'
    case 'warning':
      return '#F59E0B'
    case 'danger':
      return '#EF4444'
    default:
      return '#0D9488'
  }
})

const iconBg = computed(() => {
  const c = iconColor.value
  return `${c}15` // 8% alpha
})

const trendClass = computed(() => ({
  'is-up': (props.trend ?? 0) >= 0,
  'is-down': (props.trend ?? 0) < 0,
}))
</script>

<style lang="scss" scoped>
.stats-card {
  &__inner {
    @include flex-start;
    gap: $spacing-md;
  }
  &__icon {
    @include flex-center;
    width: 56px;
    height: 56px;
    border-radius: $radius-lg;
    flex-shrink: 0;
  }
  &__body {
    flex: 1;
    min-width: 0;
  }
  &__label {
    font-size: $font-size-sm;
    color: $text-secondary;
    margin-bottom: 4px;
  }
  &__value {
    font-size: $font-size-3xl;
    font-weight: 700;
    color: $text-primary;
    line-height: 1.2;
    .suffix {
      font-size: $font-size-md;
      color: $text-secondary;
      margin-left: 4px;
      font-weight: 500;
    }
  }
  &__trend {
    @include flex-start;
    gap: 2px;
    margin-top: 4px;
    font-size: $font-size-xs;

    &.is-up {
      color: $color-success;
    }
    &.is-down {
      color: $color-danger;
    }
    .trend-label {
      color: $text-secondary;
      margin-left: 4px;
    }
  }
}
</style>

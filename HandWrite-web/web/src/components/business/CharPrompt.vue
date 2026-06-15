<template>
  <div class="char-prompt" :class="{ 'is-large': size === 'large' }">
    <div class="char-prompt__box">
      <span v-if="!char" class="placeholder">?</span>
      <span v-else class="char">{{ char }}</span>
    </div>
    <div v-if="showMeta && char" class="char-prompt__meta">
      <span v-if="pinyin" class="pinyin">{{ pinyin }}</span>
      <span v-if="strokeCount" class="strokes">{{ strokeCount }} 画</span>
      <span v-if="difficulty" class="difficulty">难度: {{ difficulty }}/5</span>
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    char?: string
    pinyin?: string
    strokeCount?: number
    difficulty?: number
    showMeta?: boolean
    size?: 'default' | 'large'
  }>(),
  { showMeta: true, size: 'default' }
)
</script>

<style lang="scss" scoped>
.char-prompt {
  @include flex-column;
  align-items: center;
  gap: $spacing-sm;

  &__box {
    @include flex-center;
    width: 120px;
    height: 120px;
    background: $bg-elevated;
    border: 2px dashed $color-primary-lighter;
    border-radius: $radius-lg;
    box-shadow: $shadow-sm;
    transition: all $transition-base;

    .is-large & {
      width: 180px;
      height: 180px;
    }
  }

  &__box:hover {
    transform: scale(1.02);
    border-color: $color-primary;
  }

  .char {
    font-family: $font-family-cn;
    font-size: 80px;
    font-weight: 600;
    color: $color-primary-darker;
    line-height: 1;

    .is-large & {
      font-size: 120px;
    }
  }

  .placeholder {
    font-size: 60px;
    color: $text-placeholder;
  }

  &__meta {
    @include flex-start;
    gap: $spacing-md;
    font-size: $font-size-sm;
    color: $text-secondary;

    .pinyin {
      color: $color-primary;
      font-weight: 500;
    }
  }
}
</style>

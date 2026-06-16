<template>
  <div class="char-prompt" :class="{ 'is-large': size === 'large' }">
    <div class="char-prompt__box" :style="{ fontFamily: activeFont.stack }">
      <span v-if="!char" class="placeholder">?</span>
      <span v-else class="char">{{ char }}</span>
    </div>
    <div v-if="showMeta && char" class="char-prompt__meta">
      <span v-if="pinyin" class="pinyin">{{ pinyin }}</span>
      <span v-if="strokeCount" class="strokes">{{ strokeCount }} 画</span>
      <span v-if="difficulty" class="difficulty">难度: {{ difficulty }}/5</span>
    </div>

    <!-- 字体切换条：联动"换一个"按钮 -->
    <div v-if="showFontSwitcher" class="char-prompt__fonts" role="tablist" aria-label="字体切换">
      <button
        v-for="f in CHAR_FONTS"
        :key="f.id"
        type="button"
        role="tab"
        class="char-prompt__font"
        :class="{ 'is-active': f.id === fontId }"
        :title="f.tip"
        :aria-selected="f.id === fontId"
        :style="{ fontFamily: f.stack }"
        @click="onSelect(f.id)"
      >
        {{ f.name }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  CHAR_FONTS,
  CHAR_FONTS_BY_ID,
  DEFAULT_CHAR_FONT_ID,
  type CharFont,
} from '@/utils/charFonts'

const props = withDefaults(
  defineProps<{
    char?: string
    pinyin?: string
    strokeCount?: number
    difficulty?: number
    showMeta?: boolean
    showFontSwitcher?: boolean
    size?: 'default' | 'large'
    fontId?: string
  }>(),
  { showMeta: true, showFontSwitcher: true, size: 'default', fontId: DEFAULT_CHAR_FONT_ID }
)

const emit = defineEmits<{
  'update:fontId': [value: string]
  refresh: []
}>()

const activeFont = computed<CharFont>(
  () => CHAR_FONTS_BY_ID[props.fontId] ?? CHAR_FONTS_BY_ID[DEFAULT_CHAR_FONT_ID]
)

function onSelect(id: string) {
  if (id === props.fontId) {
    // 再次点同一字体 → 触发"换一个"语义
    emit('refresh')
    return
  }
  emit('update:fontId', id)
}
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
    font-family: inherit;
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

  /* 字体切换行（≥ 5 种字体示例） */
  &__fonts {
    margin-top: $spacing-sm;
    display: grid;
    grid-template-columns: repeat(4, minmax(56px, 1fr));
    gap: 6px;
    width: 100%;
    @media (max-width: 480px) {
      grid-template-columns: repeat(3, 1fr);
    }
  }
  &__font {
    appearance: none;
    border: 1px solid $border-base;
    background: $bg-elevated;
    color: $text-regular;
    padding: 6px 4px;
    border-radius: $radius-sm;
    cursor: pointer;
    font-size: 16px;
    line-height: 1.1;
    transition: all 0.2s ease;
    user-select: none;
    min-height: 32px;
    text-align: center;

    &:hover {
      border-color: $color-primary;
      background: $bg-muted;
      transform: translateY(-1px);
    }
    &:active {
      transform: translateY(0);
    }
    &.is-active {
      border-color: $color-primary;
      background: $color-primary;
      color: $text-inverse;
      box-shadow: 0 2px 6px rgba(13, 148, 136, 0.25);
    }
    &:focus-visible {
      outline: 2px solid $color-primary-light;
      outline-offset: 2px;
    }
  }
}
</style>

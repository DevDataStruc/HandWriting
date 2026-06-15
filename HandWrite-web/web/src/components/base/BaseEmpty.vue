<template>
  <div class="base-empty" :style="{ minHeight: `${minHeight}px` }">
    <div class="base-empty__image">
      <el-icon :size="80" color="#CCFBF1">
        <component :is="iconComponent" />
      </el-icon>
    </div>
    <div class="base-empty__title">{{ title }}</div>
    <div v-if="description" class="base-empty__desc">{{ description }}</div>
    <div v-if="$slots.action" class="base-empty__action">
      <slot name="action" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { DocumentRemove, Picture, Search, Folder } from '@element-plus/icons-vue'

const props = withDefaults(
  defineProps<{
    title?: string
    description?: string
    icon?: 'doc' | 'image' | 'search' | 'folder'
    minHeight?: number
  }>(),
  { title: '暂无数据', description: '', icon: 'doc', minHeight: 200 }
)

const iconComponent = computed(() => {
  switch (props.icon) {
    case 'image':
      return Picture
    case 'search':
      return Search
    case 'folder':
      return Folder
    default:
      return DocumentRemove
  }
})
</script>

<style lang="scss" scoped>
.base-empty {
  @include flex-center;
  flex-direction: column;
  width: 100%;
  padding: $spacing-2xl;
  text-align: center;

  &__image {
    margin-bottom: $spacing-md;
  }
  &__title {
    font-size: $font-size-md;
    color: $text-primary;
    margin-bottom: $spacing-xs;
  }
  &__desc {
    font-size: $font-size-sm;
    color: $text-secondary;
    margin-bottom: $spacing-md;
  }
}
</style>

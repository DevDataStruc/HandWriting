<template>
  <el-breadcrumb separator="/" class="app-breadcrumb">
    <el-breadcrumb-item v-for="(item, i) in items" :key="i" :to="item.to">
      {{ item.title }}
    </el-breadcrumb-item>
  </el-breadcrumb>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, type RouteLocationMatched } from 'vue-router'

const route = useRoute()

const items = computed(() => {
  const matched = route.matched.filter((m) => m.meta.title)
  return matched.map((m: RouteLocationMatched) => ({
    title: m.meta.title as string,
    to: m.path || '/',
  }))
})
</script>

<style lang="scss" scoped>
.app-breadcrumb {
  font-size: $font-size-sm;
  color: $text-secondary;
  margin-bottom: $spacing-md;

  :deep(.el-breadcrumb__inner) {
    color: $text-secondary;
    font-weight: normal;
  }
  :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
    color: $text-primary;
    font-weight: 600;
  }
}
</style>

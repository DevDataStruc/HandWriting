<template>
  <el-config-provider :locale="elementLocale">
    <router-view />
  </el-config-provider>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import enUs from 'element-plus/es/locale/lang/en'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()

const elementLocale = computed(() => (appStore.locale === 'zh-CN' ? zhCn : enUs))
</script>

<style lang="scss">
@use '@/styles/index.scss';
</style>

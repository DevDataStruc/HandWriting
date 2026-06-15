<template>
  <div class="default-layout">
    <AppHeader />
    <main class="default-layout__main">
      <router-view v-slot="{ Component, route }">
        <transition name="route-fade" mode="out-in">
          <keep-alive :include="keepAliveNames">
            <component :is="Component" :key="route.fullPath" />
          </keep-alive>
        </transition>
      </router-view>
    </main>
    <AppFooter />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import AppHeader from './components/AppHeader.vue'
import AppFooter from './components/AppFooter.vue'

const keepAliveNames = computed<string[]>(() => ['Home', 'MySamples'])
</script>

<style lang="scss" scoped>
.default-layout {
  @include flex-column;
  min-height: 100vh;
  background: $bg-base;

  &__main {
    flex: 1;
    @include flex-column;
  }
}
</style>

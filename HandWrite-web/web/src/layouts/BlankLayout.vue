<template>
  <div class="blank-layout">
    <div class="decorations">
      <div class="dec dec--1"></div>
      <div class="dec dec--2"></div>
      <div class="dec dec--3"></div>
    </div>
    <router-view v-slot="{ Component, route }">
      <transition name="route-fade" mode="out-in">
        <component :is="Component" :key="route.fullPath" />
      </transition>
    </router-view>
  </div>
</template>

<script setup lang="ts"></script>

<style lang="scss" scoped>
.blank-layout {
  position: relative;
  min-height: 100vh;
  @include flex-center;
  background: linear-gradient(135deg, $bg-base 0%, $bg-muted 100%);
  overflow: hidden;
}

.decorations {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}

.dec {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
  animation: float 12s ease-in-out infinite;

  &--1 {
    width: 320px;
    height: 320px;
    top: -100px;
    left: -80px;
    background: $color-primary-lighter;
  }
  &--2 {
    width: 360px;
    height: 360px;
    bottom: -120px;
    right: -100px;
    background: $color-cta;
    animation-delay: 4s;
  }
  &--3 {
    width: 240px;
    height: 240px;
    bottom: 30%;
    right: 25%;
    background: $color-primary-light;
    animation-delay: 8s;
  }
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) scale(1);
  }
  50% {
    transform: translateY(-30px) scale(1.05);
  }
}

:deep(.auth-card) {
  position: relative;
  z-index: 1;
}
</style>

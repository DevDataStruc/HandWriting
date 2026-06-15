<template>
  <footer class="app-footer">
    <div class="hw-container app-footer__inner">
      <span class="copyright">© 2026 手写体采集管理系统 · Powered by Vue 3 + Vite</span>
      <div class="links">
        <a href="/about">关于我们</a>
        <a href="/notices">公告</a>
        <a href="https://github.com" target="_blank" rel="noreferrer">GitHub</a>
      </div>
    </div>
  </footer>
</template>

<script setup lang="ts"></script>

<style lang="scss" scoped>
.app-footer {
  height: $footer-height;
  background: $bg-elevated;
  border-top: 1px solid $border-base;
  color: $text-secondary;
  font-size: $font-size-sm;

  &__inner {
    height: 100%;
    @include flex-between;
  }
}

.links {
  display: flex;
  gap: $spacing-md;
  a {
    color: $text-secondary;
    &:hover {
      color: $color-primary;
    }
  }
}

.copyright {
  @include ellipsis;
}
</style>

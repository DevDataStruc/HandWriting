<template>
  <div class="error-page">
    <div class="error-content">
      <div class="code">404</div>
      <h1>页面不存在</h1>
      <p>你访问的页面已被删除或暂时无法访问。</p>
      <div class="actions">
        <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts"></script>

<style lang="scss" scoped>
.error-page {
  @include flex-center;
  min-height: 100vh;
  background: $bg-base;
}
.error-content {
  text-align: center;
  .code {
    font-size: 120px;
    font-weight: 700;
    @include text-gradient;
    line-height: 1;
  }
  h1 {
    font-size: 24px;
    color: $text-primary;
    margin: $spacing-md 0;
  }
  p {
    color: $text-secondary;
    margin-bottom: $spacing-lg;
  }
  .actions {
    @include flex-center;
    gap: $spacing-md;
  }
}
</style>

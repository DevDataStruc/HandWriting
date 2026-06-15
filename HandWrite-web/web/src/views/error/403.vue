<template>
  <div class="error-page">
    <div class="error-content">
      <div class="code">403</div>
      <h1>无访问权限</h1>
      <p>你的账号没有访问此页面的权限，请联系管理员。</p>
      <div class="actions">
        <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
        <el-button @click="$router.back()">返回上一页</el-button>
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
    @include text-gradient($from: $color-cta, $to: $color-cta-light);
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

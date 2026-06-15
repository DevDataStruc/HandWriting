<template>
  <div class="hw-page about-view">
    <div class="hw-container">
      <h1 class="hw-page-title">关于我们</h1>
      <BaseCard class="about-card">
        <h2>项目简介</h2>
        <p>
          手写体采集管理系统是一个面向 AI 训练数据生产的开源项目，
          旨在通过众包方式高效收集高质量的中文手写体样本。
        </p>
        <h2>技术架构</h2>
        <ul class="tech-list">
          <li><strong>前端：</strong>Vue 3.4 + Vite 5 + TypeScript 5 + Pinia 2 + Element Plus 2</li>
          <li><strong>后端：</strong>Spring Boot + MySQL + 对象存储 (OSS)</li>
          <li><strong>鉴权：</strong>JWT + RBAC (USER / AUDITOR / ADMIN)</li>
          <li><strong>可视化：</strong>ECharts 5</li>
          <li><strong>手写还原：</strong>perfect-freehand + signature_pad</li>
        </ul>
        <h2>开源协议</h2>
        <p>本项目基于 MIT 协议开源，欢迎社区贡献。</p>
      </BaseCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import BaseCard from '@/components/base/BaseCard.vue'
</script>

<style lang="scss" scoped>
.about-view {
  h2 {
    font-size: $font-size-lg;
    color: $color-primary;
    margin: $spacing-lg 0 $spacing-sm;
  }
  p {
    color: $text-regular;
    line-height: 1.8;
    margin-bottom: $spacing-sm;
  }
  .tech-list {
    list-style: disc;
    padding-left: $spacing-lg;
    color: $text-regular;
    line-height: 1.8;
  }
}
.about-card {
  max-width: 800px;
  margin: 0 auto;
}
</style>

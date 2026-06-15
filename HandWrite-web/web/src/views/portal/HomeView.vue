<template>
  <div class="home-view">
    <section class="hero">
      <div class="hero__bg">
        <div class="blob blob--1" />
        <div class="blob blob--2" />
        <div class="blob blob--3" />
      </div>
      <div class="hw-container hero__inner">
        <div class="hero__content anim-slide-up">
          <el-tag effect="dark" round class="hero__tag">汉字 · 手写 · 数据</el-tag>
          <h1 class="hero__title">
            为 AI 训练<br />
            贡献你的<span class="text-gradient">汉字笔迹</span>
          </h1>
          <p class="hero__desc">
            基于 Vue 3 + Vite + TypeScript 构建的现代化样本采集系统。<br />
            支持压感笔触、田字格提示、实时笔锋还原。
          </p>
          <div class="hero__actions">
            <BaseButton type="cta" size="large" @click="$router.push({ name: 'Collect' })">
              <el-icon><EditPen /></el-icon>
              <span>开始书写</span>
            </BaseButton>
            <BaseButton type="secondary" size="large" @click="$router.push({ name: 'About' })">
              <span>了解项目</span>
            </BaseButton>
          </div>
          <div class="hero__stats">
            <div class="stat">
              <div class="stat__value">{{ stats.samples }}</div>
              <div class="stat__label">累计样本</div>
            </div>
            <div class="stat">
              <div class="stat__value">{{ stats.users }}</div>
              <div class="stat__label">参与用户</div>
            </div>
            <div class="stat">
              <div class="stat__value">{{ stats.chars }}</div>
              <div class="stat__label">覆盖字符</div>
            </div>
          </div>
        </div>
        <div class="hero__visual anim-fade-in">
          <BaseCard class="visual-card" hoverable>
            <div class="visual-grid">
              <span v-for="(c, i) in demoChars" :key="i" class="demo-char">{{ c }}</span>
            </div>
          </BaseCard>
        </div>
      </div>
    </section>

    <section class="features">
      <div class="hw-container">
        <h2 class="section-title">核心特性</h2>
        <div class="features__grid">
          <BaseCard
            v-for="(f, i) in features"
            :key="i"
            hoverable
            class="feature-card"
          >
            <div class="feature-icon" :style="{ background: f.bg }">
              <el-icon :size="24" :color="f.color"><component :is="f.icon" /></el-icon>
            </div>
            <h3 class="feature-title">{{ f.title }}</h3>
            <p class="feature-desc">{{ f.desc }}</p>
          </BaseCard>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseCard from '@/components/base/BaseCard.vue'

const stats = ref({ samples: '12,580', users: '326', chars: '3,755' })
const demoChars = '永字八法楷行草书笔墨纸砚诗酒花月'.split('')

const features = [
  {
    icon: 'EditPen',
    title: '笔锋还原',
    desc: '集成 perfect-freehand，还原真实手写笔触与压力变化。',
    color: '#0D9488',
    bg: 'rgba(13, 148, 136, 0.1)',
  },
  {
    icon: 'PictureFilled',
    title: '田字格提示',
    desc: '内置田字格参考线，帮助书写者保持字形规范。',
    color: '#F97316',
    bg: 'rgba(249, 115, 22, 0.1)',
  },
  {
    icon: 'Upload',
    title: '断点续传',
    desc: '支持大文件分片上传与失败重试，传输稳定可靠。',
    color: '#3B82F6',
    bg: 'rgba(59, 130, 246, 0.1)',
  },
  {
    icon: 'DataAnalysis',
    title: '数据可视化',
    desc: 'ECharts 驱动的后台看板，实时掌握采集与审核进度。',
    color: '#10B981',
    bg: 'rgba(16, 185, 129, 0.1)',
  },
  {
    icon: 'Lock',
    title: 'JWT 鉴权',
    desc: '基于 RBAC 的细粒度权限控制，三角色分权分责。',
    color: '#8B5CF6',
    bg: 'rgba(139, 92, 246, 0.1)',
  },
  {
    icon: 'Cellphone',
    title: '多端适配',
    desc: '响应式布局支持 PC / 平板 / 手机，触屏压感均可用。',
    color: '#EC4899',
    bg: 'rgba(236, 72, 153, 0.1)',
  },
]
</script>

<style lang="scss" scoped>
.home-view {
  width: 100%;
}

.hero {
  position: relative;
  padding: $spacing-3xl 0;
  overflow: hidden;
  background: linear-gradient(180deg, $bg-muted 0%, $bg-base 100%);

  &__bg {
    position: absolute;
    inset: 0;
    pointer-events: none;
  }

  .blob {
    position: absolute;
    border-radius: 50%;
    filter: blur(80px);
    opacity: 0.5;
    animation: float 14s ease-in-out infinite;
  }
  .blob--1 {
    width: 380px;
    height: 380px;
    top: -120px;
    left: -100px;
    background: $color-primary-lighter;
  }
  .blob--2 {
    width: 320px;
    height: 320px;
    bottom: -100px;
    right: -80px;
    background: $color-cta;
    opacity: 0.35;
    animation-delay: 4s;
  }
  .blob--3 {
    width: 240px;
    height: 240px;
    top: 40%;
    right: 30%;
    background: $color-primary-light;
    animation-delay: 8s;
  }

  &__inner {
    position: relative;
    @include flex-between;
    gap: $spacing-2xl;
    flex-wrap: wrap;
  }

  &__content {
    flex: 1;
    min-width: 320px;
  }

  &__tag {
    background: linear-gradient(135deg, $color-primary, $color-primary-light);
    border: none;
    margin-bottom: $spacing-md;
  }

  &__title {
    font-size: 48px;
    line-height: 1.2;
    font-weight: 700;
    color: $text-primary;
    margin-bottom: $spacing-md;

    .text-gradient {
      @include text-gradient;
    }

    @include responsive(md) {
      font-size: 32px;
    }
  }

  &__desc {
    font-size: $font-size-md;
    color: $text-secondary;
    line-height: 1.7;
    margin-bottom: $spacing-lg;
  }

  &__actions {
    @include flex-start;
    gap: $spacing-md;
    margin-bottom: $spacing-2xl;
  }

  &__stats {
    display: flex;
    gap: $spacing-xl;
    flex-wrap: wrap;
  }

  &__visual {
    flex: 1;
    min-width: 320px;
  }
}

.stat {
  &__value {
    font-size: 28px;
    font-weight: 700;
    @include text-gradient;
  }
  &__label {
    font-size: $font-size-sm;
    color: $text-secondary;
    margin-top: 2px;
  }
}

.visual-card {
  max-width: 420px;
  margin-left: auto;
}

.visual-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: $spacing-sm;
}

.demo-char {
  @include flex-center;
  aspect-ratio: 1;
  font-family: $font-family-cn;
  font-size: 32px;
  font-weight: 600;
  color: $color-primary-dark;
  background: $bg-muted;
  border-radius: $radius-md;
  transition: all $transition-fast;

  &:hover {
    background: $color-primary;
    color: $text-inverse;
    transform: scale(1.05);
  }
}

.features {
  padding: $spacing-3xl 0;

  .section-title {
    font-size: 32px;
    text-align: center;
    margin-bottom: $spacing-xl;
    @include text-gradient;
  }
}

.features__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: $spacing-md;
}

.feature-card {
  .feature-icon {
    @include flex-center;
    width: 48px;
    height: 48px;
    border-radius: $radius-md;
    margin-bottom: $spacing-sm;
  }
  .feature-title {
    font-size: $font-size-md;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: $spacing-xs;
  }
  .feature-desc {
    font-size: $font-size-sm;
    color: $text-secondary;
    line-height: 1.6;
  }
}

@keyframes float {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  50% {
    transform: translate(20px, -20px) scale(1.05);
  }
}
</style>

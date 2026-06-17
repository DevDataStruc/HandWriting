<template>
  <div class="auth-page">
    <BaseCard class="auth-card" hoverable>
      <div class="auth-card__header">
        <h1>找回密码</h1>
        <p>如忘记密码，请联系系统管理员重置</p>
      </div>
      <el-alert
        type="info"
        show-icon
        :closable="false"
        title="API.md 暂未定义该接口"
        description="当前 API 规范未包含找回密码接口。如需重置密码，请联系系统管理员，或后续通过邮件验证码 / 短信验证码实现。"
      />
      <div class="auth-card__footer">
        想起来了？
        <router-link :to="{ name: 'Login' }">返回登录</router-link>
      </div>
    </BaseCard>
  </div>
</template>

<script setup lang="ts">
import BaseCard from '@/components/base/BaseCard.vue'
</script>

<style lang="scss" scoped>
.auth-page {
  @include flex-center;
  min-height: 100vh;
  padding: $spacing-md;
}
.auth-card {
  width: 100%;
  max-width: 460px;
  border-radius: $radius-2xl;
  padding: $spacing-xl;
  &__header {
    text-align: center;
    margin-bottom: $spacing-lg;
    h1 {
      font-size: 24px;
      color: $text-primary;
      margin-bottom: $spacing-xs;
    }
    p {
      color: $text-secondary;
      font-size: $font-size-sm;
    }
  }
  &__footer {
    text-align: center;
    margin-top: $spacing-md;
    font-size: $font-size-sm;
    color: $text-secondary;
    a {
      color: $color-primary;
      font-weight: 500;
    }
  }
}
</style>

<template>
  <div class="auth-page">
    <!-- 统一卡片：brand(左) + form(右) 同处一个白底卡片 -->
    <div class="auth-card login-view">
      <!-- 左侧：品牌区（卡片内左半） -->
      <div class="auth-card__brand">
        <div class="brand-inner">
          <div class="brand-logo">
            <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect x="6" y="6" width="36" height="36" rx="10" fill="url(#brand-grad)" />
              <path
                d="M14 32 L20 18 L24 26 L28 14 L34 32"
                stroke="white"
                stroke-width="2.5"
                stroke-linecap="round"
                stroke-linejoin="round"
                fill="none"
              />
              <defs>
                <linearGradient id="brand-grad" x1="0" y1="0" x2="48" y2="48">
                  <stop offset="0%" stop-color="#14B8A6" />
                  <stop offset="100%" stop-color="#0F766E" />
                </linearGradient>
              </defs>
            </svg>
          </div>
          <h2 class="brand-title">欢迎回来！</h2>
          <p class="brand-subtitle">登录以继续你的手写采集之旅</p>
          <ul class="brand-features">
            <li>
              <el-icon><CircleCheckFilled /></el-icon>
              <span>标准化字符样本管理</span>
            </li>
            <li>
              <el-icon><CircleCheckFilled /></el-icon>
              <span>直观的手写画板与笔顺回放</span>
            </li>
            <li>
              <el-icon><CircleCheckFilled /></el-icon>
              <span>高效的审核与统计工作流</span>
            </li>
          </ul>
        </div>
      </div>

      <!-- 右侧：表单区（卡片内右半） -->
      <div class="auth-card__form">
        <div class="form-inner">
          <div class="form-header">
            <h1>登录账号</h1>
            <p>输入你的账号信息</p>
          </div>

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-position="top"
            size="large"
            @submit.prevent="handleSubmit"
            @keyup.enter="handleSubmit"
          >
            <el-form-item label="用户名" prop="username">
              <BaseInput
                v-model="form.username"
                placeholder="请输入用户名"
                :prefix-icon="'User'"
                clearable
                autocomplete="username"
              />
            </el-form-item>

            <el-form-item label="密码" prop="password">
              <BaseInput
                v-model="form.password"
                type="password"
                placeholder="请输入密码"
                :prefix-icon="'Lock'"
                show-password
                autocomplete="current-password"
              />
            </el-form-item>

            <el-form-item label="验证码" prop="captchaCode">
              <div class="captcha-row">
                <BaseInput
                  v-model="form.captchaCode"
                  placeholder="请输入验证码"
                  :maxlength="6"
                  autocomplete="one-time-code"
                />
                <div
                  class="captcha-img"
                  :class="{ 'is-loading': captchaLoading }"
                  :title="captchaLoading ? '加载中…' : '点击刷新'"
                  @click="refreshCaptcha"
                >
                  <img v-if="captchaData" :src="captchaData" alt="验证码" />
                  <span v-else class="captcha-placeholder">
                    <el-icon class="is-loading"><Loading /></el-icon>
                    加载中
                  </span>
                </div>
              </div>
            </el-form-item>

            <div class="form-actions">
              <el-checkbox v-model="remember">记住我 7 天</el-checkbox>
              <router-link class="auth-link" :to="{ name: 'ForgotPassword' }">
                忘记密码？
              </router-link>
            </div>

            <BaseButton
              class="form-submit"
              type="primary"
              :loading="loading"
              block
              size="large"
              native-type="submit"
              @click="handleSubmit"
            >
              <template v-if="!loading">登录</template>
            </BaseButton>

            <div class="form-divider">
              <span>其他登录方式</span>
            </div>
            <div class="form-third-party">
              <el-tooltip content="暂未开放" placement="top">
                <el-button circle plain disabled>
                  <el-icon><ChatDotRound /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="暂未开放" placement="top">
                <el-button circle plain disabled>
                  <el-icon><Platform /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="暂未开放" placement="top">
                <el-button circle plain disabled>
                  <el-icon><Cellphone /></el-icon>
                </el-button>
              </el-tooltip>
            </div>

            <div class="form-footer">
              还没有账号？
              <router-link class="auth-link auth-link--strong" :to="{ name: 'Register' }">
                立即注册
              </router-link>
            </div>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  Cellphone,
  ChatDotRound,
  CircleCheckFilled,
  Loading,
  Platform,
} from '@element-plus/icons-vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import { getCaptcha, getTotpStatus } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const remember = ref(true)
const captchaData = ref<string>('')
const captchaKey = ref<string>('')
const captchaLoading = ref(false)

const form = reactive({
  username: '',
  password: '',
  captchaCode: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

async function refreshCaptcha() {
  captchaLoading.value = true
  try {
    const data = await getCaptcha()
    captchaKey.value = data.captchaKey
    captchaData.value = data.imageBase64
  } catch {
    // mock 模式可能未实现，忽略
  } finally {
    captchaLoading.value = false
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await userStore.login({
        username: form.username,
        password: form.password,
        captchaKey: captchaKey.value,
        captchaCode: form.captchaCode,
      })
      // 后台拉一次完整 profile（覆盖 LoginVO 中精简的用户信息）
      await userStore.fetchProfile().catch(() => null)
      ElMessage.success({
        message: '登录成功，欢迎回来！',
        duration: 2000,
      })
      // 首次登录提示：检查是否设置密码找回方式
      void promptRecoverySetup()
      const redirect = (route.query.redirect as string) || '/'
      router.replace(redirect)
    } catch (err) {
      console.warn('login error', err)
    } finally {
      loading.value = false
    }
  })
}

/**
 * 首次登录提示：未绑定 2FA 且未设置密保时，建议用户去个人中心配置。
 * 已在弹窗中给出"稍后设置"选项，避免阻塞流程。
 */
async function promptRecoverySetup() {
  try {
    const status = await getTotpStatus()
    if (status.bound) return
  } catch {
    return
  }
  // 仅在用户停留时间较短的提示窗，避免每次都强弹
  try {
    await ElMessageBox.confirm(
      '你尚未配置密码找回方式（动态口令 / 密保问题）。建议立即设置，避免忘记密码时无法找回。',
      '首次登录提示',
      {
        type: 'warning',
        confirmButtonText: '立即设置',
        cancelButtonText: '稍后',
      }
    )
    router.push({ name: 'TotpSetup' })
  } catch {
    /* cancel - 用户选择稍后 */
  }
}

onMounted(() => {
  refreshCaptcha()
})
</script>

<style lang="scss" scoped>
/* =========================================================================
   登录页布局（split-card 模式）
   - 整个 auth-card 是白底大卡片，brand 与 form 同处一个容器
   - 通过容器查询：auth-card 宽度 < 600px 时舍弃 brand，form 占满
   - < 380px 进一步压缩 captcha
   ======================================================================= */

.auth-page {
  width: 100%;
  min-height: 100vh;
  /* 用 grid 居中避免 flex item 高度坍缩（BlankLayout 也是 flex row，
     flex item 的 min-height:100vh 会被忽略导致 auth-page 高度坍缩） */
  display: grid;
  place-items: center;
  padding: $spacing-xl;
}

/* 统一白底大卡片 */
.auth-card {
  display: flex;
  width: 100%;
  max-width: 960px;
  min-height: 560px;
  background: $bg-elevated;
  border-radius: $radius-2xl;
  overflow: hidden;                     // 让 brand 渐变被卡片圆角裁剪
  box-shadow: $shadow-xl;

  /* 关键：声明为容器查询容器，宽度变化可触发内部 @container 规则 */
  container-type: inline-size;
  container-name: auth-card;

  // ============= 左侧：品牌区 =============
  &__brand {
    flex: 0 0 42%;
    position: relative;
    background:
      radial-gradient(circle at 20% 20%, rgba(94, 234, 212, 0.25) 0%, transparent 50%),
      radial-gradient(circle at 80% 80%, rgba(13, 148, 136, 0.18) 0%, transparent 60%),
      linear-gradient(135deg, $color-primary-darker 0%, $color-primary 100%);
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: $spacing-2xl;
    overflow: hidden;
    transition: padding 0.3s ease;
  }

  // ============= 右侧：表单区 =============
  &__form {
    flex: 1 1 58%;
    min-width: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: $spacing-xl $spacing-2xl;
    overflow-y: auto;
  }
}

.brand-inner {
  position: relative;
  z-index: 1;
  max-width: 320px;
  text-align: center;
}

.brand-logo {
  width: 64px;
  height: 64px;
  margin: 0 auto $spacing-lg;
  filter: drop-shadow(0 8px 24px rgba(0, 0, 0, 0.2));
  animation: hw-float 4s ease-in-out infinite;
}

.brand-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 $spacing-sm;
  letter-spacing: 1px;
  line-height: 1.2;
}

.brand-subtitle {
  font-size: $font-size-sm;
  opacity: 0.85;
  margin: 0 0 $spacing-xl;
  letter-spacing: 0.5px;
}

.brand-features {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
  text-align: left;

  li {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    font-size: $font-size-sm;
    opacity: 0.92;

    .el-icon {
      font-size: 18px;
      color: $color-primary-lighter;
      flex-shrink: 0;
    }
  }
}

@keyframes hw-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

/* ============= 表单内部 ============= */
.form-inner {
  width: 100%;
  max-width: 360px;
}

.form-header {
  text-align: center;
  margin-bottom: $spacing-lg;

  h1 {
    font-size: 24px;
    color: $text-primary;
    margin: 0 0 $spacing-xs;
    font-weight: 700;
    letter-spacing: 0.5px;
  }
  p {
    color: $text-secondary;
    font-size: $font-size-sm;
    margin: 0;
  }
}

.form-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: -4px 0 $spacing-md;
  font-size: $font-size-sm;
}

.form-divider {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin: $spacing-lg 0 $spacing-md;
  color: $text-placeholder;
  font-size: $font-size-xs;
  letter-spacing: 0.5px;

  &::before,
  &::after {
    content: '';
    flex: 1;
    height: 1px;
    background: $border-light;
  }
}

.form-footer {
  text-align: center;
  margin-top: $spacing-md;
  font-size: $font-size-sm;
  color: $text-secondary;
}

.form-submit {
  margin-top: $spacing-xs;
  font-weight: 600;
  letter-spacing: 1px;
}

.form-third-party {
  display: flex;
  justify-content: center;
  gap: $spacing-md;
}

.auth-link {
  color: $color-primary;
  transition: color $transition-fast;

  &:hover {
    color: $color-primary-dark;
  }

  &--strong {
    font-weight: 600;
    margin-left: 4px;
  }
}

/* ============= 验证码行 ============= */
.captcha-row {
  display: flex;
  gap: $spacing-sm;
  width: 100%;

  :deep(.el-input) {
    flex: 1 1 auto;
    min-width: 0;
  }
}

.captcha-img {
  flex: 0 0 120px;
  max-width: 120px;
  height: 44px;
  flex-shrink: 0;
  border: 1px solid $border-light;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
  background: $bg-muted;
  font-size: $font-size-xs;
  color: $text-secondary;
  transition: all $transition-fast;
  user-select: none;

  &:hover:not(.is-loading) {
    border-color: $color-primary-light;
    transform: translateY(-1px);
    box-shadow: 0 2px 6px -1px rgba(13, 148, 136, 0.2);
  }

  &.is-loading { cursor: wait; }

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
}

.captcha-placeholder {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

/* ====================================================================
   容器查询：响应"auth-card 自身宽度"
   - < 760px : brand 隐藏特性列表，压缩 padding
   - < 600px : 自动舍弃 brand，form 占满
   - < 480px : form padding 进一步压缩
   - < 380px : captcha 缩到 100px
   ==================================================================== */

@container auth-card (max-width: 760px) {
  .auth-card {
    &__brand {
      padding: $spacing-xl;
    }
  }
  .brand-features { display: none; }
  .brand-title { font-size: 24px; }
  .brand-subtitle { margin-bottom: 0; }
}

@container auth-card (max-width: 600px) {
  .auth-card {
    flex-direction: column;          // brand 仍然占位但极简化时，需要 stack
    min-height: auto;

    &__brand {
      display: none;                 // 关键：动态舍弃 brand，保留关键登录因素
    }
    &__form {
      flex: 1 1 auto;
      padding: $spacing-xl;
    }
  }
}

@container auth-card (max-width: 480px) {
  .auth-card__form {
    padding: $spacing-md;
  }
  .form-inner {
    max-width: 100%;
  }
}

@container auth-card (max-width: 380px) {
  .captcha-img {
    flex: 0 0 100px;
    max-width: 100px;
  }
  .form-header h1 { font-size: 20px; }
}
</style>

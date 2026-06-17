<template>
  <div class="auth-page login-view">
    <BaseCard class="auth-card" hoverable>
      <div class="auth-card__header">
        <h1>欢迎回来</h1>
        <p>登录以继续你的手写采集之旅</p>
      </div>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleSubmit"
      >
        <el-form-item label="用户名" prop="username">
          <BaseInput
            v-model="form.username"
            placeholder="请输入用户名"
            :prefix-icon="'User'"
            clearable
          />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <BaseInput
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="'Lock'"
            show-password
          />
        </el-form-item>
        <el-form-item label="验证码" prop="captchaCode">
          <div class="captcha-row">
            <BaseInput v-model="form.captchaCode" placeholder="请输入验证码" />
            <div class="captcha-img" @click="refreshCaptcha">
              <img v-if="captchaData" :src="captchaData" alt="验证码" />
              <span v-else>加载中</span>
            </div>
          </div>
        </el-form-item>
        <div class="auth-card__actions">
          <el-checkbox v-model="remember">记住我</el-checkbox>
          <router-link :to="{ name: 'ForgotPassword' }">忘记密码？</router-link>
        </div>
        <BaseButton
          type="primary"
          :loading="loading"
          block
          native-type="submit"
          @click="handleSubmit"
        >
          登录
        </BaseButton>
        <div class="auth-card__footer">
          还没有账号？
          <router-link :to="{ name: 'Register' }">立即注册</router-link>
        </div>
      </el-form>
    </BaseCard>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import BaseCard from '@/components/base/BaseCard.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import { getCaptcha } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const remember = ref(true)
const captchaData = ref<string>('')
const captchaKey = ref<string>('')

const form = reactive({
  username: '',
  password: '',
  captchaCode: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

async function refreshCaptcha() {
  try {
    const data = await getCaptcha()
    captchaKey.value = data.captchaKey
    captchaData.value = data.imageBase64
  } catch {
    // mock 模式可能未实现，忽略
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
      ElMessage.success('登录成功')
      const redirect = (route.query.redirect as string) || '/'
      router.replace(redirect)
    } catch (err) {
      console.warn('login error', err)
    } finally {
      loading.value = false
    }
  })
}

onMounted(() => {
  refreshCaptcha()
})
</script>

<style lang="scss" scoped>
.auth-page {
  @include flex-center;
  min-height: 100vh;
  padding: $spacing-md;
}

.auth-card {
  width: 100%;
  max-width: 420px;
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

  &__actions {
    @include flex-between;
    margin-bottom: $spacing-md;
    font-size: $font-size-sm;
    a {
      color: $color-primary;
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

.captcha-row {
  display: flex;
  gap: $spacing-sm;
  width: 100%;
}

.captcha-img {
  width: 110px;
  height: 40px;
  border: 1px solid $border-base;
  border-radius: $radius-md;
  @include flex-center;
  cursor: pointer;
  overflow: hidden;
  background: $bg-muted;
  font-size: $font-size-xs;
  color: $text-secondary;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}
</style>

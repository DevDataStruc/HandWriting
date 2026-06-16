<template>
  <div class="auth-page">
    <BaseCard class="auth-card" hoverable>
      <div class="auth-card__header">
        <h1>找回密码</h1>
        <p>输入注册邮箱，重置你的密码</p>
      </div>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleSubmit"
      >
        <el-form-item label="注册邮箱" prop="email">
          <BaseInput v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <BaseInput
            v-model="form.newPassword"
            type="password"
            show-password
            placeholder="6-32 位字符"
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <BaseInput
            v-model="form.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入密码"
          />
        </el-form-item>
        <BaseButton
          type="primary"
          :loading="loading"
          block
          native-type="submit"
          @click="handleSubmit"
        >
          提交重置
        </BaseButton>
        <div class="auth-card__footer">
          想起来了？
          <router-link :to="{ name: 'Login' }">返回登录</router-link>
        </div>
      </el-form>
    </BaseCard>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import BaseCard from '@/components/base/BaseCard.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import { forgotPassword } from '@/api/auth'
import { isEmail, isPassword } from '@/utils/validator'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  email: '',
  newPassword: '',
  confirmPassword: '',
})

const rules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { validator: (_r, v, cb) => (isEmail(v) ? cb() : cb(new Error('邮箱格式不正确'))) },
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { validator: (_r, v, cb) => (isPassword(v) ? cb() : cb(new Error('密码格式不正确'))) },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: (_r, v, cb) => (v === form.newPassword ? cb() : cb(new Error('两次密码不一致'))) },
  ],
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await forgotPassword({ ...form })
      ElMessage.success('密码已重置，请使用新密码登录')
      router.push({ name: 'Login' })
    } catch (err) {
      console.warn('forgot error', err)
    } finally {
      loading.value = false
    }
  })
}
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

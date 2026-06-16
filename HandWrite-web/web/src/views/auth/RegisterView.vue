<template>
  <div class="auth-page">
    <BaseCard class="auth-card" hoverable>
      <div class="auth-card__header">
        <h1>注册账号</h1>
        <p>加入我们，一起为 AI 训练贡献笔迹</p>
      </div>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleSubmit"
      >
        <el-form-item label="用户名" prop="username">
          <BaseInput v-model="form.username" placeholder="3-20 位字母/数字/下划线" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <BaseInput v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <BaseInput v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <BaseInput
            v-model="form.password"
            type="password"
            placeholder="6-32 位字符"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <BaseInput
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            show-password
          />
        </el-form-item>
        <BaseButton
          type="primary"
          :loading="loading"
          block
          native-type="submit"
          @click="handleSubmit"
        >
          注册
        </BaseButton>
        <div class="auth-card__footer">
          已有账号？
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
import { register } from '@/api/auth'
import { isEmail, isPassword, isUsername } from '@/utils/validator'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  nickname: '',
  email: '',
  password: '',
  confirmPassword: '',
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { validator: (_r, v, cb) => (isUsername(v) ? cb() : cb(new Error('用户名格式不正确'))) },
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { validator: (_r, v, cb) => (isEmail(v) ? cb() : cb(new Error('邮箱格式不正确'))) },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { validator: (_r, v, cb) => (isPassword(v) ? cb() : cb(new Error('密码格式不正确'))) },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_r, v, cb) => (v === form.password ? cb() : cb(new Error('两次密码不一致'))),
      trigger: 'blur',
    },
  ],
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await register({ ...form })
      ElMessage.success('注册成功，请登录')
      router.push({ name: 'Login' })
    } catch (err) {
      console.warn('register error', err)
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

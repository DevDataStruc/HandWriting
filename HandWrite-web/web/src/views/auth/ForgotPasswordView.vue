<template>
  <!--
    密码找回 - 多步骤向导
    样式仅作用于本文件（scoped），类名加 rp- 前缀避免与父布局冲突
  -->
  <div class="rp-page">
    <div class="rp-card">
      <!-- 左侧：品牌装饰 -->
      <div class="rp-card__brand">
        <div class="rp-brand__inner">
          <div class="rp-brand__icon">
            <el-icon :size="36"><Key /></el-icon>
          </div>
          <h2 class="rp-brand__title">找回密码</h2>
          <p class="rp-brand__sub">通过动态口令或密保问题重置登录密码</p>
          <ul class="rp-brand__steps">
            <li :class="{ active: step >= 1, done: step > 1 }">
              <span class="rp-step__no">1</span>
              <span>输入账号</span>
            </li>
            <li :class="{ active: step >= 2, done: step > 2 }">
              <span class="rp-step__no">2</span>
              <span>验证身份</span>
            </li>
            <li :class="{ active: step >= 3, done: step > 3 }">
              <span class="rp-step__no">3</span>
              <span>设置新密码</span>
            </li>
          </ul>
        </div>
      </div>

      <!-- 右侧：表单 -->
      <div class="rp-card__form">
        <div class="rp-form__inner">
          <!-- ============== Step 1: 输入账号 ============== -->
          <div v-if="step === 1" class="rp-step-pane">
            <h1 class="rp-form__title">第一步：输入账号</h1>
            <p class="rp-form__desc">输入需要找回密码的账号名</p>

            <el-form
              ref="formRef1"
              :model="form1"
              :rules="rules1"
              label-position="top"
              @submit.prevent="submitStep1"
            >
              <el-form-item label="账号" prop="username">
                <BaseInput
                  v-model="form1.username"
                  placeholder="请输入用户名"
                  :prefix-icon="'User'"
                  clearable
                  autocomplete="username"
                />
              </el-form-item>

              <BaseButton
                class="rp-form__submit"
                type="primary"
                block
                size="large"
                :loading="loading"
                native-type="submit"
                @click="submitStep1"
              >
                下一步
              </BaseButton>
            </el-form>
          </div>

          <!-- ============== Step 2: 验证身份 ============== -->
          <div v-else-if="step === 2" class="rp-step-pane">
            <h1 class="rp-form__title">第二步：验证身份</h1>
            <p class="rp-form__desc">
              账号 <strong>{{ startVO?.maskedUsername }}</strong>，请任选以下任一方式验证身份：
            </p>

            <!-- 选择方式：始终列出 TOTP + 密保问题（后端不再暴露账号是否启用哪些方式） -->
            <div class="rp-methods">
              <button
                v-for="m in availableMethods"
                :key="m.value"
                type="button"
                class="rp-method"
                :class="{ active: chosenMethod === m.value }"
                @click="onChooseMethod(m.value)"
              >
                <el-icon :size="22" class="rp-method__icon"><component :is="m.icon" /></el-icon>
                <span class="rp-method__label">{{ m.label }}</span>
                <el-icon v-if="chosenMethod === m.value" class="rp-method__check"
                  ><CircleCheckFilled
                /></el-icon>
              </button>
            </div>

            <!-- TOTP 验证码输入 -->
            <el-form
              v-if="chosenMethod === 'TOTP'"
              ref="formRef2a"
              :model="form2Totp"
              :rules="rules2Totp"
              label-position="top"
              class="rp-form-inline"
              @submit.prevent="submitStep2"
            >
              <el-tabs
                v-model="totpTab"
                class="rp-totp-tabs"
                @tab-change="onTotpTabChange"
              >
                <el-tab-pane label="动态口令" name="code">
                  <el-form-item label="6 位动态口令" prop="code">
                    <BaseInput
                      v-model="form2Totp.code"
                      placeholder="请输入认证器中的 6 位数字"
                      :maxlength="6"
                      autocomplete="one-time-code"
                    />
                  </el-form-item>
                </el-tab-pane>
                <el-tab-pane label="一次性恢复码" name="recovery">
                  <el-form-item label="恢复码" prop="recoveryCode">
                    <BaseInput
                      v-model="form2Totp.recoveryCode"
                      placeholder="形如 ABCDE-FGHJK"
                      :maxlength="11"
                    />
                  </el-form-item>
                  <p class="rp-tip">
                    恢复码是绑定 2FA 时一次性下发的 10 个备用码，每个仅可使用一次。
                  </p>
                </el-tab-pane>
              </el-tabs>
            </el-form>

            <!-- 密保问题答案 -->
            <el-form
              v-else-if="chosenMethod === 'SECURITY_QUESTION'"
              ref="formRef2b"
              :model="form2Sq"
              :rules="rules2Sq"
              label-position="top"
              class="rp-form-inline"
              @submit.prevent="submitStep2"
            >
              <el-form-item
                v-for="(q, idx) in form2Sq.questions"
                :key="idx"
                :label="`问题 ${idx + 1}：${q.questionText}`"
                :prop="`questions.${idx}.answer`"
                :rules="[
                  { required: true, message: '请输入答案', trigger: 'blur' },
                  { min: 1, max: 64, message: '答案 1-64 字符', trigger: 'blur' },
                ]"
              >
                <BaseInput
                  v-model="q.answer"
                  placeholder="请输入答案"
                  :maxlength="64"
                  show-password
                />
              </el-form-item>
              <p class="rp-tip">
                答案在保存时会去除首尾空格并忽略大小写，请尽量使用一致的写法。
              </p>
            </el-form>

            <div class="rp-form-actions">
              <BaseButton type="ghost" @click="step = 1">上一步</BaseButton>
              <BaseButton
                type="primary"
                :loading="loading"
                :disabled="!chosenMethod"
                @click="submitStep2"
              >
                验证身份
              </BaseButton>
            </div>
          </div>

          <!-- ============== Step 3: 重置密码 ============== -->
          <div v-else-if="step === 3" class="rp-step-pane">
            <h1 class="rp-form__title">第三步：设置新密码</h1>
            <p class="rp-form__desc">
              请设置新密码
              <span v-if="countdown > 0" class="rp-countdown">
                （{{ formatCountdown(countdown) }} 后失效）
              </span>
            </p>

            <el-form
              ref="formRef3"
              :model="form3"
              :rules="rules3"
              label-position="top"
              @submit.prevent="submitStep3"
            >
              <el-form-item label="新密码" prop="newPassword">
                <BaseInput
                  v-model="form3.newPassword"
                  type="password"
                  placeholder="6-64 位，且至少包含字母和数字"
                  :prefix-icon="'Lock'"
                  show-password
                />
              </el-form-item>
              <el-form-item label="确认新密码" prop="confirm">
                <BaseInput
                  v-model="form3.confirm"
                  type="password"
                  placeholder="再次输入新密码"
                  :prefix-icon="'Lock'"
                  show-password
                />
              </el-form-item>

              <BaseButton
                class="rp-form__submit"
                type="primary"
                block
                size="large"
                :loading="loading"
                :disabled="countdown <= 0"
                native-type="submit"
                @click="submitStep3"
              >
                重置密码
              </BaseButton>
            </el-form>
          </div>

          <!-- ============== Step 4: 完成 ============== -->
          <div v-else class="rp-step-pane rp-result">
            <el-result icon="success" title="密码重置成功" sub-title="请使用新密码重新登录">
              <template #icon>
                <div class="rp-result__icon"><el-icon :size="48"><CircleCheckFilled /></el-icon></div>
              </template>
              <template #extra>
                <BaseButton type="primary" @click="goLogin">前往登录</BaseButton>
              </template>
            </el-result>
          </div>

          <div v-if="step < 4" class="rp-form__footer">
            想起来了？
            <router-link :to="{ name: 'Login' }" class="rp-link">返回登录</router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  ChatLineRound,
  CircleCheckFilled,
  Key,
  Memo,
} from '@element-plus/icons-vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import {
  recoveryGetSecurityQuestions,
  recoveryReset,
  recoveryStart,
  recoveryVerifySecurity,
  recoveryVerifyTotp,
} from '@/api/auth'
import type { ForgotPasswordStartVO } from '@/api/contracts/auth'

const router = useRouter()

const step = ref<1 | 2 | 3 | 4>(1)
const loading = ref(false)
const startVO = ref<ForgotPasswordStartVO | null>(null)
const chosenMethod = ref<'TOTP' | 'SECURITY_QUESTION' | ''>('')

const formRef1 = ref<FormInstance>()
const formRef2a = ref<FormInstance>()
const formRef2b = ref<FormInstance>()
const formRef3 = ref<FormInstance>()

const form1 = reactive({ username: '' })
const form2Totp = reactive({ code: '', recoveryCode: '' })
const form2Sq = reactive({
  // 始终用 3 个空位（即使后端没回传，UI 上用占位）
  questions: [
    { questionIndex: 1, questionKey: '', questionText: '请等待后端返回问题…', answer: '' },
    { questionIndex: 2, questionKey: '', questionText: '请等待后端返回问题…', answer: '' },
    { questionIndex: 3, questionKey: '', questionText: '请等待后端返回问题…', answer: '' },
  ] as Array<{
    questionIndex: number
    questionKey: string
    questionText: string
    answer: string
  }>,
})
const form3 = reactive({ newPassword: '', confirm: '' })

const resetToken = ref('')
const totpTab = ref<'code' | 'recovery'>('code')

const countdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const rules1: FormRules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    {
      validator: (_r, v, cb) =>
        /^[A-Za-z0-9_.-]{3,64}$/.test(v) ? cb() : cb(new Error('账号为 3-64 位字母/数字/_-.')),
      trigger: 'blur',
    },
  ],
}
// TOTP 表单的 rules 改为 computed：只验证当前 tab 字段，
// 避免 el-tabs 默认 lazy=false 时两个 el-form-item 同时存在导致的"另一 tab 必填字段为空 -> 永远验证失败"
const rules2Totp = computed<FormRules>(() => {
  if (totpTab.value === 'code') {
    return {
      code: [
        { required: true, message: '请输入动态口令', trigger: 'blur' },
        { len: 6, message: '动态口令为 6 位数字', trigger: 'blur' },
      ],
    }
  }
  return {
    recoveryCode: [
      { required: true, message: '请输入恢复码', trigger: 'blur' },
      { min: 8, max: 32, message: '恢复码长度异常', trigger: 'blur' },
    ],
  }
})
const rules2Sq: FormRules = {}
const rules3: FormRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度 6-64', trigger: 'blur' },
    {
      validator: (_r, v, cb) =>
        /^(?=.*[A-Za-z])(?=.*\d).{6,64}$/.test(v) ? cb() : cb(new Error('密码需同时包含字母和数字')),
      trigger: 'blur',
    },
  ],
  confirm: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_r, v, cb) => (v === form3.newPassword ? cb() : cb(new Error('两次输入不一致'))),
      trigger: 'blur',
    },
  ],
}

const availableMethods = computed(() => {
  // 出于防用户枚举考虑：始终展示 TOTP + 密保问题两个选项，
  // 真正的"用户是否启用了该方式"在 verify 阶段才校验，
  // 无论选哪个，错误信息统一为"验证失败"
  return [
    { value: 'TOTP' as const, label: '动态口令（Authenticator）', icon: ChatLineRound },
    { value: 'SECURITY_QUESTION' as const, label: '密保问题', icon: Memo },
  ]
})

async function onChooseMethod(v: 'TOTP' | 'SECURITY_QUESTION') {
  chosenMethod.value = v
  // 选择密保问题时，去拉取真实问题正文（用户不存在时后端会返回 3 个占位）
  if (v === 'SECURITY_QUESTION' && startVO.value?.challengeId) {
    try {
      const qs = await recoveryGetSecurityQuestions(startVO.value.challengeId)
      form2Sq.questions = (qs || []).slice(0, 3).map((q, i) => ({
        questionIndex: q.questionIndex ?? i + 1,
        questionKey: q.questionKey || '',
        questionText: q.questionText || `你的第 ${i + 1} 道密保问题`,
        answer: '',
      }))
      // 不足 3 道则补齐占位
      while (form2Sq.questions.length < 3) {
        const idx = form2Sq.questions.length + 1
        form2Sq.questions.push({
          questionIndex: idx,
          questionKey: '',
          questionText: `你的第 ${idx} 道密保问题`,
          answer: '',
        })
      }
    } catch (err) {
      console.warn('fetch security questions failed', err)
    }
  }
}

/**
 * TOTP 切换 tab 时清掉旧校验状态,
 * 避免 el-form 残留的"另一 tab 字段未填"错误提示卡在界面上
 */
function onTotpTabChange() {
  if (formRef2a.value) {
    formRef2a.value.clearValidate()
  }
}

async function submitStep1() {
  if (!formRef1.value) return
  await formRef1.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const vo = await recoveryStart({ username: form1.username.trim() })
      startVO.value = vo
      // 出于防用户枚举考虑，密保问题正文不在 start 步骤返回，
      // 而是在用户选择 SECURITY_QUESTION 后由后端按需返回
      form2Sq.questions = [
        { questionIndex: 1, questionKey: '', questionText: '你的第 1 道密保问题', answer: '' },
        { questionIndex: 2, questionKey: '', questionText: '你的第 2 道密保问题', answer: '' },
        { questionIndex: 3, questionKey: '', questionText: '你的第 3 道密保问题', answer: '' },
      ]
      step.value = 2
    } catch (err) {
      console.warn('recovery start error', err)
    } finally {
      loading.value = false
    }
  })
}

async function submitStep2() {
  if (!chosenMethod.value) return
  if (chosenMethod.value === 'TOTP') {
    return submitStep2Totp()
  }
  return submitStep2Sq()
}

async function submitStep2Totp() {
  if (!formRef2a.value) return
  await formRef2a.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const useRecovery = totpTab.value === 'recovery'
      const vo = await recoveryVerifyTotp({
        challengeId: startVO.value!.challengeId,
        code: useRecovery ? undefined : form2Totp.code,
        recoveryCode: useRecovery ? form2Totp.recoveryCode : undefined,
        useRecoveryCode: useRecovery,
      })
      onTicketIssued(vo)
    } catch (err: any) {
      // request.ts 已经在 245 行兜底显示 ElMessage.error,
      // 这里做一下 console 详细日志,便于排查 (用户反馈"无反应"时能拿到栈)
      console.warn('verify totp error', err)
    } finally {
      loading.value = false
    }
  })
}

async function submitStep2Sq() {
  if (!formRef2b.value) return
  await formRef2b.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const answers = form2Sq.questions.map((q) => ({
        questionIndex: q.questionIndex,
        answer: q.answer,
      }))
      const vo = await recoveryVerifySecurity({
        challengeId: startVO.value!.challengeId,
        answers,
      })
      onTicketIssued(vo)
    } catch (err: any) {
      console.warn('verify security error', err)
    } finally {
      loading.value = false
    }
  })
}

function onTicketIssued(vo: { resetToken: string; expiresIn: number }) {
  resetToken.value = vo.resetToken
  countdown.value = vo.expiresIn
  step.value = 3
  startCountdown()
}

function startCountdown() {
  if (countdownTimer) clearInterval(countdownTimer)
  countdownTimer = setInterval(() => {
    if (countdown.value > 0) {
      countdown.value -= 1
    } else if (countdownTimer) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

function formatCountdown(sec: number): string {
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return `${m}:${s.toString().padStart(2, '0')}`
}

async function submitStep3() {
  if (!formRef3.value) return
  await formRef3.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await recoveryReset({
        resetToken: resetToken.value,
        newPassword: form3.newPassword,
      })
      ElMessage.success('密码重置成功，请重新登录')
      step.value = 4
    } catch (err) {
      console.warn('reset error', err)
    } finally {
      loading.value = false
    }
  })
}

function goLogin() {
  router.replace({ name: 'Login' })
}

onMounted(() => {
  // 防止 iOS 软键盘导致过渡
})

onBeforeUnmount(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})
</script>

<style lang="scss" scoped>
/* =========================================================================
   密码找回页（ForgotPassword）— 样式仅作用于本文件
   - 所有类以 rp- 前缀（recovery-password）避免与父布局 / 工作区冲突
   - 使用 scoped：data-v-xxx 哈希自动隔离
   ========================================================================= */

.rp-page {
  width: 100%;
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: $spacing-xl;
}

.rp-card {
  display: flex;
  width: 100%;
  max-width: 980px;
  min-height: 600px;
  background: $bg-elevated;
  border-radius: $radius-2xl;
  overflow: hidden;
  box-shadow: $shadow-xl;
  container-type: inline-size;
  container-name: rp-card;

  &__brand {
    flex: 0 0 38%;
    background: linear-gradient(135deg, $color-primary-darker 0%, $color-primary 60%, $color-primary-light 100%);
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: $spacing-2xl;
    position: relative;
    overflow: hidden;
  }

  &__form {
    flex: 1 1 62%;
    min-width: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: $spacing-xl $spacing-2xl;
    overflow-y: auto;
  }
}

.rp-brand__inner {
  position: relative;
  z-index: 1;
  max-width: 320px;
  text-align: center;
}

.rp-brand__icon {
  width: 72px;
  height: 72px;
  margin: 0 auto $spacing-lg;
  border-radius: $radius-xl;
  background: rgba(255, 255, 255, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.18);
}

.rp-brand__title {
  font-size: 26px;
  font-weight: 700;
  margin: 0 0 $spacing-sm;
  letter-spacing: 1px;
}

.rp-brand__sub {
  font-size: $font-size-sm;
  opacity: 0.85;
  margin: 0 0 $spacing-xl;
}

.rp-brand__steps {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
  text-align: left;

  li {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    font-size: $font-size-sm;
    opacity: 0.55;
    transition: opacity 0.25s ease;

    &.active {
      opacity: 1;
    }
    &.done {
      opacity: 0.85;
    }
  }
}

.rp-step__no {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  font-size: 12px;
  font-weight: 600;

  .active & {
    background: #fff;
    color: $color-primary-dark;
  }
}

/* ====== 表单 ====== */
.rp-form__inner {
  width: 100%;
  max-width: 380px;
}

.rp-form__title {
  font-size: 22px;
  color: $text-primary;
  font-weight: 700;
  margin: 0 0 $spacing-xs;
  text-align: center;
}

.rp-form__desc {
  font-size: $font-size-sm;
  color: $text-secondary;
  text-align: center;
  margin: 0 0 $spacing-lg;

  strong {
    color: $text-primary;
    font-weight: 600;
  }
}

.rp-form__submit {
  margin-top: $spacing-sm;
  font-weight: 600;
  letter-spacing: 1px;
}

.rp-form__footer {
  text-align: center;
  margin-top: $spacing-md;
  font-size: $font-size-sm;
  color: $text-secondary;
}

.rp-link {
  color: $color-primary;
  font-weight: 500;
  margin-left: 4px;
  &:hover {
    color: $color-primary-dark;
  }
}

.rp-form-actions {
  display: flex;
  gap: $spacing-sm;
  margin-top: $spacing-lg;

  > * {
    flex: 1;
  }
}

.rp-form-inline {
  margin-top: $spacing-md;
}

.rp-tip {
  font-size: $font-size-xs;
  color: $text-placeholder;
  margin: -$spacing-sm 0 $spacing-sm;
  line-height: 1.6;
}

.rp-countdown {
  color: $color-warning;
  font-weight: 500;
  margin-left: 6px;
}

/* ====== 选择方式卡 ====== */
.rp-methods {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
  margin-bottom: $spacing-md;
}

.rp-method {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-md;
  background: $bg-muted;
  border: 1px solid $border-base;
  border-radius: $radius-lg;
  cursor: pointer;
  text-align: left;
  font-size: $font-size-base;
  color: $text-primary;
  transition: all $transition-fast;

  &__icon {
    color: $color-primary;
    flex-shrink: 0;
  }

  &__label {
    flex: 1;
  }

  &__check {
    color: $color-success;
  }

  &:hover {
    border-color: $color-primary-light;
    transform: translateY(-1px);
    box-shadow: 0 4px 10px -2px rgba(13, 148, 136, 0.18);
  }

  &.active {
    background: $bg-overlay;
    border-color: $color-primary;
    box-shadow: 0 0 0 3px rgba(13, 148, 136, 0.12);
  }
}

.rp-totp-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: $spacing-sm;
  }
  :deep(.el-tabs__nav-wrap::after) {
    background: $border-light;
  }
}

/* ====== 完成态 ====== */
.rp-result {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: $spacing-lg 0;
}

.rp-result__icon {
  color: $color-success;
  margin-bottom: $spacing-sm;
}

/* ====== 容器查询：响应式断点 ====== */
@container rp-card (max-width: 760px) {
  .rp-card {
    &__brand {
      padding: $spacing-xl;
    }
  }
  .rp-brand__sub {
    display: none;
  }
  .rp-brand__steps {
    gap: $spacing-sm;
  }
}

@container rp-card (max-width: 600px) {
  .rp-card {
    flex-direction: column;
    min-height: auto;

    &__brand {
      display: none;
    }
    &__form {
      flex: 1 1 auto;
      padding: $spacing-xl;
    }
  }
  .rp-form__inner {
    max-width: 100%;
  }
}

@container rp-card (max-width: 380px) {
  .rp-form__title {
    font-size: 18px;
  }
  .rp-form-actions {
    flex-direction: column;
  }
}
</style>

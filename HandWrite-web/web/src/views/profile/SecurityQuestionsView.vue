<template>
  <!--
    密保问题设置
    - 样式仅作用于本文件（scoped），所有类加 sq- 前缀
  -->
  <div class="sq-page">
    <div class="sq-card">
      <div class="sq-header">
        <div class="sq-header__icon">
          <el-icon :size="24"><Lock /></el-icon>
        </div>
        <div class="sq-header__text">
          <h1 class="sq-header__title">密保问题</h1>
          <p class="sq-header__sub">
            设置 3 道密保问题，用于在忘记密码时验证身份。
            答案会去除首尾空格并忽略大小写后再保存，请尽量使用一致的写法。
          </p>
        </div>
      </div>

      <el-alert
        v-if="!isSet"
        type="info"
        :closable="false"
        show-icon
        class="sq-alert"
        title="你尚未设置密保"
        description="强烈建议设置 3 道密保问题作为密码找回的备用方式。"
      />

      <el-alert
        v-else
        type="success"
        :closable="false"
        show-icon
        class="sq-alert"
        :title="`已设置 ${existing.length} 道密保`"
        description="修改会覆盖原有密保；清空后无法使用密保方式找回密码。"
      />

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="onSubmit"
      >
        <div
          v-for="(q, idx) in form.questions"
          :key="idx"
          class="sq-item"
        >
          <div class="sq-item__no">{{ idx + 1 }}</div>
          <div class="sq-item__body">
            <el-form-item :label="`问题 ${idx + 1}`" :prop="`questions.${idx}.questionKey`">
              <el-select
                v-model="q.questionKey"
                placeholder="选择问题"
                @change="onPresetPick(idx, $event as string)"
                style="width: 100%"
              >
                <el-option
                  v-for="opt in presetOptions"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="自定义问题正文" :prop="`questions.${idx}.questionText`">
              <BaseInput
                v-model="q.questionText"
                placeholder="可基于上述预设改写，或直接自定义"
                :maxlength="255"
              />
            </el-form-item>

            <el-form-item label="答案" :prop="`questions.${idx}.answer`">
              <BaseInput
                v-model="q.answer"
                :type="revealMap[idx] ? 'text' : 'password'"
                placeholder="1-64 字符"
                :maxlength="64"
                show-password
              />
            </el-form-item>

            <div class="sq-item__actions">
              <el-checkbox
                :model-value="!!revealMap[idx]"
                @change="(v) => (revealMap[idx] = !!v)"
              >显示答案</el-checkbox>
            </div>
          </div>
        </div>

        <div class="sq-actions">
          <BaseButton
            type="primary"
            :loading="submitting"
            native-type="submit"
            @click="onSubmit"
          >
            保存密保
          </BaseButton>
          <BaseButton
            v-if="isSet"
            type="danger"
            plain
            :loading="clearing"
            @click="onClear"
          >
            清空密保
          </BaseButton>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Lock } from '@element-plus/icons-vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import {
  clearSecurityQuestions,
  listSecurityQuestions,
  setupSecurityQuestions,
} from '@/api/auth'
import type {
  SecurityQuestionItem,
  SecurityQuestionVO,
} from '@/api/contracts/auth'

const existing = ref<SecurityQuestionVO[]>([])
const isSet = ref(false)

const formRef = ref<FormInstance>()
const submitting = ref(false)
const clearing = ref(false)

const form = reactive<{ questions: SecurityQuestionItem[] }>({
  questions: [
    { questionKey: '', questionText: '', answer: '' },
    { questionKey: '', questionText: '', answer: '' },
    { questionKey: '', questionText: '', answer: '' },
  ],
})

const revealMap = reactive<Record<number, boolean>>({ 0: false, 1: false, 2: false })

// 预设问题（与后端 SecurityQuestionService.PRESET_QUESTIONS 保持一致）
const presetOptions = [
  { value: 'favoriteTeacher', label: '你小学时最喜欢的老师姓什么？' },
  { value: 'firstPet', label: '你的第一只宠物叫什么？' },
  { value: 'bornCity', label: '你出生在哪个城市？' },
  { value: 'motherName', label: '你母亲的姓名是？' },
  { value: 'firstSchool', label: '你就读的第一所学校名称是？' },
  { value: 'childhoodNickname', label: '你的童年昵称是？' },
  { value: 'favoriteMovie', label: '你最喜欢的一部电影是？' },
  { value: 'favoriteBook', label: '你最喜欢的一本书是？' },
]

const rules: FormRules = {
  'questions.*.questionKey': [
    { required: true, message: '请选择题库或自定义', trigger: 'change' },
  ],
  'questions.*.questionText': [
    { required: true, message: '请填写问题正文', trigger: 'blur' },
    { min: 2, max: 255, message: '问题 2-255 字符', trigger: 'blur' },
  ],
  'questions.*.answer': [
    { required: true, message: '请输入答案', trigger: 'blur' },
    { min: 1, max: 64, message: '答案 1-64 字符', trigger: 'blur' },
  ],
}

function onPresetPick(idx: number, key: string) {
  const opt = presetOptions.find((o) => o.value === key)
  if (opt) {
    form.questions[idx].questionText = opt.label
  }
}

async function refresh() {
  try {
    const list = await listSecurityQuestions()
    existing.value = list
    isSet.value = list.length >= 3
    if (isSet.value) {
      form.questions = list.map((q) => ({
        questionKey: q.questionKey,
        questionText: q.questionText,
        answer: '',
      }))
    }
  } catch (err) {
    console.warn('list sq error', err)
  }
}

async function onSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await setupSecurityQuestions({ questions: form.questions })
      ElMessage.success('密保问题已保存')
      await refresh()
      // 清空答案
      form.questions.forEach((q) => (q.answer = ''))
    } catch (err) {
      console.warn('setup sq error', err)
    } finally {
      submitting.value = false
    }
  })
}

async function onClear() {
  try {
    await ElMessageBox.confirm(
      '清空后无法使用密保方式找回密码，确认清空？',
      '清空密保',
      { type: 'warning', confirmButtonText: '确认清空', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  clearing.value = true
  try {
    await clearSecurityQuestions()
    ElMessage.success('已清空密保')
    form.questions = [
      { questionKey: '', questionText: '', answer: '' },
      { questionKey: '', questionText: '', answer: '' },
      { questionKey: '', questionText: '', answer: '' },
    ]
    isSet.value = false
  } catch (err) {
    console.warn('clear sq error', err)
  } finally {
    clearing.value = false
  }
}

onMounted(refresh)
</script>

<style lang="scss" scoped>
/* =========================================================================
   密保问题设置 — 样式仅作用于本文件
   - 所有类以 sq- 前缀（security-question）
   - scoped 隔离
   ========================================================================= */

.sq-page {
  width: 100%;
  padding: $spacing-lg 0;
}

.sq-card {
  width: 100%;
  max-width: 720px;
  margin: 0 auto;
  background: $bg-elevated;
  border-radius: $radius-2xl;
  padding: $spacing-xl $spacing-2xl;
  box-shadow: $shadow-md;
}

.sq-header {
  display: flex;
  align-items: flex-start;
  gap: $spacing-md;
  padding-bottom: $spacing-md;
  border-bottom: 1px solid $border-light;
  margin-bottom: $spacing-lg;

  &__icon {
    width: 48px;
    height: 48px;
    border-radius: $radius-lg;
    background: linear-gradient(135deg, $color-cta 0%, $color-cta-dark 100%);
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  &__title {
    font-size: 20px;
    font-weight: 700;
    margin: 0 0 4px;
    color: $text-primary;
  }

  &__sub {
    font-size: $font-size-sm;
    color: $text-secondary;
    margin: 0;
    line-height: 1.6;
  }
}

.sq-alert {
  margin-bottom: $spacing-md;
}

.sq-item {
  display: flex;
  gap: $spacing-md;
  padding: $spacing-md;
  background: $bg-muted;
  border-radius: $radius-lg;
  margin-bottom: $spacing-md;

  &__no {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: $color-primary;
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 700;
    flex-shrink: 0;
  }

  &__body {
    flex: 1;
    min-width: 0;
  }

  &__actions {
    display: flex;
    justify-content: flex-end;
    margin-top: -$spacing-sm;
  }
}

.sq-actions {
  display: flex;
  gap: $spacing-sm;
  margin-top: $spacing-md;
  padding-top: $spacing-md;
  border-top: 1px solid $border-light;
}
</style>

<template>
  <div class="di-root">
    <!-- 头部说明 + KPI 概览 -->
    <section class="di-header">
      <div class="di-header__text">
        <h2 class="di-header__title">
          <SvgIcon icon-name="dict" :size="20" class="di-header__icon" />
          <span>字符字典录入</span>
        </h2>
        <p class="di-header__sub">
          支持单条新增、批量粘贴、JSON 文本导入以及 txt / Word / Excel 文件解析，字符直接落入
          <code class="di-code di-code--blue">t_char_dict</code>
          数据库表，重复字符将被跳过。
        </p>
      </div>
      <div class="di-header__stats">
        <div class="di-pill di-pill--ok">
          <span class="di-pill__lbl">本次新增</span>
          <span class="di-pill__num">{{ lastResult?.inserted ?? 0 }}</span>
        </div>
        <div class="di-pill di-pill--warn">
          <span class="di-pill__lbl">跳过</span>
          <span class="di-pill__num">{{ lastResult?.skipped ?? 0 }}</span>
        </div>
        <div class="di-pill di-pill--err">
          <span class="di-pill__lbl">失败</span>
          <span class="di-pill__num">{{ lastResult?.failed ?? 0 }}</span>
        </div>
      </div>
    </section>

    <!-- Tab 切换 -->
    <div class="di-tabs">
      <div class="di-tabs__nav">
        <button
          v-for="t in tabs"
          :key="t.name"
          type="button"
          class="di-tabs__btn"
          :class="{ 'is-active': activeTab === t.name }"
          @click="activeTab = t.name"
        >
          <SvgIcon :icon-name="t.icon" :size="16" />
          <span>{{ t.label }}</span>
        </button>
      </div>

      <div class="di-tabs__panel">
        <!-- 1) 单条新增 -->
        <section v-show="activeTab === 'single'" class="di-panel">
          <div class="di-card">
            <header class="di-card__head">
              <h3 class="di-card__title">单条字符</h3>
              <p class="di-card__sub">适用于临时补录少量字符</p>
            </header>
            <div class="di-card__body">
              <form class="di-form" @submit.prevent="onSubmitSingle">
                <div class="di-form__row">
                  <label class="di-form__label">
                    <span class="di-form__required">*</span>字符
                  </label>
                  <div class="di-form__ctrl">
                    <input
                      v-model="singleForm.charValue"
                      class="di-input"
                      placeholder="例如：永"
                      maxlength="8"
                    />
                  </div>
                </div>
                <div class="di-form__row">
                  <label class="di-form__label">
                    <span class="di-form__required">*</span>分类
                  </label>
                  <div class="di-form__ctrl">
                    <select v-model="singleForm.category" class="di-select">
                      <option v-for="c in categoryOptions" :key="c" :value="c">
                        {{ c }}
                      </option>
                    </select>
                  </div>
                </div>
                <div class="di-form__row">
                  <label class="di-form__label">
                    <span class="di-form__required">*</span>难度
                  </label>
                  <div class="di-form__ctrl">
                    <div class="di-rate" @click="onRateClick($event, singleForm, 'difficulty')">
                      <span
                        v-for="n in 5"
                        :key="n"
                        class="di-rate__star"
                        :class="{ 'is-on': n <= singleForm.difficulty }"
                        >★</span
                      >
                    </div>
                  </div>
                </div>
                <div class="di-form__row">
                  <label class="di-form__label">启用</label>
                  <div class="di-form__ctrl di-form__ctrl--inline">
                    <button
                      type="button"
                      class="di-switch"
                      :class="{ 'is-on': singleForm.enabledBool }"
                      @click="singleForm.enabledBool = !singleForm.enabledBool"
                    >
                      <span class="di-switch__dot" />
                    </button>
                    <span class="di-form__hint">关闭后采集端不再推送</span>
                  </div>
                </div>
                <div class="di-form__row">
                  <label class="di-form__label">描述</label>
                  <div class="di-form__ctrl">
                    <textarea
                      v-model="singleForm.description"
                      class="di-textarea"
                      rows="2"
                      placeholder="可选：字符备注、读音、部首等"
                      maxlength="255"
                    />
                    <div class="di-form__counter">{{ singleForm.description.length }} / 255</div>
                  </div>
                </div>
                <div class="di-form__row">
                  <label class="di-form__label" />
                  <div class="di-form__ctrl di-form__ctrl--inline">
                    <button type="submit" class="di-btn di-btn--primary" :disabled="submitting">
                      <SvgIcon icon-name="check" :size="14" class="di-btn__ico" />
                      立即提交
                    </button>
                    <button type="button" class="di-btn" @click="resetSingle">重置</button>
                  </div>
                </div>
              </form>
            </div>
          </div>
        </section>

        <!-- 2) 批量新增 -->
        <section v-show="activeTab === 'batch'" class="di-panel">
          <div class="di-card">
            <header class="di-card__head">
              <h3 class="di-card__title">批量字符</h3>
              <p class="di-card__sub">每行一字符 / 多字符自动拆分；支持 # 注释行</p>
            </header>
            <div class="di-card__body">
              <form class="di-form" @submit.prevent="onSubmitBatch">
                <div class="di-form__row">
                  <label class="di-form__label">默认分类</label>
                  <div class="di-form__ctrl di-form__ctrl--inline">
                    <select v-model="batchForm.defaultCategory" class="di-select">
                      <option v-for="c in categoryOptions" :key="c" :value="c">
                        {{ c }}
                      </option>
                    </select>
                    <span class="di-form__hint">对所有未指定分类的字符生效</span>
                  </div>
                </div>
                <div class="di-form__row">
                  <label class="di-form__label">默认难度</label>
                  <div class="di-form__ctrl">
                    <div
                      class="di-rate"
                      @click="onRateClick($event, batchForm, 'defaultDifficulty')"
                    >
                      <span
                        v-for="n in 5"
                        :key="n"
                        class="di-rate__star"
                        :class="{ 'is-on': n <= batchForm.defaultDifficulty }"
                        >★</span
                      >
                    </div>
                  </div>
                </div>
                <div class="di-form__row">
                  <label class="di-form__label">
                    <span class="di-form__required">*</span>字符内容
                  </label>
                  <div class="di-form__ctrl">
                    <textarea
                      v-model="batchForm.text"
                      class="di-textarea di-textarea--mono"
                      rows="10"
                      :placeholder="batchPlaceholder"
                      maxlength="20000"
                    />
                    <div class="di-form__counter">{{ batchForm.text.length }} / 20000</div>
                    <div class="di-form__meta">
                      <span
                        >共解析出 <b>{{ previewFromText.length }}</b> 个字符</span
                      >
                      <button
                        v-if="batchForm.text"
                        type="button"
                        class="di-link"
                        @click="batchForm.text = ''"
                      >
                        清空
                      </button>
                    </div>
                  </div>
                </div>
                <div class="di-form__row">
                  <label class="di-form__label" />
                  <div class="di-form__ctrl di-form__ctrl--inline">
                    <button type="submit" class="di-btn di-btn--primary" :disabled="submitting">
                      <SvgIcon icon-name="check" :size="14" class="di-btn__ico" />
                      批量提交
                    </button>
                    <button type="button" class="di-btn" @click="loadSample">填充示例</button>
                  </div>
                </div>
              </form>
            </div>
          </div>
        </section>

        <!-- 3) JSON 导入 -->
        <section v-show="activeTab === 'json'" class="di-panel">
          <div class="di-card">
            <header class="di-card__head">
              <h3 class="di-card__title">JSON 文本导入</h3>
              <p class="di-card__sub">支持字符串数组或对象数组</p>
            </header>
            <div class="di-card__body">
              <form class="di-form" @submit.prevent="onSubmitJson">
                <div class="di-form__row">
                  <label class="di-form__label">默认分类</label>
                  <div class="di-form__ctrl">
                    <select v-model="jsonForm.defaultCategory" class="di-select">
                      <option v-for="c in categoryOptions" :key="c" :value="c">
                        {{ c }}
                      </option>
                    </select>
                  </div>
                </div>
                <div class="di-form__row">
                  <label class="di-form__label">
                    <span class="di-form__required">*</span>JSON 文本
                  </label>
                  <div class="di-form__ctrl">
                    <textarea
                      v-model="jsonForm.text"
                      class="di-textarea di-textarea--mono"
                      rows="10"
                      :placeholder="jsonPlaceholder"
                      maxlength="20000"
                    />
                    <div class="di-form__counter">{{ jsonForm.text.length }} / 20000</div>
                  </div>
                </div>
                <div class="di-form__row">
                  <label class="di-form__label">或上传文件</label>
                  <div class="di-form__ctrl">
                    <label class="di-filebtn">
                      <SvgIcon icon-name="import" :size="14" class="di-btn__ico" />
                      选择 .json 文件
                      <input
                        type="file"
                        class="di-filebtn__input"
                        accept=".json,application/json"
                        @change="onPickJsonFile"
                      />
                    </label>
                    <span v-if="jsonFileName" class="di-form__hint"
                      >已选择：{{ jsonFileName }}</span
                    >
                  </div>
                </div>
                <div class="di-form__row">
                  <label class="di-form__label" />
                  <div class="di-form__ctrl di-form__ctrl--inline">
                    <button type="submit" class="di-btn di-btn--primary" :disabled="submitting">
                      <SvgIcon icon-name="import" :size="14" class="di-btn__ico" />
                      导入
                    </button>
                    <button type="button" class="di-btn" @click="loadJsonSample">填充示例</button>
                  </div>
                </div>
              </form>
            </div>
          </div>
        </section>

        <!-- 4) 文件导入 -->
        <section v-show="activeTab === 'file'" class="di-panel">
          <div class="di-card">
            <header class="di-card__head">
              <h3 class="di-card__title">文件导入</h3>
              <p class="di-card__sub">支持 .txt / .docx / .xls / .xlsx / .json</p>
            </header>
            <div class="di-card__body">
              <form class="di-form" @submit.prevent="onSubmitFile">
                <div class="di-form__row">
                  <label class="di-form__label">默认分类</label>
                  <div class="di-form__ctrl">
                    <select v-model="fileForm.defaultCategory" class="di-select">
                      <option v-for="c in categoryOptions" :key="c" :value="c">
                        {{ c }}
                      </option>
                    </select>
                  </div>
                </div>
                <div class="di-form__row">
                  <label class="di-form__label">默认难度</label>
                  <div class="di-form__ctrl">
                    <div
                      class="di-rate"
                      @click="onRateClick($event, fileForm, 'defaultDifficulty')"
                    >
                      <span
                        v-for="n in 5"
                        :key="n"
                        class="di-rate__star"
                        :class="{ 'is-on': n <= fileForm.defaultDifficulty }"
                        >★</span
                      >
                    </div>
                  </div>
                </div>
                <div class="di-form__row">
                  <label class="di-form__label">
                    <span class="di-form__required">*</span>文件
                  </label>
                  <div class="di-form__ctrl">
                    <label class="di-filebtn">
                      <SvgIcon icon-name="upload" :size="14" class="di-btn__ico" />
                      选择文件
                      <input
                        type="file"
                        class="di-filebtn__input"
                        accept=".txt,.json,.xls,.xlsx,.docx"
                        @change="onPickFile"
                      />
                    </label>
                    <span v-if="fileName" class="di-form__hint">已选择：{{ fileName }}</span>
                    <button v-if="fileName" type="button" class="di-link" @click="onRemoveFile">
                      移除
                    </button>
                    <div class="di-form__hint di-form__hint--top">
                      单文件最大 10MB；Excel 需含列头
                      <code class="di-code di-code--blue"
                        >charValue / category / difficulty / description / enabled</code
                      >
                    </div>
                  </div>
                </div>
                <div class="di-form__row">
                  <label class="di-form__label" />
                  <div class="di-form__ctrl di-form__ctrl--inline">
                    <button
                      type="submit"
                      class="di-btn di-btn--primary"
                      :disabled="submitting || !fileRaw"
                    >
                      <SvgIcon icon-name="import" :size="14" class="di-btn__ico" />
                      上传并导入
                    </button>
                    <button
                      type="button"
                      class="di-btn"
                      :disabled="submitting || !fileRaw"
                      @click="onPreviewFile"
                    >
                      <SvgIcon icon-name="search" :size="14" class="di-btn__ico" />
                      预览前 20 条
                    </button>
                  </div>
                </div>

                <div v-if="previewRows.length" class="di-form__row di-form__row--block">
                  <label class="di-form__label">预览</label>
                  <div class="di-form__ctrl">
                    <div class="di-preview">
                      <div v-for="(p, i) in previewRows" :key="i" class="di-preview__cell">
                        <b>{{ p.charValue }}</b>
                        <span class="di-preview__cat">{{ p.category || '-' }}</span>
                      </div>
                      <div v-if="previewOverflow" class="di-preview__more">
                        仅展示前 {{ previewRows.length }} 条，共 {{ previewTotal }} 条
                      </div>
                    </div>
                  </div>
                </div>
              </form>
            </div>
          </div>
        </section>
      </div>
    </div>

    <!-- 最近一次导入结果 -->
    <div v-if="lastResult" class="di-card di-card--result">
      <header class="di-card__head">
        <h3 class="di-card__title">最近一次导入结果</h3>
      </header>
      <div class="di-card__body">
        <div class="di-alert" :class="`di-alert--${resultAlertType}`" role="alert">
          <span class="di-alert__title">{{ lastResult.message || '完成' }}</span>
          <div
            v-if="lastResult.failedSamples && lastResult.failedSamples.length"
            class="di-alert__sub"
          >
            失败样本（前 50）：
            <code class="di-code di-code--red">{{ lastResult.failedSamples.join(' / ') }}</code>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 字符字典录入（管理员）
 * - 单条 / 批量 / JSON / 文件 四种录入方式
 * - 样式完全自包含：不使用 SCSS 变量、mixin、全局 CSS 变量，也不继承 BaseCard / Element Plus 等父级样式
 * - 与后端 CharDictAdminController / CharDictFileController / CharDictService 联动
 */
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import SvgIcon from '@/components/base/SvgIcon.vue'
import {
  batchCreateChars,
  importFileChars,
  importJsonChars,
  previewImportFile,
  type CharDictImportResult,
  type CharDictItem,
} from '@/api/dict-admin'

defineOptions({ name: 'DictInputView' })

type Tab = 'single' | 'batch' | 'json' | 'file'

const tabs: { name: Tab; label: string; icon: string }[] = [
  { name: 'single', label: '单条新增', icon: 'text-input' },
  { name: 'batch', label: '批量新增', icon: 'batch' },
  { name: 'json', label: 'JSON 导入', icon: 'document' },
  { name: 'file', label: '文件导入', icon: 'upload' },
]

const activeTab = ref<Tab>('single')

/* ========================= 表单态 ========================= */
const categoryOptions = ['HANZI', 'DIGIT', 'LETTER', 'SYMBOL']

const singleForm = reactive({
  charValue: '',
  category: 'HANZI' as string,
  difficulty: 2,
  enabledBool: true,
  description: '',
})

const batchForm = reactive({
  defaultCategory: 'HANZI' as string,
  defaultDifficulty: 2,
  text: '',
})

const jsonForm = reactive({
  defaultCategory: 'HANZI' as string,
  text: '',
})

const fileForm = reactive<{
  defaultCategory: string
  defaultDifficulty: number
}>({
  defaultCategory: 'HANZI',
  defaultDifficulty: 2,
})

const submitting = ref(false)
const lastResult = ref<CharDictImportResult | null>(null)

/* 文件选择状态（使用原生 input，绕开 Element Plus 上传组件的样式依赖） */
const jsonFileName = ref('')
const fileName = ref('')
const fileRaw = ref<File | null>(null)

const resultAlertType = computed<'success' | 'warning' | 'error'>(() => {
  if (!lastResult.value) return 'success'
  if (lastResult.value.failed > 0 && lastResult.value.inserted === 0) return 'error'
  if (lastResult.value.failed > 0 || lastResult.value.skipped > 0) return 'warning'
  return 'success'
})

/* 自绘星级（不依赖 Element Plus 的 el-rate） */
function onRateClick(ev: MouseEvent, target: { [k: string]: unknown }, key: string) {
  const el = ev.currentTarget as HTMLElement | null
  if (!el) return
  const rect = el.getBoundingClientRect()
  const x = ev.clientX - rect.left
  const starWidth = rect.width / 5
  const idx = Math.min(5, Math.max(1, Math.ceil(x / starWidth)))
  target[key] = idx
}

/* ========================= 单条 ========================= */
async function onSubmitSingle() {
  if (!singleForm.charValue.trim()) {
    ElMessage.warning('请输入字符')
    return
  }
  if (!singleForm.category) {
    ElMessage.warning('请选择分类')
    return
  }
  submitting.value = true
  try {
    await batchCreateChars({
      defaultCategory: singleForm.category,
      defaultDifficulty: singleForm.difficulty,
      defaultDescription: singleForm.description,
      defaultEnabled: singleForm.enabledBool ? 1 : 0,
      items: [{ charValue: singleForm.charValue.trim() }],
    })
    lastResult.value = {
      inserted: 1,
      skipped: 0,
      failed: 0,
      message: `已提交：${singleForm.charValue.trim()}`,
    }
    ElMessage.success('新增成功')
    resetSingle()
  } catch (e: unknown) {
    console.warn('[DictInput] single submit error', e)
  } finally {
    submitting.value = false
  }
}

function resetSingle() {
  singleForm.charValue = ''
  singleForm.description = ''
  singleForm.difficulty = 2
  singleForm.enabledBool = true
  singleForm.category = 'HANZI'
}

/* ========================= 批量 ========================= */

const batchPlaceholder = `每行一字符（注释行以 # 开头）\n例如：\n永\n的\n# 这是注释\n我\n你`

const previewFromText = computed<CharDictItem[]>(() => {
  return splitByCharacters(batchForm.text)
})

function loadSample() {
  batchForm.text = '永\n的\n我\n你\n他\n爱\n中\n国\n# 这一行是注释，会被忽略\n'
}

async function onSubmitBatch() {
  if (!previewFromText.value.length) {
    ElMessage.warning('请输入至少 1 个字符')
    return
  }
  submitting.value = true
  try {
    const res = await batchCreateChars({
      defaultCategory: batchForm.defaultCategory,
      defaultDifficulty: batchForm.defaultDifficulty,
      items: previewFromText.value.map((it, idx) => ({
        charValue: it.charValue,
        lineNo: idx + 1,
      })),
    })
    lastResult.value = res
    if (res.inserted > 0) {
      ElMessage.success(res.message || `新增 ${res.inserted} 条`)
      batchForm.text = ''
    } else if (res.skipped > 0) {
      ElMessage.warning(`全部 ${res.skipped} 条字符已存在，已跳过`)
    } else if (res.failed > 0) {
      ElMessage.error(res.message || '导入失败')
    }
  } catch (e: unknown) {
    console.warn('[DictInput] batch submit error', e)
  } finally {
    submitting.value = false
  }
}

function splitByCharacters(text: string): CharDictItem[] {
  if (!text) return []
  const lines = text.split(/\r?\n/)
  const items: CharDictItem[] = []
  let lineNo = 0
  for (const raw of lines) {
    lineNo++
    const line = raw.trim()
    if (!line || line.startsWith('#')) continue
    for (let i = 0; i < line.length; i++) {
      const c = line.charAt(i)
      if (/\s/.test(c)) continue
      if (line.length === 1) {
        items.push({ charValue: line, lineNo })
        break
      }
      items.push({ charValue: c, lineNo })
    }
  }
  return items
}

/* ========================= JSON ========================= */

const jsonPlaceholder = `方式一：字符串数组（每条独立字符会自动拆开）
["A","B","C"]

方式二：对象数组（携带元数据）
[
  { "charValue": "永", "category": "HANZI", "difficulty": 3, "description": "汉字：永" },
  { "char": "的", "cat": "HANZI", "level": 2 }
]`

function loadJsonSample() {
  jsonForm.text = `[\n  { "charValue": "永", "category": "HANZI", "difficulty": 3, "description": "汉字：永" },\n  { "charValue": "的", "category": "HANZI", "difficulty": 2, "description": "汉字：的" },\n  { "charValue": "1", "category": "DIGIT", "difficulty": 1 }\n]`
}

async function onSubmitJson() {
  if (!jsonForm.text.trim()) {
    ElMessage.warning('请填写 JSON 文本')
    return
  }
  submitting.value = true
  try {
    const res = await importJsonChars(jsonForm.text.trim(), {
      defaultCategory: jsonForm.defaultCategory,
    })
    lastResult.value = res
    if (res.inserted > 0) {
      ElMessage.success(res.message || `新增 ${res.inserted} 条`)
      jsonForm.text = ''
      jsonFileName.value = ''
    } else if (res.skipped > 0) {
      ElMessage.warning(`全部 ${res.skipped} 条字符已存在，已跳过`)
    } else if (res.failed > 0) {
      ElMessage.error(res.message || '导入失败')
    }
  } catch (e: unknown) {
    console.warn('[DictInput] json submit error', e)
  } finally {
    submitting.value = false
  }
}

async function onPickJsonFile(ev: Event) {
  const input = ev.target as HTMLInputElement
  const f = input.files?.[0]
  if (!f) return
  jsonFileName.value = f.name
  const text = await f.text()
  jsonForm.text = text
  ElMessage.success(`已读取 ${f.name}（${text.length} 字节）`)
}

/* ========================= 文件 ========================= */

function onPickFile(ev: Event) {
  const input = ev.target as HTMLInputElement
  const f = input.files?.[0]
  if (!f) return
  fileRaw.value = f
  fileName.value = f.name
  previewRows.value = []
  previewTotal.value = 0
}

function onRemoveFile() {
  fileRaw.value = null
  fileName.value = ''
  previewRows.value = []
}

const previewRows = ref<CharDictItem[]>([])
const previewTotal = ref(0)
const previewOverflow = computed(() => previewTotal.value > previewRows.value.length)

async function onSubmitFile() {
  const f = fileRaw.value
  if (!f) {
    ElMessage.warning('请先选择文件')
    return
  }
  submitting.value = true
  try {
    const res = await importFileChars(f, {
      defaultCategory: fileForm.defaultCategory,
      defaultDifficulty: fileForm.defaultDifficulty,
    })
    lastResult.value = res
    if (res.inserted > 0) {
      ElMessage.success(res.message || `新增 ${res.inserted} 条`)
      onRemoveFile()
    } else if (res.skipped > 0) {
      ElMessage.warning(`全部 ${res.skipped} 条字符已存在，已跳过`)
    } else if (res.failed > 0) {
      ElMessage.error(res.message || '导入失败')
    }
  } catch (e: unknown) {
    console.warn('[DictInput] file submit error', e)
  } finally {
    submitting.value = false
  }
}

async function onPreviewFile() {
  const f = fileRaw.value
  if (!f) {
    ElMessage.warning('请先选择文件')
    return
  }
  submitting.value = true
  try {
    const res = await previewImportFile(f, 20)
    if (res.ok) {
      previewRows.value = res.preview || []
      previewTotal.value = res.total || 0
      ElMessage.success(`共解析 ${previewTotal.value} 条字符`)
    } else {
      previewRows.value = []
      previewTotal.value = 0
      ElMessage.error(res.message || '解析失败')
    }
  } catch (e: unknown) {
    console.warn('[DictInput] preview error', e)
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
/* ==========================================================================
 * 字符字典录入页 · 样式完全自包含
 * - 全部数值硬编码，不引用任何 SCSS 变量 / mixin
 * - 不使用 var(--xxx) 全局 CSS 变量
 * - 不继承 BaseCard / Element Plus 等父级样式（已用本地 .di-card / .di-tabs 等替换）
 * - 选择器以 .di- 前缀命名，避免与其他文件冲突
 * ========================================================================== */

.di-root {
  /* 局部 CSS 自定义属性（仅本文件内部使用，不污染全局） */
  --di-bg: #fff;
  --di-bg-soft: #f6fbf9;
  --di-bg-soft-2: #eef7f3;
  --di-border: #d6e7df;
  --di-border-soft: #e6efeb;
  --di-text: #1f2d2a;
  --di-text-soft: #5b6b67;
  --di-text-dim: #8a9894;
  --di-primary: #0d9488;
  --di-primary-dark: #0f766e;
  --di-primary-soft: #ccfbf1;
  --di-green: #10b981;
  --di-green-soft: #d1fae5;
  --di-amber: #f59e0b;
  --di-amber-soft: #fef3c7;
  --di-red: #ef4444;
  --di-red-soft: #fee2e2;
  --di-blue: #2563eb;
  --di-shadow: 0 1px 2px rgba(15, 76, 69, 0.04), 0 4px 12px rgba(15, 76, 69, 0.06);

  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 4px;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', sans-serif;
  color: var(--di-text);
  box-sizing: border-box;
}

.di-root *,
.di-root *::before,
.di-root *::after {
  box-sizing: border-box;
}

/* -------------------- 头部 -------------------- */
.di-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 22px;
  background: var(--di-bg);
  border: 1px solid var(--di-border-soft);
  border-radius: 14px;
  box-shadow: var(--di-shadow);
  flex-wrap: wrap;
}

.di-header__text {
  min-width: 0;
  flex: 1 1 320px;
}

.di-header__title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 6px;
  font-size: 18px;
  font-weight: 700;
  color: var(--di-text);
  letter-spacing: 0.5px;
}

.di-header__icon {
  color: var(--di-green);
}

.di-header__sub {
  margin: 0;
  font-size: 13px;
  color: var(--di-text-soft);
  line-height: 1.6;
  max-width: 720px;
}

.di-header__stats {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.di-pill {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 96px;
  padding: 10px 14px;
  border-radius: 10px;
  background: var(--di-bg-soft);
  border: 1px solid var(--di-border-soft);
}

.di-pill__lbl {
  font-size: 12px;
  color: var(--di-text-dim);
}

.di-pill__num {
  font-size: 20px;
  font-weight: 700;
  color: var(--di-text);
  font-variant-numeric: tabular-nums;
}

.di-pill--ok {
  background: var(--di-green-soft);
  border-color: rgba(16, 185, 129, 0.3);
}

.di-pill--ok .di-pill__num {
  color: var(--di-green);
}

.di-pill--warn {
  background: var(--di-amber-soft);
  border-color: rgba(245, 158, 11, 0.3);
}

.di-pill--warn .di-pill__num {
  color: var(--di-amber);
}

.di-pill--err {
  background: var(--di-red-soft);
  border-color: rgba(239, 68, 68, 0.3);
}

.di-pill--err .di-pill__num {
  color: var(--di-red);
}

/* -------------------- 通用 code -------------------- */
.di-code {
  display: inline-block;
  background: var(--di-bg-soft-2);
  padding: 1px 6px;
  border-radius: 4px;
  font-family: 'Cascadia Mono', Consolas, Monaco, monospace;
  font-size: 12px;
  color: var(--di-text);
  word-break: break-all;
}

.di-code--blue {
  color: var(--di-blue);
}

.di-code--red {
  color: var(--di-red);
}

/* -------------------- Tabs -------------------- */
.di-tabs {
  background: var(--di-bg);
  border: 1px solid var(--di-border-soft);
  border-radius: 14px;
  box-shadow: var(--di-shadow);
  overflow: hidden;
}

.di-tabs__nav {
  display: flex;
  gap: 0;
  padding: 6px 6px 0;
  border-bottom: 1px solid var(--di-border-soft);
  background: var(--di-bg-soft);
  overflow-x: auto;
}

.di-tabs__btn {
  appearance: none;
  background: transparent;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 18px;
  font-size: 14px;
  color: var(--di-text-soft);
  cursor: pointer;
  border-radius: 8px 8px 0 0;
  border: 1px solid transparent;
  border-bottom: none;
  margin-bottom: -1px;
  transition:
    color 150ms ease,
    background 150ms ease;
  font-family: inherit;
  white-space: nowrap;
}

.di-tabs__btn:hover {
  color: var(--di-text);
  background: rgba(13, 148, 136, 0.06);
}

.di-tabs__btn.is-active {
  color: var(--di-primary-dark);
  background: var(--di-bg);
  border-color: var(--di-border-soft);
  font-weight: 600;
}

.di-tabs__panel {
  padding: 20px;
}

.di-panel {
  display: block;
}

/* -------------------- Card -------------------- */
.di-card {
  background: var(--di-bg);
  border: 1px solid var(--di-border-soft);
  border-radius: 12px;
  overflow: hidden;
}

.di-card__head {
  padding: 14px 18px;
  border-bottom: 1px solid var(--di-border-soft);
  background: var(--di-bg-soft);
}

.di-card__title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--di-text);
}

.di-card__sub {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--di-text-dim);
}

.di-card__body {
  padding: 18px;
}

.di-card--result {
  border-color: var(--di-primary-soft);
}

/* -------------------- Form -------------------- */
.di-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.di-form__row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.di-form__row--block {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.di-form__label {
  flex: 0 0 92px;
  text-align: right;
  font-size: 13px;
  color: var(--di-text-soft);
  line-height: 32px;
  padding-top: 0;
}

.di-form__required {
  color: var(--di-red);
  margin-right: 2px;
}

.di-form__ctrl {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.di-form__ctrl--inline {
  flex-direction: row;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.di-form__hint {
  font-size: 12px;
  color: var(--di-text-dim);
}

.di-form__hint--top {
  margin-top: 4px;
}

.di-form__counter {
  align-self: flex-end;
  font-size: 12px;
  color: var(--di-text-dim);
  font-variant-numeric: tabular-nums;
}

.di-form__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 12px;
  color: var(--di-text-soft);
  padding: 4px 2px 0;
}

.di-form__meta b {
  color: var(--di-green);
  font-size: 14px;
  font-weight: 700;
}

/* -------------------- Inputs -------------------- */
.di-input,
.di-select,
.di-textarea {
  width: 100%;
  max-width: 480px;
  font: inherit;
  font-size: 14px;
  color: var(--di-text);
  background: var(--di-bg);
  border: 1px solid var(--di-border);
  border-radius: 8px;
  padding: 7px 10px;
  outline: none;
  transition:
    border-color 150ms ease,
    box-shadow 150ms ease;
}

.di-input::placeholder,
.di-textarea::placeholder {
  color: var(--di-text-dim);
}

.di-input:hover,
.di-select:hover,
.di-textarea:hover {
  border-color: var(--di-primary);
}

.di-input:focus,
.di-select:focus,
.di-textarea:focus {
  border-color: var(--di-primary);
  box-shadow: 0 0 0 3px rgba(13, 148, 136, 0.15);
}

.di-textarea {
  resize: vertical;
  min-height: 80px;
  line-height: 1.6;
}

.di-textarea--mono {
  font-family: 'Cascadia Mono', Consolas, Monaco, monospace;
  line-height: 1.7;
  font-size: 13px;
}

.di-select {
  appearance: none;
  padding-right: 28px;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'><path fill='%235b6b67' d='M2.5 4.25L6 7.75l3.5-3.5z'/></svg>");
  background-repeat: no-repeat;
  background-position: right 8px center;
  background-size: 12px 12px;
  max-width: 240px;
  cursor: pointer;
}

/* -------------------- Switch -------------------- */
.di-switch {
  appearance: none;
  position: relative;
  width: 38px;
  height: 22px;
  border-radius: 9999px;
  background: #cbd5d1;
  border: none;
  cursor: pointer;
  padding: 0;
  transition: background 200ms ease;
  flex: 0 0 auto;
}

.di-switch__dot {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
  transition: left 200ms ease;
}

.di-switch.is-on {
  background: var(--di-primary);
}

.di-switch.is-on .di-switch__dot {
  left: 18px;
}

/* -------------------- Rate (自绘) -------------------- */
.di-rate {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
  user-select: none;
  padding: 4px 0;
}

.di-rate__star {
  color: #d3d9d7;
  transition:
    color 120ms ease,
    transform 120ms ease;
  display: inline-block;
}

.di-rate__star.is-on {
  color: var(--di-amber);
}

.di-rate:hover .di-rate__star {
  color: #e2e7e5;
}

.di-rate:hover .di-rate__star.is-on {
  color: var(--di-amber);
}

/* -------------------- Buttons -------------------- */
.di-btn {
  appearance: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 7px 16px;
  font: inherit;
  font-size: 13px;
  line-height: 1.5;
  color: var(--di-text);
  background: var(--di-bg);
  border: 1px solid var(--di-border);
  border-radius: 8px;
  cursor: pointer;
  transition:
    background 150ms ease,
    border-color 150ms ease,
    color 150ms ease;
}

.di-btn:hover:not(:disabled) {
  border-color: var(--di-primary);
  color: var(--di-primary-dark);
}

.di-btn:active:not(:disabled) {
  background: var(--di-bg-soft);
}

.di-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.di-btn--primary {
  background: var(--di-primary);
  border-color: var(--di-primary);
  color: #fff;
}

.di-btn--primary:hover:not(:disabled) {
  background: var(--di-primary-dark);
  border-color: var(--di-primary-dark);
  color: #fff;
}

.di-btn--primary:active:not(:disabled) {
  background: #0b5e58;
  border-color: #0b5e58;
}

.di-btn__ico {
  margin-right: 2px;
  vertical-align: -2px;
}

.di-link {
  appearance: none;
  background: none;
  border: none;
  padding: 0;
  font: inherit;
  font-size: 12px;
  color: var(--di-primary);
  cursor: pointer;
}

.di-link:hover {
  color: var(--di-primary-dark);
  text-decoration: underline;
}

/* -------------------- File button -------------------- */
.di-filebtn {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 7px 14px;
  font-size: 13px;
  color: var(--di-text);
  background: var(--di-bg);
  border: 1px solid var(--di-border);
  border-radius: 8px;
  cursor: pointer;
  transition:
    border-color 150ms ease,
    color 150ms ease;
  overflow: hidden;
}

.di-filebtn:hover {
  border-color: var(--di-primary);
  color: var(--di-primary-dark);
}

.di-filebtn__input {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: pointer;
}

/* -------------------- Preview -------------------- */
.di-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  max-width: 720px;
  padding: 12px;
  background: var(--di-bg-soft);
  border: 1px solid var(--di-border-soft);
  border-radius: 8px;
}

.di-preview__cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  background: var(--di-bg);
  border: 1px solid var(--di-border);
  border-radius: 8px;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.di-preview__cell b {
  font-size: 22px;
  color: var(--di-text);
  line-height: 1.1;
  font-weight: 600;
}

.di-preview__cat {
  font-size: 10px;
  color: var(--di-text-dim);
  margin-top: 2px;
}

.di-preview__more {
  align-self: center;
  font-size: 12px;
  color: var(--di-text-soft);
}

/* -------------------- Alert -------------------- */
.di-alert {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px 16px;
  border-radius: 10px;
  border: 1px solid var(--di-border);
  background: var(--di-bg-soft);
}

.di-alert__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--di-text);
}

.di-alert__sub {
  font-size: 13px;
  color: var(--di-text-soft);
  line-height: 1.6;
}

.di-alert--success {
  background: var(--di-green-soft);
  border-color: rgba(16, 185, 129, 0.35);
}

.di-alert--success .di-alert__title {
  color: #065f46;
}

.di-alert--warning {
  background: var(--di-amber-soft);
  border-color: rgba(245, 158, 11, 0.35);
}

.di-alert--warning .di-alert__title {
  color: #92400e;
}

.di-alert--error {
  background: var(--di-red-soft);
  border-color: rgba(239, 68, 68, 0.35);
}

.di-alert--error .di-alert__title {
  color: #991b1b;
}

/* -------------------- 响应式 -------------------- */
@media (width <= 768px) {
  .di-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .di-form__row {
    flex-direction: column;
    align-items: stretch;
  }

  .di-form__label {
    text-align: left;
    flex: none;
    line-height: 1.5;
  }

  .di-input,
  .di-select,
  .di-textarea {
    max-width: 100%;
  }
}
</style>

<template>
  <div class="hw-page collect-view">
    <div class="hw-container collect-grid">
      <BaseCard class="prompt-card">
        <template #header>
          <div class="prompt-card__header">
            <span>请书写字符</span>
            <button
              type="button"
              class="prompt-card__refresh"
              :disabled="refreshing"
              :aria-label="t('collect.refresh')"
              @click="onRefreshClick"
            >
              <el-icon :class="{ spin: refreshing }"><Refresh /></el-icon>
              <span>{{ t('collect.refresh') }}</span>
            </button>
          </div>
        </template>
        <div class="prompt-card__body">
          <CharPrompt
            :char="currentChar?.char"
            :pinyin="currentChar?.pinyin"
            :stroke-count="currentChar?.strokeCount"
            :difficulty="currentChar?.difficulty"
            :font-id="fontId"
            size="large"
            @update:font-id="onFontChange"
            @refresh="onRefreshClick"
          />
          <div class="tip">请在右侧田字格中按规范书写此字</div>
        </div>
      </BaseCard>

      <BaseCard class="pad-card" title="手写板" subtitle="支持鼠标 / 触屏 / 压感笔">
        <HandwritingPad
          ref="padRef"
          :width="600"
          :height="400"
          :initial-artwork-title="artworkTitle"
          @change="onStrokeChange"
          @save="onPadSave"
          @update:artwork-title="onTitleChange"
        />
      </BaseCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import BaseCard from '@/components/base/BaseCard.vue'
import HandwritingPad from '@/components/business/HandwritingPad.vue'
import CharPrompt from '@/components/business/CharPrompt.vue'
import { useDictStore } from '@/stores/dict'
import { useSampleStore } from '@/stores/sample'
import { useI18n } from 'vue-i18n'
import { storage } from '@/utils/storage'
import { DEFAULT_CHAR_FONT_ID } from '@/utils/charFonts'
import type { CharDict } from '@/api/contracts/sample'

const { t } = useI18n()
const dictStore = useDictStore()
const sampleStore = useSampleStore()
const route = useRoute()

const padRef = ref<InstanceType<typeof HandwritingPad> | null>(null)
const currentChar = ref<CharDict | null>(dictStore.currentChar)
const artworkTitle = ref('')
const strokeCount = ref(0)
const uploading = ref(false)
const refreshing = ref(false)
const startTime = ref<number>(0)
const durationMs = ref(0)
const fontId = ref<string>(storage.getString('hw:fontId') || DEFAULT_CHAR_FONT_ID)
let timer: number | null = null

async function refreshChar() {
  if (refreshing.value) return
  refreshing.value = true
  try {
    const c = await dictStore.fetchRandom()
    currentChar.value = c
    onClear()
  } catch (err) {
    console.warn('refresh char error', err)
    ElMessage.warning('字符加载失败，请检查网络')
  } finally {
    refreshing.value = false
  }
}

function onRefreshClick() {
  refreshChar()
}

function onFontChange(id: string) {
  fontId.value = id
  storage.setString('hw:fontId', id)
}

function onStrokeChange(n: number) {
  strokeCount.value = n
  if (n > 0 && startTime.value === 0) {
    startTime.value = Date.now()
    timer = window.setInterval(() => {
      durationMs.value = Date.now() - startTime.value
    }, 200)
  } else if (n === 0 && timer) {
    window.clearInterval(timer)
    timer = null
    startTime.value = 0
    durationMs.value = 0
  }
}

function onClear() {
  padRef.value?.clear()
  strokeCount.value = 0
  startTime.value = 0
  durationMs.value = 0
  if (timer) {
    window.clearInterval(timer)
    timer = null
  }
}

function onTitleChange(title: string) {
  artworkTitle.value = title
}

/**
 * 板内"存稿"按钮触发：与提交样本走同一上传流程
 * - 正常：调用 sampleStore.upload
 * - 失败：本地持久化到 storage（草稿箱），刷新后可恢复
 */
async function onPadSave(payload: {
  blob: Blob
  svg: string
  dataUrl: string
  strokeCount: number
  durationMs: number
  title: string
}) {
  if (!currentChar.value) {
    ElMessage.warning('请先选择字符')
    return
  }
  if (payload.strokeCount === 0) return
  uploading.value = true
  try {
    await sampleStore.upload(currentChar.value.id, payload.blob, {
      duration: payload.durationMs,
      strokeCount: payload.strokeCount,
      remark: payload.title,
    })
    ElMessage.success('已存入「我的样本」')
    onClear()
    refreshChar()
  } catch (err) {
    // 兜底：本地草稿持久化，确保刷新/重开不丢
    sampleStore.persistDraft(currentChar.value.id, payload.blob, payload.dataUrl, {
      strokeCount: payload.strokeCount,
      duration: payload.durationMs,
      remark: payload.title,
    })
    console.warn('save error, persisted as local draft', err)
    ElMessage.warning('服务器暂不可用，已保存到本地草稿箱')
    onClear()
  } finally {
    uploading.value = false
  }
}

onMounted(async () => {
  // 支持 ?charId=xxx 直接定位到某字符（来自"我的样本"重新采集）
  const charId = route.query.charId
  if (charId) {
    try {
      currentChar.value = await dictStore.fetchById(String(charId))
    } catch {
      /* ignore */
    }
  }
  if (!currentChar.value) {
    refreshChar()
  }
})

onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<style lang="scss" scoped>
.collect-view {
  background: linear-gradient(180deg, $bg-base 0%, $bg-muted 100%);
}

.collect-grid {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: $spacing-md;
  align-items: start;

  @include responsive(md) {
    grid-template-columns: 1fr;
  }
}

.prompt-card {
  position: sticky;
  top: 80px;
  &__header {
    @include flex-between;
    width: 100%;
    color: $text-primary;
    font-weight: 600;
  }
  &__refresh {
    appearance: none;
    border: 1px solid $color-primary-lighter;
    background: $bg-elevated;
    color: $color-primary-dark;
    padding: 4px 10px;
    border-radius: $radius-md;
    font-size: $font-size-sm;
    cursor: pointer;
    display: inline-flex;
    align-items: center;
    gap: 4px;
    transition: all 0.2s ease;

    &:hover:not(:disabled) {
      background: $color-primary;
      color: $text-inverse;
      border-color: $color-primary;
      transform: translateY(-1px);
      box-shadow: 0 2px 6px rgba(13, 148, 136, 0.25);
    }
    &:active:not(:disabled) {
      transform: translateY(0);
    }
    &:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
    &:focus-visible {
      outline: 2px solid $color-primary-light;
      outline-offset: 2px;
    }
    .spin {
      animation: refresh-spin 0.8s linear infinite;
    }
  }
  &__body {
    @include flex-center;
    flex-direction: column;
    padding: $spacing-md 0;
  }
  .tip {
    margin-top: $spacing-md;
    font-size: $font-size-sm;
    color: $text-secondary;
    text-align: center;
  }
}

.pad-card {
  & :deep(.base-card__body) {
    padding: $spacing-md;
  }
}

@keyframes refresh-spin {
  from {
    transform: rotate(0);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>

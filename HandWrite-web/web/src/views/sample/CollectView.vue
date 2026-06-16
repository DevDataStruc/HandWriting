<template>
  <div class="hw-page collect-view">
    <div class="hw-container collect-grid">
      <BaseCard class="prompt-card">
        <template #header>
          <div class="prompt-card__header">
            <span>请书写字符</span>
            <el-button link @click="refreshChar">
              <el-icon><Refresh /></el-icon>
              <span>换一个</span>
            </el-button>
          </div>
        </template>
        <div class="prompt-card__body">
          <CharPrompt
            :char="currentChar?.char"
            :pinyin="currentChar?.pinyin"
            :stroke-count="currentChar?.strokeCount"
            :difficulty="currentChar?.difficulty"
            size="large"
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

const dictStore = useDictStore()
const sampleStore = useSampleStore()
const route = useRoute()

const padRef = ref<InstanceType<typeof HandwritingPad> | null>(null)
const currentChar = ref(dictStore.currentChar)
const artworkTitle = ref('')
const strokeCount = ref(0)
const uploading = ref(false)
const startTime = ref<number>(0)
const durationMs = ref(0)
let timer: number | null = null

async function refreshChar() {
  try {
    currentChar.value = await dictStore.fetchRandom()
    onClear()
  } catch (err) {
    console.warn('refresh char error', err)
    ElMessage.warning('字符加载失败')
  }
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

function onTitleChange(t: string) {
  artworkTitle.value = t
}

/** 板内"存稿"按钮触发：与提交样本走同一上传流程 */
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
    console.warn('save error', err)
    ElMessage.error('保存失败')
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

.pad-footer {
  @include flex-between;
  flex-wrap: wrap;
  gap: $spacing-md;
  margin-top: $spacing-md;
  padding-top: $spacing-md;
  border-top: 1px solid $border-light;

  &__info {
    @include flex-start;
    gap: $spacing-md;
    font-size: $font-size-sm;
    color: $text-secondary;
    font-family: $font-family-mono;
  }

  &__actions {
    @include flex-end;
    gap: $spacing-sm;
  }
}
</style>

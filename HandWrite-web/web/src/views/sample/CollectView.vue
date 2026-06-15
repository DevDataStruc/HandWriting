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
          @change="onStrokeChange"
        />
        <div class="pad-footer">
          <div class="pad-footer__info">
            <span>笔画数：{{ strokeCount }}</span>
            <span>用时：{{ formatDuration(durationMs) }}</span>
          </div>
          <div class="pad-footer__actions">
            <BaseButton type="ghost" @click="onClear">
              <el-icon><Delete /></el-icon>
              <span>清空</span>
            </BaseButton>
            <BaseButton type="cta" :loading="uploading" :disabled="strokeCount === 0" @click="onSubmit">
              <el-icon><Upload /></el-icon>
              <span>提交样本</span>
            </BaseButton>
          </div>
        </div>
      </BaseCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import BaseCard from '@/components/base/BaseCard.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import HandwritingPad from '@/components/business/HandwritingPad.vue'
import CharPrompt from '@/components/business/CharPrompt.vue'
import { useDictStore } from '@/stores/dict'
import { useSampleStore } from '@/stores/sample'
import { formatDuration } from '@/utils/format'

const dictStore = useDictStore()
const sampleStore = useSampleStore()

const padRef = ref<InstanceType<typeof HandwritingPad> | null>(null)
const currentChar = ref(dictStore.currentChar)
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

async function onSubmit() {
  if (!currentChar.value) {
    ElMessage.warning('请先选择字符')
    return
  }
  if (strokeCount.value === 0) {
    ElMessage.warning('请先书写')
    return
  }
  const blob = await padRef.value?.toBlob()
  if (!blob) {
    ElMessage.error('样本生成失败')
    return
  }
  uploading.value = true
  try {
    await sampleStore.upload(currentChar.value.id, blob, {
      duration: durationMs.value,
      strokeCount: strokeCount.value,
    })
    ElMessage.success('上传成功')
    onClear()
    refreshChar()
  } catch (err) {
    console.warn('upload error', err)
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
  }
}

onMounted(() => {
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

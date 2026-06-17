<template>
  <div class="sample-uploader">
    <el-progress
      v-if="uploading"
      :percentage="progress"
      :stroke-width="6"
      :show-text="false"
      class="upload-progress"
    />
    <div class="upload-actions">
      <BaseButton type="cta" :loading="uploading" :disabled="!hasContent" @click="handleUpload">
        <el-icon><Upload /></el-icon>
        <span>{{ uploading ? `上传中 ${progress}%` : '上传样本' }}</span>
      </BaseButton>
      <el-button v-if="!hideClear" link @click="handleClear">清空</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import BaseButton from '@/components/base/BaseButton.vue'

const props = defineProps<{
  hasContent: boolean
  uploading?: boolean
  progress?: number
  hideClear?: boolean
}>()

const emit = defineEmits<{
  upload: [blob: Blob]
  clear: []
}>()

function handleUpload() {
  if (!props.hasContent) {
    ElMessage.warning('请先书写再上传')
    return
  }
  // 通过父组件提供的 ref.getBlob() 拿到
  // 此处仅触发事件，由父组件收集 blob
  emit('upload', new Blob())
}

function handleClear() {
  emit('clear')
}
</script>

<style lang="scss" scoped>
.sample-uploader {
  @include flex-column;
  gap: $spacing-sm;
  width: 100%;
}

.upload-progress {
  width: 100%;
}

.upload-actions {
  @include flex-start;
  gap: $spacing-sm;
}
</style>

<template>
  <div class="audit-action-bar">
    <BaseButton type="primary" :disabled="!canApprove" @click="handleApprove">
      <el-icon><Check /></el-icon>
      <span>通过</span>
    </BaseButton>
    <BaseButton type="danger" :disabled="!canReject" @click="handleReject">
      <el-icon><Close /></el-icon>
      <span>驳回</span>
    </BaseButton>
    <slot name="extra" />
  </div>
</template>

<script setup lang="ts">
import { ElMessageBox } from 'element-plus'
import BaseButton from '@/components/base/BaseButton.vue'

const props = withDefaults(
  defineProps<{
    canApprove?: boolean
    canReject?: boolean
    requireReason?: boolean
  }>(),
  { canApprove: true, canReject: true, requireReason: false }
)

const emit = defineEmits<{
  approve: []
  reject: [reason: string]
}>()

async function handleApprove() {
  if (!props.canApprove) return
  try {
    await ElMessageBox.confirm('确认通过该样本吗？', '审核', { type: 'success' })
    emit('approve')
  } catch {
    /* cancel */
  }
}

async function handleReject() {
  if (!props.canReject) return
  try {
    const { value } = await ElMessageBox.prompt(
      props.requireReason ? '请输入驳回原因' : '请输入驳回原因（可选）',
      '审核',
      {
        confirmButtonText: '确认驳回',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputValidator: (v) => !props.requireReason || !!v,
      }
    )
    emit('reject', value || '')
  } catch {
    /* cancel */
  }
}
</script>

<style lang="scss" scoped>
.audit-action-bar {
  @include flex-end;
  gap: $spacing-sm;
}
</style>

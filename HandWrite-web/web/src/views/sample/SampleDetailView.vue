<template>
  <div class="hw-page sample-detail-view">
    <div class="hw-container">
      <el-button link @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回</span>
      </el-button>
      <BaseCard v-if="sample" class="detail-card" title="样本详情">
        <div class="detail-grid">
          <div class="image-box">
            <img v-if="sample.imageUrl" :src="sample.imageUrl" :alt="sample.char" />
            <span v-else class="placeholder">{{ sample.char }}</span>
          </div>
          <el-descriptions :column="1" border class="info-table">
            <el-descriptions-item label="字符">{{ sample.char || sample.charId }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusType(sample.status)" effect="light">
                {{ statusLabel(sample.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="笔画数">{{ sample.strokeCount ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="用时">{{ formatDuration(sample.duration) }}</el-descriptions-item>
            <el-descriptions-item label="设备">{{ sample.deviceInfo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="提交时间">{{ formatDate(sample.createdAt) }}</el-descriptions-item>
            <el-descriptions-item v-if="sample.reviewedAt" label="审核时间">
              {{ formatDate(sample.reviewedAt) }}
            </el-descriptions-item>
            <el-descriptions-item v-if="sample.rejectReason" label="驳回原因">
              <span class="reject-reason">{{ sample.rejectReason }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </BaseCard>
      <BaseEmpty v-else title="样本不存在或已删除" icon="doc" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import BaseCard from '@/components/base/BaseCard.vue'
import BaseEmpty from '@/components/base/BaseEmpty.vue'
import { useSampleStore } from '@/stores/sample'
import { formatDate, formatDuration } from '@/utils/format'
import type { Sample, SampleStatus } from '@/api/contracts/sample'

const route = useRoute()
const sampleStore = useSampleStore()
const sample = ref<Sample | null>(null)

function statusType(s: SampleStatus) {
  return s === 'APPROVED' ? 'success' : s === 'REJECTED' ? 'danger' : 'warning'
}
function statusLabel(s: SampleStatus) {
  return s === 'APPROVED' ? '已通过' : s === 'REJECTED' ? '已驳回' : '待审核'
}

onMounted(async () => {
  const id = route.params.id as string
  if (!id) return
  try {
    sample.value = await sampleStore.fetchDetail(id)
  } catch {
    sample.value = null
  }
})
</script>

<style lang="scss" scoped>
.sample-detail-view {
  .detail-card {
    margin-top: $spacing-md;
  }
  .detail-grid {
    display: grid;
    grid-template-columns: 400px 1fr;
    gap: $spacing-lg;
    @include responsive(md) {
      grid-template-columns: 1fr;
    }
  }
  .image-box {
    @include flex-center;
    aspect-ratio: 4/3;
    background: $bg-muted;
    border-radius: $radius-md;
    img {
      max-width: 100%;
      max-height: 100%;
      object-fit: contain;
    }
    .placeholder {
      font-family: $font-family-cn;
      font-size: 96px;
      color: $text-placeholder;
    }
  }
  .reject-reason {
    color: $color-danger;
  }
}
</style>

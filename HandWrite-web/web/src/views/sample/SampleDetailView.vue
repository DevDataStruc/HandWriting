<template>
  <div class="hw-page sample-detail-view">
    <div class="hw-container">
      <el-button link @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon>
        <span>{{ t('samples.back') }}</span>
      </el-button>
      <BaseCard v-if="sample" class="detail-card" :title="t('samples.detail')">
        <template #header-extra>
          <el-button-group>
            <el-tooltip :content="t('samples.exportPng')">
              <el-button link @click="onExport('png')">
                <el-icon><Download /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip :content="t('samples.exportSvg')">
              <el-button link @click="onExport('svg')">
                <el-icon><PictureFilled /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip :content="t('samples.reCollect')">
              <el-button link @click="onReCollect">
                <el-icon><Refresh /></el-icon>
              </el-button>
            </el-tooltip>
          </el-button-group>
        </template>
        <div class="detail-grid">
          <div class="image-box" @click="onImagePreview">
            <img v-if="sample.imageUrl" :src="sample.imageUrl" :alt="sample.char" />
            <span v-else class="placeholder">{{ sample.char }}</span>
          </div>
          <el-descriptions :column="1" border class="info-table">
            <el-descriptions-item :label="t('samples.char')">
              {{ sample.char || sample.charId }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('samples.status')">
              <el-tag :type="statusType(sample.status)" effect="light">
                {{ statusLabel(sample.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item :label="t('samples.strokes')">
              {{ sample.strokeCount ?? '-' }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('collect.duration')">
              {{ formatDuration(sample.duration) }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('samples.device')">
              {{ sample.deviceInfo || '-' }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('samples.submittedAt')">
              {{ formatDate(sample.createdAt) }}
            </el-descriptions-item>
            <el-descriptions-item v-if="sample.reviewedAt" :label="t('samples.reviewedAt')">
              {{ formatDate(sample.reviewedAt) }}
            </el-descriptions-item>
            <el-descriptions-item v-if="sample.rejectReason" :label="t('samples.rejectReason')">
              <span class="reject-reason">{{ sample.rejectReason }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </BaseCard>
      <BaseEmpty
        v-else-if="!loading"
        :title="t('samples.notFound')"
        :description="t('samples.notFoundDesc')"
        icon="doc"
      >
        <el-button type="primary" @click="$router.replace({ name: 'MySamples' })">
          {{ t('samples.title') }}
        </el-button>
      </BaseEmpty>
    </div>

    <el-image-viewer
      v-if="previewVisible"
      :url-list="previewList"
      :initial-index="0"
      @close="previewVisible = false"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElImageViewer, ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import BaseCard from '@/components/base/BaseCard.vue'
import BaseEmpty from '@/components/base/BaseEmpty.vue'
import { useSampleStore } from '@/stores/sample'
import { formatDate, formatDuration } from '@/utils/format'
import {
  downloadBlob,
  downloadSvgString,
  pngUrlToSvgString,
  triggerDownload,
} from '@/utils/download'
import type { Sample, SampleStatus } from '@/api/contracts/sample'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const sampleStore = useSampleStore()
const sample = ref<Sample | null>(null)
const loading = ref(false)
const previewVisible = ref(false)
const previewList = ref<string[]>([])

function statusType(s: SampleStatus) {
  return s === 'APPROVED' ? 'success' : s === 'REJECTED' ? 'danger' : 'warning'
}
function statusLabel(s: SampleStatus) {
  return s === 'APPROVED'
    ? t('samples.approved')
    : s === 'REJECTED'
      ? t('samples.rejected')
      : t('samples.pending')
}

function onImagePreview() {
  if (sample.value?.imageUrl) {
    previewList.value = [sample.value.imageUrl]
    previewVisible.value = true
  }
}

function onReCollect() {
  if (!sample.value) return
  router.push({ name: 'Collect', query: { charId: String(sample.value.charId) } })
}

async function onExport(cmd: 'png' | 'svg') {
  if (!sample.value?.imageUrl) {
    ElMessage.warning('该样本无可下载图像')
    return
  }
  const base = `${sample.value.char || 'sample'}_${sample.value.id}`
  try {
    if (cmd === 'png') {
      const res = await fetch(sample.value.imageUrl, { mode: 'cors' })
      const blob = await res.blob()
      await downloadBlob(blob, `${base}.png`)
    } else {
      const svg = await pngUrlToSvgString(sample.value.imageUrl)
      downloadSvgString(svg, `${base}.svg`)
    }
    ElMessage.success(`${t('samples.export')} ✓`)
  } catch (err) {
    console.warn('[detail export] fallback', err)
    triggerDownload(sample.value.imageUrl, `${base}.${cmd}`)
  }
}

onMounted(async () => {
  const id = route.params.id as string
  if (!id) return
  loading.value = true
  try {
    sample.value = await sampleStore.fetchDetail(id)
  } catch {
    sample.value = null
  } finally {
    loading.value = false
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
    cursor: zoom-in;
    overflow: hidden;
    transition: opacity $transition-fast;
    img {
      max-width: 100%;
      max-height: 100%;
      object-fit: contain;
    }
    &:hover {
      opacity: 0.9;
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

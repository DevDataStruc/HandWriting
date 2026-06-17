<template>
  <div class="hw-page my-samples-view">
    <div class="hw-container">
      <div class="page-header">
        <h1 class="hw-page-title">{{ t('samples.title') }}</h1>
        <div class="page-header__actions">
          <el-input
            v-model="query.keyword"
            :placeholder="t('samples.keyword')"
            clearable
            style="width: 240px"
            @keyup.enter="onSearch"
          >
            <template #prefix
              ><el-icon><Search /></el-icon
            ></template>
          </el-input>
          <el-select
            v-model="query.status"
            :placeholder="t('samples.status')"
            clearable
            style="width: 130px"
            @change="onSearch"
          >
            <el-option :label="t('samples.pending')" :value="0" />
            <el-option :label="t('samples.approved')" :value="1" />
            <el-option :label="t('samples.rejected')" :value="2" />
          </el-select>
          <el-button v-if="selectedIds.length > 0" type="danger" plain @click="onBatchDelete">
            <el-icon><Delete /></el-icon>
            <span>{{ t('samples.batchDelete') }} ({{ selectedIds.length }})</span>
          </el-button>
          <el-button type="primary" @click="$router.push({ name: 'Collect' })">
            <el-icon><EditPen /></el-icon>
            <span>{{ t('samples.goCollect') }}</span>
          </el-button>
        </div>
      </div>

      <BaseEmpty
        v-if="!sampleStore.loading && sampleStore.list.length === 0"
        :title="t('samples.empty')"
        :description="t('samples.emptyDesc')"
        icon="image"
      />
      <div v-else class="sample-grid">
        <BaseCard
          v-for="item in sampleStore.list"
          :key="item.id"
          hoverable
          class="sample-card"
          :class="{ 'is-selected': selectedIds.includes(String(item.id)) }"
        >
          <el-checkbox
            v-if="batchMode"
            class="sample-card__check"
            :model-value="selectedIds.includes(String(item.id))"
            @change="onSelectChange(item.id, $event)"
          />
          <div class="sample-card__image" @click="goDetail(item.id)">
            <img
              v-if="displayUrl(item)"
              :src="displayUrl(item)"
              :alt="displayChar(item)"
              loading="lazy"
            />
            <span v-else class="placeholder">{{ displayChar(item) || '?' }}</span>
          </div>
          <div class="sample-card__info">
            <div class="char">{{ displayChar(item) || item.charId }}</div>
            <div class="tags">
              <el-tag :type="statusType(item.status)" size="small" effect="light">
                {{ statusLabel(item.status) }}
              </el-tag>
              <el-tag v-if="isLocal(item)" type="info" size="small" effect="plain">
                本地草稿
              </el-tag>
            </div>
          </div>
          <div class="sample-card__meta">
            <span class="time">{{ formatDate(item.createTime, 'YYYY-MM-DD HH:mm') }}</span>
            <span v-if="item.strokeCount" class="stroke-count">
              {{ t('samples.strokes') }} {{ item.strokeCount }}
            </span>
          </div>
          <div class="sample-card__footer">
            <el-button-group>
              <el-tooltip :content="t('samples.view')">
                <el-button link @click="goDetail(item.id)">
                  <el-icon><View /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip :content="t('samples.export')">
                <el-dropdown trigger="click" @command="(cmd: string) => onExport(cmd, item)">
                  <el-button link>
                    <el-icon><Download /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="png">{{
                        t('samples.exportPng')
                      }}</el-dropdown-item>
                      <el-dropdown-item command="svg">{{
                        t('samples.exportSvg')
                      }}</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </el-tooltip>
              <el-tooltip :content="t('samples.reCollect')">
                <el-button link @click="onReCollect(item)">
                  <el-icon><Refresh /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip :content="t('samples.delete')">
                <el-button link type="danger" @click="onDelete(item.id)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </el-tooltip>
            </el-button-group>
          </div>
        </BaseCard>
      </div>

      <div class="pagination-wrap">
        <el-checkbox v-model="batchMode" class="batch-toggle">
          {{ t('samples.batchDelete') }}
        </el-checkbox>
        <el-pagination
          v-model:current-page="sampleStore.pageNum"
          v-model:page-size="sampleStore.pageSize"
          :total="sampleStore.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="onSearch"
          @current-change="onSearch"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import BaseCard from '@/components/base/BaseCard.vue'
import BaseEmpty from '@/components/base/BaseEmpty.vue'
import { useSampleStore } from '@/stores/sample'
import { formatDate } from '@/utils/format'
import {
  downloadBlob,
  downloadSvgString,
  pngUrlToSvgString,
  triggerDownload,
} from '@/utils/download'
import type { SampleVO } from '@/api/contracts/sample'

const { t } = useI18n()
const router = useRouter()
const sampleStore = useSampleStore()

const query = reactive({
  keyword: '',
  status: undefined as number | undefined,
})

const selectedIds = ref<string[]>([])
const batchMode = ref(false)

function statusType(s: number) {
  return s === 1 ? 'success' : s === 2 ? 'danger' : 'warning'
}
function statusLabel(s: number) {
  return s === 1 ? t('samples.approved') : s === 2 ? t('samples.rejected') : t('samples.pending')
}

/** 显示用的图片 URL：优先 thumbUrl > fileUrl > 本地 imageUrl */
function displayUrl(item: SampleVO): string {
  const local = (item as { imageUrl?: string }).imageUrl
  return item.thumbUrl || item.fileUrl || local || ''
}

/** 显示用的字符：char > 本地扩展 > charValue */
function displayChar(item: SampleVO): string {
  return item.char || (item as { charValue?: string }).charValue || ''
}

function isLocal(item: SampleVO): boolean {
  return String(item.id).startsWith('local-')
}

async function onSearch() {
  await sampleStore.fetchPage({ keyword: query.keyword, status: query.status })
}

function goDetail(id: number | string) {
  router.push({ name: 'SampleDetail', params: { id: String(id) } })
}

function onSelectChange(id: number | string, checked: boolean | string | number) {
  const sid = String(id)
  if (checked) {
    if (!selectedIds.value.includes(sid)) selectedIds.value.push(sid)
  } else {
    selectedIds.value = selectedIds.value.filter((x) => x !== sid)
  }
}

async function onDelete(id: number | string) {
  try {
    await ElMessageBox.confirm(t('samples.deleteConfirm'), '', { type: 'warning' })
    await sampleStore.remove(id)
    selectedIds.value = selectedIds.value.filter((x) => x !== String(id))
    ElMessage.success(t('samples.deleteSuccess'))
  } catch {
    /* cancel */
  }
}

async function onBatchDelete() {
  if (selectedIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(
      `${t('samples.deleteConfirm')} (${selectedIds.value.length})`,
      t('samples.batchDelete'),
      { type: 'warning' }
    )
    await sampleStore.removeMany(selectedIds.value)
    ElMessage.success(t('samples.deleteSuccess'))
    selectedIds.value = []
  } catch {
    /* cancel */
  }
}

function onReCollect(item: SampleVO) {
  router.push({ name: 'Collect', query: { charId: String(item.charId) } })
}

async function onExport(cmd: string, item: SampleVO) {
  const url = displayUrl(item)
  if (!url) {
    ElMessage.warning('该样本无可下载图像')
    return
  }
  const isLocalDraft = String(item.id).startsWith('local-')
  const base = `${displayChar(item) || 'sample'}_${item.id}`
  try {
    if (cmd === 'png') {
      if (isLocalDraft && url.startsWith('data:image')) {
        // 本地草稿走 dataURL → Blob 通道，避开 CORS
        const res = await fetch(url)
        const blob = await res.blob()
        await downloadBlob(blob, `${base}.png`)
      } else {
        const res = await fetch(url, { mode: 'cors' })
        const blob = await res.blob()
        await downloadBlob(blob, `${base}.png`)
      }
    } else if (cmd === 'svg') {
      const svg = await pngUrlToSvgString(url)
      downloadSvgString(svg, `${base}.svg`)
    }
    ElMessage.success(`${t('samples.export')} ✓`)
  } catch (err) {
    console.warn('[export] fallback to direct link', err)
    triggerDownload(url, `${base}.${cmd}`)
  }
}

onMounted(() => {
  onSearch()
})
</script>

<style lang="scss" scoped>
.my-samples-view {
  .page-header {
    @include flex-between;
    flex-wrap: wrap;
    gap: $spacing-md;
    margin-bottom: $spacing-md;
  }
  .page-header__actions {
    @include flex-end;
    flex-wrap: wrap;
    gap: $spacing-sm;
  }
}

.sample-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: $spacing-md;
}

.sample-card {
  position: relative;
  transition:
    outline $transition-fast,
    transform $transition-fast;

  &.is-selected {
    outline: 2px solid $color-primary;
    outline-offset: -2px;
  }

  &__check {
    position: absolute;
    top: 6px;
    left: 6px;
    z-index: 2;
    background: rgba(255, 255, 255, 0.85);
    border-radius: 4px;
    padding: 0 4px;
  }

  &__image {
    @include flex-center;
    aspect-ratio: 4/3;
    background: $bg-muted;
    border-radius: $radius-md;
    overflow: hidden;
    cursor: pointer;
    transition: transform $transition-fast;
    img {
      max-width: 100%;
      max-height: 100%;
      object-fit: contain;
    }
    &:hover {
      transform: scale(1.02);
    }
    .placeholder {
      font-family: $font-family-cn;
      font-size: 64px;
      color: $text-placeholder;
    }
  }

  &__info {
    @include flex-between;
    margin-top: $spacing-sm;
    .char {
      font-family: $font-family-cn;
      font-size: $font-size-lg;
      font-weight: 600;
      color: $text-primary;
    }
  }

  &__meta {
    @include flex-between;
    margin-top: $spacing-xs;
    font-size: $font-size-xs;
    color: $text-secondary;
    font-family: $font-family-mono;

    .stroke-count {
      color: $color-primary-dark;
    }
  }

  &__footer {
    @include flex-end;
    margin-top: $spacing-sm;
  }
}

.pagination-wrap {
  @include flex-between;
  flex-wrap: wrap;
  gap: $spacing-md;
  margin-top: $spacing-lg;

  .batch-toggle {
    color: $text-secondary;
  }
}
</style>

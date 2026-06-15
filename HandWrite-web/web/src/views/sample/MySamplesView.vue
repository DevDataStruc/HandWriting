<template>
  <div class="hw-page my-samples-view">
    <div class="hw-container">
      <div class="page-header">
        <h1 class="hw-page-title">我的样本</h1>
        <div class="page-header__actions">
          <el-input
            v-model="query.keyword"
            placeholder="搜索字符"
            clearable
            style="width: 240px"
            @keyup.enter="onSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="onSearch">
            <el-option label="待审核" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
          <el-button type="primary" @click="$router.push({ name: 'Collect' })">去采集</el-button>
        </div>
      </div>

      <BaseEmpty v-if="!sampleStore.loading && sampleStore.list.length === 0" title="暂无样本" description="快去采集第一个样本吧" icon="image" />
      <div v-else class="sample-grid">
        <BaseCard
          v-for="item in sampleStore.list"
          :key="item.id"
          hoverable
          class="sample-card"
        >
          <div class="sample-card__image" @click="goDetail(item.id)">
            <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.char" />
            <span v-else class="placeholder">{{ item.char || '?' }}</span>
          </div>
          <div class="sample-card__info">
            <div class="char">{{ item.char || item.charId }}</div>
            <el-tag :type="statusType(item.status)" size="small" effect="light">
              {{ statusLabel(item.status) }}
            </el-tag>
          </div>
          <div class="sample-card__footer">
            <span class="time">{{ formatDate(item.createdAt, 'MM-DD HH:mm') }}</span>
            <el-button link type="danger" @click="onDelete(item.id)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </BaseCard>
      </div>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="sampleStore.pageNum"
          v-model:page-size="sampleStore.pageSize"
          :total="sampleStore.total"
          :page-sizes="[10, 20, 50]"
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
import { onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import BaseCard from '@/components/base/BaseCard.vue'
import BaseEmpty from '@/components/base/BaseEmpty.vue'
import { useSampleStore } from '@/stores/sample'
import { formatDate } from '@/utils/format'
import type { SampleStatus } from '@/api/contracts/sample'

const router = useRouter()
const sampleStore = useSampleStore()

const query = reactive({
  keyword: '',
  status: undefined as SampleStatus | undefined,
})

function statusType(s: SampleStatus) {
  return s === 'APPROVED' ? 'success' : s === 'REJECTED' ? 'danger' : 'warning'
}
function statusLabel(s: SampleStatus) {
  return s === 'APPROVED' ? '已通过' : s === 'REJECTED' ? '已驳回' : '待审核'
}

async function onSearch() {
  await sampleStore.fetchPage({ keyword: query.keyword, status: query.status })
}

function goDetail(id: number | string) {
  router.push({ name: 'SampleDetail', params: { id: String(id) } })
}

async function onDelete(id: number | string) {
  try {
    await ElMessageBox.confirm('确定要删除该样本吗？', '提示', { type: 'warning' })
    await sampleStore.remove(id)
    ElMessage.success('删除成功')
  } catch {
    /* cancel */
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
    gap: $spacing-sm;
  }
}

.sample-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: $spacing-md;
}

.sample-card {
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
  &__footer {
    @include flex-between;
    margin-top: $spacing-sm;
    .time {
      font-size: $font-size-xs;
      color: $text-secondary;
      font-family: $font-family-mono;
    }
  }
}

.pagination-wrap {
  @include flex-end;
  margin-top: $spacing-lg;
}
</style>

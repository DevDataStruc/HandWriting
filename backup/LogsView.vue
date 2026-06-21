<template>
  <div class="logs-view">
    <BaseCard title="审核历史">
      <template #extra>
        <el-select
          v-model="query.status"
          placeholder="状态"
          clearable
          style="width: 140px"
          @change="loadData"
        >
          <el-option :label="'待审核'" :value="0" />
          <el-option :label="'通过'" :value="1" />
          <el-option :label="'驳回'" :value="2" />
        </el-select>
        <el-input
          v-model="query.keyword"
          placeholder="搜索字符/提交人"
          clearable
          style="width: 240px"
          @keyup.enter="loadData"
        />
        <el-button type="primary" @click="loadData">查询</el-button>
      </template>
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column label="ID" width="80">
          <template #default="{ row }">{{ row.id }}</template>
        </el-table-column>
        <el-table-column label="字符" width="80">
          <template #default="{ row }">
            <span class="char-cell">{{ row.char || row.charId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="缩略图" width="120">
          <template #default="{ row }">
            <div class="thumb-cell">
              <img v-if="displayUrl(row as SampleVO)" :src="displayUrl(row as SampleVO)" :alt="(row as SampleVO).char || ''" />
              <span v-else class="placeholder">?</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="提交人" width="120">
          <template #default="{ row }">@{{ row.userId }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small" effect="light">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核人" width="120">
          <template #default="{ row }">
            {{ row.auditedBy ? `@${row.auditedBy}` : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="审核时间" width="180">
          <template #default="{ row }">
            {{ row.auditedTime ? formatDate(row.auditedTime) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="驳回原因" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.rejectReason" class="reject-reason">{{ row.rejectReason }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="180">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </BaseCard>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import BaseCard from '@/components/base/BaseCard.vue'
import { useAuditStore } from '@/stores/audit'
import { formatDate } from '@/utils/format'
import type { SampleVO } from '@/api/contracts/sample.d'
import { SampleStatusCode } from '@/api/constants/sampleStatus'

const { t } = useI18n()
const auditStore = useAuditStore()

const list = ref<SampleVO[]>([])
const total = ref(0)
const loading = ref(false)

const query = reactive({
  keyword: '',
  status: undefined as number | undefined,
  pageNum: 1,
  pageSize: 20,
})

function statusType(s: number): 'success' | 'danger' | 'warning' {
  if (s === SampleStatusCode.APPROVED) return 'success'
  if (s === SampleStatusCode.REJECTED) return 'danger'
  return 'warning'
}

function statusLabel(s: number): string {
  if (s === SampleStatusCode.APPROVED) return t('samples.approved')
  if (s === SampleStatusCode.REJECTED) return t('samples.rejected')
  return t('samples.pending')
}

function displayUrl(row: SampleVO): string {
  return row.thumbUrl || row.fileUrl || row.imageUrl || ''
}

async function loadData(): Promise<void> {
  loading.value = true
  try {
    const res = await auditStore.fetchHistory({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      status: query.status,
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    console.warn('[logs] load history error', err)
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.logs-view {
  .char-cell {
    font-family: $font-family-cn;
    font-size: 18px;
    font-weight: 600;
    color: $color-primary-dark;
  }
  .thumb-cell {
    @include flex-center;
    width: 80px;
    height: 60px;
    background: $bg-muted;
    border-radius: $radius-sm;
    overflow: hidden;
    img {
      max-width: 100%;
      max-height: 100%;
      object-fit: contain;
    }
    .placeholder {
      font-family: $font-family-cn;
      font-size: 24px;
      color: $text-placeholder;
    }
  }
  .reject-reason {
    color: $color-danger;
  }
  .pagination {
    @include flex-end;
    margin-top: $spacing-md;
  }
}
</style>

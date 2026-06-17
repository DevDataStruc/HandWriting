<template>
  <div class="sample-audit">
    <BaseCard title="样本审核工作台">
      <template #extra>
        <el-input
          v-model="query.keyword"
          placeholder="搜索字符/用户"
          clearable
          style="width: 200px"
          @keyup.enter="onSearch"
        />
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button :disabled="selection.length === 0" @click="onBatch('APPROVED')"
          >批量通过</el-button
        >
        <el-button :disabled="selection.length === 0" type="danger" @click="onBatch('REJECTED')"
          >批量驳回</el-button
        >
      </template>
      <el-table
        v-loading="auditStore.loading"
        :data="auditStore.pendingList"
        border
        stripe
        @selection-change="onSelection"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column label="样本" width="120">
          <template #default="{ row }">
            <div class="image-cell">
              <img v-if="displayUrl(row as SampleVO)" :src="displayUrl(row as SampleVO)" />
              <span v-else>{{ (row as SampleVO).char || (row as SampleVO).charId || '?' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="字符" prop="char" width="80" />
        <el-table-column label="提交人" width="120">
          <template #default="{ row }">@{{ (row as SampleVO).userId }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default>
            <el-tag type="warning" size="small">待审核</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="笔画" prop="strokeCount" width="80" />
        <el-table-column label="用时" width="100">
          <template #default="{ row }">{{ formatDuration((row as SampleVO).duration) }}</template>
        </el-table-column>
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ formatDate((row as SampleVO).createTime) }}</template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <AuditActionBar
              require-reason
              @approve="onApprove(row as SampleVO)"
              @reject="(r) => onReject(row as SampleVO, r)"
            />
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination
          v-model:current-page="auditStore.pageNum"
          v-model:page-size="auditStore.pageSize"
          :total="auditStore.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="onSearch"
          @current-change="onSearch"
        />
      </div>
    </BaseCard>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import BaseCard from '@/components/base/BaseCard.vue'
import AuditActionBar from '@/components/business/AuditActionBar.vue'
import { useAuditStore } from '@/stores/audit'
import { formatDate, formatDuration } from '@/utils/format'
import type { SampleVO } from '@/api/contracts/sample'

const auditStore = useAuditStore()
const selection = ref<SampleVO[]>([])

const query = reactive({
  keyword: '',
})

function displayUrl(row: SampleVO): string {
  return row.thumbUrl || row.fileUrl || row.imageUrl || ''
}

function onSelection(rows: SampleVO[]) {
  selection.value = rows
}

async function onSearch() {
  await auditStore.fetchPending({
    ...query,
    pageNum: auditStore.pageNum,
    pageSize: auditStore.pageSize,
  })
}

async function onApprove(row: SampleVO) {
  try {
    await auditStore.approve(row.id as number)
    ElMessage.success('已通过')
  } catch (err) {
    console.warn('approve error', err)
  }
}

async function onReject(row: SampleVO, reason: string) {
  if (!reason) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  try {
    await auditStore.reject(row.id as number, { reason })
    ElMessage.success('已驳回')
  } catch (err) {
    console.warn('reject error', err)
  }
}

async function onBatch(action: 'APPROVED' | 'REJECTED') {
  if (selection.value.length === 0) return
  try {
    await ElMessageBox.confirm(
      `确认对 ${selection.value.length} 个样本执行${action === 'APPROVED' ? '通过' : '驳回'}操作？`,
      '批量审核',
      { type: 'warning' }
    )
    let reason = ''
    if (action === 'REJECTED') {
      const { value } = await ElMessageBox.prompt('请输入批量驳回原因', '提示', {
        inputType: 'textarea',
      })
      reason = value || ''
    }
    const ids = selection.value.map((s) => s.id as number)
    const res = await auditStore.batchAudit(ids, action, reason)
    ElMessage.success(`已处理 ${res.successCount} 个，失败 ${res.failedCount} 个`)
  } catch {
    /* cancel */
  }
}

onMounted(() => {
  onSearch()
})
</script>

<style lang="scss" scoped>
.sample-audit {
  .image-cell {
    @include flex-center;
    width: 80px;
    height: 60px;
    background: $bg-muted;
    border-radius: $radius-sm;
    font-family: $font-family-cn;
    font-size: 32px;
    color: $color-primary;
    img {
      max-width: 100%;
      max-height: 100%;
      object-fit: contain;
    }
  }
  .pagination {
    @include flex-end;
    margin-top: $spacing-md;
  }
}
</style>

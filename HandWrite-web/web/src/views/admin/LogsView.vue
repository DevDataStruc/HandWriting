<template>
  <div class="logs-view">
    <BaseCard title="审计日志">
      <template #extra>
        <el-input v-model="query.keyword" placeholder="搜索操作/用户" clearable style="width: 240px" @keyup.enter="loadData" />
        <el-button type="primary" @click="loadData">查询</el-button>
      </template>
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="操作人" prop="username" width="160" />
        <el-table-column label="操作" prop="action" width="180" />
        <el-table-column label="模块" prop="module" width="120" />
        <el-table-column label="IP" prop="ip" width="140" />
        <el-table-column label="结果" width="100">
          <template #default="{ row }">
            <el-tag :type="row.result === 'success' ? 'success' : 'danger'" size="small">
              {{ row.result === 'success' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="详情" prop="detail" min-width="200" show-overflow-tooltip />
        <el-table-column label="时间" width="180">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          layout="total, prev, pager, next"
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
import BaseCard from '@/components/base/BaseCard.vue'
import { formatDate } from '@/utils/format'
import request from '@/utils/request'

interface LogItem {
  id: number
  username: string
  action: string
  module: string
  ip: string
  result: 'success' | 'fail'
  detail: string
  createdAt: string
}

const list = ref<LogItem[]>([])
const total = ref(0)
const loading = ref(false)

const query = reactive({
  keyword: '',
  pageNum: 1,
  pageSize: 20,
})

async function loadData() {
  loading.value = true
  try {
    const res = await request.get<{ list: LogItem[]; total: number }>('/admin/logs', query)
    list.value = res.list
    total.value = res.total
  } catch (err) {
    console.warn('load logs error', err)
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
  .pagination {
    @include flex-end;
    margin-top: $spacing-md;
  }
}
</style>

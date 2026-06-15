<template>
  <div class="user-management">
    <BaseCard title="用户管理">
      <template #extra>
        <el-input v-model="query.keyword" placeholder="搜索用户名/邮箱" clearable style="width: 240px" @keyup.enter="loadData">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="loadData">查询</el-button>
      </template>
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="用户" min-width="200">
          <template #default="{ row }">
            <div class="user-cell">
              <UserAvatar :user="row" :size="32" />
              <div>
                <div class="name">{{ row.nickname || row.username }}</div>
                <div class="sub">@{{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column label="角色" width="160">
          <template #default="{ row }">
            <el-tag v-for="r in row.roles" :key="r" size="small" effect="dark" style="margin-right: 4px">
              {{ r }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sampleCount" label="样本数" width="100" align="right" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'danger'" size="small">
              {{ row.status === 'active' ? '正常' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="180">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small">编辑</el-button>
            <el-button link type="danger" size="small" @click="toggleStatus(row)">
              {{ row.status === 'active' ? '禁用' : '启用' }}
            </el-button>
          </template>
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
import { ElMessage } from 'element-plus'
import BaseCard from '@/components/base/BaseCard.vue'
import UserAvatar from '@/components/business/UserAvatar.vue'
import { formatDate } from '@/utils/format'
import type { UserProfile } from '@/api/contracts/user'
import request from '@/utils/request'

const list = ref<UserProfile[]>([])
const total = ref(0)
const loading = ref(false)

const query = reactive({
  keyword: '',
  pageNum: 1,
  pageSize: 10,
})

async function loadData() {
  loading.value = true
  try {
    // 此处使用通用 request 调用 /admin/users 列表
    const res = await request.get<{ list: UserProfile[]; total: number }>('/admin/users', query)
    list.value = res.list
    total.value = res.total
  } catch (err) {
    console.warn('load users error', err)
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function toggleStatus(row: UserProfile) {
  ElMessage.info(`已 ${row.status === 'active' ? '禁用' : '启用'} ${row.username}（演示）`)
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.user-management {
  .user-cell {
    @include flex-start;
    gap: $spacing-sm;
    .name {
      color: $text-primary;
      font-weight: 500;
    }
    .sub {
      color: $text-secondary;
      font-size: $font-size-xs;
    }
  }
  .pagination {
    @include flex-end;
    margin-top: $spacing-md;
  }
}
</style>

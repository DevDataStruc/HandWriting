<template>
  <div class="urls-view hw-page">
    <h1 class="hw-page-title">URL 索引</h1>
    <p class="urls-view__intro">
      汇总本项目所有的前端路由与后端 API 地址，便于联调、查阅与文档同步。
      实际请求 Base URL：<code>{{ baseURL }}</code>
    </p>

    <!-- 统计卡片 -->
    <div class="stat-row">
      <div class="stat-card">
        <div class="stat-card__label">前端路由</div>
        <div class="stat-card__value">{{ frontendUrls.length }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-card__label">后端 API</div>
        <div class="stat-card__value">{{ backendApis.length }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-card__label">需鉴权</div>
        <div class="stat-card__value">{{ authRequiredCount }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-card__label">仅管理员</div>
        <div class="stat-card__value">{{ adminOnlyCount }}</div>
      </div>
    </div>

    <!-- 搜索 -->
    <BaseCard class="section">
      <template #header>
        <div class="search-bar">
          <el-input
            v-model="keyword"
            placeholder="搜索路径、名称或描述"
            clearable
            style="max-width: 360px"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-radio-group v-model="filterType" size="small">
            <el-radio-button label="all">全部</el-radio-button>
            <el-radio-button label="frontend">前端路由</el-radio-button>
            <el-radio-button label="backend">后端 API</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <!-- 前端路由 -->
      <template v-if="filterType !== 'backend'">
        <h2 class="section-title">
          <el-icon><Promotion /></el-icon>
          前端路由（{{ filteredFrontend.length }}）
        </h2>

        <el-empty
          v-if="filteredFrontend.length === 0"
          description="没有匹配的前端路由"
        />

        <div v-for="group in groupedFrontend" :key="group.module" class="group">
          <div class="group__header">
            <span class="group__title">{{ group.module }}</span>
            <el-tag size="small" type="info">{{ group.items.length }} 条</el-tag>
          </div>
          <el-table :data="group.items" stripe border size="small">
            <el-table-column prop="method" label="方法" width="90">
              <template #default="{ row }">
                <el-tag
                  :type="row.methodColor"
                  size="small"
                  effect="dark"
                  class="method-tag"
                >
                  {{ row.method }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="path" label="路径" min-width="220">
              <template #default="{ row }">
                <code class="path-code">{{ row.path }}</code>
              </template>
            </el-table-column>
            <el-table-column prop="name" label="名称" width="160">
              <template #default="{ row }">
                <span v-if="row.name" class="name-cell">{{ row.name }}</span>
                <span v-else class="hw-text-muted">—</span>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="标题" width="140" />
            <el-table-column label="鉴权" width="100">
              <template #default="{ row }">
                <el-tag
                  v-if="row.requiresAuth"
                  size="small"
                  type="warning"
                  effect="light"
                >
                  {{ row.rolesLabel }}
                </el-tag>
                <el-tag v-else size="small" type="info" effect="plain">公开</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="row.canVisit"
                  text
                  type="primary"
                  size="small"
                  @click="visit(row.path)"
                >
                  访问
                </el-button>
                <el-button text type="primary" size="small" @click="copy(row.path)">
                  复制
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </template>

      <el-divider v-if="filterType === 'all'" />

      <!-- 后端 API -->
      <template v-if="filterType !== 'frontend'">
        <h2 class="section-title">
          <el-icon><Connection /></el-icon>
          后端 API（{{ filteredBackend.length }}）
        </h2>

        <el-empty
          v-if="filteredBackend.length === 0"
          description="没有匹配的后端 API"
        />

        <div v-for="group in groupedBackend" :key="group.module" class="group">
          <div class="group__header">
            <span class="group__title">{{ group.module }}</span>
            <el-tag size="small" type="info">{{ group.items.length }} 条</el-tag>
          </div>
          <el-table :data="group.items" stripe border size="small">
            <el-table-column label="方法" width="80">
              <template #default="{ row }">
                <el-tag
                  :type="row.methodColor"
                  size="small"
                  effect="dark"
                  class="method-tag"
                >
                  {{ row.method }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="完整路径" min-width="320">
              <template #default="{ row }">
                <code class="path-code">{{ row.fullPath }}</code>
              </template>
            </el-table-column>
            <el-table-column label="模块路径" min-width="220">
              <template #default="{ row }">
                <code class="path-code path-code--muted">{{ row.path }}</code>
              </template>
            </el-table-column>
            <el-table-column prop="name" label="函数名" min-width="180">
              <template #default="{ row }">
                <span class="name-cell">{{ row.name }}()</span>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="说明" min-width="240" />
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" size="small" @click="copy(row.fullPath)">
                  复制
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </template>
    </BaseCard>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Promotion, Connection } from '@element-plus/icons-vue'
import BaseCard from '@/components/base/BaseCard.vue'
import { routes } from '@/router/routes'
import { Role } from '@/types/role'

const router = useRouter()
const keyword = ref('')
const filterType = ref<'all' | 'frontend' | 'backend'>('all')
const baseURL = import.meta.env.VITE_API_BASE_URL || '/v1'

interface UrlRow {
  method: string
  methodColor: 'success' | 'warning' | 'danger' | 'info' | 'primary'
  path: string
  name: string
  title?: string
  requiresAuth?: boolean
  rolesLabel?: string
  canVisit?: boolean
}

interface ApiRow {
  method: string
  methodColor: 'success' | 'warning' | 'danger' | 'info' | 'primary'
  path: string
  fullPath: string
  name: string
  description: string
}

// 提取所有前端路由
const frontendUrls = computed<UrlRow[]>(() => {
  const list: UrlRow[] = []
  for (const r of routes) {
    // 父级路由（layout）
    if (r.children) {
      for (const c of r.children) {
        if (c.redirect) continue
        const fullPath = combinePath(r.path, c.path)
        list.push(buildFrontendRow(c as unknown as Record<string, unknown>, fullPath))
      }
    } else if (r.path) {
      // 没有 children 的顶层路由（如 /403, /404）
      list.push(buildFrontendRow(r as unknown as Record<string, unknown>, r.path))
    }
  }
  return list
})

function buildFrontendRow(c: Record<string, unknown>, fullPath: string): UrlRow {
  const meta = (c.meta as { title?: string; requiresAuth?: boolean; roles?: Role[] }) || {}
  const requiresAuth = !!meta.requiresAuth
  const roles = meta.roles || []
  return {
    method: 'PAGE',
    methodColor: 'info',
    path: fullPath,
    name: (c.name as string) || '',
    title: meta.title || '—',
    requiresAuth,
    rolesLabel: roleLabel(roles),
    canVisit: true,
  }
}

function combinePath(parent: string, child: string): string {
  if (child.startsWith('/')) return child
  if (parent.endsWith('/')) return parent + child
  return parent + '/' + child
}

function roleLabel(roles: Role[]): string {
  if (!roles || roles.length === 0) return '需登录'
  if (roles.includes(Role.ADMIN) && roles.length === 1) return '仅管理员'
  if (roles.includes(Role.AUDITOR)) return '审核员+'
  return '已登录'
}

interface ModuleGroup<T> {
  module: string
  items: T[]
}

// 前端路由按 layout 分组
const groupedFrontend = computed<ModuleGroup<UrlRow>[]>(() => {
  const map = new Map<string, UrlRow[]>()
  for (const item of filteredFrontend.value) {
    const layout = layoutOf(item.path)
    if (!map.has(layout)) map.set(layout, [])
    map.get(layout)!.push(item)
  }
  return Array.from(map.entries()).map(([module, items]) => ({ module, items }))
})

function layoutOf(path: string): string {
  if (path.startsWith('/admin')) return '管理后台 (AdminLayout)'
  if (path.startsWith('/auth')) return '鉴权 (BlankLayout)'
  if (path.includes(':pathMatch')) return '兜底路由'
  return '门户 (DefaultLayout)'
}

const filteredFrontend = computed(() => {
  return frontendUrls.value.filter((r) => matchKeyword(r.path, r.name, r.title))
})

// 后端 API 列表（与 src/api/*.ts 一一对应）
const backendApis: ApiRow[] = ([
  // auth
  { method: 'GET', methodColor: 'success', path: '/auth/captcha', name: 'getCaptcha', description: '获取图形验证码' },
  { method: 'POST', methodColor: 'warning', path: '/auth/login', name: 'login', description: '登录' },
  { method: 'POST', methodColor: 'warning', path: '/auth/register', name: 'register', description: '注册' },
  { method: 'POST', methodColor: 'warning', path: '/auth/refresh', name: 'refreshToken', description: '刷新 Access Token' },
  { method: 'POST', methodColor: 'warning', path: '/auth/logout', name: 'logout', description: '注销' },
  // user
  { method: 'GET', methodColor: 'success', path: '/user/profile', name: 'getProfile', description: '获取个人信息' },
  { method: 'PUT', methodColor: 'primary', path: '/user/profile', name: 'updateProfile', description: '修改个人信息' },
  // dict
  { method: 'GET', methodColor: 'success', path: '/dict/chars', name: 'pageChars', description: '分页查询字符字典' },
  { method: 'GET', methodColor: 'success', path: '/dict/chars/list', name: 'listChars', description: '按分类列出字符' },
  // sample
  { method: 'POST', methodColor: 'warning', path: '/sample/upload', name: 'uploadSample', description: '上传样本（元数据）' },
  { method: 'GET', methodColor: 'success', path: '/sample/page', name: 'pageMySamples', description: '分页查询我的样本' },
  { method: 'GET', methodColor: 'success', path: '/sample/{id}', name: 'getSample', description: '样本详情' },
  { method: 'DELETE', methodColor: 'danger', path: '/sample/{id}', name: 'deleteSample', description: '删除样本' },
  // file
  { method: 'POST', methodColor: 'warning', path: '/file/sign', name: 'getUploadSign', description: '获取对象存储直传签名' },
  // audit
  { method: 'GET', methodColor: 'success', path: '/audit/pending', name: 'pendingAudits', description: '待审核列表' },
  { method: 'GET', methodColor: 'success', path: '/audit/history', name: 'auditHistory', description: '审核历史' },
  { method: 'POST', methodColor: 'warning', path: '/audit/{id}/approve', name: 'approve', description: '审核通过' },
  { method: 'POST', methodColor: 'warning', path: '/audit/{id}/reject', name: 'reject', description: '审核驳回' },
  { method: 'POST', methodColor: 'warning', path: '/audit/batch', name: 'batchAudit', description: '批量审核（前端循环）' },
  // stats
  { method: 'GET', methodColor: 'success', path: '/stats/overview', name: 'overview', description: '总览数据' },
  { method: 'GET', methodColor: 'success', path: '/stats/trend', name: 'trend', description: '样本趋势（默认近 30 天）' },
  { method: 'GET', methodColor: 'success', path: '/stats/status-distribution', name: 'fetchStatusDistribution', description: '状态分布（前端扩展）' },
  { method: 'GET', methodColor: 'success', path: '/stats/top-contributors', name: 'fetchTopContributors', description: '贡献者排行（前端扩展）' },
  { method: 'GET', methodColor: 'success', path: '/stats/dict-progress', name: 'fetchDictProgress', description: '字符采集进度（前端扩展）' },
] satisfies Array<Omit<ApiRow, 'fullPath'>>).map((a) => ({ ...a, fullPath: baseURL + a.path }))

const filteredBackend = computed(() => {
  return backendApis.filter((a) => matchKeyword(a.fullPath, a.name, a.description))
})

const groupedBackend = computed<ModuleGroup<ApiRow>[]>(() => {
  const map = new Map<string, ApiRow[]>()
  for (const item of filteredBackend.value) {
    const module = moduleOf(item.path)
    if (!map.has(module)) map.set(module, [])
    map.get(module)!.push(item)
  }
  return Array.from(map.entries()).map(([module, items]) => ({ module, items }))
})

function moduleOf(path: string): string {
  const seg = path.split('/').filter(Boolean)[0] || 'other'
  const map: Record<string, string> = {
    auth: '鉴权 (auth)',
    user: '用户 (user)',
    dict: '字典 (dict)',
    sample: '样本 (sample)',
    file: '文件 (file)',
    audit: '审核 (audit)',
    stats: '统计 (stats)',
  }
  return map[seg] || seg
}

function matchKeyword(...fields: Array<string | undefined>): boolean {
  const k = keyword.value.trim().toLowerCase()
  if (!k) return true
  return fields.some((f) => (f || '').toLowerCase().includes(k))
}

const authRequiredCount = computed(
  () => frontendUrls.value.filter((r) => r.requiresAuth).length
)
const adminOnlyCount = computed(
  () => frontendUrls.value.filter((r) => r.rolesLabel === '仅管理员').length
)

function visit(path: string) {
  // 兜底路由不能直接访问
  if (path.includes(':pathMatch')) return
  router.push(path).catch(() => undefined)
}

async function copy(text: string) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(`已复制：${text}`)
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}
</script>

<style lang="scss" scoped>
.urls-view {
  max-width: 1280px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: $spacing-md;

  &__intro {
    color: $text-secondary;
    font-size: $font-size-sm;
    line-height: $line-height-relaxed;
    margin: 0;
    code {
      background: $bg-muted;
      color: $color-primary-dark;
      padding: 2px 8px;
      border-radius: $radius-sm;
      font-family: $font-family-mono;
      font-size: $font-size-sm;
    }
  }
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: $spacing-md;
}

.stat-card {
  background: $bg-elevated;
  border: 1px solid $border-light;
  border-radius: $radius-lg;
  padding: $spacing-md $spacing-lg;
  transition:
    transform $transition-fast,
    box-shadow $transition-fast,
    border-color $transition-fast;

  &:hover {
    transform: translateY(-2px);
    box-shadow: $shadow-md;
    border-color: $color-primary-lighter;
  }

  &__label {
    font-size: $font-size-sm;
    color: $text-secondary;
    margin-bottom: 4px;
  }
  &__value {
    font-size: 28px;
    font-weight: 700;
    @include text-gradient;
    font-family: $font-family-mono;
  }
}

.section {
  width: 100%;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  flex-wrap: wrap;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: $font-size-lg;
  font-weight: 600;
  color: $text-primary;
  margin: 0 0 $spacing-md;
  padding-left: 6px;
  border-left: 3px solid $color-primary;
  line-height: 1;
}

.group {
  margin-bottom: $spacing-lg;

  &__header {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    margin-bottom: $spacing-sm;
    padding: $spacing-xs $spacing-sm;
    background: $bg-muted;
    border-radius: $radius-sm;
  }
  &__title {
    font-size: $font-size-sm;
    font-weight: 600;
    color: $color-primary-dark;
    letter-spacing: 0.3px;
  }
}

.method-tag {
  font-family: $font-family-mono;
  font-weight: 600;
  letter-spacing: 0.5px;
  min-width: 64px;
  text-align: center;
}

.path-code {
  font-family: $font-family-mono;
  font-size: $font-size-sm;
  color: $color-primary-dark;
  background: $bg-muted;
  padding: 2px 8px;
  border-radius: $radius-sm;
  word-break: break-all;
  display: inline-block;

  &--muted {
    color: $text-secondary;
    background: $bg-base;
  }
}

.name-cell {
  font-family: $font-family-mono;
  font-size: $font-size-sm;
  color: $text-regular;
}
</style>

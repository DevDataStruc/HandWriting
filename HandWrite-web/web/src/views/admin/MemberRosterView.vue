<template>
  <div class="crew-roster">
    <div class="roster-panel">
      <header class="roster-panel__top">
        <h2>成员管理</h2>
        <div class="roster-panel__tools">
          <div class="search-box">
            <span class="search-box__icon">🔍</span>
            <input
              v-model="keyword"
              type="text"
              placeholder="搜索用户名/邮箱..."
              class="search-box__input"
            />
          </div>
          <button class="action-btn action-btn--primary" @click="handleQuery">查询</button>
        </div>
      </header>

      <div class="roster-grid">
        <div v-for="user in filteredUsers" :key="user.id" class="roster-card">
          <div class="roster-card__avatar">
            <div class="avatar-circle" :style="{ background: user.avatarColor }">
              {{ user.name.charAt(0) }}
            </div>
          </div>
          <div class="roster-card__info">
            <div class="roster-card__name">{{ user.name }}</div>
            <div class="roster-card__handle">@{{ user.username }}</div>
            <div class="roster-card__email">{{ user.email }}</div>
          </div>
          <div class="roster-card__meta">
            <div class="meta-row">
              <span class="meta-label">角色</span>
              <div class="role-badges">
                <span
                  v-for="role in user.roles"
                  :key="role"
                  class="role-badge"
                  :class="'role-badge--' + role"
                >
                  {{ roleLabel(role) }}
                </span>
              </div>
            </div>
            <div class="meta-row">
              <span class="meta-label">贡献</span>
              <span class="meta-value">{{ user.contributions }} 样本</span>
            </div>
            <div class="meta-row">
              <span class="meta-label">状态</span>
              <span
                class="status-dot"
                :class="user.active ? 'status-dot--active' : 'status-dot--inactive'"
              ></span>
              <span class="meta-value">{{ user.active ? '活跃' : '禁用' }}</span>
            </div>
            <div class="meta-row">
              <span class="meta-label">加入</span>
              <span class="meta-value">{{ user.joined }}</span>
            </div>
          </div>
          <div class="roster-card__actions">
            <button class="icon-btn" title="编辑" @click="handleEdit(user)">✏️</button>
            <button
              class="icon-btn"
              :class="user.active ? 'icon-btn--danger' : 'icon-btn--success'"
              :title="user.active ? '禁用' : '启用'"
              @click="handleToggle(user)"
            >
              {{ user.active ? '🚫' : '✅' }}
            </button>
          </div>
        </div>
      </div>

      <div v-if="filteredUsers.length === 0" class="empty-state">
        <div class="empty-state__icon">👤</div>
        <p class="empty-state__text">未找到匹配的成员</p>
      </div>

      <footer class="roster-panel__footer">
        <div class="page-info">共 {{ filteredUsers.length }} 位成员</div>
        <div class="page-controls">
          <button class="page-btn" :disabled="currentPage <= 1" @click="currentPage--">‹</button>
          <span
            v-for="p in totalPages"
            :key="p"
            class="page-num"
            :class="{ 'page-num--active': p === currentPage }"
            @click="currentPage = p"
            >{{ p }}</span
          >
          <button class="page-btn" :disabled="currentPage >= totalPages" @click="currentPage++">
            ›
          </button>
        </div>
      </footer>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 成员管理（管理员）
 *
 * 数据来源（与 HandWrite-client api2.md §管理员-用户管理 对齐）：
 *   - 用户列表  → GET   /v1/admin/users?keyword=&pageNum=&pageSize=
 *   - 启用/禁用 → PATCH /v1/admin/users/{id}/status
 *
 * 字段映射说明：
 *   - name          ← nickname || username
 *   - avatarColor   ← 基于 userId 在 10 色调色板上的稳定散列
 *   - roles         ← 后端 USER/AUDITOR/ADMIN 映射到小写 admin/editor/viewer（与 CSS 类名一致）
 *   - active        ← statusExt === 'active'
 *   - joined        ← createTime 取前 10 位（YYYY-MM-DD）
 */
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listUsers, toggleUserStatus } from '@/api/admin'
import type { AdminUserVO } from '@/api/contracts/user'

/* ========================= 类型与常量 ========================= */
interface RosterRow {
  id: number
  name: string
  username: string
  email: string
  avatarColor: string
  roles: Array<'admin' | 'editor' | 'viewer'>
  contributions: number
  active: boolean
  joined: string
}

const ROLE_DISPLAY: Record<string, 'admin' | 'editor' | 'viewer'> = {
  ADMIN: 'admin',
  AUDITOR: 'editor',
  USER: 'viewer',
}

const AVATAR_PALETTE = [
  '#0d9488',
  '#38bdf8',
  '#22c55e',
  '#f59e0b',
  '#ef4444',
  '#8b5cf6',
  '#ec4899',
  '#14b8a6',
  '#f97316',
  '#06b6d4',
]

/* ========================= 状态 ========================= */
const keyword = ref('')
const currentPage = ref(1)
const pageSize = 12
const total = ref(0)

const users = ref<RosterRow[]>([])
const loading = ref(false)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

/* ========================= 工具函数 ========================= */
function pickAvatarColor(id: number): string {
  return AVATAR_PALETTE[Math.abs(id) % AVATAR_PALETTE.length]
}

function normalizeRoles(raw: string[] | undefined): Array<'admin' | 'editor' | 'viewer'> {
  if (!raw || raw.length === 0) return ['viewer']
  return raw.map((r) => ROLE_DISPLAY[(r || '').toUpperCase()] ?? 'viewer')
}

function formatDate(iso: string | undefined): string {
  if (!iso) return '—'
  // 既兼容 "2024-03-15" 也兼容 "2024-03-15T12:00:00Z"
  return iso.length >= 10 ? iso.substring(0, 10) : iso
}

function roleLabel(role: 'admin' | 'editor' | 'viewer'): string {
  const map = { admin: '管理员', editor: '编辑', viewer: '观察者' }
  return map[role] ?? role
}

function mapAdminUserVO(vo: AdminUserVO): RosterRow {
  return {
    id: vo.id,
    name: vo.nickname || vo.username || `user#${vo.id}`,
    username: vo.username,
    email: vo.email || '',
    avatarColor: pickAvatarColor(vo.id),
    roles: normalizeRoles(vo.roles),
    contributions: Number(vo.sampleCount ?? 0),
    active: vo.statusExt === 'active',
    joined: formatDate(vo.createTime || vo.createdAt),
  }
}

/* ========================= 数据加载 ========================= */
async function loadUsers() {
  loading.value = true
  try {
    const res = await listUsers({
      keyword: keyword.value || undefined,
      pageNum: currentPage.value,
      pageSize,
    })
    users.value = (res.list ?? []).map(mapAdminUserVO)
    total.value = Number(res.total ?? 0)
  } catch (err) {
    console.error('[MemberRoster] 加载用户列表失败', err)
    users.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/* ========================= 用户操作 ========================= */
function handleQuery() {
  currentPage.value = 1
  loadUsers()
}

function handleEdit(user: RosterRow) {
  ElMessage.info(`编辑成员：${user.name}（暂未实现）`)
}

async function handleToggle(user: RosterRow) {
  const nextActive = !user.active
  const action = nextActive ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确认${action}成员「${user.name}」？`, `${action}确认`, {
      type: 'warning',
      confirmButtonText: `确认${action}`,
      cancelButtonText: '取消',
    })
  } catch {
    return // 用户取消
  }
  try {
    const updated = await toggleUserStatus(user.id)
    const mapped = mapAdminUserVO(updated)
    const idx = users.value.findIndex((u) => u.id === user.id)
    if (idx >= 0) users.value[idx] = mapped
    ElMessage.success(`${action}成功：${user.name}`)
  } catch (err) {
    console.error('[MemberRoster] 切换用户状态失败', err)
    ElMessage.error(`${action}失败：${user.name}`)
  }
}

/* ========================= 模板引用保持兼容 ========================= */
/**
 * 模板使用 `filteredUsers` 作为展示列表。这里：
 *   - 关键字过滤由后端承担（请求里带 keyword）
 *   - 这里再加一层客户端兜底过滤，兼容后端忽略 keyword 或大小写差异
 */
const filteredUsers = computed(() => {
  if (!keyword.value) return users.value
  const kw = keyword.value.toLowerCase()
  return users.value.filter(
    (u) =>
      u.name.toLowerCase().includes(kw) ||
      u.username.toLowerCase().includes(kw) ||
      u.email.toLowerCase().includes(kw)
  )
})

// 关键字变更时回到第 1 页
watch(keyword, () => {
  currentPage.value = 1
})

// 页码变化触发重新加载
watch(currentPage, () => {
  loadUsers()
})

onMounted(loadUsers)
</script>

<style scoped>
.crew-roster {
  padding: 0;
}

.roster-panel {
  background: var(--bg-card);
  border-radius: 14px;
  padding: 24px;
  transition: background 0.3s ease;
}

.roster-panel__top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
}

.roster-panel__top h2 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.roster-panel__tools {
  display: flex;
  gap: 12px;
  align-items: center;
}

.search-box {
  position: relative;
  display: flex;
  align-items: center;
}

.search-box__icon {
  position: absolute;
  left: 12px;
  font-size: 14px;
  pointer-events: none;
}

.search-box__input {
  width: 240px;
  height: 40px;
  padding: 0 12px 0 36px;
  background: var(--bg-base);
  border: none;
  border-radius: 8px;
  color: var(--text-primary);
  font-size: 14px;
  outline: none;
  transition: background 0.2s;
}

.search-box__input:focus {
  background: var(--bg-hover);
}

.search-box__input::placeholder {
  color: var(--text-dim);
}

.action-btn {
  height: 40px;
  padding: 0 20px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn--primary {
  background: var(--accent-green);
  color: var(--text-on-accent);
}

.action-btn--primary:hover {
  background: var(--accent-green-dark);
}

.roster-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.roster-card {
  background: var(--bg-base);
  border-radius: 12px;
  padding: 20px;
  transition:
    transform 0.2s,
    box-shadow 0.2s;
}

.roster-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
}

.roster-card__avatar {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.avatar-circle {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
  color: #fff;
}

.roster-card__info {
  text-align: center;
  margin-bottom: 16px;
}

.roster-card__name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.roster-card__handle {
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 4px;
}

.roster-card__email {
  font-size: 12px;
  color: var(--text-dim);
}

.roster-card__meta {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
  padding: 12px;
  background: var(--bg-card);
  border-radius: 8px;
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.meta-label {
  color: var(--text-dim);
  min-width: 48px;
}

.meta-value {
  color: var(--text-faint);
}

.role-badges {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.role-badge {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
}

.role-badge--admin {
  background: var(--accent-red-soft);
  color: var(--accent-red);
}

.role-badge--editor {
  background: var(--accent-green-soft);
  color: var(--accent-green);
}

.role-badge--viewer {
  background: var(--accent-blue-soft);
  color: var(--accent-blue);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-dot--active {
  background: var(--accent-green);
}

.status-dot--inactive {
  background: var(--text-dim);
}

.roster-card__actions {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.icon-btn {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 8px;
  background: var(--bg-card);
  cursor: pointer;
  font-size: 16px;
  transition: all 0.2s;
}

.icon-btn:hover {
  background: var(--bg-hover);
}

.icon-btn--danger:hover {
  background: var(--accent-red-soft);
}

.icon-btn--success:hover {
  background: var(--accent-green-soft);
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.empty-state__icon {
  font-size: 48px;
  margin-bottom: 12px;
  opacity: 0.5;
}

.empty-state__text {
  font-size: 14px;
  color: var(--text-dim);
}

.roster-panel__footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 20px;
  border-top: 1px solid var(--border-base);
}

.page-info {
  font-size: 13px;
  color: var(--text-muted);
}

.page-controls {
  display: flex;
  gap: 6px;
  align-items: center;
}

.page-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 6px;
  background: var(--bg-base);
  color: var(--text-muted);
  font-size: 18px;
  cursor: pointer;
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-num {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  font-size: 13px;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.2s;
}

.page-num:hover {
  background: var(--bg-base);
}

.page-num--active {
  background: var(--accent-green);
  color: var(--text-on-accent);
  font-weight: 600;
}
</style>

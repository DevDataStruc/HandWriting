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

    <!-- 编辑身份弹窗 -->
    <transition name="pop">
      <div v-if="showEditModal" class="edit-mask" @click.self="closeEditModal">
        <div class="edit-dialog">
          <header class="edit-dialog__head">
            <h3>编辑成员身份</h3>
            <button class="edit-dialog__close" @click="closeEditModal">✕</button>
          </header>

          <div class="edit-dialog__body">
            <div class="edit-target">
              <div
                class="avatar-circle avatar-circle--sm"
                :style="{ background: editTarget?.avatarColor }"
              >
                {{ editTarget?.name?.charAt(0) }}
              </div>
              <div class="edit-target__info">
                <div class="edit-target__name">{{ editTarget?.name }}</div>
                <div class="edit-target__handle">@{{ editTarget?.username }}</div>
              </div>
            </div>

            <div class="edit-section">
              <span class="edit-section__label">分配角色（可多选）</span>
              <div class="role-options">
                <label
                  v-for="opt in ROLE_OPTIONS"
                  :key="opt.key"
                  class="role-option"
                  :class="{ 'is-checked': editRoles.includes(opt.key) }"
                >
                  <input
                    type="checkbox"
                    :value="opt.key"
                    :checked="editRoles.includes(opt.key)"
                    @change="toggleEditRole(opt.key)"
                  />
                  <span class="role-option__dot" :class="'role-option__dot--' + opt.key"></span>
                  <span class="role-option__name">{{ opt.label }}</span>
                  <span class="role-option__hint">{{ opt.hint }}</span>
                </label>
              </div>
            </div>
          </div>

          <footer class="edit-dialog__foot">
            <button class="pill-btn" @click="closeEditModal">取消</button>
            <button class="pill-btn pill-btn--go" :disabled="saving" @click="saveEdit">
              {{ saving ? '保存中...' : '保存' }}
            </button>
          </footer>
        </div>
      </div>
    </transition>
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
import { listUsers, toggleUserStatus, updateUserRoles } from '@/api/admin'
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

/** 反向映射：本地小写 key → 后端角色编码 */
const ROLE_BACKEND: Record<'admin' | 'editor' | 'viewer', 'ADMIN' | 'AUDITOR' | 'USER'> = {
  admin: 'ADMIN',
  editor: 'AUDITOR',
  viewer: 'USER',
}

/** 编辑弹窗中的可选角色 */
const ROLE_OPTIONS: Array<{
  key: 'admin' | 'editor' | 'viewer'
  label: string
  hint: string
}> = [
  { key: 'admin', label: '管理员', hint: '拥有全部后台权限' },
  { key: 'editor', label: '审核员', hint: '可处理待审核样本' },
  { key: 'viewer', label: '观察者', hint: '只读访问' },
]

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

/* ========================= 编辑弹窗状态 ========================= */
const showEditModal = ref(false)
const editTarget = ref<RosterRow | null>(null)
const editRoles = ref<Array<'admin' | 'editor' | 'viewer'>>([])
const saving = ref(false)

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
  editTarget.value = user
  editRoles.value = [...user.roles]
  showEditModal.value = true
}

function closeEditModal() {
  showEditModal.value = false
  editTarget.value = null
  editRoles.value = []
}

function toggleEditRole(role: 'admin' | 'editor' | 'viewer') {
  if (editRoles.value.includes(role)) {
    editRoles.value = editRoles.value.filter((r) => r !== role)
  } else {
    editRoles.value = [...editRoles.value, role]
  }
}

async function saveEdit() {
  if (!editTarget.value) return
  if (editRoles.value.length === 0) {
    ElMessage.warning('请至少分配一个角色')
    return
  }
  // 把本地小写 key 转成后端大写编码
  const backendRoles = editRoles.value.map((r) => ROLE_BACKEND[r])
  saving.value = true
  try {
    const updated = await updateUserRoles(editTarget.value.id, backendRoles)
    const mapped = mapAdminUserVO(updated)
    const idx = users.value.findIndex((u) => u.id === editTarget.value!.id)
    if (idx >= 0) users.value[idx] = mapped
    ElMessage.success(`已更新「${editTarget.value.name}」的身份`)
    closeEditModal()
  } catch (err) {
    console.error('[MemberRoster] 更新用户角色失败', err)
    ElMessage.error('更新身份失败：后端可能尚未提供 PUT /v1/admin/users/{id}/roles')
  } finally {
    saving.value = false
  }
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

/* === 编辑身份弹窗 === */
.edit-mask {
  position: fixed;
  inset: 0;
  background: var(--overlay-mask);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.edit-dialog {
  background: var(--bg-card);
  border-radius: 14px;
  width: 480px;
  max-width: 90vw;
  box-shadow: var(--shadow-md);
  overflow: hidden;
}

.edit-dialog__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 22px;
  border-bottom: 1px solid var(--border-base);
}

.edit-dialog__head h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.edit-dialog__close {
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  color: var(--text-muted);
  font-size: 16px;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s;
}
.edit-dialog__close:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.edit-dialog__body {
  padding: 20px 22px;
}

.edit-target {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--bg-base);
  border-radius: 10px;
  margin-bottom: 18px;
}

.avatar-circle--sm {
  width: 40px;
  height: 40px;
  font-size: 16px;
  flex-shrink: 0;
}

.edit-target__info {
  min-width: 0;
}

.edit-target__name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.edit-target__handle {
  font-size: 12px;
  color: var(--text-muted);
}

.edit-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.edit-section__label {
  font-size: 13px;
  color: var(--text-muted);
  font-weight: 500;
}

.role-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.role-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  background: var(--bg-base);
  border: 1px solid transparent;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}
.role-option:hover {
  background: var(--bg-hover);
}
.role-option.is-checked {
  background: var(--bg-soft);
  border-color: var(--accent-green);
}
.role-option input {
  display: none;
}

.role-option__dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}
.role-option__dot--admin {
  background: var(--accent-red);
}
.role-option__dot--editor {
  background: var(--accent-green);
}
.role-option__dot--viewer {
  background: var(--accent-blue);
}

.role-option__name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.role-option__hint {
  font-size: 12px;
  color: var(--text-dim);
  margin-left: auto;
}

.edit-dialog__foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 22px;
  border-top: 1px solid var(--border-base);
}

.pill-btn {
  height: 36px;
  padding: 0 18px;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  background: var(--bg-hover);
  color: var(--text-faint);
}
.pill-btn:hover:not(:disabled) {
  background: var(--bg-elevated);
}
.pill-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.pill-btn--go {
  background: var(--accent-green);
  color: var(--text-on-accent);
}
.pill-btn--go:hover:not(:disabled) {
  filter: brightness(0.92);
}

.pop-enter-active,
.pop-leave-active {
  transition: all 0.25s ease;
}
.pop-enter-from,
.pop-leave-to {
  opacity: 0;
  transform: scale(0.95);
}
</style>

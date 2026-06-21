<template>
  <div class="console-layout" :class="[{ 'is-folded': isFolded, 'is-light': !isDark }]">
    <!-- 侧边栏 -->
    <aside class="console-sidebar">
      <div class="console-sidebar__brand">
        <div class="brand-mark">
          <SvgIcon name="trend-up" :size="18" color="#fff" />
        </div>
        <span class="brand-text">管理控制台</span>
      </div>

      <nav class="console-sidebar__menu">
        <button
          v-for="item in menuItems"
          :key="item.key"
          class="menu-entry"
          :class="{ 'menu-entry--active': activeKey === item.key }"
          @click="onMenuClick(item.key)"
        >
          <span class="menu-entry__glyph"><SvgIcon :name="item.glyph" :size="18" /></span>
          <span class="menu-entry__label">{{ item.label }}</span>
          <span v-if="getBadge(item.key)" class="menu-entry__badge">{{ getBadge(item.key) }}</span>
        </button>
      </nav>

      <div class="console-sidebar__foot">
        <button class="foot-entry" @click="isFolded = !isFolded">
          <span class="foot-entry__glyph">
            <SvgIcon :name="isFolded ? 'chevron-right' : 'chevron-left'" :size="16" />
          </span>
          <span class="foot-entry__label">收起侧边栏</span>
        </button>
      </div>
    </aside>

    <!-- 右侧内容区 -->
    <div class="console-layout__right">
      <!-- 顶部栏 -->
      <header class="console-topbar">
        <div class="console-topbar__crumb">
          <span class="crumb-piece">首页</span>
          <span class="crumb-sep">/</span>
          <span class="crumb-piece crumb-piece--active">{{ activeLabel }}</span>
        </div>
        <div class="console-topbar__tools">
          <button class="tool-entry" title="刷新" @click="onReload">
            <SvgIcon name="refresh" :size="16" />
          </button>
          <button class="tool-entry" title="切换主题" @click="onToggleTheme">
            <SvgIcon :name="isDark ? 'sun' : 'moon'" :size="16" />
          </button>
          <button class="tool-entry tool-entry--notice" title="通知">
            <SvgIcon name="bell" :size="16" />
            <span class="notice-dot"></span>
          </button>
          <div class="account-trigger" @click="showAccountMenu = !showAccountMenu">
            <div class="account-avatar">A</div>
            <span class="account-name">管理员</span>
            <span class="account-arrow">
              <SvgIcon name="arrow-down" :size="10" />
            </span>
            <transition name="pop">
              <div v-if="showAccountMenu" class="account-dropdown">
                <div class="dropdown-cell" @click.stop="onBackPortal">返回门户</div>
                <div class="dropdown-cell dropdown-cell--risk" @click.stop="onLogout">退出登录</div>
              </div>
            </transition>
          </div>
        </div>
      </header>

      <!-- 主内容 -->
      <main class="console-stage">
        <transition name="stage-fade" mode="out-in">
          <component :is="activeComponent" :key="activeKey" />
        </transition>
      </main>
    </div>

    <!-- 退出确认弹窗 -->
    <transition name="pop">
      <div v-if="logoutConfirm" class="overlay-mask" @click.self="logoutConfirm = false">
        <div class="confirm-card">
          <h3 class="confirm-card__title">确认退出？</h3>
          <p class="confirm-card__desc">退出后需要重新登录才能进入管理后台。</p>
          <div class="confirm-card__actions">
            <button class="act-btn" @click="logoutConfirm = false">取消</button>
            <button class="act-btn act-btn--danger" @click="onConfirmLogout">确认退出</button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { pendingAudits } from '@/api/audit'
import SvgIcon from '@/components/base/SvgIcon.vue'
import type { SvgIconName } from '@/assets/icons/svg-icons'
import CommandCenterView from './../views/admin/CommandCenterView.vue'
import MemberRosterView from './../views/admin/MemberRosterView.vue'
import ReviewWorkbenchView from './../views/admin/ReviewWorkbenchView.vue'
import DataInsightView from './../views/admin/DataInsightView.vue'
import AuditTrailView from './../views/admin/AuditTrailView.vue'
const isFolded = ref(false)
const isDark = ref(true)
const showAccountMenu = ref(false)
const logoutConfirm = ref(false)
const activeKey = ref('command')

// 待审核数量（驱动侧边栏 badge）
const pendingTotal = ref(0)

async function loadPendingTotal() {
  try {
    const res = await pendingAudits({ pageNum: 1, pageSize: 1 })
    pendingTotal.value = Number(res.total ?? 0)
  } catch (err) {
    console.error('[AdminLayout] 加载待审核数量失败', err)
    pendingTotal.value = 0
  }
}

onMounted(() => {
  const saved = localStorage.getItem('admin-theme')
  if (saved === 'light') isDark.value = false
  else if (saved === 'dark') isDark.value = true
  loadPendingTotal()
})

watch(isDark, (val) => {
  localStorage.setItem('admin-theme', val ? 'dark' : 'light')
})


const menuItems: { key: string; label: string; glyph: SvgIconName }[] = [
  { key: 'command', label: '控制台', glyph: 'dashboard' },
  { key: 'members', label: '成员管理', glyph: 'users' },
  { key: 'review', label: '审核工作台', glyph: 'audit' },
  { key: 'insight', label: '数据洞察', glyph: 'trend-up' },
  { key: 'audit', label: '审核日志', glyph: 'clipboard' },
]

type MenuKey = 'command' | 'members' | 'review' | 'insight' | 'audit'

const componentMap: Record<MenuKey, unknown> = {
  command: CommandCenterView,
  members: MemberRosterView,
  review: ReviewWorkbenchView,
  insight: DataInsightView,
  audit: AuditTrailView,
}

const activeLabel = computed(() => menuItems.find(i => i.key === activeKey.value)?.label || '')
const activeComponent = computed(() => componentMap[activeKey.value as MenuKey])

// 各菜单项的 badge：审核工作台显示未审核数量，其他为空
function getBadge(key: string) {
  if (key === 'review') return pendingTotal.value > 0 ? pendingTotal.value : ''
  return ''
}

function onMenuClick(key: string) {
  activeKey.value = key
  // 切换菜单时刷新未审核数，确保审核后 badge 同步
  loadPendingTotal()
}

function onReload() {
  window.location.reload()
}

function onToggleTheme() {
  isDark.value = !isDark.value
}

function onBackPortal() {
  showAccountMenu.value = false
  if (window.confirm('确定返回门户首页？')) {
    window.location.href = '/'
  }
}

function onLogout() {
  showAccountMenu.value = false
  logoutConfirm.value = true
}

function onConfirmLogout() {
  if (logoutConfirm.value) {
    try {
    localStorage.removeItem('hwtoken')
  } catch { /* empty */ }
  window.location.href = '/auth/login'

}
}
</script>

<style>
/* === 全局主题变量（暗色为默认） === */
.console-layout {
  --bg-darkest: #0B1120;
  --bg-base: #0F172A;
  --bg-card: #1E293B;
  --bg-hover: #334155;
  --bg-elevated: #475569;
  --bg-soft: #1a2740;
  --border-base: #334155;
  --text-primary: #F8FAFC;
  --text-regular: #E2E8F0;
  --text-muted: #94A3B8;
  --text-dim: #64748B;
  --text-faint: #CBD5E1;
  --text-on-accent: #0F172A;
  --accent-green: #22C55E;
  --accent-green-dark: #16A34A;
  --accent-blue: #38BDF8;
  --accent-amber: #F59E0B;
  --accent-red: #EF4444;
  --accent-teal: #0d9488;
  --accent-green-soft: rgba(34, 197, 94, 0.12);
  --accent-green-soft-hover: rgba(34, 197, 94, 0.25);
  --accent-red-soft: rgba(239, 68, 68, 0.15);
  --accent-red-soft-hover: rgba(239, 68, 68, 0.25);
  --accent-amber-soft: rgba(245, 158, 11, 0.15);
  --accent-amber-soft-hover: rgba(245, 158, 11, 0.25);
  --accent-blue-soft: rgba(56, 189, 248, 0.15);
  --accent-blue-soft-hover: rgba(56, 189, 248, 0.25);
  --accent-teal-soft: rgba(13, 148, 136, 0.15);
  --accent-teal-soft-hover: rgba(13, 148, 136, 0.25);
  --hover-overlay: rgba(255, 255, 255, 0.03);
  --table-divider: rgba(51, 65, 85, 0.5);
  --overlay-mask: rgba(0, 0, 0, 0.5);
  --shadow-sm: 0 2px 8px rgba(0, 0, 0, 0.2);
  --shadow-md: 0 8px 24px rgba(0, 0, 0, 0.25);
  --shadow-lg: 0 8px 24px rgba(0, 0, 0, 0.3);
  --scrollbar-thumb: var(--bg-hover);
}

/* === 亮色主题变量 === */
.console-layout.is-light {
  --bg-darkest: #F8FAFC;
  --bg-base: #F1F5F9;
  --bg-card: #FFFFFF;
  --bg-hover: #E2E8F0;
  --bg-elevated: #CBD5E1;
  --bg-soft: #F1F5F9;
  --border-base: #E2E8F0;
  --text-primary: #0F172A;
  --text-regular: #334155;
  --text-muted: #64748B;
  --text-dim: #94A3B8;
  --text-faint: #475569;
  --text-on-accent: #FFFFFF;
  --accent-green: #16A34A;
  --accent-green-dark: #15803D;
  --accent-blue: #0284C7;
  --accent-amber: #D97706;
  --accent-red: #DC2626;
  --accent-teal: #0F766E;
  --accent-green-soft: rgba(22, 163, 74, 0.10);
  --accent-green-soft-hover: rgba(22, 163, 74, 0.20);
  --accent-red-soft: rgba(220, 38, 38, 0.10);
  --accent-red-soft-hover: rgba(220, 38, 38, 0.18);
  --accent-amber-soft: rgba(217, 119, 6, 0.10);
  --accent-amber-soft-hover: rgba(217, 119, 6, 0.20);
  --accent-blue-soft: rgba(2, 132, 199, 0.10);
  --accent-blue-soft-hover: rgba(2, 132, 199, 0.20);
  --accent-teal-soft: rgba(15, 118, 110, 0.10);
  --accent-teal-soft-hover: rgba(15, 118, 110, 0.20);
  --hover-overlay: rgba(15, 23, 42, 0.04);
  --table-divider: rgba(203, 213, 225, 0.6);
  --overlay-mask: rgba(15, 23, 42, 0.35);
  --shadow-sm: 0 2px 8px rgba(15, 23, 42, 0.05);
  --shadow-md: 0 8px 24px rgba(15, 23, 42, 0.08);
  --shadow-lg: 0 8px 24px rgba(15, 23, 42, 0.12);
  --scrollbar-thumb: #CBD5E1;
}
</style>

<style scoped>
.console-layout {
  display: flex;
  min-height: 100vh;
  background: var(--bg-base);
  color: var(--text-primary);
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
  transition: background 0.3s ease, color 0.3s ease;
}

.console-sidebar,
.console-topbar,
.console-stage,
.account-dropdown,
.confirm-card,
.tool-entry,
.account-trigger,
.menu-entry,
.foot-entry {
  transition: background 0.3s ease, color 0.3s ease, border-color 0.3s ease;
}

/* === 侧边栏 === */
.console-sidebar {
  width: 240px;
  background: var(--bg-card);
  border-right: 1px solid var(--border-base);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  transition: width 0.25s ease;
}

.console-layout.is-folded .console-sidebar {
  width: 72px;
}

.console-sidebar__brand {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 20px;
  border-bottom: 1px solid var(--border-base);
  overflow: hidden;
}

.brand-mark {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, var(--accent-green), var(--accent-green-dark));
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.brand-text {
  font-size: 16px;
  font-weight: 700;
  white-space: nowrap;
  opacity: 1;
  transition: opacity 0.2s;
}

.console-layout.is-folded .brand-text {
  opacity: 0;
}

.console-sidebar__menu {
  flex: 1;
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  overflow-y: auto;
}

.menu-entry {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: transparent;
  border: none;
  border-radius: 8px;
  color: var(--text-muted);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  text-align: left;
  position: relative;
  overflow: hidden;
}

.menu-entry:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.menu-entry--active {
  background: var(--accent-green-soft);
  color: var(--accent-green);
}

.menu-entry__glyph {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  line-height: 0;
}

.menu-entry__label {
  flex: 1;
  white-space: nowrap;
  opacity: 1;
  transition: opacity 0.2s;
}

.console-layout.is-folded .menu-entry__label {
  opacity: 0;
}

.menu-entry__badge {
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  background: var(--accent-red);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  opacity: 1;
  transition: opacity 0.2s;
}

.console-layout.is-folded .menu-entry__badge {
  position: absolute;
  top: 8px;
  right: 8px;
  min-width: 8px;
  height: 8px;
  padding: 0;
  border-radius: 50%;
}

.console-sidebar__foot {
  padding: 12px;
  border-top: 1px solid var(--border-base);
}

.foot-entry {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  background: transparent;
  border: none;
  border-radius: 8px;
  color: var(--text-muted);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.foot-entry:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.foot-entry__glyph {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  line-height: 0;
}

.foot-entry__label {
  white-space: nowrap;
  opacity: 1;
  transition: opacity 0.2s;
}

.console-layout.is-folded .foot-entry__label {
  opacity: 0;
}

/* === 右侧区域 === */
.console-layout__right {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

/* === 顶部栏 === */
.console-topbar {
  height: 64px;
  padding: 0 24px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-base);
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}

.console-topbar__crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.crumb-piece {
  color: var(--text-muted);
}

.crumb-piece--active {
  color: var(--text-primary);
  font-weight: 600;
}

.crumb-sep {
  color: var(--text-dim);
}

.console-topbar__tools {
  display: flex;
  align-items: center;
  gap: 12px;
}

.tool-entry {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 8px;
  background: var(--bg-base);
  color: var(--text-muted);
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.tool-entry:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.tool-entry--notice {
  position: relative;
}

.notice-dot {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 8px;
  height: 8px;
  background: var(--accent-red);
  border-radius: 50%;
}

.account-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px 4px 4px;
  background: var(--bg-base);
  border-radius: 20px;
  cursor: pointer;
  position: relative;
  transition: background 0.2s;
}

.account-trigger:hover {
  background: var(--bg-hover);
}

.account-avatar {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #0d9488, #14b8a6);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
}

.account-name {
  font-size: 13px;
  color: var(--text-regular);
}

.account-arrow {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--text-dim);
}

.account-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 140px;
  background: var(--bg-card);
  border: 1px solid var(--border-base);
  border-radius: 10px;
  box-shadow: var(--shadow-md);
  overflow: hidden;
  z-index: 50;
}

.dropdown-cell {
  padding: 12px 16px;
  font-size: 13px;
  color: var(--text-regular);
  cursor: pointer;
  transition: background 0.15s;
}

.dropdown-cell:hover {
  background: var(--bg-hover);
}

.dropdown-cell--risk:hover {
  background: var(--accent-red-soft);
  color: var(--accent-red);
}

/* === 主舞台 === */
.console-stage {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  background: var(--bg-base);
}

/* === 弹窗 === */
.overlay-mask {
  position: fixed;
  inset: 0;
  background: var(--overlay-mask);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.confirm-card {
  width: 360px;
  background: var(--bg-card);
  border-radius: 14px;
  padding: 24px;
  box-shadow: var(--shadow-md);
}

.confirm-card__title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 8px;
}

.confirm-card__desc {
  font-size: 13px;
  color: var(--text-muted);
  margin: 0 0 20px;
}

.confirm-card__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.act-btn {
  height: 36px;
  padding: 0 18px;
  border: none;
  border-radius: 8px;
  background: var(--bg-hover);
  color: var(--text-regular);
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s;
}

.act-btn:hover {
  background: var(--bg-elevated);
}

.act-btn--danger {
  background: var(--accent-red-soft);
  color: var(--accent-red);
}

.act-btn--danger:hover {
  background: var(--accent-red-soft-hover);
}

/* === 动画 === */
.stage-fade-enter-active,
.stage-fade-leave-active {
  transition: all 0.2s ease;
}
.stage-fade-enter-from,
.stage-fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

.pop-enter-active,
.pop-leave-active {
  transition: all 0.2s ease;
}
.pop-enter-from,
.pop-leave-to {
  opacity: 0;
  transform: scale(0.95);
}

/* === 滚动条 === */
.console-sidebar__menu::-webkit-scrollbar,
.console-stage::-webkit-scrollbar {
  width: 6px;
}
.console-sidebar__menu::-webkit-scrollbar-track,
.console-stage::-webkit-scrollbar-track {
  background: transparent;
}
.console-sidebar__menu::-webkit-scrollbar-thumb,
.console-stage::-webkit-scrollbar-thumb {
  background: var(--scrollbar-thumb);
  border-radius: 3px;
}
</style>
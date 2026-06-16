<template>
  <el-aside
    :width="width"
    class="app-sidebar"
    :class="{ 'is-collapsed': appStore.sidebarCollapsed }"
  >
    <div class="app-sidebar__inner">
      <div class="sidebar-brand">
        <img src="@/assets/logo.svg" alt="logo" class="logo" />
        <transition name="fade">
          <span v-show="!appStore.sidebarCollapsed" class="brand-name">手写体采集</span>
        </transition>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="appStore.sidebarCollapsed"
        :collapse-transition="false"
        class="sidebar-menu"
        background-color="transparent"
        text-color="#E2E8F0"
        active-text-color="#22C55E"
        router
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
      <div class="sidebar-footer">
        <el-button link class="collapse-btn" @click="appStore.toggleSidebar">
          <el-icon :size="18"
            ><component :is="appStore.sidebarCollapsed ? 'Expand' : 'Fold'"
          /></el-icon>
          <span v-if="!appStore.sidebarCollapsed">收起</span>
        </el-button>
      </div>
    </div>
  </el-aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { Role } from '@/types/role'

const route = useRoute()
const appStore = useAppStore()
const userStore = useUserStore()

const width = computed(() => (appStore.sidebarCollapsed ? '64px' : '220px'))

const allMenus = [
  {
    path: '/admin/dashboard',
    title: '数据看板',
    icon: 'Odometer',
    roles: [Role.AUDITOR, Role.ADMIN],
  },
  {
    path: '/admin/audit',
    title: '样本审核',
    icon: 'CircleCheck',
    roles: [Role.AUDITOR, Role.ADMIN],
  },
  { path: '/admin/users', title: '用户管理', icon: 'User', roles: [Role.ADMIN] },
  { path: '/admin/stats', title: '统计分析', icon: 'TrendCharts', roles: [Role.ADMIN] },
  { path: '/admin/logs', title: '审计日志', icon: 'Document', roles: [Role.ADMIN] },
]

const menuItems = computed(() =>
  allMenus.filter((m) => m.roles.some((r) => userStore.roles.includes(r)))
)

const activeMenu = computed(() => route.path)
</script>

<style lang="scss" scoped>
.app-sidebar {
  background: linear-gradient(180deg, $color-primary-darker 0%, #0f172a 100%);
  border-right: 1px solid rgba(255, 255, 255, 0.05);
  transition: width $transition-base;
  height: 100vh;
  position: sticky;
  top: 0;
  overflow: hidden;

  &__inner {
    @include flex-column;
    height: 100%;
  }
}

.sidebar-brand {
  @include flex-start;
  gap: $spacing-sm;
  padding: 0 $spacing-md;
  height: $header-height;

  .logo {
    width: 32px;
    height: 32px;
    flex-shrink: 0;
  }
  .brand-name {
    color: $text-inverse;
    font-weight: 700;
    font-size: $font-size-md;
    @include text-gradient($from: $color-primary-light, $to: $color-primary-lighter);
  }
}

.sidebar-menu {
  flex: 1;
  border-right: none;
  padding: $spacing-sm 0;

  :deep(.el-menu-item) {
    margin: 4px 8px;
    border-radius: $radius-md;
    height: 42px;
    line-height: 42px;

    &:hover {
      background: rgba(34, 197, 94, 0.1) !important;
    }
    &.is-active {
      background: rgba(34, 197, 94, 0.18) !important;
      color: $color-primary-lighter !important;
    }
  }
}

.sidebar-footer {
  padding: $spacing-sm $spacing-md;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  color: $text-secondary;
}

.collapse-btn {
  color: $text-secondary;
  width: 100%;
  @include flex-start;
  gap: $spacing-sm;
  &:hover {
    color: $color-primary-lighter;
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity $transition-fast;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

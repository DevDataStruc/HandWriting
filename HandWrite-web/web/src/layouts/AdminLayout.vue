<template>
  <div class="admin-layout" :class="{ 'is-collapsed': appStore.sidebarCollapsed }">
    <AppSidebar />
    <div class="admin-layout__body">
      <header class="admin-header">
        <div class="admin-header__left">
          <AppBreadcrumb />
        </div>
        <div class="admin-header__right">
          <el-tooltip content="刷新">
            <el-button link @click="reload">
              <el-icon :size="18"><Refresh /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="主题">
            <el-button link @click="toggleTheme">
              <el-icon :size="18"><component :is="appStore.theme === 'dark' ? 'Sunny' : 'Moon'" /></el-icon>
            </el-button>
          </el-tooltip>
          <el-dropdown>
            <div class="user-trigger">
              <UserAvatar :user="userStore.profile" :size="30" />
              <span class="name">{{ userStore.profile?.nickname || userStore.profile?.username }}</span>
              <el-tag v-if="userStore.isAdmin" type="success" size="small" effect="dark">ADMIN</el-tag>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$router.push('/')">返回门户</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <main class="admin-content">
        <router-view v-slot="{ Component, route }">
          <transition name="route-fade" mode="out-in">
            <component :is="Component" :key="route.fullPath" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { ThemeMode } from '@/utils/constants'
import AppSidebar from './components/AppSidebar.vue'
import AppBreadcrumb from './components/AppBreadcrumb.vue'
import UserAvatar from '@/components/business/UserAvatar.vue'

const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()

function reload() {
  router.go(0)
}

function toggleTheme() {
  appStore.setTheme(appStore.theme === ThemeMode.DARK ? ThemeMode.LIGHT : ThemeMode.DARK)
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
    await userStore.logout()
    router.push({ name: 'Login' })
  } catch {
    /* cancel */
  }
}

onMounted(() => {
  // 默认进入 dark 风格，更贴近管理后台
  if (appStore.theme !== ThemeMode.DARK) {
    // 用户可手动切换
  }
})
</script>

<style lang="scss" scoped>
.admin-layout {
  @include flex-start;
  min-height: 100vh;
  background: $bg-base;
  color: $text-primary;

  &__body {
    flex: 1;
    @include flex-column;
    min-width: 0;
  }
}

.admin-header {
  position: sticky;
  top: 0;
  z-index: $z-sticky;
  height: $header-height;
  background: $bg-elevated;
  border-bottom: 1px solid $border-base;
  padding: 0 $spacing-lg;
  @include flex-between;
  box-shadow: $shadow-xs;

  &__left {
    @include flex-start;
    gap: $spacing-md;
  }
  &__right {
    @include flex-end;
    gap: $spacing-md;
  }
}

.user-trigger {
  @include flex-start;
  gap: $spacing-sm;
  padding: 4px 8px;
  border-radius: $radius-md;
  cursor: pointer;
  &:hover {
    background: $bg-muted;
  }
  .name {
    color: $text-primary;
    font-size: $font-size-sm;
  }
}

.admin-content {
  flex: 1;
  padding: $spacing-lg;
  background: $bg-base;
}
</style>

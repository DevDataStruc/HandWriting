<template>
  <el-header class="app-header" :class="{ 'is-scrolled': scrolled }">
    <div class="app-header__inner hw-container">
      <div class="app-header__left">
        <el-button v-if="showMenu" link class="menu-toggle" @click="emit('toggle-sidebar')">
          <el-icon :size="20"><component :is="collapsed ? 'Expand' : 'Fold'" /></el-icon>
        </el-button>
        <router-link to="/" class="brand">
          <img src="@/assets/logo.svg" alt="logo" class="brand__logo" />
          <span class="brand__name">手写体采集系统</span>
        </router-link>
      </div>
      <nav class="app-header__nav">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="nav-link"
          active-class="is-active"
        >
          {{ item.label }}
        </router-link>
      </nav>
      <div class="app-header__right">
        <el-tooltip content="切换语言">
          <el-button link @click="toggleLocale">
            <span class="locale-text">{{ appStore.locale === 'zh-CN' ? 'EN' : '中' }}</span>
          </el-button>
        </el-tooltip>
        <template v-if="userStore.isLoggedIn">
          <el-dropdown trigger="click">
            <div class="user-trigger">
              <UserAvatar :user="userStore.profile" :size="32" />
              <span class="user-name">{{ userStore.profile?.nickname || userStore.profile?.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="goProfile">个人中心</el-dropdown-item>
                <el-dropdown-item @click="goMySamples">我的样本</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button type="primary" plain @click="$router.push({ name: 'Login' })">登录</el-button>
          <el-button type="primary" @click="$router.push({ name: 'Register' })">注册</el-button>
        </template>
      </div>
    </div>
  </el-header>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { ElMessageBox } from 'element-plus'
import UserAvatar from '@/components/business/UserAvatar.vue'

defineProps<{ collapsed?: boolean; showMenu?: boolean }>()
const emit = defineEmits<{ 'toggle-sidebar': [] }>()

const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

const scrolled = ref(false)
const navItems = [
  { path: '/', label: '首页' },
  { path: '/sample/collect', label: '开始书写' },
  { path: '/sample/my', label: '我的样本' },
  { path: '/notices', label: '公告' },
  { path: '/about', label: '关于' },
]

function onScroll() {
  scrolled.value = window.scrollY > 8
}

function toggleLocale() {
  appStore.setLocale(appStore.locale === 'zh-CN' ? 'en-US' : 'zh-CN')
  ElMessageBox.alert('i18n 已切换，开发模式下请在 store 中持久化', '提示', { confirmButtonText: 'OK' }).catch(() => undefined)
}

function goProfile() {
  router.push('/sample/my')
}
function goMySamples() {
  router.push('/sample/my')
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
  window.addEventListener('scroll', onScroll, { passive: true })
})
onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
})
</script>

<style lang="scss" scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: $z-fixed;
  height: $header-height;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: saturate(180%) blur(12px);
  border-bottom: 1px solid transparent;
  transition: border-color $transition-base, box-shadow $transition-base;

  &.is-scrolled {
    border-bottom-color: $border-base;
    box-shadow: $shadow-sm;
  }

  &__inner {
    height: 100%;
    @include flex-between;
  }

  &__left {
    @include flex-start;
    gap: $spacing-md;
  }

  &__nav {
    display: flex;
    gap: $spacing-lg;
    flex: 1;
    margin-left: $spacing-xl;

    @include responsive(md) {
      display: none;
    }
  }

  &__right {
    @include flex-end;
    gap: $spacing-sm;
  }
}

.brand {
  @include flex-start;
  gap: $spacing-sm;
  color: $text-primary;
  font-weight: 700;

  &__logo {
    width: 32px;
    height: 32px;
  }
  &__name {
    font-size: $font-size-md;
    @include text-gradient;
  }
}

.nav-link {
  font-size: $font-size-base;
  color: $text-regular;
  padding: $spacing-xs $spacing-sm;
  border-radius: $radius-md;
  transition: color $transition-fast, background $transition-fast;

  &:hover {
    color: $color-primary;
    background: $bg-muted;
  }
  &.is-active {
    color: $color-primary;
    font-weight: 600;
  }
}

.user-trigger {
  @include flex-start;
  gap: $spacing-sm;
  padding: 4px 8px;
  border-radius: $radius-md;
  cursor: pointer;
  transition: background $transition-fast;
  &:hover {
    background: $bg-muted;
  }
}

.user-name {
  font-size: $font-size-sm;
  color: $text-primary;
  @include ellipsis;
  max-width: 100px;
}

.locale-text {
  font-size: $font-size-sm;
  color: $text-regular;
}

.menu-toggle {
  display: none;
  @include responsive(md) {
    display: inline-flex;
  }
}
</style>

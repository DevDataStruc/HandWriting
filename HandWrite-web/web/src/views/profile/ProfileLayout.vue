<template>
  <!--
    个人中心 — 左侧导航 + 右侧内容（嵌套 router-view）
    样式仅作用于本文件（scoped），所有类以 pc- 前缀
  -->
  <div class="pc-page">
    <div class="pc-shell">
      <!-- 侧边栏 -->
      <aside class="pc-side">
        <div class="pc-side__header">
          <div class="pc-side__avatar">
            <UserAvatar v-if="profile?.avatar" :src="profile.avatar" :size="48" />
            <div v-else class="pc-side__avatar-fallback">
              {{ avatarLetter }}
            </div>
          </div>
          <div class="pc-side__user">
            <div class="pc-side__name">{{ profile?.nickname || profile?.username }}</div>
            <div class="pc-side__role">{{ roleText }}</div>
          </div>
        </div>

        <nav class="pc-nav">
          <button
            v-for="item in navItems"
            :key="item.name"
            type="button"
            class="pc-nav__item"
            :class="{ active: activeName === item.name }"
            @click="goTo(item.name)"
          >
            <el-icon :size="18"><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </button>
        </nav>
      </aside>

      <!-- 右侧内容 -->
      <main class="pc-main">
        <router-view v-slot="{ Component }">
          <transition name="route-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Cellphone, Lock } from '@element-plus/icons-vue'
import UserAvatar from '@/components/business/UserAvatar.vue'
import { useUserStore } from '@/stores/user'
import { Role } from '@/types/role'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const profile = computed(() => userStore.profile)

const activeName = computed(() => {
  const n = (route.name as string) || 'TotpSetup'
  return n
})

const avatarLetter = computed(() => {
  const s = profile.value?.nickname || profile.value?.username || '?'
  return s.charAt(0).toUpperCase()
})

const roleText = computed(() => {
  const roles = userStore.roles || []
  if (roles.includes(Role.ADMIN)) return '管理员'
  if (roles.includes(Role.AUDITOR)) return '审核员'
  return '普通用户'
})

const navItems = [
  { name: 'TotpSetup', label: '动态口令（2FA）', icon: Cellphone },
  { name: 'SecurityQuestions', label: '密保问题', icon: Lock },
]

function goTo(name: string) {
  if (name === activeName.value) return
  router.push({ name })
}
</script>

<style lang="scss" scoped>
/* =========================================================================
   个人中心外壳 — 样式仅作用于本文件
   ========================================================================= */

.pc-page {
  width: 100%;
  padding: $spacing-lg 0;
}

.pc-shell {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: $spacing-lg;
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 $spacing-md;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

.pc-side {
  background: $bg-elevated;
  border-radius: $radius-2xl;
  padding: $spacing-lg $spacing-md;
  box-shadow: $shadow-sm;
  height: fit-content;
  position: sticky;
  top: $header-height + $spacing-sm;

  &__header {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    padding: 0 $spacing-sm $spacing-md;
    border-bottom: 1px solid $border-light;
    margin-bottom: $spacing-md;
  }

  &__avatar {
    flex-shrink: 0;
  }

  &__avatar-fallback {
    width: 48px;
    height: 48px;
    border-radius: 50%;
    background: linear-gradient(135deg, $color-primary 0%, $color-primary-dark 100%);
    color: #fff;
    font-size: 20px;
    font-weight: 700;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__user {
    min-width: 0;
  }

  &__name {
    font-size: $font-size-md;
    font-weight: 600;
    color: $text-primary;
    @include ellipsis;
  }

  &__role {
    font-size: $font-size-xs;
    color: $text-placeholder;
    margin-top: 2px;
  }
}

.pc-nav {
  display: flex;
  flex-direction: column;
  gap: 2px;

  &__item {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    padding: $spacing-sm $spacing-md;
    border-radius: $radius-md;
    background: transparent;
    border: none;
    cursor: pointer;
    color: $text-regular;
    font-size: $font-size-base;
    text-align: left;
    transition: all $transition-fast;

    &:hover {
      background: $bg-muted;
      color: $color-primary;
    }

    &.active {
      background: $bg-overlay;
      color: $color-primary;
      font-weight: 600;
    }
  }
}

.pc-main {
  min-width: 0;
}

.route-fade-enter-active,
.route-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.route-fade-enter-from {
  opacity: 0;
  transform: translateY(4px);
}
.route-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>

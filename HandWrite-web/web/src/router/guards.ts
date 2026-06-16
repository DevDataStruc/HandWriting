import type { Router } from 'vue-router'
import NProgress from 'nprogress'
import { useUserStore } from '@/stores/user'
import { storage } from '@/utils/storage'
import { TOKEN_KEY } from '@/utils/constants'

export function setupGuards(router: Router): void {
  router.beforeEach(async (to, _from, next) => {
    NProgress.start()
    const token = storage.getString(TOKEN_KEY)
    const userStore = useUserStore()

    // 已登录但未拉取 profile 时拉取一次
    if (token && !userStore.hasProfile) {
      try {
        await userStore.fetchProfile()
      } catch {
        /* 忽略，request 拦截器会处理未登录 */
      }
    }

    const requiresAuth = to.matched.some((r) => r.meta.requiresAuth)
    if (requiresAuth && !userStore.isLoggedIn) {
      next({ name: 'Login', query: { redirect: to.fullPath } })
      return
    }

    const requiredRoles = to.meta.roles
    if (requiredRoles && requiredRoles.length > 0) {
      // 兜底：已登录但后端未下发 roles（JWT 没塞 / profile 接口没返回）时，
      // 视作普通用户放行，避免新用户一进就 403。
      const effectiveRoles = userStore.roles.length > 0 ? userStore.roles : ['USER' as const]
      if (!requiredRoles.some((r) => effectiveRoles.includes(r as never))) {
        next({ name: 'Forbidden' })
        return
      }
    }

    // 已登录访问 /auth/login 跳到首页
    if (to.name === 'Login' && userStore.isLoggedIn) {
      next({ path: '/' })
      return
    }

    next()
  })

  router.afterEach((to) => {
    const base = '手写体采集管理系统'
    document.title = to.meta.title ? `${to.meta.title} · ${base}` : base
    NProgress.done()
  })

  router.onError(() => {
    NProgress.done()
  })
}

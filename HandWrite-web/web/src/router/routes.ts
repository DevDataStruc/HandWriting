import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { Role } from '@/types/role'
import { setUnauthorizedHandler } from '@/utils/request'
import { storage } from '@/utils/storage'
import { TOKEN_KEY } from '@/utils/constants'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    roles?: Role[]
    title?: string
    icon?: string
    layout?: 'default' | 'admin' | 'blank'
    hideInMenu?: boolean
    keepAlive?: boolean
  }
}

const ALL_ROLES: Role[] = [Role.USER, Role.AUDITOR, Role.ADMIN]
const STAFF_ROLES: Role[] = [Role.AUDITOR, Role.ADMIN]
const ADMIN_ONLY: Role[] = [Role.ADMIN]

export const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layouts/DefaultLayout.vue'),
    meta: { layout: 'default' },
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/portal/HomeView.vue'),
        meta: { title: '首页', requiresAuth: false },
      },
      {
        path: 'about',
        name: 'About',
        component: () => import('@/views/portal/AboutView.vue'),
        meta: { title: '关于我们', requiresAuth: false },
      },
      {
        path: 'notices',
        name: 'Notices',
        component: () => import('@/views/portal/NoticesView.vue'),
        meta: { title: '公告中心', requiresAuth: false },
      },
      {
        path: 'urls',
        name: 'Urls',
        component: () => import('@/views/portal/UrlsView.vue'),
        meta: { title: 'URL 索引', requiresAuth: false, hideInMenu: true },
      },
      {
        path: 'toast-test',
        name: 'ToastTest',
        component: () => import('@/views/test/ToastTestView.vue'),
        meta: { title: 'Toast 测试', requiresAuth: false, hideInMenu: true },
      },
      {
        path: 'components-test',
        name: 'ComponentsTest',
        component: () => import('@/views/test/ComponentsTestView.vue'),
        meta: { title: '组件测试', requiresAuth: false, hideInMenu: true },
      },
      {
        path: 'sample/collect',
        name: 'Collect',
        component: () => import('@/views/sample/CollectView.vue'),
        meta: { title: '手写体采集', requiresAuth: true, roles: ALL_ROLES },
      },
      {
        path: 'sample/my',
        name: 'MySamples',
        component: () => import('@/views/sample/ArtGalleryView.vue'),
        meta: { title: '我的样本', requiresAuth: false, roles: ALL_ROLES },
      },
      {
        path: 'sample/:id',
        name: 'SampleDetail',
        component: () => import('@/views/sample/SampleDetailView.vue'),
        meta: { title: '样本详情', requiresAuth: true, roles: ALL_ROLES },
        props: true,
      },
      {
        path: 'profile',
        component: () => import('@/views/profile/ProfileLayout.vue'),
        meta: { title: '个人中心', requiresAuth: true, roles: ALL_ROLES },
        children: [
          {
            path: '',
            redirect: { name: 'TotpSetup' },
          },
          {
            path: 'totp',
            name: 'TotpSetup',
            component: () => import('@/views/profile/TotpSetupView.vue'),
            meta: { title: '动态口令', requiresAuth: true, roles: ALL_ROLES },
          },
          {
            path: 'security-questions',
            name: 'SecurityQuestions',
            component: () => import('@/views/profile/SecurityQuestionsView.vue'),
            meta: { title: '密保问题', requiresAuth: true, roles: ALL_ROLES },
          },
        ],
      },
    ],
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { layout: 'admin', requiresAuth: false, roles: STAFF_ROLES },
    children: [
      {
        path: '',
        redirect: { name: 'Dashboard' },
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/admin/DashboardView.vue'),
        meta: { title: '数据看板', icon: 'data', roles: STAFF_ROLES },
      },
      {
        path: 'users',
        name: 'UserManagement',
        component: () => import('@/views/admin/UserManagementView.vue'),
        meta: { title: '用户管理', icon: 'user', roles: ADMIN_ONLY },
      },
      {
        path: 'audit',
        name: 'SampleAudit',
        component: () => import('@/views/admin/SampleAuditView.vue'),
        meta: { title: '样本审核', icon: 'check', roles: STAFF_ROLES },
      },
      {
        path: 'dict-input',
        name: 'DictInput',
        component: () => import('@/views/admin/DictInputView.vue'),
        meta: { title: '字符字典录入', icon: 'dict', roles: ADMIN_ONLY },
      },
      {
        path: 'stats',
        name: 'Stats',
        component: () => import('@/views/admin/StatsView.vue'),
        meta: { title: '统计分析', icon: 'chart', roles: ADMIN_ONLY },
      },
      {
        path: 'logs',
        name: 'Logs',
        component: () => import('@/views/admin/LogsView.vue'),
        meta: { title: '审计日志', icon: 'log', roles: ADMIN_ONLY },
      },
    ],
  },
  {
    path: '/auth',
    component: () => import('@/layouts/BlankLayout.vue'),
    meta: { layout: 'blank' },
    children: [
      {
        path: 'login',
        name: 'Login',
        component: () => import('@/views/auth/LoginView.vue'),
        meta: { title: '登录', requiresAuth: false },
      },
      {
        path: 'register',
        name: 'Register',
        component: () => import('@/views/auth/RegisterView.vue'),
        meta: { title: '注册', requiresAuth: false },
      },
      {
        path: 'forgot',
        name: 'ForgotPassword',
        component: () => import('@/views/auth/ForgotPasswordView.vue'),
        meta: { title: '找回密码', requiresAuth: false },
      },
    ],
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/403.vue'),
    meta: { title: '无权限', requiresAuth: false },
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在', requiresAuth: false },
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: { name: 'NotFound' },
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior(_to, _from, saved) {
    return saved || { top: 0, behavior: 'smooth' }
  },
})

/** 在 user store 注册之前，先行让 request 模块知道如何跳转 */
setUnauthorizedHandler(() => {
  storage.remove(TOKEN_KEY)
  const current = router.currentRoute.value.fullPath
  if (!router.currentRoute.value.path.startsWith('/auth')) {
    router.replace({ name: 'Login', query: { redirect: current } })
  }
})

export default router

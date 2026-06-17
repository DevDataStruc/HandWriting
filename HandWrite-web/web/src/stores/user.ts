import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as authApi from '@/api/auth'
import * as userApi from '@/api/user'
import type { LoginDTO, LoginVO } from '@/api/contracts/auth'
import type { UserProfileDTO, UserVO } from '@/api/contracts/user'
import { Role } from '@/types/role'
import { storage } from '@/utils/storage'
import { TOKEN_KEY, REFRESH_TOKEN_KEY, USER_PROFILE_KEY } from '@/utils/constants'
import { getRolesFromToken, isTokenExpired } from '@/utils/auth'
import { setTokenRefreshedHandler } from '@/utils/request'

/** 缓存中的 Profile 不持久化 token 字段（安全） */
type CachedProfile = Omit<UserVO, 'permissions'> & {
  permissions?: string[]
  /** 前端扩展字段 */
  roles?: Role[]
}

function readCachedProfile(): CachedProfile | null {
  const v = storage.get<CachedProfile>(USER_PROFILE_KEY)
  return v ?? null
}

function writeCachedProfile(p: CachedProfile | null): void {
  if (p) storage.set(USER_PROFILE_KEY, p)
  else storage.remove(USER_PROFILE_KEY)
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(storage.getString(TOKEN_KEY, ''))
  const refreshToken = ref<string>(storage.getString(REFRESH_TOKEN_KEY, ''))
  const profile = ref<CachedProfile | null>(readCachedProfile())
  const roles = ref<Role[]>(getRolesFromToken(token.value))

  const isLoggedIn = computed(() => !!token.value && !isTokenExpired(token.value))
  const hasProfile = computed(() => profile.value != null)
  const isAdmin = computed(() => roles.value.includes(Role.ADMIN))
  const isAuditor = computed(() => roles.value.includes(Role.AUDITOR))

  /**
   * 写入 accessToken / refreshToken（可同时落 localStorage）
   * 不持久化 accessToken 到 storage（仅内存），refreshToken 走 localStorage
   */
  function setToken(at: string, rt?: string): void {
    token.value = at
    if (at) storage.setString(TOKEN_KEY, at)
    else storage.remove(TOKEN_KEY)
    if (rt != null) {
      refreshToken.value = rt
      storage.setString(REFRESH_TOKEN_KEY, rt)
    }
    roles.value = getRolesFromToken(at)
  }

  function setProfile(p: CachedProfile | null): void {
    profile.value = p
    writeCachedProfile(p)
    if (p) {
      const arr = (p.roles || (getRolesFromToken(token.value) as Role[])) as Role[]
      if (arr.length) roles.value = arr
    }
  }

  /**
   * 登录：解析 LoginVO，写入 accessToken/refreshToken/user
   * 同时把 user.roles 同步到 roles ref
   */
  async function login(payload: LoginDTO): Promise<LoginVO> {
    const vo = await authApi.login(payload)
    applyLoginVO(vo)
    return vo
  }

  function applyLoginVO(vo: LoginVO): void {
    setToken(vo.accessToken, vo.refreshToken)
    if (vo.user) {
      const cached: CachedProfile = {
        id: vo.user.id,
        username: vo.user.username,
        nickname: vo.user.nickname,
        avatar: vo.user.avatar || '',
        email: '',
        phone: '',
        status: 1,
        lastLoginTime: '',
        createTime: '',
        roles: vo.user.roles as unknown as Role[],
        permissions: vo.user.permissions,
      }
      setProfile(cached)
    }
  }

  async function fetchProfile(): Promise<UserVO | null> {
    if (!token.value) return null
    try {
      const p = await userApi.getProfile()
      const cached: CachedProfile = {
        ...p,
        roles: p.roles ?? (getRolesFromToken(token.value) as Role[]),
      }
      setProfile(cached)
      return p
    } catch (err) {
      console.warn('[user store] fetchProfile error', err)
      return null
    }
  }

  async function updateProfile(data: UserProfileDTO): Promise<UserVO> {
    const p = await userApi.updateProfile(data)
    const cached: CachedProfile = {
      ...(profile.value || ({} as CachedProfile)),
      ...p,
      roles: p.roles ?? roles.value,
    }
    setProfile(cached)
    return p
  }

  /**
   * 注销：调后端（带 refreshToken）并清空本地
   */
  async function logout(): Promise<void> {
    const rt = refreshToken.value
    try {
      if (rt) await authApi.logout({ refreshToken: rt })
    } catch {
      /* ignore - 本地清理照常 */
    }
    reset()
  }

  function reset(): void {
    token.value = ''
    refreshToken.value = ''
    profile.value = null
    roles.value = []
    storage.remove(TOKEN_KEY)
    storage.remove(REFRESH_TOKEN_KEY)
    storage.remove(USER_PROFILE_KEY)
  }

  function hasRole(required: Role | Role[]): boolean {
    const list = Array.isArray(required) ? required : [required]
    return list.some((r) => roles.value.includes(r))
  }

  /**
   * 注册 request 层刷新 token 回调
   * 这样在自动刷新场景下，store 里的 accessToken / refreshToken / profile 都能即时同步
   */
  setTokenRefreshedHandler((accessToken, refresh) => {
    token.value = accessToken
    refreshToken.value = refresh
    storage.setString(TOKEN_KEY, accessToken)
    storage.setString(REFRESH_TOKEN_KEY, refresh)
    roles.value = getRolesFromToken(accessToken)
  })

  return {
    token,
    refreshToken,
    profile,
    roles,
    isLoggedIn,
    hasProfile,
    isAdmin,
    isAuditor,
    login,
    logout,
    fetchProfile,
    updateProfile,
    setToken,
    setProfile,
    reset,
    hasRole,
  }
})

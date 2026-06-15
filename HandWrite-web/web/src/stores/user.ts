import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as authApi from '@/api/auth'
import * as userApi from '@/api/user'
import type { LoginRequest, LoginResponse } from '@/api/contracts/auth'
import type { UpdateProfileRequest, UserProfile } from '@/api/contracts/user'
import type { Role } from '@/types/role'
import { storage } from '@/utils/storage'
import { TOKEN_KEY, REFRESH_TOKEN_KEY, USER_PROFILE_KEY } from '@/utils/constants'
import { getRolesFromToken, isTokenExpired } from '@/utils/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(storage.getString(TOKEN_KEY, ''))
  const refreshToken = ref<string>(storage.getString(REFRESH_TOKEN_KEY, ''))
  const profile = ref<UserProfile | null>(storage.get<UserProfile>(USER_PROFILE_KEY) || null)
  const roles = ref<Role[]>(getRolesFromToken(token.value))

  const isLoggedIn = computed(() => !!token.value && !isTokenExpired(token.value))
  const hasProfile = computed(() => profile.value != null)
  const isAdmin = computed(() => roles.value.includes(Role.ADMIN))
  const isAuditor = computed(() => roles.value.includes(Role.AUDITOR))

  function setToken(t: string, refresh?: string): void {
    token.value = t
    if (t) storage.setString(TOKEN_KEY, t)
    else storage.remove(TOKEN_KEY)
    if (refresh != null) {
      refreshToken.value = refresh
      storage.setString(REFRESH_TOKEN_KEY, refresh)
    }
    roles.value = getRolesFromToken(t)
  }

  function setProfile(p: UserProfile | null): void {
    profile.value = p
    if (p) {
      storage.set(USER_PROFILE_KEY, p)
      if (p.roles && p.roles.length) {
        roles.value = p.roles
      }
    } else {
      storage.remove(USER_PROFILE_KEY)
    }
  }

  async function login(payload: LoginRequest): Promise<LoginResponse> {
    const res = await authApi.login(payload)
    setToken(res.token, res.refreshToken)
    return res
  }

  async function fetchProfile(): Promise<UserProfile | null> {
    if (!token.value) return null
    try {
      const p = await userApi.fetchProfile()
      setProfile(p)
      return p
    } catch (err) {
      console.warn('[user store] fetchProfile error', err)
      return null
    }
  }

  async function updateProfile(data: UpdateProfileRequest): Promise<UserProfile> {
    const p = await userApi.updateProfile(data)
    setProfile(p)
    return p
  }

  async function logout(): Promise<void> {
    try {
      await authApi.logout()
    } catch {
      /* ignore */
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

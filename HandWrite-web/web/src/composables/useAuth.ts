import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { storage } from '@/utils/storage'
import { TOKEN_KEY } from '@/utils/constants'

/**
 * 鉴权 composable
 */
export function useAuth() {
  const router = useRouter()
  const userStore = useUserStore()

  const token = computed(() => userStore.token)
  const profile = computed(() => userStore.profile)
  const isLoggedIn = computed(() => userStore.isLoggedIn)
  const roles = computed(() => userStore.roles)

  async function login(payload: Parameters<typeof userStore.login>[0]) {
    const res = await userStore.login(payload)
    return res
  }

  async function logout(redirect = true): Promise<void> {
    await userStore.logout()
    storage.remove(TOKEN_KEY)
    if (redirect) {
      router.push({ name: 'Login' })
    }
  }

  async function refreshProfile() {
    return userStore.fetchProfile()
  }

  return {
    token,
    profile,
    isLoggedIn,
    roles,
    login,
    logout,
    refreshProfile,
  }
}

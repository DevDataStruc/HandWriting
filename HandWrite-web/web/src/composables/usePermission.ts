import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import type { Role } from '@/types/role'
import { ROLE_RANK } from '@/types/role'

/**
 * 权限检查 composable
 */
export function usePermission() {
  const userStore = useUserStore()
  const roles = computed<Role[]>(() => userStore.roles)
  const isLoggedIn = computed(() => userStore.isLoggedIn)

  function hasRole(required: Role | Role[]): boolean {
    const list = Array.isArray(required) ? required : [required]
    return list.some((r) => roles.value.includes(r))
  }

  function hasRoleRank(required: Role): boolean {
    const requiredRank = ROLE_RANK[required]
    return roles.value.some((r) => ROLE_RANK[r] >= requiredRank)
  }

  function hasAnyRole(list: Role[]): boolean {
    return list.some((r) => roles.value.includes(r))
  }

  return {
    roles,
    isLoggedIn,
    hasRole,
    hasRoleRank,
    hasAnyRole,
  }
}

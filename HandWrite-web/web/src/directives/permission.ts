import type { Directive, DirectiveBinding } from 'vue'
import type { Role } from '@/types/role'
import { useUserStore } from '@/stores/user'

/**
 * v-permission="['ADMIN']" 或 v-permission="'ADMIN'"
 * 元素不满足权限时从 DOM 移除
 */
function check(value: unknown): boolean {
  if (value == null) return true
  const userStore = useUserStore()
  const list = (Array.isArray(value) ? value : [value]) as (Role | string)[]
  return list.some((r) => userStore.roles.includes(r as Role))
}

function update(el: HTMLElement, binding: DirectiveBinding<unknown>) {
  const ok = check(binding.value)
  if (!ok) {
    el.parentNode?.removeChild(el)
  }
}

export const permission: Directive<HTMLElement, unknown> = {
  mounted: update,
  updated: update,
}

export default permission

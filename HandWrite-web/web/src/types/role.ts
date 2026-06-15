/**
 * 角色枚举
 * 与后端 RBAC 角色保持一致
 */
export enum Role {
  USER = 'USER',
  AUDITOR = 'AUDITOR',
  ADMIN = 'ADMIN',
}

/**
 * 角色优先级（用于权限比较）
 */
export const ROLE_RANK: Record<Role, number> = {
  [Role.USER]: 1,
  [Role.AUDITOR]: 2,
  [Role.ADMIN]: 3,
}

/**
 * 角色中文描述
 */
export const ROLE_LABELS: Record<Role, string> = {
  [Role.USER]: '普通用户',
  [Role.AUDITOR]: '审核员',
  [Role.ADMIN]: '管理员',
}

/**
 * 检查角色是否匹配
 * @param has 当前拥有的角色列表
 * @param required 需要的角色列表（任一匹配即可）
 */
export function hasRole(has: Role[] | undefined, required: Role | Role[]): boolean {
  if (!has || has.length === 0) return false
  const requiredList = Array.isArray(required) ? required : [required]
  return requiredList.some((r) => has.includes(r))
}

/**
 * 检查是否拥有高级角色（基于等级）
 */
export function hasRoleRank(has: Role[] | undefined, required: Role): boolean {
  if (!has || has.length === 0) return false
  const requiredRank = ROLE_RANK[required]
  return has.some((r) => ROLE_RANK[r] >= requiredRank)
}

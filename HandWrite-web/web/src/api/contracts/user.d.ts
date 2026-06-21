import type { Role } from '@/types/role'

/**
 * 用户档案（GET /v1/user/profile）
 * 字段与 API.md §9 UserVO 对齐
 */
export interface UserVO {
  id: number
  username: string
  nickname: string
  email: string
  phone: string
  avatar: string
  status: number
  lastLoginTime: string
  createTime: string
  roles?: Role[]
  permissions?: string[]
}

/**
 * 用户扩展信息（管理员后台用，前端扩展）
 * - 与 UserVO 字段一致
 * - 增加 sampleCount、status: 'active' | 'disabled' 等扩展字段
 */
export interface UserProfile extends UserVO {
  sampleCount?: number
  /** 简化状态：active / disabled（前端约定） */
  statusExt?: 'active' | 'disabled'
  createdAt?: string
}

/** 更新个人资料请求（PUT /v1/user/profile） */
export interface UserProfileDTO {
  nickname?: string
  email?: string
  phone?: string
  avatar?: string
}

/** 修改密码请求（不在 API.md 中但前端常见用，可选） */
export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

/**
 * 管理员-用户档案（GET /v1/admin/users、PATCH /v1/admin/users/{id}/status）
 * 字段与 HandWrite-client api2.md 中 AdminUserVO 对齐
 */
export interface AdminUserVO {
  id: number
  username: string
  nickname: string
  email: string
  phone: string
  avatar: string
  /** 原始状态码（0 正常 / 1 禁用） */
  status: number
  /** 扩展状态：active / disabled（后端友好别名） */
  statusExt: 'active' | 'disabled'
  lastLoginTime: string
  createTime: string
  /** createTime 别名（后端冗余字段） */
  createdAt?: string
  /** 角色编码列表（USER / AUDITOR / ADMIN） */
  roles: string[]
  permissions: string[]
  /** 该用户提交的样本数 */
  sampleCount: number
}

/**
 * 管理员-用户分页响应（GET /v1/admin/users）
 * 注意：与 sample/audit 分页结构（PageResult）不一致，使用 list/total
 */
export interface AdminUserListResponse {
  list: AdminUserVO[]
  total: number
}

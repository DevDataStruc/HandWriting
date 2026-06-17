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

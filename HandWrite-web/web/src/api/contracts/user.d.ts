import type { Role } from '@/types/role'

/**
 * 用户档案
 */
export interface UserProfile {
  id: number | string
  username: string
  nickname?: string
  email?: string
  phone?: string
  avatar?: string
  gender?: 0 | 1 | 2
  bio?: string
  roles: Role[]
  permissions?: string[]
  createdAt?: string
  updatedAt?: string
  lastLoginAt?: string
  status?: 'active' | 'disabled'
  sampleCount?: number
}

/**
 * 更新个人资料请求
 */
export interface UpdateProfileRequest {
  nickname?: string
  email?: string
  phone?: string
  gender?: 0 | 1 | 2
  bio?: string
  avatar?: string
}

/**
 * 修改密码请求
 */
export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

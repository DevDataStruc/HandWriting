import request from '@/utils/request'
import type { UserVO, UserProfileDTO } from './contracts/user'

/**
 * 用户模块 - 与 API.md §1.2 / §7.2 一致
 * 基础路径：/v1/user
 */

/** GET /v1/user/profile - 获取个人信息 */
export function getProfile(): Promise<UserVO> {
  return request.get<UserVO>('/user/profile')
}

/** PUT /v1/user/profile - 修改个人信息 */
export function updateProfile(data: UserProfileDTO): Promise<UserVO> {
  return request.put<UserVO>('/user/profile', data)
}

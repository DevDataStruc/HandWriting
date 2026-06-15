import request from '@/utils/request'
import type {
  ChangePasswordRequest,
  UpdateProfileRequest,
  UserProfile,
} from './contracts/user'

/** 获取个人信息 */
export function fetchProfile(): Promise<UserProfile> {
  return request.get<UserProfile>('/user/profile')
}

/** 更新个人信息 */
export function updateProfile(data: UpdateProfileRequest): Promise<UserProfile> {
  return request.put<UserProfile>('/user/profile', data)
}

/** 修改密码 */
export function changePassword(data: ChangePasswordRequest): Promise<void> {
  return request.post<void>('/user/change-password', data)
}

/** 上传头像（直传对象存储前可调用获取签名） */
export function uploadAvatar(formData: FormData, onProgress?: (p: number) => void): Promise<{ url: string }> {
  return request.upload<{ url: string }>('/user/avatar', formData, onProgress)
}

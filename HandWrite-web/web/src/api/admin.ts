import request from '@/utils/request'
import type { AdminUserListResponse, AdminUserVO } from './contracts/user'

/**
 * 管理员-用户管理（与 HandWrite-client api2.md §管理员-用户管理 对齐）
 * 基础路径：/v1/admin/users
 */

/** GET /v1/admin/users - 分页查询用户列表（关键字 / 状态） */
export function listUsers(params?: {
  keyword?: string
  status?: number
  pageNum?: number
  pageSize?: number
}): Promise<AdminUserListResponse> {
  return request.get<AdminUserListResponse>('/admin/users', {
    ...params,
  })
}

/** PATCH /v1/admin/users/{id}/status - 切换用户启用/禁用状态 */
export function toggleUserStatus(id: number): Promise<AdminUserVO> {
  return request.patch<AdminUserVO>(`/admin/users/${id}/status`)
}

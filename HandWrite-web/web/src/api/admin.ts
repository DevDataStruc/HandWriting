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

/**
 * PUT /v1/admin/users/{id}/roles - 更新用户角色
 *
 * 注意：当前后端 API 文档（HandWrite-client api2.md）尚未定义此接口，
 * 前端按 REST 约定调用 PUT /v1/admin/users/{id}/roles，若后端返回 404
 * 会冒到调用方处理。建议后端尽快补齐此端点。
 */
export function updateUserRoles(id: number, roles: string[]): Promise<AdminUserVO> {
  return request.put<AdminUserVO>(`/admin/users/${id}/roles`, { roles })
}

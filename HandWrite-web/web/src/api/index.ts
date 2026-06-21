/**
 * API 统一入口（re-export）
 * 字段与接口定义与 docs/API.md 完全对齐
 */
export * as authApi from './auth'
export * as userApi from './user'
export * as dictApi from './dict'
export * as sampleApi from './sample'
export * as fileApi from './file'
export * as auditApi from './audit'
export * as statsApi from './stats'
export * as dictAdminApi from './dict-admin'

export type * from './contracts/common'
export type * from './contracts/auth'
export type * from './contracts/user'
export type * from './contracts/sample'
export type * from './contracts/audit'
export type * from './contracts/stats'
export type * from './contracts/file'

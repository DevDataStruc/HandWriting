import type { Role } from '@/types/role'

/** 图形验证码响应（GET /v1/auth/captcha） */
export interface CaptchaVO {
  captchaKey: string
  imageBase64: string
  expireSeconds: number
}

/** 登录请求（POST /v1/auth/login） */
export interface LoginDTO {
  username: string
  password: string
  captchaKey: string
  captchaCode: string
}

/** 注册请求（POST /v1/auth/register） */
export interface RegisterDTO {
  /** 3-64 字符，^[A-Za-z0-9_.-]+$ */
  username: string
  /** 6-64 字符 */
  password: string
  nickname?: string
  email?: string
}

/** 刷新 Token 请求（POST /v1/auth/refresh） */
export interface RefreshTokenDTO {
  refreshToken: string
}

/** 用户基本信息（登录响应中携带） */
export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar?: string
  roles: string[]
  permissions: string[]
}

/** 登录响应（POST /v1/auth/login & /v1/auth/register & /v1/auth/refresh） */
export interface LoginVO {
  accessToken: string
  refreshToken: string
  tokenType: 'Bearer'
  expiresIn: number
  user: UserInfo
}

/** 携带角色与权限的 token payload（解析后） */
export interface TokenPayload {
  sub: string
  username: string
  roles?: Role[]
  exp?: number
  iat?: number
}

export type { Role }

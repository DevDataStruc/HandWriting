import type { Role } from '@/types/role'

/**
 * 登录请求
 */
export interface LoginRequest {
  username: string
  password: string
  captchaId?: string
  captchaCode?: string
}

/**
 * 登录响应
 */
export interface LoginResponse {
  token: string
  refreshToken?: string
  expiresIn?: number
}

/**
 * 注册请求
 */
export interface RegisterRequest {
  username: string
  password: string
  confirmPassword: string
  email?: string
  phone?: string
  nickname?: string
  captchaId?: string
  captchaCode?: string
}

/**
 * 验证码
 */
export interface CaptchaResponse {
  captchaId: string
  imageData: string
}

/**
 * 找回密码请求
 */
export interface ForgotPasswordRequest {
  email: string
  captchaId?: string
  captchaCode?: string
  newPassword: string
  confirmPassword: string
}

/**
 * 刷新 Token 响应
 */
export interface RefreshTokenResponse {
  token: string
  expiresIn?: number
}

/**
 * 携带角色与权限的 token payload（解析后）
 */
export interface TokenPayload {
  sub: string
  username: string
  roles?: Role[]
  exp?: number
  iat?: number
}

export type { Role }

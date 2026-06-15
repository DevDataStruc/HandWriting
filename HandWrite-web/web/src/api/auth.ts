import request from '@/utils/request'
import type {
  CaptchaResponse,
  ForgotPasswordRequest,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
} from './contracts/auth'

/** 登录 */
export function login(data: LoginRequest): Promise<LoginResponse> {
  return request.post<LoginResponse>('/auth/login', data)
}

/** 注册 */
export function register(data: RegisterRequest): Promise<void> {
  return request.post<void>('/auth/register', data)
}

/** 图形验证码 */
export function fetchCaptcha(): Promise<CaptchaResponse> {
  return request.get<CaptchaResponse>('/auth/captcha')
}

/** 退出登录 */
export function logout(): Promise<void> {
  return request.post<void>('/auth/logout')
}

/** 刷新 Token */
export function refreshToken(): Promise<LoginResponse> {
  return request.post<LoginResponse>('/auth/refresh')
}

/** 找回密码 */
export function forgotPassword(data: ForgotPasswordRequest): Promise<void> {
  return request.post<void>('/auth/forgot-password', data)
}

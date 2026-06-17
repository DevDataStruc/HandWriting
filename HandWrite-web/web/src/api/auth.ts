import request from '@/utils/request'
import type { CaptchaVO, LoginDTO, LoginVO, RefreshTokenDTO, RegisterDTO } from './contracts/auth'

/**
 * 鉴权模块 - 与 API.md §1.2 / §7.1 一致
 * 基础路径：/v1/auth
 */

/** GET /v1/auth/captcha - 获取图形验证码 */
export function getCaptcha(): Promise<CaptchaVO> {
  return request.get<CaptchaVO>('/auth/captcha')
}

/** POST /v1/auth/login - 登录 */
export function login(data: LoginDTO): Promise<LoginVO> {
  return request.post<LoginVO>('/auth/login', data)
}

/** POST /v1/auth/register - 注册 */
export function register(data: RegisterDTO): Promise<LoginVO> {
  return request.post<LoginVO>('/auth/register', data)
}

/** POST /v1/auth/refresh - 刷新 Access Token */
export function refreshToken(data: RefreshTokenDTO): Promise<LoginVO> {
  return request.post<LoginVO>('/auth/refresh', data)
}

/** POST /v1/auth/logout - 注销（带 refreshToken） */
export function logout(data: RefreshTokenDTO): Promise<void> {
  return request.post<void>('/auth/logout', data)
}

import request from '@/utils/request'
import type {
  BindTotpDTO,
  CaptchaVO,
  ForgotPasswordStartDTO,
  ForgotPasswordStartVO,
  LoginDTO,
  LoginVO,
  PasswordResetTicketVO,
  RefreshTokenDTO,
  RegisterDTO,
  ResetPasswordDTO,
  SecurityQuestionVO,
  SetupSecurityQuestionsDTO,
  TotpSetupVO,
  TotpStatusVO,
  VerifyRecoveryCodeDTO,
  VerifySecurityQuestionsDTO,
  VerifyTotpDTO,
  VerifyTotpForResetDTO,
} from './contracts/auth'

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

/* =============================================================
 *  密码找回（公开，无需登录）
 * ============================================================= */

/** POST /v1/auth/recovery/start - 发起找回 */
export function recoveryStart(
  data: ForgotPasswordStartDTO
): Promise<ForgotPasswordStartVO> {
  return request.post<ForgotPasswordStartVO>('/auth/recovery/start', data)
}

/** GET /v1/auth/recovery/security-questions - 用 challengeId 换取密保问题正文 */
export function recoveryGetSecurityQuestions(
  challengeId: string
): Promise<SecurityQuestionVO[]> {
  return request.get<SecurityQuestionVO[]>('/auth/recovery/security-questions', {
    params: { challengeId },
  })
}

/** POST /v1/auth/recovery/verify-security - 密保问题校验 */
export function recoveryVerifySecurity(
  data: VerifySecurityQuestionsDTO
): Promise<PasswordResetTicketVO> {
  return request.post<PasswordResetTicketVO>('/auth/recovery/verify-security', data)
}

/** POST /v1/auth/recovery/verify-totp - 动态口令 / 恢复码校验 */
export function recoveryVerifyTotp(
  data: VerifyTotpForResetDTO
): Promise<PasswordResetTicketVO> {
  return request.post<PasswordResetTicketVO>('/auth/recovery/verify-totp', data)
}

/** POST /v1/auth/recovery/reset - 重置密码 */
export function recoveryReset(data: ResetPasswordDTO): Promise<void> {
  return request.post<void>('/auth/recovery/reset', data)
}

/* =============================================================
 *  2FA（已登录）
 * ============================================================= */

/** GET /v1/auth/totp/status - 2FA 状态 */
export function getTotpStatus(): Promise<TotpStatusVO> {
  return request.get<TotpStatusVO>('/auth/totp/status')
}

/** POST /v1/auth/totp/setup - 初始化 2FA */
export function setupTotp(): Promise<TotpSetupVO> {
  return request.post<TotpSetupVO>('/auth/totp/setup')
}

/** POST /v1/auth/totp/bind - 确认绑定 */
export function bindTotp(data: BindTotpDTO): Promise<void> {
  return request.post<void>('/auth/totp/bind', data)
}

/** DELETE /v1/auth/totp/bind - 解绑 */
export function unbindTotp(): Promise<void> {
  return request.delete<void>('/auth/totp/bind')
}

/** POST /v1/auth/totp/verify - 已登录态动态口令二次校验 */
export function verifyTotp(data: VerifyTotpDTO): Promise<void> {
  return request.post<void>('/auth/totp/verify', data)
}

/** POST /v1/auth/totp/verify-recovery - 已登录态一次性恢复码 */
export function verifyTotpRecovery(data: VerifyRecoveryCodeDTO): Promise<void> {
  return request.post<void>('/auth/totp/verify-recovery', data)
}

/* =============================================================
 *  密保问题（已登录）
 * ============================================================= */

/** GET /v1/auth/security-questions - 我的密保问题 */
export function listSecurityQuestions(): Promise<SecurityQuestionVO[]> {
  return request.get<SecurityQuestionVO[]>('/auth/security-questions')
}

/** POST /v1/auth/security-questions - 设置/覆盖 */
export function setupSecurityQuestions(
  data: SetupSecurityQuestionsDTO
): Promise<void> {
  return request.post<void>('/auth/security-questions', data)
}

/** DELETE /v1/auth/security-questions - 清空 */
export function clearSecurityQuestions(): Promise<void> {
  return request.delete<void>('/auth/security-questions')
}


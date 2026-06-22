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

/* =============================================================
 *  密码找回 / 2FA / 密保 相关契约
 * ============================================================= */

/** 找回密码第一步请求 */
export interface ForgotPasswordStartDTO {
  username: string
}

/** 找回密码第一步响应 */
export interface ForgotPasswordStartVO {
  challengeId: string
  maskedUsername: string
  /** 可用恢复方式: TOTP / SECURITY_QUESTION */
  methods: string[]
  /** 密保问题正文（仅 SECURITY_QUESTION 可用时返回，元素仅含 questionIndex/Key/Text，无答案） */
  securityQuestions?: SecurityQuestionVO[]
}

/** 密保问题答案（单题） */
export interface SecurityAnswerItem {
  questionIndex: number
  answer: string
}

/** 找回密码第二步：密保校验 */
export interface VerifySecurityQuestionsDTO {
  challengeId: string
  answers: SecurityAnswerItem[]
}

/** 找回密码第二步：TOTP 校验 */
export interface VerifyTotpForResetDTO {
  challengeId: string
  code?: string
  recoveryCode?: string
  useRecoveryCode: boolean
}

/** 找回密码第二步响应：短期 resetToken */
export interface PasswordResetTicketVO {
  resetToken: string
  expiresIn: number
}

/** 找回密码第三步：重置密码 */
export interface ResetPasswordDTO {
  resetToken: string
  newPassword: string
}

/** 2FA 初始化响应 */
export interface TotpSetupVO {
  secret: string
  otpauthUrl: string
  qrCodeBase64: string
  recoveryCodes: string[]
  issuer: string
  accountLabel: string
}

/** 2FA 状态 */
export interface TotpStatusVO {
  bound: boolean
  issuer?: string
  accountLabel?: string
  remainingRecoveryCodes?: number
  confirmedAt?: string
}

/** 2FA 校验（已登录态） */
export interface VerifyTotpDTO {
  code: string
}

/** 2FA 恢复码校验（已登录态） */
export interface VerifyRecoveryCodeDTO {
  recoveryCode: string
}

/** 密保问题摘要（不含答案） */
export interface SecurityQuestionVO {
  questionIndex: number
  questionKey: string
  questionText: string
}

/** 密保问题条目（设置时） */
export interface SecurityQuestionItem {
  questionKey: string
  questionText: string
  answer: string
}

/** 设置密保请求 */
export interface SetupSecurityQuestionsDTO {
  questions: SecurityQuestionItem[]
}

/** 2FA 确认绑定请求 */
export interface BindTotpDTO {
  code: string
}

export type { Role }

/**
 * 通用校验工具
 */

export function isUsername(value: string): boolean {
  return /^[a-zA-Z0-9_]{3,20}$/.test(value)
}

export function isPassword(value: string): boolean {
  return /^[A-Za-z0-9!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]{6,32}$/.test(value)
}

export function isEmail(value: string): boolean {
  return /^[\w.+-]+@[\w-]+\.[\w.-]+$/.test(value)
}

export function isPhone(value: string): boolean {
  return /^1[3-9]\d{9}$/.test(value)
}

export function isIdCard(value: string): boolean {
  return /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/.test(value)
}

export function isUrl(value: string): boolean {
  return /^https?:\/\/[\w.-]+(?::\d+)?(\/.*)?$/.test(value)
}

export const validators = {
  username: (v: string) => isUsername(v) || '用户名需 3-20 位字母、数字或下划线',
  password: (v: string) => isPassword(v) || '密码需 6-32 位合法字符',
  email: (v: string) => !v || isEmail(v) || '邮箱格式不正确',
  phone: (v: string) => !v || isPhone(v) || '手机号格式不正确',
}

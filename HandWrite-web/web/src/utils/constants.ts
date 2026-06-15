/**
 * 全局常量
 */
export const APP_NAME = '手写体采集管理系统'

/** Token 在 localStorage 中的 key */
export const TOKEN_KEY = 'hw_token'
export const REFRESH_TOKEN_KEY = 'hw_refresh_token'
export const USER_PROFILE_KEY = 'hw_user_profile'

/** 默认分页大小 */
export const DEFAULT_PAGE_SIZE = 10
export const PAGE_SIZE_OPTIONS = [10, 20, 50, 100]

/** 文件上传大小限制（10MB） */
export const UPLOAD_MAX_SIZE = 10 * 1024 * 1024

/** 允许的图片类型 */
export const ALLOWED_IMAGE_TYPES = ['image/png', 'image/jpeg', 'image/svg+xml']

/** 主题键 */
export const THEME_KEY = 'hw_theme'
export const SIDEBAR_KEY = 'hw_sidebar_collapsed'
export const LOCALE_KEY = 'hw_locale'

/** 默认主题（用户端 teal / 管理员 slate） */
export enum ThemeMode {
  LIGHT = 'light',
  DARK = 'dark',
}

export const HTTP_STATUS = {
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  SERVER_ERROR: 500,
} as const

export const STROKE_DEFAULT_SIZE = 8
export const STROKE_MIN_SIZE = 1
export const STROKE_MAX_SIZE = 32

export const CANVAS_DEFAULT_WIDTH = 600
export const CANVAS_DEFAULT_HEIGHT = 400

/** 业务异常码 */
export const ERROR_CODE = {
  SUCCESS: 0,
  UNAUTHORIZED: 1001,
  FORBIDDEN: 1003,
  BUSINESS: 2001,
  VALIDATION: 2002,
  SERVER: 5000,
} as const

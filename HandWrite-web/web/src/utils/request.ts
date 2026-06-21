import axios, {
  type AxiosInstance,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'
import { ElMessage } from 'element-plus'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { storage } from './storage'
import { TOKEN_KEY, REFRESH_TOKEN_KEY } from './constants'
import { BusinessCode } from '@/api/contracts/common'
import { refreshToken as refreshTokenApi } from '@/api/auth'

NProgress.configure({ showSpinner: false, trickleSpeed: 200 })

/**
 * 扩展内部请求配置，支持自定义控制
 */
type ExtendedAxiosRequestConfig = InternalAxiosRequestConfig & {
  /** 是否展示顶部进度条（默认 true） */
  showProgress?: boolean
}

/**
 * 统一业务异常（与 API.md §5 对齐）
 */
export class ApiError extends Error {
  code: number
  data: unknown
  constructor(code: number, msg: string, data?: unknown) {
    super(msg)
    this.name = 'ApiError'
    this.code = code
    this.data = data
  }
}

/** Token 失效回调，由业务注册（用于跳转登录） */
type OnUnauthorized = () => void
let unauthorizedHandler: OnUnauthorized | null = null
export function setUnauthorizedHandler(fn: OnUnauthorized): void {
  unauthorizedHandler = fn
}

/** AccessToken 刷新回调（用于同步到 store） */
type OnTokenRefreshed = (accessToken: string, refreshToken: string) => void
let tokenRefreshedHandler: OnTokenRefreshed | null = null
export function setTokenRefreshedHandler(fn: OnTokenRefreshed): void {
  tokenRefreshedHandler = fn
}

const baseURL = import.meta.env.VITE_API_BASE_URL || '/v1'

const instance: AxiosInstance = axios.create({
  baseURL,
  timeout: 30_000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
  },
  withCredentials: false,
  /**
   * 关键：后端 PageQuery 是 @ModelAttribute query 复杂对象，
   * 需要将嵌套对象展开为 `pageNum=1&pageSize=20` 而非 `query[pageNum]=1`
   * 自定义 paramsSerializer 把 { pageNum: 1, pageSize: 20 } 展开为平铺 query
   */
  paramsSerializer: {
    serialize: (params: Record<string, unknown> | undefined): string => {
      if (!params) return ''
      const parts: string[] = []
      const append = (k: string, v: unknown): void => {
        if (v == null) return
        if (Array.isArray(v)) {
          v.forEach((item) => append(k, item))
        } else if (typeof v === 'object') {
          Object.entries(v as Record<string, unknown>).forEach(([sk, sv]) => append(sk, sv))
        } else {
          parts.push(`${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
        }
      }
      Object.entries(params).forEach(([k, v]) => append(k, v))
      return parts.join('&')
    },
  },
})

/** 是否正在刷新（避免并发 1101 触发多次刷新） */
let refreshing: Promise<string> | null = null

/**
 * 刷新 AccessToken（带去重）
 * 任意时刻只有一个真正的请求
 */
function doRefresh(): Promise<string> {
  if (refreshing) return refreshing
  const rt = storage.getString(REFRESH_TOKEN_KEY)
  if (!rt) {
    return Promise.reject(new ApiError(BusinessCode.REFRESH_TOKEN_INVALID, '未登录'))
  }
  refreshing = refreshTokenApi({ refreshToken: rt })
    .then((vo) => {
      if (tokenRefreshedHandler) {
        tokenRefreshedHandler(vo.accessToken, vo.refreshToken)
      } else {
        storage.setString(TOKEN_KEY, vo.accessToken)
        storage.setString(REFRESH_TOKEN_KEY, vo.refreshToken)
      }
      return vo.accessToken
    })
    .finally(() => {
      refreshing = null
    })
  return refreshing
}

/** 请求拦截器：自动加 token */
instance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const ext = config as ExtendedAxiosRequestConfig
    if (config.headers && !config.headers.has('Authorization')) {
      const token = storage.getString(TOKEN_KEY)
      if (token) {
        config.headers.set('Authorization', `Bearer ${token}`)
      }
    }
    // multipart/form-data：让浏览器自动设置 Content-Type（含 boundary），必须显式移除默认 JSON
    if (config.data instanceof FormData) {
      if (config.headers?.has('Content-Type')) {
        config.headers.delete('Content-Type')
      }
    } else if (!config.headers?.has('Content-Type')) {
      config.headers?.set('Content-Type', 'application/json;charset=UTF-8')
    }
    if (ext.showProgress !== false) NProgress.start()
    return config
  },
  (error) => {
    NProgress.done()
    return Promise.reject(error)
  }
)

/** 响应拦截器：解包 R<T>、自动刷新、统一错误处理 */
instance.interceptors.response.use(
  async (response: AxiosResponse) => {
    NProgress.done()
    const res = response.data
    if (res == null) return response

    // 非标准响应（mock/raw 等），直接透传
    if (typeof res !== 'object' || !('code' in res)) {
      return res
    }

    const { code, msg, data } = res as {
      code: number
      msg: string
      data: unknown
    }

    // 业务成功：解包 data
    if (code === BusinessCode.SUCCESS) {
      return data
    }

    const config = response.config as InternalAxiosRequestConfig & { _retry?: boolean }

    // 1101 = AccessToken 过期/无效 → 尝试 refresh + 重发
    if (
      code === BusinessCode.TOKEN_INVALID &&
      !config._retry &&
      !config.url?.includes('/auth/refresh') &&
      !config.url?.includes('/auth/login')
    ) {
      config._retry = true
      try {
        const newToken = await doRefresh()
        if (config.headers) {
          config.headers.set('Authorization', `Bearer ${newToken}`)
        }
        return instance.request(config)
      } catch {
        // refresh 失败 → 交给 1102 路径处理
        handleUnauthorized('登录已过期，请重新登录')
        return Promise.reject(new ApiError(BusinessCode.REFRESH_TOKEN_INVALID, '登录已过期'))
      }
    }

    // 1102 / 1103 = RefreshToken 失效或缺失 → 强制登出
    if (code === BusinessCode.REFRESH_TOKEN_INVALID || code === BusinessCode.TOKEN_MISSING) {
      handleUnauthorized(msg)
      return Promise.reject(new ApiError(code, msg || '请重新登录'))
    }

    // 1104 = 无权限
    if (code === BusinessCode.FORBIDDEN) {
      ElMessage.error(msg || '无访问权限')
      return Promise.reject(new ApiError(code, msg || '无访问权限', data))
    }

    // 1002 = 验证码错误（仅登录页需要，向上抛）
    if (code === BusinessCode.CAPTCHA_INVALID) {
      return Promise.reject(new ApiError(code, msg || '验证码错误', data))
    }

    // 1005 = 用户名已存在（仅注册页需要，向上抛）
    if (code === BusinessCode.USERNAME_EXISTS) {
      return Promise.reject(new ApiError(code, msg || '用户名已被占用', data))
    }

    // 2001 = 样本不存在
    if (code === BusinessCode.SAMPLE_NOT_FOUND) {
      return Promise.reject(new ApiError(code, msg || '样本不存在', data))
    }

    // 3001 / 2005 = 参数/类型错误
    if (code === BusinessCode.PARAM_INVALID || code === BusinessCode.FILE_TYPE_INVALID) {
      ElMessage.error(msg || '参数错误')
      return Promise.reject(new ApiError(code, msg || '参数错误', data))
    }

    // 4001 = 频率超限
    if (code === BusinessCode.RATE_LIMIT) {
      ElMessage.warning(msg || '操作过于频繁，请稍后重试')
      return Promise.reject(new ApiError(code, msg || '操作过于频繁', data))
    }

    // 5000 / 5100 / 5200 / 5300 = 系统异常
    if (
      code === BusinessCode.INTERNAL_ERROR ||
      code === BusinessCode.DB_ERROR ||
      code === BusinessCode.REDIS_ERROR ||
      code === BusinessCode.CONFIG_ERROR
    ) {
      ElMessage.error(msg || '服务异常，请稍后重试')
      return Promise.reject(new ApiError(code, msg || '服务异常', data))
    }

    // 2004 = 文件过大
    if (code === BusinessCode.FILE_TOO_LARGE) {
      ElMessage.error(msg || '文件过大，请压缩后再传')
      return Promise.reject(new ApiError(code, msg || '文件过大', data))
    }

    // 其他业务错误
    ElMessage.error(msg || '请求失败')
    return Promise.reject(new ApiError(code, msg || '请求失败', data))
  },
  (error) => {
    NProgress.done()
    if (error?.response) {
      const { status, data } = error.response
      const msg = (data && (data.msg || data.message)) || error.message || '请求失败'
      if (status === 401) {
        handleUnauthorized(msg)
      } else if (status === 403) {
        ElMessage.error('无访问权限')
      } else if (status === 404) {
        ElMessage.error(msg || '资源不存在')
      } else if (status >= 500) {
        ElMessage.error('服务异常，请稍后重试')
      } else {
        ElMessage.error(msg)
      }
      return Promise.reject(new ApiError(status, msg, data))
    }
    if (error?.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请检查网络')
    } else {
      ElMessage.error(error?.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

let unauthorizedPrompted = false
function handleUnauthorized(msg?: string): void {
  storage.remove(TOKEN_KEY)
  storage.remove(REFRESH_TOKEN_KEY)
  if (unauthorizedHandler) {
    unauthorizedHandler()
    return
  }
  if (unauthorizedPrompted) return
  unauthorizedPrompted = true

  // 用顶部 toast 替代模态框，体验更轻量、灵动；3s 后自动跳转登录页
  ElMessage({
    type: 'warning',
    message: msg || '登录已过期，请重新登录',
    duration: 3000,
    customClass: 'hw-unauthorized-toast',
    onClose: () => {
      unauthorizedPrompted = false
      window.location.href = '/login'
    },
  })
  // 兜底：toast 关闭事件在某些场景（duration:0 / 手动关闭）下不一定触发，
  // 这里再设一个定时器确保一定跳转到登录页
  window.setTimeout(() => {
    unauthorizedPrompted = false
    window.location.href = '/login'
  }, 3200)
}

export interface RequestOptions {
  showError?: boolean
  showProgress?: boolean
  silent?: boolean
}

export const request = {
  get<T = unknown>(
    url: string,
    params?: Record<string, unknown>,
    _options?: RequestOptions
  ): Promise<T> {
    return instance.get(url, { params }) as unknown as Promise<T>
  },
  post<T = unknown>(url: string, data?: unknown, _options?: RequestOptions): Promise<T> {
    return instance.post(url, data) as unknown as Promise<T>
  },
  put<T = unknown>(url: string, data?: unknown, _options?: RequestOptions): Promise<T> {
    return instance.put(url, data) as unknown as Promise<T>
  },
  delete<T = unknown>(
    url: string,
    params?: Record<string, unknown>,
    _options?: RequestOptions
  ): Promise<T> {
    return instance.delete(url, { params }) as unknown as Promise<T>
  },
  patch<T = unknown>(url: string, data?: unknown, _options?: RequestOptions): Promise<T> {
    return instance.patch(url, data) as unknown as Promise<T>
  },
  /**
   * 文件上传（multipart/form-data）
   * - Content-Type 由浏览器自动设置（boundary）
   * - 上传进度通过 onUploadProgress 回调
   */
  upload<T = unknown>(
    url: string,
    formData: FormData,
    onProgress?: (percent: number) => void,
    _options?: RequestOptions
  ): Promise<T> {
    return instance.post(url, formData, {
      onUploadProgress: (e) => {
        if (onProgress && e.total) {
          onProgress(Math.round((e.loaded * 100) / e.total))
        }
      },
    }) as unknown as Promise<T>
  },
  raw: instance,
}

export default request

import axios, {
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { storage } from './storage'
import { TOKEN_KEY } from './constants'
import { BusinessCode } from '@/api/contracts/common'

NProgress.configure({ showSpinner: false, trickleSpeed: 200 })

/**
 * 统一业务异常
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

/** Token 失效回调，由业务注册 */
type OnUnauthorized = () => void
let unauthorizedHandler: OnUnauthorized | null = null
export function setUnauthorizedHandler(fn: OnUnauthorized): void {
  unauthorizedHandler = fn
}

const baseURL = import.meta.env.VITE_API_BASE_URL || '/v1'

const instance: AxiosInstance = axios.create({
  baseURL,
  timeout: 30_000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
  },
  withCredentials: false,
})

/** 请求拦截器 */
instance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    NProgress.start()
    const token = storage.getString(TOKEN_KEY)
    if (token && config.headers) {
      config.headers.set('Authorization', `Bearer ${token}`)
    }
    return config
  },
  (error) => {
    NProgress.done()
    return Promise.reject(error)
  }
)

/** 响应拦截器 */
instance.interceptors.response.use(
  (response: AxiosResponse) => {
    NProgress.done()
    const res = response.data
    if (res == null) {
      return response
    }
    if (typeof res === 'object' && 'code' in res) {
      const { code, msg, data } = res as { code: number; msg: string; data: unknown }
      if (code === BusinessCode.SUCCESS) {
        return data
      }
      if (code === BusinessCode.UNAUTHORIZED) {
        handleUnauthorized(msg)
        return Promise.reject(new ApiError(code, msg || '未登录或登录已过期'))
      }
      if (code === BusinessCode.FORBIDDEN) {
        ElMessage.error(msg || '无访问权限')
        return Promise.reject(new ApiError(code, msg || '无访问权限', data))
      }
      ElMessage.error(msg || '请求失败')
      return Promise.reject(new ApiError(code, msg || '请求失败', data))
    }
    return res
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
  if (unauthorizedHandler) {
    unauthorizedHandler()
    return
  }
  if (unauthorizedPrompted) return
  unauthorizedPrompted = true
  ElMessageBox.alert(msg || '登录已过期，请重新登录', '提示', {
    confirmButtonText: '去登录',
    type: 'warning',
  })
    .then(() => {
      window.location.href = '/login'
    })
    .finally(() => {
      unauthorizedPrompted = false
    })
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
  upload<T = unknown>(
    url: string,
    formData: FormData,
    onProgress?: (percent: number) => void,
    options?: RequestOptions
  ): Promise<T> {
    return instance.post(url, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
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

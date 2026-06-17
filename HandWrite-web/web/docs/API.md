# 前端接口实现技术文档

> **目标读者**：HandWrite-web 前端开发人员
> **接口来源**：[`HandWrite-client/api.json`](../HandWrite-client/api.json)（OpenAPI 3.0.1，14 个接口）
> **后端框架**：Spring Boot 3 + JWT + 对象存储
> **基线版本**：HandWrite-client v1.0.0

***

## 目录

1. [接口总览](#1-接口总览)
2. [统一规范](#2-统一规范)
3. [鉴权与 Token 管理](#3-鉴权与-token-管理)
4. [请求/响应数据格式](#4-请求响应数据格式)
5. [错误处理机制](#5-错误处理机制)
6. [API 封装策略](#6-api-封装策略)
7. [各模块接口调用示例](#7-各模块接口调用示例)
8. [对象存储直传实现](#8-对象存储直传实现)
9. [TypeScript 类型定义](#9-typescript-类型定义)
10. [性能优化建议](#10-性能优化建议)
11. [测试与 Mock](#11-测试与-mock)
12. [附录：错误码字典](#12-附录错误码字典)

***

## 1. 接口总览

### 1.1 服务信息

| 项               | 值                                     |
| --------------- | ------------------------------------- |
| **BaseURL（开发）** | `http://127.0.0.1:8080`               |
| **BaseURL（生产）** | `https://api.your-domain.com`         |
| **统一前缀**        | `/v1`                                 |
| **认证方式**        | `Authorization: Bearer <accessToken>` |
| **Token 类型**    | JWT（Access 15min / Refresh 7d）        |
| **CORS**        | 仅允许 `app.cors.allowed-origins` 白名单    |
| **请求/响应格式**     | `application/json; charset=utf-8`     |
| **时间格式**        | ISO-8601 `yyyy-MM-ddTHH:mm:ss`        |
| **接口文档**        | `/swagger-ui.html` / `/v3/api-docs`   |

### 1.2 接口清单

| 模块     | 方法     | 路径                       | 鉴权        | 描述              |
| ------ | ------ | ------------------------ | --------- | --------------- |
| **鉴权** | GET    | `/v1/auth/captcha`       | ❌         | 获取图形验证码         |
| <br /> | POST   | `/v1/auth/register`      | ❌         | 用户注册            |
| <br /> | POST   | `/v1/auth/login`         | ❌         | 用户登录            |
| <br /> | POST   | `/v1/auth/refresh`       | ❌         | 刷新 Access Token |
| <br /> | POST   | `/v1/auth/logout`        | ✅         | 用户注销            |
| **用户** | GET    | `/v1/user/profile`       | ✅         | 获取个人信息          |
| <br /> | PUT    | `/v1/user/profile`       | ✅         | 修改个人信息          |
| **文件** | POST   | `/v1/file/sign`          | ✅         | 获取对象存储直传签名      |
| **样本** | POST   | `/v1/sample/upload`      | ✅         | 上传样本（元数据）       |
| <br /> | GET    | `/v1/sample/page`        | ✅         | 分页查询我的样本        |
| <br /> | GET    | `/v1/sample/{id}`        | ✅         | 样本详情            |
| <br /> | DELETE | `/v1/sample/{id}`        | ✅         | 删除样本            |
| **字典** | GET    | `/v1/dict/chars`         | ✅         | 分页查询字符字典        |
| <br /> | GET    | `/v1/dict/chars/list`    | ✅         | 按分类列出字符         |
| **审核** | GET    | `/v1/audit/pending`      | ✅ (ADMIN) | 待审核列表           |
| <br /> | GET    | `/v1/audit/history`      | ✅ (ADMIN) | 审核历史            |
| <br /> | POST   | `/v1/audit/{id}/approve` | ✅ (ADMIN) | 审核通过            |
| <br /> | POST   | `/v1/audit/{id}/reject`  | ✅ (ADMIN) | 审核驳回            |
| **统计** | GET    | `/v1/stats/overview`     | ✅         | 总览数据            |
| <br /> | GET    | `/v1/stats/trend`        | ✅         | 样本趋势            |

> ⚠️ 接口清单来自 `api.json` 实际定义；如后端新增接口，须同步更新本表与下方 `services/*.ts`。

***

## 2. 统一规范

### 2.1 URL 规范

```
{BaseURL}/v1/{module}/{action}
        ↓        ↓
       /v1   业务模块名（auth/user/sample/...）
```

- 全部小写、复数语义（`users` 而非 `user`）
- 路径参数（id）放在 URL 末尾：`/v1/sample/{id}`
- 查询参数使用 camelCase：`pageNum` / `pageSize` / `status`

### 2.2 命名规范

| 维度     | 规范           | 示例                     |
| ------ | ------------ | ---------------------- |
| 文件     | kebab-case   | `auth.service.ts`      |
| 变量     | camelCase    | `accessToken`          |
| 类      | PascalCase   | `AuthService`          |
| 常量     | UPPER\_SNAKE | `BASE_URL`             |
| 接口/类型  | PascalCase   | `LoginVO`, `SampleDTO` |
| DTO 后缀 | DTO（入参）      | `LoginDTO`             |
| VO 后缀  | VO（出参）       | `LoginVO`              |

### 2.3 时间与数字

| 类型    | 格式               | 例子                        |
| ----- | ---------------- | ------------------------- |
| 时间    | ISO-8601 string  | `2026-06-15T22:00:00`     |
| 时间戳   | number（毫秒）       | `1750000000000`           |
| 整数 ID | number（int64）    | `123`                     |
| 金额    | number（**分**为单位） | `199 = 1.99元`             |
| 布尔    | 0/1 整数           | `status: 0` / `status: 1` |

***

## 3. 鉴权与 Token 管理

### 3.1 Token 生命周期

```
登录 / 注册
  ↓
返回 {accessToken (15min), refreshToken (7d)}
  ↓
所有受保护请求带: Authorization: Bearer <accessToken>
  ↓
  / 401 (token 过期) ─── 调 /v1/auth/refresh ──→ 拿到新 accessToken
  ↓                                              ↓
重发原请求                                旧 token 失效，写入新 token
```

### 3.2 存储策略（推荐）

| Token            | 存储位置                               | 理由       |
| ---------------- | ---------------------------------- | -------- |
| **accessToken**  | 内存（Pinia store）                    | 短生命周期，敏感 |
| **refreshToken** | `httpOnly` cookie 或 `localStorage` | 跨页面持久化   |
| **userInfo**     | Pinia store                        | 配合 UI 渲染 |

> ⚠️ 不要把 `accessToken` 放到 `localStorage`（防 XSS）。\
> ⚠️ 不要把 `refreshToken` 放到非 `httpOnly` cookie。

### 3.3 自动刷新实现

```typescript
// utils/auth.ts
import { useAuthStore } from '@/stores/auth'

let refreshing: Promise<string> | null = null

export async function refreshAccessToken(): Promise<string> {
  if (refreshing) return refreshing
  const auth = useAuthStore()
  refreshing = axios
    .post<LoginVO>('/v1/auth/refresh', {
      refreshToken: auth.refreshToken,
    })
    .then((res) => {
      const { accessToken, refreshToken, user } = res.data.data
      auth.setTokens(accessToken, refreshToken, user)
      return accessToken
    })
    .finally(() => { refreshing = null })
  return refreshing
}
```

***

## 4. 请求/响应数据格式

### 4.1 请求格式

| 项      | 规范                                              |
| ------ | ----------------------------------------------- |
| Method | `GET` / `POST` / `PUT` / `DELETE`               |
| Header | `Content-Type: application/json; charset=utf-8` |
| Header | `Authorization: Bearer <token>`（受保护接口）          |
| Body   | JSON object；`GET` 无 body                        |

### 4.2 响应统一格式 `R<T>`

所有接口（无论成功/失败）都包裹在 `R` 结构中：

```typescript
interface R<T> {
  code: number         // 业务码，0 = 成功
  msg: string          // 提示信息（中文）
  data: T | object     // 业务数据，无数据时为 {}
  ts: number           // 服务器时间戳（毫秒）
  success: boolean     // code === 0 时为 true
}
```

### 4.3 成功响应示例

```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": {
      "id": 1,
      "username": "alice",
      "nickname": "Alice",
      "roles": ["USER"],
      "permissions": ["sample:upload"]
    }
  },
  "ts": 1750000000000,
  "success": true
}
```

### 4.4 失败响应示例

```json
{
  "code": 1001,
  "msg": "用户名或密码错误",
  "data": {},
  "ts": 1750000000000,
  "success": false
}
```

### 4.5 分页结构 `PageResult<T>`

```typescript
interface PageResult<T> {
  total: number       // 总记录数
  pageNum: number     // 当前页码（1-based）
  pageSize: number    // 每页大小
  records: T[]        // 数据列表
}
```

**分页请求 DTO**：

```typescript
interface PageQuery {
  pageNum: number     // 页码，默认 1
  pageSize: number    // 每页，默认 20，最大 100
}
```

**请求示例**：

```
GET /v1/sample/page?status=0&query=PageQuery(pageNum=1,pageSize=20)
```

> 后端用 Spring MVC，`PageQuery` 是 query 复杂对象（`@ModelAttribute`），前端用 `qs.stringify()` 序列化。

***

## 5. 错误处理机制

### 5.1 错误码体系

按 `5xxxx` 分段（前缀见 `ErrorCode.java`）：

| 段      | 含义    | 例子                        |
| ------ | ----- | ------------------------- |
| `0`    | 成功    | 0                         |
| `1xxx` | 用户/认证 | 1001 用户名密码错，1101 token 无效 |
| `2xxx` | 业务校验  | 2001 样本不存在                |
| `5xxx` | 系统异常  | 5000 服务器内部错误，5300 配置错误    |

**常用错误码**（详见 §12）：

| code | 含义          | 前端处理                   |
| ---- | ----------- | ---------------------- |
| 0    | 成功          | 正常流程                   |
| 1001 | 用户名/密码错     | 弹 toast，提示用户           |
| 1002 | 验证码错        | 刷新验证码，提示重输             |
| 1101 | Token 无效/过期 | 尝试 refresh，失败则跳转登录     |
| 2001 | 样本不存在       | 404 页面                 |
| 5000 | 服务器异常       | 弹 toast "服务暂不可用，请稍后重试" |

### 5.2 HTTP 状态码

| 状态码 | 含义                  | 前端处理           |
| --- | ------------------- | -------------- |
| 200 | 业务成功或业务失败（看 R.code） | 查 `R.code`     |
| 400 | 参数校验失败              | 弹窗显示 `msg`     |
| 401 | 未登录 / Token 无效      | 跳转登录页          |
| 403 | 无权限                 | 弹 toast "无权操作" |
| 404 | 接口不存在               | 404 页面         |
| 500 | 服务器异常               | 弹 toast + 上报   |

> ⚠️ **后端 200 + code != 0** = 业务错误。**HTTP 4xx/5xx** = 传输错误。两者都要处理。

### 5.3 错误处理流程

```typescript
// 拦截器统一处理
axios.interceptors.response.use(
  (response) => {
    const r: R<any> = response.data
    if (r.code === 0) return r.data   // 业务成功 → 解包
    if (r.code === 1101) {            // token 失效
      return refreshAndRetry(response.config)
    }
    if (r.code === 1102) {            // refresh 失效
      authStore.logout()
      router.push('/login')
    }
    showErrorToast(r.msg)             // 业务错误弹窗
    return Promise.reject(r)
  },
  (error) => {                        // HTTP 错误
    if (error.response?.status === 401) {
      authStore.logout()
    }
    showErrorToast('网络异常，请稍后重试')
    return Promise.reject(error)
  }
)
```

***

## 6. API 封装策略

### 6.1 分层架构

```
src/
├── api/                          # 第 1 层：基础请求
│   ├── request.ts                # axios 实例 + 拦截器
│   └── types.ts                  # R<T> / PageQuery / PageResult
├── services/                     # 第 2 层：业务服务
│   ├── auth.service.ts
│   ├── user.service.ts
│   ├── sample.service.ts
│   ├── file.service.ts
│   ├── dict.service.ts
│   ├── audit.service.ts
│   └── stats.service.ts
├── stores/                       # 第 3 层：状态管理
│   ├── auth.ts                   # token + userInfo
│   └── ...
└── views/                        # 第 4 层：页面调用
    └── login/
        └── index.vue
```

### 6.2 基础请求层 `request.ts`

```typescript
// api/request.ts
import axios, { AxiosInstance, AxiosRequestConfig } from 'axios'
import { useAuthStore } from '@/stores/auth'
import { refreshAccessToken } from '@/utils/auth'
import type { R } from './types'

const instance: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8080',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json;charset=UTF-8' },
})

// 请求拦截器：自动加 token
instance.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.accessToken) {
    config.headers.Authorization = `Bearer ${auth.accessToken}`
  }
  return config
})

// 响应拦截器：解包 R + 自动刷新
instance.interceptors.response.use(
  async (response) => {
    const r: R<any> = response.data
    if (r.code === 0) {
      response.data = r.data  // 解包，前端直接拿到 data
      return response
    }

    // 1101 = token 过期/无效 → 尝试 refresh
    if (r.code === 1101 && !response.config._retry) {
      response.config._retry = true
      try {
        const newToken = await refreshAccessToken()
        response.config.headers.Authorization = `Bearer ${newToken}`
        return instance(response.config)
      } catch {
        // refresh 失败
      }
    }

    // 业务错误
    if (r.code === 1102 || r.code === 1103) {
      const auth = useAuthStore()
      auth.clear()
      window.location.href = '/login'
    }

    return Promise.reject({
      code: r.code,
      msg: r.msg,
      config: response.config,
    })
  },
  (error) => {
    // HTTP 错误（4xx/5xx/网络异常）
    const status = error.response?.status
    if (status === 401) {
      const auth = useAuthStore()
      auth.clear()
      window.location.href = '/login'
    }
    return Promise.reject({
      code: status || -1,
      msg: error.response?.data?.msg || error.message || '网络异常',
    })
  }
)

export function request<T = any>(config: AxiosRequestConfig): Promise<T> {
  return instance(config) as unknown as Promise<T>
}
```

### 6.3 业务服务层

每个服务一个文件，**只导出函数**，不暴露 axios：

```typescript
// services/auth.service.ts
import { request } from '@/api/request'
import type { LoginDTO, LoginVO, RegisterDTO, RefreshTokenDTO, CaptchaVO } from './types'

export const authService = {
  /** 获取图形验证码 */
  getCaptcha: () =>
    request<R<CaptchaVO>>({ url: '/v1/auth/captcha', method: 'GET' }),

  /** 登录 */
  login: (data: LoginDTO) =>
    request<R<LoginVO>>({ url: '/v1/auth/login', method: 'POST', data }),

  /** 注册 */
  register: (data: RegisterDTO) =>
    request<R<LoginVO>>({ url: '/v1/auth/register', method: 'POST', data }),

  /** 刷新 token */
  refresh: (data: RefreshTokenDTO) =>
    request<R<LoginVO>>({ url: '/v1/auth/refresh', method: 'POST', data }),

  /** 注销 */
  logout: (data: RefreshTokenDTO) =>
    request<R<void>>({ url: '/v1/auth/logout', method: 'POST', data }),
}
```

### 6.4 Pinia Store 集成

```typescript
// stores/auth.ts
import { defineStore } from 'pinia'
import type { UserInfo } from '@/services/types'

interface AuthState {
  accessToken: string
  refreshToken: string
  user: UserInfo | null
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    accessToken: localStorage.getItem('accessToken') || '',
    refreshToken: localStorage.getItem('refreshToken') || '',
    user: JSON.parse(localStorage.getItem('user') || 'null'),
  }),

  actions: {
    setTokens(accessToken: string, refreshToken: string, user: UserInfo) {
      this.accessToken = accessToken
      this.refreshToken = refreshToken
      this.user = user
      localStorage.setItem('accessToken', accessToken)
      localStorage.setItem('refreshToken', refreshToken)
      localStorage.setItem('user', JSON.stringify(user))
    },

    clear() {
      this.accessToken = ''
      this.refreshToken = ''
      this.user = null
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('user')
    },
  },
})
```

### 6.5 Vue 组件调用

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { authService } from '@/services/auth.service'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()
const form = ref({ username: '', password: '', captchaKey: '', captchaCode: '' })

async function handleLogin() {
  try {
    const vo = await authService.login(form.value)
    auth.setTokens(vo.accessToken, vo.refreshToken, vo.user)
    router.push('/home')
  } catch (err: any) {
    ElMessage.error(err.msg)
  }
}
</script>
```

***

## 7. 各模块接口调用示例

### 7.1 鉴权模块

```typescript
// 1. 获取验证码
const captcha = await authService.getCaptcha()
// captcha = { captchaKey: 'uuid-xxxx', imageBase64: 'data:image/png;base64,...', expireSeconds: 300 }
// <img :src="captcha.imageBase64" /> + 用户输入

// 2. 登录
const vo = await authService.login({
  username: 'alice',
  password: 'Aa123456',
  captchaKey: 'uuid-xxxx',
  captchaCode: 'a8K2',
})
// vo = { accessToken, refreshToken, tokenType, expiresIn, user }

// 3. 注册
await authService.register({
  username: 'bob',
  password: 'Bb123456',
  nickname: 'Bob',
  email: 'bob@example.com',
})

// 4. 刷新
const newVo = await authService.refresh({ refreshToken: vo.refreshToken })

// 5. 注销
await authService.logout({ refreshToken: vo.refreshToken })
```

### 7.2 用户模块

```typescript
// services/user.service.ts
export const userService = {
  getProfile: () => request<R<UserVO>>({ url: '/v1/user/profile', method: 'GET' }),

  updateProfile: (data: UserProfileDTO) =>
    request<R<UserVO>>({ url: '/v1/user/profile', method: 'PUT', data }),
}
```

```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { userService } from '@/services/user.service'

const profile = ref<UserVO>()

onMounted(async () => {
  profile.value = await userService.getProfile()
})

async function save() {
  await userService.updateProfile({
    nickname: 'New Name',
    email: 'new@example.com',
  })
  ElMessage.success('保存成功')
}
</script>
```

### 7.3 字符字典模块

```typescript
// services/dict.service.ts
export const dictService = {
  /** 分页查询 */
  page: (params: { category?: string; pageNum: number; pageSize: number }) =>
    request<R<PageResult<CharDict>>>({
      url: '/v1/dict/chars',
      method: 'GET',
      params,
      paramsSerializer: { indexes: null },  // 让 axios 展开嵌套对象
    }),

  /** 按分类列出（缓存友好，量小时优选） */
  list: (category?: string) =>
    request<R<CharDict[]>>({
      url: '/v1/dict/chars/list',
      method: 'GET',
      params: { category },
    }),
}
```

> **注意**：后端 `PageQuery` 是 query 复杂对象。**必须配置** **`paramsSerializer`**（或者用 `qs.stringify`），否则 axios 默认序列化会把 `query: {pageNum: 1}` 变成 `query[pageNum]=1`，后端收不到。

### 7.4 审核模块（管理员）

```typescript
// services/audit.service.ts
export const auditService = {
  /** 待审核列表 */
  pending: (pageNum = 1, pageSize = 20) =>
    request<R<PageResult<SampleVO>>>({
      url: '/v1/audit/pending',
      method: 'GET',
      params: { pageNum, pageSize },
      paramsSerializer: { indexes: null },
    }),

  /** 审核历史 */
  history: (status: number, pageNum = 1, pageSize = 20) =>
    request<R<PageResult<SampleVO>>>({
      url: '/v1/audit/history',
      method: 'GET',
      params: { status, pageNum, pageSize },
      paramsSerializer: { indexes: null },
    }),

  /** 审核通过 */
  approve: (id: number) =>
    request<R<void>>({ url: `/v1/audit/${id}/approve`, method: 'POST' }),

  /** 审核驳回 */
  reject: (id: number, reason: string) =>
    request<R<void>>({ url: `/v1/audit/${id}/reject`, method: 'POST', data: { reason } }),
}
```

### 7.5 统计模块

```typescript
// services/stats.service.ts
export const statsService = {
  overview: () =>
    request<R<Record<string, any>>>({ url: '/v1/stats/overview', method: 'GET' }),

  trend: (days = 30) =>
    request<R<Record<string, any>>>({
      url: '/v1/stats/trend',
      method: 'GET',
      params: { days },
    }),
}
```

***

## 8. 对象存储直传实现

### 8.1 整体流程

```
┌──────────┐                              ┌──────────┐
│  前端    │ 1. POST /v1/file/sign        │  后端    │
│          │  { purpose, ext, bizId? }   │          │
│          │ ─────────────────────────►  │          │
│          │                              │ 生成签名 │
│          │  { uploadUrl, objectKey,    │   URL    │
│          │    method, expireSeconds,   │          │
│          │    requiredHeader? }        │          │
│          │ ◄─────────────────────────  │          │
│          │                              └──────────┘
│ 2. PUT   │                              ┌──────────┐
│  直传    │  uploadUrl + body=file      │  对象存储 │
│          │ ─────────────────────────►  │ (MinIO/  │
│          │  （不经过后端）              │  Azure)  │
│          │ ◄─────────────────────────  │          │
│          │  200 OK / 400 失败           └──────────┘
│ 3. POST  │
│ /v1/     │  { charId, fileKey,        ┌──────────┐
│  sample/ │    fileSize, sha256,       │  后端    │
│  upload  │    device }                │          │
│          │ ─────────────────────────►  │ 落库     │
│          │ ◄─────────────────────────  │          │
└──────────┘  { id, fileUrl, status }   └──────────┘
```

### 8.2 完整实现

```typescript
// services/file.service.ts + services/sample.service.ts
import { request } from '@/api/request'

interface FileSignVO {
  bucket: string
  objectKey: string
  uploadUrl: string
  method: 'PUT'
  expireSeconds: number
  requiredHeader?: string  // e.g. "x-ms-blob-type:BlockBlob"（Azure 必须）
}

interface FileSignDTO {
  purpose: 'AVATAR' | 'SAMPLE_FILE' | 'AUDIT_FILE'
  ext: string             // "png" | "jpg" | "jpeg" | "pdf"
  bizId?: number
}

export const fileService = {
  /** 获取直传签名 */
  sign: (data: FileSignDTO) =>
    request<R<FileSignVO>>({ url: '/v1/file/sign', method: 'POST', data }),
}

// composables/useDirectUpload.ts
import { ref } from 'vue'
import { fileService } from '@/services/file.service'
import { sampleService } from '@/services/sample.service'

export function useDirectUpload() {
  const progress = ref(0)
  const uploading = ref(false)

  async function upload(
    file: File,
    purpose: FileSignDTO['purpose'],
    meta: { charId: number; device: string }
  ) {
    uploading.value = true
    progress.value = 0

    try {
      // 1) 拿签名
      const ext = file.name.split('.').pop()?.toLowerCase() || 'png'
      const sign = await fileService.sign({ purpose, ext, bizId: meta.charId })

      // 2) 计算 SHA-256（可选）
      const sha256 = await fileSha256(file)

      // 3) PUT 直传到对象存储
      const headers: Record<string, string> = {}
      if (sign.requiredHeader) {
        const [k, v] = sign.requiredHeader.split(':')
        headers[k.trim()] = v.trim()
      }

      await new Promise<void>((resolve, reject) => {
        const xhr = new XMLHttpRequest()
        xhr.upload.onprogress = (e) => {
          if (e.lengthComputable) progress.value = Math.round((e.loaded / e.total) * 100)
        }
        xhr.onload = () => (xhr.status === 200 ? resolve() : reject(new Error(`HTTP ${xhr.status}`)))
        xhr.onerror = () => reject(new Error('Network error'))
        xhr.open(sign.method, sign.uploadUrl)
        Object.entries(headers).forEach(([k, v]) => xhr.setRequestHeader(k, v))
        xhr.send(file)
      })

      // 4) 调后端落库
      const sample = await sampleService.upload({
        charId: meta.charId,
        fileKey: sign.objectKey,
        fileSize: file.size,
        sha256,
        device: meta.device,
      })

      return sample
    } finally {
      uploading.value = false
    }
  }

  return { progress, uploading, upload }
}

async function fileSha256(file: File): Promise<string> {
  const buf = await file.arrayBuffer()
  const hash = await crypto.subtle.digest('SHA-256', buf)
  return Array.from(new Uint8Array(hash))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('')
}
```

### 8.3 上传组件

```vue
<!-- views/sample/CollectView.vue -->
<script setup lang="ts">
import { useDirectUpload } from '@/composables/useDirectUpload'

const { progress, uploading, upload } = useDirectUpload()
const file = ref<File | null>(null)
const charId = ref(1)

async function onSubmit() {
  if (!file.value) return
  try {
    const sample = await upload(file.value, 'SAMPLE_FILE', {
      charId: charId.value,
      device: 'WEB',
    })
    ElMessage.success(`上传成功，ID=${sample.id}`)
  } catch (err: any) {
    ElMessage.error(err.msg)
  }
}
</script>

<template>
  <el-upload v-model:file-list="file" :auto-upload="false" :limit="1" accept="image/*">
    <el-button>选择图片</el-button>
  </el-upload>
  <el-progress v-if="uploading" :percentage="progress" />
  <el-button @click="onSubmit" :loading="uploading">上传</el-button>
</template>
```

***

## 9. TypeScript 类型定义

把 `api.json` 的所有 schema 转为 TS 类型，统一放 `services/types.ts`：

```typescript
// services/types.ts
export interface R<T = any> {
  code: number
  msg: string
  data: T
  ts: number
  success: boolean
}

export interface PageQuery {
  pageNum: number
  pageSize: number
}

export interface PageResult<T> {
  total: number
  pageNum: number
  pageSize: number
  records: T[]
}

// ========== Auth ==========
export interface CaptchaVO {
  captchaKey: string
  imageBase64: string
  expireSeconds: number
}

export interface LoginDTO {
  username: string
  password: string
  captchaKey: string
  captchaCode: string
}

export interface RegisterDTO {
  username: string          // 3-64 字符，^[A-Za-z0-9_.-]+$
  password: string          // 6-64 字符
  nickname?: string
  email?: string
}

export interface RefreshTokenDTO {
  refreshToken: string
}

export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar?: string
  roles: string[]
  permissions: string[]
}

export interface LoginVO {
  accessToken: string
  refreshToken: string
  tokenType: 'Bearer'
  expiresIn: number
  user: UserInfo
}

// ========== User ==========
export interface UserProfileDTO {
  nickname?: string
  email?: string
  phone?: string
  avatar?: string
}

export interface UserVO {
  id: number
  username: string
  nickname: string
  email: string
  phone: string
  avatar: string
  status: number
  lastLoginTime: string
  createTime: string
}

// ========== File ==========
export interface FileSignDTO {
  purpose: 'AVATAR' | 'SAMPLE_FILE' | 'AUDIT_FILE'
  ext: string
  bizId?: number
}

export interface FileSignVO {
  bucket: string
  objectKey: string
  uploadUrl: string
  method: 'PUT'
  expireSeconds: number
  requiredHeader?: string
}

// ========== Sample ==========
export interface SampleUploadDTO {
  charId: number
  fileKey: string
  fileUrl?: string
  fileSize: number
  sha256?: string
  device?: string
}

export interface SampleVO {
  id: number
  userId: number
  charId: number
  fileKey: string
  fileUrl: string
  fileSize: number
  device: string
  status: number           // 0 待审核 1 通过 2 驳回
  rejectReason?: string
  auditedBy?: number
  auditedTime?: string
  createTime: string
}

// ========== Dict ==========
export interface CharDict {
  id: number
  charValue: string
  category: string
  difficulty: number       // 1-5
  description: string
  enabled: number          // 0/1
  createTime: string
  updateTime: string
}

// ========== Audit ==========
export interface AuditDecisionDTO {
  reason?: string
}
```

### 9.1 自动从 OpenAPI 生成类型（推荐）

安装 `openapi-typescript`：

```bash
pnpm add -D openapi-typescript
```

```bash
# 从后端实时生成
npx openapi-typescript http://127.0.0.1:8080/v3/api-docs -o src/api/types.d.ts

# 或从本地文件
npx openapi-typescript ../HandWrite-client/api.json -o src/api/types.d.ts
```

> ⚠️ 生成的类型是**只读**的，建议**手写**业务层的 VO/DTO（更易读、易重构）。

***

## 10. 性能优化建议

### 10.1 请求层

| 优化               | 实现                                          |
| ---------------- | ------------------------------------------- |
| **请求合并**         | 同一时刻的相同请求只发一次（axios adapter + key 缓存）       |
| **请求去抖**         | 搜索框输入用 300ms debounce                       |
| **取消过期请求**       | 路由切换 / 用户操作变更时，调用 `AbortController.abort()` |
| **批量接口**         | 后端提供批量接口时，前端用 `Promise.all`                 |
| **分页懒加载**        | 大列表用 infinite scroll，避免一次拉 1000 条           |
| **预取（prefetch）** | hover 链接时预取详情                               |

### 10.2 缓存层

| 策略                 | 场景       | 实现                       |
| ------------------ | -------- | ------------------------ |
| **Pinia 内存缓存**     | 字典、用户信息  | `useDictStore()`         |
| **SessionStorage** | 表单草稿     | `sessionStorage.setItem` |
| **LocalStorage**   | 主题、token | 启动时读入                    |
| **Vue Query**      | 复杂服务端状态  | `useQuery`/`useMutation` |
| **SWR 策略**         | 看板类弱一致性  | `stale-while-revalidate` |

### 10.3 网络层

| 优化            | 说明                    |
| ------------- | --------------------- |
| **Gzip 压缩**   | 后端启用，前端无感知            |
| **HTTP/2**    | 多路复用，减少连接数            |
| **CDN 加速**    | 静态资源走 CDN             |
| **图片优化**      | WebP、懒加载、响应式 `srcset` |
| **请求合并**      | 多个小请求合并为一个            |
| **WebSocket** | 审核通知、样本状态推送           |

### 10.4 渲染层

| 优化               | 说明                                  |
| ---------------- | ----------------------------------- |
| **虚拟列表**         | 10000+ 样本列表用 `vue-virtual-scroller` |
| **路由懒加载**        | `() => import('@/views/...')`       |
| **组件懒加载**        | `defineAsyncComponent`              |
| **Tree-shaking** | 按需引入 Element Plus                   |
| **Vite 构建优化**    | 分包、预构建、gzip                         |

### 10.5 上传优化（针对样本）

| 优化            | 说明                        |
| ------------- | ------------------------- |
| **分片上传**      | 大文件（>5MB）分片，每片 1MB        |
| **断点续传**      | 记录已上传分片                   |
| **并发上传**      | 多个样本并行（限 3 个）             |
| **本地预览**      | 选完图先本地显示，不阻塞              |
| **WebP 压缩**   | 客户端用 `canvas` 转 WebP，体积减半 |
| **直传 + 异步回调** | 已实现，避免经后端转                |

***

## 11. 测试与 Mock

### 11.1 Mock 服务（开发期）

安装 `msw`（Mock Service Worker）：

```bash
pnpm add -D msw
```

```typescript
// mocks/handlers.ts
import { http, HttpResponse } from 'msw'

export const handlers = [
  http.get('/v1/auth/captcha', () =>
    HttpResponse.json({
      code: 0,
      msg: 'ok',
      data: {
        captchaKey: 'mock-key',
        imageBase64: 'data:image/png;base64,iVBORw0...',
        expireSeconds: 300,
      },
      ts: Date.now(),
      success: true,
    })
  ),

  http.post('/v1/auth/login', async ({ request }) => {
    const body = await request.json() as any
    if (body.username === 'alice' && body.password === 'Aa123456') {
      return HttpResponse.json({
        code: 0,
        msg: 'ok',
        data: {
          accessToken: 'mock-access-token',
          refreshToken: 'mock-refresh-token',
          tokenType: 'Bearer',
          expiresIn: 900,
          user: {
            id: 1,
            username: 'alice',
            nickname: 'Alice',
            roles: ['USER'],
            permissions: ['sample:upload'],
          },
        },
        ts: Date.now(),
        success: true,
      })
    }
    return HttpResponse.json({
      code: 1001, msg: '用户名或密码错误', data: {}, ts: Date.now(), success: false,
    }, { status: 200 })
  }),
]
```

### 11.2 单元测试

```typescript
// tests/services/auth.service.spec.ts
import { authService } from '@/services/auth.service'
import { setupServer } from 'msw/node'
import { handlers } from '@/mocks/handlers'

const server = setupServer(...handlers)
beforeAll(() => server.listen())
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

test('login success', async () => {
  const vo = await authService.login({
    username: 'alice', password: 'Aa123456',
    captchaKey: 'k', captchaCode: 'c',
  })
  expect(vo.accessToken).toBe('mock-access-token')
})

test('login failure', async () => {
  await expect(
    authService.login({
      username: 'wrong', password: 'wrong',
      captchaKey: 'k', captchaCode: 'c',
    })
  ).rejects.toMatchObject({ code: 1001 })
})
```

### 11.3 E2E 测试（Playwright）

```typescript
// e2e/login.spec.ts
import { test, expect } from '@playwright/test'

test('login flow', async ({ page }) => {
  await page.goto('/login')
  await page.fill('input[name=username]', 'alice')
  await page.fill('input[name=password]', 'Aa123456')
  await page.fill('input[name=captcha]', 'abcd')
  await page.click('button[type=submit]')
  await expect(page).toHaveURL('/home')
})
```

***

## 12. 附录：错误码字典

> 来源：后端 [`ErrorCode.java`](../HandWrite-client/src/main/java/com/example/handwriting/common/result/ErrorCode.java)

| code | 标识                      | 含义               | 前端建议          |
| ---- | ----------------------- | ---------------- | ------------- |
| 0    | SUCCESS                 | 成功               | 正常流程          |
| 1001 | USER\_PASSWORD\_INVALID | 用户名或密码错误         | 弹 toast       |
| 1002 | CAPTCHA\_INVALID        | 验证码错误或已过期        | 刷新验证码         |
| 1003 | USER\_DISABLED          | 用户被禁用            | 弹窗提示并退出       |
| 1004 | USER\_NOT\_FOUND        | 用户不存在            | 引导注册          |
| 1005 | USERNAME\_EXISTS        | 用户名已被占用          | 弹 toast       |
| 1101 | TOKEN\_INVALID          | Token 无效或已过期     | 触发 refresh    |
| 1102 | REFRESH\_TOKEN\_INVALID | Refresh Token 无效 | 跳登录           |
| 1103 | TOKEN\_MISSING          | 未提供 Token        | 跳登录           |
| 1104 | FORBIDDEN               | 无权限操作            | 弹 toast + 跳首页 |
| 2001 | SAMPLE\_NOT\_FOUND      | 样本不存在            | 404 页面        |
| 2002 | SAMPLE\_AUDITED         | 样本已审核，不可重复操作     | 刷新列表          |
| 2003 | CHAR\_NOT\_FOUND        | 字符字典项不存在         | 提示并刷新         |
| 2004 | FILE\_TOO\_LARGE        | 文件超过大小限制         | 提示压缩后再传       |
| 2005 | FILE\_TYPE\_INVALID     | 文件类型不支持          | 提示支持的类型       |
| 3001 | PARAM\_INVALID          | 参数校验失败           | 弹窗显示 `msg`    |
| 4001 | RATE\_LIMIT             | 频率超限             | 提示稍后重试        |
| 5000 | INTERNAL\_ERROR         | 服务器内部错误          | 弹 toast + 上报  |
| 5100 | DB\_ERROR               | 数据库异常            | 弹 toast       |
| 5200 | REDIS\_ERROR            | Redis 异常         | 提示刷新          |
| 5300 | CONFIG\_ERROR           | 配置错误             | 联系管理员         |
| 9999 | UNKNOWN                 | 未知错误             | 弹 toast       |

***

## 附录 A：环境变量配置

前端 Vite 环境变量（`.env.development` / `.env.production`）：

```bash
# .env.development
VITE_API_BASE_URL=http://127.0.0.1:8080

# .env.production
VITE_API_BASE_URL=https://api.your-domain.com
```

**禁止**把后端密钥（DB 密码、JWT secret、Azure key）放到前端 env。

***

## 附录 B：常见问题 FAQ

### Q1：刷新 token 时多个并发请求怎么办？

**A**：用 `Promise` 缓存，单一 refresh 请求：

```typescript
let refreshing: Promise<string> | null = null
// 见 §3.3
```

### Q2：Token 过期但 refresh 失败怎么办？

**A**：清空本地 token + 跳登录页 + 提示用户。

### Q3：跨域问题（CORS）？

**A**：在 `application.yml` 的 `app.cors.allowed-origins` 加上前端地址（如 `http://localhost:5173`）。\
**禁止**把后端 CORS 设为 `*` + `allow-credentials: true`（浏览器会拒绝）。

### Q4：上传大文件失败？

**A**：

1. 检查后端 `UPLOAD_MAX_FILE_SIZE`（默认 10MB）
2. 检查对象存储单文件上限（MinIO 5GB / Azure 200GB 默认）
3. 实施分片上传（§10.5）

### Q5：接口返回 401 但 token 没过期？

**A**：

1. 检查时区（JWT exp 是 UTC）
2. 检查 `app.jwt.access-token-ttl-seconds` 是否被改过
3. 用 [jwt.io](https://jwt.io) 解码看 `exp` 字段

***

## 附录 C：变更记录

| 日期         | 版本    | 变更                      |
| ---------- | ----- | ----------------------- |
| 2026-06-15 | 1.0.0 | 初版，基于 `api.json` v1.0.0 |


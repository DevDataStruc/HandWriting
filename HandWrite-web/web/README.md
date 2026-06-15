# 手写体采集管理系统 - 前端

> 基于 **Vue 3.4 + Vite 5 + TypeScript 5** 的中文手写体样本采集与管理 Web 端，包含用户门户、样本采集端与管理员后台。

![Vue](https://img.shields.io/badge/Vue-3.4-42b883?logo=vue.js)
![Vite](https://img.shields.io/badge/Vite-5-646cff?logo=vite)
![TypeScript](https://img.shields.io/badge/TypeScript-5-3178c6?logo=typescript)
![Element Plus](https://img.shields.io/badge/Element_Plus-2-409eff)
![Pinia](https://img.shields.io/badge/Pinia-2-f7d336)
![License](https://img.shields.io/badge/License-MIT-green)

---

## 目录

- [项目概览](#项目概览)
- [技术栈](#技术栈)
- [快速开始](#快速开始)
- [项目结构](#项目结构)
- [可用脚本](#可用脚本)
- [环境变量](#环境变量)
- [设计系统](#设计系统)
- [核心特性](#核心特性)
- [附录](#附录)

## 项目概览

本项目是中文手写体样本采集与管理系统的 Web 前端，对应后端 Spring Boot + MySQL + 对象存储。它提供：

- **用户端**：登录/注册、字符提示书写、压感 Canvas 板、样本上传、历史样本管理
- **管理端**：数据看板、用户管理、样本审核（通过/驳回/批量）、统计分析、审计日志
- **公共门户**：首页、关于、公告

## 技术栈

| 类别 | 选型 | 版本 |
| --- | --- | --- |
| 开发语言 | TypeScript | 5.x |
| 框架 | Vue 3 (Composition API + `<script setup>`) | 3.4+ |
| 构建 | Vite | 5.x |
| 路由 | Vue Router | 4.x |
| 状态 | Pinia | 2.x |
| UI | Element Plus | 2.x |
| 样式 | SCSS + CSS 变量 | - |
| HTTP | Axios | 1.x |
| 手写 | perfect-freehand + signature_pad | - |
| 图表 | ECharts + vue-echarts | 5.x |
| 工具 | VueUse / dayjs / lodash-es / nprogress | - |
| 国际化 | vue-i18n | 9.x |
| 测试 | Vitest + @vue/test-utils | - |
| 代码规范 | ESLint + Prettier + Stylelint | - |
| 包管理 | pnpm | 9.x |

## 快速开始

### 1. 环境要求

- Node.js **20.x LTS** （建议）
- pnpm **9.x**（推荐） 或 npm 10+

```bash
node -v
pnpm -v
```

### 2. 安装依赖

```bash
pnpm install
```

### 3. 启动开发服务器

```bash
pnpm dev
```

默认运行在 [http://localhost:5173](http://localhost:5173)，已配置 `/v1` 代理到后端（默认 `http://localhost:8080`）。

### 4. 生产构建

```bash
pnpm build
```

构建产物输出至 `dist/`，并自动生成 `.gz` / `.br` 压缩文件。

### 5. 预览构建产物

```bash
pnpm preview
```

## 项目结构

```
web/
├── public/                        # 静态资源
├── mock/                          # 本地 mock 数据
├── src/
│   ├── api/                       # 接口层
│   │   ├── contracts/             # TS 契约类型
│   │   ├── auth.ts
│   │   ├── user.ts
│   │   ├── dict.ts
│   │   ├── sample.ts
│   │   ├── audit.ts
│   │   ├── stats.ts
│   │   ├── file.ts
│   │   └── index.ts
│   ├── assets/                    # 图片/SVG
│   ├── components/
│   │   ├── base/                  # 基础组件 (Button/Input/Card/Empty)
│   │   └── business/              # 业务组件 (HandwritingPad/...)
│   ├── composables/               # 组合式函数
│   ├── directives/                # 全局指令
│   ├── layouts/                   # 布局 (Default/Admin/Blank)
│   │   └── components/            # Header/Sidebar/Breadcrumb/Footer
│   ├── locales/                   # i18n
│   ├── router/                    # 路由 + 守卫
│   ├── stores/                    # Pinia 状态
│   ├── styles/                    # SCSS 主题
│   ├── types/                     # 全局类型
│   ├── utils/                     # 工具方法
│   ├── views/
│   │   ├── portal/                # 公共门户
│   │   ├── auth/                  # 登录/注册
│   │   ├── sample/                # 样本采集
│   │   ├── admin/                 # 管理后台
│   │   └── error/                 # 错误页
│   ├── App.vue
│   ├── main.ts
│   └── env.d.ts
├── index.html
├── package.json
├── vite.config.ts
├── tsconfig.json
├── tsconfig.node.json
├── .eslintrc.cjs
├── .prettierrc
├── .stylelintrc.cjs
├── .env.development
├── .env.production
└── README.md
```

## 可用脚本

| 命令 | 说明 |
| --- | --- |
| `pnpm dev` | 启动开发服务器（默认 :5173） |
| `pnpm build` | 类型检查 + 生产构建 |
| `pnpm build:prod` | 显式指定 production 模式构建 |
| `pnpm preview` | 预览生产产物 |
| `pnpm typecheck` | 仅做 TypeScript 类型检查 |
| `pnpm lint` | ESLint + 自动修复 |
| `pnpm lint:style` | Stylelint 检查 |
| `pnpm format` | Prettier 格式化 |
| `pnpm test` | 运行单元测试（Vitest） |
| `pnpm test:watch` | 监听模式测试 |
| `pnpm test:coverage` | 生成覆盖率报告 |
| `pnpm test:e2e` | Playwright E2E 测试 |

## 环境变量

在 `.env.development` / `.env.production` 中维护：

| 变量 | 说明 | 默认 |
| --- | --- | --- |
| `VITE_APP_TITLE` | 应用标题 | 手写体采集管理系统 |
| `VITE_API_BASE_URL` | API 基础 URL | `/v1` |
| `VITE_USE_MOCK` | 是否启用本地 mock | `true` |
| `VITE_PROXY_TARGET` | 开发期代理目标 | `http://localhost:8080` |
| `VITE_APP_ENV` | 当前环境 | `development` / `production` |

## 设计系统

### 用户端（portal / sample / auth）
- **主色**：`#0D9488` (teal-600)
- **辅助**：`#14B8A6`
- **CTA**：`#F97316` (orange-500)
- **背景**：`#F0FDFA` (teal-50)
- **文字**：`#134E4A` (teal-900)
- **字体**：`Baloo 2` / `Comic Neue` + 中文系统字体回退

### 管理端（admin）
- **主色**：`#0F172A` (slate-900)
- **CTA**：`#22C55E` (green-500)
- **背景**：`#020617` (slate-950)
- **文字**：`#F8FAFC` (slate-50)
- **字体**：`Fira Code` / `Fira Sans` + 中文系统字体回退

### 间距 / 圆角 / 阴影
- 间距阶梯：`4 / 8 / 16 / 24 / 32 / 48 / 64` px
- 圆角阶梯：`4 / 6 / 8 / 12 / 16 / 24` px
- 阴影：6 档 (`xs` ~ `2xl`)

## 核心特性

### 1. 手写板 HandwritingPad
- 基于 HTML5 SVG + `perfect-freehand` 实现笔锋还原
- Pointer Events 同时支持鼠标、触屏、压感笔
- 实时记录 `pressure`，输出 PNG/SVG/Blob
- 支持撤销/重做、清空、笔画粗细与颜色调整
- 暴露 `toDataURL` / `toBlob` / `toSVGString` / `clear` 等方法

### 2. 统一请求封装
- `src/utils/request.ts` 单一 Axios 实例
- 自动注入 `Authorization: Bearer <token>`
- 业务异常码集中处理：`1001 未登录` / `1003 无权限` / `2001 业务异常` / `5000 服务异常`
- `onUploadProgress` 上传进度回调

### 3. RBAC 权限
- 三种角色：`USER` / `AUDITOR` / `ADMIN`
- 路由元 `meta.roles` 限制；指令 `v-permission="['ADMIN']"`
- 后端为最终权威，前端仅做 UI 显隐

### 4. Pinia 状态
- `user`：token / profile / roles / permissions
- `sample`：列表 / 上传进度
- `audit`：待审核 / 批量操作
- `dict`：字符字典
- `app`：主题 / 侧边栏 / 语言

### 5. ECharts 看板
- 按需引入 `echarts/core`，按需注册 chart 与 component
- 折线 / 饼图 / 柱状图示例
- `vue-echarts` 组件化 + autoresize

### 6. 主题切换
- 通过 `data-theme` 在 `light` / `dark` 之间切换
- SCSS 变量驱动颜色 token，Element Plus 主题同步切换

## 附录

### 与后端联调

1. 启动后端（默认 `:8080`）
2. 修改 `.env.development` 中的 `VITE_PROXY_TARGET`
3. 设置 `VITE_USE_MOCK=false`
4. 重新 `pnpm dev`

### 目录规范

- 组件：`PascalCase.vue`，`<script setup lang="ts">`
- Composables：`useXxx`
- Stores：`useXxxStore`
- 视图：`XxxView.vue`

### 浏览器兼容

- Chrome / Edge / Firefox / Safari 最近 2 个大版本
- 移动端：iOS Safari 15+，Android Chrome 100+
- 屏幕：≥ 360px

---

完整需求详见 [`web.md`](./web.md)。

# 前端实现文档（web.md）

> 适用项目：基于 Java 的手写体收集和管理系统
> 文档版本：v1.0
> 编制日期：2026-06-15
> 对应模块：Web 前端（管理后台 + 用户端）

---

## 一、文档概述

本文档为系统前端工程化实现提供完整路径，覆盖技术选型理由、架构设计、模块划分、接口规范、开发流程、分阶段任务、里程碑节点、质量验收与部署策略。前端作为系统入口，负责手写体样本采集、用户交互、数据可视化及与后端 API 的对接。

### 1.1 设计目标
- 现代化布局与交互体验，适配 PC、平板、手机三端。
- 与后端（Spring Boot + MySQL + 对象存储）解耦，通过 RESTful API 通信。
- 可维护、可扩展、易测试，符合企业级前端规范。
- 满足「手写体样本在线采集、上传、管理、检索」的核心业务。

### 1.2 适用范围
- 样本采集端（用户登录/注册/书写上传）。
- 管理后台（用户管理、样本审核、数据统计）。
- 公共门户（首页、关于、公告）。

---

## 二、技术选型

### 2.1 选型总览

| 类别 | 选型 | 版本建议 | 选型理由 |
| --- | --- | --- | --- |
| 开发语言 | TypeScript | 5.x | 类型系统降低联调成本，配合 Vue3 提升可维护性 |
| 前端框架 | Vue 3 | 3.4+ | 需求明确指定；Composition API 更适合复杂业务拆分 |
| 构建工具 | Vite | 5.x | 启动/HMR 极快，原生支持 ESM、TS、Vue3 插件 |
| 路由 | Vue Router | 4.x | 官方方案，支持动态路由、路由守卫、懒加载 |
| 状态管理 | Pinia | 2.x | Vue3 官方推荐，TS 友好，替代 Vuex |
| UI 组件库 | Element Plus | 2.x | 中后台场景成熟，文档齐全，社区活跃 |
| 样式方案 | SCSS + CSS 变量 | - | 模块化样式，便于主题切换 |
| HTTP 客户端 | Axios | 1.x | 拦截器机制完善，统一处理 Token/异常/重试 |
| 手写板 | perfect-freehand / signature_pad | - | 满足 Canvas 手写轨迹采集与回放 |
| 图表 | ECharts | 5.x | 数据统计模块可视化（样本量、用户量等） |
| 工具库 | VueUse / dayjs / lodash-es | - | 提升编码效率，按需引入避免包体膨胀 |
| 代码规范 | ESLint + Prettier + Stylelint | - | 统一代码风格，配合 husky + lint-staged 卡点 |
| 提交规范 | Commitlint + cz-git | - | 规范化 commit 信息，生成 CHANGELOG |
| 单元测试 | Vitest + Vue Test Utils | - | 与 Vite 同生态，启动快 |
| E2E 测试 | Playwright / Cypress | - | 关键流程回归（登录、上传、审核） |
| 鉴权 | JWT（前端仅持有） | - | 与后端无状态认证方案保持一致 |
| 国际化 | vue-i18n | 9.x | 预留多语言能力 |
| 部署 | Nginx（静态托管） | 1.24+ | 与后端 Spring Boot 部署解耦 |

### 2.2 关键选型理由详述
- **Vue 3 + Vite**：需求明确要求 Vue3；Vite 解决 Vue CLI 在大型项目中冷启动慢的问题，且对 TS / Vue3 / JSX 支持更好。
- **Pinia 替代 Vuex**：去 mutation，TS 推断友好，store 可按域拆分，契合「用户域 / 样本域 / 审核域」业务划分。
- **Element Plus**：中后台场景开箱即用，Table / Form / Upload / Dialog 等组件覆盖采集端与管理端。
- **perfect-freehand + signature_pad**：手写体采集核心组件，前者用于笔锋还原，后者用于签名板/触摸轨迹采集。
- **Axios 统一拦截器**：配合后端约定的统一返回结构（如 `code/msg/data`）做集中错误处理与 Token 续签。
- **ECharts**：管理后台统计大屏必备，支持 Canvas / SVG 双模式，性能稳定。
- **Playwright 优先**：相比 Cypress 在跨浏览器与多端适配上更稳，CI 中可并行执行。

---

## 三、架构设计

### 3.1 整体架构
采用「分层 + 模块化」结构：

```
src/
├── api/                # 接口层：按业务域聚合的 axios 请求
├── assets/             # 静态资源
├── components/         # 通用组件（业务无关）
│   ├── base/           # 基础原子组件（按钮/输入框封装）
│   └── business/       # 业务组件（手写板、上传卡片等）
├── composables/        # 组合式函数（hooks）
├── directives/         # 自定义指令（v-permission 等）
├── layouts/            # 布局（Default / Admin / Blank）
├── router/             # 路由配置 + 守卫
├── stores/             # Pinia stores（user/sample/audit）
├── styles/             # 全局样式、SCSS 变量、主题
├── utils/              # 工具方法（request/storage/format）
├── views/              # 页面级组件
│   ├── portal/         # 门户
│   ├── auth/           # 登录注册
│   ├── sample/         # 样本采集
│   └── admin/          # 管理后台
├── types/              # 全局 TS 类型声明
├── locales/            # i18n 文案
├── main.ts
└── App.vue
```

### 3.2 数据流
1. View 组件触发用户行为。
2. 通过 Pinia action 调用 `api/` 下对应方法。
3. Axios 拦截器注入 Token、统一处理错误/重试。
4. 后端返回 JSON，前端按统一结构解析并写入 store。
5. 组件通过 computed/getter 响应式渲染。

### 3.3 手写体采集核心流程
1. 用户进入采集页 → 加载字符提示图（来自后端 `dict` 接口）。
2. Canvas 初始化（适配 DPR，启用压感事件）。
3. 用户书写 → `pointermove` 实时生成轨迹 → `perfect-freehand` 渲染。
4. 完成时通过 `toDataURL` 或 `toBlob` 导出 PNG/SVG。
5. 调用 `sample/upload` 接口提交，元数据（字符 ID、设备信息、用户 ID）通过 FormData 同步上传。
6. 上传进度由 Axios `onUploadProgress` 驱动 UI。

### 3.4 权限与路由守卫
- 路由元信息声明 `meta.requiresAuth` / `meta.roles`。
- 全局 `beforeEach` 守卫：未登录跳转登录页；无权限返回 403。
- 后端 RBAC 角色（USER / AUDITOR / ADMIN）由后端返回，前端在 `user store` 缓存，仅用于 UI 显隐控制。

---

## 四、接口规范

### 4.1 通用约定
- BaseURL：`https://api.<domain>/v1`
- 鉴权：`Authorization: Bearer <JWT>`，由 Axios 拦截器自动注入。
- Content-Type：`application/json;charset=UTF-8`，文件上传为 `multipart/form-data`。
- 统一返回结构：

```json
{
  "code": 0,
  "msg": "ok",
  "data": { /* 业务数据 */ }
}
```

- 错误码：`0` 成功；非 0 失败，常见 `1001 未登录`、`1003 无权限`、`2001 业务异常`、`5000 服务异常`。
- 分页参数：`pageNum`（从 1 起）、`pageSize`（默认 10，上限 100）。
- 时间格式：`yyyy-MM-dd HH:mm:ss`，时区 `Asia/Shanghai`。

### 4.2 接口清单（核心）

| 模块 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 鉴权 | POST | /auth/login | 用户名+密码登录，返回 JWT |
| 鉴权 | POST | /auth/register | 用户注册 |
| 鉴权 | GET  | /auth/captcha | 获取图形验证码 |
| 用户 | GET  | /user/profile | 获取个人信息 |
| 用户 | PUT  | /user/profile | 修改个人信息 |
| 字典 | GET  | /dict/chars | 获取待采集字符集 |
| 样本 | POST | /sample/upload | 上传单个手写体样本（multipart） |
| 样本 | GET  | /sample/page | 分页查询我的样本 |
| 样本 | DELETE | /sample/{id} | 删除样本 |
| 审核 | GET  | /audit/pending | 待审核列表（审核员） |
| 审核 | POST | /audit/{id}/approve | 通过 |
| 审核 | POST | /audit/{id}/reject | 驳回 + 原因 |
| 统计 | GET  | /stats/overview | 总览数据 |
| 文件 | GET  | /file/sign | 获取对象存储直传签名 |

### 4.3 接口 Mock
- 开发期使用 Vite 插件 `vite-plugin-mock` 按业务域生成 mock 数据。
- Mock 字段、状态码、延迟与后端联调时保持一致，便于切换。

### 4.4 TS 类型契约
- `src/api/contracts/` 下按业务域声明 `*.d.ts`，与后端字段一一对应。
- CI 阶段可通过 `openapi-typescript` 从后端 Swagger 自动生成并校对。

---

## 五、开发流程

### 5.1 环境准备
| 工具 | 版本 | 校验命令 |
| --- | --- | --- |
| Node.js | 20.x LTS | `node -v` |
| pnpm | 9.x | `pnpm -v` |
| Git | 2.40+ | `git --version` |

### 5.2 工程初始化步骤
1. `pnpm create vite handwriting-web --template vue-ts`。
2. 安装核心依赖：`vue-router pinia axios element-plus @vueuse/core dayjs echarts vue-i18n`。
3. 安装手写板：`perfect-freehand signature_pad`。
4. 安装开发依赖：`eslint prettier stylelint husky lint-staged @commitlint/cli cz-git vitest @vue/test-utils playwright`。
5. 配置 `vite.config.ts`（路径别名、代理、压缩、SCSS）。
6. 配置 `.eslintrc.cjs` / `.prettierrc` / `.stylelintrc.cjs`。
7. 接入 husky 钩子（pre-commit → lint-staged；commit-msg → commitlint）。

### 5.3 编码规范
- 组件名 PascalCase，文件名与组件名一致。
- Composable 以 `useXxx` 命名。
- 任何异步请求必须经过 `api/` 层，禁止在组件内直连 axios。
- 复杂表单拆分独立子组件，单文件不超过 300 行。
- 严禁使用 `any`；对后端未定义字段使用 `unknown` 并显式收窄。

### 5.4 Git 工作流
- 主分支：`main`（受保护，仅可合并）。
- 长期分支：`develop`。
- 短期分支：`feature/*`、`bugfix/*`、`release/*`。
- PR 要求：CI 全绿 + 至少 1 名 Code Reviewer 通过。

### 5.5 联调流程
1. 后端在测试环境部署并提供 Swagger 文档。
2. 前端通过 `vite proxy` 代理 `/v1` → 测试后端。
3. 字段不一致时在 `contracts/` 维护差异表，提 PR 至后端。
4. 联调通过后冻结接口，双方进入回归测试。

---

## 六、分阶段开发任务

### 阶段 M1：工程基线（第 1~2 周）
- 完成脚手架、代码规范、CI 流水线（lint + test + build）。
- 接入路由、布局、全局样式、Axios 封装、Pinia。
- 交付物：可在 `pnpm dev` 启动的空壳工程 + CI 截图。

### 阶段 M2：用户体系（第 3 周）
- 登录、注册、验证码、找回密码、JWT 持久化。
- 个人信息修改、头像上传（对接对象存储直传）。
- 单元测试覆盖率 ≥ 70%。

### 阶段 M3：手写体采集核心（第 4~5 周）
- 字符字典加载、Canvas 手写板组件。
- 笔锋还原、压感、清空/撤销/重做。
- 样本上传（支持 PNG/SVG）、上传进度、断点续传。
- 历史样本列表、详情预览、删除。

### 阶段 M4：管理后台（第 6~7 周）
- 用户管理、角色管理、权限点。
- 样本审核工作流（待审/通过/驳回/批量）。
- 数据统计大屏（ECharts）。
- 审计日志查看。

### 阶段 M5：质量与体验（第 8 周）
- 性能优化（路由懒加载、图片懒加载、CDN、按需引入）。
- 国际化（zh-CN 优先，预留 en-US）。
- 可访问性（键盘导航、ARIA）。
- E2E 关键路径用例。

### 阶段 M6：上线准备（第 9 周）
- 文档完善（README、CHANGELOG、组件库 Storybook）。
- 生产构建、镜像打包、灰度发布。
- 监控接入（PV/UV、JS 错误、性能指标）。

---

## 七、里程碑节点

| 节点 | 时间 | 验收产物 |
| --- | --- | --- |
| M1-Gate | 第 2 周末 | 基线工程 + CI 通过 |
| M2-Gate | 第 3 周末 | 用户体系可用 |
| M3-Gate | 第 5 周末 | 采集端全链路打通 |
| M4-Gate | 第 7 周末 | 管理后台闭环 |
| M5-Gate | 第 8 周末 | 性能/可访问性达标 |
| 正式发布 | 第 9 周末 | 灰度全量上线 |

---

## 八、质量验收标准

### 8.1 功能验收
- 所有 P0 需求用例通过（详见后端联调用例集）。
- 关键流程 E2E 100% 通过：登录、采集上传、审核、统计。

### 8.2 性能验收
- 首屏 LCP ≤ 2.5s（4G 网络）。
- 路由切换 TTFB ≤ 200ms（本地）。
- 主包 gzip ≤ 300KB；按需懒加载后首屏总资源 ≤ 1MB。

### 8.3 兼容性
- 浏览器：Chrome 100+、Edge 100+、Firefox 100+、Safari 15+。
- 移动端：iOS Safari 15+、Android Chrome 100+。
- 屏幕：≥ 360px 宽度适配；1280px / 1920px 桌面端布局。

### 8.4 代码质量
- ESLint / Prettier / Stylelint 0 error。
- 单元测试覆盖率：核心模块 ≥ 80%、整体 ≥ 60%。
- 任意 PR 阻断：未通过 lint、test、build 任意一步。

### 8.5 安全合规
- 严格 CSP、禁止 `eval`、第三方依赖白名单。
- 敏感字段（密码、手机号）前端脱敏展示。
- 严格按角色控制菜单/按钮显隐，后端二次校验。

---

## 九、部署策略

### 9.1 构建产物
- 命令：`pnpm build`，产物输出至 `dist/`。
- 资源指纹化（文件名含 hash），便于 CDN 缓存与版本回滚。
- 通过 `vite-plugin-compression` 生成 `.gz` / `.br`。

### 9.2 部署方式
| 环境 | 方式 | 备注 |
| --- | --- | --- |
| 开发 | `pnpm dev` + Vite proxy | 本地联调 |
| 测试 | Nginx 静态托管 + 自动化脚本 | 部署至内网测试机 |
| 预发 | Docker 镜像 + Nginx | 与生产同构 |
| 生产 | CDN + 对象存储 + Nginx 回源 | 静态资源走 CDN，HTML 走 Nginx |

### 9.3 Nginx 关键配置（摘录）
```nginx
server {
  listen 80;
  server_name web.example.com;
  root /usr/share/nginx/html;
  index index.html;

  location / {
    try_files $uri $uri/ /index.html;
  }

  location /v1/ {
    proxy_pass http://backend-upstream/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
  }

  gzip on;
  gzip_types text/plain text/css application/javascript application/json image/svg+xml;
  gzip_min_length 1k;
}
```

### 9.4 CI/CD
- GitHub Actions / GitLab CI 流水线：
  1. `pnpm install --frozen-lockfile`
  2. `pnpm lint`
  3. `pnpm test:unit`
  4. `pnpm build`
  5. 镜像构建并推送至 Harbor
  6. 通过 SSH/Ansible 滚动发布至 Nginx 服务器

### 9.5 灰度与回滚
- 按城市/渠道切量，前端可通过环境变量注入灰度标识。
- 保留最近 5 个版本构建产物，发现异常秒级回滚。

---

## 十、风险与应对

| 风险 | 影响 | 应对 |
| --- | --- | --- |
| 手写板兼容性问题 | 采集体验下降 | 多组件降级方案（fallback 静态图片标注） |
| 后端接口字段变动 | 联调阻塞 | 引入 OpenAPI 自动生成 TS 类型 + 字段差异表 |
| 浏览器缓存导致版本不生效 | 用户停留在旧版 | 强制 HTML 不缓存 + 文件 hash 命名 |
| 对象存储直传签名泄露 | 数据安全 | 短期签名 + Referer 限制 + 后端校验上传大小/类型 |

---

## 附录 A：推荐 VS Code 插件
- Volar（Vue3 + TS 必备）
- ESLint / Prettier / Stylelint
- GitLens
- i18n Ally

## 附录 B：参考命令速查
```bash
pnpm dev            # 启动开发
pnpm build          # 生产构建
pnpm preview        # 预览构建产物
pnpm lint           # 代码检查
pnpm test:unit      # 单元测试
pnpm test:e2e       # E2E 测试
```

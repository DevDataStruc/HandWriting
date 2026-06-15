# 后端实现文档（client.md）

> 适用项目：基于 Java 的手写体收集和管理系统
> 文档版本：v1.0
> 编制日期：2026-06-15
> 对应模块：服务端 API（client 端 API，对应前后端分离中的后端工程）

---

## 一、文档概述

本文档为系统后端工程化实现提供完整路径，涵盖技术选型、架构设计、模块划分、数据库设计、接口规范、开发流程、分阶段任务、里程碑节点、质量验收与部署策略。后端作为数据中枢，负责用户/角色/权限、手写体样本元数据、审核流、统计与文件签名分发等能力；文件二进制内容走对象存储（OSS/S3 兼容）。

### 1.1 设计目标
- 前后端彻底解耦，统一 RESTful + JSON 通信。
- 业务可水平扩展，数据一致性与性能兼顾。
- 与前端（Vue3 + Vite）形成契约式开发，可生成 OpenAPI 文档。
- 支持对象存储分桶（采集桶 / 审核桶 / 备份桶），便于分级管理。

### 1.2 适用范围
- 用户与权限服务。
- 样本采集与审核服务。
- 文件存储签名服务。
- 统计与日志服务。

---

## 二、技术选型

### 2.1 选型总览

| 类别 | 选型 | 版本建议 | 选型理由 |
| --- | --- | --- | --- |
| 开发语言 | Java | 17 LTS | 性能与稳定性兼顾，长期支持周期长 |
| 基础框架 | Spring Boot | 3.2.x | 需求指定；自动装配、生态成熟，匹配 Java 17 |
| 持久层 | Spring Data JPA + MyBatis-Plus | 3.x / 3.5.x | JPA 处理单表 CRUD，MP 应对复杂 SQL，分场景使用 |
| 数据库 | MySQL | 8.0+ | 需求指定；事务、索引、分区能力满足样本元数据规模 |
| 缓存 | Redis | 7.x | 会话/字典/限流/分布式锁，热点字典与计数器 |
| 消息队列 | RabbitMQ | 3.12+ | 异步任务（审核通知、统计聚合、缩略图生成） |
| 搜索引擎（可选） | Elasticsearch | 8.x | 样本标签/字符检索（如需全文检索可后续接入） |
| 对象存储 | MinIO / 阿里云 OSS | - | 需求要求存储桶；MinIO 自建可控，云端 OSS 适合生产 |
| 安全框架 | Spring Security + JJWT | 6.x / 0.12.x | RBAC + JWT 无状态鉴权 |
| 参数校验 | Hibernate Validator | - | 与 Spring Boot 无缝集成 |
| 接口文档 | springdoc-openapi | 2.x | 自动生成 OpenAPI 3 规范，与前端共享 |
| 日志 | Logback + Logstash + ELK | - | 结构化日志与集中检索 |
| 监控 | Spring Boot Actuator + Prometheus + Grafana | - | 应用与 JVM 指标可视化 |
| 链路追踪 | Micrometer Tracing + Zipkin | - | 多服务调用链追踪 |
| 工具库 | Lombok / MapStruct / Hutool | - | 减少模板代码 |
| 构建工具 | Maven | 3.9+ | 多模块管理规范，CI 友好 |
| 测试 | JUnit 5 / Mockito / Testcontainers | - | 单元/集成测试，支持 MySQL 容器化 |
| 部署 | Docker + Docker Compose / Kubernetes | - | 容器化部署，与前端/中间件统一编排 |
| JDK | Eclipse Temurin 17 | - | 开源 LTS，CI 友好 |

### 2.2 关键选型理由详述
- **Spring Boot 3 + Java 17**：响应需求；同时获得原生镜像、虚拟线程（Loom，预览）等新能力。
- **JPA + MyBatis-Plus 双栈**：JPA 写实体清晰、MP 写复杂 SQL 灵活；通过模块边界控制，避免混用。
- **MySQL 8.0**：原生 JSON 类型、窗口函数、CTE，统计查询表达力强；按 `user_id` / `char_id` 分区。
- **Redis 7**：多倍库隔离业务（用户、字典、限流、分布式锁）。
- **RabbitMQ**：相比 Kafka 更轻量，适合中等吞吐的异步任务；保证审核通知与缩略图生成最终一致。
- **MinIO / OSS 存储桶**：需求明确「使用存储桶解决」；MinIO 兼容 S3 协议，迁移云端零改造。
- **JWT 无状态**：便于水平扩展，避免 Session 共享；Refresh Token + 黑名单双轨制。
- **springdoc-openapi**：生成的 JSON 可直接被前端 `openapi-typescript` 消费，建立契约。
- **Testcontainers**：集成测试拉起真实 MySQL/Redis，避免 H2 兼容陷阱。

---

## 三、架构设计

### 3.1 分层架构
```
com.example.handwriting
├── api/                # Controller 层：参数接收、返回包装
├── application/        # Service 层：业务用例编排
├── domain/             # 领域模型（entity / value object / domain service）
├── infrastructure/     # 基础设施（持久化、缓存、对象存储、MQ、第三方）
├── config/             # 配置类（Security / OpenAPI / Web / Redis / MQ）
├── common/             # 通用工具（异常、响应包装、常量）
└── ClientApplication   # 启动类
```

依赖方向：`api → application → domain ← infrastructure`。

### 3.2 多模块拆分（Maven）
```
handwriting-platform
├── handwriting-client      # 对外 API（当前文档主体）
├── handwriting-common      # 公共工具与通用实体
├── handwriting-domain      # 领域模型
├── handwriting-infra       # 基础设施实现
└── handwriting-scheduler   # 定时任务（统计/清理/通知）
```

### 3.3 关键流程

#### 3.3.1 手写体上传
1. 客户端请求 `/file/sign` 获取 OSS/MinIO 直传签名（PUT 方式）。
2. 客户端直传文件至对象存储桶，路径规则：`sample/{yyyy}/{mm}/{userId}/{uuid}.png`。
3. 上传完成后调用 `/sample/upload`，提交元数据（fileKey、charId、size、device、sha256）。
4. 服务端校验签名有效性、文件元信息（HeadObject），落库并发送 MQ 消息触发缩略图生成与统计计数。
5. 返回样本 ID 与预览 URL。

#### 3.3.2 审核流程
1. 审核员拉取待审列表 `/audit/pending`。
2. 提交 `/audit/{id}/approve` 或 `/reject`。
3. 服务端更新样本状态、写审计日志、异步通知提交用户（站内信 / 邮件）。

#### 3.3.3 统计聚合
- 实时计数：Redis `INCR sample:count:{yyyyMMdd}`。
- 离线聚合：每日 00:30 调度任务将 Redis 计数落入 MySQL `stats_daily` 表。

### 3.4 安全架构
- JWT：Access（15min）+ Refresh（7d），Refresh 存 Redis 支持主动吊销。
- 权限模型：RBAC（USER / AUDITOR / ADMIN）+ 细粒度权限点（`sample:upload`、`audit:approve` 等）。
- 接口防刷：基于 Redis 令牌桶限流（`/auth/*` 与 `/sample/upload` 重点防护）。
- 文件安全：服务端校验 MIME、扩展名、大小；OSS 桶策略设置 Referer 白名单与最短签名有效期。
- 审计日志：所有写操作记录 `audit_log`（操作者/IP/时间/入参摘要）。

---

## 四、数据库设计

### 4.1 命名与规范
- 表名小写蛇形，以 `t_` 开头（可选，团队约定）。
- 主键统一 `id BIGINT AUTO_INCREMENT`。
- 必备字段：`create_time`、`update_time`、`create_by`、`update_by`、`deleted`（逻辑删除）。
- 字符集：`utf8mb4`，排序规则：`utf8mb4_0900_ai_ci`。

### 4.2 核心表（示例）

#### t_user
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 主键 |
| username | VARCHAR(64) UNIQUE | 用户名 |
| password | VARCHAR(128) | BCrypt 加密 |
| nickname | VARCHAR(64) | 昵称 |
| email | VARCHAR(128) | 邮箱 |
| phone | VARCHAR(20) | 手机号 |
| avatar | VARCHAR(255) | 头像 URL |
| status | TINYINT | 0 正常 / 1 禁用 |
| last_login_time | DATETIME | 最近登录时间 |
| create_time / update_time | DATETIME | - |

#### t_role
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | |
| code | VARCHAR(32) UNIQUE | 角色编码（USER/AUDITOR/ADMIN） |
| name | VARCHAR(64) | 角色名 |

#### t_user_role
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| user_id | BIGINT | |
| role_id | BIGINT | 联合主键 |

#### t_char_dict（字符字典）
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | |
| char_value | VARCHAR(8) | 字符（如「永」） |
| category | VARCHAR(32) | 分类（汉字/数字/字母/符号） |
| difficulty | TINYINT | 难度 1-5 |
| enabled | TINYINT | 是否启用 |

#### t_sample（手写体样本）
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | |
| user_id | BIGINT | 所属用户 |
| char_id | BIGINT | 字符 ID |
| file_key | VARCHAR(255) | 对象存储 Key |
| file_url | VARCHAR(512) | 访问 URL |
| file_size | INT | 字节 |
| sha256 | CHAR(64) | 文件哈希 |
| device | VARCHAR(64) | 设备信息 |
| status | TINYINT | 0 待审 / 1 通过 / 2 驳回 |
| reject_reason | VARCHAR(255) | 驳回原因 |
| audited_by | BIGINT | 审核人 |
| audited_time | DATETIME | 审核时间 |
| create_time | DATETIME | 上传时间 |

索引：`idx_user_status(user_id, status)`、`idx_status_time(status, create_time)`。

#### t_audit_log
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | |
| user_id | BIGINT | 操作人 |
| action | VARCHAR(64) | 动作 |
| target_type | VARCHAR(32) | 对象类型 |
| target_id | BIGINT | 对象 ID |
| ip | VARCHAR(45) | IP |
| payload | JSON | 入参摘要 |
| create_time | DATETIME | |

#### t_stats_daily
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| stat_date | DATE PK | 日期 |
| new_users | INT | 新增用户 |
| sample_total | INT | 累计样本 |
| sample_approved | INT | 累计通过 |

### 4.3 分区与归档
- `t_sample` 按 `create_time` 月分区，超过 12 个月的数据转冷归档（OSS 归档桶 + MySQL 压缩表）。
- `t_audit_log` 按月分区，按需清理。

---

## 五、接口规范

### 5.1 通用约定
- BaseURL：`/v1`，前缀化便于版本切换。
- 鉴权：`Authorization: Bearer <jwt>`，网关或过滤器统一解析并注入 `SecurityContext`。
- 请求/响应：`Content-Type: application/json;charset=UTF-8`；文件上传为 `multipart/form-data`。
- 统一响应结构：

```json
{
  "code": 0,
  "msg": "ok",
  "data": { /* 业务数据 */ }
}
```

- 业务异常通过 `@RestControllerAdvice` 统一捕获，转换为统一响应。

### 5.2 错误码体系
| 范围 | 含义 |
| --- | --- |
| 0 | 成功 |
| 1xxx | 通用客户端错误（参数、未登录、无权限） |
| 2xxx | 业务异常（样本不存在、状态非法等） |
| 5xxx | 服务端异常（数据库、第三方、未知） |

### 5.3 接口清单（与前端契约一致）

| 模块 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 鉴权 | POST | /v1/auth/login | 登录返回 access/refresh |
| 鉴权 | POST | /v1/auth/refresh | 刷新 access |
| 鉴权 | POST | /v1/auth/logout | 注销并吊销 refresh |
| 用户 | GET | /v1/user/profile | 个人信息 |
| 用户 | PUT | /v1/user/profile | 修改 |
| 用户 | POST | /v1/user/avatar | 上传头像（直传 OSS 后回调） |
| 字典 | GET | /v1/dict/chars?category=&difficulty= | 字符字典分页 |
| 文件 | GET | /v1/file/sign | 获取直传签名 |
| 文件 | POST | /v1/file/callback | 客户端上传完成回调（可选） |
| 样本 | POST | /v1/sample/upload | 提交样本元数据 |
| 样本 | GET | /v1/sample/page | 分页查询（支持我的/全部） |
| 样本 | GET | /v1/sample/{id} | 详情 |
| 样本 | DELETE | /v1/sample/{id} | 删除（仅本人未审/驳回） |
| 审核 | GET | /v1/audit/pending | 待审 |
| 审核 | POST | /v1/audit/{id}/approve | 通过 |
| 审核 | POST | /v1/audit/{id}/reject | 驳回 |
| 审核 | GET | /v1/audit/history | 历史审核 |
| 统计 | GET | /v1/stats/overview | 总览 |
| 统计 | GET | /v1/stats/trend?days=30 | 趋势 |
| 管理 | GET/POST/PUT/DELETE | /v1/admin/users / roles / permissions | 管理后台 CRUD |

### 5.4 OpenAPI 与契约
- `springdoc-openapi` 自动暴露 `/v3/api-docs`、`/swagger-ui.html`。
- CI 阶段导出 `openapi.json` 至 artifacts，前端流水线拉取并生成 TS 类型。
- 接口变更必须先 PR 至接口仓库，双方确认后再开发。

### 5.5 幂等与防重
- 写接口支持 `Idempotency-Key` 头，服务端以 Redis 去重，TTL 24h。
- 样本上传使用 `sha256` 作为业务幂等键。

---

## 六、开发流程

### 6.1 环境准备
| 工具 | 版本 | 校验命令 |
| --- | --- | --- |
| JDK | 17 | `java -version` |
| Maven | 3.9+ | `mvn -v` |
| Docker | 24+ | `docker -v` |
| MySQL | 8.0+ | `mysql --version` |
| Redis | 7+ | `redis-cli -v` |

### 6.2 工程初始化
1. 通过 Spring Initializr 生成多模块骨架，纳入 `handwriting-platform` 父 POM。
2. 统一父 POM 依赖版本、编译插件、Checkstyle/Spotless。
3. 引入 MySQL/Redis/MinIO/RabbitMQ Docker Compose，本地一键启动。
4. 配置 `application-{dev,test,prod}.yml`，敏感信息通过环境变量注入。

### 6.3 编码规范
- 遵循 Alibaba Java Coding Guidelines，结合 Checkstyle 校验。
- 禁止 `SELECT *`、禁止循环内调用远程接口。
- 业务异常统一抛出 `BizException`，由全局处理类转统一响应。
- Service 命名以业务动作结尾：`SampleUploadService#submit`。
- 严格分层，禁止 Controller 直接调用 Mapper。

### 6.4 Git 工作流
- 与前端一致：`main` / `develop` / `feature/*`。
- 提交信息遵循 Conventional Commits（feat/fix/refactor/docs/test/chore）。
- PR 必须关联需求/缺陷号，并附接口变更说明。

### 6.5 联调与契约测试
- 使用 Pact 或 Spring Cloud Contract 编写生产者契约测试，前端 Mock 工具消费。
- 关键接口（上传、审核、登录）必须有契约测试覆盖。

---

## 七、分阶段开发任务

### 阶段 S1：基础设施（第 1~2 周）
- 父 POM、模块骨架、Docker Compose 编排。
- 统一返回、全局异常、参数校验、OpenAPI、Security 基础配置。
- 集成 MySQL/Redis/RabbitMQ 健康检查。
- 交付物：`/actuator/health` 全绿 + Swagger 可访问。

### 阶段 S2：用户与权限（第 3 周）
- 注册/登录/刷新/注销、图形验证码、密码加密策略。
- RBAC 数据初始化与注解式权限控制。
- 审计日志拦截器。

### 阶段 S3：字典与文件服务（第 4 周）
- 字符字典 CRUD、批量导入。
- MinIO/OSS 接入、直传签名、回调、文件元信息校验。

### 阶段 S4：样本与上传（第 5~6 周）
- 样本提交、分页、详情、删除。
- 限流、幂等、防刷。
- MQ 触发缩略图生成、统计计数。

### 阶段 S5：审核工作流（第 7 周）
- 审核状态机、批量操作、通知 MQ 消费者。
- 审核时效统计。

### 阶段 S6：管理与统计（第 8 周）
- 管理后台 CRUD（用户/角色/权限）。
- 统计总览与趋势、定时聚合任务、导出 CSV。

### 阶段 S7：质量与上线（第 9 周）
- 全链路压测、慢 SQL 优化、缓存预热。
- 监控告警、灰度方案、灾备演练。
- 文档完善（README、API 文档、运维手册）。

---

## 八、里程碑节点

| 节点 | 时间 | 验收产物 |
| --- | --- | --- |
| S1-Gate | 第 2 周末 | 基线可启动 + 健康检查通过 |
| S2-Gate | 第 3 周末 | 用户体系闭环 |
| S3-Gate | 第 4 周末 | 字典与直传可用 |
| S4-Gate | 第 6 周末 | 样本采集全链路打通 |
| S5-Gate | 第 7 周末 | 审核流闭环 |
| S6-Gate | 第 8 周末 | 管理与统计可用 |
| 正式发布 | 第 9 周末 | 灰度全量上线 |

---

## 九、质量验收标准

### 9.1 功能验收
- 所有 P0 用例通过，契约测试 100% 通过。
- 关键接口通过 Postman/JMeter 自动化用例回归。

### 9.2 性能验收（生产环境单实例基线）
| 指标 | 目标 |
| --- | --- |
| 登录 P99 | ≤ 200ms |
| 样本上传（不含直传） P99 | ≤ 300ms |
| 审核分页 P99 | ≤ 200ms |
| 统计总览 P99 | ≤ 400ms |
| 写入 TPS | ≥ 500（样本提交） |
| 读 TPS | ≥ 2000（详情/分页） |

### 9.3 可用性与稳定性
- 单元测试覆盖率：核心模块 ≥ 80%、整体 ≥ 60%。
- 故障注入演练：Redis 挂掉时降级到 DB 缓存；MQ 不可用时本地任务表补偿。
- 灾备：MySQL 主从 + 每日全量备份；对象存储跨区复制。

### 9.4 安全合规
- OWASP Top 10 防护：SQL 注入（参数化）、XSS（Jackson 转义）、CSRF（前后端纯 API + SameSite Cookie / 头部 Token）。
- 密码 BCrypt cost ≥ 10；JWT 强签名（HS256+ 256bit secret 或 RS256）。
- 依赖漏洞扫描：`mvn dependency-check:check` 通过高危零容忍。

### 9.5 可观测性
- 日志：JSON 结构化，traceId 贯穿；关键错误 5xx 实时告警。
- 指标：JVM、Tomcat、HikariCP、Redis、RabbitMQ 自定义埋点。
- 链路：跨服务调用 100% 接入追踪。

---

## 十、部署策略

### 10.1 镜像构建
- 多阶段 Dockerfile：
  1. `eclipse-temurin:17-jdk` 构建（运行 `mvn -B -DskipTests package`）。
  2. `eclipse-temurin:17-jre` 运行时，仅拷贝 jar 与启动脚本。
- 镜像标签：`<git-commit>-<yyyyMMddHHmm>`。

### 10.2 编排模板（节选）
```yaml
version: '3.9'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: handwriting
    volumes:
      - mysql-data:/var/lib/mysql
    ports: ["3306:3306"]
  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
  rabbitmq:
    image: rabbitmq:3.12-management
    ports: ["5672:5672","15672:15672"]
  minio:
    image: minio/minio
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minio
      MINIO_ROOT_PASSWORD: minio123
    ports: ["9000:9000","9001:9001"]
  client:
    build: ./handwriting-client
    depends_on: [mysql, redis, rabbitmq, minio]
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_URL: jdbc:mysql://mysql:3306/handwriting?useSSL=false&serverTimezone=Asia/Shanghai
      REDIS_HOST: redis
      MQ_HOST: rabbitmq
      OSS_ENDPOINT: http://minio:9000
    ports: ["8080:8080"]
```

### 10.3 部署方式
| 环境 | 方式 | 备注 |
| --- | --- | --- |
| 开发 | Docker Compose | 本地一键起 |
| 测试 | Docker Compose + GitLab CI | 部署至内网 |
| 预发 | Kubernetes | 与生产同构 |
| 生产 | Kubernetes + HPA | 滚动发布 + 自动伸缩 |

### 10.4 发布策略
- 蓝绿发布：新旧版本并行，逐步切量；异常秒级回滚。
- 灰度维度：按租户/地区/抽样比例。
- 数据库变更：Flyway 版本化迁移，迁移与回滚脚本均需在 PR 中评审。

### 10.5 配置与密钥
- 配置中心：Nacos / Apollo（按团队既有能力选型）。
- 密钥：K8s Secret / Vault 注入，绝不入库。
- 配置文件拆分：`application.yml`（公共）+ `application-{env}.yml`（环境）。

### 10.6 备份与恢复
- MySQL：每日全量 + 实时 binlog，恢复点目标 RPO ≤ 5min。
- 对象存储：跨区复制 + 版本开启，RTO ≤ 30min。

---

## 十一、风险与应对

| 风险 | 影响 | 应对 |
| --- | --- | --- |
| MySQL 大表查询慢 | 审核列表超时 | 分区 + 联合索引 + 读写分离 |
| 对象存储不可用 | 上传失败 | 多端点重试 + 临时本地缓存降级 |
| JWT 泄露 | 越权 | 短有效期 + Refresh 吊销 + 异常设备告警 |
| MQ 积压 | 通知延迟 | 消费者多实例 + 死信队列 + 监控告警 |
| 接口频繁变动 | 前端联调阻塞 | OpenAPI 自动生成 + 契约测试 + 版本前缀 |

---

## 附录 A：Maven 关键依赖（节选）
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
  <groupId>com.baomidou</groupId>
  <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
  <version>3.5.5</version>
</dependency>
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-api</artifactId>
  <version>0.12.5</version>
</dependency>
<dependency>
  <groupId>org.springdoc</groupId>
  <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
  <version>2.5.0</version>
</dependency>
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>mysql</artifactId>
  <scope>test</scope>
</dependency>
```

## 附录 B：常用命令
```bash
mvn -B -DskipTests package          # 打包
mvn spring-boot:run                  # 本地启动
mvn -B verify                        # 跑测试
docker compose -f deploy/docker-compose.yml up -d   # 本地起依赖
kubectl apply -f deploy/k8s/         # K8s 部署
```

## 附录 C：与前端协作清单
- 接口域名：测试 `https://api-test.<domain>/v1`，生产 `https://api.<domain>/v1`。
- 鉴权：Access Token 通过请求头传递；前端在 401 且非登录接口时调用 `/auth/refresh`。
- CORS：允许来源限定为前端域名，方法限定 GET/POST/PUT/DELETE/OPTIONS。
- 文件直传：服务端返回的 `uploadUrl` 有效期 ≤ 600s，方法 PUT。

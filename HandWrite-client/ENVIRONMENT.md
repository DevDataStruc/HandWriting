# 环境变量说明（ENVIRONMENT.md）

本文档列出 `HandWrite-client` 工程中所有通过 `${XXX:default}` 占位符读取的环境变量，按业务模块分组，并标注用途、是否敏感、推荐值与生成方式。

> 所有 `password / secret / key` 类变量均为**敏感信息**，生产环境必须通过密钥管理（K8s Secret / Vault / CI 变量）注入，**严禁**入库或写入镜像层。

---

## 1. 服务自身

| 变量 | 默认值 | 是否敏感 | 用途 | 备注 |
| --- | --- | --- | --- | --- |
| `SERVER_PORT` | `8080` | 否 | HTTP 监听端口 | 容器化部署时常用 `8080` |
| `SERVER_CONTEXT_PATH` | `/` | 否 | Servlet 上下文路径 | 默认为根 |
| `SERVER_TOMCAT_MAX_THREADS` | `200` | 否 | Tomcat 最大工作线程 | 与压测吞吐相关 |
| `SPRING_PROFILES_ACTIVE` | `dev` | 否 | Spring Profile | `dev` / `prod` |
| `APP_TIMEZONE` | `Asia/Shanghai` | 否 | 全局时区 | Jackson 与 JPA 共用 |
| `LOG_LEVEL_APP` | `DEBUG` | 否 | 应用包日志级别 | 生产建议 `INFO` |
| `LOG_FILE_PATH` | `logs/handwriting-client.log` | 否 | 日志文件路径 | 生产建议挂载到持久卷 |

## 2. MySQL 数据源

| 变量 | 默认值 | 是否敏感 | 用途 | 备注 |
| --- | --- | --- | --- | --- |
| `DB_HOST` | `127.0.0.1` | 否 | 数据库主机 | |
| `DB_PORT` | `3306` | 否 | 数据库端口 | |
| `DB_NAME` | `handwriting` | 否 | 数据库名 | 需提前创建 |
| `DB_USERNAME` | `root` | **是** | 数据库账号 | 生产使用专用账号 |
| `DB_PASSWORD` | `root` | **是** | 数据库密码 | **必须** 替换 |
| `DB_USE_SSL` | `false` | 否 | 是否启用 SSL | 公网访问建议 `true` |
| `DB_TIMEZONE` | `Asia/Shanghai` | 否 | JDBC 时区 | |
| `DB_POOL_MAX` | `20` | 否 | HikariCP 最大连接数 | 视并发量调整 |
| `DB_POOL_MIN` | `5` | 否 | HikariCP 最小空闲连接 | |

## 3. JPA / Flyway

| 变量 | 默认值 | 是否敏感 | 用途 | 备注 |
| --- | --- | --- | --- | --- |
| `JPA_DDL_AUTO` | `validate` | 否 | Hibernate DDL 策略 | 生产严禁 `update`/`create` |
| `FLYWAY_ENABLED` | `true` | 否 | 是否启用 Flyway 迁移 | |

## 4. Redis

| 变量 | 默认值 | 是否敏感 | 用途 | 备注 |
| --- | --- | --- | --- | --- |
| `REDIS_HOST` | `127.0.0.1` | 否 | Redis 主机 | |
| `REDIS_PORT` | `6379` | 否 | Redis 端口 | |
| `REDIS_PASSWORD` | *(空)* | **是** | Redis 密码 | 无密码时留空 |
| `REDIS_DATABASE` | `0` | 否 | 库号 | 建议业务独享 |
| `REDIS_POOL_MAX_ACTIVE` | `16` | 否 | Lettuce 连接池上限 | |

## 5. 邮件

| 变量 | 默认值 | 是否敏感 | 用途 | 备注 |
| --- | --- | --- | --- | --- |
| `MAIL_HOST` | `smtp.example.com` | 否 | SMTP 主机 | |
| `MAIL_PORT` | `465` | 否 | SMTP 端口 | SSL 常用 465 |
| `MAIL_USERNAME` | *(空)* | **是** | SMTP 账号 | |
| `MAIL_PASSWORD` | *(空)* | **是** | SMTP 授权码 | |
| `MAIL_SSL_TRUST` | `smtp.example.com` | 否 | SSL 信任主机 | |

## 6. 文件上传（备用直传，本项目主推对象存储直传）

| 变量 | 默认值 | 是否敏感 | 用途 | 备注 |
| --- | --- | --- | --- | --- |
| `UPLOAD_MAX_FILE_SIZE` | `10MB` | 否 | 单文件大小上限 | |
| `UPLOAD_MAX_REQUEST_SIZE` | `20MB` | 否 | 单请求大小上限 | |

## 7. JWT

| 变量 | 默认值 | 是否敏感 | 用途 | 备注 |
| --- | --- | --- | --- | --- |
| `JWT_SECRET` | 内置默认值 | **是** | Base64 编码的 256 位 HMAC 密钥 | 生产**必须**替换；生成方式：`openssl rand -base64 32` |
| `JWT_ISSUER` | `handwriting-client` | 否 | Token 签发者 | |
| `JWT_ACCESS_TTL` | `900` | 否 | Access Token 有效期（秒） | 建议 900（15min） |
| `JWT_REFRESH_TTL` | `604800` | 否 | Refresh Token 有效期（秒） | 建议 604800（7d） |
| `JWT_HEADER` | `Authorization` | 否 | Token Header 名 | |
| `JWT_PREFIX` | `"Bearer "` | 否 | Token 前缀 | 注意带末尾空格 |

## 8. CORS

| 变量 | 默认值 | 是否敏感 | 用途 | 备注 |
| --- | --- | --- | --- | --- |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | 否 | 允许的跨域来源 | 多个以英文逗号分隔；通配使用 `*` |

## 9. 对象存储（MinIO / S3 兼容）

| 变量 | 默认值 | 是否敏感 | 用途 | 备注 |
| --- | --- | --- | --- | --- |
| `STORAGE_PROVIDER` | `minio` | 否 | 提供方标识 | `minio` / `s3` |
| `STORAGE_ENDPOINT` | `http://127.0.0.1:9000` | 否 | 服务端点 URL | |
| `STORAGE_ACCESS_KEY` | `minio` | **是** | 访问密钥 | |
| `STORAGE_SECRET_KEY` | `minio123` | **是** | 秘密密钥 | **必须** 替换 |
| `STORAGE_REGION` | `us-east-1` | 否 | 区域 | S3 必填；MinIO 可忽略 |
| `STORAGE_BUCKET_COLLECT` | `handwriting-collect` | 否 | 采集桶 | 需提前创建 |
| `STORAGE_BUCKET_AUDIT` | `handwriting-audit` | 否 | 审核桶 | |
| `STORAGE_BUCKET_BACKUP` | `handwriting-backup` | 否 | 备份桶 | |
| `STORAGE_PRESIGN_EXPIRE` | `600` | 否 | 直传签名有效期（秒） | 不建议超过 600 |
| `STORAGE_PUBLIC_BASE_URL` | *(空)* | 否 | 公开访问基础 URL | CDN / 代理场景 |

## 10. 限流

| 变量 | 默认值 | 是否敏感 | 用途 | 备注 |
| --- | --- | --- | --- | --- |
| `RATELIMIT_AUTH_PER_MIN` | `20` | 否 | 登录/注册类接口每分钟每 IP 限流 | |
| `RATELIMIT_UPLOAD_PER_MIN` | `60` | 否 | 上传类接口每分钟每用户限流 | |

---

## 注入方式

### 11.1 本地开发（IDE / Maven）

```bash
# 方式一：export 后启动
export $(grep -v '^#' .env | xargs) && mvn spring-boot:run

# 方式二：IDEA 启动配置中追加 Environment variables
```

### 11.2 Docker

```bash
docker run --rm -p 8080:8080 --env-file .env handwriting-client:1.0.0
```

### 11.3 Docker Compose

参见 `deploy/docker-compose.yml` 中通过 `environment:` 注入。

### 11.4 Kubernetes

```yaml
envFrom:
  - secretRef:
      name: handwriting-client-secret   # 通过 kubectl create secret 预创建
  - configMapRef:
      name: handwriting-client-config   # 非敏感参数
```

### 11.5 CI/CD

- GitHub Actions / GitLab CI：将敏感变量配置在 Secret 中，通过 `env:` 注入。
- 严禁把真实密钥写入 CI 脚本或日志。

---

## 必改清单（生产部署 Checklist）

- [ ] `DB_PASSWORD` 替换为强密码
- [ ] `JWT_SECRET` 重新生成：`openssl rand -base64 32`
- [ ] `STORAGE_ACCESS_KEY` / `STORAGE_SECRET_KEY` 替换
- [ ] `MAIL_USERNAME` / `MAIL_PASSWORD` 替换
- [ ] `CORS_ALLOWED_ORIGINS` 收敛为生产域名
- [ ] `JPA_DDL_AUTO=validate`（严禁 `update`）
- [ ] `SPRING_PROFILES_ACTIVE=prod`
- [ ] 关闭 `LOG_LEVEL_APP=INFO`（必要时 `WARN`）

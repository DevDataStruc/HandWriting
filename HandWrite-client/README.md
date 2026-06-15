# HandWrite-client 构建与运行指南

> 本文档面向开发、测试、运维三类角色，配套 `Design/client.md` 中的技术选型与接口规范。

## 一、目录结构

```
HandWrite-client/
├── pom.xml                                 # Maven 配置（Spring Boot 3.2 / Java 17）
├── Dockerfile                              # 多阶段构建
├── .env.example                            # 环境变量样例
├── ENVIRONMENT.md                          # 环境变量说明（必读）
├── README.md                               # 本文件
├── deploy/
│   └── docker-compose.yml                  # 一键起 MySQL/Redis/MinIO/Client
├── src/
│   ├── main/
│   │   ├── java/com/example/handwriting/
│   │   │   ├── HandwritingClientApplication.java
│   │   │   ├── common/        # 统一响应、异常、常量、工具
│   │   │   ├── config/        # Web/OpenAPI/Redis/MyBatis-Plus/Minio/AppProperties
│   │   │   ├── security/      # JWT、过滤器、Spring Security
│   │   │   ├── auth/          # 登录、注册、验证码、刷新、注销
│   │   │   ├── user/          # 用户档案
│   │   │   ├── dict/          # 字符字典
│   │   │   ├── file/          # 对象存储直传签名
│   │   │   ├── sample/        # 手写体样本
│   │   │   ├── audit/         # 审核
│   │   │   ├── stats/         # 统计
│   │   │   └── admin/         # 角色 / 用户角色关联
│   │   └── resources/
│   │       ├── application.yml           # 公共配置（全部走环境变量）
│   │       ├── application-dev.yml        # 开发环境 profile
│   │       ├── application-prod.yml       # 生产环境 profile
│   │       ├── logback-spring.xml         # 日志配置
│   │       └── db/migration/              # Flyway 迁移脚本
│   └── test/
│       └── java/.../HandwritingClientApplicationTests.java
```

## 二、构建前提

| 工具 | 最低版本 | 用途 |
| --- | --- | --- |
| JDK | 17 | 编译运行 |
| Maven | 3.9 | 构建（也可改用项目内 wrapper） |
| Docker / Docker Compose | 24+ | 一键起依赖（推荐） |
| MySQL | 8.0+ | 主库 |
| Redis | 7+ | 缓存 / 限流 / Refresh Token |
| MinIO | 最新 | 对象存储（S3 兼容），亦可替换为阿里云 OSS |

## 三、环境变量准备

1. 复制样例文件并按需修改：

```bash
cp .env.example .env
# 编辑 .env，至少修改以下项：
#   DB_PASSWORD / JWT_SECRET / STORAGE_ACCESS_KEY / STORAGE_SECRET_KEY
```

2. 关于 `JWT_SECRET`：

```bash
openssl rand -base64 32
# 把输出写入 .env 的 JWT_SECRET
```

3. 创建对象存储桶（MinIO）：

```bash
# 进入 MinIO 控制台 http://127.0.0.1:9001
# 使用 .env 中的 STORAGE_ACCESS_KEY / STORAGE_SECRET_KEY 登录
# 创建三个桶：handwriting-collect / handwriting-audit / handwriting-backup
```

> 也可使用 mc 客户端：`mc mb minio/handwriting-collect` 等。

## 四、本地构建

```bash
# 1. 编译打包（跳过测试）
mvn -B -DskipTests package

# 产物：target/handwriting-client.jar
```

## 五、本地运行

### 5.1 仅启动应用（依赖已就绪）

```bash
# 加载 .env 后启动（Linux/macOS）
set -a; source .env; set +a
mvn spring-boot:run

# 或直接运行已打好的 jar
java -jar target/handwriting-client.jar
```

### 5.2 一键起 MySQL / Redis / MinIO + 应用（推荐）

```bash
docker compose -f deploy/docker-compose.yml --env-file .env up -d

# 查看日志
docker compose -f deploy/docker-compose.yml logs -f client
```

### 5.3 Windows PowerShell

```powershell
# 加载 .env
Get-Content .env | ForEach-Object { if($_ -notmatch '^\s*#' -and $_ -match '^(.*?)=(.*)$') { [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process') } }

# 启动
mvn spring-boot:run
```

## 六、接口验证

启动后访问：

- 健康检查：`http://127.0.0.1:8080/actuator/health`
- Swagger UI：`http://127.0.0.1:8080/swagger-ui.html`
- OpenAPI JSON：`http://127.0.0.1:8080/v3/api-docs`

最小验证流程（curl）：

```bash
# 1. 获取验证码
curl -s http://127.0.0.1:8080/v1/auth/captcha

# 2. 注册（同时返回 access/refresh）
curl -s -X POST http://127.0.0.1:8080/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"Aa123456","nickname":"Alice","email":"a@example.com"}'

# 3. 登录
curl -s -X POST http://127.0.0.1:8080/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"Aa123456","captchaKey":"xxx","captchaCode":"abcd"}'

# 4. 携带 Token 访问受保护接口
curl -s http://127.0.0.1:8080/v1/user/profile \
  -H "Authorization: Bearer <accessToken>"
```

## 七、镜像构建与发布

```bash
# 构建镜像
docker build -t handwriting-client:1.0.0 .

# 通过环境变量启动
docker run --rm -p 8080:8080 --env-file .env handwriting-client:1.0.0

# 推送至 Harbor
docker tag handwriting-client:1.0.0 harbor.example.com/handwriting/handwriting-client:1.0.0
docker push harbor.example.com/handwriting/handwriting-client:1.0.0
```

## 八、K8s 部署示例

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: handwriting-client
spec:
  replicas: 2
  selector:
    matchLabels: { app: handwriting-client }
  template:
    metadata:
      labels: { app: handwriting-client }
    spec:
      containers:
        - name: app
          image: harbor.example.com/handwriting/handwriting-client:1.0.0
          ports: [{ containerPort: 8080 }]
          envFrom:
            - secretRef: { name: handwriting-client-secret }
            - configMapRef: { name: handwriting-client-config }
          readinessProbe:
            httpGet: { path: /actuator/health/readiness, port: 8080 }
          livenessProbe:
            httpGet: { path: /actuator/health/liveness, port: 8080 }
          resources:
            requests: { cpu: "500m", memory: "512Mi" }
            limits:   { cpu: "2000m", memory: "2Gi" }
---
apiVersion: v1
kind: Service
metadata: { name: handwriting-client }
spec:
  selector: { app: handwriting-client }
  ports: [{ port: 80, targetPort: 8080 }]
```

预创建 Secret（生产环境应使用 Vault / KMS 注入）：

```bash
kubectl create secret generic handwriting-client-secret \
  --from-literal=DB_PASSWORD='****' \
  --from-literal=JWT_SECRET='****' \
  --from-literal=STORAGE_ACCESS_KEY='****' \
  --from-literal=STORAGE_SECRET_KEY='****'
```

## 九、健康检查与监控

| 端点 | 用途 |
| --- | --- |
| `GET /actuator/health` | 总体健康（需鉴权才显示详情） |
| `GET /actuator/health/readiness` | 就绪探针（K8s） |
| `GET /actuator/health/liveness` | 存活探针（K8s） |
| `GET /actuator/prometheus` | Prometheus 抓取端点 |
| `GET /actuator/info` | 应用信息 |

建议接入 Prometheus + Grafana，配合告警规则（5xx 比例、JVM GC、线程池队列等）。

## 十、常见问题（FAQ）

1. **启动报 `Communications link failure`** → 检查 `DB_HOST/DB_PORT/DB_PASSWORD`，并确认 MySQL 已就绪。
2. **登录提示 `验证码错误`** → 图形验证码是大小写不敏感的，但仍需在 TTL（5 分钟）内使用。
3. **文件直传 403** → 确认 MinIO 桶已创建，且 `STORAGE_ACCESS_KEY/SECRET_KEY` 有写权限。
4. **Swagger 报 401** → 公共白名单已包含 `/v3/api-docs/**` 与 `/swagger-ui/**`，若自定义网关需放行。
5. **JWT 解析失败** → `JWT_SECRET` 变更后，旧 Token 全部失效，需要重新登录。
6. **Flyway 失败** → 检查 `flyway_schema_history` 表与 `V1/V2` 脚本是否被外部变更过。

## 十一、版本与变更

| 版本 | 日期 | 变更 |
| --- | --- | --- |
| 1.0.0 | 2026-06-15 | 初版：用户/字典/文件/样本/审核/统计 基础能力 |

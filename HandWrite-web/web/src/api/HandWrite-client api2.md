---
title: HandWrite-client api2
language_tabs:
  - shell: Shell
  - http: HTTP
  - javascript: JavaScript
  - ruby: Ruby
  - python: Python
  - php: PHP
  - java: Java
  - go: Go
toc_footers: []
includes: []
search: true
code_clipboard: true
highlight_theme: darkula
headingLevel: 2
generator: "@tarslib/widdershins v4.0.30"

---

# HandWrite-client api2

手写体收集和管理系统 - 客户端 API 文档

Base URLs:

Email: <a href="mailto:dev@example.com">handwriting-team</a> 
 License: Apache 2.0

# Authentication

- HTTP Authentication, scheme: bearer

# 审核

<a id="opIdreject"></a>

## POST 审核驳回

POST /v1/audit/{id}/reject

> Body 请求参数

```json
{
  "reason": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|integer(int64)| 是 |none|
|body|body|[AuditDecisionDTO](#schemaauditdecisiondto)| 否 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":{},"ts":0,"success":true}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[RVoid](#schemarvoid)|

<a id="opIdapprove"></a>

## POST 审核通过

POST /v1/audit/{id}/approve

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|integer(int64)| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":{},"ts":0,"success":true}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[RVoid](#schemarvoid)|

<a id="opIdpending"></a>

## GET 待审核列表

GET /v1/audit/pending

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|pageNum|query|integer(int64)| 否 |none|
|pageSize|query|integer(int64)| 否 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":{"total":0,"pageNum":0,"pageSize":0,"records":[{"id":0,"userId":0,"charId":0,"fileKey":"string","fileUrl":"string","fileSize":0,"device":"string","status":0,"rejectReason":"string","auditedBy":0,"auditedTime":"2019-08-24T14:15:22Z","createTime":"2019-08-24T14:15:22Z","char":"string"}]},"ts":0,"success":true}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[RPageResultSampleVO](#schemarpageresultsamplevo)|

<a id="opIdhistory"></a>

## GET 审核历史

GET /v1/audit/history

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|status|query|integer(int32)| 否 |none|
|pageNum|query|integer(int64)| 否 |none|
|pageSize|query|integer(int64)| 否 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":{"total":0,"pageNum":0,"pageSize":0,"records":[{"id":0,"userId":0,"charId":0,"fileKey":"string","fileUrl":"string","fileSize":0,"device":"string","status":0,"rejectReason":"string","auditedBy":0,"auditedTime":"2019-08-24T14:15:22Z","createTime":"2019-08-24T14:15:22Z","char":"string"}]},"ts":0,"success":true}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[RPageResultSampleVO](#schemarpageresultsamplevo)|

# 管理员-用户管理

<a id="opIdtoggleStatus"></a>

## PATCH 切换用户启用/禁用状态

PATCH /v1/admin/users/{id}/status

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|integer(int64)| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":{"id":0,"username":"string","nickname":"string","email":"string","phone":"string","avatar":"string","status":0,"statusExt":"string","lastLoginTime":"2019-08-24T14:15:22Z","createTime":"2019-08-24T14:15:22Z","createdAt":"string","roles":["string"],"permissions":["string"],"sampleCount":0},"ts":0,"success":true}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[RAdminUserVO](#schemaradminuservo)|

<a id="opIdlist_1"></a>

## GET 分页查询用户列表（关键字 / 状态）

GET /v1/admin/users

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|keyword|query|string| 否 |none|
|status|query|integer(int32)| 否 |none|
|pageNum|query|integer(int64)| 否 |none|
|pageSize|query|integer(int64)| 否 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":{"list":[{"id":0,"username":"string","nickname":"string","email":"string","phone":"string","avatar":"string","status":0,"statusExt":"string","lastLoginTime":"2019-08-24T14:15:22Z","createTime":"2019-08-24T14:15:22Z","createdAt":"string","roles":["string"],"permissions":["string"],"sampleCount":0}],"total":0},"ts":0,"success":true}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[RUserListResponse](#schemaruserlistresponse)|

# 控制台 Dashboard

<a id="opIdtrend"></a>

## GET 样本/用户趋势（最近 N 天）

GET /v1/stats/trend

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|days|query|integer(int32)| 否 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":{"dates":["string"],"samples":[0],"users":[0]},"ts":0,"success":true}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[RSampleTrendVO](#schemarsampletrendvo)|

<a id="opIdtopContributors"></a>

## GET 贡献者排行 TOP N（默认 10）

GET /v1/stats/top-contributors

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|limit|query|integer(int32)| 否 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":[{"userId":0,"username":"string","nickname":"string","avatar":"string","sampleCount":0,"approvedCount":0}],"ts":0,"success":true}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[RListTopContributorVO](#schemarlisttopcontributorvo)|

<a id="opIdstatusDistribution"></a>

## GET 样本状态分布（按 PENDING/APPROVED/REJECTED）

GET /v1/stats/status-distribution

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":[{"status":"string","count":0}],"ts":0,"success":true}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[RListStatusDistributionVO](#schemarliststatusdistributionvo)|

<a id="opIdoverview"></a>

## GET 总览数据

GET /v1/stats/overview

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":{"totalSamples":0,"totalUsers":0,"totalChars":0,"todaySamples":0,"todayUsers":0,"pendingAudits":0,"approvedSamples":0,"rejectedSamples":0,"growthRate":0.1},"ts":0,"success":true}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[RStatsOverviewVO](#schemarstatsoverviewvo)|

<a id="opIddictProgress"></a>

## GET 字符采集进度（按进度降序，便于 TOP 20 展示）

GET /v1/stats/dict-progress

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":[{"charId":0,"charValue":"string","collected":0,"target":0,"progress":0}],"ts":0,"success":true}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[RListDictProgressVO](#schemarlistdictprogressvo)|

# 数据模型

<h2 id="tocS_SampleVO">SampleVO</h2>

<a id="schemasamplevo"></a>
<a id="schema_SampleVO"></a>
<a id="tocSsamplevo"></a>
<a id="tocssamplevo"></a>

```json
{
  "id": 0,
  "userId": 0,
  "charId": 0,
  "fileKey": "string",
  "fileUrl": "string",
  "fileSize": 0,
  "device": "string",
  "status": 0,
  "rejectReason": "string",
  "auditedBy": 0,
  "auditedTime": "2019-08-24T14:15:22Z",
  "createTime": "2019-08-24T14:15:22Z",
  "char": "string"
}

```

样本视图

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer(int64)|false|none||none|
|userId|integer(int64)|false|none||none|
|charId|integer(int64)|false|none||none|
|fileKey|string|false|none||none|
|fileUrl|string|false|none||none|
|fileSize|integer(int32)|false|none||none|
|device|string|false|none||none|
|status|integer(int32)|false|none||none|
|rejectReason|string|false|none||none|
|auditedBy|integer(int64)|false|none||none|
|auditedTime|string(date-time)|false|none||none|
|createTime|string(date-time)|false|none||none|
|char|string|false|none||字符（汉字/数字/字母/符号）|

<h2 id="tocS_RVoid">RVoid</h2>

<a id="schemarvoid"></a>
<a id="schema_RVoid"></a>
<a id="tocSrvoid"></a>
<a id="tocsrvoid"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": {},
  "ts": 0,
  "success": true
}

```

统一 API 响应

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|false|none||状态码，0 表示成功|
|msg|string|false|none||提示信息|
|data|object|false|none||业务数据|
|ts|integer(int64)|false|none||服务器时间戳（毫秒）|
|success|boolean|false|none||none|

<h2 id="tocS_AuditDecisionDTO">AuditDecisionDTO</h2>

<a id="schemaauditdecisiondto"></a>
<a id="schema_AuditDecisionDTO"></a>
<a id="tocSauditdecisiondto"></a>
<a id="tocsauditdecisiondto"></a>

```json
{
  "reason": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|reason|string|false|none||none|

<h2 id="tocS_AdminUserVO">AdminUserVO</h2>

<a id="schemaadminuservo"></a>
<a id="schema_AdminUserVO"></a>
<a id="tocSadminuservo"></a>
<a id="tocsadminuservo"></a>

```json
{
  "id": 0,
  "username": "string",
  "nickname": "string",
  "email": "string",
  "phone": "string",
  "avatar": "string",
  "status": 0,
  "statusExt": "string",
  "lastLoginTime": "2019-08-24T14:15:22Z",
  "createTime": "2019-08-24T14:15:22Z",
  "createdAt": "string",
  "roles": [
    "string"
  ],
  "permissions": [
    "string"
  ],
  "sampleCount": 0
}

```

管理员用户档案

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer(int64)|false|none||用户 ID|
|username|string|false|none||用户名|
|nickname|string|false|none||昵称|
|email|string|false|none||邮箱|
|phone|string|false|none||手机号|
|avatar|string|false|none||头像 URL|
|status|integer(int32)|false|none||原始状态码（0 正常 / 1 禁用）|
|statusExt|string|false|none||扩展状态：active / disabled|
|lastLoginTime|string(date-time)|false|none||最后登录时间|
|createTime|string(date-time)|false|none||创建时间|
|createdAt|string|false|none||创建时间（别名）|
|roles|[string]|false|none||角色编码列表|
|permissions|[string]|false|none||权限点列表|
|sampleCount|integer(int64)|false|none||该用户提交的样本数|

<h2 id="tocS_RAdminUserVO">RAdminUserVO</h2>

<a id="schemaradminuservo"></a>
<a id="schema_RAdminUserVO"></a>
<a id="tocSradminuservo"></a>
<a id="tocsradminuservo"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "id": 0,
    "username": "string",
    "nickname": "string",
    "email": "string",
    "phone": "string",
    "avatar": "string",
    "status": 0,
    "statusExt": "string",
    "lastLoginTime": "2019-08-24T14:15:22Z",
    "createTime": "2019-08-24T14:15:22Z",
    "createdAt": "string",
    "roles": [
      "string"
    ],
    "permissions": [
      "string"
    ],
    "sampleCount": 0
  },
  "ts": 0,
  "success": true
}

```

统一 API 响应

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|false|none||状态码，0 表示成功|
|msg|string|false|none||提示信息|
|data|[AdminUserVO](#schemaadminuservo)|false|none||管理员用户档案|
|ts|integer(int64)|false|none||服务器时间戳（毫秒）|
|success|boolean|false|none||none|

<h2 id="tocS_RSampleTrendVO">RSampleTrendVO</h2>

<a id="schemarsampletrendvo"></a>
<a id="schema_RSampleTrendVO"></a>
<a id="tocSrsampletrendvo"></a>
<a id="tocsrsampletrendvo"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "dates": [
      "string"
    ],
    "samples": [
      0
    ],
    "users": [
      0
    ]
  },
  "ts": 0,
  "success": true
}

```

统一 API 响应

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|false|none||状态码，0 表示成功|
|msg|string|false|none||提示信息|
|data|[SampleTrendVO](#schemasampletrendvo)|false|none||样本趋势|
|ts|integer(int64)|false|none||服务器时间戳（毫秒）|
|success|boolean|false|none||none|

<h2 id="tocS_SampleTrendVO">SampleTrendVO</h2>

<a id="schemasampletrendvo"></a>
<a id="schema_SampleTrendVO"></a>
<a id="tocSsampletrendvo"></a>
<a id="tocssampletrendvo"></a>

```json
{
  "dates": [
    "string"
  ],
  "samples": [
    0
  ],
  "users": [
    0
  ]
}

```

样本趋势

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|dates|[string]|false|none||日期序列（yyyy-MM-dd）|
|samples|[integer]|false|none||每日新增样本数|
|users|[integer]|false|none||每日新增用户数|

<h2 id="tocS_RListTopContributorVO">RListTopContributorVO</h2>

<a id="schemarlisttopcontributorvo"></a>
<a id="schema_RListTopContributorVO"></a>
<a id="tocSrlisttopcontributorvo"></a>
<a id="tocsrlisttopcontributorvo"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": [
    {
      "userId": 0,
      "username": "string",
      "nickname": "string",
      "avatar": "string",
      "sampleCount": 0,
      "approvedCount": 0
    }
  ],
  "ts": 0,
  "success": true
}

```

统一 API 响应

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|false|none||状态码，0 表示成功|
|msg|string|false|none||提示信息|
|data|[[TopContributorVO](#schematopcontributorvo)]|false|none||业务数据|
|ts|integer(int64)|false|none||服务器时间戳（毫秒）|
|success|boolean|false|none||none|

<h2 id="tocS_TopContributorVO">TopContributorVO</h2>

<a id="schematopcontributorvo"></a>
<a id="schema_TopContributorVO"></a>
<a id="tocStopcontributorvo"></a>
<a id="tocstopcontributorvo"></a>

```json
{
  "userId": 0,
  "username": "string",
  "nickname": "string",
  "avatar": "string",
  "sampleCount": 0,
  "approvedCount": 0
}

```

贡献者排行

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|userId|integer(int64)|false|none||用户 ID|
|username|string|false|none||用户名|
|nickname|string|false|none||昵称|
|avatar|string|false|none||头像 URL|
|sampleCount|integer(int64)|false|none||提交样本总数|
|approvedCount|integer(int64)|false|none||已通过样本数|

<h2 id="tocS_RListStatusDistributionVO">RListStatusDistributionVO</h2>

<a id="schemarliststatusdistributionvo"></a>
<a id="schema_RListStatusDistributionVO"></a>
<a id="tocSrliststatusdistributionvo"></a>
<a id="tocsrliststatusdistributionvo"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": [
    {
      "status": "string",
      "count": 0
    }
  ],
  "ts": 0,
  "success": true
}

```

统一 API 响应

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|false|none||状态码，0 表示成功|
|msg|string|false|none||提示信息|
|data|[[StatusDistributionVO](#schemastatusdistributionvo)]|false|none||业务数据|
|ts|integer(int64)|false|none||服务器时间戳（毫秒）|
|success|boolean|false|none||none|

<h2 id="tocS_StatusDistributionVO">StatusDistributionVO</h2>

<a id="schemastatusdistributionvo"></a>
<a id="schema_StatusDistributionVO"></a>
<a id="tocSstatusdistributionvo"></a>
<a id="tocsstatusdistributionvo"></a>

```json
{
  "status": "string",
  "count": 0
}

```

样本状态分布

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|status|string|false|none||状态名|
|count|integer(int64)|false|none||数量|

<h2 id="tocS_RStatsOverviewVO">RStatsOverviewVO</h2>

<a id="schemarstatsoverviewvo"></a>
<a id="schema_RStatsOverviewVO"></a>
<a id="tocSrstatsoverviewvo"></a>
<a id="tocsrstatsoverviewvo"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "totalSamples": 0,
    "totalUsers": 0,
    "totalChars": 0,
    "todaySamples": 0,
    "todayUsers": 0,
    "pendingAudits": 0,
    "approvedSamples": 0,
    "rejectedSamples": 0,
    "growthRate": 0.1
  },
  "ts": 0,
  "success": true
}

```

统一 API 响应

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|false|none||状态码，0 表示成功|
|msg|string|false|none||提示信息|
|data|[StatsOverviewVO](#schemastatsoverviewvo)|false|none||统计总览|
|ts|integer(int64)|false|none||服务器时间戳（毫秒）|
|success|boolean|false|none||none|

<h2 id="tocS_StatsOverviewVO">StatsOverviewVO</h2>

<a id="schemastatsoverviewvo"></a>
<a id="schema_StatsOverviewVO"></a>
<a id="tocSstatsoverviewvo"></a>
<a id="tocsstatsoverviewvo"></a>

```json
{
  "totalSamples": 0,
  "totalUsers": 0,
  "totalChars": 0,
  "todaySamples": 0,
  "todayUsers": 0,
  "pendingAudits": 0,
  "approvedSamples": 0,
  "rejectedSamples": 0,
  "growthRate": 0.1
}

```

统计总览

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|totalSamples|integer(int64)|false|none||累计样本数|
|totalUsers|integer(int64)|false|none||注册用户数|
|totalChars|integer(int64)|false|none||字符字典总数（启用）|
|todaySamples|integer(int64)|false|none||今日新增样本数|
|todayUsers|integer(int64)|false|none||今日新增用户数|
|pendingAudits|integer(int64)|false|none||待审核样本数|
|approvedSamples|integer(int64)|false|none||已通过样本数|
|rejectedSamples|integer(int64)|false|none||已驳回样本数|
|growthRate|number(double)|false|none||样本增长率（较昨日，百分比）|

<h2 id="tocS_DictProgressVO">DictProgressVO</h2>

<a id="schemadictprogressvo"></a>
<a id="schema_DictProgressVO"></a>
<a id="tocSdictprogressvo"></a>
<a id="tocsdictprogressvo"></a>

```json
{
  "charId": 0,
  "charValue": "string",
  "collected": 0,
  "target": 0,
  "progress": 0
}

```

字符采集进度

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|charId|integer(int64)|false|none||字符 ID|
|charValue|string|false|none||字符（汉字/数字/字母/符号）|
|collected|integer(int64)|false|none||已通过采集数|
|target|integer(int64)|false|none||目标采集数（默认 100，可在配置中调整）|
|progress|integer(int64)|false|none||进度（百分比，0-100）|

<h2 id="tocS_RListDictProgressVO">RListDictProgressVO</h2>

<a id="schemarlistdictprogressvo"></a>
<a id="schema_RListDictProgressVO"></a>
<a id="tocSrlistdictprogressvo"></a>
<a id="tocsrlistdictprogressvo"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": [
    {
      "charId": 0,
      "charValue": "string",
      "collected": 0,
      "target": 0,
      "progress": 0
    }
  ],
  "ts": 0,
  "success": true
}

```

统一 API 响应

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|false|none||状态码，0 表示成功|
|msg|string|false|none||提示信息|
|data|[[DictProgressVO](#schemadictprogressvo)]|false|none||业务数据|
|ts|integer(int64)|false|none||服务器时间戳（毫秒）|
|success|boolean|false|none||none|

<h2 id="tocS_PageResultSampleVO">PageResultSampleVO</h2>

<a id="schemapageresultsamplevo"></a>
<a id="schema_PageResultSampleVO"></a>
<a id="tocSpageresultsamplevo"></a>
<a id="tocspageresultsamplevo"></a>

```json
{
  "total": 0,
  "pageNum": 0,
  "pageSize": 0,
  "records": [
    {
      "id": 0,
      "userId": 0,
      "charId": 0,
      "fileKey": "string",
      "fileUrl": "string",
      "fileSize": 0,
      "device": "string",
      "status": 0,
      "rejectReason": "string",
      "auditedBy": 0,
      "auditedTime": "2019-08-24T14:15:22Z",
      "createTime": "2019-08-24T14:15:22Z",
      "char": "string"
    }
  ]
}

```

分页结果

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|total|integer(int64)|false|none||总记录数|
|pageNum|integer(int64)|false|none||当前页码|
|pageSize|integer(int64)|false|none||每页大小|
|records|[[SampleVO](#schemasamplevo)]|false|none||数据列表|

<h2 id="tocS_RPageResultSampleVO">RPageResultSampleVO</h2>

<a id="schemarpageresultsamplevo"></a>
<a id="schema_RPageResultSampleVO"></a>
<a id="tocSrpageresultsamplevo"></a>
<a id="tocsrpageresultsamplevo"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "total": 0,
    "pageNum": 0,
    "pageSize": 0,
    "records": [
      {
        "id": 0,
        "userId": 0,
        "charId": 0,
        "fileKey": "string",
        "fileUrl": "string",
        "fileSize": 0,
        "device": "string",
        "status": 0,
        "rejectReason": "string",
        "auditedBy": 0,
        "auditedTime": "2019-08-24T14:15:22Z",
        "createTime": "2019-08-24T14:15:22Z",
        "char": "string"
      }
    ]
  },
  "ts": 0,
  "success": true
}

```

统一 API 响应

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|false|none||状态码，0 表示成功|
|msg|string|false|none||提示信息|
|data|[PageResultSampleVO](#schemapageresultsamplevo)|false|none||分页结果|
|ts|integer(int64)|false|none||服务器时间戳（毫秒）|
|success|boolean|false|none||none|

<h2 id="tocS_RUserListResponse">RUserListResponse</h2>

<a id="schemaruserlistresponse"></a>
<a id="schema_RUserListResponse"></a>
<a id="tocSruserlistresponse"></a>
<a id="tocsruserlistresponse"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "list": [
      {
        "id": 0,
        "username": "string",
        "nickname": "string",
        "email": "string",
        "phone": "string",
        "avatar": "string",
        "status": 0,
        "statusExt": "string",
        "lastLoginTime": "2019-08-24T14:15:22Z",
        "createTime": "2019-08-24T14:15:22Z",
        "createdAt": "string",
        "roles": [
          "string"
        ],
        "permissions": [
          "string"
        ],
        "sampleCount": 0
      }
    ],
    "total": 0
  },
  "ts": 0,
  "success": true
}

```

统一 API 响应

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|false|none||状态码，0 表示成功|
|msg|string|false|none||提示信息|
|data|[UserListResponse](#schemauserlistresponse)|false|none||管理员用户分页响应|
|ts|integer(int64)|false|none||服务器时间戳（毫秒）|
|success|boolean|false|none||none|

<h2 id="tocS_UserListResponse">UserListResponse</h2>

<a id="schemauserlistresponse"></a>
<a id="schema_UserListResponse"></a>
<a id="tocSuserlistresponse"></a>
<a id="tocsuserlistresponse"></a>

```json
{
  "list": [
    {
      "id": 0,
      "username": "string",
      "nickname": "string",
      "email": "string",
      "phone": "string",
      "avatar": "string",
      "status": 0,
      "statusExt": "string",
      "lastLoginTime": "2019-08-24T14:15:22Z",
      "createTime": "2019-08-24T14:15:22Z",
      "createdAt": "string",
      "roles": [
        "string"
      ],
      "permissions": [
        "string"
      ],
      "sampleCount": 0
    }
  ],
  "total": 0
}

```

管理员用户分页响应

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|list|[[AdminUserVO](#schemaadminuservo)]|false|none||用户列表|
|total|integer(int64)|false|none||总记录数|


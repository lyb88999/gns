# API 参考文档 (API Reference)

本文档详细描述了通用通知系统 (GNS) 的所有 REST API 接口。

## 基础信息 (General Info)

*   **Endpoint URL**: `http://<your-server>:8080`
*   **Content-Type**: `application/json`
*   **认证方式**: Bearer Token
    *   在 Header 中添加 `Authorization: Bearer <your_token>`
    *   Token 可以通过 `/auth/login` (用户登录) 或 `/tokens` (API Token) 获取。

---

## 1. 认证 (Auth)

### 1.1 用户登录
`POST /api/v1/auth/login`

获取访问令牌（JWT 或 Session Token）。

**请求参数 (Request):**
```json
{
  "username": "admin",  // 必填
  "password": "password" // 必填
}
```

**响应 (Response):**
```json
{
  "token": "sk_live_...",
  "expiresAt": "2026-01-14T12:00:00",
  "userInfo": {
    "id": 1,
    "username": "admin",
    "role": "admin",
    "teamId": null
  }
}
```

### 1.2 用户注册
`POST /api/v1/auth/register`

注册新用户。

**请求参数 (Request):**
```json
{
  "username": "newuser", // 必填, max 50
  "password": "password", // 必填, min 6
  "email": "user@example.com" // 必填, email格式
}
```

### 1.3 获取当前用户信息
`GET /api/v1/auth/me`

**Header**: `Authorization: Bearer <token>`

---

## 2. 通知 (Notification)

### 2.1 发送通知
`POST /api/v1/notify`

根据任务（模版）发送单条通知。

**Header**: `Authorization: Bearer <token>`

**请求参数 (Request):**
```json
{
  "taskId": "550e8400-e29b-41d4-a716-446655440000", // 必填, 任务ID
  "data": { // 必填, 模版变量
    "name": "Alice",
    "code": "1234"
  },
  "priority": "High", // 可选: Low, Normal, High
  "attachments": [ // 可选
    {
      "filename": "invoice.pdf",
      "content": "base64_encoded_string..."
    }
  ]
}
```

**响应 (Response):**
返回该通知任务的完整配置 (NotificationTaskResponse)。

---

## 3. 任务管理 (Tasks)

### 3.1 创建任务
`POST /api/v1/tasks`

**请求参数 (Request):**
```json
{
  "name": "每日报表", // 必填
  "description": "每天发送的统计数据",
  "triggerType": "cron", // 必填, cron 或 api
  "cronExpression": "0 0 12 * * ?", // 当 triggerType=cron 时必填
  "channels": ["Email", "DingTalk"], // 必填, 至少一个
  "messageTemplate": "您好 ${user}, 今日数据如下...", // 必填
  "priority": "Normal",
  "rateLimitEnabled": true,
  "maxPerHour": 100,
  "maxPerDay": 1000,
  "silentStart": "22:00", // 静默开始时间
  "silentEnd": "08:00",   // 静默结束时间
  "mergeWindowMinutes": 5 // 合并窗口(分钟)
}
```

### 3.2 更新任务
`PUT /api/v1/tasks/{taskId}`

参数同创建任务。

### 3.3 获取单个任务详情
`GET /api/v1/tasks/{taskId}`

### 3.4 任务列表
`GET /api/v1/tasks?page=0&size=10`

### 3.5 删除任务
`DELETE /api/v1/tasks/{taskId}`

### 3.6 手动触发任务
`POST /api/v1/tasks/{taskId}/execute`

立即执行一次该任务（即便是 cron 类型）。

---

## 4. 日志 (Logs)

### 4.1 查询日志
`GET /api/v1/logs`

**Query 参数**:
*   `page`: 页码 (默认 0)
*   `size`: 每页数量 (默认 10)
*   `status`: 筛选状态 (SUCCESS, FAILED, BLOCKED)
*   `search`: 搜索关键字 (TaskId 或 ErrorMsg)

---

## 5. API 令牌 (Api Tokens)

### 5.1 创建令牌
`POST /api/v1/tokens`

**请求参数:**
```json
{
  "name": "CI/CD Token", // 必填
  "scopes": ["read", "write"],
  "expiresAt": "2027-01-01T00:00:00" // 可选, 不填则永不过期
}
```

### 5.2 令牌列表
`GET /api/v1/tokens`

### 5.3 删除/撤销令牌
`DELETE /api/v1/tokens/{id}`

---

## 6. 用户与团队 (Admin Only)

### 6.1 用户管理
*   `GET /api/v1/users`: 用户列表
*   `POST /api/v1/users`: 创建用户
    ```json
    {
      "username": "bob",
      "password": "password",
      "email": "bob@example.com",
      "role": "user", // admin, team_admin, user
      "teamId": 1
    }
    ```
*   `PUT /api/v1/users/{id}`: 更新用户
*   `DELETE /api/v1/users/{id}`: 删除用户

### 6.2 团队管理
*   `GET /api/v1/teams`: 团队列表
*   `POST /api/v1/teams`: 创建团队
    ```json
    {
      "name": "DevOps Team",
      "description": "运维团队"
    }
    ```
*   `PUT /api/v1/teams/{id}`: 更新团队
*   `DELETE /api/v1/teams/{id}`: 删除团队

---

## 7. 系统 (System)

### 7.1 仪表盘统计
`GET /api/v1/dashboard/stats`

**响应**:
```json
{
  "activeTasks": 12,
  "totalNotifications": 10500,
  "successRate": 99.5,
  "successRateChange": 0.2
}
```

### 7.2 健康检查
`GET /health`

**响应**:
```json
{
  "service": "universal-notification-system",
  "status": "UP",
  "timestamp": "2026-01-13T15:58:00Z"
}
```

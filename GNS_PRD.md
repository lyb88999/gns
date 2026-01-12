# 通用通知系统 产品需求文档（PRD）

| **属性**     | **内容**                                      |
| ------------ | --------------------------------------------- |
| **产品名称** | 通用通知系统（Universal Notification System） |
| **文档版本** | v1.1 (轻量版 - 适配低配服务器)                |
| **编写日期** | 2026-01-09                                    |
| **目标用户** | 研发团队（初期2人，后续扩展）                 |
| **开发周期** | MVP 2周，后续迭代3周                          |

## 一、产品概述

### 1.1 产品定位

面向研发团队的私有部署通用通知系统，为异步任务执行提供多渠道、可配置的通知服务，解决长时间运行任务的实时通知问题。

### 1.2 核心价值

- **统一接入**：一次接入，支持多种通知渠道
- **配置灵活**：Web界面可视化配置，无需修改代码
- **智能控制**：频率限制、消息合并、静默时段
- **团队协作**：支持多用户、权限管理、任务共享

### 1.3 产品愿景

成为团队内部标准的通知服务，覆盖所有需要异步通知的场景（实验训练、数据处理、定时任务、系统监控等）。

## 二、需求背景

### 2.1 问题描述

**现状痛点：**

- 实验代码运行时间长（数小时），无法实时知道完成状态
- 每次都要轮询或手动检查，效率低下
- 多个项目各自实现通知功能，代码重复
- 通知方式单一（仅邮件），无法适应不同场景

**典型场景示例：**

```
# 当前的做法
def train_model():
    model.train()  # 运行3小时
    # 完成了也不知道，要不断刷新查看

# 期望的做法
def train_model():
    model.train()
    notify.send(
        task_id="ml_training",
        data={"accuracy": 0.95, "time": "3h"}
    )
    # 自动通过微信/邮件通知
```

### 2.2 目标用户

- **主要用户**：算法工程师、后端开发
- **次要用户**：测试、运维
- **用户规模**：初期2人，半年内扩展至10人

## 三、用户画像与用户故事

### 3.1 用户画像

- **Persona 1：算法工程师 - 小李**
  - **年龄**：28岁
  - **工作内容**：深度学习模型训练
  - **痛点**：实验跑一次3-8小时，经常忘记查看结果
  - **期望**：训练完成自动通知到微信，附带关键指标
- **Persona 2：后端开发 - 小王**
  - **年龄**：26岁
  - **工作内容**：数据ETL任务开发
  - **痛点**：定时任务失败时无法及时发现
  - **期望**：任务失败立即告警，成功不通知（减少干扰）

### 3.2 用户故事

| **ID** | **角色**   | **需求**                         | **目的**                 | **优先级** |
| ------ | ---------- | -------------------------------- | ------------------------ | ---------- |
| US-001 | 算法工程师 | 我想在代码中一行调用发送通知     | 快速接入，不影响主流程   | P0         |
| US-002 | 算法工程师 | 我想收到训练完成通知，包含准确率 | 及时了解实验结果         | P0         |
| US-003 | 后端开发   | 我想配置定时任务通知             | 每天早上收到昨日数据统计 | P0         |
| US-004 | 团队管理员 | 我想管理团队成员的通知权限       | 避免通知滥用             | P1         |
| US-005 | 所有用户   | 我想在Web界面创建通知任务        | 不用写代码就能配置       | P0         |
| US-006 | 所有用户   | 我想查看历史通知记录             | 追溯问题、统计分析       | P1         |
| US-007 | 算法工程师 | 我想附加训练日志文件             | 直接查看详细信息         | P1         |
| US-008 | 所有用户   | 我想设置夜间免打扰               | 避免半夜被通知吵醒       | P1         |

## 四、功能需求

### 4.1 功能架构

- **用户管理模块**：用户注册/登录、角色权限管理、团队管理
- **任务配置模块**：通知任务CRUD、触发规则配置、通知渠道选择、消息模板配置、频率控制设置
- **通知发送模块**：API接入、消息队列处理（Redis Stream）、多渠道发送、附件处理
- **历史记录模块**：通知历史查询、数据统计分析、日志归档
- **API管理模块**：Token生成、Token权限管理、API文档
- **系统配置模块**：渠道配置（SMTP/企业微信/钉钉）、全局限流配置、存储策略配置

### 4.2 用户管理模块（P0）

**功能点：**

- 用户注册、登录、登出
- 用户信息管理（邮箱、手机号）
- 角色管理（超级管理员、团队管理员、普通用户）
- 团队创建与成员管理

**角色权限：**

- **超级管理员**：系统配置管理、用户/团队管理、通知渠道配置
- **团队管理员**：团队成员管理、团队通知配置管理、查看团队所有通知记录
- **普通用户**：创建/管理个人通知任务、查看个人通知记录、接收通知

### 4.3 任务配置模块（P0）

**任务创建 - 基本字段：**

| **字段名**       | **类型** | **必填** | **说明**                              | **示例**                        |
| ---------------- | -------- | -------- | ------------------------------------- | ------------------------------- |
| task_id          | String   | 是       | 任务唯一标识                          | ml_training_01                  |
| name             | String   | 是       | 任务名称                              | 模型训练通知                    |
| description      | Text     | 否       | 任务描述                              | ResNet50训练完成通知            |
| trigger_type     | Enum     | 是       | 触发方式                              | api/cron/webhook                |
| cron_expression  | String   | 条件     | Cron表达式（trigger_type=cron时必填） | 0 9 * * *                       |
| channels         | Array    | 是       | 通知渠道（多选）                      | `["email", "wechat"]`           |
| message_template | Text     | 是       | 消息模板（支持变量）                  | 训练完成！准确率: {{.accuracy}} |
| priority         | Enum     | 否       | 优先级                                | low/normal/high/urgent          |

**频率控制字段：**

| **字段名**           | **类型** | **默认值** | **说明**         |
| -------------------- | -------- | ---------- | ---------------- |
| rate_limit_enabled   | Boolean  | true       | 是否启用频率限制 |
| max_per_hour         | Integer  | 10         | 每小时最大发送数 |
| max_per_day          | Integer  | 100        | 每天最大发送数   |
| silent_start         | Time     | null       | 静默开始时间     |
| silent_end           | Time     | null       | 静默结束时间     |
| merge_window_minutes | Integer  | 5          | 合并窗口（分钟） |

### 4.4 通知发送模块（P0）

**API接入示例：**

```
POST /api/v1/notify
Authorization: Bearer {token}
Content-Type: application/json

{
  "task_id": "ml_training_01",
  "data": {
    "model_name": "ResNet50",
    "accuracy": 0.95,
    "training_time": "3h 25m"
  },
  "attachments": [
    {
      "filename": "training.log",
      "content": "base64_encoded_content"
    }
  ],
  "priority": "normal"
}
```

**支持的通知渠道：**

- **邮件（P0）**：SMTP协议，支持HTML、附件
- **企业微信（P1）**：企业微信机器人Webhook，支持文本、Markdown
- **钉钉（P1）**：钉钉机器人Webhook，支持文本、Markdown、@指定人

### 4.5 历史记录模块（P1）

**查询条件：**

- 时间范围：支持快捷选择（今天、昨天、最近7天、最近30天、自定义）
- 任务筛选：下拉选择
- 渠道筛选：多选
- 状态筛选：pending/success/failed
- 关键词搜索：任务名称、消息内容

**统计维度：**

- 今日/本周/本月通知总数
- 成功率趋势图（折线图）
- 各渠道使用占比（饼图）
- TOP 5 活跃任务（柱状图）
- 失败原因分布

## 五、技术方案

### 5.1 技术选型（轻量化适配）

| **技术栈**   | **选型**              | **版本** | **说明**                                                     |
| ------------ | --------------------- | -------- | ------------------------------------------------------------ |
| 后端框架     | Spring Boot           | 3.2.x    | Java主流框架，单进程部署                                     |
| ORM          | MyBatis Plus          | 3.5.x    | 简化CRUD操作                                                 |
| 认证         | Spring Security + JWT | -        | 安全框架                                                     |
| **消息队列** | **Redis Stream**      | 7.x      | **替代Kafka**，复用Redis，极大节省内存                       |
| 缓存         | Redis                 | 7.x      | 频率限制、分布式锁、消息队列                                 |
| 数据库       | MySQL                 | 8.0.x    | 关系型数据库 (建议配置 `innodb_buffer_pool_size=256M` 以防 OOM) |
| 前端框架     | Vue 3                 | 3.4.x    | 组合式API                                                    |
| UI组件库     | Element Plus          | 2.5.x    | 企业级组件库                                                 |
| 状态管理     | Pinia                 | 2.1.x    | Vue官方推荐                                                  |
| 容器化       | Docker                | -        | 统一部署环境                                                 |

### 5.2 系统架构

```
graph TD
    User[用户浏览器] -->|HTTPS| Nginx[Nginx 反向代理]
    Nginx --> Vue[Vue 3 前端]
    Vue <--> API[Spring Boot API]
    API --> MySQL[MySQL]
    API -->|读写缓存 & 消息投递| Redis[Redis (Stream + Cache)]
    
    subgraph "后端服务 (Spring Boot)"
        API
        Worker[内部异步 Workers]
    end
    
    Worker -->|消费消息| Redis
    Worker --> Email[邮件]
    Worker --> Wechat[企业微信]
    Worker --> DingTalk[钉钉]
```

### 5.3 核心流程

**通知发送流程：**

1. 用户代码调用API
2. 参数校验 + 权限检查
3. 查询任务配置
4. 频率限制检查（Redis）
5. 静默时段检查
6. 合并窗口检查（Redis）
7. 渲染消息模板
8. 处理附件上传
9. 写入数据库日志（status=pending）
10. **发送到 Redis Stream (XADD)**
11. 返回成功响应

**Worker消费流程（Spring Boot 内部线程）：**

1. **从 Redis Stream 消费消息 (XREADGROUP)**
2. 根据渠道类型调用对应 Handler
3. 发送通知（邮件/企业微信/钉钉）
4. 更新数据库日志状态
5. 失败则重试（最多3次，利用 Redis Stream 的 Pending List 机制）

## 六、数据库设计

### 6.1 核心数据表

1. **用户表（users）**
   - `id`: BIGINT 主键
   - `username`: VARCHAR(50) 用户名
   - `password`: VARCHAR(255) 密码（bcrypt加密）
   - `email`: VARCHAR(100) 邮箱
   - `role`: ENUM 角色（admin/team_admin/user）
   - `team_id`: BIGINT 团队ID
   - `status`: TINYINT 状态（1:启用 0:禁用）
   - `created_at`, `updated_at`: TIMESTAMP
2. **团队表（teams）**
   - `id`: BIGINT 主键
   - `name`: VARCHAR(100) 团队名称
   - `description`: TEXT 描述
   - `created_at`, `updated_at`: TIMESTAMP
3. **通知任务配置表（notification_tasks）**
   - `id`: BIGINT 主键
   - `task_id`: VARCHAR(100) 任务唯一标识
   - `name`: VARCHAR(200) 任务名称
   - `description`: TEXT 描述
   - `user_id`: BIGINT 用户ID
   - `team_id`: BIGINT 团队ID
   - `trigger_type`: ENUM 触发方式
   - `cron_expression`: VARCHAR(100)
   - `channels`: JSON 通知渠道数组
   - `message_template`: TEXT
   - `custom_data`: JSON
   - `rate_limit_enabled`: TINYINT
   - `max_per_hour`: INT
   - `max_per_day`: INT
   - `silent_start`, `silent_end`: TIME
   - `merge_window_minutes`: INT
   - `priority`: ENUM
   - `status`: TINYINT
   - `created_at`, `updated_at`: TIMESTAMP
4. **通知历史表（notification_logs）**
   - `id`: BIGINT 主键
   - `notification_id`: VARCHAR(64) 通知ID
   - `task_id`: VARCHAR(100) 任务ID
   - `user_id`: BIGINT 用户ID
   - `channel`: VARCHAR(50) 通知渠道
   - `recipient`: VARCHAR(200) 接收人
   - `subject`: VARCHAR(500) 主题
   - `content`: TEXT 内容
   - `custom_data`: JSON
   - `attachments`: JSON
   - `status`: ENUM 状态
   - `error_message`: TEXT
   - `retry_count`: INT
   - `sent_at`: TIMESTAMP
   - `created_at`: TIMESTAMP
5. **API Token表（api_tokens）**
   - `id`: BIGINT 主键
   - `user_id`: BIGINT 用户ID
   - `token`: VARCHAR(64) Token（SHA256）
   - `name`: VARCHAR(100) 备注名
   - `scopes`: JSON 权限范围
   - `last_used_at`: TIMESTAMP
   - `expires_at`: TIMESTAMP
   - `created_at`: TIMESTAMP
6. **系统配置表（system_configs）**
   - `id`: BIGINT 主键
   - `config_key`: VARCHAR(100)
   - `config_value`: TEXT
   - `description`: VARCHAR(500)
   - `updated_at`: TIMESTAMP

### 6.2 索引设计

- **notification_tasks表**：
  - `idx_user (user_id)`
  - `idx_team (team_id)`
  - `idx_task_id (task_id)` UNIQUE
- **notification_logs表**：
  - `idx_notification_id (notification_id)`
  - `idx_task (task_id)`
  - `idx_user (user_id)`
  - `idx_status (status)`
  - `idx_created (created_at)` - 用于归档查询

### 6.3 数据归档策略

- **保留周期**：7天
- **归档方式**：定时任务（每天凌晨2点）将7天前数据移至归档表
- **归档表**：`notification_logs_archive`（结构与原表相同）
- **附件清理**：同步清理归档数据对应的附件文件

## 七、非功能需求

### 7.1 性能需求

| **指标**     | **目标值** | **说明**                     |
| ------------ | ---------- | ---------------------------- |
| API响应时间  | < 200ms    | P95                          |
| 通知发送延迟 | < 30s      | 从API调用到实际发送          |
| 并发处理能力 | ≥ 50 QPS   | 适配硬件降级，满足小团队需求 |
| 数据库查询   | < 100ms    | 单表查询                     |

### 7.2 可用性需求

- **系统可用性**：≥ 99%
- **数据可靠性**：通知记录不丢失
- **故障恢复**：**Redis AOF持久化**，服务重启后自动恢复消费进度

### 7.3 安全需求

- **认证**：JWT Token，有效期24小时
- **授权**：基于角色的权限控制（RBAC）
- **数据加密**：密码使用BCrypt加密，敏感配置使用AES-256加密
- **传输安全**：生产环境强制HTTPS
- **防护**：API限流、SQL注入防护、XSS防护

## 八、开发计划

### 8.1 MVP阶段（2周）

**第1周：核心功能**

- **Day 1-2：环境搭建 + 数据库设计**
  - Docker环境搭建（MySQL 8.0 + Redis）
  - 数据库表创建
  - Spring Boot项目脚手架
- **Day 3-4：后端API开发**
  - 用户认证（JWT）
  - 任务CRUD API
  - 发送通知API
  - Token管理API
- **Day 5：Redis消费者 + 邮件发送**
  - **Redis Stream 消费者开发**
  - 邮件发送逻辑实现

**第2周：前端 + 完善**

- **Day 1-3：Vue前端开发**
  - 登录页面
  - 任务列表/创建/编辑
  - 通知历史查询
  - Token管理页面
- **Day 4：SDK开发**
  - Python SDK
  - 使用文档
- **Day 5：测试 + 部署**
  - 功能测试
  - **Docker Compose 一键部署脚本**
  - 文档完善

### 8.2 后续迭代

- **迭代2（1周）**
  - 企业微信/钉钉集成
  - 附件上传功能
  - 频率控制完善
- **迭代3（1周）**
  - 数据统计看板
  - 告警规则
  - 更多通知渠道

## 九、风险与挑战

### 9.1 技术风险

| **风险**           | **影响**           | **应对措施**                                                 |
| ------------------ | ------------------ | ------------------------------------------------------------ |
| **Redis 内存满载** | **新消息无法写入** | **Redis Stream 设置 MAXLEN 限制队列长度；配置 Redis 内存淘汰策略** |
| 邮件服务器不稳定   | 通知发送失败       | 实现重试机制，配置备用邮件服务器                             |
| MySQL 内存溢出     | 服务宕机           | 配置 MySQL `innodb_buffer_pool_size` 为 256M 或更低          |
| 附件存储空间不足   | 附件上传失败       | 定期归档清理，设置存储告警                                   |

### 9.2 业务风险

| **风险**     | **影响**       | **应对措施**                   |
| ------------ | -------------- | ------------------------------ |
| 通知滥用     | 用户体验下降   | 严格的频率限制，管理员审核机制 |
| 敏感信息泄露 | 安全风险       | 权限控制，敏感数据脱敏         |
| 用户需求变化 | 功能不满足需求 | 快速迭代，及时收集反馈         |

## 十、附录

### 10.1 术语表

| **术语**         | **说明**                                              |
| ---------------- | ----------------------------------------------------- |
| **Task**         | 通知任务，用户配置的通知规则                          |
| **Channel**      | 通知渠道，如邮件、企业微信、钉钉等                    |
| **Worker**       | 消费者，负责从消息队列消费消息并发送通知              |
| **Redis Stream** | Redis 5.0+ 引入的流式数据结构，用于实现轻量级消息队列 |
| **RBAC**         | 基于角色的访问控制                                    |
| **Cron**         | 定时任务表达式                                        |

### 10.2 参考资料

- Spring Boot官方文档：https://spring.io/projects/spring-boot
- MyBatis Plus文档：https://baomidou.com
- **Redis Stream教程**：https://www.google.com/search?q=https://redis.io/docs/data-types/streams/
- Vue 3官方文档：https://vuejs.org
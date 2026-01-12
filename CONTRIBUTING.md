# 贡献指南 (Contributing Guide)

为了保持代码库的整洁和历史记录的可读性，我们采用 [Conventional Commits](https://www.conventionalcommits.org/) (约定式提交) 规范。

## 提交信息格式 (Commit Message Format)

每条提交信息由 **Header (标题)**, **Body (正文)** 和 **Footer (页脚)** 组成。

```
<type>(<scope>): <subject>
<BLANK LINE>
<body>
<BLANK LINE>
<footer>
```

### 1. Header (标题)

标题只有一行，包括三个字段：`type`（必需）、`scope`（可选）和 `subject`（必需）。

#### Type (类型)

必须是以下类型之一：

*   **feat**: 新功能 (Feature)
*   **fix**: 修复 Bug
*   **docs**: 仅文档更改
*   **style**: 不影响代码含义的更改（空格、格式化、缺少分号等）
*   **refactor**: 代码重构（既不是修复 bug 也不是添加功能）
*   **perf**: 提高性能的代码更改
*   **test**: 添加缺失的测试或更正现有的测试
*   **build**: 影响构建系统或外部依赖项的更改 (示例 scopes: gulp, broccoli, npm)
*   **ci**: 对 CI 配置文件和脚本的更改 (示例 scopes: Travis, Circle, BrowserStack, SauceLabs)
*   **chore**: 其他不修改 src 或 test 文件的更改
*   **revert**: 撤销之前的提交

#### Scope (范围) - 可选

用于说明提交影响的范围，例如：
*   `auth`
*   `task`
*   `sdk`
*   `frontend`
*   `deps`

#### Subject (主题)

*   简短描述变更内容。
*   使用中文或英文均可（建议团队内部统一，本项目默认推荐 **中文**）。
*   以动词开头，使用第一人称现在时（例如："添加..." 而不是 "添加了..."）。
*   结尾不加句号。

### 2. Body (正文) - 可选

*   详细描述代码变更的动机和与之前行为的对比。
*   可以包含多行。

### 3. Footer (页脚) - 可选

*   **不兼容变动 (Breaking Changes)**: 以 `BREAKING CHANGE:` 开头，后面跟一个空格或两个换行符。
*   **关闭 Issue**: 例如 `Closes #123`, `Fixes #123`。

---

## 示例 (Examples)

### 新功能
```
feat(task): 增加任务定时调度功能

使用 Redis Stream 实现异步任务调度，支持 Cron 表达式。
```

### 修复 Bug
```
fix(frontend): 修复登录页面的表单验证错误

当用户名为空时，现在会正确显示错误提示 "Username is required"。
Closes #28
```

### 文档修改
```
docs: 更新 API 文档中的 Token 认证部分
```

### 性能优化
```
perf(API): 优化日志查询接口性能

为 notification_logs 表添加了 (user_id, created_at) 联合索引，查询速度提升 50%。
```

### 破坏性变更
```
feat(auth): 迁移至 JWT 认证

BREAKING CHANGE: 登录接口返回值结构改变，不再返回 session_id，改为返回 access_token。
```

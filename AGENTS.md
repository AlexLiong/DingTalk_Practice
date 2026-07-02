# DingTalk Practice — 仿钉钉企业协作管理系统

> Spring Boot 3.2.5 + Vue 3 + Spring AI 1.0.9 + MyBatis-Plus + WebSocket + H2/MySQL

## 项目概述

这是一个功能完整的企业协作管理平台，模拟钉钉核心工作流。系统采用前后端分离架构，后端基于 Spring Boot 3.2.5 全家桶，前端基于 Vue 3 + Element Plus，支持即时通讯、RBAC 权限管理、文件协作、审批流、通知公告、待办任务、日程管理、数据看板、AI 智能助手等全线功能。

## 技术栈

| 层级 | 技术选型 | 版本 |
|------|---------|------|
| **后端框架** | Spring Boot | 3.2.5 |
| **ORM** | MyBatis-Plus | 3.5.7 |
| **安全认证** | Spring Security + JWT (jjwt 0.12.5) | RBAC 模型 |
| **实时通信** | Spring WebSocket (STOMP) + SockJS | — |
| **AI 能力** | Spring AI (OpenAI 兼容接口) | 1.0.9 |
| **文档解析** | Apache Tika | 2.9.2 |
| **数据库** | H2 (开发) / MySQL (生产) | — |
| **前端框架** | Vue 3 (Composition API) | 3.4.31 |
| **UI 组件库** | Element Plus | 2.7.6 |
| **状态管理** | Pinia | 2.1.7 |
| **路由** | Vue Router | 4.4.0 |
| **构建工具** | Vite | 5.3.3 |
| **图表库** | ECharts | 5.5.0 |
| **鸿蒙端** | HarmonyOS (ArkTS + WebView) | — |

## 关键模块速查

### 即时通讯 (IM)
- `ChatService` — 会话管理、消息收发、撤回、已读回执、单聊/群聊
- `ChatController` — REST + WebSocket 双通道消息推送
- `Workbench.vue` — 钉钉风格三栏布局前端（导航 + 会话列表 + 聊天区）
- 支持：文本/图片/文件/视频消息、@提醒、表情反应、消息引用、消息收藏

### AI 助手
- `UnifiedAiService` — 统一 AI 服务入口，融合 **RAG 问答 + Function Calling**
- `AiChatConfig` — AI 配置（ChatClient + MessageWindowChatMemory 多轮记忆）
- `AiDocumentIngestService` — 文档解析、分块、向量化入库（Apache Tika）
- Function Calling 工具（静态内部类）：
  - `ScheduleTool` — AI 对话创建日程
  - `SendMessageTool` — AI 代发消息
  - `SendMailTool` — AI 代发邮件
  - `SendDingTool` — AI 发送 DING 提醒
  - `CreateTodoTool` — AI 创建待办
  - `PublishNoticeTool` — AI 发布公告（需管理员权限）
  - `SearchUserTool` — AI 查询通讯录
- API: `POST /api/chat/ai-reply` (SSE 流式)

### 后台管理 (RBAC)
- `PermissionService` — 权限查询（菜单/角色/权限标识）
- `UserAdminService` / `RoleService` / `DeptService` — 用户/角色/部门 CRUD
- `MenuMapper` — 三级菜单树（目录→菜单→按钮），支持动态渲染
- `DataInitializer` — 种子数据（演示账号、菜单、权限、角色）
- 前端 `v-if="hasPerm('xxx')"` + 后端 `@PreAuthorize` 双重校验

### 企业协作
- `FileService` — 文件上传/管理/共享（含 AI 自动索引）
- `ApprovalService` — 审批流（请假/报销）
- `TodoService` — 待办任务
- `ScheduleController` — 日程 CRUD

### 通知与协同
- `NoticeService` — 系统公告/通知（RBAC 权限控制）
- `MailboxService` — 企业邮箱
- `DingService` — DING 紧急消息
- `WorkNoticeService` — 统一 WebSocket 工作通知推送

### 数据看板
- `DashboardService` — 运营数据汇总 + ECharts 可视化

## 开发规范

- 后端：中文注释，统一返回 `Result<T>`（code=200/403/500），业务异常 `BizException`
- 前端：Composition API，API 统一入口 `src/api/index.js`，自动注入 Bearer Token
- 权限标识命名：`模块:功能:操作`（如 `system:user:add`）
- 消息类型：1文本 2图片 3文件 4系统 5视频
- 会话类型：1单聊 2群聊
- 安全工具：`SecurityUtils.getUserId()` 获取当前用户 ID
- AI Function Calling 工具：静态内部类 + `@Tool` 注解 + `@ToolParam` 注解

## 环境要求

- JDK 17+、Maven 3.6+、Node.js 18+

## 启动

```bash
# 后端
cd backend && mvn spring-boot:run  # → http://localhost:8080

# 前端
cd frontend && npm install && npm run dev  # → http://localhost:5173
```

## 演示账号

| 账号 | 密码 | 角色 |
|------|------|------|
| admin | Boz@2026 | 超级管理员 |
| chenyuqi | Boz@2026 | 普通员工 |

## 更多文档

详见 `document/` 目录下的模块设计文档。

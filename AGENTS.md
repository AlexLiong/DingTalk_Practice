# DingTalk Practice

## 项目概述
这是一个仿钉钉的企业协作管理平台，包含前后端项目。

## 技术栈
- 后端：Spring Boot 3.2.5 + MyBatis-Plus + Spring AI 1.0.9
- 前端：React/Vue（待确认）
- AI集成：Spring AI OpenAI 兼容接口

## 关键模块

### AI 相关
- `AiChatConfig` - AI聊天配置，包含多轮对话记忆（MessageWindowChatMemory）
- `UnifiedAiService` - **统一 AI 服务**，融合 RAG 问答 + Function Calling 日程创建
    - 通过 `/api/chat/ai-reply` 单一入口，自动识别用户意图
    - 包含内部类 `ScheduleTool`，将 `createSchedule` 注册为 Function Calling 工具
- `AiDocumentIngestService` - 文档索引服务

### Function Calling 使用
- 用户通过 `/api/chat/ai-reply` 接口发送自然语言请求
- AI自动识别意图：普通问答走 RAG，日程相关调用 `createSchedule` 工具
- 工具方法提取参数（标题、时间等）并写入数据库

### 日程相关
- `SysSchedule` - 日程实体类
- `ScheduleMapper` - MyBatis-Plus Mapper
- `ScheduleController` - REST API 控制器

### 日程相关
- `SysSchedule` - 日程实体类
- `ScheduleMapper` - MyBatis-Plus Mapper
- `ScheduleController` - REST API 控制器

## 开发规范
- 使用中文注释
- 遵循现有代码风格
- 安全工具类：`SecurityUtils.getUserId()` 获取当前用户ID

# 项目整体架构 & Code Wiki 索引

> 本文档是仿钉钉企业协作管理系统的 Code Wiki 总索引，按业务功能模块拆分。

---

## 📚 文档导航

- 🔐 认证与权限 
- 💬 即时通讯
- 👥 RBAC 管理 
- 📁 企业协作
- 📢 通知与协同
- 📊 数据看板 & AI
- 🗄️ 数据库设计 

---

## 一、项目简介

**仿钉钉企业协作管理系统** 是一个基于 Spring Boot 3 + Vue 3 的企业级协作办公平台，提供即时通讯、RBAC 权限管理、文件管理、审批流、待办任务、日程管理等核心功能。

### 技术栈

| 层级 | 技术选型 | 版本 |
|------|---------|------|
| **后端框架** | Spring Boot | 3.2.5 |
| **ORM** | MyBatis-Plus | 3.5.7 |
| **数据库** | H2（默认）/ MySQL | - |
| **安全认证** | Spring Security + JWT (jjwt 0.12.5) | - |
| **实时通信** | Spring WebSocket (STOMP) | - |
| **AI 能力** | Spring AI (OpenAI 兼容) | 1.0.9 |
| **文档解析** | Apache Tika | 2.9.2 |
| **前端框架** | Vue 3 | 3.4.31 |
| **UI 组件库** | Element Plus | 2.7.6 |
| **状态管理** | Pinia | 2.1.7 |
| **路由** | Vue Router | 4.4.0 |
| **构建工具** | Vite | 5.3.3 |
| **HTTP 客户端** | Axios | 1.7.2 |
| **图表** | ECharts | 5.5.0 |
| **WebSocket 客户端** | @stomp/stompjs + sockjs-client | 7.0.0 / 1.6.1 |

### 环境要求

- **JDK**: 17+
- **Maven**: 3.6+
- **Node.js**: 18+（推荐 20/22）
- **数据库**: H2（内置，免安装）或 MySQL 5.7+/8.0

---

## 二、整体架构

### 2.1 架构分层

```
┌─────────────────────────────────────────────────────┐
│                     前端层 (Vue 3)                   │
│  ┌─────────┬─────────┬─────────┬─────────────────┐  │
│  │  Views  │ Store   │ Router  │ Components      │  │
│  └─────────┴─────────┴─────────┴─────────────────┘  │
│  ┌─────────┬─────────┐                               │
│  │   API   │  Utils  │  (Axios / WebSocket / 工具)   │
│  └─────────┴─────────┘                               │
└────────────────────────┬────────────────────────────┘
                         │ HTTP / WebSocket
                         ▼
┌─────────────────────────────────────────────────────┐
│                   后端层 (Spring Boot)                │
│  ┌─────────────────────────────────────────────────┐ │
│  │  Controller 层 (REST API / WebSocket)            │ │
│  └─────────────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────────────┐ │
│  │  Service 层 (业务逻辑)                            │ │
│  └─────────────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────────────┐ │
│  │  Mapper 层 (MyBatis-Plus)                        │ │
│  └─────────────────────────────────────────────────┘ │
│  ┌─────────┬──────────┬──────────┬────────────────┐ │
│  │ Common  │ Security │  Config  │ Entity/DTO/VO  │ │
│  └─────────┴──────────┴──────────┴────────────────┘ │
└────────────────────────┬────────────────────────────┘
                         │ JDBC
                         ▼
┌─────────────────────────────────────────────────────┐
│                  数据层 (H2 / MySQL)                 │
│  系统表 + 聊天表 + 业务表                            │
└─────────────────────────────────────────────────────┘
```

### 2.2 模块划分

#### 后端模块

| 模块 | 包路径 | 职责说明 |
|------|--------|---------|
| **通用模块** | `common` | 统一返回体、业务异常、全局异常处理、安全工具 |
| **配置模块** | `config` | 安全配置、WebSocket 配置、MyBatis 配置、数据初始化 |
| **安全模块** | `security` | JWT 工具类、JWT 认证过滤器 |
| **实体模块** | `entity` | 数据库实体类（用户、角色、菜单、聊天、文件等） |
| **数据访问层** | `mapper` | MyBatis-Plus Mapper 接口 |
| **业务逻辑层** | `service` | 核心业务逻辑实现 |
| **控制层** | `controller` | REST API 控制器 |
| **数据传输对象** | `dto` | 请求参数封装 |
| **视图对象** | `vo` | 响应数据封装 |

#### 前端模块

| 模块 | 目录 | 职责说明 |
|------|------|---------|
| **API 层** | `src/api` | Axios 封装、接口定义、SSE 流式 API |
| **状态管理** | `src/store` | Pinia Store（用户、主题、协作、偏好） |
| **路由** | `src/router` | 路由配置、登录守卫 |
| **布局** | `src/layout` | 管理后台布局组件 |
| **组件** | `src/components` | 通用组件（侧边导航等） |
| **页面** | `src/views` | 业务页面组件 |
| **工具** | `src/utils` | WebSocket 封装、工作台通知等 |

---

## 三、核心功能模块

### 3.1 即时通讯 (IM)

- **单聊/群聊**: 支持一对一私聊和多人群聊
- **实时消息**: 基于 WebSocket (STOMP) 的消息推送
- **消息类型**: 文本、图片、文件、系统消息、视频
- **消息撤回**: 本人或群主可撤回消息
- **@提醒**: 群聊内 @ 成员，高亮显示
- **已读回执**: 消息已读状态追踪
- **消息搜索**: 全局搜索聊天记录
- **会话管理**: 置顶、免打扰、特别关注
- **表情反应**: 消息表情回应
- **AI 助手**: 基于 Spring AI 的智能对话 + RAG 文件分析

### 3.2 RBAC 权限管理

- **用户管理**: 用户 CRUD、角色分配、重置密码、部门筛选
- **角色管理**: 角色 CRUD、权限树分配
- **菜单管理**: 目录/菜单/按钮三级结构、权限标识维护
- **部门管理**: 多级部门树、组织架构
- **双层权限控制**: 前端 `v-if` + 后端 `@PreAuthorize`
- **动态菜单**: 登录后按权限渲染侧边栏

### 3.3 企业协作

- **文件管理**: 文件上传、下载、共享、锁定、回收站
- **审批流**: 请假、报销等审批申请与处理
- **待办任务**: 任务创建、分配、状态跟踪
- **日程管理**: 日历视图、日程安排
- **通知公告**: 通知发布、阅读状态
- **邮箱**: 邮件收发、星标、归档
- **DING**: 紧急消息提醒、确认
- **收藏**: 消息收藏管理
- **数据看板**: ECharts 数据可视化

---

## 四、关键技术架构

### 4.1 认证与授权

```
用户登录 → AuthService.login()
    ↓
验证用户名密码 (BCrypt)
    ↓
生成 JWT Token (JwtUtil.generate())
    ↓
返回 Token + 用户信息 + 权限列表 + 菜单树

每次请求 → JwtAuthenticationFilter
    ↓
解析 Authorization: Bearer {token}
    ↓
验证 Token 有效性 → 加载用户权限
    ↓
注入 SecurityContext → @PreAuthorize 校验
```

### 4.2 WebSocket 实时通信

```
客户端连接 /ws (SockJS)
    ↓
WebSocketConfig 拦截 CONNECT 帧
    ↓
从 Header 提取 JWT → 验证 → 绑定 Principal
    ↓
订阅 /user/queue/message (个人消息)
    ↓
发送消息 → ChatService.sendMessage()
    ↓
保存消息 → 更新会话 → SimpMessagingTemplate 推送
```

### 4.3 前后端交互

- **HTTP API**: `/api/**` 前缀，统一返回 `Result<T>` 格式
- **WebSocket**: `/ws` 端点，STOMP 协议
- **文件上传**: `/api/file/upload`，最大 100MB
- **SSE 流式**: AI 对话使用 Server-Sent Events 流式响应

---

## 五、项目目录结构

```
DingTalk_Practice/
├── backend/                          # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/dingtalk/
│       │   ├── DingtalkAdminApplication.java    # 启动类
│       │   ├── common/                          # 通用模块
│       │   ├── config/                          # 配置模块
│       │   ├── security/                        # 安全模块
│       │   ├── entity/                          # 实体类
│       │   ├── mapper/                          # Mapper 接口
│       │   ├── dto/                             # 请求 DTO
│       │   ├── vo/                              # 响应 VO
│       │   ├── service/                         # 业务服务
│       │   └── controller/                      # REST 控制器
│       └── resources/
│           ├── application.yml                  # 主配置
│           ├── application-h2.yml               # H2 配置
│           ├── application-mysql.yml            # MySQL 配置
│           └── schema.sql                       # 建表脚本
├── frontend/                         # Vue 3 前端
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── api/                             # API 接口
│       ├── store/                           # Pinia 状态
│       ├── router/                          # 路由配置
│       ├── layout/                          # 布局组件
│       ├── components/                      # 通用组件
│       ├── views/                           # 页面视图
│       ├── utils/                           # 工具函数
│       ├── App.vue
│       └── main.js
├── harmony/                          # 鸿蒙端（可选）
├── document/                         # 项目文档
├── AGENTS.md                         # 项目说明
└── README.md                         # 项目 README
```

---

## 六、启动方式

### 6.1 后端启动

```bash
cd backend
mvn spring-boot:run
```

- 后端地址: http://localhost:8080
- H2 控制台: http://localhost:8080/h2-console

### 6.2 前端启动

```bash
cd frontend
npm install
npm run dev
```

- 前端地址: http://localhost:5173

### 6.3 演示账号

| 账号 | 密码 | 角色 | 部门 |
|------|------|------|------|
| admin | Boz@2026 | 超级管理员 | 技术研发部 |
| chenyuqi | Boz@2026 | 普通员工 | 产品运营部 |
| zhouleheng | Boz@2026 | 普通员工 | 前端组 |
| liuyulin | Boz@2026 | 普通员工 | 后端组 |
| liangqinwei | Boz@2026 | 普通员工 | 设计创意部 |
| jiangzezhi | Boz@2026 | 普通员工 | 人力行政部 |

---

## 七、依赖关系图

### 后端核心依赖

```
DingtalkAdminApplication
    ├── common (Result, BizException, SecurityUtils)
    ├── config (SecurityConfig, WebSocketConfig, DataInitializer)
    ├── security (JwtUtil, JwtAuthenticationFilter)
    ├── entity (SysUser, SysRole, SysMenu, ChatSession, ChatMessage, ...)
    ├── mapper (UserMapper, RoleMapper, MenuMapper, ...)
    ├── service
    │   ├── AuthService (依赖 UserMapper, JwtUtil, PermissionService)
    │   ├── PermissionService (依赖 UserRoleMapper, RoleMenuMapper, MenuMapper)
    │   ├── ChatService (依赖 ChatSessionMapper, ChatMessageMapper, SimpMessagingTemplate)
    │   ├── UserAdminService (依赖 UserMapper, RoleService)
    │   ├── RoleService (依赖 RoleMapper, RoleMenuMapper)
    │   ├── DeptService (依赖 DeptMapper)
    │   ├── NoticeService (依赖 NoticeMapper)
    │   ├── FileService (依赖 FileMapper)
    │   ├── ApprovalService (依赖 ApprovalMapper)
    │   ├── TodoService (依赖 TodoMapper)
    │   ├── MailboxService (依赖 MailboxMailMapper)
    │   ├── DingService (依赖 DingMapper)
    │   ├── OnlineService (在线状态管理)
    │   ├── AiRagService (AI RAG 服务)
    │   └── ...
    └── controller (各模块 REST API)
```

### 前端核心依赖

```
main.js
    ├── App.vue
    ├── router (路由守卫)
    ├── pinia (状态管理)
    │   ├── user.js (用户信息、权限)
    │   ├── theme.js (主题)
    │   ├── collab.js (协作)
    │   └── userPreference.js (用户偏好)
    ├── api (Axios 封装)
    │   ├── request.js (拦截器)
    │   └── index.js (接口定义)
    └── views (各业务页面)
        ├── Login.vue
        ├── Workbench.vue (工作台/聊天)
        ├── Documents.vue
        ├── Mailbox.vue
        ├── Ding.vue
        ├── Todo.vue
        ├── Approval.vue
        ├── Calendar.vue
        ├── Notice.vue
        ├── Profile.vue
        ├── Favorites.vue
        └── admin/ (管理后台页面)
```

---

## 八、设计约定

### 8.1 后端约定

- **统一返回格式**: `Result<T>`，code=200 成功，code=500 失败，code=403 无权限
- **异常处理**: 业务异常抛 `BizException`，全局由 `GlobalExceptionHandler` 处理
- **权限标识命名**: `模块:功能:操作`，如 `system:user:add`
- **菜单类型**: 1=目录，2=菜单，3=按钮
- **消息类型**: 1=文本，2=图片，3=文件，4=系统，5=视频
- **会话类型**: 1=单聊，2=群聊
- **成员角色**: 1=群主，2=普通成员

### 8.2 前端约定

- **API 调用**: 统一通过 `src/api/index.js` 导入，自动附加 Bearer Token
- **路由守卫**: 只检查 token（`meta.auth`），不做路由级权限过滤
- **权限判断**: `useUserStore().hasPerm('xxx')` 或 `v-if="hasPerm('xxx')"`
- **状态管理**: 使用 Pinia，用户信息存在 user store
- **组件风格**: Vue 3 Composition API + Element Plus

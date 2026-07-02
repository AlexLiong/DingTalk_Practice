# 仿钉钉企业协作管理系统 —— 可运行脚手架

> Spring Boot 3.2.5 + Vue 3 + Spring AI 1.0.9 + MyBatis-Plus + WebSocket + H2/MySQL
> 开箱即用：后端用 **H2 内存数据库**（免装 MySQL，启动自带种子数据），登录后可 **实时聊天**（WebSocket）。

---

## 一、目录结构
```
dingtalk-admin-demo/
├── backend/                 # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/dingtalk/
│       │   ├── DingtalkAdminApplication.java   启动类
│       │   ├── common/      统一返回/异常/工具
│       │   ├── config/      Security/WebSocket/MyBatis/AI Config/种子数据
│       │   ├── security/    JWT 工具与过滤器
│       │   ├── entity/      实体
│       │   ├── mapper/      MyBatis-Plus Mapper
│       │   ├── dto/ vo/     传输/视图对象
│       │   ├── service/     业务逻辑（含 AI Function Calling 工具）
│       │   └── controller/  REST 接口
│       └── resources/
│           ├── application.yml
│           └── schema.sql   H2 建表脚本
├── frontend/                # Vue3 前端
│   ├── package.json
│   ├── vite.config.js       /api /ws 代理到后端 8080
│   └── src/
│       ├── api/             axios 封装 + 接口
│       ├── store/           Pinia (user/theme/collab/preference)
│       ├── router/          路由 + 登录守卫
│       ├── utils/ws.js      STOMP WebSocket 封装
│       └── views/
│           ├── Login.vue        登录/注册页
│           ├── Workbench.vue    钉钉三栏主界面(导航/会话/聊天)
│           ├── Documents.vue    文件管理
│           ├── Mailbox.vue      企业邮箱
│           ├── Ding.vue         DING消息
│           ├── Todo.vue         待办任务
│           ├── Approval.vue     审批中心
│           ├── Calendar.vue     日程管理
│           ├── Notice.vue       通知公告
│           ├── Profile.vue      个人中心
│           ├── Favorites.vue    消息收藏
│           └── admin/           管理后台
│               ├── UserAdmin.vue      用户管理
│               ├── RoleAdmin.vue      角色管理
│               ├── MenuAdmin.vue      菜单管理
│               ├── DeptAdmin.vue      部门管理
│               ├── Dashboard.vue      数据看板
│               └── NoticeManage.vue   公告管理
├── harmony/                 # 鸿蒙端 (ArkTS + WebView)
├── document/                # 项目模块文档
├── docs-output/             # 自动生成的文档输出目录
├── AGENTS.md                # 项目 AI 开发说明
└── README.md                # 本文件
```

---

## 二、环境要求
- JDK 17+
- Maven 3.6+
- Node.js 18+ (推荐 20/22)

---

## 三、启动步骤
### 1. 启动后端（端口 8080）
```bash
cd backend
mvn spring-boot:run
```

启动成功后：
- 后端地址：http://localhost:8080
- H2 数据库控制台：http://localhost:8080/h2-console
  （JDBC URL: `jdbc:h2:file:./data/dingtalk`，用户名 `sa`，密码空）

> 注意：H2 是文件库，**数据会持久化**。种子数据仅首次初始化时插入，后续重启不会重置数据。

### 2. 启动前端（端口 5173）
```bash
cd frontend
npm install
npm run dev
```

打开浏览器访问：http://localhost:5173

---

## 四、演示账号
| 账号 | 密码 | 角色 | 部门 |
|------|------|------|------|
| admin | Boz@2026 | 超级管理员 | 技术研发部 |
| chenyuqi | Boz@2026 | 普通员工 | 产品运营部 |
| zhouleheng | Boz@2026 | 普通员工 | 前端组 |
| liuyulin | Boz@2026 | 普通员工 | 后端组 |
| liangqinwei | Boz@2026 | 普通员工 | 设计创意部 |
| jiangzezhi | Boz@2026 | 普通员工 | 人力行政部 |

### 体验实时聊天
1. 浏览器 A 用 `admin` 登录
2. 浏览器 B（或无痕窗口）用 `chenyuqi` 登录
3. 在任一端发消息，另一端 **实时收到**（WebSocket 推送）
4. 「通讯录」点击任意同事可发起新的单聊

---

## 五、核心功能

### 即时通讯 (IM)
-  登录 / 注册（JWT 无状态认证 + BCrypt 加密）
-  钉钉风格三栏布局（导航栏 + 会话列表 + 聊天窗口）
-  会话列表（单聊/群聊、最后消息、时间、未读红点）
-  实时消息收发（Spring WebSocket + STOMP，点对点推送）
-  历史消息加载、未读清除、通讯录、发起单聊
-  图片消息（上传、缩略图、点击大图预览）
-  文件消息（上传、下载）
-  消息撤回（本人/群主可撤回）
-  @提醒（群聊内 @ 成员，高亮显示）
-  表情反应、消息引用、消息收藏

### 群聊管理
-  建群（多选成员、自定义群名）
-  拉人 / 踢人（群主权限）
-  群成员列表（群主标记）、群公告、退群

### AI 智能助手
-  基于 Spring AI（OpenAI 兼容接口）的智能对话
-  SSE 流式响应，打字机效果
-  RAG 知识库问答（文档解析 → 分块 → 向量化 → 语义检索）
-  文档分析（支持 PDF/Word/Excel/PPT/TXT/MD 等多种格式，Apache Tika 解析）
-  多轮对话记忆（MessageWindowChatMemory）
-  Function Calling 工具集（AI 可主动调用系统功能）：
  - `createSchedule` — AI 对话创建日程
  - `sendMessage` — AI 代发消息（含联系人/群聊存在性校验）
  - `sendMail` — AI 代发邮件
  - `sendDing` — AI 发送 DING 提醒
  - `createTodo` — AI 创建待办
  - `publishNotice` — AI 发布公告（权限校验）
  - `searchUser` — AI 查询通讯录

### 企业协作
-  文件管理（上传、下载、共享、版本、回收站，含 AI 自动索引）
-  审批流（请假/报销申请与处理）
-  待办任务（任务创建、分配、状态跟踪）
-  日程管理（日历视图，支持月/周/日，AI 一键创建）

### 通知与协同
-  通知公告（发布、浏览、阅读状态，RBAC 权限控制）
-  企业邮箱（收发、星标、归档、草稿、回复）
-  DING 紧急消息（确认机制、多次提醒、截止时间）
-  工作通知（WebSocket 实时推送）

### 数据看板
-  概览统计卡片（用户数/部门数/消息数/在线数）
-  部门分布饼图（ECharts）
-  消息趋势折线图（近7天）
-  响应式布局，适配不同屏幕尺寸

### 后台管理 (RBAC)
-  用户管理（分页、搜索、CRUD、分配角色、重置密码、按部门筛选）
-  角色管理（CRUD、分配权限树）
-  菜单管理（目录/菜单/按钮三级、权限标识树形维护）
-  组织架构（多级部门树、CRUD）
-  按钮级权限控制（前端 v-if + 后端 @PreAuthorize 双重校验）
-  动态菜单（登录后按权限渲染后台侧边栏，管理员可见管理后台入口）

### 个人中心
-  修改资料（姓名/手机/邮箱/性别）、上传头像、修改密码

### 移动端适配
-  响应式布局，手机比例下自动适配
-  鸿蒙端（HarmonyOS ArkTS + WebView）

---

## 六、权限说明
- `admin` 是「超级管理员」（role_key = `admin`），可见「管理后台」入口，拥有全部权限
- 其他账号是「普通员工」，只有协作功能，无后台入口
- 普通员工即使直接调后台接口也会被 `@PreAuthorize` 拦截（返回「无操作权限」）

---

## 七、关键接口

### 认证
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/login | 登录 |
| POST | /api/auth/register | 注册 |
| GET | /api/auth/info | 当前用户 |

### 即时通讯
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/chat/sessions | 我的会话列表 |
| GET | /api/chat/messages?sessionId= | 历史消息 |
| POST | /api/chat/messages | 发消息 |
| POST | /api/chat/read?sessionId= | 标记已读 |
| POST | /api/chat/single?targetUserId= | 发起单聊 |
| WS | /ws → /user/queue/message | 实时消息订阅 |

### AI 助手
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/chat/ai-reply | AI 对话（SSE 流式，含 Function Calling） |

### 后台管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/admin/user/page | 用户分页列表 |
| POST | /api/admin/user | 新增用户 |
| PUT | /api/admin/user | 编辑用户 |
| DELETE | /api/admin/user/{id} | 删除用户 |
| GET | /api/role/page | 角色分页列表 |
| POST | /api/role | 新增角色 |
| PUT | /api/role | 编辑角色 |
| DELETE | /api/role/{id} | 删除角色 |
| GET | /api/menu/tree | 菜单树 |
| POST | /api/menu | 新增菜单 |
| PUT | /api/menu | 编辑菜单 |
| DELETE | /api/menu/{id} | 删除菜单 |
| GET | /api/dept/tree | 部门树 |
| POST | /api/dept | 新增部门 |
| PUT | /api/dept | 编辑部门 |
| DELETE | /api/dept/{id} | 删除部门 |

### 企业协作
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/file/list | 文件列表 |
| POST | /api/file/upload | 文件上传 |
| GET | /api/approval/list | 审批列表 |
| POST | /api/approval | 发起审批 |
| GET | /api/todo/list | 待办列表 |
| POST | /api/todo | 新建待办 |
| GET | /api/schedule/list | 日程列表 |
| POST | /api/schedule | 新建日程 |
| GET | /api/notice/list | 公告列表 |
| POST | /api/notice | 发布公告 |
| GET | /api/mailbox/list | 邮箱列表 |
| POST | /api/mailbox/send | 发送邮件 |
| GET | /api/ding/list | DING 列表 |
| POST | /api/ding | 发送 DING |
| GET | /api/dashboard/overview | 看板概览 |
| GET | /api/dashboard/dept-distribution | 部门分布 |
| GET | /api/dashboard/message-trend | 消息趋势 |

---

## 八、改用 MySQL（可选）

1. `pom.xml` 把 H2 依赖换成 `mysql-connector-j`
2. `application.yml` 数据源改为 MySQL，并执行 `schema.sql` 建表
3. 去掉 `spring.sql.init` 配置（或保留首次建表）
4. `DataInitializer` 可改为只在表为空时插入种子数据

---

## 九、更多文档

详见 `document/` 目录下的模块设计文档

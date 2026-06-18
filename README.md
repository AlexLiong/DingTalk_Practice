# 仿钉钉企业协作管理系统 — 可运行脚手架

> Spring Boot 3 + MyBatis-Plus + Vue 3 + Element Plus
> 开箱即用：后端用 **H2 内存数据库**（免装 MySQL，启动自带种子数据），登录后可**实时聊天**（WebSocket）。

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
│       │   ├── config/      Security/WebSocket/MyBatis/种子数据
│       │   ├── security/    JWT 工具与过滤器
│       │   ├── entity/      实体
│       │   ├── mapper/      MyBatis-Plus Mapper
│       │   ├── dto/ vo/     传输/视图对象
│       │   ├── service/     业务逻辑
│       │   └── controller/  REST 接口
│       └── resources/
│           ├── application.yml
│           └── schema.sql   H2 建表脚本
└── frontend/                # Vue3 前端
    ├── package.json
    ├── vite.config.js       /api /ws 代理到后端 8080
    └── src/
        ├── api/             axios 封装 + 接口
        ├── store/           Pinia (user)
        ├── router/          路由 + 登录守卫
        ├── utils/ws.js      STOMP WebSocket 封装
        └── views/
            ├── Login.vue        登录/注册页
            └── Workbench.vue    钉钉三栏主界面(导航/会话/聊天)
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
# 或先打包再运行
# mvn clean package -DskipTests
# java -jar target/dingtalk-admin.jar
```

启动成功后：
- 后端地址：http://localhost:8080
- H2 数据库控制台：http://localhost:8080/h2-console
  （JDBC URL: `jdbc:h2:mem:dingtalk`，用户名 `sa`，密码空）

> 注意：H2 是内存库，**重启后数据重置**为种子数据。需要持久化请改 `application.yml` 换 MySQL。

### 2. 启动前端（端口 5173）

```bash
cd frontend
npm install
npm run dev
```

打开浏览器访问：http://localhost:5173

---

## 四、演示账号

| 账号 | 密码 | 角色 |
|------|------|------|
| admin | Boz@2026 | 管理员/技术部 |
| zhangsan | Boz@2026 | 产品经理/产品部 |
| lisi | Boz@2026 | UI设计师/设计部 |
| wangwu | Boz@2026 | 后端工程师/技术部 |

初始数据：1 个群聊「产品研发群」(含全部成员) + admin 与 zhangsan 的单聊，均带历史消息。

### 体验实时聊天
1. 浏览器 A 用 `admin` 登录
2. 浏览器 B（或无痕窗口）用 `zhangsan` 登录
3. 在任一端发消息，另一端**实时收到**（WebSocket 推送）
4. 「通讯录」点任意同事可发起新的单聊

---

## 五、核心功能

### 即时通讯 (IM)
- [x] 登录 / 注册（JWT 无状态认证 + BCrypt 加密）
- [x] 钉钉风格三栏布局（导航栏 + 会话列表 + 聊天窗口）
- [x] 会话列表（单聊/群聊、最后消息、时间、未读红点）
- [x] 实时消息收发（Spring WebSocket + STOMP，点对点推送）
- [x] 历史消息加载、未读清除、通讯录、发起单聊
- [x] 图片消息（上传、缩略图、点击大图预览）
- [x] 文件消息（上传、下载）
- [x] 消息撤回（本人 / 群主可撤回）
- [x] @提醒（群聊内 @ 成员，高亮显示）

### 群聊管理
- [x] 建群（多选成员、自定义群名）
- [x] 拉人 / 踢人（群主权限）
- [x] 群成员列表（群主标记）、群公告、退群

### 后台管理 (RBAC)
- [x] 用户管理（分页、搜索、CRUD、分配角色、重置密码、按部门筛选）
- [x] 角色管理（CRUD、分配权限树）
- [x] 菜单管理（目录/菜单/按钮三级、权限标识树形维护）
- [x] 组织架构（多级部门树、CRUD）
- [x] 按钮级权限控制（前端 v-if + 后端 @PreAuthorize 双重校验）
- [x] 动态菜单（登录后按权限渲染后台侧边栏）

### 个人中心
- [x] 修改资料（姓名/手机/邮箱/性别）、上传头像、修改密码

## 六、权限说明
- `admin` 是「超级管理员」，可见「管理后台」入口，拥有全部 17 项权限
- `zhangsan` / `lisi` / `wangwu` 是「普通员工」，只有协作功能，无后台入口
- 普通员工即使直接调后台接口也会被 `@PreAuthorize` 拦截（返回「无操作权限」）

## 七、可扩展方向（继续加分）
- 工作台应用宫格、通知公告、待办任务、审批流、数据看板(ECharts)
- 在线状态、消息已读回执、表情包、消息引用
- 换 MySQL + Redis（未读数/在线状态加速）

---

## 七、关键接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/login | 登录 |
| POST | /api/auth/register | 注册 |
| GET | /api/auth/info | 当前用户 |
| GET | /api/users | 通讯录 |
| GET | /api/chat/sessions | 我的会话列表 |
| GET | /api/chat/messages?sessionId= | 历史消息 |
| POST | /api/chat/messages | 发消息 |
| POST | /api/chat/read?sessionId= | 标记已读 |
| POST | /api/chat/single?targetUserId= | 发起单聊 |
| WS | /ws → /user/queue/message | 实时消息订阅 |

---

## 八、改用 MySQL（可选）

1. `pom.xml` 把 H2 依赖换成 `mysql-connector-j`
2. `application.yml` 数据源改为 MySQL，并执行 `schema.sql` 建表
3. 去掉 `spring.sql.init` 配置（或保留首次建表）
4. `DataInitializer` 可改为只在表为空时插入种子数据

---

> 本脚手架已通过编译、接口冒烟测试和 WebSocket 端到端联调验证。

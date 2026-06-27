# 认证与权限模块

## 一、模块概述

认证与权限模块是系统的安全基石，负责用户身份认证、JWT 令牌管理、权限校验等核心安全功能。采用 **JWT 无状态认证 + Spring Security + RBAC 权限模型** 的架构。

**模块职责**：
- 用户登录/注册认证
- JWT Token 生成与验证
- 请求权限过滤
- 权限标识加载与校验
- 密码加密存储

---

## 二、后端实现

### 2.1 核心类

| 类名 | 文件 | 职责 |
|------|------|------|
| `JwtUtil` | [JwtUtil.java](file:///e:/ProgramScript/java/DingTalk_Practice/backend/src/main/java/com/example/dingtalk/security/JwtUtil.java) | JWT 生成、解析、验证 |
| `JwtAuthenticationFilter` | [JwtAuthenticationFilter.java](file:///e:/ProgramScript/java/DingTalk_Practice/backend/src/main/java/com/example/dingtalk/security/JwtAuthenticationFilter.java) | JWT 认证过滤器 |
| `AuthService` | [AuthService.java](file:///e:/ProgramScript/java/DingTalk_Practice/backend/src/main/java/com/example/dingtalk/service/AuthService.java) | 认证业务逻辑 |
| `AuthController` | [AuthController.java](file:///e:/ProgramScript/java/DingTalk_Practice/backend/src/main/java/com/example/dingtalk/controller/AuthController.java) | 认证 REST 接口 |
| `SecurityConfig` | [SecurityConfig.java](file:///e:/ProgramScript/java/DingTalk_Practice/backend/src/main/java/com/example/dingtalk/config/SecurityConfig.java) | Spring Security 配置 |
| `PermissionService` | [PermissionService.java](file:///e:/ProgramScript/java/DingTalk_Practice/backend/src/main/java/com/example/dingtalk/service/PermissionService.java) | 权限/菜单查询服务 |

### 2.2 API 接口

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/auth/login` | 用户登录 | 公开 |
| POST | `/api/auth/register` | 用户注册 | 公开 |
| GET | `/api/auth/info` | 获取当前用户信息 | 已登录 |

#### 登录接口详情

**请求体**:
```json
{
  "username": "admin",
  "password": "Boz@2026"
}
```

**响应体**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "user": {
      "id": 1,
      "username": "admin",
      "nickname": "洪辰新",
      "avatar": null,
      "jobTitle": "技术总监",
      "deptName": "技术研发部"
    },
    "permissions": ["system:user:list", "system:user:add", "..."],
    "menus": [{ "id": 1, "name": "系统管理", "children": [...] }]
  }
}
```

### 2.3 登录流程

```
1. 用户提交用户名密码
   ↓
2. AuthController.login() 接收请求
   ↓
3. AuthService.login() 处理
   ├─ 根据用户名查询用户 (UserMapper)
   ├─ 校验账号状态（是否停用）
   ├─ BCrypt 密码比对验证
   ├─ 缓存在线状态 (OnlineService)
   ├─ 生成 JWT Token (JwtUtil.generate)
   ├─ 查询用户权限列表 (PermissionService.getPerms)
   └─ 查询用户菜单树 (PermissionService.getMenuTree)
   ↓
4. 返回完整登录信息（token + 用户 + 权限 + 菜单）
```

### 2.4 JWT 令牌结构

JWT Payload 包含：

| 字段 | 类型 | 说明 |
|------|------|------|
| `sub` | String | 用户名（subject） |
| `uid` | Long | 用户 ID（自定义 claim） |
| `iat` | Date | 签发时间 |
| `exp` | Date | 过期时间 |

**配置项** (application.yml):
| 配置 | 默认值 | 说明 |
|------|--------|------|
| `jwt.secret` | dingtalk-admin-demo-secret-key... | 签名密钥 |
| `jwt.expire` | 86400000 (24h) | 过期时间（毫秒） |

### 2.5 请求认证流程

```
HTTP 请求到达
   ↓
JwtAuthenticationFilter 过滤器
   ├─ 从 Authorization Header 提取 Token
   ├─ 格式校验: Bearer {token}
   ├─ JWT 有效性验证 (JwtUtil.valid)
   ├─ 解析用户ID和用户名
   ├─ 加载用户权限列表 (PermissionService.getPerms)
   ├─ 构建 UsernamePasswordAuthenticationToken
   │   ├─ principal = userId
   │   ├─ authorities = [ROLE_USER, 权限标识...]
   │   └─ details = username
   └─ 注入 SecurityContextHolder
   ↓
Controller 方法执行
   ├─ SecurityUtils.getUserId() 获取当前用户
   └─ @PreAuthorize 注解校验权限
   ↓
返回响应
```

### 2.6 权限控制机制

#### 双层权限控制

| 层级 | 实现方式 | 说明 |
|------|---------|------|
| 前端层 | `v-if="hasPerm('xxx')"` | 控制按钮/菜单显示 |
| 后端层 | `@PreAuthorize("hasAuthority('xxx')")` | 方法级权限校验 |

#### 权限标识命名规范

格式: `模块:功能:操作`

示例：
- `system:user:list` - 用户列表查看
- `system:user:add` - 新增用户
- `system:user:edit` - 编辑用户
- `system:user:remove` - 删除用户
- `system:user:resetPwd` - 重置密码
- `system:role:list` - 角色列表查看
- `system:menu:list` - 菜单列表查看
- `system:dept:list` - 部门列表查看
- `notice:list` - 公告列表查看
- `dashboard:view` - 数据看板查看

### 2.7 安全配置

**SecurityConfig** 核心配置：

| 配置项 | 值 | 说明 |
|-------|----|------|
| CSRF | 禁用 | REST API 无状态 |
| 会话策略 | STATELESS | 无状态会话 |
| CORS | 启用 | 允许所有来源/方法/头 |
| 方法安全 | @EnableMethodSecurity | 启用 @PreAuthorize |
| 密码加密 | BCryptPasswordEncoder | BCrypt 强哈希 |

**放行路径**:
- `/api/auth/login`, `/api/auth/register` - 认证接口
- `/ws/**` - WebSocket 端点
- `/h2-console/**` - H2 控制台
- 前端静态资源与 SPA 路由

### 2.8 密码加密

- **算法**: BCrypt 强哈希算法
- **加密**: `passwordEncoder.encode(rawPassword)` - 注册/修改密码时
- **验证**: `passwordEncoder.matches(rawPassword, encodedPassword)` - 登录时校验
- **特点**: 自带盐值，计算成本可调，安全性高

---

## 三、前端实现

### 3.1 核心文件

| 文件 | 职责 |
|------|------|
| [user.js](file:///e:/ProgramScript/java/DingTalk_Practice/frontend/src/store/user.js) | Pinia 用户状态管理 |
| [request.js](file:///e:/ProgramScript/java/DingTalk_Practice/frontend/src/api/request.js) | Axios 请求拦截器 |
| [index.js](file:///e:/ProgramScript/java/DingTalk_Practice/frontend/src/api/index.js) | API 接口定义 |
| [index.js](file:///e:/ProgramScript/java/DingTalk_Practice/frontend/src/router/index.js) | 路由守卫 |

### 3.2 用户状态管理 (Pinia)

**Store: useUserStore**

| State | 类型 | 说明 |
|-------|------|------|
| `token` | String | JWT Token（持久化到 localStorage） |
| `user` | Object | 当前用户信息 |
| `permissions` | Array | 权限标识列表 |
| `menus` | Array | 菜单树 |

| Actions | 说明 |
|---------|------|
| `login(form)` | 登录，获取 token 和用户信息 |
| `fetchInfo()` | 获取当前用户信息 |
| `hasPerm(code)` | 判断是否有指定权限 |
| `setUser(u)` | 设置用户信息 |
| `logout()` | 退出登录，清除状态 |

| Getter | 说明 |
|--------|------|
| `isAdmin` | 是否为管理员（权限列表非空） |

### 3.3 请求拦截器

Axios 请求拦截器自动注入 Token：

```
请求发出前
   ↓
从 localStorage 获取 token
   ↓
存在则设置 Header: Authorization: Bearer {token}
   ↓
继续请求
```

### 3.4 响应拦截器

| 场景 | 处理逻辑 |
|------|---------|
| code === 200 | 直接返回 data 字段 |
| code !== 200 | ElMessage.error 提示，返回 reject |
| HTTP 401/403 | 提示登录过期，清除 token，跳转登录页 |
| 其他错误 | 提示网络错误 |

### 3.5 路由守卫

全局前置守卫 `beforeEach`：

```
路由跳转前
   ↓
检查目标路由 meta.auth
   ↓
需要认证?
   ├─ 是 → 检查 localStorage.token
   │       ├─ 有 token → 放行
   │       └─ 无 token → 跳转到 /login
   └─ 否 → 是登录页且已登录?
           ├─ 是 → 跳转到 /chat
           └─ 否 → 放行
```

### 3.6 权限判断使用

```javascript
// Composition API 中
const userStore = useUserStore()
if (userStore.hasPerm('system:user:add')) {
  // 有权限
}

// 模板中
<button v-if="hasPerm('system:user:add')">新增用户</button>
```

---

## 四、数据模型

### 4.1 相关数据表

| 表名 | 说明 |
|------|------|
| `sys_user` | 用户表（存储用户信息和密码） |
| `sys_role` | 角色表 |
| `sys_menu` | 菜单/权限表 |
| `sys_user_role` | 用户-角色关联表 |
| `sys_role_menu` | 角色-菜单关联表 |

### 4.2 权限计算流程

```
用户ID
   ↓
查询用户角色 (sys_user_role)
   ↓
查询角色菜单 (sys_role_menu)
   ↓
去重得到菜单ID集合
   ↓
查询菜单详情 → 提取权限标识 (perms 字段)
   → 构建菜单树（过滤出目录和菜单）
```

---

## 五、模块依赖关系

```
认证与权限模块
   ├── 依赖模块
   │   ├── 用户管理模块（用户数据）
   │   └── RBAC 管理模块（角色/菜单数据）
   ├── 被依赖模块
   │   ├── 所有业务模块（身份识别）
   │   └── 管理后台模块（权限校验）
   └── 核心依赖
       ├── Spring Security
       ├── JJWT 0.12.x
       └── BCryptPasswordEncoder
```

---

## 六、设计要点

### 6.1 无状态认证

- 使用 JWT 实现完全无状态认证
- 服务端不存储 Session，便于水平扩展
- Token 中包含用户身份信息

### 6.2 双重安全保障

- **显示层控制**: 前端 v-if 控制按钮和菜单显示
- **接口层控制**: 后端 @PreAuthorize 真正拦截无权限请求
- 前端仅做 UX 优化，后端才是安全底线

### 6.3 权限预加载

- 每次请求都重新从数据库加载权限（确保实时性）
- 权限注入到 SecurityContext 供 @PreAuthorize 使用
- 可优化为缓存策略（如 Redis 缓存权限）

### 6.4 密码安全

- 使用 BCrypt 算法，自带盐值
- 密码从不在响应中返回
- 数据库中只存哈希值，不可逆

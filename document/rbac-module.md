# RBAC 管理模块

## 一、模块概述

RBAC（基于角色的访问控制）管理模块是系统的权限管理核心，提供用户管理、角色管理、菜单管理、部门管理四大功能，构成完整的企业组织架构与权限体系。

**模块职责**：
- 用户管理（用户 CRUD、角色分配、重置密码）
- 角色管理（角色 CRUD、权限分配）
- 菜单管理（菜单/目录/按钮的树形管理）
- 部门管理（组织架构树形管理）
- 数据看板（系统数据统计）

---

## 二、功能架构

```
RBAC 管理模块
   ├── 用户管理
   │   ├── 用户列表（分页、筛选、部门过滤）
   │   ├── 新增用户
   │   ├── 编辑用户
   │   ├── 删除用户
   │   ├── 分配角色
   │   └── 重置密码
   ├── 角色管理
   │   ├── 角色列表（分页）
   │   ├── 新增角色
   │   ├── 编辑角色
   │   ├── 删除角色
   │   └── 分配菜单权限
   ├── 菜单管理
   │   ├── 菜单树
   │   ├── 新增目录/菜单/按钮
   │   ├── 编辑菜单
   │   └── 删除菜单
   ├── 部门管理
   │   ├── 部门树
   │   ├── 新增部门
   │   ├── 编辑部门
   │   └── 删除部门
   └── 数据看板
       ├── 概览统计
       ├── 部门分布图
       └── 消息趋势图
```

---

## 三、后端实现

### 3.1 核心服务

| 服务类 | 说明 | 文件 |
|--------|------|------|
| `UserAdminService` | 用户管理服务 | [UserAdminService.java](file:///e:/ProgramScript/java/DingTalk_Practice/backend/src/main/java/com/example/dingtalk/service/UserAdminService.java) |
| `RoleService` | 角色管理服务 | [RoleService.java](file:///e:/ProgramScript/java/DingTalk_Practice/backend/src/main/java/com/example/dingtalk/service/RoleService.java) |
| `MenuService` | 菜单管理服务 | - |
| `DeptService` | 部门管理服务 | [DeptService.java](file:///e:/ProgramScript/java/DingTalk_Practice/backend/src/main/java/com/example/dingtalk/service/DeptService.java) |
| `PermissionService` | 权限查询服务 | [PermissionService.java](file:///e:/ProgramScript/java/DingTalk_Practice/backend/src/main/java/com/example/dingtalk/service/PermissionService.java) |
| `DashboardService` | 数据看板服务 | - |

### 3.2 核心控制器

| 控制器 | 路径前缀 | 说明 |
|--------|---------|------|
| `UserAdminController` | `/api/admin/user` | 用户管理接口 |
| `RoleController` | `/api/role` | 角色管理接口 |
| `MenuController` | `/api/menu` | 菜单管理接口 |
| `DeptController` | `/api/dept` | 部门管理接口 |
| `DashboardController` | `/api/dashboard` | 数据看板接口 |

### 3.3 API 接口

#### 用户管理

| 方法 | 路径 | 说明 | 权限标识 |
|------|------|------|---------|
| GET | `/api/admin/user/page` | 用户分页列表 | system:user:list |
| GET | `/api/admin/user/{id}/roles` | 获取用户角色 | system:user:list |
| POST | `/api/admin/user` | 新增用户 | system:user:add |
| PUT | `/api/admin/user` | 编辑用户 | system:user:edit |
| DELETE | `/api/admin/user/{id}` | 删除用户 | system:user:remove |
| PUT | `/api/admin/user/{id}/password` | 重置密码 | system:user:resetPwd |

#### 角色管理

| 方法 | 路径 | 说明 | 权限标识 |
|------|------|------|---------|
| GET | `/api/role/page` | 角色分页列表 | system:role:list |
| GET | `/api/role/all` | 所有角色列表 | system:role:list |
| GET | `/api/role/{id}/menus` | 获取角色菜单 | system:role:list |
| POST | `/api/role` | 新增角色 | system:role:add |
| PUT | `/api/role` | 编辑角色 | system:role:edit |
| DELETE | `/api/role/{id}` | 删除角色 | system:role:remove |

#### 菜单管理

| 方法 | 路径 | 说明 | 权限标识 |
|------|------|------|---------|
| GET | `/api/menu/tree` | 菜单树 | system:menu:list |
| POST | `/api/menu` | 新增菜单 | system:menu:add |
| PUT | `/api/menu` | 编辑菜单 | system:menu:edit |
| DELETE | `/api/menu/{id}` | 删除菜单 | system:menu:remove |

#### 部门管理

| 方法 | 路径 | 说明 | 权限标识 |
|------|------|------|---------|
| GET | `/api/dept/tree` | 部门树 | system:dept:list |
| POST | `/api/dept` | 新增部门 | system:dept:add |
| PUT | `/api/dept` | 编辑部门 | system:dept:edit |
| DELETE | `/api/dept/{id}` | 删除部门 | system:dept:remove |

#### 数据看板

| 方法 | 路径 | 说明 | 权限标识 |
|------|------|------|---------|
| GET | `/api/dashboard/overview` | 概览统计 | dashboard:view |
| GET | `/api/dashboard/dept-distribution` | 部门分布 | dashboard:view |
| GET | `/api/dashboard/message-trend` | 消息趋势 | dashboard:view |

---

## 四、前端实现

### 4.1 页面组件

| 页面 | 文件 | 路由 | 说明 |
|------|------|------|------|
| 管理后台布局 | [AdminLayout.vue](file:///e:/ProgramScript/java/DingTalk_Practice/frontend/src/layout/AdminLayout.vue) | /admin | 侧边栏 + 主内容区 |
| 用户管理 | [UserManage.vue](file:///e:/ProgramScript/java/DingTalk_Practice/frontend/src/views/admin/UserManage.vue) | /admin/user | 用户增删改查 |
| 角色管理 | [RoleManage.vue](file:///e:/ProgramScript/java/DingTalk_Practice/frontend/src/views/admin/RoleManage.vue) | /admin/role | 角色增删改查 |
| 菜单管理 | [MenuManage.vue](file:///e:/ProgramScript/java/DingTalk_Practice/frontend/src/views/admin/MenuManage.vue) | /admin/menu | 菜单树管理 |
| 部门管理 | [DeptManage.vue](file:///e:/ProgramScript/java/DingTalk_Practice/frontend/src/views/admin/DeptManage.vue) | /admin/dept | 部门树管理 |
| 数据看板 | [Dashboard.vue](file:///e:/ProgramScript/java/DingTalk_Practice/frontend/src/views/admin/Dashboard.vue) | /admin/dashboard | ECharts 数据图表 |
| 公告管理 | [NoticeManage.vue](file:///e:/ProgramScript/java/DingTalk_Practice/frontend/src/views/admin/NoticeManage.vue) | /admin/notice | 公告管理 |

### 4.2 管理后台布局

```
┌──────────────────────────────────────────┐
│  Header:  logo | 菜单展开 | 用户信息    │
├──────────┬───────────────────────────────┤
│          │                               │
│ Sidebar  │   Main Content (router-view)  │
│ 菜单树   │                               │
│          │                               │
└──────────┴───────────────────────────────┘
```

**动态菜单渲染**：
- 菜单数据来自 userStore.menus（登录时获取）
- 根据权限动态渲染可见菜单
- 支持多级菜单嵌套（目录 → 菜单）
- 按钮级权限用 `v-if="hasPerm()"` 控制

### 4.3 用户管理页面

功能列表：
- **表格展示**: 头像、用户名、姓名、部门、职位、手机号、状态、操作
- **筛选条件**: 用户名搜索、部门选择、状态筛选
- **新增用户**: 表单弹窗，填写基本信息 + 分配角色
- **编辑用户**: 表单弹窗，修改信息 + 调整角色
- **删除用户**: 二次确认，删除用户及关联数据
- **重置密码**: 重置为默认密码

### 4.4 角色管理页面

功能列表：
- **表格展示**: 角色名称、角色标识、排序、状态、操作
- **新增角色**: 名称、标识、排序、状态、备注
- **分配权限**: 树形组件选择菜单权限（支持半选状态）
- **删除角色**: 二次确认

### 4.5 菜单管理页面

功能列表：
- **树状表格**: 层级展示菜单结构
- **新增**: 可选择新增目录/菜单/按钮
- **编辑**: 修改菜单名称、路由、图标、权限标识等
- **删除**: 有子菜单时不可删除

#### 菜单类型

| 类型 | 说明 | 必填字段 |
|------|------|---------|
| 目录 | 一级导航，包含子菜单 | 名称、排序、图标 |
| 菜单 | 页面级菜单，有路由 | 名称、路由路径、权限标识 |
| 按钮 | 按钮级权限，仅控制 | 名称、权限标识 |

### 4.6 部门管理页面

功能列表：
- **树状表格**: 层级展示组织架构
- **新增子部门**: 在指定部门下新增
- **编辑**: 修改部门名称、负责人、排序等
- **删除**: 有子部门时不可删除

### 4.7 数据看板页面

使用 ECharts 实现数据可视化：

| 图表 | 类型 | 说明 |
|------|------|------|
| 概览统计 | 数据卡片 | 用户数、部门数、消息数、待办数 |
| 部门分布 | 饼图 | 各部门人数占比 |
| 消息趋势 | 折线图 | 近 7 天消息数量趋势 |

---

## 五、数据模型

### 5.1 核心数据表

| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `sys_user` | 用户表 | id, username, password, nickname, dept_id, status |
| `sys_role` | 角色表 | id, name, role_key, sort, status |
| `sys_menu` | 菜单表 | id, parent_id, name, type, path, icon, perms, sort |
| `sys_dept` | 部门表 | id, parent_id, name, ancestors, sort, leader, status |
| `sys_user_role` | 用户-角色关联 | user_id, role_id |
| `sys_role_menu` | 角色-菜单关联 | role_id, menu_id |

### 5.2 RBAC 关系图

```
sys_user ──< sys_user_role >── sys_role ──< sys_role_menu >── sys_menu
    │
    └── 归属 ──> sys_dept (树形结构)
```

### 5.3 树形结构设计

菜单和部门都采用父子树形结构：
- `parent_id` 指向父节点，0 表示顶级
- 通过代码递归构建树形结构
- 排序字段 `sort` 控制同级顺序

---

## 六、权限体系

### 6.1 权限分层

```
用户 (sys_user)
   │
   ├── 拥有多个角色 (sys_user_role)
   │
   ▼
角色 (sys_role)
   │
   ├── 拥有多个菜单/权限 (sys_role_menu)
   │
   ▼
菜单 (sys_menu)
   ├── 目录 (type=1) - 仅导航
   ├── 菜单 (type=2) - 页面级
   └── 按钮 (type=3) - 操作级
       └── perms 字段存储权限标识
```

### 6.2 权限标识规范

格式: `模块:功能:操作`

**系统管理模块**:
- `system:user:list/add/edit/remove/resetPwd`
- `system:role:list/add/edit/remove`
- `system:menu:list/add/edit/remove`
- `system:dept:list/add/edit/remove`

**其他模块**:
- `notice:list` - 公告查看
- `dashboard:view` - 看板查看

### 6.3 权限校验流程

```
用户请求接口
   ↓
JwtAuthenticationFilter 加载权限
   ├─ 查询用户角色
   ├─ 查询角色菜单
   ├─ 提取权限标识集合
   └─ 注入 SecurityContext
   ↓
@PreAuthorize("hasAuthority('xxx')") 校验
   ↓
通过 → 执行业务逻辑
不通过 → 抛出 AccessDeniedException → 403
```

---

## 七、模块依赖关系

```
RBAC 管理模块
   ├── 依赖模块
   │   └── 认证与权限模块（身份识别、权限校验）
   ├── 被依赖模块
   │   ├── 认证与权限模块（菜单/权限数据）
   │   ├── 即时通讯模块（用户信息）
   │   └── 所有业务模块（用户、部门信息）
   └── 管理入口
       └── 管理后台 (AdminLayout)
```

---

## 八、设计要点

### 8.1 RBAC 模型优势

- **灵活性**: 通过角色间接分配权限，用户只需调整角色
- **可扩展性**: 新增角色/菜单不影响现有体系
- **易维护**: 权限集中管理，便于审计

### 8.2 树形数据处理

- 数据库扁平存储，代码构建树
- 递归构建支持无限层级
- 半选状态处理（分配权限时）
- 删除前校验子节点存在性

### 8.3 前后端权限双控

- **前端**: 菜单过滤 + 按钮隐藏（提升 UX）
- **后端**: 接口级别 @PreAuthorize 校验（安全底线）
- 权限标识前后端一致，一一对应

### 8.4 种子数据

`DataInitializer` 初始化完整 RBAC 体系：
- 6 级部门架构
- 17+ 个菜单（目录/菜单/按钮）
- 2 个角色（超级管理员、普通员工）
- 6 个演示用户（1 管理员 + 5 员工）

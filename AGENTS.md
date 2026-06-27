# 项目：仿钉钉企业协作管理系统

## 技术栈
- **后端**: Spring Boot 3 + MyBatis-Plus + H2(可切换MySQL) + JWT + WebSocket(STOMP)
- **前端**: Vue 3 + Element Plus + Pinia + Vite
- **认证**: JWT 无状态 + BCrypt 加密

## 目录结构
```
backend/src/main/java/com/example/dingtalk/
  ├── common/          Result, BizException, GlobalExceptionHandler, SecurityUtils
  ├── config/          SecurityConfig, WebSocketConfig, DataInitializer(种子数据)
  ├── security/        JwtUtil, JwtAuthenticationFilter
  ├── entity/          SysUser, SysRole, SysMenu, SysDept + 聊天/文件/日程等
  ├── mapper/          MyBatis-Plus Mapper 接口
  ├── service/         业务逻辑层
  ├── controller/      REST 控制器
  ├── dto/             数据传输对象
  └── vo/              视图对象
frontend/src/
  ├── api/             Axios 封装 + 接口定义(index.js)
  ├── store/           Pinia(user.js)
  ├── router/          路由 + 登录守卫
  ├── layout/          AdminLayout.vue(管理后台布局)
  ├── components/      AppSideNav.vue(协作导航)
  └── views/           页面组件
```

## 权限管理模块 (RBAC)
- 6 张核心表：sys_user, sys_role, sys_menu, sys_dept, sys_user_role, sys_role_menu
- 权限标识命名：`模块:功能:操作`（如 system:user:add）
- 后端双层防护：JWT 过滤器注入权限 + `@PreAuthorize("hasAuthority('xxx')")`
- 前端双层控制：`v-if="hasPerm('xxx')"` 控制按钮 + 登录下发动态菜单树
- 菜单三级结构：目录(1) → 菜单(2) → 按钮(3)
- 管理页面 6 个：Dashboard / UserManage / RoleManage / MenuManage / DeptManage / NoticeManage
- 通知管理(Notice)未加 @PreAuthorize，仅靠管理员身份(v-if="isAdmin")控制
- 数据看板(Dashboard)未加 @PreAuthorize，任何已登录用户可访问

## 关键约定
- **修改业务代码后必须更新 CLAUDE.md**
- 后端异常统一由 `GlobalExceptionHandler` 处理，返回 `Result`
- 前端 API 统一通过 `frontend/src/api/index.js` 调用，自动附加 Bearer Token
- 路由守卫只检查 token（`meta.auth`），不做路由级权限过滤
- 所有管理员只有 `admin`(洪辰新)，其他人是普通员工

## 演示账号
| 账号 | 密码 | 角色 |
|------|------|------|
| admin | Boz@2026 | 超级管理员 |
| chenyuqi / zhouleheng / liuyulin / liangqinwei / jiangzezhi | Boz@2026 | 普通员工 |

## 启动方式
```bash
cd backend && mvn spring-boot:run    # 后端 :8080
cd frontend && npm run dev           # 前端 :5173
```

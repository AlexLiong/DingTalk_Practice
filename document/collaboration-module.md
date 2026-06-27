# 企业协作模块

## 一、模块概述

企业协作模块提供企业日常办公所需的核心协作工具，包括文件管理、审批流程、待办任务、日程安排四大功能，支撑团队高效协同工作。

**模块职责**：
- 文件管理（上传、下载、共享、版本、回收）
- 审批流程（请假、报销等审批申请与处理）
- 待办任务（任务创建、分配、状态跟踪）
- 日程管理（日历视图、日程安排）
---

## 二、文件管理子模块

### 2.1 功能说明

企业云盘功能，支持文件上传、下载、共享、锁定、回收站等全生命周期管理。

### 2.2 后端实现

**核心服务**: `FileService`

**核心实体**: `SysFile` - 文件实体

**数据表**: `sys_file`

#### API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/file/upload` | 文件上传 |
| GET | `/api/file/list` | 文件列表 |
| PUT | `/api/file/{id}` | 更新文件信息 |
| DELETE | `/api/file/{id}` | 删除文件 |
| POST | `/api/file/{id}/share` | 分享文件 |
| POST | `/api/file/{id}/lock` | 锁定/解锁文件 |
| POST | `/api/file/{id}/recycle` | 回收/恢复文件 |

#### 文件上传配置

| 配置项 | 值 | 说明 |
|-------|----|------|
| `file.upload-dir` | ./uploads | 上传目录 |
| `max-file-size` | 100MB | 单文件最大 |
| `max-request-size` | 100MB | 请求最大 |

#### 文件字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| name | String | 文件名 |
| url | String | 文件访问 URL |
| size | Long | 文件大小（字节） |
| type | String | 文件类型 |
| uploaderId | Long | 上传者 ID |
| scene | String | 场景（chat/文档/...） |
| description | String | 描述 |
| status | String | 状态（normal/recycled） |
| shareCount | Integer | 分享次数 |
| historyJson | String | 历史版本 JSON |

### 2.3 前端实现

**页面**: [Documents.vue](file:///e:/ProgramScript/java/DingTalk_Practice/frontend/src/views/Documents.vue)

**路径**: `/documents`

#### 页面功能

- **文件列表**: 表格展示，支持排序、筛选
- **文件上传**: 拖拽上传 + 点击上传，进度条
- **文件操作**: 下载、重命名、删除、分享
- **文件预览**: 图片预览、文档预览
- **文件夹**: 目录树导航
- **回收站**: 已删除文件，可恢复/彻底删除
- **我的分享**: 我分享的文件列表

---

## 三、审批流子模块

### 3.1 功能说明

提供企业内部审批流程，支持请假、报销等常见审批类型，包含发起审批、审批处理、审批历史等功能。

### 3.2 后端实现

**核心服务**: `ApprovalService`

**核心实体**: `SysApproval` - 审批实体

**数据表**: `sys_approval`

#### API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/approval/list` | 审批列表（全部） |
| GET | `/api/approval/pending` | 待我审批 |
| GET | `/api/approval/applied` | 我的申请 |
| POST | `/api/approval` | 发起审批 |
| PUT | `/api/approval/{id}` | 审批通过/驳回 |

#### 审批类型

| 类型值 | 类型名称 | 说明 |
|--------|---------|------|
| leave | 请假审批 | 包含请假类型、天数、原因等 |
| expense | 报销审批 | 包含报销金额、明细、事由等 |

#### 审批状态

| 状态值 | 状态名称 | 说明 |
|--------|---------|------|
| 0 | 待审批 | 提交后等待审批 |
| 1 | 已通过 | 审批通过 |
| 2 | 已驳回 | 审批驳回 |

#### 审批字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| type | String | 审批类型（leave/expense） |
| title | String | 审批标题 |
| content | String | 审批内容（JSON 格式） |
| applicantId | Long | 申请人 ID |
| approverId | Long | 审批人 ID |
| status | Integer | 状态（0待批 1通过 2驳回） |
| remark | String | 审批备注 |
| createTime | LocalDateTime | 申请时间 |
| approveTime | LocalDateTime | 审批时间 |

### 3.3 前端实现

**页面**: [Approval.vue](file:///e:/ProgramScript/java/DingTalk_Practice/frontend/src/views/Approval.vue)

**路径**: `/approval`

#### 页面功能

- **Tab 切换**: 我发起的 / 待我审批 / 全部
- **发起审批**: 选择类型（请假/报销），填写表单
- **审批详情**: 查看审批内容、审批状态
- **审批操作**: 通过 / 驳回（需填写备注）
- **审批列表**: 按状态筛选，时间倒序

---

## 四、待办任务子模块

### 4.1 功能说明

个人和团队任务管理工具，支持任务创建、分配、优先级设置、状态跟踪等功能。

### 4.2 后端实现

**核心服务**: `TodoService`

**核心实体**: `SysTodo` - 待办实体

**数据表**: `sys_todo`

#### API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/todo/list` | 待办列表 |
| GET | `/api/todo/stats` | 待办统计 |
| POST | `/api/todo` | 新增待办 |
| PUT | `/api/todo` | 编辑待办 |
| PUT | `/api/todo/{id}/status` | 更新状态 |
| DELETE | `/api/todo/{id}` | 删除待办 |

#### 优先级

| 优先级值 | 名称 | 颜色 |
|---------|------|------|
| 1 | 高 | 红色 |
| 2 | 中 | 橙色 |
| 3 | 低 | 蓝色 |

#### 任务状态

| 状态值 | 名称 | 说明 |
|--------|------|------|
| 0 | 待办 | 未开始 |
| 1 | 进行中 | 正在处理 |
| 2 | 已完成 | 已完成 |

#### 待办字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| title | String | 标题 |
| content | String | 内容描述 |
| creatorId | Long | 创建者 ID |
| assigneeId | Long | 负责人 ID |
| priority | Integer | 优先级（1高 2中 3低） |
| status | Integer | 状态（0待办 1进行中 2完成） |
| dueTime | LocalDateTime | 截止时间 |
| createTime | LocalDateTime | 创建时间 |

### 4.3 前端实现

**页面**: [Todo.vue](file:///e:/ProgramScript/java/DingTalk_Practice/frontend/src/views/Todo.vue)

**路径**: `/todo`

#### 页面功能

- **统计卡片**: 待办数、进行中、已完成、逾期
- **任务列表**: 按状态/优先级筛选
- **新增任务**: 标题、描述、负责人、优先级、截止时间
- **状态切换**: 点击切换待办→进行中→完成
- **编辑/删除**: 修改任务信息或删除
- **到期提醒**: 即将到期任务高亮显示

---

## 五、日程管理子模块

### 5.1 功能说明

个人日程安排管理，提供日历视图，支持日程的创建、编辑、删除和提醒。

### 5.2 后端实现

**核心服务**: -

**核心实体**: `SysSchedule` - 日程实体

**数据表**: `sys_schedule`

#### API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/schedule/list` | 日程列表 |
| POST | `/api/schedule` | 新增日程 |
| PUT | `/api/schedule` | 编辑日程 |
| DELETE | `/api/schedule/{id}` | 删除日程 |

#### 日程字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| title | String | 标题 |
| content | String | 内容 |
| userId | Long | 用户 ID |
| startTime | LocalDateTime | 开始时间 |
| endTime | LocalDateTime | 结束时间 |
| allDay | Integer | 是否全天（0否 1是） |
| color | String | 颜色标记 |
| createTime | LocalDateTime | 创建时间 |

### 5.3 前端实现

**页面**: [Calendar.vue](file:///e:/ProgramScript/java/DingTalk_Practice/frontend/src/views/Calendar.vue)

**路径**: `/calendar`

#### 页面功能

- **日历视图**: 月视图 / 周视图 / 日视图
- **新增日程**: 点击日期添加，填写标题、时间、颜色
- **编辑日程**: 拖拽调整时间，点击编辑详情
- **日程颜色**: 不同颜色分类标识
- **全天日程**: 顶部全天区域显示
- **日程列表**: 侧边栏展示今日/本周日程

---

## 六、数据模型

### 6.1 核心数据表汇总

| 子模块 | 表名 | 说明 |
|--------|------|------|
| 文件管理 | `sys_file` | 文件信息表 |
| 审批流 | `sys_approval` | 审批表 |
| 待办任务 | `sys_todo` | 待办表 |
| 日程管理 | `sys_schedule` | 日程表 |

### 6.2 ER 关系

```
sys_user ──┬──> sys_file (上传者)
           ├──> sys_approval (申请人/审批人)
           ├──> sys_todo (创建者/负责人)
           └──> sys_schedule (个人日程)
```

---

## 七、模块依赖关系

```
企业协作模块
   ├── 子模块
   │   ├── 文件管理子模块
   │   ├── 审批流子模块
   │   ├── 待办任务子模块
   │   └── 日程管理子模块
   ├── 依赖模块
   │   ├── 认证与权限模块（身份识别）
   │   ├── RBAC 管理模块（用户/部门信息）
   │   └── 即时通讯模块（文件消息、审批通知）
   └── 入口
       └── 工作台侧边导航
```

---

## 八、设计要点

### 8.1 模块化设计

- 每个子功能独立数据表和服务
- 通过用户 ID 关联，各模块互不耦合
- 可单独扩展，不影响其他模块

### 8.2 文件存储策略

- 本地文件系统存储（可扩展为 OSS）
- 数据库存储文件元信息
- URL 访问模式，静态资源映射
- 支持不同场景分类（chat/文档/头像等）

### 8.3 审批流简化设计

- 采用单级审批（申请人 → 审批人）
- 内容字段用 JSON 存储，支持不同审批类型的差异化字段
- 状态简单清晰：待批 / 通过 / 驳回
- 可扩展为多级审批流程

### 8.4 任务管理

- 支持创建者和负责人两个角色
- 三级优先级 + 三级状态
- 截止时间 + 逾期提醒
- 可扩展为更复杂的项目管理

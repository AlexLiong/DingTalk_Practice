# 通知与协同模块

## 一、模块概述

通知与协同模块提供企业内部信息传达和协同工具，包括通知公告、企业邮箱、DING 紧急消息、消息收藏四大功能，确保重要信息高效触达和管理。

**模块职责**：
- 通知公告（发布、浏览、阅读状态）
- 企业邮箱（收发、星标、归档、草稿）
- DING 消息（紧急提醒、确认、追踪）
- 消息收藏（重要消息收藏管理）

---

## 二、通知公告子模块

### 2.1 功能说明

企业内部通知公告发布平台，支持管理员发布通知/公告，员工查看和阅读确认。

### 2.2 后端实现

**核心服务**: `NoticeService`

**核心实体**: `SysNotice` - 公告实体、`SysNoticeRead` - 阅读记录实体

**数据表**: `sys_notice`, `sys_notice_read`

#### API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/notice/list` | 公告列表（员工端） |
| GET | `/api/notice/{id}` | 公告详情 |
| GET | `/api/notice/admin/list` | 公告列表（管理端） |
| POST | `/api/notice` | 发布公告 |
| PUT | `/api/notice` | 编辑公告 |
| DELETE | `/api/notice/{id}` | 删除公告 |

#### 公告类型

| 类型值 | 类型名称 | 说明 |
|--------|---------|------|
| 1 | 通知 | 一般通知 |
| 2 | 公告 | 重要公告 |

#### 优先级

| 优先级值 | 名称 | 说明 |
|---------|------|------|
| 1 | 紧急 | 红色高亮，置顶 |
| 2 | 普通 | 正常显示 |

#### 公告字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| title | String | 标题 |
| content | String | 内容（富文本） |
| type | Integer | 类型（1通知 2公告） |
| priority | Integer | 优先级（1紧急 2普通） |
| publisherId | Long | 发布者 ID |
| status | Integer | 状态（0草稿 1已发布） |
| createTime | LocalDateTime | 创建时间 |

#### 阅读记录

`sys_notice_read` 表记录每个用户的公告阅读情况：
- `notice_id` - 公告 ID
- `user_id` - 用户 ID
- `read_time` - 阅读时间

### 2.3 前端实现

**页面**: [Notice.vue](file:///e:/ProgramScript/java/DingTalk_Practice/frontend/src/views/Notice.vue)

**路径**: `/notice`

**管理端页面**: [NoticeManage.vue](file:///e:/ProgramScript/java/DingTalk_Practice/frontend/src/views/admin/NoticeManage.vue)

**管理端路径**: `/admin/notice`

#### 员工端功能

- **公告列表**: 按时间倒序，紧急公告置顶
- **公告详情**: 查看完整内容，自动标记已读
- **未读标识**: 未读公告显示红点
- **筛选**: 按类型/优先级筛选

#### 管理端功能

- **公告管理**: 列表、新增、编辑、删除
- **发布状态**: 草稿 / 已发布
- **阅读统计**: 查看已读/未读人数

---

## 三、企业邮箱子模块

### 3.1 功能说明

模拟企业内部邮件系统，支持邮件收发、星标、归档、草稿、回复等功能。

### 3.2 后端实现

**核心服务**: `MailboxService`

**核心实体**: `SysMailboxMail` - 邮件实体

**数据表**: `sys_mailbox_mail`

#### API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/mailbox/list` | 邮件列表 |
| POST | `/api/mailbox/reset` | 重置邮件数据 |
| POST | `/api/mailbox/send` | 发送邮件 |
| POST | `/api/mailbox/{id}/open` | 打开邮件（标记已读） |
| PUT | `/api/mailbox/{id}/read` | 更新已读状态 |
| PUT | `/api/mailbox/{id}/star` | 更新星标状态 |
| PUT | `/api/mailbox/{id}/archive` | 归档/取消归档 |
| PUT | `/api/mailbox/{id}/draft` | 保存草稿 |
| POST | `/api/mailbox/{id}/reply` | 回复邮件 |

#### 邮件方向

| 方向 | 说明 |
|------|------|
| received | 收到的邮件 |
| sent | 已发送的邮件 |

#### 邮件字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| userId | Long | 用户 ID（邮件所属） |
| senderId | Long | 发送者 ID |
| direction | String | 方向（received/sent） |
| sender | String | 发送者名称 |
| senderRole | String | 发送者角色/职位 |
| subject | String | 主题 |
| preview | String | 预览文本 |
| content | CLOB | 正文内容 |
| sendTime | LocalDateTime | 发送时间 |
| unread | Integer | 是否未读（0已读 1未读） |
| starred | Integer | 是否星标 |
| archived | Integer | 是否归档 |
| tagName | String | 标签名（工作/重要/个人） |
| priority | String | 优先级（high/normal/low） |
| recipientsJson | CLOB | 收件人 JSON |
| ccJson | CLOB | 抄送人 JSON |
| attachmentsJson | CLOB | 附件 JSON |
| draft | CLOB | 草稿内容 |

### 3.3 前端实现

**页面**: [Mailbox.vue](file:///e:/ProgramScript/java/DingTalk_Practice/frontend/src/views/Mailbox.vue)

**路径**: `/mailbox`

#### 页面布局

三栏布局：
```
┌──────────┬──────────┬─────────────────┐
│ 文件夹   │ 邮件列表  │   邮件详情       │
│ 收件箱   │          │                 │
| 星标邮件 |          │                 │
| 草稿箱   |          │                 │
| 已发送   |          │                 │
| 归档     |          │                 │
└──────────┴──────────┴─────────────────┘
```

#### 功能列表

- **多文件夹**: 收件箱、星标、草稿、已发送、归档
- **邮件列表**: 发件人、主题、预览、时间、星标/未读标识
- **邮件详情**: 完整正文、收件人、附件
- **写邮件**: 收件人、主题、正文、附件
- **星标/归档**: 快捷标记
- **回复/转发**: 邮件回复和转发
- **草稿保存**: 自动保存草稿
- **搜索**: 按主题/发件人搜索

---

## 四、DING 消息子模块

### 4.1 功能说明

DING 是钉钉特色的紧急消息提醒功能，用于发送重要/紧急消息，确保对方及时查看和响应，支持短信/电话级别的提醒追踪。

### 4.2 后端实现

**核心服务**: `DingService`

**核心实体**: `SysDing` - DING 消息实体

**数据表**: `sys_ding`

#### API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ding/list` | DING 消息列表 |
| POST | `/api/ding/reset` | 重置 DING 数据 |
| POST | `/api/ding/send` | 发送 DING |
| POST | `/api/ding/{id}/confirm` | 确认收到 |
| POST | `/api/ding/{id}/done` | 标记完成 |
| POST | `/api/ding/{id}/remind` | 再次提醒 |
| PUT | `/api/ding/{id}/note` | 保存备注 |

#### DING 方向

| 方向 | 说明 |
|------|------|
| sent | 我发出的 DING |
| received | 我收到的 DING |

#### DING 状态

| 状态 | 说明 |
|------|------|
| pending | 待确认 |
| confirmed | 已确认 |
| done | 已完成 |

#### DING 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| userId | Long | 用户 ID |
| ownerUserId | Long | 所属用户 ID |
| targetUserId | Long | 目标用户 ID |
| title | String | 标题 |
| content | CLOB | 内容 |
| direction | String | 方向（sent/received） |
| ownerName | String | 发送者姓名 |
| targetName | String | 接收者姓名 |
| type | String | 类型 |
| scene | String | 场景 |
| status | String | 状态 |
| priority | String | 优先级 |
| deadlineTime | LocalDateTime | 截止时间 |
| sentTime | LocalDateTime | 发送时间 |
| remindCount | Integer | 提醒次数 |
| note | CLOB | 备注 |

### 4.3 前端实现

**页面**: [Ding.vue](file:///e:/ProgramScript/java/DingTalk_Practice/frontend/src/views/Ding.vue)

**路径**: `/ding`

#### 页面功能

- **Tab 切换**: 我发出的 / 我收到的
- **DING 列表**: 按时间倒序，显示状态、优先级
- **发送 DING**: 选择接收人、标题、内容、截止时间
- **DING 详情**: 查看内容、状态、确认情况
- **确认收到**: 收到 DING 后点击确认
- **标记完成**: 完成后标记已完成
- **再次提醒**: 未确认时可重复提醒
- **备注**: 添加处理备注
- **紧急程度**: 高/中/低优先级标识

---

## 五、消息收藏子模块

### 5.1 功能说明

收藏重要聊天消息，便于后续快速查找和回顾。

### 5.2 后端实现

**核心实体**: `ChatFavorite` - 收藏实体

**数据表**: `chat_favorite`

#### API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/favorite/list` | 收藏列表 |
| POST | `/api/favorite` | 添加收藏 |
| DELETE | `/api/favorite/{id}` | 删除收藏 |

#### 收藏字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| userId | Long | 用户 ID |
| messageId | Long | 消息 ID |
| sessionId | Long | 会话 ID |
| content | String | 收藏内容 |
| senderName | String | 发送者姓名 |
| createTime | LocalDateTime | 创建时间 |

### 5.3 前端实现

**页面**: [Favorites.vue](file:///e:/ProgramScript/java/DingTalk_Practice/frontend/src/views/Favorites.vue)

**路径**: `/favorites`

#### 功能列表

- **收藏列表**: 按时间倒序展示收藏的消息
- **发送者信息**: 显示发送者姓名和头像
- **跳转到会话**: 点击收藏消息跳转到对应聊天
- **删除收藏**: 取消收藏
- **搜索**: 按内容搜索收藏

---

## 六、数据模型

### 6.1 核心数据表汇总

| 子模块 | 表名 | 说明 |
|--------|------|------|
| 通知公告 | `sys_notice` | 公告表 |
| 通知公告 | `sys_notice_read` | 公告阅读记录表 |
| 企业邮箱 | `sys_mailbox_mail` | 邮件表 |
| DING 消息 | `sys_ding` | DING 消息表 |
| 消息收藏 | `chat_favorite` | 收藏表 |

### 6.2 ER 关系

```
sys_user ──┬──> sys_notice (发布者)
           ├──> sys_notice_read (阅读者)
           ├──> sys_mailbox_mail (收发)
           ├──> sys_ding (收发)
           └──> chat_favorite (收藏者)
```

---

## 七、模块依赖关系

```
通知与协同模块
   ├── 子模块
   │   ├── 通知公告子模块
   │   ├── 企业邮箱子模块
   │   ├── DING 消息子模块
   │   └── 消息收藏子模块
   ├── 依赖模块
   │   ├── 认证与权限模块（身份识别）
   │   ├── RBAC 管理模块（用户信息）
   │   └── 即时通讯模块（消息收藏）
   └── 入口
       └── 工作台侧边导航
```

---

## 八、设计要点

### 8.1 信息触达层级

| 层级 | 功能 | 紧急程度 |
|------|------|---------|
| 1 | 通知公告 | 普通 - 主动查看 |
| 2 | 企业邮箱 | 中等 - 邮件通知 |
| 3 | DING 消息 | 紧急 - 强制提醒 |

三级信息传递体系，确保不同重要程度的信息有合适的触达方式。

### 8.2 邮件系统设计

- 单表设计，用 `direction` 字段区分收件/发件
- `userId` 表示邮件所属用户（每个人有自己的收件箱/发件箱）
- JSON 字段存储收件人列表、附件等扩展信息
- 支持星标、归档、草稿等常见邮箱功能

### 8.3 DING 特色设计

- 双向追踪：发送方可追踪接收状态
- 确认机制：接收方需确认收到
- 多次提醒：未确认可反复提醒
- 截止时间：设置 DDL，超时高亮
- 备注功能：记录处理过程

### 8.4 收藏与消息关联

- 收藏独立表，保留消息快照（content, senderName）
- 即使原消息被撤回，收藏仍保留
- 可跳转回原会话位置（消息撤回后可能失效）

# 数据库设计文档

## 一、数据库概述

项目支持两种数据库：
- **H2**（默认）: 内存数据库，开箱即用，适合开发演示
- **MySQL**: 持久化存储，适合生产环境

**建表脚本**: [schema.sql](file:///e:/ProgramScript/java/DingTalk_Practice/backend/src/main/resources/schema.sql)

---

## 二、系统管理表

### 2.1 sys_user - 用户表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| username | VARCHAR(64) | NOT NULL, UNIQUE | 用户名 |
| password | VARCHAR(128) | NOT NULL | 密码（BCrypt加密） |
| nickname | VARCHAR(64) | - | 昵称/姓名 |
| avatar | VARCHAR(255) | - | 头像URL |
| mobile | VARCHAR(20) | - | 手机号 |
| email | VARCHAR(64) | - | 邮箱 |
| gender | TINYINT | DEFAULT 0 | 性别：0未知 1男 2女 |
| job_title | VARCHAR(64) | - | 职位 |
| dept_id | BIGINT | - | 部门ID |
| dept_name | VARCHAR(64) | - | 部门名称 |
| status | TINYINT | DEFAULT 1 | 状态：0停用 1启用 |
| chat_status | TINYINT | DEFAULT 1 | 聊天状态：1在线 2忙碌 3离开 4离线 |
| remark | VARCHAR(255) | - | 备注 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

---

### 2.2 sys_role - 角色表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| name | VARCHAR(64) | - | 角色名称 |
| role_key | VARCHAR(64) | UNIQUE | 角色标识 |
| sort | INT | DEFAULT 0 | 排序 |
| status | TINYINT | DEFAULT 1 | 状态：0停用 1启用 |
| remark | VARCHAR(255) | - | 备注 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

---

### 2.3 sys_menu - 菜单/权限表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| parent_id | BIGINT | DEFAULT 0 | 父级ID |
| name | VARCHAR(64) | - | 菜单名称 |
| type | TINYINT | - | 类型：1目录 2菜单 3按钮 |
| path | VARCHAR(128) | - | 路由路径 |
| icon | VARCHAR(64) | - | 图标 |
| perms | VARCHAR(128) | - | 权限标识 |
| sort | INT | DEFAULT 0 | 排序 |
| visible | TINYINT | DEFAULT 1 | 是否可见：0隐藏 1显示 |

---

### 2.4 sys_dept - 部门表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| parent_id | BIGINT | DEFAULT 0 | 父级ID |
| name | VARCHAR(64) | NOT NULL | 部门名称 |
| ancestors | VARCHAR(255) | - | 祖级列表 |
| sort | INT | DEFAULT 0 | 排序 |
| leader | VARCHAR(64) | - | 负责人 |
| status | TINYINT | DEFAULT 1 | 状态：0停用 1启用 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

---

### 2.5 sys_user_role - 用户-角色关联表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| user_id | BIGINT | NOT NULL | 用户ID |
| role_id | BIGINT | NOT NULL | 角色ID |

---

### 2.6 sys_role_menu - 角色-菜单关联表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| role_id | BIGINT | NOT NULL | 角色ID |
| menu_id | BIGINT | NOT NULL | 菜单ID |

---

### 2.7 sys_user_preference - 用户偏好表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| user_id | BIGINT | NOT NULL, UNIQUE | 用户ID |
| theme | VARCHAR(16) | DEFAULT 'light' | 主题 |
| last_route | VARCHAR(512) | - | 最后访问路由 |
| view_state_json | CLOB | - | 视图状态JSON |
| connected_online | TINYINT | DEFAULT 0 | 是否在线 |
| last_online_at | DATETIME | - | 最后在线时间 |
| last_offline_at | DATETIME | - | 最后离线时间 |
| last_active_at | DATETIME | - | 最后活跃时间 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 更新时间 |

---

## 三、聊天模块表

### 3.1 chat_session - 会话表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| type | TINYINT | NOT NULL | 类型：1单聊 2群聊 |
| name | VARCHAR(128) | - | 会话名称（群聊） |
| avatar | VARCHAR(255) | - | 会话头像（群聊） |
| owner_id | BIGINT | - | 群主ID |
| notice | VARCHAR(512) | - | 群公告 |
| last_msg | VARCHAR(255) | - | 最后消息预览 |
| last_time | DATETIME | - | 最后消息时间 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

---

### 3.2 chat_session_member - 会话成员表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| session_id | BIGINT | NOT NULL | 会话ID |
| user_id | BIGINT | NOT NULL | 用户ID |
| member_role | TINYINT | DEFAULT 2 | 角色：1群主 2普通成员 |
| unread | INT | DEFAULT 0 | 未读数 |
| is_top | TINYINT | DEFAULT 0 | 是否置顶 |
| is_mute | TINYINT | DEFAULT 0 | 是否免打扰 |
| is_star | TINYINT | DEFAULT 0 | 是否特别关注 |
| join_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 加入时间 |

---

### 3.3 chat_message - 消息表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| session_id | BIGINT | NOT NULL | 会话ID |
| sender_id | BIGINT | NOT NULL | 发送者ID（0=系统） |
| content_type | TINYINT | DEFAULT 1 | 类型：1文本 2图片 3文件 4系统 5视频 |
| content | TEXT | - | 消息内容 |
| extra | VARCHAR(512) | - | 附加信息（JSON） |
| at_user_ids | VARCHAR(255) | - | @的用户ID（逗号分隔） |
| status | TINYINT | DEFAULT 1 | 状态：0撤回 1正常 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

---

### 3.4 chat_message_read - 消息已读表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| message_id | BIGINT | NOT NULL | 消息ID |
| session_id | BIGINT | NOT NULL | 会话ID |
| user_id | BIGINT | NOT NULL | 用户ID |
| read_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 阅读时间 |

---

### 3.5 chat_message_reaction - 消息反应表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| message_id | BIGINT | NOT NULL | 消息ID |
| user_id | BIGINT | NOT NULL | 用户ID |
| emoji | VARCHAR(16) | NOT NULL | 表情符号 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

---

### 3.6 chat_favorite - 收藏表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| user_id | BIGINT | NOT NULL | 用户ID |
| message_id | BIGINT | NOT NULL | 消息ID |
| session_id | BIGINT | NOT NULL | 会话ID |
| content | TEXT | - | 收藏内容 |
| sender_name | VARCHAR(64) | - | 发送者姓名 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

---

### 3.7 chat_saved_link - 保存的链接表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| user_id | BIGINT | NOT NULL | 用户ID |
| title | VARCHAR(255) | NOT NULL | 链接标题 |
| url | VARCHAR(1024) | NOT NULL | 链接URL |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 更新时间 |

---

### 3.8 chat_add_usage - 添加面板使用记录表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| user_id | BIGINT | NOT NULL | 用户ID |
| item_key | VARCHAR(255) | NOT NULL | 项目标识 |
| use_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 使用时间 |

---

## 四、业务模块表

### 4.1 sys_file - 文件表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| name | VARCHAR(255) | - | 文件名 |
| url | VARCHAR(512) | - | 文件URL |
| size | BIGINT | - | 文件大小（字节） |
| type | VARCHAR(64) | - | 文件类型 |
| uploader_id | BIGINT | - | 上传者ID |
| scene | VARCHAR(64) | - | 场景 |
| description | VARCHAR(255) | - | 描述 |
| status | VARCHAR(16) | - | 状态 |
| share_count | INT | DEFAULT 0 | 分享次数 |
| history_json | CLOB | - | 历史记录JSON |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 更新时间 |

---

### 4.2 ai_file_chunk - AI文件分片表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| file_id | BIGINT | NOT NULL | 文件ID |
| user_id | BIGINT | NOT NULL | 用户ID |
| file_name | VARCHAR(255) | - | 文件名 |
| file_url | VARCHAR(512) | - | 文件URL |
| chunk_index | INT | NOT NULL | 分片索引 |
| chunk_text | CLOB | - | 分片文本 |
| embedding_json | CLOB | - | 向量JSON |
| metadata_json | CLOB | - | 元数据JSON |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 更新时间 |

**索引**:
- `uk_ai_file_chunk_file_idx`: (file_id, chunk_index) 唯一索引
- `idx_ai_file_chunk_user`: (user_id)
- `idx_ai_file_chunk_file`: (file_id)

---

### 4.3 sys_notice - 通知公告表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| title | VARCHAR(128) | NOT NULL | 标题 |
| content | TEXT | - | 内容 |
| type | TINYINT | DEFAULT 1 | 类型：1通知 2公告 |
| priority | TINYINT | DEFAULT 2 | 优先级：1紧急 2普通 |
| publisher_id | BIGINT | - | 发布者ID |
| status | TINYINT | DEFAULT 1 | 状态：0草稿 1已发布 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

---

### 4.4 sys_notice_read - 公告阅读记录表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| notice_id | BIGINT | NOT NULL | 公告ID |
| user_id | BIGINT | NOT NULL | 用户ID |
| read_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 阅读时间 |

---

### 4.5 sys_todo - 待办表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| title | VARCHAR(128) | NOT NULL | 标题 |
| content | VARCHAR(512) | - | 内容 |
| creator_id | BIGINT | - | 创建者ID |
| assignee_id | BIGINT | - | 负责人ID |
| priority | TINYINT | DEFAULT 2 | 优先级：1高 2中 3低 |
| status | TINYINT | DEFAULT 0 | 状态：0待办 1进行中 2完成 |
| due_time | DATETIME | - | 截止时间 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

---

### 4.6 sys_approval - 审批表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| type | VARCHAR(32) | - | 类型：leave请假 expense报销 |
| title | VARCHAR(128) | - | 标题 |
| content | TEXT | - | 内容（JSON格式） |
| applicant_id | BIGINT | - | 申请人ID |
| approver_id | BIGINT | - | 审批人ID |
| status | TINYINT | DEFAULT 0 | 状态：0待审批 1通过 2驳回 |
| remark | VARCHAR(255) | - | 审批备注 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 申请时间 |
| approve_time | DATETIME | - | 审批时间 |

---

### 4.7 sys_schedule - 日程表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| title | VARCHAR(128) | NOT NULL | 标题 |
| content | VARCHAR(512) | - | 内容 |
| user_id | BIGINT | NOT NULL | 用户ID |
| start_time | DATETIME | - | 开始时间 |
| end_time | DATETIME | - | 结束时间 |
| all_day | TINYINT | DEFAULT 0 | 是否全天 |
| color | VARCHAR(16) | DEFAULT '#1677ff' | 颜色 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

---

### 4.8 sys_mailbox_mail - 邮箱邮件表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| user_id | BIGINT | NOT NULL | 用户ID |
| sender_id | BIGINT | - | 发送者ID |
| direction | VARCHAR(16) | DEFAULT 'received' | 方向 |
| sender | VARCHAR(64) | NOT NULL | 发送者名称 |
| sender_role | VARCHAR(64) | - | 发送者角色 |
| subject | VARCHAR(128) | NOT NULL | 主题 |
| preview | VARCHAR(255) | - | 预览 |
| content | CLOB | - | 内容 |
| send_time | DATETIME | - | 发送时间 |
| unread | TINYINT | DEFAULT 1 | 是否未读 |
| starred | TINYINT | DEFAULT 0 | 是否星标 |
| archived | TINYINT | DEFAULT 0 | 是否归档 |
| tag_name | VARCHAR(32) | - | 标签名 |
| priority | VARCHAR(16) | - | 优先级 |
| recipients_json | CLOB | - | 收件人JSON |
| cc_json | CLOB | - | 抄送人JSON |
| attachments_json | CLOB | - | 附件JSON |
| draft | CLOB | - | 草稿内容 |
| use_cases_json | CLOB | - | 用例JSON |
| history_json | CLOB | - | 历史JSON |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

---

### 4.9 sys_ding - DING消息表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| user_id | BIGINT | NOT NULL | 用户ID |
| owner_user_id | BIGINT | - | 所属用户ID |
| target_user_id | BIGINT | - | 目标用户ID |
| link_key | VARCHAR(64) | - | 链接键 |
| title | VARCHAR(128) | NOT NULL | 标题 |
| content | CLOB | - | 内容 |
| direction | VARCHAR(16) | NOT NULL | 方向 |
| owner_name | VARCHAR(64) | - | 所属者姓名 |
| target_name | VARCHAR(64) | - | 目标姓名 |
| type | VARCHAR(32) | - | 类型 |
| scene | VARCHAR(64) | - | 场景 |
| status | VARCHAR(16) | - | 状态 |
| priority | VARCHAR(16) | - | 优先级 |
| deadline_time | DATETIME | - | 截止时间 |
| sent_time | DATETIME | - | 发送时间 |
| update_time | DATETIME | - | 更新时间 |
| remind_count | INT | DEFAULT 0 | 提醒次数 |
| note | CLOB | - | 备注 |
| use_cases_json | CLOB | - | 用例JSON |
| history_json | CLOB | - | 历史JSON |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

---

## 五、ER关系图

### RBAC 权限体系

```
sys_user ──< sys_user_role >── sys_role ──< sys_role_menu >── sys_menu
    │
    └── 归属 ──> sys_dept (树形)
```

### 聊天体系

```
chat_session (1) ──< chat_session_member >── (N) sys_user
     │
     └──(1)── chat_message (N)
                │
                ├── chat_message_read (N)
                └── chat_message_reaction (N)
```

### 业务体系

```
sys_user ──┬──> sys_notice (发布)
           ├──> sys_todo (创建/负责)
           ├──> sys_approval (申请/审批)
           ├──> sys_schedule (个人)
           ├──> sys_file (上传)
           ├──> sys_mailbox_mail (收发)
           └──> sys_ding (收发)
```

---

## 六、数据字典

### 6.1 通用状态值

| 值 | 说明 |
|----|------|
| 0 | 停用/禁用/隐藏/撤回/草稿 |
| 1 | 启用/正常/显示/已发布 |

### 6.2 菜单类型

| 值 | 说明 |
|----|------|
| 1 | 目录 |
| 2 | 菜单 |
| 3 | 按钮 |

### 6.3 会话类型

| 值 | 说明 |
|----|------|
| 1 | 单聊 |
| 2 | 群聊 |

### 6.4 消息类型

| 值 | 说明 |
|----|------|
| 1 | 文本 |
| 2 | 图片 |
| 3 | 文件 |
| 4 | 系统 |
| 5 | 视频 |

### 6.5 成员角色

| 值 | 说明 |
|----|------|
| 1 | 群主 |
| 2 | 普通成员 |

### 6.6 审批状态

| 值 | 说明 |
|----|------|
| 0 | 待审批 |
| 1 | 通过 |
| 2 | 驳回 |

### 6.7 待办状态

| 值 | 说明 |
|----|------|
| 0 | 待办 |
| 1 | 进行中 |
| 2 | 完成 |

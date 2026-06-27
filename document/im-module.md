# 即时通讯模块 (IM)

## 一、模块概述

即时通讯模块是系统的核心功能之一，提供企业内部即时聊天能力，支持单聊、群聊、消息收发、已读回执、消息撤回、@提醒、表情反应等丰富功能。

**技术方案**: Spring WebSocket (STOMP) + SockJS + 前端 @stomp/stompjs

**模块职责**：
- 单聊与会话管理
- 群聊管理（创建、加人、退群、公告）
- 消息收发与持久化
- 实时消息推送（WebSocket）
- 消息状态（撤回、已读回执）
- 会话设置（置顶、免打扰、特别关注）
- 消息搜索与收藏
- 在线状态管理

---

## 二、后端实现

### 2.1 核心类

| 类名 | 文件 | 职责 |
|------|------|------|
| `ChatService` | [ChatService.java](file:///e:/ProgramScript/java/DingTalk_Practice/backend/src/main/java/com/example/dingtalk/service/ChatService.java) | 聊天业务逻辑核心 |
| `ChatController` | [ChatController.java](file:///e:/ProgramScript/java/DingTalk_Practice/backend/src/main/java/com/example/dingtalk/controller/ChatController.java) | 聊天 REST 接口 |
| `WebSocketConfig` | [WebSocketConfig.java](file:///e:/ProgramScript/java/DingTalk_Practice/backend/src/main/java/com/example/dingtalk/config/WebSocketConfig.java) | WebSocket 配置与鉴权 |
| `WebSocketEventListener` | [WebSocketEventListener.java](file:///e:/ProgramScript/java/DingTalk_Practice/backend/src/main/java/com/example/dingtalk/config/WebSocketEventListener.java) | WebSocket 事件监听 |
| `OnlineService` | [OnlineService.java](file:///e:/ProgramScript/java/DingTalk_Practice/backend/src/main/java/com/example/dingtalk/service/OnlineService.java) | 在线状态管理 |

### 2.2 数据实体

| 实体类 | 表名 | 说明 |
|--------|------|------|
| `ChatSession` | `chat_session` | 会话表（单聊/群聊） |
| `ChatSessionMember` | `chat_session_member` | 会话成员表 |
| `ChatMessage` | `chat_message` | 消息表 |
| `ChatMessageRead` | `chat_message_read` | 消息已读记录表 |
| `ChatMessageReaction` | `chat_message_reaction` | 消息表情反应表 |
| `ChatFavorite` | `chat_favorite` | 消息收藏表 |

### 2.3 API 接口

#### 会话管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/chat/sessions` | 获取用户会话列表 |
| POST | `/api/chat/single` | 发起/获取单聊会话 |
| GET | `/api/chat/messages` | 获取会话历史消息 |
| POST | `/api/chat/read` | 标记会话已读 |
| POST | `/api/chat/toggle-top` | 切换会话置顶 |
| POST | `/api/chat/toggle-mute` | 切换会话免打扰 |
| POST | `/api/chat/toggle-star` | 切换特别关注 |

#### 消息操作

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/chat/messages` | 发送消息 |
| POST | `/api/chat/recall` | 撤回消息 |
| POST | `/api/chat/read-receipt` | 消息已读回执 |
| GET | `/api/chat/message-readers` | 获取消息已读用户列表 |
| GET | `/api/chat/search` | 搜索消息 |

#### 群聊管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/chat/group` | 创建群聊 |
| GET | `/api/chat/group/{sessionId}` | 获取群信息 |
| GET | `/api/chat/group/{sessionId}/members` | 获取群成员列表 |
| POST | `/api/chat/group/{sessionId}/members` | 添加群成员 |
| DELETE | `/api/chat/group/{sessionId}/members/{userId}` | 移除群成员 |
| POST | `/api/chat/group/{sessionId}/quit` | 退出群聊 |
| PUT | `/api/chat/group/{sessionId}/notice` | 更新群公告 |

#### 在线状态

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/online/users` | 获取在线用户列表 |

### 2.4 WebSocket 配置

**端点**: `/ws`（支持 SockJS 和原生 WebSocket）

**消息代理**:

| 前缀 | 类型 | 说明 |
|------|------|------|
| `/topic` | 广播 | 主题消息（在线状态等） |
| `/queue` | 点对点 | 用户私有队列 |
| `/app` | 应用目的地 | 客户端发送到服务端 |
| `/user` | 用户目的地 | 转换为用户私有队列 |

**推送目的地**:

| 目的地 | 说明 |
|--------|------|
| `/user/queue/message` | 新消息推送 |
| `/user/queue/receipt` | 已读回执推送 |
| `/topic/online-status` | 在线状态广播 |
| `/user/queue/session-unread` | 会话未读数更新 |
| `/user/queue/work-notice` | 工作台通知 |
| `/user/queue/kick` | 踢人下线 |

### 2.5 WebSocket 鉴权

通过 `ChannelInterceptor` 拦截 STOMP CONNECT 帧：

```
客户端发起 CONNECT
   ↓
从 native header 提取 Authorization
   ↓
解析 Bearer Token
   ↓
JWT 验证有效性
   ↓
绑定 StompPrincipal (name = userId)
   ↓
建立 WebSocket 连接
```

### 2.6 消息发送流程

```
1. 调用 sendMessage(userId, dto)
   ↓
2. 校验用户是否为会话成员
   ↓
3. 构建 ChatMessage 对象并保存
   ├─ sessionId, senderId, contentType
   ├─ content, extra, atUserIds
   └─ status = 1 (正常)
   ↓
4. 更新会话最后消息和时间
   ↓
5. 查询所有会话成员
   ├─ 为其他成员增加未读数
   └─ 更新 ChatSessionMember.unread
   ↓
6. 通过 SimpMessagingTemplate 推送
   ├─ 遍历会话成员
   └─ convertAndSendToUser(userId, "/queue/message", messageVO)
   ↓
7. 推送未读数更新
   ↓
8. 返回 MessageVO
```

### 2.7 群聊管理

#### 创建群聊

```
1. 校验群名称和成员列表
   ↓
2. 创建 chat_session 记录
   ├─ type = 2 (群聊)
   ├─ name, ownerId = 创建者
   └─ 创建时间
   ↓
3. 批量添加成员
   ├─ 创建者: member_role = 1 (群主)
   └─ 其他成员: member_role = 2 (普通)
   ↓
4. 发送系统消息（"xxx 创建了群聊"）
   ↓
5. 推送会话更新给所有成员
```

#### 添加/移除成员

- 添加：校验权限（群主可加人），批量插入成员记录，发系统消息
- 移除：校验权限（群主可踢人），删除成员记录，发系统消息
- 退群：群主不可退（需先转让），删除成员记录，发系统消息

### 2.8 消息类型

| 类型值 | 类型名称 | content 内容 | extra 附加信息 |
|--------|---------|-------------|---------------|
| 1 | 文本 | 文本内容 | - |
| 2 | 图片 | 图片 URL | `{ name, size, width, height }` |
| 3 | 文件 | 文件 URL | `{ name, size, type }` |
| 4 | 系统 | 系统消息文本 | - |
| 5 | 视频 | 视频 URL | `{ name, size, duration }` |

### 2.9 已读回执机制

#### 会话级已读

- `markRead()` - 标记整个会话已读
- 更新 `chat_session_member.unread = 0`
- 推送已读通知给发送者

#### 消息级已读回执

- `markMessageRead()` - 标记单条消息已读
- 插入 `chat_message_read` 记录
- 推送已读回执给消息发送者
- 前端可查看「哪些人已读」

### 2.10 在线状态管理

**OnlineService** 提供：

| 方法 | 说明 |
|------|------|
| `setOnline(userId)` | 设置用户在线 |
| `setOffline(userId)` | 设置用户离线 |
| `isOnline(userId)` | 判断用户是否在线 |
| `getOnlineUsers()` | 获取所有在线用户 |

实现方式：内存缓存 + WebSocket 事件监听

- 连接建立 → setOnline
- 连接断开 → setOffline
- 广播在线状态变化

---

## 三、前端实现

### 3.1 核心文件

| 文件 | 职责 |
|------|------|
| [Workbench.vue](file:///e:/ProgramScript/java/DingTalk_Practice/frontend/src/views/Workbench.vue) | 工作台主界面（三栏布局） |
| [ws.js](file:///e:/ProgramScript/java/DingTalk_Practice/frontend/src/utils/ws.js) | WebSocket 客户端封装 |
| [index.js](file:///e:/ProgramScript/java/DingTalk_Practice/frontend/src/api/index.js) | 聊天 API 接口 |
| [AppSideNav.vue](file:///e:/ProgramScript/java/DingTalk_Practice/frontend/src/components/AppSideNav.vue) | 左侧导航栏 |

### 3.2 页面布局

工作台采用三栏布局：

```
┌─────┬──────────┬──────────────────┐
│导航 │ 会话列表  │    聊天窗口       │
│栏   │          │                  │
│消息 │ 单聊/群聊 │ 消息展示区域      │
│文档 │          │                  │
│邮箱 │          │ 输入框 + 工具栏   │
│...  │          │                  │
└─────┴──────────┴──────────────────┘
```

### 3.3 WebSocket 封装 (ws.js)

#### 连接管理

| 函数 | 说明 |
|------|------|
| `connectWs(token, onMessage, onOnline, ...)` | 建立连接并注册监听器 |
| `disconnectWs()` | 断开连接 |
| `removeWsListeners(onMessage, ...)` | 移除监听器 |

#### 事件监听

支持以下事件类型的多监听器模式：

| 事件 | 订阅路径 | 触发时机 |
|------|---------|---------|
| message | `/user/queue/message` | 收到新消息 |
| receipt | `/user/queue/receipt` | 收到已读回执 |
| online | `/topic/online-status` | 在线状态变化 |
| workNotice | `/user/queue/work-notice` | 工作台通知 |
| sessionUnread | `/user/queue/session-unread` | 未读数更新 |
| kick | `/user/queue/kick` | 被踢下线 |

#### 连接配置

| 配置项 | 值 | 说明 |
|-------|----|------|
| brokerURL | `ws://host:port/ws` | WebSocket 地址 |
| connectHeaders | `{ Authorization: Bearer token }` | 鉴权头 |
| reconnectDelay | 3000ms | 自动重连间隔 |
| heartbeatIncoming | 10000ms | 入站心跳 |
| heartbeatOutgoing | 10000ms | 出站心跳 |

#### 自动重连机制

- 网络断开后自动重连
- 重连间隔 3 秒
- 重连成功后重新订阅所有队列
- 监听器不丢失

### 3.4 会话列表

#### 会话项展示

- 会话头像/名称
- 最后一条消息预览
- 最后消息时间
- 未读数角标
- 置顶/免打扰/特别关注标识

#### 会话排序

1. 置顶会话排在最前
2. 按最后消息时间倒序
3. 未读数高亮显示

### 3.5 聊天窗口

#### 消息展示

- 消息气泡（左收右发）
- 发送者头像和姓名
- 消息时间
- 已读状态（对勾图标）
- @高亮显示
- 撤回提示
- 表情反应条

#### 消息操作

- 右键/悬停菜单：复制、转发、撤回、收藏、添加表情
- 已读回执：查看已读成员列表
- 撤回：本人或群主可撤回

#### 输入功能

- 文本输入
- 表情选择
- 图片上传
- 文件上传
- @提及成员
- 发送快捷键（Enter 发送，Shift+Enter 换行）

### 3.6 群聊功能

#### 群设置

- 群信息展示（名称、公告、成员数）
- 群成员列表
- 添加成员 / 移除成员（群主）
- 修改群公告（群主）
- 退出群聊

#### 群聊特色

- @全体成员 / @特定成员
- 群公告置顶显示
- 群主标识
- 成员在线状态显示

---

## 四、数据模型

### 4.1 核心数据表

| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `chat_session` | 会话表 | id, type(1单聊/2群聊), name, owner_id, last_msg, last_time |
| `chat_session_member` | 会话成员表 | id, session_id, user_id, member_role, unread, is_top, is_mute, is_star |
| `chat_message` | 消息表 | id, session_id, sender_id, content_type, content, extra, status |
| `chat_message_read` | 消息已读表 | id, message_id, session_id, user_id, read_time |
| `chat_message_reaction` | 消息反应表 | id, message_id, user_id, emoji |

### 4.2 ER 关系

```
chat_session (1) ──< chat_session_member >── (N) sys_user
     │
     └──(1)── chat_message (N)
                │
                ├── chat_message_read (N)
                └── chat_message_reaction (N)
```

---

## 五、模块依赖关系

```
即时通讯模块
   ├── 依赖模块
   │   ├── 认证与权限模块（身份识别）
   │   ├── 用户管理模块（用户信息）
   │   ├── 文件管理模块（图片/文件消息）
   │   └── AI 助手模块（AI 对话）
   ├── 被依赖模块
   │   ├── 工作台（主入口）
   │   └── 通知协同模块（DING 提醒）
   └── 核心技术
       ├── Spring WebSocket + STOMP
       ├── SockJS (降级兼容)
       └── @stomp/stompjs (前端)
```

---

## 六、设计要点

### 6.1 实时通信架构

- 使用 STOMP 协议，提供消息队列语义
- SockJS 兜底，兼容不支持 WebSocket 的环境
- 点对点消息使用 `/user/queue/` 前缀
- 广播消息使用 `/topic/` 前缀

### 6.2 消息可靠性

- 消息先持久化再推送
- WebSocket 推送失败不影响数据库
- 历史消息通过 REST API 拉取
- 连接重连后可补拉消息

### 6.3 未读数管理

- 未读数存在 `chat_session_member` 表
- 发送消息时批量更新未读数
- 进入会话时清零未读数
- 未读数变化实时推送

### 6.4 扩展性设计

- 消息类型可扩展（content_type）
- 附加信息用 extra JSON 字段
- 系统消息复用消息表（sender_id = 0）
- 表情反应独立表，支持多种表情

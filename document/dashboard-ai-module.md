# 数据看板与 AI 助手模块

## 一、模块概述

本模块包含数据可视化看板和 AI 智能助手两大功能。数据看板提供企业运营数据的直观展示，AI 助手基于大语言模型提供智能对话和文档分析能力。

**模块职责**：
- 数据看板（概览统计、部门分布、消息趋势）
- AI 助手（智能对话、流式响应、文件分析 RAG）
- 文档向量化与检索（RAG 知识库）
---

## 二、数据看板子模块

### 2.1 功能说明

企业数据可视化面板，通过图表直观展示人员、消息、部门等核心数据指标，帮助管理者快速了解企业运营状况。

### 2.2 后端实现

**核心服务**: `DashboardService`

**数据表**: 聚合查询 `sys_user`, `sys_dept`, `chat_message` 等表

#### API 接口

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/dashboard/overview` | 概览统计 | dashboard:view |
| GET | `/api/dashboard/dept-distribution` | 部门分布 | dashboard:view |
| GET | `/api/dashboard/message-trend` | 消息趋势 | dashboard:view |

#### 概览统计

返回核心指标数据：

| 指标 | 说明 |
|------|------|
| userCount | 用户总数 |
| deptCount | 部门总数 |
| messageCount | 消息总数 |
| todoCount | 待办总数 |
| onlineCount | 在线用户数 |

#### 部门分布

按部门统计人数，用于饼图展示：

```json
[
  { "name": "技术研发部", "value": 30 },
  { "name": "产品运营部", "value": 15 },
]
```

#### 消息趋势

按日期统计近 7 天消息数量，用于折线图展示：

```json
{
  "dates": ["06-20", "06-21",],
  "counts": [128, 256, ]
}
```

### 2.3 前端实现

**页面**: [Dashboard.vue](file:///e:/ProgramScript/java/DingTalk_Practice/frontend/src/views/admin/Dashboard.vue)

**路径**: `/admin/dashboard`

**技术**: ECharts 5.x

#### 页面布局

```
┌──────────────────────────────────────────┐
│  概览卡片区域                              │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐   │
│  │ 用户数│ │部门数│ │消息数│ │在线数│   │
│  └──────┘ └──────┘ └──────┘ └──────┘   │
├─────────────────────┬────────────────────┤
│                     │                    │
│  部门分布饼图        │   消息趋势折线图    │
│                     │                    │
└─────────────────────┴────────────────────┘
```

#### 功能列表

- **数据卡片**: 4 个核心指标，数字 + 图标
- **部门分布饼图**: 各部门人数占比，悬停显示详情
- **消息趋势折线图**: 近 7 天消息量变化趋势
- **数据刷新**: 自动刷新 / 手动刷新
- **响应式布局**: 自适应不同屏幕尺寸

---

## 三、AI 助手子模块

### 3.1 功能说明

基于 Spring AI 的智能对话助手，集成到大模型能力，支持自然语言问答、流式响应、文件知识库问答（RAG）等功能。

### 3.2 后端实现

**核心服务**: `ChatService.aiReply()`, `AiRagService`, `AiDocumentIngestService`

**技术栈**:
- Spring AI 1.0.9（OpenAI 兼容）
- Apache Tika 2.9.2（文档解析）
- 向量相似度检索（本地存储）

#### API 接口

| 方法 | 路径 | 说明 | 方式 |
|------|------|------|------|
| POST | `/api/chat/ai-reply` | AI 回复 | SSE 流式 |

#### 配置项 (application.yml)

| 配置 | 默认值 | 说明 |
|------|--------|------|
| `spring.ai.model.chat` | openai | 聊天模型提供者 |
| `spring.ai.openai.api-key` | disabled | API 密钥 |
| `spring.ai.openai.base-url` | https://api.openai.com | API 地址 |
| `spring.ai.openai.chat.options.model` | gpt-4o-mini | 模型名称 |
| `app.ai.enabled` | true | AI 功能开关 |
| `app.ai.rag.top-k` | 4 | RAG 返回片段数 |
| `app.ai.rag.min-score` | 0.18 | RAG 最低相似度 |

#### AI 对话流程

```
用户发送问题
   ↓
ChatService.aiReply()
   ├─ 构建对话上下文
   ├─ 检查是否需要 RAG 检索
   │   └─ 是 → AiRagService.similaritySearch()
   │       ├─ 向量相似度匹配
   │       └─ 返回最相关的文档片段
   ├─ 组装 Prompt（系统提示 + 上下文 + RAG 结果）
   ├─ 调用大模型 (ChatClient.stream())
   └─ SSE 流式返回
   ↓
前端逐字渲染响应
```

#### RAG 检索增强生成

**知识库构建流程**:
1. 用户上传文档到文件系统
2. `AiDocumentIngestService` 处理文档
   - Tika 解析文档内容（PDF/Word/Excel/PPT 等）
   - 文本分片（按段落/长度切分）
   - 调用 Embedding API 生成向量
   - 存储到 `ai_file_chunk` 表

**检索流程**:
1. 用户提问
2. 对问题生成 Embedding 向量
3. 与知识库向量做相似度计算
4. 返回 Top-K 个最相关片段
5. 拼接到 Prompt 中作为上下文
6. 大模型基于知识库回答

### 3.3 前端实现

**位置**: 工作台聊天窗口内，AI 助手会话

**技术**: SSE (Server-Sent Events) 流式接收

#### 流式响应实现

```javascript
// 使用自定义 SSE 客户端
const sse = apiCreateAiSse(sessionId, question)
sse.onmessage = (event) => {
  // 逐字追加到消息内容
  message.content += event.data
}
sse.onerror = (err) => {
  console.error('SSE error:', err)
}
```

**前端 API**: `apiCreateAiSse` — 返回类 EventSource 对象

| 方法/属性 | 说明 |
|----------|------|
| `onmessage` | 消息回调（每收到一段数据触发） |
| `onerror` | 错误回调 |
| `onclose` | 关闭回调 |
| `close()` | 手动关闭连接 |

#### AI 对话特色

- **流式输出**: 打字机效果，逐字显示
- **Markdown 渲染**: 支持代码高亮、列表、表格等
- **引用来源**: RAG 回答时显示引用的文档
- **对话上下文**: 保留多轮对话历史
- **文件分析**: 上传文档后可针对文档提问

---

## 四、文档向量化子模块

### 4.1 功能说明

将上传的文档解析并向量化，构建企业知识库，支撑 AI 助手的 RAG 检索问答。

### 4.2 后端实现

**核心服务**: `AiDocumentIngestService`

**核心实体**: `AiFileChunk` - 文件分片实体

**数据表**: `ai_file_chunk`

#### 文档解析支持格式

- PDF (.pdf)
- Word (.doc, .docx)
- Excel (.xls, .xlsx)
- PowerPoint (.ppt, .pptx)
- 文本文件 (.txt, .md, .csv, .json 等)
- 图片（OCR 可选）

#### 处理流程

```
文件上传
   ↓
AiDocumentIngestService.ingestDocument()
   ├─ 1. 文件下载（从本地存储读取）
   ├─ 2. Tika 解析文本内容
   ├─ 3. 文本清洗与分片
   │   ├─ 按段落/句子切分
   │   └─ 每片 ~500-1000 tokens
   ├─ 4. 批量生成 Embedding 向量
   └─ 5. 存储到 ai_file_chunk 表
   ↓
完成向量化，可用于 RAG 检索
```

#### 向量数据表

`ai_file_chunk` 表结构：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| file_id | Long | 关联文件 ID |
| user_id | Long | 用户 ID |
| file_name | String | 文件名 |
| file_url | String | 文件 URL |
| chunk_index | Integer | 分片索引 |
| chunk_text | CLOB | 分片文本内容 |
| embedding_json | CLOB | 向量数组 JSON |
| metadata_json | CLOB | 元数据 JSON |
| create_time | DateTime | 创建时间 |

**索引**:
- `(file_id, chunk_index)` 唯一索引
- `(user_id)` 普通索引
- `(file_id)` 普通索引

### 4.3 相似度检索

**核心服务**: `AiRagService.similaritySearch()`

**检索算法**: 余弦相似度

```
查询文本 → 生成向量
   ↓
与知识库所有分片向量计算余弦相似度
   ↓
按相似度降序排序
   ↓
过滤相似度 < min-score 的结果
   ↓
返回 Top-K 个最相关片段
```

**检索参数**:
- `top-k`: 返回结果数量（默认 4）
- `min-score`: 最低相似度阈值（默认 0.18）
- `user_id`: 只检索该用户上传的文档

---

## 五、数据模型

### 5.1 核心数据表

| 子模块 | 表名 | 说明 |
|--------|------|------|
| 数据看板 | （聚合查询） | 无独立表，聚合各业务表 |
| AI 助手 | `ai_file_chunk` | AI 文件分片向量表 |

### 5.2 ER 关系

```
sys_file ──(1:N)──> ai_file_chunk
                        │
                        └── 归属: sys_user (user_id)
```

---

## 六、模块依赖关系

```
数据看板与AI助手模块
   ├── 子模块
   │   ├── 数据看板子模块
   │   ├── AI 助手子模块
   │   └── 文档向量化子模块
   ├── 依赖模块
   │   ├── 认证与权限模块（身份识别）
   │   ├── RBAC 管理模块（用户/部门数据）
   │   ├── 即时通讯模块（AI 对话入口）
   │   └── 文件管理模块（文档源）
   ├── 核心技术
   │   ├── ECharts（前端图表）
   │   ├── Spring AI（大模型调用）
   │   ├── Apache Tika（文档解析）
   │   └── 向量相似度检索
   └── 入口
       ├── 管理后台 → 数据看板
       └── 工作台聊天 → AI 助手
```

---

## 七、设计要点

### 7.1 数据看板设计

- **数据聚合**: 直接聚合业务表，不单独建统计表
- **实时性**: 每次请求实时计算，数据最新
- **可视化**: ECharts 丰富的图表类型
- **权限控制**: `dashboard:view` 权限标识保护

### 7.2 AI 流式响应

- **SSE 技术**: Server-Sent Events，服务端推送
- **打字机效果**: 提升交互体验
- **内存友好**: 流式处理，无需等待完整响应
- **错误处理**: 网络中断可重新生成

### 7.3 RAG 知识库

- **文档分片**: 平衡上下文长度和检索精度
- **向量检索**: 语义匹配，优于关键词搜索
- **权限隔离**: 每个用户只能检索自己上传的文档
- **可插拔**: 可切换不同向量数据库（Milvus, Pinecone 等）

### 7.4 AI 功能开关

- `app.ai.enabled` 配置项控制整体开关
- API Key 为 `disabled` 时提示配置
- 不影响其他模块正常使用
- 支持多种 OpenAI 兼容的大模型服务

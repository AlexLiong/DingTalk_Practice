-- H2 (MySQL 模式) 建表脚本; 文件库场景下可重复执行且不会清空数据

-- 部门表(树形)
CREATE TABLE IF NOT EXISTS sys_dept (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_id   BIGINT DEFAULT 0,
  name        VARCHAR(64) NOT NULL,
  ancestors   VARCHAR(255),
  sort        INT DEFAULT 0,
  leader      VARCHAR(64),
  status      TINYINT DEFAULT 1,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  username    VARCHAR(64)  NOT NULL UNIQUE,
  password    VARCHAR(128) NOT NULL,
  nickname    VARCHAR(64),
  avatar      VARCHAR(255),
  mobile      VARCHAR(20),
  email       VARCHAR(64),
  gender      TINYINT DEFAULT 0,
  job_title   VARCHAR(64),
  dept_id     BIGINT,
  dept_name   VARCHAR(64),
  status      TINYINT DEFAULT 1,
  chat_status TINYINT DEFAULT 1,
  remark      VARCHAR(255),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 用户偏好/在线留痕
CREATE TABLE IF NOT EXISTS sys_user_preference (
  id               BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id          BIGINT NOT NULL UNIQUE,
  theme            VARCHAR(16) DEFAULT 'light',
  last_route       VARCHAR(512),
  view_state_json  CLOB,
  connected_online TINYINT DEFAULT 0,
  last_online_at   DATETIME,
  last_offline_at  DATETIME,
  last_active_at   DATETIME,
  create_time      DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time      DATETIME DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE sys_user_preference ADD COLUMN IF NOT EXISTS theme VARCHAR(16) DEFAULT 'light';
ALTER TABLE sys_user_preference ADD COLUMN IF NOT EXISTS last_route VARCHAR(512);
ALTER TABLE sys_user_preference ADD COLUMN IF NOT EXISTS view_state_json CLOB;
ALTER TABLE sys_user_preference ADD COLUMN IF NOT EXISTS connected_online TINYINT DEFAULT 0;
ALTER TABLE sys_user_preference ADD COLUMN IF NOT EXISTS last_online_at DATETIME;
ALTER TABLE sys_user_preference ADD COLUMN IF NOT EXISTS last_offline_at DATETIME;
ALTER TABLE sys_user_preference ADD COLUMN IF NOT EXISTS last_active_at DATETIME;
ALTER TABLE sys_user_preference ADD COLUMN IF NOT EXISTS create_time DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE sys_user_preference ADD COLUMN IF NOT EXISTS update_time DATETIME DEFAULT CURRENT_TIMESTAMP;

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  name        VARCHAR(64),
  role_key    VARCHAR(64) UNIQUE,
  sort        INT DEFAULT 0,
  status      TINYINT DEFAULT 1,
  remark      VARCHAR(255),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 菜单/权限表
CREATE TABLE IF NOT EXISTS sys_menu (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_id   BIGINT DEFAULT 0,
  name        VARCHAR(64),
  type        TINYINT,            -- 1目录 2菜单 3按钮
  path        VARCHAR(128),
  icon        VARCHAR(64),
  perms       VARCHAR(128),       -- 权限标识 如 system:user:add
  sort        INT DEFAULT 0,
  visible     TINYINT DEFAULT 1
);

-- 用户-角色
CREATE TABLE IF NOT EXISTS sys_user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL
);

-- 角色-菜单
CREATE TABLE IF NOT EXISTS sys_role_menu (
  role_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL
);

-- 会话表
CREATE TABLE IF NOT EXISTS chat_session (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  type        TINYINT NOT NULL,           -- 1单聊 2群聊
  name        VARCHAR(128),
  avatar      VARCHAR(255),
  owner_id    BIGINT,
  notice      VARCHAR(512),               -- 群公告
  last_msg    VARCHAR(255),
  last_time   DATETIME,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 会话成员
CREATE TABLE IF NOT EXISTS chat_session_member (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id  BIGINT NOT NULL,
  user_id     BIGINT NOT NULL,
  member_role TINYINT DEFAULT 2,          -- 1群主 2普通成员
  unread      INT DEFAULT 0,
  is_top      TINYINT DEFAULT 0,
  is_mute     TINYINT DEFAULT 0,          -- 免打扰
  is_star     TINYINT DEFAULT 0,          -- 特别关注
  join_time   DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 消息表
CREATE TABLE IF NOT EXISTS chat_message (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id   BIGINT NOT NULL,
  sender_id    BIGINT NOT NULL,
  content_type TINYINT DEFAULT 1,         -- 1文本 2图片 3文件 4系统
  content      TEXT,
  extra        VARCHAR(512),              -- 文件名/大小/url 等 json
  at_user_ids  VARCHAR(255),              -- @的用户id, 逗号分隔
  status       TINYINT DEFAULT 1,         -- 1正常 0撤回
  create_time  DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 文件表
CREATE TABLE IF NOT EXISTS sys_file (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  name        VARCHAR(255),
  url         VARCHAR(512),
  size        BIGINT,
  type        VARCHAR(64),
  uploader_id BIGINT,
  scene       VARCHAR(64),
  description VARCHAR(255),
  status      VARCHAR(16),
  share_count INT DEFAULT 0,
  history_json CLOB,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE sys_file ADD COLUMN IF NOT EXISTS scene VARCHAR(64);
ALTER TABLE sys_file ADD COLUMN IF NOT EXISTS description VARCHAR(255);
ALTER TABLE sys_file ADD COLUMN IF NOT EXISTS status VARCHAR(16);
ALTER TABLE sys_file ADD COLUMN IF NOT EXISTS share_count INT DEFAULT 0;
ALTER TABLE sys_file ADD COLUMN IF NOT EXISTS history_json CLOB;
ALTER TABLE sys_file ADD COLUMN IF NOT EXISTS update_time DATETIME DEFAULT CURRENT_TIMESTAMP;

-- AI 文档分片/向量
CREATE TABLE IF NOT EXISTS ai_file_chunk (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  file_id       BIGINT NOT NULL,
  user_id       BIGINT NOT NULL,
  file_name     VARCHAR(255),
  file_url      VARCHAR(512),
  chunk_index   INT NOT NULL,
  chunk_text    CLOB,
  embedding_json CLOB,
  metadata_json CLOB,
  create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_file_chunk_file_idx ON ai_file_chunk(file_id, chunk_index);
CREATE INDEX IF NOT EXISTS idx_ai_file_chunk_user ON ai_file_chunk(user_id);
CREATE INDEX IF NOT EXISTS idx_ai_file_chunk_file ON ai_file_chunk(file_id);

-- 通知公告
CREATE TABLE IF NOT EXISTS sys_notice (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  title        VARCHAR(128) NOT NULL,
  content      TEXT,
  type         TINYINT DEFAULT 1,       -- 1通知 2公告
  priority     TINYINT DEFAULT 2,       -- 1紧急 2普通
  publisher_id BIGINT,
  status       TINYINT DEFAULT 1,       -- 1已发布 0草稿
  create_time  DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_notice_read (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  notice_id  BIGINT NOT NULL,
  user_id    BIGINT NOT NULL,
  read_time  DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 待办任务
CREATE TABLE IF NOT EXISTS sys_todo (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  title        VARCHAR(128) NOT NULL,
  content      VARCHAR(512),
  creator_id   BIGINT,
  assignee_id  BIGINT,
  priority     TINYINT DEFAULT 2,       -- 1高 2中 3低
  status       TINYINT DEFAULT 0,       -- 0待办 1进行中 2完成
  due_time     DATETIME,
  create_time  DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 消息已读回执
CREATE TABLE IF NOT EXISTS chat_message_read (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  message_id  BIGINT NOT NULL,
  session_id  BIGINT NOT NULL,
  user_id     BIGINT NOT NULL,
  read_time   DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 审批
CREATE TABLE IF NOT EXISTS sys_approval (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  type         VARCHAR(32),             -- leave请假 expense报销
  title        VARCHAR(128),
  content      TEXT,                    -- JSON: 事由/天数/金额 等
  applicant_id BIGINT,
  approver_id  BIGINT,
  status       TINYINT DEFAULT 0,       -- 0待审批 1通过 2驳回
  remark       VARCHAR(255),
  create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
  approve_time DATETIME
);

-- 消息收藏
CREATE TABLE IF NOT EXISTS chat_favorite (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id     BIGINT NOT NULL,
  message_id  BIGINT NOT NULL,
  session_id  BIGINT NOT NULL,
  content     TEXT,
  sender_name VARCHAR(64),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 添加面板: 已保存链接
CREATE TABLE IF NOT EXISTS chat_saved_link (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id     BIGINT NOT NULL,
  title       VARCHAR(255) NOT NULL,
  url         VARCHAR(1024) NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 添加面板: 最近使用
CREATE TABLE IF NOT EXISTS chat_add_usage (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id     BIGINT NOT NULL,
  item_key    VARCHAR(255) NOT NULL,
  use_time    DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 日程
CREATE TABLE IF NOT EXISTS sys_schedule (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  title       VARCHAR(128) NOT NULL,
  content     VARCHAR(512),
  user_id     BIGINT NOT NULL,
  start_time  DATETIME,
  end_time    DATETIME,
  all_day     TINYINT DEFAULT 0,
  color       VARCHAR(16) DEFAULT '#1677ff',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 邮箱
CREATE TABLE IF NOT EXISTS sys_mailbox_mail (
  id               BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id          BIGINT NOT NULL,
  sender_id        BIGINT,
  direction        VARCHAR(16) DEFAULT 'received',
  sender           VARCHAR(64) NOT NULL,
  sender_role      VARCHAR(64),
  subject          VARCHAR(128) NOT NULL,
  preview          VARCHAR(255),
  content          CLOB,
  send_time        DATETIME,
  unread           TINYINT DEFAULT 1,
  starred          TINYINT DEFAULT 0,
  archived         TINYINT DEFAULT 0,
  tag_name         VARCHAR(32),
  priority         VARCHAR(16),
  recipients_json  CLOB,
  cc_json          CLOB,
  attachments_json CLOB,
  draft            CLOB,
  use_cases_json   CLOB,
  history_json     CLOB,
  create_time      DATETIME DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE sys_mailbox_mail ADD COLUMN IF NOT EXISTS sender_id BIGINT;
ALTER TABLE sys_mailbox_mail ADD COLUMN IF NOT EXISTS direction VARCHAR(16) DEFAULT 'received';

-- DING
CREATE TABLE IF NOT EXISTS sys_ding (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id        BIGINT NOT NULL,
  owner_user_id  BIGINT,
  target_user_id BIGINT,
  link_key       VARCHAR(64),
  title          VARCHAR(128) NOT NULL,
  content        CLOB,
  direction      VARCHAR(16) NOT NULL,
  owner_name     VARCHAR(64),
  target_name    VARCHAR(64),
  type           VARCHAR(32),
  scene          VARCHAR(64),
  status         VARCHAR(16),
  priority       VARCHAR(16),
  deadline_time  DATETIME,
  sent_time      DATETIME,
  update_time    DATETIME,
  remind_count   INT DEFAULT 0,
  note           CLOB,
  use_cases_json CLOB,
  history_json   CLOB,
  create_time    DATETIME DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE sys_ding ADD COLUMN IF NOT EXISTS owner_user_id BIGINT;
ALTER TABLE sys_ding ADD COLUMN IF NOT EXISTS target_user_id BIGINT;
ALTER TABLE sys_ding ADD COLUMN IF NOT EXISTS link_key VARCHAR(64);

-- 消息表情反应
CREATE TABLE IF NOT EXISTS chat_message_reaction (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  message_id  BIGINT NOT NULL,
  user_id     BIGINT NOT NULL,
  emoji       VARCHAR(16) NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

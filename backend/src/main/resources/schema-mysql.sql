-- ============================================================
-- 仿钉钉企业协作管理系统 - MySQL/MariaDB 建表脚本
-- 字符集: utf8mb4  引擎: InnoDB
-- ============================================================

-- 部门表(树形)
CREATE TABLE IF NOT EXISTS sys_dept (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_id   BIGINT DEFAULT 0      COMMENT '父部门id, 0为根',
  name        VARCHAR(64) NOT NULL  COMMENT '部门名',
  ancestors   VARCHAR(255)          COMMENT '祖级列表 0,1,3',
  sort        INT DEFAULT 0,
  leader      VARCHAR(64)           COMMENT '负责人',
  status      TINYINT DEFAULT 1     COMMENT '1正常 0停用',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门';

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  username    VARCHAR(64)  NOT NULL UNIQUE COMMENT '登录账号',
  password    VARCHAR(128) NOT NULL       COMMENT 'BCrypt密文',
  nickname    VARCHAR(64)                 COMMENT '姓名',
  avatar      VARCHAR(255)                COMMENT '头像url',
  mobile      VARCHAR(20),
  email       VARCHAR(64),
  gender      TINYINT DEFAULT 0           COMMENT '0未知 1男 2女',
  job_title   VARCHAR(64)                 COMMENT '职位',
  dept_id     BIGINT                      COMMENT '所属部门',
  dept_name   VARCHAR(64),
  status      TINYINT DEFAULT 1,
  chat_status TINYINT DEFAULT 1          COMMENT '聊天状态 1在线 2忙碌 3离开',
  remark      VARCHAR(255),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

-- 用户偏好/在线留痕
CREATE TABLE IF NOT EXISTS sys_user_preference (
  id               BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id          BIGINT NOT NULL,
  theme            VARCHAR(16) DEFAULT 'light',
  last_route       VARCHAR(512),
  view_state_json  LONGTEXT,
  connected_online TINYINT DEFAULT 0,
  last_online_at   DATETIME,
  last_offline_at  DATETIME,
  last_active_at   DATETIME,
  create_time      DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time      DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_preference_user (user_id),
  INDEX idx_user_preference_online (connected_online),
  INDEX idx_user_preference_active (last_active_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户偏好与在线留痕';

ALTER TABLE sys_user_preference ADD COLUMN IF NOT EXISTS theme VARCHAR(16) DEFAULT 'light';
ALTER TABLE sys_user_preference ADD COLUMN IF NOT EXISTS last_route VARCHAR(512);
ALTER TABLE sys_user_preference ADD COLUMN IF NOT EXISTS view_state_json LONGTEXT;
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
  role_key    VARCHAR(64) UNIQUE    COMMENT '权限字符 如 admin',
  sort        INT DEFAULT 0,
  status      TINYINT DEFAULT 1,
  remark      VARCHAR(255),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色';

-- 菜单/权限表
CREATE TABLE IF NOT EXISTS sys_menu (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_id   BIGINT DEFAULT 0,
  name        VARCHAR(64),
  type        TINYINT               COMMENT '1目录 2菜单 3按钮',
  path        VARCHAR(128),
  icon        VARCHAR(64),
  perms       VARCHAR(128)          COMMENT '权限标识 如 system:user:add',
  sort        INT DEFAULT 0,
  visible     TINYINT DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单/权限';

-- 用户-角色
CREATE TABLE IF NOT EXISTS sys_user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  INDEX idx_user (user_id),
  INDEX idx_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色';

-- 角色-菜单
CREATE TABLE IF NOT EXISTS sys_role_menu (
  role_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL,
  INDEX idx_role (role_id),
  INDEX idx_menu (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-菜单';

-- 会话表
CREATE TABLE IF NOT EXISTS chat_session (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  type        TINYINT NOT NULL      COMMENT '1单聊 2群聊',
  name        VARCHAR(128),
  avatar      VARCHAR(255),
  owner_id    BIGINT,
  notice      VARCHAR(512)          COMMENT '群公告',
  last_msg    VARCHAR(255),
  last_time   DATETIME,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话';

-- 会话成员
CREATE TABLE IF NOT EXISTS chat_session_member (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id  BIGINT NOT NULL,
  user_id     BIGINT NOT NULL,
  member_role TINYINT DEFAULT 2     COMMENT '1群主 2普通成员',
  unread      INT DEFAULT 0,
  is_top      TINYINT DEFAULT 0,
  is_mute     TINYINT DEFAULT 0     COMMENT '免打扰',
  is_star     TINYINT DEFAULT 0     COMMENT '特别关注',
  join_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_session_user (session_id, user_id),
  INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话成员';

-- 消息表
CREATE TABLE IF NOT EXISTS chat_message (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id   BIGINT NOT NULL,
  sender_id    BIGINT NOT NULL,
  content_type TINYINT DEFAULT 1    COMMENT '1文本 2图片 3文件 4系统',
  content      TEXT,
  extra        VARCHAR(512)         COMMENT '文件信息/引用等 json',
  at_user_ids  VARCHAR(255)         COMMENT '@的用户id, 逗号分隔',
  status       TINYINT DEFAULT 1    COMMENT '1正常 0撤回',
  create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_session_time (session_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息';

-- 消息已读回执
CREATE TABLE IF NOT EXISTS chat_message_read (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  message_id  BIGINT NOT NULL,
  session_id  BIGINT NOT NULL,
  user_id     BIGINT NOT NULL,
  read_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_msg (message_id),
  INDEX idx_session_user (session_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息已读回执';

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
  history_json LONGTEXT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件';

ALTER TABLE sys_file ADD COLUMN IF NOT EXISTS scene VARCHAR(64);
ALTER TABLE sys_file ADD COLUMN IF NOT EXISTS description VARCHAR(255);
ALTER TABLE sys_file ADD COLUMN IF NOT EXISTS status VARCHAR(16);
ALTER TABLE sys_file ADD COLUMN IF NOT EXISTS share_count INT DEFAULT 0;
ALTER TABLE sys_file ADD COLUMN IF NOT EXISTS history_json LONGTEXT;
ALTER TABLE sys_file ADD COLUMN IF NOT EXISTS update_time DATETIME DEFAULT CURRENT_TIMESTAMP;

-- AI 文档分片/向量
CREATE TABLE IF NOT EXISTS ai_file_chunk (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT,
  file_id        BIGINT NOT NULL,
  user_id        BIGINT NOT NULL,
  file_name      VARCHAR(255),
  file_url       VARCHAR(512),
  chunk_index    INT NOT NULL,
  chunk_text     LONGTEXT,
  embedding_json LONGTEXT,
  metadata_json  LONGTEXT,
  create_time    DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time    DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_ai_file_chunk_file_idx (file_id, chunk_index),
  INDEX idx_ai_file_chunk_user (user_id),
  INDEX idx_ai_file_chunk_file (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 文档分片向量';

-- 通知公告
CREATE TABLE IF NOT EXISTS sys_notice (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  title        VARCHAR(128) NOT NULL,
  content      TEXT,
  type         TINYINT DEFAULT 1    COMMENT '1通知 2公告',
  priority     TINYINT DEFAULT 2    COMMENT '1紧急 2普通',
  publisher_id BIGINT,
  status       TINYINT DEFAULT 1    COMMENT '1已发布 0草稿',
  create_time  DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知公告';

CREATE TABLE IF NOT EXISTS sys_notice_read (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  notice_id  BIGINT NOT NULL,
  user_id    BIGINT NOT NULL,
  read_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_notice (notice_id),
  INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知已读';

-- 待办任务
CREATE TABLE IF NOT EXISTS sys_todo (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  title        VARCHAR(128) NOT NULL,
  content      VARCHAR(512),
  creator_id   BIGINT,
  assignee_id  BIGINT,
  priority     TINYINT DEFAULT 2    COMMENT '1高 2中 3低',
  status       TINYINT DEFAULT 0    COMMENT '0待办 1进行中 2完成',
  due_time     DATETIME,
  create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_creator (creator_id),
  INDEX idx_assignee (assignee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='待办';

-- 审批
CREATE TABLE IF NOT EXISTS sys_approval (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  type         VARCHAR(32)          COMMENT 'leave请假 expense报销',
  title        VARCHAR(128),
  content      TEXT                 COMMENT 'JSON: 事由/天数/金额',
  applicant_id BIGINT,
  approver_id  BIGINT,
  status       TINYINT DEFAULT 0    COMMENT '0待审批 1通过 2驳回',
  remark       VARCHAR(255),
  create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
  approve_time DATETIME,
  INDEX idx_applicant (applicant_id),
  INDEX idx_approver (approver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批';

-- 消息收藏
CREATE TABLE IF NOT EXISTS chat_favorite (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id     BIGINT NOT NULL,
  message_id  BIGINT NOT NULL,
  session_id  BIGINT NOT NULL,
  content     TEXT,
  sender_name VARCHAR(64),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息收藏';

-- 添加面板: 已保存链接
CREATE TABLE IF NOT EXISTS chat_saved_link (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id     BIGINT NOT NULL,
  title       VARCHAR(255) NOT NULL,
  url         VARCHAR(1024) NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_url (user_id, url),
  INDEX idx_user_update (user_id, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='添加面板已保存链接';

-- 添加面板: 最近使用
CREATE TABLE IF NOT EXISTS chat_add_usage (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id     BIGINT NOT NULL,
  item_key    VARCHAR(255) NOT NULL,
  use_time    DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_item (user_id, item_key),
  INDEX idx_user_time (user_id, use_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='添加面板最近使用';

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
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日程';

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
  content          LONGTEXT,
  send_time        DATETIME,
  unread           TINYINT DEFAULT 1,
  starred          TINYINT DEFAULT 0,
  archived         TINYINT DEFAULT 0,
  tag_name         VARCHAR(32),
  priority         VARCHAR(16),
  recipients_json  LONGTEXT,
  cc_json          LONGTEXT,
  attachments_json LONGTEXT,
  draft            LONGTEXT,
  use_cases_json   LONGTEXT,
  history_json     LONGTEXT,
  create_time      DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_mail_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮箱邮件';

-- DING
CREATE TABLE IF NOT EXISTS sys_ding (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id        BIGINT NOT NULL,
  owner_user_id  BIGINT,
  target_user_id BIGINT,
  link_key       VARCHAR(64),
  title          VARCHAR(128) NOT NULL,
  content        LONGTEXT,
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
  note           LONGTEXT,
  use_cases_json LONGTEXT,
  history_json   LONGTEXT,
  create_time    DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_ding_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DING 提醒';

-- 消息表情反应
CREATE TABLE IF NOT EXISTS chat_message_reaction (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  message_id  BIGINT NOT NULL,
  user_id     BIGINT NOT NULL,
  emoji       VARCHAR(16) NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_msg (message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表情反应';

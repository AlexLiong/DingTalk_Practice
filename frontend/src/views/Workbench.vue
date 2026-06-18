<template>
  <div class="workbench" :class="{ dark: isDark }">
    <!-- ========== 1. 左侧导航栏 (仿钉钉宽版文字导航 ~130px) ========== -->
    <div class="side-nav">
      <!-- 组织选择器 -->
      <div class="org-header" @click="profilePanel = !profilePanel">
        <div class="nav-avatar-wrap">
          <el-avatar :size="28" shape="square" :src="user?.avatar" :style="avatarStyle(user?.nickname)">{{ firstChar(user?.nickname) }}</el-avatar>
          <span class="nav-status-dot" :class="myStatusClass"></span>
        </div>
        <span class="org-name">{{ user?.deptName || '企业协作' }}</span>
        <el-icon :size="12"><ArrowDown /></el-icon>
      </div>

      <!-- 导航菜单 (可滚动) -->
      <div class="nav-list">
        <div class="nav-row" :class="{ active: tab === 'chat' && chatFilter === 'all' }" @click="tab = 'chat'; chatFilter = 'all'">
          <span class="nav-icon">💬</span><span>消息</span>
          <span v-if="unreadTotal > 0" class="nav-badge">{{ unreadTotal > 99 ? '99+' : unreadTotal }}</span>
        </div>
        <div class="nav-row" @click="$router.push('/documents')">
          <span class="nav-icon">📄</span><span>文档</span>
        </div>
        <div class="nav-row" :class="{ active: tab === 'work' }" @click="tab = 'work'">
          <span class="nav-icon">💼</span><span>工作台</span>
        </div>
        <div class="nav-row" :class="{ active: tab === 'contacts' }" @click="switchContacts">
          <span class="nav-icon">📇</span><span>通讯录</span>
        </div>
        <div class="nav-row" @click="tab = 'chat'; chatFilter = 'atme'">
          <span class="nav-icon">@</span><span>@我</span>
        </div>
        <div class="nav-row" @click="$router.push('/calendar')">
          <span class="nav-icon">📅</span><span>日历</span>
        </div>

        <div class="nav-sep"></div>

        <div class="nav-row" :class="{ active: tab === 'chat' && chatFilter === 'single' }" @click="tab = 'chat'; chatFilter = 'single'">
          <span class="nav-icon">👤</span><span>单聊</span>
        </div>
        <div class="nav-row" :class="{ active: tab === 'chat' && chatFilter === 'group' }" @click="tab = 'chat'; chatFilter = 'group'">
          <span class="nav-icon">👥</span><span>群聊</span>
        </div>
        <div class="nav-row" @click="$router.push('/notice')">
          <span class="nav-icon">🔔</span><span>通知</span>
        </div>

        <div class="nav-sep"></div>

        <div class="nav-row" @click="$router.push('/mailbox')">
          <span class="nav-icon">📧</span><span>邮箱</span>
          <span v-if="collabCounts.mailboxUnread > 0" class="nav-badge">{{ formatCounter(collabCounts.mailboxUnread) }}</span>
        </div>
        <div class="nav-row" @click="$router.push('/todo')">
          <span class="nav-icon">📝</span><span>待办</span>
        </div>
        <div class="nav-row" @click="$router.push('/ding')">
          <span class="nav-icon">🔔</span><span>DING</span>
          <span v-if="collabCounts.dingPending > 0" class="nav-badge">{{ formatCounter(collabCounts.dingPending) }}</span>
        </div>
        <div class="nav-row" @click="$router.push('/favorites')">
          <span class="nav-icon">⭐</span><span>收藏</span>
        </div>
        <div class="nav-row" v-if="isAdmin" @click="$router.push('/admin/dashboard')">
          <span class="nav-icon">📊</span><span>数据</span>
        </div>
        <div class="nav-row" v-if="isAdmin" @click="$router.push('/admin')">
          <span class="nav-icon">⚙️</span><span>管理</span>
        </div>
      </div>

      <!-- 底部按钮 -->
      <div class="nav-footer">
        <div class="nav-row" @click="themeStore.toggle()">
          <span class="nav-icon">{{ isDark ? '☀️' : '🌙' }}</span><span>主题</span>
        </div>
        <div class="nav-row nav-logout" @click="logout">
          <span class="nav-icon">🚪</span><span>退出</span>
        </div>
      </div>
    </div>

    <!-- 个人信息侧边面板 (仿钉钉) -->
    <Transition name="panel-slide">
      <div v-if="profilePanel" class="profile-panel" @click.self="profilePanel = false">
        <div class="pp-content">
          <div class="pp-header">
            <el-upload :show-file-list="false" :http-request="uploadAvatar" accept="image/*" class="pp-avatar-upload">
              <el-avatar :size="56" shape="square" :src="user?.avatar" :style="avatarStyle(user?.nickname)" class="pp-avatar-clickable">{{ firstChar(user?.nickname) }}</el-avatar>
              <div class="pp-avatar-overlay">换头像</div>
            </el-upload>
            <div class="pp-info">
              <div class="pp-name" @click="$router.push('/profile'); profilePanel = false">{{ user?.nickname }} <el-icon :size="14"><ArrowRight /></el-icon></div>
              <div class="pp-dept">{{ user?.deptName || '' }} · {{ user?.jobTitle || '' }}</div>
            </div>
          </div>
          <el-dropdown trigger="click" @command="changeMyStatus">
            <div class="pp-status">
              <span class="status-dot" :class="myStatusClass"></span>{{ statusText }}<el-icon :size="12"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="item in statusOptions" :key="item.value" :command="item.value">
                  <span class="status-option-dot" :class="item.value"></span>{{ item.label }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <div class="pp-divider"></div>
          <div class="pp-menu-item" @click="$router.push('/profile'); profilePanel = false"><el-icon :size="18"><UserFilled /></el-icon> 个人中心</div>
          <div class="pp-menu-item" @click="themeStore.toggle(); profilePanel = false"><el-icon :size="18"><Moon /></el-icon> 个性主题 <span class="pp-tag">{{ isDark ? '深色' : '浅色' }}</span></div>
          <div class="pp-menu-item" v-if="isAdmin" @click="$router.push('/admin'); profilePanel = false"><el-icon :size="18"><Setting /></el-icon> 系统管理</div>
          <div class="pp-divider"></div>
          <div class="pp-menu-item" @click="profilePanel = false"><el-icon :size="18"><QuestionFilled /></el-icon> 客服与帮助</div>
          <div class="pp-menu-item" @click="profilePanel = false"><el-icon :size="18"><InfoFilled /></el-icon> 关于</div>
          <div class="pp-divider"></div>
          <div class="pp-menu-item pp-logout" @click="logout"><el-icon :size="18"><SwitchButton /></el-icon> 退出登录</div>
        </div>
      </div>
    </Transition>

    <!-- ========== 2. 分组筛选面板 (可折叠) ========== -->
    <Transition name="gp-slide">
      <div v-if="tab === 'chat' && groupPanelOpen" class="group-panel">
        <div class="gp-header">
          <el-icon :size="14"><Operation /></el-icon>
          <span>分组</span>
          <el-icon :size="14" class="gp-close" @click="groupPanelOpen = false"><DArrowLeft /></el-icon>
        </div>
        <div class="gp-title">消息</div>
        <div class="gp-list">
          <div class="gp-item" :class="{ active: chatFilter === 'all' }" @click="chatFilter = 'all'">消息</div>
          <div class="gp-item" :class="{ active: chatFilter === 'top' }" @click="chatFilter = 'top'">置顶</div>
          <div class="gp-item" :class="{ active: chatFilter === 'unread' }" @click="chatFilter = 'unread'">
            未读
            <span v-if="unreadTotal > 0" class="gp-badge">{{ unreadTotal > 99 ? '99+' : unreadTotal }}</span>
          </div>
          <div class="gp-item" :class="{ active: chatFilter === 'atme' }" @click="chatFilter = 'atme'">@我</div>
          <div class="gp-item" :class="{ active: chatFilter === 'single' }" @click="chatFilter = 'single'">单聊</div>
          <div class="gp-item" :class="{ active: chatFilter === 'group' }" @click="chatFilter = 'group'">群聊</div>
          <div class="gp-item" @click="$router.push('/notice')">通知</div>
          <div class="gp-item" :class="{ active: chatFilter === 'ai' }" @click="openAiAssistant">AI 助手</div>
          <div class="gp-item" :class="{ active: chatFilter === 'mute' }" @click="chatFilter = 'mute'">免打扰</div>
          <div class="gp-item" :class="{ active: chatFilter === 'star' }" @click="chatFilter = 'star'">特别关注</div>
        </div>
      </div>
    </Transition>
    <!-- 收起时的展开按钮 -->
    <div v-if="tab === 'chat' && !groupPanelOpen" class="gp-toggle" @click="groupPanelOpen = true">
      <el-icon :size="14"><DArrowRight /></el-icon>
    </div>

    <!-- ========== 3. 中栏: 会话列表 / 通讯录 / 工作台 ========== -->
    <div class="mid-panel">
      <div class="mid-header">
        <el-input v-model="keyword" placeholder="搜索" :prefix-icon="Search" size="default" clearable class="search-input" @input="onSearchInput" />
        <div class="add-entry" ref="addWrapRef">
          <el-button class="add-btn" :icon="Plus" circle @click.stop="toggleAddPanel" />
          <div v-if="addPanelOpen" class="add-panel" @click.stop>
            <div class="add-panel-head">
              <div>
                <div class="add-panel-title">添加</div>
                <div class="add-panel-subtitle">支持文件、工作台应用和网页链接</div>
              </div>
              <div class="add-panel-toolbox">
                <button type="button" class="add-panel-settings" :class="{ active: addSettingsOpen }" @click.stop="toggleAddSettings">
                  <span>设置</span>
                </button>
                <div v-if="addSettingsOpen" class="add-settings-menu">
                  <div class="add-settings-group">
                    <div class="add-settings-label">链接</div>
                    <div class="add-settings-card">
                      <button type="button" class="add-settings-item" @click="toggleSavedLinkManage">
                        <span class="add-settings-item-icon theme-link">
                          <el-icon :size="14"><Link /></el-icon>
                        </span>
                        <div class="add-settings-item-main">
                          <div class="add-settings-item-title">{{ manageSavedLinks ? '完成链接管理' : '管理已保存链接' }}</div>
                          <div class="add-settings-item-desc">查看、删除已经保存的网页链接</div>
                        </div>
                        <span class="add-settings-item-meta">{{ manageSavedLinks ? '编辑中' : `${savedLinkItems.length} 项` }}</span>
                        <el-icon class="add-settings-item-arrow" :size="14"><ArrowRight /></el-icon>
                      </button>
                    </div>
                  </div>
                  <div class="add-settings-group">
                    <div class="add-settings-label">内容</div>
                    <div class="add-settings-card">
                      <button type="button" class="add-settings-item" @click="refreshAddFiles">
                        <span class="add-settings-item-icon theme-refresh">
                          <el-icon :size="14"><Refresh /></el-icon>
                        </span>
                        <div class="add-settings-item-main">
                          <div class="add-settings-item-title">刷新最近文件</div>
                          <div class="add-settings-item-desc">重新拉取你最近上传过的文件</div>
                        </div>
                        <span class="add-settings-item-meta">同步</span>
                        <el-icon class="add-settings-item-arrow" :size="14"><ArrowRight /></el-icon>
                      </button>
                      <button type="button" class="add-settings-item danger" :disabled="!recentUsageKeys.length" @click="clearRecentUsage">
                        <span class="add-settings-item-icon theme-danger">
                          <el-icon :size="14"><Delete /></el-icon>
                        </span>
                        <div class="add-settings-item-main">
                          <div class="add-settings-item-title">清空最近使用</div>
                          <div class="add-settings-item-desc">只清理这个弹层里的最近使用记录</div>
                        </div>
                        <span class="add-settings-item-meta">{{ recentUsageKeys.length ? `${recentUsageKeys.length} 条` : '空' }}</span>
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div class="add-link-box">
              <button v-if="!linkEditorOpen" type="button" class="add-link-trigger" @click="openLinkEditor">添加链接</button>
              <div v-else class="add-link-editor">
                <el-input
                  ref="addLinkInputRef"
                  v-model="linkDraft"
                  placeholder="输入网页链接，例如 https://example.com"
                  class="add-link-input"
                  @keyup.enter="saveLink"
                />
                <div class="add-link-actions">
                  <button type="button" class="add-link-cancel" @click="cancelLinkEdit">取消</button>
                  <button type="button" class="add-link-save" @click="saveLink">保存</button>
                </div>
              </div>
            </div>
            <template v-if="manageSavedLinks">
              <div class="add-panel-section">
                <span>已保存链接</span>
                <span class="add-panel-count">{{ savedLinkItems.length }}</span>
              </div>
              <div class="add-panel-list">
                <div v-for="item in savedLinkItems" :key="item.key" class="add-panel-item">
                  <div class="add-item-badge" :style="{ background: item.color }">
                    <template v-if="item.kind === 'file'">
                      <div v-if="item.iconShape === 'tile'" class="add-tile-icon" :class="`theme-${item.iconTheme || 'stack'}`">
                        <span v-if="item.iconTheme === 'stack'" class="add-tile-stack"></span>
                        <span v-else class="add-tile-glyph">{{ item.iconBadge || item.badge }}</span>
                      </div>
                      <div v-else class="add-file-icon" :class="`theme-${item.iconTheme || 'generic'}`">
                        <span class="add-file-fold"></span>
                        <span class="add-file-lines"></span>
                        <span class="add-file-type">{{ item.iconBadge || item.badge }}</span>
                      </div>
                    </template>
                    <template v-else>{{ item.badge }}</template>
                  </div>
                  <div class="add-item-main">
                    <div class="add-item-title">{{ item.title }}</div>
                    <div class="add-item-desc">{{ item.description }}</div>
                  </div>
                  <div class="add-item-actions">
                    <button type="button" class="add-item-action ghost" @click.stop="sendAddItem(item)">添加</button>
                    <button type="button" class="add-item-action delete" @click.stop="removeSavedLink(item)">删除</button>
                  </div>
                </div>
                <el-empty v-if="!savedLinkItems.length" description="暂无已保存链接" :image-size="56" />
              </div>
            </template>
            <template v-else>
              <div class="add-panel-section">
                <span>最近使用</span>
              </div>
              <div class="add-panel-list">
                <div v-for="item in addPanelItems" :key="item.key" class="add-panel-item compact">
                  <div class="add-item-badge" :style="{ background: item.color }">
                    <template v-if="item.kind === 'file'">
                      <div v-if="item.iconShape === 'tile'" class="add-tile-icon" :class="`theme-${item.iconTheme || 'stack'}`">
                        <span v-if="item.iconTheme === 'stack'" class="add-tile-stack"></span>
                        <span v-else class="add-tile-glyph">{{ item.iconBadge || item.badge }}</span>
                      </div>
                      <div v-else class="add-file-icon" :class="`theme-${item.iconTheme || 'generic'}`">
                        <span class="add-file-fold"></span>
                        <span class="add-file-lines"></span>
                        <span class="add-file-type">{{ item.iconBadge || item.badge }}</span>
                      </div>
                    </template>
                    <template v-else>{{ item.badge }}</template>
                  </div>
                  <div class="add-item-main">
                    <div class="add-item-title">{{ item.title }}</div>
                    <div v-if="item.kind === 'link'" class="add-item-desc">{{ item.description }}</div>
                  </div>
                  <button type="button" class="add-item-action" @click.stop="sendAddItem(item)">添加</button>
                </div>
                <el-empty v-if="!addPanelItems.length" description="暂无可添加内容" :image-size="56" />
              </div>
            </template>
            <div class="add-panel-footer">
              <button type="button" class="add-footer-btn" @click="openGroupCreate">发起群聊</button>
              <span class="add-footer-sep"></span>
              <button type="button" class="add-footer-btn" @click="openContactFinder">找人单聊</button>
            </div>
          </div>
        </div>
      </div>

      <!-- 搜索结果 -->
      <div v-if="searchMode" class="list">
        <div class="contact-section">搜索结果 — 联系人</div>
        <div v-for="u in searchContacts" :key="'c'+u.id" class="session-item" @click="startChat(u)">
          <el-avatar :size="36" shape="square" :style="avatarStyle(u.nickname)">{{ firstChar(u.nickname) }}</el-avatar>
          <div class="session-info"><div class="line1"><span class="name">{{ u.nickname }}</span></div><div class="line2">{{ u.deptName }}</div></div>
        </div>
        <div v-if="searchMsgs.length" class="contact-section" style="margin-top:12px">搜索结果 — 消息</div>
        <div v-for="m in searchMsgs" :key="'m'+m.id" class="session-item" @click="jumpToMessage(m)">
          <el-avatar :size="36" shape="square" :style="avatarStyle(m.senderName)">{{ firstChar(m.senderName) }}</el-avatar>
          <div class="session-info"><div class="line1"><span class="name">{{ m.senderName }}</span><span class="time">{{ fmtTime(m.createTime) }}</span></div><div class="line2">{{ displayMessageContent(m) }}</div></div>
        </div>
        <el-empty v-if="!searchContacts.length && !searchMsgs.length" description="无结果" :image-size="60" />
      </div>

      <!-- 会话列表 -->
      <div v-else-if="tab === 'chat'" class="list">
        <div v-for="s in displaySessions" :key="s.id"
             class="session-item" :class="{ active: current?.id === s.id, topped: s.isTop === 1 }"
             @click="openSession(s)" @contextmenu.prevent="onSessionMenu($event, s)">
          <el-badge :value="s.unread" :hidden="!s.unread" :max="99" class="sess-badge">
            <div class="avatar-wrap">
              <el-avatar :size="42" shape="square" :src="s.avatar" :style="avatarStyle(s.name)">{{ firstChar(s.name) }}</el-avatar>
              <span v-if="s.type === 1 && s.targetUserId" class="user-online" :class="presenceClass(s.targetUserId)"></span>
            </div>
          </el-badge>
          <div class="session-info">
            <div class="line1">
              <span class="name">{{ s.name }}</span>
              <span class="time">{{ fmtTime(s.lastTime) }}</span>
            </div>
            <div class="line2">
              <el-tag v-if="s.type === 2" size="small" type="info" effect="plain" class="group-label">内部群</el-tag>
              {{ s.lastMsg || '暂无消息' }}
            </div>
          </div>
          <el-icon v-if="s.isTop === 1" class="pin-icon" :size="12"><Top /></el-icon>
          <span v-if="s.isMute === 1" class="mute-icon">🔕</span>
          <span v-if="s.isStar === 1" class="star-icon">⭐</span>
        </div>
        <el-empty v-if="!displaySessions.length" :description="chatFilter === 'all' ? '暂无会话' : '没有符合条件的会话'" :image-size="80" />
      </div>

      <!-- 通讯录 -->
      <div v-else-if="tab === 'contacts'" class="list">
        <div class="contact-section">企业通讯录 ({{ contacts.length }})</div>
        <div v-for="u in filteredContacts" :key="u.id" class="session-item" @click="startChat(u)">
          <div class="avatar-wrap">
            <el-avatar :size="42" shape="square" :src="u.avatar" :style="avatarStyle(u.nickname)">{{ firstChar(u.nickname) }}</el-avatar>
            <span v-if="u.id" class="user-online" :class="presenceClass(u.id)"></span>
          </div>
          <div class="session-info">
            <div class="line1"><span class="name">{{ u.nickname }}</span><el-tag size="small" :type="presenceTagType(u.id)" effect="plain">{{ presenceTextById(u.id) }}</el-tag></div>
            <div class="line2">{{ u.deptName }} · {{ u.jobTitle }}</div>
          </div>
        </div>
      </div>

      <!-- 工作台 -->
      <div v-else class="list work-grid">
        <div class="grid-title">工作台</div>
        <div class="work-summary-row">
          <button type="button" class="work-summary-card mailbox" @click="$router.push('/mailbox')">
            <div class="work-summary-label">未读邮件</div>
            <div class="work-summary-value">{{ collabCounts.mailboxUnread }}</div>
            <div class="work-summary-desc">{{ collabCounts.mailboxUnread ? '有新邮件待处理' : '当前没有未读邮件' }}</div>
          </button>
          <button type="button" class="work-summary-card ding" @click="$router.push('/ding')">
            <div class="work-summary-label">待处理 DING</div>
            <div class="work-summary-value">{{ collabCounts.dingPending }}</div>
            <div class="work-summary-desc">{{ collabCounts.dingUrgent ? `${collabCounts.dingUrgent} 条紧急` : '处理状态已同步' }}</div>
          </button>
          <button type="button" class="work-summary-card feed" @click="openWorkNotice(latestWorkNotice)">
            <div class="work-summary-label">最新动态</div>
            <div class="work-summary-title">{{ latestWorkNotice?.title || '等待新的协同提醒' }}</div>
            <div class="work-summary-desc">{{ latestWorkNotice?.summary || '邮箱和 DING 的最新变化会实时出现在这里' }}</div>
          </button>
        </div>
        <div class="app-grid">
          <div class="app-card" @click="$router.push('/documents')"><div class="app-icon" style="background:#4c8bf5">📄</div><span>文档中心</span></div>
          <div class="app-card" @click="$router.push('/notice')"><div class="app-icon" style="background:#1677ff">📢</div><span>通知公告</span></div>
          <div class="app-card" @click="$router.push('/mailbox')">
            <div class="app-icon-wrap">
              <div class="app-icon" style="background:#5f6cf8">📧</div>
              <span v-if="collabCounts.mailboxUnread > 0" class="app-badge">{{ formatCounter(collabCounts.mailboxUnread) }}</span>
            </div>
            <span>企业邮箱</span>
          </div>
          <div class="app-card" @click="$router.push('/todo')"><div class="app-icon" style="background:#52c41a">✅</div><span>待办任务</span></div>
          <div class="app-card" @click="$router.push('/ding')">
            <div class="app-icon-wrap">
              <div class="app-icon" style="background:#ff9f1a">🔔</div>
              <span v-if="collabCounts.dingPending > 0" class="app-badge">{{ formatCounter(collabCounts.dingPending) }}</span>
            </div>
            <span>DING 提醒</span>
          </div>
          <div class="app-card" @click="$router.push('/approval')"><div class="app-icon" style="background:#fa8c16">📝</div><span>审批中心</span></div>
          <div class="app-card" @click="$router.push('/calendar')"><div class="app-icon" style="background:#722ed1">📅</div><span>日历</span></div>
          <div class="app-card" @click="$router.push('/favorites')"><div class="app-icon" style="background:#13c2c2">⭐</div><span>收藏</span></div>
          <div v-if="isAdmin" class="app-card" @click="$router.push('/admin/dashboard')"><div class="app-icon" style="background:#722ed1">📊</div><span>数据看板</span></div>
          <div v-if="isAdmin" class="app-card" @click="$router.push('/admin/notice')"><div class="app-icon" style="background:#eb2f96">📋</div><span>公告管理</span></div>
          <div v-if="isAdmin" class="app-card" @click="$router.push('/admin')"><div class="app-icon" style="background:#13c2c2">⚙️</div><span>系统管理</span></div>
        </div>
        <div class="work-feed-card">
          <div class="work-feed-head">
            <span>协同提醒</span>
            <span class="work-feed-meta">{{ workNotices.length ? `${workNotices.length} 条动态` : '实时刷新' }}</span>
          </div>
          <div v-if="workNotices.length" class="work-feed-list">
            <button
              v-for="notice in workNotices"
              :key="notice.id"
              type="button"
              class="work-feed-item"
              @click="openWorkNotice(notice)"
            >
              <span class="work-feed-badge" :class="notice.category">{{ workNoticeCategoryText(notice) }}</span>
              <div class="work-feed-main">
                <div class="work-feed-title">{{ notice.title }}</div>
                <div class="work-feed-desc">{{ notice.summary }}</div>
              </div>
              <span class="work-feed-time">{{ formatWorkNoticeTime(notice.createTime) }}</span>
            </button>
          </div>
          <div v-else class="work-feed-empty">发送邮箱或 DING 后，最新联动会实时显示在这里。</div>
        </div>
      </div>
    </div>

    <!-- ========== 4. 右栏: 聊天窗口 ========== -->
    <div class="chat-panel">
      <template v-if="current">
        <div class="chat-header">
          <div class="ch-title">{{ current.name }}<span v-if="current.type === 2" class="ch-count">(群聊)</span>
            <span v-if="current.type === 1" class="status-tag" :class="presenceClass(current.targetUserId)">{{ presenceTextById(current.targetUserId) }}</span>
          </div>
          <div class="ch-actions">
            <el-icon v-if="current.type === 2" :size="20" @click="openGroupSetting"><Setting /></el-icon>
            <el-icon :size="18"><MoreFilled /></el-icon>
          </div>
        </div>

        <div class="chat-body" ref="bodyRef">
          <template v-for="(m, i) in messages" :key="m.id">
            <div v-if="showTime(i)" class="time-divider"><span>{{ fmtFullTime(m.createTime) }}</span></div>
            <div v-if="m.contentType === 4 || m.status === 0" class="sys-msg">{{ m.status === 0 ? recallText(m) : m.content }}</div>
            <div v-else class="msg-row" :class="{ mine: m.senderId === user?.id }" @mouseenter="hoverId = m.id" @mouseleave="hoverId = null">
              <el-avatar :size="40" shape="square" :src="m.senderAvatar" :style="avatarStyle(m.senderName)">{{ firstChar(m.senderName) }}</el-avatar>
              <div class="msg-content">
                <div v-if="current.type === 2 && m.senderId !== user?.id" class="msg-sender">{{ m.senderName }}</div>
                <div v-if="m._quote" class="quote-block"><span class="q-name">{{ m._quote.senderName }}:</span> {{ m._quote.content?.substring(0, 60) }}</div>
                <div class="bubble-wrap">
                  <div v-if="m.contentType === 1" class="bubble" v-html="renderText(m.content)"></div>
                  <div v-else-if="m.contentType === 2" class="bubble img-bubble"><img :src="m.content" @click="previewImg(m.content)" /></div>
                  <div v-else-if="m.contentType === 3 && isDocumentShareMessage(m)" class="bubble doc-card-bubble" @click="openDocumentMessage(m)">
                    <div class="doc-card-head">
                      <div class="doc-card-icon" :class="`theme-${documentBadgeMeta(m).theme}`">
                        <span class="doc-card-icon-label">{{ documentBadgeMeta(m).badge }}</span>
                      </div>
                      <div class="doc-card-title-wrap">
                        <div class="doc-card-type">协作文档</div>
                        <div class="doc-card-title">{{ documentTitle(m) }}</div>
                      </div>
                      <div class="doc-card-status">{{ documentStatus(m) }}</div>
                    </div>
                    <div class="doc-card-desc">{{ documentDesc(m) }}</div>
                    <div class="doc-card-foot">
                      <div class="doc-card-meta">
                        <span>{{ documentScene(m) }}</span>
                        <span>{{ fmtSize(documentMeta(m).size) }}</span>
                        <span v-if="documentMeta(m).shareCount">{{ `已分享${documentMeta(m).shareCount}次` }}</span>
                      </div>
                      <button type="button" class="doc-card-action" @click.stop="downloadDocumentFile(m)">下载</button>
                    </div>
                  </div>
                  <div v-else-if="m.contentType === 3" class="bubble file-bubble" @click="downloadFile(m)">
                    <el-icon :size="32" color="#1677ff"><Document /></el-icon>
                    <div class="file-info"><div class="file-name">{{ m.content }}</div><div class="file-size">{{ fmtSize(parseExtra(m.extra).size) }}</div></div>
                  </div>
                  <div v-else-if="m.contentType === 6" class="bubble link-card-bubble" @click="openCardMessage(m)">
                    <div class="card-badge" :style="cardBadgeStyle(m)">{{ cardBadgeText(m) }}</div>
                    <div class="card-main">
                      <div class="card-type">{{ cardTypeText(m) }}</div>
                      <div class="card-title">{{ cardTitle(m) }}</div>
                      <div class="card-desc">{{ cardDesc(m) }}</div>
                    </div>
                    <el-icon :size="16" class="card-arrow"><ArrowRight /></el-icon>
                  </div>
                  <!-- 视频消息 contentType=5 -->
                  <div v-else-if="m.contentType === 5" class="bubble video-bubble">
                    <video :src="m.content" controls preload="metadata" class="chat-video"></video>
                    <div class="video-name">{{ parseExtra(m.extra).name || '视频' }}</div>
                  </div>
                  <div v-if="hoverId === m.id" class="msg-actions">
                    <span @click="quoteMsg(m)">引用</span>
                    <span @click="favoriteMsg(m)">收藏</span>
                    <el-popover trigger="click" :width="200" placement="top">
                      <template #reference><span>😀</span></template>
                      <div class="reaction-picker">
                        <span v-for="e in quickReactions" :key="e" class="reaction-emoji" @click="toggleReaction(m.id, e)">{{ e }}</span>
                      </div>
                    </el-popover>
                    <span v-if="m.senderId === user?.id" @click="recall(m)">撤回</span>
                  </div>
                </div>
                <!-- 消息反应展示 -->
                <div v-if="m.reactions && m.reactions.length" class="reaction-bar">
                  <span v-for="r in m.reactions" :key="r.emoji" class="reaction-tag"
                        :class="{ mine: r.userIds?.includes(user?.id) }"
                        @click="toggleReaction(m.id, r.emoji)">
                    {{ r.emoji }} {{ r.count }}
                  </span>
                </div>
                <div v-if="m.senderId === user?.id && current.type === 1" class="read-status">{{ (m.readCount > 0 || readReceipts[m.id]) ? '已读' : '未读' }}</div>
                <div v-if="m.senderId === user?.id && current.type === 2" class="read-status" @click="showReaders(m.id)">{{ (m.readCount || 0) + (readCounts[m.id] || 0) }}人已读</div>
              </div>
            </div>
          </template>
          <el-empty v-if="!messages.length" description="发送第一条消息" :image-size="90" />
        </div>

        <div v-if="quoteRef" class="quote-bar">
          <span>回复 {{ quoteRef.senderName }}: {{ quoteRef.content?.substring(0, 40) }}</span>
          <el-icon @click="quoteRef = null"><Close /></el-icon>
        </div>

        <div class="chat-input">
          <div class="input-toolbar">
            <div class="toolbar-emoji" ref="emojiWrapRef">
              <el-tooltip content="表情包" placement="top">
                <button type="button" class="toolbar-btn" :class="{ active: emojiPanelOpen }" @click.stop="toggleEmojiPanel">
                  <el-icon :size="20"><ChatDotRound /></el-icon>
                </button>
              </el-tooltip>
              <div v-if="emojiPanelOpen" class="emoji-pop" @click.stop>
                <div class="emoji-panel-header">
                  <span class="emoji-panel-title">表情包</span>
                  <button type="button" class="emoji-close" @click="emojiPanelOpen = false">
                    <el-icon :size="14"><Close /></el-icon>
                  </button>
                </div>
                <div class="emoji-panel">
                <div class="emoji-tabs">
                  <span v-for="(cat, i) in emojiCategories" :key="i" class="emoji-tab"
                        :class="{ active: emojiTab === i }" @click="emojiTab = i">{{ cat.icon }}</span>
                </div>
                <div class="emoji-tab-title">{{ activeEmojiCategory.name }}</div>
                <div class="emoji-grid" :class="{ 'sticker-grid': activeEmojiCategory.type === 'sticker' }">
                  <span v-for="e in activeEmojiCategory.list" :key="e" class="emoji-item"
                        :class="{ 'sticker-item': activeEmojiCategory.type === 'sticker' }"
                        @click="insertEmoji(e)">{{ e }}</span>
                </div>
              </div>
              </div>
            </div>
            <el-upload :show-file-list="false" :http-request="sendImage" accept="image/*">
              <el-tooltip content="图片" placement="top"><el-icon :size="20"><Picture /></el-icon></el-tooltip>
            </el-upload>
            <el-upload :show-file-list="false" :http-request="sendVideo" accept="video/*">
              <el-tooltip content="视频" placement="top"><el-icon :size="20"><VideoCamera /></el-icon></el-tooltip>
            </el-upload>
            <el-upload :show-file-list="false" :http-request="sendFile">
              <el-tooltip content="文件" placement="top"><el-icon :size="20"><Folder /></el-icon></el-tooltip>
            </el-upload>
            <el-tooltip v-if="current.type === 2" content="@提醒" placement="top">
              <el-icon :size="20" @click="openAt"><Bell /></el-icon>
            </el-tooltip>
          </div>
          <el-input v-model="draft" type="textarea" :rows="4" resize="none" class="msg-textarea"
                    placeholder="输入消息, 回车发送" ref="inputRef"
                    @keydown.enter.exact.prevent="sendWithAi" />
          <div class="input-foot">
            <span class="foot-tip">Enter 发送 / Shift+Enter 换行</span>
            <el-button type="primary" :disabled="!draft.trim()" @click="sendWithAi">发送</el-button>
          </div>
        </div>
      </template>
      <div v-else class="chat-empty">
        <div class="empty-illustration"><el-icon :size="56"><ChatLineRound /></el-icon></div>
        <p>选择一个会话开始聊天</p>
      </div>
    </div>

    <!-- 弹窗区 -->
    <el-dialog v-model="groupDialog" title="发起群聊" width="460px">
      <el-input v-model="groupForm.name" placeholder="群名称(可选)" style="margin-bottom:12px" />
      <div class="member-pick"><el-checkbox-group v-model="groupForm.memberIds">
        <div v-for="u in contacts" :key="u.id" class="pick-item"><el-checkbox :value="u.id"><el-avatar :size="28" shape="square" :style="avatarStyle(u.nickname)">{{ firstChar(u.nickname) }}</el-avatar><span class="pick-name">{{ u.nickname }}</span></el-checkbox></div>
      </el-checkbox-group></div>
      <template #footer><el-button @click="groupDialog = false">取消</el-button><el-button type="primary" :disabled="!groupForm.memberIds.length" @click="createGroup">创建</el-button></template>
    </el-dialog>

    <el-drawer v-model="settingDrawer" title="群设置" size="340px">
      <div class="setting-block"><div class="block-title">群公告</div>
        <el-input v-model="groupNotice" type="textarea" :rows="3" :readonly="!isGroupOwner" placeholder="暂无群公告" />
        <el-button v-if="isGroupOwner" size="small" type="primary" style="margin-top:8px" @click="saveNotice">保存</el-button>
      </div>
      <div class="setting-block"><div class="block-title">群成员 ({{ groupMembers.length }})<el-button v-if="isGroupOwner" link type="primary" @click="openAddMember">+ 添加</el-button></div>
        <div v-for="m in groupMembers" :key="m.userId" class="member-row">
          <div class="avatar-wrap-sm"><el-avatar :size="32" shape="square" :src="m.avatar" :style="avatarStyle(m.nickname)">{{ firstChar(m.nickname) }}</el-avatar><span v-if="m.userId" class="user-online-sm" :class="presenceClass(m.userId)"></span></div>
          <span class="m-name">{{ m.nickname }}</span>
          <el-tag v-if="m.memberRole === 1" size="small" type="warning">群主</el-tag>
          <span class="spacer"></span>
          <el-button v-if="isGroupOwner && m.memberRole !== 1" link type="danger" size="small" @click="kick(m)">移除</el-button>
        </div>
      </div>
      <div class="setting-block"><el-button v-if="!isGroupOwner" type="danger" plain style="width:100%" @click="quit">退出群聊</el-button></div>
    </el-drawer>
    <el-dialog v-model="addMemberDialog" title="添加成员" width="420px">
      <div class="member-pick"><el-checkbox-group v-model="addMemberIds">
        <div v-for="u in nonMembers" :key="u.id" class="pick-item"><el-checkbox :value="u.id"><el-avatar :size="28" shape="square" :style="avatarStyle(u.nickname)">{{ firstChar(u.nickname) }}</el-avatar><span class="pick-name">{{ u.nickname }}</span></el-checkbox></div>
      </el-checkbox-group></div>
      <template #footer><el-button @click="addMemberDialog = false">取消</el-button><el-button type="primary" :disabled="!addMemberIds.length" @click="confirmAddMember">添加</el-button></template>
    </el-dialog>
    <el-dialog v-model="atDialog" title="选择要@的成员" width="380px">
      <div v-for="m in groupMembers.filter(x => x.userId !== user?.id)" :key="m.userId" class="pick-item" @click="pickAt(m)">
        <el-avatar :size="28" shape="square" :style="avatarStyle(m.nickname)">{{ firstChar(m.nickname) }}</el-avatar><span class="pick-name">{{ m.nickname }}</span>
      </div>
    </el-dialog>
    <el-dialog v-model="readersDialog" title="已读成员" width="360px">
      <div v-for="r in readersList" :key="r.userId" class="pick-item">
        <el-avatar :size="28" shape="square" :style="avatarStyle(r.nickname)">{{ firstChar(r.nickname) }}</el-avatar>
        <span class="pick-name">{{ r.nickname }}</span>
        <span style="color:#999;margin-left:auto;font-size:12px">{{ fmtFullTime(r.readTime) }}</span>
      </div>
    </el-dialog>
    <div v-if="ctxMenu.visible" class="ctx-menu" :style="{ top: ctxMenu.y + 'px', left: ctxMenu.x + 'px' }" @mouseleave="ctxMenu.visible = false">
      <div class="ctx-item" @click="toggleTop(ctxMenu.session)">{{ ctxMenu.session?.isTop === 1 ? '取消置顶' : '📌 置顶' }}</div>
      <div class="ctx-item" @click="toggleMute(ctxMenu.session)">{{ ctxMenu.session?.isMute === 1 ? '取消免打扰' : '🔕 免打扰' }}</div>
      <div class="ctx-item" @click="toggleStar(ctxMenu.session)">{{ ctxMenu.session?.isStar === 1 ? '取消关注' : '⭐ 特别关注' }}</div>
    </div>
    <el-image-viewer v-if="previewUrl" :url-list="[previewUrl]" @close="previewUrl = ''" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElImageViewer } from 'element-plus'
import {
  ChatDotRound, ChatLineRound, User, Grid, Setting, SwitchButton,
  Search, Plus, MoreFilled, Picture, Folder, Bell, Document, Close, Top,
  ArrowRight, ArrowDown, UserFilled, Moon, DataLine, QuestionFilled, InfoFilled,
  Link, Refresh, Delete,
  Operation, DArrowLeft, DArrowRight, VideoCamera
} from '@element-plus/icons-vue'
import { useUserStore } from '../store/user'
import { useThemeStore } from '../store/theme'
import { useCollabStore } from '../store/collab'
import { useUserPreferenceStore } from '../store/userPreference'
import {
  apiSessions, apiMessages, apiSendMessage, apiMarkRead, apiUserList, apiSingleChat,
  apiCreateGroup, apiGroupInfo, apiGroupMembers, apiAddGroupMembers,
  apiRemoveGroupMember, apiQuitGroup, apiUpdateNotice, apiUpload, apiFileList, apiRecall,
  apiReadReceipt, apiMessageReaders, apiSearchMessages, apiToggleTop, apiOnlineUsers,
  apiToggleMute, apiToggleStar, apiAiReply, apiFavoriteAdd,
  apiAddPanelLinks, apiAddPanelSaveLink, apiAddPanelDeleteLink,
  apiAddPanelRecentUsage, apiAddPanelRememberUsage, apiAddPanelClearUsage,
  apiToggleReaction, apiUpdateProfile
} from '../api'
import { connectWs, disconnectWs, removeWsListeners } from '../utils/ws'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const themeStore = useThemeStore()
const collabStore = useCollabStore()
const preferenceStore = useUserPreferenceStore()
const user = computed(() => userStore.user)
const isAdmin = computed(() => userStore.isAdmin)
const isDark = computed(() => themeStore.dark)

const tab = ref('chat')
const keyword = ref('')
const sessions = ref([])
const contacts = ref([])
const current = ref(null)
const messages = ref([])
const draft = ref('')
const bodyRef = ref(null)
const inputRef = ref(null)
const emojiWrapRef = ref(null)
const addWrapRef = ref(null)
const addLinkInputRef = ref(null)
const hoverId = ref(null)
const atUserSet = ref(new Set())
const previewUrl = ref('')
const quoteRef = ref(null)
const profilePanel = ref(false)
const groupPanelOpen = ref(true)
const chatFilter = ref('all')
const statusOptions = [
  { value: 'online', label: '在线' },
  { value: 'busy', label: '忙碌' },
  { value: 'away', label: '离开' },
  { value: 'offline', label: '离线' }
]

const onlineSet = ref(new Set())
const readReceipts = ref({})
const readCounts = ref({})
const readersDialog = ref(false)
const readersList = ref([])
const searchMode = ref(false)
const searchContacts = ref([])
const searchMsgs = ref([])
const ctxMenu = reactive({ visible: false, x: 0, y: 0, session: null })
const groupDialog = ref(false)
const groupForm = ref({ name: '', memberIds: [] })
const settingDrawer = ref(false)
const groupMembers = ref([])
const groupNotice = ref('')
const addMemberDialog = ref(false)
const addMemberIds = ref([])
const atDialog = ref(false)
const emojiPanelOpen = ref(false)
const addPanelOpen = ref(false)
const addSettingsOpen = ref(false)
const manageSavedLinks = ref(false)
const linkEditorOpen = ref(false)
const linkDraft = ref('')
const recentFiles = ref([])
const savedLinks = ref([])
const recentUsageKeys = ref([])
const workbenchStateReady = ref(false)
const preferredSessionId = ref(null)
const emojiTab = ref(0)
const emojiCategories = [
  { icon: '😀', name: '表情', list: ['😀','😃','😄','😁','😆','😂','🤣','😊','😇','🙂','😉','😍','🥰','😘','😋','😛','🤔','🤗','🤩','😎','😢','😭','😤','😡','🥺','😱','😰','🤯','😴','🥱','😷','🤮','🤧','🥵','🥶'] },
  { icon: '🥳', name: '大表情', type: 'sticker', list: ['🥳','🤣','😭','😡','😎','🥺','🤯','🙄','😴','👍','👏','🙏','💪','❤️','💔','🔥','🎉','💯','✨','👀'] },
  { icon: '👍', name: '手势', list: ['👍','👎','👏','🙌','🤝','🤜','🤛','✌️','🤞','🤟','🤘','👌','🤏','👈','👉','👆','👇','☝️','✋','🤚','🖐️','🖖','👋','🤙','💪','🦾','🙏','✍️','🤳','💅'] },
  { icon: '❤️', name: '符号', list: ['❤️','🧡','💛','💚','💙','💜','🖤','🤍','🤎','💔','❣️','💕','💞','💓','💗','💖','💘','💝','💟','☮️','✝️','☪️','🕉️','☸️','✡️','🔯','🕎','☯️','☦️','🛐'] },
  { icon: '🎉', name: '物品', list: ['🎉','🎊','🎈','🎁','🏆','🥇','🥈','🥉','⚽','🏀','🎯','🎮','🎲','🎭','🎨','🎵','🎶','🎤','🎧','🎸','🎺','🎻','🎬','📷','💻','📱','⌚','💡','🔑','🔒'] },
  { icon: '🌟', name: '自然', list: ['☀️','🌙','⭐','🌟','🌈','☁️','⛈️','❄️','🔥','💧','🌊','🌸','🌹','🌻','🌲','🍀','🍁','🍂','🌍','🌙','🐱','🐶','🐼','🦊','🦁','🐸','🦋','🌺','🍎','🍕'] },
  { icon: '😈', name: '趣味', list: ['😈','👻','💀','☠️','👽','🤖','💩','🤡','👹','👺','👿','🧛','🧟','🧞','🧜','🦸','🦹','🧙','🧚','👼','🎅','🤶','🦄','🐲','🦖','🦕','🐉','🔮','🧿','🪬'] }
]
const quickReactions = ['👍','❤️','😂','😮','😢','🎉','🔥','👏']
const presenceValueMap = { online: 1, busy: 2, away: 3, offline: 4 }
const presenceLabelMap = { online: '在线', busy: '忙碌', away: '离开', offline: '离线' }
const chatFilterSet = new Set(['all', 'atme', 'single', 'group', 'unread', 'top', 'mute', 'star', 'ai'])
const collabCounts = collabStore

const filteredSessions = computed(() => sessions.value.filter(s => !keyword.value || (s.name || '').includes(keyword.value)))
const contactMap = computed(() => new Map(contacts.value.map(u => [u.id, u])))
const displaySessions = computed(() => {
  let list = filteredSessions.value
  if (chatFilter.value === 'single') list = list.filter(s => s.type === 1)
  else if (chatFilter.value === 'group') list = list.filter(s => s.type === 2)
  else if (chatFilter.value === 'unread') list = list.filter(s => s.unread > 0)
  else if (chatFilter.value === 'top') list = list.filter(s => s.isTop === 1)
  else if (chatFilter.value === 'mute') list = list.filter(s => s.isMute === 1)
  else if (chatFilter.value === 'star') list = list.filter(s => s.isStar === 1)
  else if (chatFilter.value === 'atme') list = list.filter(s => atMeSessions.value.has(s.id))
  else if (chatFilter.value === 'ai') list = list.filter(s => s.name === 'AI 助手')
  return list
})
const unreadTotal = computed(() => sessions.value.reduce((sum, s) => sum + (s.unread || 0), 0))
// @我: 跟踪哪些会话最近@了当前用户
const atMeSessions = ref(new Set())
const activeEmojiCategory = computed(() => emojiCategories[emojiTab.value] || emojiCategories[0])
const myStatusClass = computed(() => normalizePresence(user.value?.chatStatus))
const statusText = computed(() => presenceLabelMap[myStatusClass.value] || '在线')
const filteredContacts = computed(() => contacts.value.filter(u => !keyword.value || (u.nickname || '').includes(keyword.value)))
const isGroupOwner = computed(() => { const me = groupMembers.value.find(m => m.userId === user.value?.id); return me && me.memberRole === 1 })
const nonMembers = computed(() => { const ids = new Set(groupMembers.value.map(m => m.userId)); return contacts.value.filter(u => !ids.has(u.id)) })
const addAppItems = computed(() => {
  const items = [
    { key: 'app:/documents', kind: 'app', title: '文档中心', description: '查看项目文档与共享文件', path: '/documents', badge: '档', color: '#4c8bf5' },
    { key: 'app:/notice', kind: 'app', title: '通知公告', description: '查看最新通知与公告', path: '/notice', badge: '公', color: '#1677ff' },
    { key: 'app:/mailbox', kind: 'app', title: '企业邮箱', description: '集中查看重要邮件往来', path: '/mailbox', badge: '邮', color: '#5f6cf8' },
    { key: 'app:/todo', kind: 'app', title: '待办任务', description: '处理待办与任务提醒', path: '/todo', badge: '办', color: '#52c41a' },
    { key: 'app:/ding', kind: 'app', title: 'DING 提醒', description: '查看催办、回执与执行状态', path: '/ding', badge: '盯', color: '#ff9f1a' },
    { key: 'app:/approval', kind: 'app', title: '审批中心', description: '发起和处理审批流程', path: '/approval', badge: '批', color: '#fa8c16' },
    { key: 'app:/calendar', kind: 'app', title: '日历', description: '查看日程与会议安排', path: '/calendar', badge: '历', color: '#722ed1' },
    { key: 'app:/favorites', kind: 'app', title: '收藏', description: '打开常用收藏内容', path: '/favorites', badge: '藏', color: '#13c2c2' }
  ]
  if (isAdmin.value) {
    items.push(
      { key: 'app:/admin/dashboard', kind: 'app', title: '数据看板', description: '查看平台运营数据', path: '/admin/dashboard', badge: '数', color: '#531dab' },
      { key: 'app:/admin/notice', kind: 'app', title: '公告管理', description: '维护平台公告内容', path: '/admin/notice', badge: '告', color: '#eb2f96' },
      { key: 'app:/admin', kind: 'app', title: '系统管理', description: '进入后台管理中心', path: '/admin', badge: '管', color: '#08979c' }
    )
  }
  return items
})
const fileAddItems = computed(() => recentFiles.value.map(fileToAddItem))
const savedLinkItems = computed(() => savedLinks.value.map(linkToAddItem))
const addItemMap = computed(() => {
  const map = new Map()
  ;[...fileAddItems.value, ...savedLinkItems.value, ...addAppItems.value].forEach(item => map.set(item.key, item))
  return map
})
const addPanelItems = computed(() => {
  const list = []
  const seen = new Set()
  recentUsageKeys.value.forEach(key => {
    const item = addItemMap.value.get(key)
    if (item && !seen.has(key)) {
      list.push(item)
      seen.add(key)
    }
  })
  ;[...fileAddItems.value, ...savedLinkItems.value, ...addAppItems.value].forEach(item => {
    if (!seen.has(item.key)) {
      list.push(item)
      seen.add(item.key)
    }
  })
  return list.slice(0, 12)
})
const workNotices = computed(() => collabStore.workNotices)
const latestWorkNotice = computed(() => collabStore.latestWorkNotice)

watch(() => preferenceStore.loaded, (loaded) => {
  if (!loaded || workbenchStateReady.value) return
  restoreWorkbenchState()
}, { immediate: true })

watch(() => [route.query.tab, route.query.filter, route.query.session], syncWorkbenchViewFromRoute, { immediate: true })

watch([tab, chatFilter, keyword, () => current.value?.id ?? null], () => {
  if (!workbenchStateReady.value || !preferenceStore.loaded) return
  preferenceStore.setPageState('workbench', {
    tab: tab.value,
    chatFilter: chatFilter.value,
    keyword: keyword.value,
    currentSessionId: current.value?.id ?? null
  })
})

onMounted(async () => {
  document.addEventListener('click', handleGlobalClick)
  themeStore.init()
  if (!userStore.user) await userStore.fetchInfo()
  await loadSessions()
  contacts.value = await apiUserList()
  if (workbenchStateReady.value && keyword.value.trim()) onSearchInput()
  try { const ids = await apiOnlineUsers(); onlineSet.value = new Set(ids) } catch {}
  connectWs(userStore.token, onWsMessage, onWsReceipt, onWsOnline)
})
onUnmounted(() => {
  document.removeEventListener('click', handleGlobalClick)
  removeWsListeners(onWsMessage, onWsReceipt, onWsOnline)
})

function syncWorkbenchViewFromRoute() {
  const queryTab = typeof route.query.tab === 'string' ? route.query.tab : ''
  const queryFilter = typeof route.query.filter === 'string' ? route.query.filter : ''
  const hasExplicitRouteView = Boolean(queryTab) || Boolean(queryFilter) || Boolean(route.query.session)

  if (!hasExplicitRouteView && workbenchStateReady.value) {
    if (keyword.value.trim()) onSearchInput()
    return
  }

  searchMode.value = false
  searchContacts.value = []
  searchMsgs.value = []
  keyword.value = ''

  if (queryTab === 'work') {
    tab.value = 'work'
    chatFilter.value = 'all'
    return
  }

  if (queryTab === 'contacts') {
    tab.value = 'contacts'
    chatFilter.value = 'all'
    return
  }

  tab.value = 'chat'
  chatFilter.value = chatFilterSet.has(queryFilter) ? queryFilter : 'all'
  openSessionFromRoute()
}

function restoreWorkbenchState() {
  const state = preferenceStore.getPageState('workbench')
  if (state.tab === 'work' || state.tab === 'contacts' || state.tab === 'chat') {
    tab.value = state.tab
  }
  if (chatFilterSet.has(state.chatFilter)) {
    chatFilter.value = state.chatFilter
  }
  if (typeof state.keyword === 'string') {
    keyword.value = state.keyword
  }
  if (Number.isFinite(Number(state.currentSessionId)) && Number(state.currentSessionId) > 0) {
    preferredSessionId.value = Number(state.currentSessionId)
  }
  workbenchStateReady.value = true
}

async function loadSessions() {
  sessions.value = await apiSessions()
  collabStore.setMessageUnreadFromSessions(sessions.value)
  await hydrateAtMeSessions()
  await openSessionFromRoute()
  await openStoredSessionIfNeeded()
}
async function switchContacts() { tab.value = 'contacts'; if (!contacts.value.length) contacts.value = await apiUserList() }

async function hydrateAtMeSessions() {
  if (!user.value?.id || !sessions.value.length) {
    atMeSessions.value = new Set()
    return
  }
  const hitSessionIds = await Promise.all(sessions.value.map(async (session) => {
    try {
      const list = await apiMessages(session.id)
      return list.some(msg => String(msg.atUserIds || '').split(',').includes(String(user.value.id))) ? session.id : null
    } catch {
      return null
    }
  }))
  atMeSessions.value = new Set(hitSessionIds.filter(Boolean))
}

async function openSession(s) {
  current.value = s; quoteRef.value = null
  messages.value = (await apiMessages(s.id)).map(hydrateMessage)
  if (s.unread) {
    await apiMarkRead(s.id)
    s.unread = 0
    collabStore.setMessageUnreadFromSessions(sessions.value)
  }
  if (messages.value.length) { apiReadReceipt(s.id, messages.value[messages.value.length - 1].id).catch(() => {}) }
  scrollBottom()
}

async function openSessionFromRoute() {
  const routeSessionId = Number(route.query.session)
  if (!routeSessionId || !sessions.value.length) return
  if (current.value?.id === routeSessionId) return
  const target = sessions.value.find(session => session.id === routeSessionId)
  if (!target) return
  tab.value = 'chat'
  searchMode.value = false
  keyword.value = ''
  await openSession(target)
  const nextQuery = { ...route.query }
  delete nextQuery.session
  router.replace({ path: '/chat', query: nextQuery })
}

async function openStoredSessionIfNeeded() {
  if (route.query.session || !preferredSessionId.value || !sessions.value.length) return
  if (current.value?.id === preferredSessionId.value) return
  const target = sessions.value.find(session => session.id === preferredSessionId.value)
  if (!target) return
  await openSession(target)
}

async function startChat(u) {
  const sid = await apiSingleChat(u.id); await loadSessions()
  const s = sessions.value.find(x => x.id === sid)
  if (s) { tab.value = 'chat'; searchMode.value = false; keyword.value = ''; await openSession(s) }
}

async function send() {
  const content = draft.value.trim(); if (!content || !current.value) return
  const atIds = [...atUserSet.value].join(',')
  const extra = quoteRef.value ? JSON.stringify({ quoteId: quoteRef.value.id, quoteSender: quoteRef.value.senderName, quoteContent: quoteRef.value.content?.substring(0, 100) }) : undefined
  draft.value = ''; atUserSet.value = new Set(); quoteRef.value = null
  await apiSendMessage({ sessionId: current.value.id, content, contentType: 1, atUserIds: atIds, extra })
}
function ensureSessionSelected() {
  if (current.value) return true
  ElMessage.warning('请先选择一个会话')
  return false
}
function normalizeLinkInput(value) {
  const text = (value || '').trim()
  if (!text) return ''
  const normalized = /^https?:\/\//i.test(text) ? text : `https://${text}`
  return new URL(normalized).toString()
}
function formatHost(url) {
  try { return new URL(url).host } catch { return '' }
}
function formatLinkDescription(url) {
  try {
    const parsed = new URL(url)
    return parsed.pathname && parsed.pathname !== '/' ? `${parsed.host}${parsed.pathname}` : parsed.host
  } catch {
    return url
  }
}
function fileToAddItem(file) {
  const meta = fileBadgeMeta(file.name)
  return {
    key: `file:${file.id}`,
    kind: 'file',
    title: file.name,
    description: fmtSize(file.size) || '文件',
    url: file.url,
    size: file.size,
    badge: meta.badge,
    color: meta.color,
    iconTheme: meta.theme,
    iconShape: meta.shape,
    iconBadge: meta.iconBadge
  }
}
function linkToAddItem(link) {
  return {
    id: link.id,
    key: `link:${link.id}`,
    kind: 'link',
    title: link.title || formatHost(link.url) || '网页链接',
    description: formatLinkDescription(link.url),
    url: link.url,
    badge: '链',
    color: '#13c2c2'
  }
}
function fileBadgeMeta(name) {
  const ext = ((name || '').split('.').pop() || '').toUpperCase()
  if (['PDF'].includes(ext)) return { badge: 'PDF', color: 'transparent', theme: 'pdf', shape: 'doc', iconBadge: 'P' }
  if (['XLS', 'XLSX', 'CSV'].includes(ext)) return { badge: 'XLS', color: 'transparent', theme: 'excel', shape: 'doc', iconBadge: 'X' }
  if (['DOC', 'DOCX'].includes(ext)) return { badge: 'DOC', color: 'transparent', theme: 'word', shape: 'doc', iconBadge: 'W' }
  if (['PPT', 'PPTX'].includes(ext)) return { badge: 'PPT', color: 'transparent', theme: 'ppt', shape: 'doc', iconBadge: 'P' }
  if (['ZIP', 'RAR', '7Z', 'TAR', 'GZ'].includes(ext)) return { badge: 'ZIP', color: 'transparent', theme: 'zip', shape: 'doc', iconBadge: 'Z' }
  if (['JS', 'TS', 'JSON', 'XML', 'HTML', 'CSS', 'SCSS', 'JAVA', 'PY', 'VUE', 'MD', 'YAML', 'YML', 'SH', 'SQL'].includes(ext)) {
    return { badge: ext, color: 'transparent', theme: 'code', shape: 'tile', iconBadge: '</>' }
  }
  if (!ext || ext === (name || '').toUpperCase()) return { badge: '文件', color: 'transparent', theme: 'stack', shape: 'tile', iconBadge: '' }
  return { badge: ext.slice(0, 3), color: 'transparent', theme: 'generic', shape: 'doc', iconBadge: ext.slice(0, 1) }
}
async function loadAddPanelData() {
  const [filesResult, linksResult, usageResult] = await Promise.allSettled([
    apiFileList({ limit: 8 }),
    apiAddPanelLinks(),
    apiAddPanelRecentUsage()
  ])
  recentFiles.value = filesResult.status === 'fulfilled' && Array.isArray(filesResult.value) ? filesResult.value : []
  savedLinks.value = linksResult.status === 'fulfilled' && Array.isArray(linksResult.value) ? linksResult.value : []
  recentUsageKeys.value = usageResult.status === 'fulfilled' && Array.isArray(usageResult.value)
    ? usageResult.value.filter(item => typeof item === 'string')
    : []
}
function closeAddPanel() {
  addPanelOpen.value = false
  addSettingsOpen.value = false
  manageSavedLinks.value = false
  linkEditorOpen.value = false
}
async function toggleAddPanel() {
  if (addPanelOpen.value) {
    closeAddPanel()
    return
  }
  addSettingsOpen.value = false
  manageSavedLinks.value = false
  linkEditorOpen.value = false
  await loadAddPanelData()
  addPanelOpen.value = true
}
function toggleAddSettings() {
  addSettingsOpen.value = !addSettingsOpen.value
}
function openLinkEditor() {
  linkEditorOpen.value = true
  nextTick(() => addLinkInputRef.value?.focus?.())
}
function cancelLinkEdit() {
  linkDraft.value = ''
  linkEditorOpen.value = false
}
function toggleSavedLinkManage() {
  manageSavedLinks.value = !manageSavedLinks.value
  linkEditorOpen.value = false
  linkDraft.value = ''
  addSettingsOpen.value = false
}
async function refreshAddFiles() {
  await loadAddPanelData()
  addSettingsOpen.value = false
  ElMessage.success('最近文件已刷新')
}
async function clearRecentUsage() {
  await apiAddPanelClearUsage()
  recentUsageKeys.value = []
  addSettingsOpen.value = false
  ElMessage.success('最近使用已清空')
}
async function removeSavedLink(item) {
  await apiAddPanelDeleteLink(item.id)
  savedLinks.value = savedLinks.value.filter(link => link.id !== item.id)
  recentUsageKeys.value = recentUsageKeys.value.filter(key => key !== item.key)
  ElMessage.success('链接已删除')
}
async function rememberAddUsage(key) {
  recentUsageKeys.value = [key, ...recentUsageKeys.value.filter(item => item !== key)].slice(0, 20)
  await apiAddPanelRememberUsage(key)
}
async function saveLink() {
  let url = ''
  try {
    url = normalizeLinkInput(linkDraft.value)
  } catch {
    ElMessage.error('请输入有效链接')
    return
  }
  if (!url) {
    ElMessage.warning('请输入链接')
    return
  }
  const host = formatHost(url)
  const item = await apiAddPanelSaveLink({
    title: host || '网页链接',
    url
  })
  savedLinks.value = [item, ...savedLinks.value.filter(link => link.id !== item.id)].slice(0, 12)
  linkDraft.value = ''
  linkEditorOpen.value = false
  ElMessage.success('链接已保存')
}
async function sendAddItem(item) {
  if (!ensureSessionSelected()) return
  if (item.kind === 'file') {
    await apiSendMessage({
      sessionId: current.value.id,
      contentType: 3,
      content: item.title,
      extra: JSON.stringify({ url: item.url, size: item.size })
    })
  } else {
    await apiSendMessage({
      sessionId: current.value.id,
      contentType: 6,
      content: item.title,
      extra: JSON.stringify({
        kind: item.kind,
        title: item.title,
        description: item.description,
        url: item.url,
        path: item.path,
        badge: item.badge,
        color: item.color
      })
    })
  }
  try {
    await rememberAddUsage(item.key)
  } catch {
    /* recent usage persistence is best-effort */
  }
  closeAddPanel()
}
function validateUpload(file, type) {
  if (!file) return false
  if (type === 'video') {
    if (!(file.type || '').startsWith('video/')) {
      ElMessage.error('请选择视频文件')
      return false
    }
    if (file.size > 100 * 1024 * 1024) {
      ElMessage.error('视频大小不能超过 100MB')
      return false
    }
  }
  return true
}
async function sendImage(opt) {
  if (!ensureSessionSelected()) return
  const fd = new FormData(); fd.append('file', opt.file)
  const f = await apiUpload(fd)
  await apiSendMessage({ sessionId: current.value.id, contentType: 2, content: f.url, extra: JSON.stringify({ name: f.name, size: f.size }) })
}
async function sendFile(opt) {
  if (!ensureSessionSelected()) return
  const fd = new FormData(); fd.append('file', opt.file)
  const f = await apiUpload(fd)
  await apiSendMessage({ sessionId: current.value.id, contentType: 3, content: f.name, extra: JSON.stringify({ url: f.url, size: f.size }) })
}
async function sendVideo(opt) {
  if (!ensureSessionSelected() || !validateUpload(opt.file, 'video')) return
  const fd = new FormData(); fd.append('file', opt.file)
  const f = await apiUpload(fd, { timeout: 120000 })
  await apiSendMessage({ sessionId: current.value.id, contentType: 5, content: f.url, extra: JSON.stringify({ name: f.name, size: f.size }) })
}
async function recall(m) { await apiRecall(m.id) }

// 消息表情反应
async function toggleReaction(messageId, emoji) {
  await apiToggleReaction({ messageId, emoji })
  // 刷新消息列表获取最新反应
  if (current.value) messages.value = (await apiMessages(current.value.id)).map(hydrateMessage)
}

// 头像上传
async function uploadAvatar(opt) {
  const fd = new FormData(); fd.append('file', opt.file)
  const f = await apiUpload(fd)
  const u = await apiUpdateProfile({ avatar: f.url })
  userStore.setUser(u)
  ElMessage.success('头像已更新')
}
function normalizePresence(value) {
  if (value === 2 || value === 'busy') return 'busy'
  if (value === 3 || value === 'away') return 'away'
  if (value === 4 || value === 'offline') return 'offline'
  return 'online'
}
function storedPresence(userId) {
  if (userId === user.value?.id) return normalizePresence(user.value?.chatStatus)
  return normalizePresence(contactMap.value.get(userId)?.chatStatus)
}
function presenceClass(userId) {
  return storedPresence(userId)
}
function presenceTextById(userId) {
  return presenceLabelMap[presenceClass(userId)] || '在线'
}
function presenceTagType(userId) {
  return { online: 'success', busy: 'danger', away: 'warning', offline: 'info' }[presenceClass(userId)] || 'success'
}
async function changeMyStatus(status) {
  const nextValue = presenceValueMap[status]
  if (!user.value || !nextValue || user.value.chatStatus === nextValue) return
  const updated = await apiUpdateProfile({ chatStatus: nextValue })
  userStore.setUser(updated)
  ElMessage.success('状态已保存')
}
function quoteMsg(m) { quoteRef.value = m }
function toggleEmojiPanel() {
  emojiPanelOpen.value = !emojiPanelOpen.value
}
function insertEmoji(e) {
  draft.value += e
  nextTick(() => inputRef.value?.focus?.())
}
function handleGlobalClick(event) {
  if (emojiWrapRef.value && !emojiWrapRef.value.contains(event.target)) emojiPanelOpen.value = false
  if (addWrapRef.value && !addWrapRef.value.contains(event.target)) closeAddPanel()
  if (ctxMenu.visible) ctxMenu.visible = false
}

function onWsMessage(msg) {
  msg = hydrateMessage(msg)
  // 追踪 @我 的会话
  if (msg.atUserIds && user.value && String(msg.atUserIds).split(',').includes(String(user.value.id))) {
    atMeSessions.value.add(msg.sessionId); atMeSessions.value = new Set(atMeSessions.value)
  }
  if (current.value && msg.sessionId === current.value.id) {
    const idx = messages.value.findIndex(m => m.id === msg.id)
    if (idx >= 0) messages.value[idx] = msg; else { messages.value.push(msg); scrollBottom() }
    if (msg.senderId !== user.value?.id) { apiMarkRead(current.value.id); apiReadReceipt(current.value.id, msg.id).catch(() => {}) }
  }
  refreshSessionMeta(msg)
}
function onWsReceipt(data) {
  if (!current.value || data.sessionId !== current.value.id) return
  if (data.readerId === user.value?.id) return
  if (current.value.type === 1) { messages.value.forEach(m => { if (m.senderId === user.value?.id && m.id <= data.lastMsgId) readReceipts.value[m.id] = true }) }
  else { messages.value.forEach(m => { if (m.senderId === user.value?.id && m.id <= data.lastMsgId) readCounts.value[m.id] = (readCounts.value[m.id] || 0) + 1 }) }
}
function onWsOnline(data) {
  if (data.online) onlineSet.value.add(data.userId); else onlineSet.value.delete(data.userId)
  onlineSet.value = new Set(onlineSet.value)
}
async function refreshSessionMeta(msg) {
  const s = sessions.value.find(x => x.id === msg.sessionId)
  if (s) {
    s.lastMsg = msg.status === 0 ? '[消息已撤回]' : previewMsg(msg); s.lastTime = msg.createTime
    if ((!current.value || current.value.id !== msg.sessionId) && msg.senderId !== user.value?.id) s.unread = (s.unread || 0) + 1
    sessions.value.sort((a, b) => ((b.isTop || 0) - (a.isTop || 0)) || (new Date(b.lastTime) - new Date(a.lastTime)))
    collabStore.setMessageUnreadFromSessions(sessions.value)
  } else await loadSessions()
}

let searchTimer = null
function onSearchInput() {
  clearTimeout(searchTimer)
  if (!keyword.value.trim()) { searchMode.value = false; return }
  searchTimer = setTimeout(async () => {
    searchMode.value = true
    searchContacts.value = contacts.value.filter(u => (u.nickname || '').includes(keyword.value))
    try { searchMsgs.value = await apiSearchMessages(keyword.value) } catch { searchMsgs.value = [] }
  }, 300)
}
async function jumpToMessage(m) {
  const s = sessions.value.find(x => x.id === m.sessionId)
  if (s) { searchMode.value = false; keyword.value = ''; tab.value = 'chat'; await openSession(s) }
}
async function showReaders(msgId) { readersList.value = await apiMessageReaders(msgId); readersDialog.value = true }
async function toggleTop(s) { ctxMenu.visible = false; await apiToggleTop(s.id); await loadSessions() }
async function toggleMute(s) { ctxMenu.visible = false; await apiToggleMute(s.id); await loadSessions() }
async function toggleStar(s) { ctxMenu.visible = false; await apiToggleStar(s.id); await loadSessions() }
function onSessionMenu(e, s) { ctxMenu.visible = true; ctxMenu.x = e.clientX; ctxMenu.y = e.clientY; ctxMenu.session = s }
function formatCounter(value) { return value > 99 ? '99+' : String(value) }
function workNoticeCategoryText(notice) {
  return notice.category === 'ding' ? 'DING' : '邮箱'
}
function formatWorkNoticeTime(value) {
  if (!value) return ''
  return fmtTime(value)
}
function openWorkNotice(notice) {
  if (!notice?.route) return
  router.push(notice.route)
}

// AI 助手
const aiSessionId = ref(null)
async function openAiAssistant() {
  chatFilter.value = 'ai'
  // 找或创建 AI 助手虚拟会话
  let aiSession = sessions.value.find(s => s.name === 'AI 助手')
  if (!aiSession) {
    // 创建一个群聊作为AI助手会话
    const { apiCreateGroup } = await import('../api')
    const sid = await apiCreateGroup({ name: 'AI 助手', memberIds: [user.value.id] })
    await loadSessions()
    aiSession = sessions.value.find(s => s.id === sid)
  }
  if (aiSession) {
    aiSessionId.value = aiSession.id
    await openSession(aiSession)
  }
}

// 覆盖 send，AI 助手会话用特殊发送
const origSend = send
async function sendWithAi() {
  if (current.value && aiSessionId.value && current.value.id === aiSessionId.value) {
    const content = draft.value.trim()
    if (!content) return
    draft.value = ''
    // 先显示用户消息
    const userMsg = hydrateMessage({ id: Date.now(), sessionId: current.value.id, senderId: user.value.id, senderName: user.value.nickname, contentType: 1, content, status: 1, createTime: new Date().toISOString() })
    messages.value.push(userMsg)
    scrollBottom()
    // 调后端 AI 回复
    try {
      await apiAiReply({ sessionId: current.value.id, content })
    } catch (e) { /* WS 会推送 */ }
    return
  }
  await origSend()
}

// 消息收藏
async function favoriteMsg(m) {
  try {
    await apiFavoriteAdd({ messageId: m.id, sessionId: m.sessionId, content: m.content, senderName: m.senderName })
    ElMessage.success('已收藏')
  } catch (e) { /* 已收藏或其他错误, 拦截器已提示 */ }
}

function openGroupCreate() { groupForm.value = { name: '', memberIds: [] }; groupDialog.value = true; closeAddPanel() }
function openContactFinder() { closeAddPanel(); switchContacts() }
async function createGroup() { const sid = await apiCreateGroup(groupForm.value); groupDialog.value = false; ElMessage.success('群聊已创建'); await loadSessions(); const s = sessions.value.find(x => x.id === sid); if (s) { tab.value = 'chat'; await openSession(s) } }
async function openGroupSetting() { settingDrawer.value = true; groupMembers.value = await apiGroupMembers(current.value.id); const info = await apiGroupInfo(current.value.id); groupNotice.value = info.notice || '' }
async function saveNotice() { await apiUpdateNotice(current.value.id, groupNotice.value); ElMessage.success('公告已更新') }
function openAddMember() { addMemberIds.value = []; addMemberDialog.value = true }
async function confirmAddMember() { await apiAddGroupMembers(current.value.id, addMemberIds.value); addMemberDialog.value = false; groupMembers.value = await apiGroupMembers(current.value.id) }
async function kick(m) { await ElMessageBox.confirm(`移除 ${m.nickname}?`); await apiRemoveGroupMember(current.value.id, m.userId); groupMembers.value = await apiGroupMembers(current.value.id) }
async function quit() { await ElMessageBox.confirm('退出群聊?'); await apiQuitGroup(current.value.id); settingDrawer.value = false; current.value = null; await loadSessions() }
async function openAt() { if (!groupMembers.value.length) groupMembers.value = await apiGroupMembers(current.value.id); atDialog.value = true }
function pickAt(m) { draft.value += `@${m.nickname} `; atUserSet.value.add(m.userId); atDialog.value = false }

function logout() { profilePanel.value = false; disconnectWs(); userStore.logout(); router.push('/login') }

function previewImg(url) { previewUrl.value = url }
function triggerDownload(url, name) {
  if (!url) return
  const link = document.createElement('a')
  link.href = url
  if (name) link.download = name
  link.rel = 'noopener'
  document.body.appendChild(link)
  link.click()
  link.remove()
}
function downloadFile(m) { const e = parseExtra(m.extra); if (e.url) triggerDownload(e.url, m.content) }
function parseExtra(extra) { try { return JSON.parse(extra || '{}') } catch { return {} } }
function isDocumentShareMessage(m) {
  const extra = parseExtra(m?.extra)
  return m?.contentType === 3 && extra.source === 'documents' && !!extra.fileId
}
function hydrateMessage(msg) {
  if (!msg) return msg
  const extra = parseExtra(msg.extra)
  if (extra.quoteId) msg._quote = { senderName: extra.quoteSender, content: extra.quoteContent }
  if (msg.contentType === 6) msg._card = extra
  return msg
}
function documentMeta(m) { return parseExtra(m?.extra) }
function documentBadgeMeta(m) { return fileBadgeMeta(documentTitle(m) || m?.content || '') }
function documentTitle(m) { return m?.content || documentMeta(m).name || '协作文档' }
function documentScene(m) { return documentMeta(m).scene || '通用协作' }
function documentStatus(m) { return documentMeta(m).status || '可用' }
function documentDesc(m) { return documentMeta(m).description || '点击查看文档详情' }
function openDocumentMessage(m) {
  const meta = documentMeta(m)
  if (meta.fileId) {
    router.push({ path: '/documents', query: { file: String(meta.fileId) } })
    return
  }
  if (meta.url) triggerDownload(meta.url, documentTitle(m))
}
function downloadDocumentFile(m) {
  const meta = documentMeta(m)
  if (meta.url) triggerDownload(meta.url, documentTitle(m))
}
function cardMeta(m) { return m?._card || parseExtra(m?.extra) }
function cardTypeText(m) { return cardMeta(m).kind === 'app' ? '工作台应用' : '网页链接' }
function cardTitle(m) { return cardMeta(m).title || m.content || '链接卡片' }
function cardDesc(m) {
  const card = cardMeta(m)
  return card.kind === 'app' ? (card.description || card.path || '') : (card.description || card.url || '')
}
function cardBadgeText(m) { return cardMeta(m).badge || (cardMeta(m).kind === 'app' ? '应' : '链') }
function cardBadgeStyle(m) { return { background: cardMeta(m).color || (cardMeta(m).kind === 'app' ? '#1677ff' : '#13c2c2') } }
function openCardMessage(m) {
  const card = cardMeta(m)
  if (card.kind === 'app' && card.path) {
    router.push(card.path)
    return
  }
  if (card.url) window.open(card.url, '_blank')
}

function showTime(i) { if (i === 0) return true; return (new Date(messages.value[i].createTime) - new Date(messages.value[i - 1].createTime)) > 5 * 60 * 1000 }
function recallText(m) { return (m.senderId === user.value?.id ? '你' : (m.senderName || '对方')) + ' 撤回了一条消息' }
function renderText(text) { if (!text) return ''; const esc = text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;'); return esc.replace(/(@\S+)/g, '<span class="at">$1</span>') }
function previewMsg(msg) {
  if (msg.contentType === 2) return '[图片]'
  if (msg.contentType === 3) return isDocumentShareMessage(msg) ? `[文档] ${documentTitle(msg)}`.trim() : '[文件]'
  if (msg.contentType === 5) return '[视频]'
  if (msg.contentType === 6) {
    const card = cardMeta(msg)
    return `${card.kind === 'app' ? '[应用]' : '[链接]'} ${card.title || msg.content || ''}`.trim()
  }
  return msg.content
}
function displayMessageContent(msg) {
  if (msg.contentType === 2) return '[图片]'
  if (msg.contentType === 3) {
    return isDocumentShareMessage(msg)
      ? `[文档] ${documentTitle(msg)}`
      : `[文件] ${parseExtra(msg.extra).name || msg.content || ''}`.trim()
  }
  if (msg.contentType === 5) return `[视频] ${parseExtra(msg.extra).name || '视频消息'}`
  if (msg.contentType === 6) {
    const card = cardMeta(msg)
    return `${card.kind === 'app' ? '[应用]' : '[链接]'} ${card.title || msg.content || ''}`.trim()
  }
  return msg.content || ''
}
function fmtSize(size) { if (!size) return ''; if (size < 1024) return size + 'B'; if (size < 1048576) return (size / 1024).toFixed(1) + 'KB'; return (size / 1048576).toFixed(1) + 'MB' }
function firstChar(name) { return name ? name.charAt(0) : '?' }
function avatarStyle(name) { const colors = ['#1677ff', '#52c41a', '#fa8c16', '#eb2f96', '#722ed1', '#13c2c2']; let hash = 0; for (const c of (name || '?')) hash += c.charCodeAt(0); return { background: colors[hash % colors.length], color: '#fff', fontSize: '16px', fontWeight: 500 } }
function fmtTime(t) { if (!t) return ''; const d = new Date(t), now = new Date(); const hh = String(d.getHours()).padStart(2, '0'), mm = String(d.getMinutes()).padStart(2, '0'); if (d.toDateString() === now.toDateString()) return `${hh}:${mm}`; return `${d.getMonth() + 1}/${d.getDate()}` }
function fmtFullTime(t) { if (!t) return ''; const d = new Date(t), now = new Date(); const hh = String(d.getHours()).padStart(2, '0'), mm = String(d.getMinutes()).padStart(2, '0'); if (d.toDateString() === now.toDateString()) return `${hh}:${mm}`; return `${d.getMonth() + 1}月${d.getDate()}日 ${hh}:${mm}` }
function scrollBottom() { nextTick(() => { if (bodyRef.value) bodyRef.value.scrollTop = bodyRef.value.scrollHeight }) }
</script>

<style scoped>
.workbench { height: 100%; display: flex; background: var(--dt-bg); }

/* ========== 左侧导航栏 (仿钉钉宽版 ~130px) ========== */
.side-nav {
  width: 130px;
  background: var(--dt-list-bg);
  border-right: 1px solid var(--dt-border);
  display: flex;
  flex-direction: column;
  user-select: none;
}
.org-header {
  display: flex; align-items: center; gap: 6px;
  padding: 14px 12px 10px;
  cursor: pointer; border-bottom: 1px solid var(--dt-border);
}
.org-header :deep(.el-avatar) { border-radius: 6px; flex-shrink: 0; }
.org-name { flex: 1; font-size: 12px; font-weight: 600; color: var(--dt-text); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.org-header .el-icon { color: var(--dt-text-secondary); }

.nav-list {
  flex: 1; overflow-y: auto; padding: 6px 0;
}
.nav-list::-webkit-scrollbar { width: 3px; }
.nav-list::-webkit-scrollbar-thumb { background: rgba(0,0,0,.1); border-radius: 3px; }

.nav-row {
  display: flex; align-items: center; gap: 8px;
  padding: 7px 14px;
  font-size: 13px; color: var(--dt-text);
  cursor: pointer; transition: all .15s;
  position: relative;
}
.nav-row:hover { background: var(--dt-hover); }
.nav-row.active {
  color: var(--dt-primary); font-weight: 600;
  background: var(--dt-active);
}
.nav-row.active::before {
  content: '';
  position: absolute; left: 0; top: 50%; transform: translateY(-50%);
  width: 3px; height: 20px; background: var(--dt-primary);
  border-radius: 0 3px 3px 0;
}
.nav-icon { width: 20px; text-align: center; font-size: 14px; flex-shrink: 0; }
.nav-badge {
  margin-left: auto;
  min-width: 18px; height: 18px; line-height: 18px;
  text-align: center; border-radius: 9px;
  background: #f56c6c; color: #fff; font-size: 10px;
  padding: 0 5px;
}
.nav-sep { height: 1px; background: var(--dt-border); margin: 6px 14px; }

.nav-footer {
  border-top: 1px solid var(--dt-border);
  padding: 4px 0;
}
.nav-logout { color: #f56c6c !important; }

/* ========== 分组筛选面板 (仿钉钉竖向 ~96px) ========== */
.group-panel {
  width: 96px;
  background: var(--dt-list-bg);
  border-right: 1px solid var(--dt-border);
  display: flex;
  flex-direction: column;
}
.gp-header {
  display: flex; align-items: center; gap: 4px;
  padding: 12px 10px 4px;
  font-size: 12px; color: var(--dt-text-secondary);
}
.gp-close { margin-left: auto; cursor: pointer; padding: 2px; border-radius: 4px; }
.gp-close:hover { background: var(--dt-hover); color: var(--dt-primary); }

/* 收起时的展开按钮 */
.gp-toggle {
  width: 20px;
  background: var(--dt-list-bg);
  border-right: 1px solid var(--dt-border);
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; color: var(--dt-text-secondary);
  transition: all .2s;
}
.gp-toggle:hover { background: var(--dt-hover); color: var(--dt-primary); }

/* 分组面板折叠动画 */
.gp-slide-enter-active, .gp-slide-leave-active { transition: all .2s ease; overflow: hidden; }
.gp-slide-enter-from, .gp-slide-leave-to { width: 0 !important; min-width: 0 !important; padding: 0 !important; opacity: 0; }
.gp-title {
  padding: 8px 12px 4px;
  font-size: 13px; font-weight: 600; color: var(--dt-text);
}
.gp-list { flex: 1; overflow-y: auto; padding: 2px 0; }
.gp-item {
  padding: 7px 12px;
  font-size: 13px; color: var(--dt-text);
  cursor: pointer; transition: all .15s;
  display: flex; align-items: center; gap: 4px;
  position: relative;
}
.gp-item:hover { background: var(--dt-hover); color: var(--dt-primary); }
.gp-item.active {
  color: var(--dt-primary); font-weight: 600;
  background: var(--dt-active);
}
.gp-item.active::before {
  content: '';
  position: absolute; left: 0; top: 50%; transform: translateY(-50%);
  width: 3px; height: 18px; background: var(--dt-primary);
  border-radius: 0 3px 3px 0;
}
.gp-badge {
  margin-left: auto;
  min-width: 16px; height: 16px; line-height: 16px;
  text-align: center; border-radius: 8px;
  background: #f56c6c; color: #fff; font-size: 10px;
  padding: 0 4px;
}

/* ========== 中栏 ========== */
.mid-panel {
  width: 280px;
  background: var(--dt-list-bg);
  border-right: 1px solid var(--dt-border);
  display: flex; flex-direction: column;
}
.mid-header {
  padding: 12px;
  display: flex; align-items: center; gap: 8px;
  border-bottom: 1px solid var(--dt-border);
}
.search-input :deep(.el-input__wrapper) { background: var(--dt-hover); box-shadow: none; border-radius: 8px; }
.add-btn { background: var(--dt-hover); border: none; color: var(--dt-text-secondary); width: 32px; height: 32px; }
.add-entry { position: relative; }
.add-panel {
  position: absolute;
  top: 44px;
  right: -44px;
  width: min(548px, calc(100vw - 32px));
  max-height: min(720px, calc(100vh - 84px));
  background: var(--dt-bg);
  border: 1px solid rgba(15, 23, 42, .08);
  border-radius: 22px;
  box-shadow: 0 26px 72px rgba(15, 23, 42, .18);
  padding: 28px 24px 18px;
  z-index: 200;
  backdrop-filter: blur(18px);
}
.add-panel-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.add-panel-title {
  font-size: 26px;
  line-height: 1.2;
  font-weight: 700;
  color: var(--dt-text);
}
.add-panel-subtitle {
  font-size: 16px;
  color: #a8adb7;
  margin-top: 8px;
}
.add-panel-toolbox { position: relative; flex-shrink: 0; }
.add-panel-settings {
  height: auto;
  border: none;
  border-radius: 0;
  padding: 2px 0;
  background: transparent;
  color: var(--dt-text);
  cursor: pointer;
  font-size: 17px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  line-height: 1.2;
}
.add-panel-settings:hover, .add-panel-settings.active {
  background: transparent;
  color: #5f6672;
}
.add-settings-menu {
  position: absolute;
  top: 34px;
  right: -6px;
  width: 298px;
  background: rgba(255,255,255,.96);
  border: 1px solid rgba(15, 23, 42, .08);
  border-radius: 20px;
  box-shadow: 0 22px 54px rgba(15, 23, 42, .16);
  padding: 10px;
  z-index: 4;
  backdrop-filter: blur(18px);
}
.add-settings-group {
  padding: 6px;
  border-radius: 16px;
  background: #f7f8fb;
  border: 1px solid #edf0f4;
}
.add-settings-group + .add-settings-group { margin-top: 8px; }
.dark .add-settings-group {
  background: rgba(255,255,255,.03);
  border-color: rgba(255,255,255,.08);
}
.add-settings-label {
  padding: 5px 8px 8px;
  font-size: 11px;
  line-height: 1;
  color: #a7abb2;
}
.add-settings-card {
  border-radius: 14px;
  overflow: hidden;
  background: transparent;
}
.dark .add-settings-card { background: rgba(255,255,255,.03); }
.add-settings-card .add-settings-item + .add-settings-item {
  border-top: 1px solid #edf0f4;
}
.dark .add-settings-card .add-settings-item + .add-settings-item {
  border-top-color: rgba(255,255,255,.06);
}
.add-settings-item {
  width: 100%;
  min-height: 54px;
  border: none;
  border-radius: 12px;
  background: transparent;
  color: var(--dt-text);
  text-align: left;
  padding: 11px 13px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 10px;
  transition: background .16s ease, box-shadow .16s ease;
}
.add-settings-item:hover {
  background: #fff;
  box-shadow: inset 0 0 0 1px #eef2f6;
}
.dark .add-settings-item:hover { background: rgba(255,255,255,.08); }
.add-settings-item:hover .add-settings-item-arrow {
  opacity: 1;
  transform: translateX(1px);
}
.add-settings-item:hover .add-settings-item-icon {
  transform: scale(1.03);
  box-shadow: inset 0 0 0 1px rgba(255,255,255,.2);
}
.add-settings-item-main { display: flex; flex-direction: column; gap: 2px; }
.add-settings-item-icon {
  width: 30px;
  height: 30px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform .16s ease, box-shadow .16s ease;
}
.add-settings-item-icon.theme-link {
  background: rgba(64, 158, 255, .12);
  color: #409eff;
}
.add-settings-item-icon.theme-refresh {
  background: rgba(82, 196, 26, .12);
  color: #52c41a;
}
.add-settings-item-icon.theme-danger {
  background: rgba(245, 108, 108, .12);
  color: #f56c6c;
}
.add-settings-item-title {
  font-size: 13px;
  color: var(--dt-text);
  line-height: 1.3;
  font-weight: 500;
}
.add-settings-item-desc {
  font-size: 11px;
  color: #a7abb2;
  line-height: 1.35;
}
.add-settings-item-meta {
  margin-left: auto;
  font-size: 11px;
  color: #8f97a3;
  white-space: nowrap;
  padding-left: 8px;
}
.add-settings-item-arrow {
  color: #b7bdc6;
  opacity: .58;
  flex-shrink: 0;
  transition: transform .16s ease, opacity .16s ease;
}
.add-settings-item.danger { color: #f56c6c; }
.add-settings-item.danger .add-settings-item-title { color: #f56c6c; }
.add-settings-item:disabled { color: #c0c4cc; cursor: not-allowed; }
.add-settings-item:disabled .add-settings-item-title,
.add-settings-item:disabled .add-settings-item-desc,
.add-settings-item:disabled .add-settings-item-meta,
.add-settings-item:disabled .add-settings-item-arrow { color: #c0c4cc; opacity: .72; }
.add-settings-item:disabled .add-settings-item-icon {
  background: rgba(192,196,204,.18);
  color: #c0c4cc;
  box-shadow: none;
}
.add-settings-item:disabled:hover {
  background: transparent;
  transform: none;
}
.add-link-box {
  margin-top: 28px;
  border: 1px solid #dcdfe5;
  border-radius: 14px;
  background: #fff;
  overflow: hidden;
}
.dark .add-link-box {
  background: rgba(255,255,255,.04);
  border-color: rgba(255,255,255,.12);
}
.add-link-trigger {
  width: 100%;
  height: 64px;
  border: none;
  background: transparent;
  color: var(--dt-text);
  cursor: pointer;
  font-size: 21px;
  font-weight: 600;
  text-align: center;
}
.add-link-trigger:hover { background: rgba(15, 23, 42, .03); }
.add-link-editor { padding: 14px; }
.add-link-input :deep(.el-input__wrapper) {
  border-radius: 12px;
  box-shadow: none;
  background: #f7f8fb;
  min-height: 44px;
}
.add-link-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}
.add-link-cancel, .add-link-save {
  height: 34px;
  border: none;
  border-radius: 10px;
  padding: 0 16px;
  font-size: 14px;
  cursor: pointer;
}
.add-link-cancel {
  background: var(--dt-hover);
  color: var(--dt-text-secondary);
}
.add-link-save {
  background: var(--dt-primary);
  color: #fff;
}
.add-panel-section {
  font-size: 17px;
  font-weight: 600;
  color: #9ba1aa;
  margin: 32px 0 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.add-panel-count {
  font-size: 13px;
  color: #a7abb2;
  font-weight: 500;
}
.add-panel-list {
  max-height: 436px;
  overflow-y: auto;
  padding-right: 4px;
}
.add-panel-list::-webkit-scrollbar { width: 8px; }
.add-panel-list::-webkit-scrollbar-track { background: transparent; }
.add-panel-list::-webkit-scrollbar-thumb {
  background: rgba(15, 23, 42, .12);
  border-radius: 999px;
}
.add-panel-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  border-radius: 18px;
  transition: background .16s ease;
}
.add-panel-item.compact { min-height: 74px; }
.add-panel-item:hover { background: rgba(15, 23, 42, .04); }
.add-item-badge {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.add-tile-icon {
  position: relative;
  width: 36px;
  height: 36px;
  border-radius: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.add-tile-icon.theme-stack {
  background: linear-gradient(180deg, #d5e9ff 0%, #bfe0ff 100%);
}
.add-tile-icon.theme-stack::before,
.add-tile-icon.theme-stack::after,
.add-tile-stack {
  content: '';
  position: absolute;
  width: 14px;
  height: 9px;
  border-radius: 3px;
  background: #2b8cff;
  transform: rotate(-45deg) skewX(-10deg);
}
.add-tile-icon.theme-stack::before { top: 8px; left: 10px; opacity: .45; }
.add-tile-icon.theme-stack::after { top: 12px; left: 10px; opacity: .75; }
.add-tile-stack { top: 16px; left: 10px; }
.add-tile-icon.theme-code {
  background: linear-gradient(135deg, #5ad7f1 0%, #28b4e2 100%);
  color: #fff;
  box-shadow: inset 0 1px 0 rgba(255,255,255,.2);
}
.add-tile-glyph {
  position: relative;
  z-index: 1;
  font-size: 10px;
  line-height: 1;
  font-weight: 700;
  letter-spacing: 0;
}
.add-file-icon {
  position: relative;
  width: 34px;
  height: 38px;
  border-radius: 10px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  border: 1px solid var(--file-border, #e8ebf2);
  box-shadow: 0 1px 3px rgba(15, 23, 42, .08);
  overflow: hidden;
  --file-accent: #909399;
  --file-border: #e8ebf2;
  --file-tint: rgba(144, 147, 153, .08);
}
.add-file-icon::after {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--file-accent);
}
.add-file-icon::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, var(--file-tint) 0%, rgba(255,255,255,0) 38%);
}
.dark .add-file-icon { border-color: rgba(255,255,255,.12); background: rgba(255,255,255,.96); }
.add-file-fold {
  position: absolute;
  top: 0;
  right: 0;
  width: 11px;
  height: 11px;
  background: linear-gradient(135deg, rgba(255,255,255,0) 46%, rgba(0,0,0,.08) 47%, #f4f7fb 48%);
}
.add-file-lines {
  position: absolute;
  left: 11px;
  top: 9px;
  width: 16px;
  height: 12px;
  border-radius: 4px;
  background:
    linear-gradient(#c9d2de, #c9d2de) 0 0 / 100% 2px no-repeat,
    linear-gradient(#dbe2ec, #dbe2ec) 0 5px / 78% 2px no-repeat,
    linear-gradient(#e6ebf2, #e6ebf2) 0 10px / 92% 2px no-repeat;
}
.add-file-type {
  position: absolute;
  left: 4px;
  bottom: 3px;
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 8px;
  font-weight: 700;
  border-radius: 5px;
  letter-spacing: 0;
  box-shadow: 0 1px 2px rgba(0,0,0,.08);
}
.add-file-icon.theme-pdf {
  --file-accent: #f56c6c;
  --file-border: #f3d5d8;
  --file-tint: rgba(245, 108, 108, .16);
}
.add-file-icon.theme-word {
  --file-accent: #1677ff;
  --file-border: #d5e3ff;
  --file-tint: rgba(22, 119, 255, .16);
}
.add-file-icon.theme-excel {
  --file-accent: #52c41a;
  --file-border: #d6efc9;
  --file-tint: rgba(82, 196, 26, .16);
}
.add-file-icon.theme-ppt {
  --file-accent: #fa8c16;
  --file-border: #f5dfc6;
  --file-tint: rgba(250, 140, 22, .18);
}
.add-file-icon.theme-zip {
  --file-accent: #13c2c2;
  --file-border: #ccefee;
  --file-tint: rgba(19, 194, 194, .16);
}
.add-file-icon.theme-generic {
  --file-accent: #909399;
  --file-border: #e1e4ea;
  --file-tint: rgba(144, 147, 153, .14);
}
.add-file-icon .add-file-type { background: var(--file-accent); }
.add-item-main { min-width: 0; flex: 1; }
.add-item-title {
  font-size: 18px;
  font-weight: 500;
  color: var(--dt-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.add-item-desc {
  margin-top: 4px;
  font-size: 13px;
  color: #9aa1ab;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.add-item-action {
  border: none;
  background: transparent;
  color: #9097a1;
  cursor: pointer;
  font-size: 17px;
  font-weight: 500;
  padding: 4px 2px;
  flex-shrink: 0;
}
.add-item-action:hover { color: #606873; opacity: 1; }
.add-item-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}
.add-item-action.ghost {
  color: var(--dt-text-secondary);
}
.add-item-action.delete {
  color: #f56c6c;
}
.add-panel-footer {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid rgba(0,0,0,.05);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}
.dark .add-panel-footer { border-top-color: rgba(255,255,255,.08); }
.add-footer-btn {
  border: none;
  background: transparent;
  color: var(--dt-text-secondary);
  cursor: pointer;
  font-size: 13px;
  padding: 2px 4px;
}
.add-footer-btn:hover { color: var(--dt-primary); }
.add-footer-sep {
  width: 1px;
  height: 12px;
  background: var(--dt-border);
}
.list { flex: 1; overflow-y: auto; }
.contact-section { padding: 14px 16px 6px; font-size: 12px; color: var(--dt-text-secondary); }

.session-item {
  display: flex; align-items: center;
  padding: 11px 14px; cursor: pointer; gap: 11px;
  transition: background .15s;
  border-left: 3px solid transparent;
  position: relative;
}
.session-item:hover { background: var(--dt-hover); }
.session-item.active { background: var(--dt-active); border-left-color: var(--dt-primary); }
.session-item.topped { background: rgba(22,119,255,.03); }
.pin-icon { position: absolute; top: 6px; right: 8px; color: var(--dt-primary); }
.mute-icon { position: absolute; bottom: 6px; right: 8px; font-size: 10px; opacity: .6; }
.star-icon { position: absolute; top: 6px; right: 24px; font-size: 10px; }
.sess-badge :deep(.el-avatar) { border-radius: 9px; }

.avatar-wrap { position: relative; display: inline-block; }
.avatar-wrap-sm { position: relative; display: inline-block; }
.user-online { position: absolute; right: -1px; bottom: -1px; width: 10px; height: 10px; background: #52c41a; border: 2px solid var(--dt-list-bg); border-radius: 50%; }
.user-online-sm { position: absolute; right: -1px; bottom: -1px; width: 8px; height: 8px; background: #52c41a; border: 2px solid var(--dt-list-bg); border-radius: 50%; }
.user-online.busy, .user-online-sm.busy { background: #fa541c; }
.user-online.away, .user-online-sm.away { background: #faad14; }
.user-online.offline, .user-online-sm.offline { background: #bfbfbf; }

.session-info { flex: 1; min-width: 0; }
.line1 { display: flex; justify-content: space-between; align-items: center; }
.name { font-size: 14px; color: var(--dt-text); font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 140px; }
.time { font-size: 11px; color: #bbb; flex-shrink: 0; }
.line2 { font-size: 12px; color: var(--dt-text-secondary); margin-top: 5px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; display: flex; align-items: center; gap: 4px; }
.group-label { transform: scale(.85); transform-origin: left center; }

/* 工作台 */
.work-grid { padding: 16px; }
.grid-title { font-size: 14px; font-weight: 600; color: var(--dt-text-secondary); margin-bottom: 14px; }
.work-summary-row {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
  margin-bottom: 14px;
}
.work-summary-card {
  border: 1px solid var(--dt-border);
  background: var(--dt-bg);
  border-radius: 12px;
  padding: 14px;
  text-align: left;
  cursor: pointer;
  transition: border-color .18s, transform .18s, box-shadow .18s;
}
.work-summary-card:hover {
  border-color: rgba(22,119,255,.3);
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(15, 23, 42, .08);
}
.work-summary-card.mailbox { background: linear-gradient(180deg, rgba(95,108,248,.08) 0%, var(--dt-bg) 100%); }
.work-summary-card.ding { background: linear-gradient(180deg, rgba(255,159,26,.10) 0%, var(--dt-bg) 100%); }
.work-summary-card.feed { background: linear-gradient(180deg, rgba(22,119,255,.08) 0%, var(--dt-bg) 100%); }
.work-summary-label {
  font-size: 12px;
  color: var(--dt-text-secondary);
}
.work-summary-value {
  margin-top: 8px;
  font-size: 28px;
  line-height: 1;
  font-weight: 700;
  color: var(--dt-text);
}
.work-summary-title {
  margin-top: 8px;
  font-size: 15px;
  line-height: 1.4;
  font-weight: 600;
  color: var(--dt-text);
}
.work-summary-desc {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--dt-text-secondary);
}
.app-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.app-card { display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 16px 8px; border-radius: 10px; cursor: pointer; transition: all .2s; }
.app-card:hover { background: var(--dt-hover); }
.app-icon-wrap { position: relative; }
.app-icon { width: 44px; height: 44px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 22px; }
.app-card span { font-size: 12px; color: var(--dt-text-secondary); }
.app-badge {
  position: absolute;
  top: -6px;
  right: -10px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: #f56c6c;
  color: #fff;
  font-size: 10px;
  line-height: 18px;
  text-align: center;
  box-shadow: 0 2px 6px rgba(245, 108, 108, .35);
}
.work-feed-card {
  margin-top: 14px;
  border-radius: 12px;
  border: 1px solid var(--dt-border);
  background: var(--dt-bg);
  overflow: hidden;
}
.work-feed-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px 10px;
  font-size: 14px;
  font-weight: 600;
  color: var(--dt-text);
}
.work-feed-meta {
  font-size: 12px;
  color: var(--dt-text-secondary);
  font-weight: 400;
}
.work-feed-list {
  display: flex;
  flex-direction: column;
  padding: 0 10px 10px;
  gap: 8px;
}
.work-feed-item {
  width: 100%;
  border: none;
  background: transparent;
  padding: 12px 10px;
  border-radius: 10px;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  cursor: pointer;
  text-align: left;
}
.work-feed-item:hover { background: var(--dt-hover); }
.work-feed-badge {
  min-width: 42px;
  height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 11px;
  line-height: 24px;
  text-align: center;
  font-weight: 600;
  flex-shrink: 0;
}
.work-feed-badge.mailbox {
  background: rgba(95,108,248,.12);
  color: #5f6cf8;
}
.work-feed-badge.ding {
  background: rgba(255,159,26,.16);
  color: #d97706;
}
.work-feed-main {
  min-width: 0;
  flex: 1;
}
.work-feed-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--dt-text);
  line-height: 1.5;
}
.work-feed-desc {
  margin-top: 4px;
  font-size: 12px;
  color: var(--dt-text-secondary);
  line-height: 1.6;
}
.work-feed-time {
  font-size: 11px;
  color: var(--dt-text-secondary);
  flex-shrink: 0;
  padding-top: 2px;
}
.work-feed-empty {
  padding: 0 16px 16px;
  font-size: 12px;
  color: var(--dt-text-secondary);
}

/* ========== 聊天区 ========== */
.chat-panel { flex: 1; display: flex; flex-direction: column; background: var(--dt-chat-bg); }
.chat-header { height: 58px; background: var(--dt-bg); border-bottom: 1px solid var(--dt-border); display: flex; align-items: center; justify-content: space-between; padding: 0 22px; }
.ch-title { font-size: 16px; font-weight: 600; color: var(--dt-text); display: flex; align-items: center; gap: 8px; }
.ch-count { color: var(--dt-text-secondary); font-size: 13px; font-weight: 400; }
.status-tag { font-size: 11px; padding: 1px 8px; border-radius: 10px; font-weight: 400; }
.status-tag.online { background: #f6ffed; color: #52c41a; border: 1px solid #b7eb8f; }
.status-tag.busy { background: #fff2f0; color: #fa541c; border: 1px solid #ffbb96; }
.status-tag.away { background: #fffbe6; color: #faad14; border: 1px solid #ffe58f; }
.status-tag.offline { background: #f5f5f5; color: #999; border: 1px solid #d9d9d9; }
.ch-actions { display: flex; gap: 18px; color: var(--dt-text-secondary); }
.ch-actions .el-icon { cursor: pointer; }
.ch-actions .el-icon:hover { color: var(--dt-primary); }

.chat-body { flex: 1; overflow-y: auto; padding: 20px 24px; }
.time-divider { text-align: center; margin: 14px 0; }
.time-divider span { font-size: 12px; color: #b0b3b8; background: rgba(0,0,0,.04); padding: 2px 10px; border-radius: 10px; }
.sys-msg { text-align: center; font-size: 12px; color: #aaa; margin: 12px 0; }

.msg-row { display: flex; gap: 12px; margin-bottom: 16px; }
.msg-row :deep(.el-avatar) { border-radius: 9px; flex-shrink: 0; }
.msg-row.mine { flex-direction: row-reverse; }
.msg-content { max-width: 56%; }
.msg-sender { font-size: 12px; color: var(--dt-text-secondary); margin-bottom: 5px; }
.quote-block { background: rgba(0,0,0,.04); border-left: 3px solid var(--dt-primary); padding: 6px 10px; margin-bottom: 6px; border-radius: 4px; font-size: 12px; color: var(--dt-text-secondary); }
.q-name { color: var(--dt-primary); }
.bubble-wrap { position: relative; display: flex; align-items: flex-start; gap: 8px; }
.msg-row.mine .bubble-wrap { flex-direction: row-reverse; }
.bubble { padding: 10px 14px; background: var(--dt-bubble-other); border-radius: 10px; font-size: 14px; line-height: 1.6; color: var(--dt-text); word-break: break-word; box-shadow: 0 1px 3px rgba(0,0,0,.06); }
.bubble :deep(.at) { color: #1677ff; }
.msg-row.mine .bubble { background: var(--dt-bubble-me); color: #fff; }
.img-bubble { padding: 4px; }
.img-bubble img { max-width: 200px; max-height: 200px; border-radius: 6px; display: block; cursor: pointer; }
.file-bubble { display: flex; align-items: center; gap: 10px; min-width: 200px; cursor: pointer; }
.doc-card-bubble {
  min-width: 250px;
  max-width: 340px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  cursor: pointer;
  background: #fff;
  color: var(--dt-text);
  border: 1px solid rgba(0,0,0,.06);
  padding: 12px 14px;
}
.msg-row.mine .doc-card-bubble {
  background: #fff;
  color: var(--dt-text);
}
.doc-card-head {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.doc-card-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 700;
  font-size: 11px;
}
.doc-card-icon.theme-pdf { background: linear-gradient(135deg, #ff7875, #cf1322); }
.doc-card-icon.theme-word { background: linear-gradient(135deg, #69b1ff, #1d39c4); }
.doc-card-icon.theme-excel { background: linear-gradient(135deg, #95de64, #237804); }
.doc-card-icon.theme-ppt { background: linear-gradient(135deg, #ffbb96, #d46b08); }
.doc-card-icon.theme-zip { background: linear-gradient(135deg, #d3adf7, #722ed1); }
.doc-card-icon.theme-generic,
.doc-card-icon.theme-stack,
.doc-card-icon.theme-code { background: linear-gradient(135deg, #91a1b7, #475467); }
.doc-card-title-wrap {
  min-width: 0;
  flex: 1;
}
.doc-card-type {
  font-size: 11px;
  color: var(--dt-text-secondary);
}
.doc-card-title {
  margin-top: 3px;
  font-size: 14px;
  font-weight: 600;
  color: var(--dt-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.doc-card-status {
  flex-shrink: 0;
  padding: 3px 8px;
  border-radius: 999px;
  background: #f2f4f7;
  font-size: 11px;
  color: #475467;
}
.doc-card-desc {
  font-size: 12px;
  color: #667085;
  line-height: 1.6;
}
.doc-card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.doc-card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 11px;
  color: #98a2b3;
}
.doc-card-action {
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid #d0d5dd;
  background: #fff;
  color: #344054;
  font-size: 12px;
  cursor: pointer;
  transition: all .18s;
}
.doc-card-action:hover {
  border-color: #bcd4ff;
  color: #1677ff;
}
.link-card-bubble {
  min-width: 240px;
  max-width: 320px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  background: #fff;
  color: var(--dt-text);
  border: 1px solid rgba(0,0,0,.06);
}
.msg-row.mine .link-card-bubble {
  background: #fff;
  color: var(--dt-text);
}
.card-badge {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.card-main { min-width: 0; flex: 1; }
.card-type { font-size: 11px; color: var(--dt-text-secondary); }
.card-title {
  margin-top: 3px;
  font-size: 14px;
  color: var(--dt-text);
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-desc {
  margin-top: 4px;
  font-size: 12px;
  color: var(--dt-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-arrow { color: #b7b7b7; flex-shrink: 0; }
.msg-actions { display: flex; gap: 4px; font-size: 11px; }
.msg-actions span { color: #999; cursor: pointer; padding: 2px 6px; background: var(--dt-hover); border-radius: 4px; white-space: nowrap; }
.msg-actions span:hover { color: var(--dt-primary); }
.read-status { font-size: 11px; color: #bbb; margin-top: 4px; text-align: right; cursor: pointer; }

.quote-bar { background: var(--dt-hover); padding: 8px 16px; display: flex; justify-content: space-between; align-items: center; font-size: 12px; color: var(--dt-text-secondary); border-top: 1px solid var(--dt-border); }
.quote-bar .el-icon { cursor: pointer; }

.chat-input { background: var(--dt-bg); border-top: 1px solid var(--dt-border); padding: 0 18px 14px; }
.input-toolbar { display: flex; gap: 18px; padding: 12px 2px 8px; color: var(--dt-text-secondary); align-items: center; }
.toolbar-emoji { position: relative; }
.toolbar-btn {
  width: 28px; height: 28px;
  border: none; border-radius: 8px;
  background: transparent; color: inherit;
  display: inline-flex; align-items: center; justify-content: center;
  padding: 0; cursor: pointer;
}
.toolbar-btn:hover, .toolbar-btn.active { background: var(--dt-hover); color: var(--dt-primary); }
.emoji-pop {
  position: absolute;
  left: 0;
  bottom: 38px;
  width: 380px;
  background: var(--dt-bg);
  border: 1px solid var(--dt-border);
  border-radius: 14px;
  box-shadow: 0 14px 36px rgba(0,0,0,.16);
  padding: 12px;
  z-index: 160;
}
.emoji-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.emoji-panel-title { font-size: 13px; font-weight: 600; color: var(--dt-text); }
.emoji-close {
  width: 24px; height: 24px;
  border: none; border-radius: 6px;
  background: transparent; color: var(--dt-text-secondary);
  display: inline-flex; align-items: center; justify-content: center;
  cursor: pointer; padding: 0;
}
.emoji-close:hover { background: var(--dt-hover); color: var(--dt-primary); }
.input-toolbar .el-icon { cursor: pointer; }
.input-toolbar .el-icon:hover { color: var(--dt-primary); }
.msg-textarea :deep(.el-textarea__inner) { box-shadow: none; resize: none; padding: 4px 2px; font-size: 14px; }
.input-foot { display: flex; justify-content: space-between; align-items: center; margin-top: 6px; }
.foot-tip { font-size: 12px; color: #c0c4cc; }

/* 分类表情面板 */
.emoji-panel { max-height: 320px; }
.emoji-tabs { display: flex; gap: 2px; border-bottom: 1px solid var(--dt-border); padding-bottom: 6px; margin-bottom: 6px; }
.emoji-tab { font-size: 18px; cursor: pointer; padding: 4px 6px; border-radius: 6px; }
.emoji-tab:hover { background: var(--dt-hover); }
.emoji-tab.active { background: var(--dt-active); }
.emoji-tab-title { font-size: 12px; color: var(--dt-text-secondary); margin-bottom: 6px; }
.emoji-grid { display: grid; grid-template-columns: repeat(10, 1fr); gap: 2px; max-height: 200px; overflow-y: auto; }
.sticker-grid { grid-template-columns: repeat(5, 1fr); gap: 8px; }
.emoji-item { font-size: 22px; cursor: pointer; text-align: center; padding: 4px; border-radius: 6px; }
.emoji-item:hover { background: var(--dt-hover); }
.sticker-item {
  font-size: 28px;
  padding: 10px 0;
  background: var(--dt-hover);
}
.sticker-item:hover {
  background: var(--dt-active);
  transform: translateY(-1px);
}

/* 视频消息 */
.video-bubble { padding: 6px; }
.chat-video { max-width: 280px; max-height: 200px; border-radius: 6px; display: block; }
.video-name { font-size: 11px; color: var(--dt-text-secondary); margin-top: 4px; }

/* 消息反应 */
.reaction-picker { display: flex; flex-wrap: wrap; gap: 6px; }
.reaction-emoji { font-size: 24px; cursor: pointer; padding: 4px; border-radius: 6px; }
.reaction-emoji:hover { background: var(--dt-hover); transform: scale(1.2); }
.reaction-bar { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 4px; }
.reaction-tag { display: inline-flex; align-items: center; gap: 2px; padding: 2px 8px; border-radius: 12px; font-size: 12px; background: var(--dt-hover); cursor: pointer; border: 1px solid transparent; }
.reaction-tag:hover { border-color: var(--dt-primary); }
.reaction-tag.mine { background: #e6f0ff; border-color: var(--dt-primary); color: var(--dt-primary); }

/* 头像上传覆盖层 */
.pp-avatar-upload { position: relative; display: inline-block; cursor: pointer; }
.pp-avatar-clickable { cursor: pointer; }
.pp-avatar-overlay { position: absolute; inset: 0; background: rgba(0,0,0,.5); color: #fff; font-size: 11px; display: flex; align-items: center; justify-content: center; border-radius: 12px; opacity: 0; transition: opacity .2s; }
.pp-avatar-upload:hover .pp-avatar-overlay { opacity: 1; }

/* 导航头像状态点 */
.nav-avatar-wrap { position: relative; display: inline-block; }
.nav-status-dot { position: absolute; right: -2px; bottom: -2px; width: 8px; height: 8px; border-radius: 50%; border: 1.5px solid var(--dt-list-bg); }
.nav-status-dot.online { background: #52c41a; }
.nav-status-dot.busy { background: #fa541c; }
.nav-status-dot.away { background: #faad14; }
.nav-status-dot.offline { background: #bfbfbf; }

.chat-empty { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 10px; }
.empty-illustration { width: 96px; height: 96px; border-radius: 50%; background: var(--dt-active); color: var(--dt-primary); display: flex; align-items: center; justify-content: center; }
.chat-empty p { color: var(--dt-text-secondary); }

/* 弹窗/抽屉公用 */
.member-pick { max-height: 320px; overflow-y: auto; }
.pick-item { padding: 8px 4px; cursor: pointer; border-radius: 6px; display: flex; align-items: center; gap: 8px; }
.pick-item:hover { background: var(--dt-hover); }
.pick-name { margin-left: 8px; font-size: 14px; }
.setting-block { margin-bottom: 24px; }
.block-title { font-size: 14px; font-weight: 600; margin-bottom: 10px; display: flex; justify-content: space-between; }
.member-row { display: flex; align-items: center; gap: 10px; padding: 8px 0; }
.m-name { font-size: 14px; }
.spacer { flex: 1; }

/* 右键菜单 */
.ctx-menu { position: fixed; z-index: 9999; background: var(--dt-bg); border: 1px solid var(--dt-border); border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,.15); padding: 4px 0; min-width: 100px; }
.ctx-item { padding: 8px 16px; cursor: pointer; font-size: 13px; color: var(--dt-text); }
.ctx-item:hover { background: var(--dt-hover); }

/* ========== 个人信息侧边面板 ========== */
.profile-panel { position: fixed; inset: 0; z-index: 9000; background: rgba(0,0,0,.25); }
.pp-content { position: absolute; left: 0; top: 0; bottom: 0; width: 280px; background: var(--dt-bg); box-shadow: 4px 0 24px rgba(0,0,0,.18); padding: 24px 20px; overflow-y: auto; }
.pp-header { display: flex; align-items: center; gap: 14px; margin-bottom: 18px; }
.pp-header :deep(.el-avatar) { border-radius: 12px; flex-shrink: 0; }
.pp-info { flex: 1; }
.pp-name { font-size: 18px; font-weight: 600; color: var(--dt-text); display: flex; align-items: center; gap: 6px; cursor: pointer; }
.pp-name:hover { color: var(--dt-primary); }
.pp-dept { font-size: 12px; color: var(--dt-text-secondary); margin-top: 4px; }
.pp-status { display: inline-flex; align-items: center; gap: 6px; padding: 6px 14px; border-radius: 16px; background: var(--dt-hover); font-size: 13px; color: var(--dt-text); cursor: pointer; margin-bottom: 12px; }
.pp-status:hover { background: var(--dt-active); }
.status-dot { width: 8px; height: 8px; border-radius: 50%; }
.status-dot.online { background: #52c41a; }
.status-dot.busy { background: #fa541c; }
.status-dot.away { background: #faad14; }
.status-dot.offline { background: #bfbfbf; }
.status-option-dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; margin-right: 8px; vertical-align: middle; }
.status-option-dot.online { background: #52c41a; }
.status-option-dot.busy { background: #fa541c; }
.status-option-dot.away { background: #faad14; }
.status-option-dot.offline { background: #bfbfbf; }
.pp-divider { height: 1px; background: var(--dt-border); margin: 12px 0; }
.pp-menu-item { display: flex; align-items: center; gap: 12px; padding: 11px 10px; border-radius: 8px; font-size: 14px; color: var(--dt-text); cursor: pointer; transition: background .15s; }
.pp-menu-item:hover { background: var(--dt-hover); }
.pp-menu-item .el-icon { color: var(--dt-text-secondary); }
.pp-tag { margin-left: auto; font-size: 11px; padding: 1px 8px; border-radius: 10px; background: var(--dt-hover); color: var(--dt-text-secondary); }
.pp-logout { color: #f56c6c; }
.pp-logout .el-icon { color: #f56c6c; }
.panel-slide-enter-active, .panel-slide-leave-active { transition: all .25s ease; }
.panel-slide-enter-active .pp-content, .panel-slide-leave-active .pp-content { transition: transform .25s ease; }
.panel-slide-enter-from { opacity: 0; }
.panel-slide-enter-from .pp-content { transform: translateX(-100%); }
.panel-slide-leave-to { opacity: 0; }
.panel-slide-leave-to .pp-content { transform: translateX(-100%); }
</style>

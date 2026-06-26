<template>
  <!-- 桌面端：左侧导航栏 -->
  <div class="side-nav desktop-nav">
    <div class="org-header" @click="profilePanel = !profilePanel">
      <div class="nav-avatar-wrap">
        <el-avatar :size="28" shape="square" :src="user?.avatar" :style="avatarStyle(user?.nickname)">{{ firstChar(user?.nickname) }}</el-avatar>
        <span class="nav-status-dot" :class="myStatusClass"></span>
      </div>
      <span class="org-name">{{ user?.deptName || '企业协作' }}</span>
      <el-icon :size="12"><ArrowDown /></el-icon>
    </div>

    <div class="nav-list">
      <div class="nav-row" :class="{ active: activeKey === 'chat' }" @click="navigateTo('chat')">
        <span class="nav-icon">💬</span><span>消息</span>
        <span v-if="messageUnread > 0" class="nav-badge">{{ formatCounter(messageUnread) }}</span>
      </div>
      <div class="nav-row" :class="{ active: activeKey === 'documents' }" @click="navigateTo('documents')">
        <span class="nav-icon">📄</span><span>文档</span>
      </div>
      <div class="nav-row" :class="{ active: activeKey === 'work' }" @click="navigateTo('work')">
        <span class="nav-icon">💼</span><span>工作台</span>
      </div>
      <div class="nav-row" :class="{ active: activeKey === 'contacts' }" @click="navigateTo('contacts')">
        <span class="nav-icon">📇</span><span>通讯录</span>
      </div>
      <div class="nav-row" :class="{ active: activeKey === 'atme' }" @click="navigateTo('atme')">
        <span class="nav-icon">@</span><span>@我</span>
      </div>
      <div class="nav-row" :class="{ active: activeKey === 'calendar' }" @click="navigateTo('calendar')">
        <span class="nav-icon">📅</span><span>日历</span>
      </div>

      <div class="nav-sep"></div>

      <div class="nav-row" :class="{ active: activeKey === 'single' }" @click="navigateTo('single')">
        <span class="nav-icon">👤</span><span>单聊</span>
      </div>
      <div class="nav-row" :class="{ active: activeKey === 'group' }" @click="navigateTo('group')">
        <span class="nav-icon">👥</span><span>群聊</span>
      </div>
      <div class="nav-row" :class="{ active: activeKey === 'notice' }" @click="navigateTo('notice')">
        <span class="nav-icon">🔔</span><span>通知</span>
      </div>

      <div class="nav-sep"></div>

      <div class="nav-row" :class="{ active: activeKey === 'mailbox' }" @click="navigateTo('mailbox')">
        <span class="nav-icon">📧</span><span>邮箱</span>
        <span v-if="collabStore.mailboxUnread > 0" class="nav-badge">{{ formatCounter(collabStore.mailboxUnread) }}</span>
      </div>
      <div class="nav-row" :class="{ active: activeKey === 'todo' }" @click="navigateTo('todo')">
        <span class="nav-icon">📝</span><span>待办</span>
      </div>
      <div class="nav-row" :class="{ active: activeKey === 'ding' }" @click="navigateTo('ding')">
        <span class="nav-icon">🔔</span><span>DING</span>
        <span v-if="collabStore.dingPending > 0" class="nav-badge">{{ formatCounter(collabStore.dingPending) }}</span>
      </div>
      <div class="nav-row" :class="{ active: activeKey === 'favorites' }" @click="navigateTo('favorites')">
        <span class="nav-icon">⭐</span><span>收藏</span>
      </div>
      <div class="nav-row" :class="{ active: activeKey === 'approval' }" @click="navigateTo('approval')">
        <span class="nav-icon">✅</span><span>审批</span>
      </div>
      <div class="nav-row" :class="{ active: activeKey === 'admin-dashboard' }" @click="navigateTo('admin-dashboard')">
        <span class="nav-icon">📊</span><span>数据</span>
      </div>
      <div v-if="isAdmin" class="nav-row" :class="{ active: activeKey === 'admin' }" @click="navigateTo('admin')">
        <span class="nav-icon">⚙️</span><span>管理</span>
      </div>
    </div>

    <div class="nav-footer">
      <div class="nav-row nav-logout" @click="logout">
        <span class="nav-icon">🚪</span><span>退出</span>
      </div>
    </div>
  </div>

  <!-- 移动端：底部Tab栏 -->
  <div class="mobile-bottom-tab">
    <div class="tab-item" :class="{ active: activeKey === 'chat' }" @click="navigateTo('chat')">
      <span class="tab-icon">💬</span>
      <span class="tab-label">消息</span>
      <span v-if="messageUnread > 0" class="tab-badge">{{ formatCounter(messageUnread) }}</span>
    </div>
    <div class="tab-item" :class="{ active: activeKey === 'documents' }" @click="navigateTo('documents')">
      <span class="tab-icon">📄</span>
      <span class="tab-label">文档</span>
    </div>
    <div class="tab-item" :class="{ active: activeKey === 'work' }" @click="navigateTo('work')">
      <span class="tab-icon">💼</span>
      <span class="tab-label">工作台</span>
    </div>
    <div class="tab-item" :class="{ active: activeKey === 'calendar' }" @click="navigateTo('calendar')">
      <span class="tab-icon">📅</span>
      <span class="tab-label">日历</span>
    </div>
    <div class="tab-item" :class="{ active: ['notice','todo','mailbox','ding','favorites','contacts','atme','single','group','admin','admin-dashboard','approval'].includes(activeKey) }" @click="mobileMenuOpen = true">
      <span class="tab-icon">☰</span>
      <span class="tab-label">更多</span>
    </div>
  </div>

  <!-- 移动端：更多菜单抽屉 -->
  <el-drawer v-model="mobileMenuOpen" direction="btt" size="auto" :show-close="false" class="mobile-menu-drawer">
    <div class="mobile-menu-grid">
      <div class="mobile-menu-item" @click="navigateTo('contacts'); mobileMenuOpen = false">
        <span class="mm-icon">📇</span><span>通讯录</span>
      </div>
      <div class="mobile-menu-item" @click="navigateTo('notice'); mobileMenuOpen = false">
        <span class="mm-icon">🔔</span><span>通知</span>
        <span v-if="collabStore.mailboxUnread > 0" class="mm-badge">{{ formatCounter(collabStore.mailboxUnread) }}</span>
      </div>
      <div class="mobile-menu-item" @click="navigateTo('mailbox'); mobileMenuOpen = false">
        <span class="mm-icon">📧</span><span>邮箱</span>
      </div>
      <div class="mobile-menu-item" @click="navigateTo('todo'); mobileMenuOpen = false">
        <span class="mm-icon">📝</span><span>待办</span>
      </div>
      <div class="mobile-menu-item" @click="navigateTo('ding'); mobileMenuOpen = false">
        <span class="mm-icon">🔔</span><span>DING</span>
        <span v-if="collabStore.dingPending > 0" class="mm-badge">{{ formatCounter(collabStore.dingPending) }}</span>
      </div>
      <div class="mobile-menu-item" @click="navigateTo('favorites'); mobileMenuOpen = false">
        <span class="mm-icon">⭐</span><span>收藏</span>
      </div>
      <div class="mobile-menu-item" @click="navigateTo('approval'); mobileMenuOpen = false">
        <span class="mm-icon">✅</span><span>审批</span>
      </div>
      <div class="mobile-menu-item" @click="navigateTo('admin-dashboard'); mobileMenuOpen = false">
        <span class="mm-icon">📊</span><span>数据</span>
      </div>
      <div v-if="isAdmin" class="mobile-menu-item" @click="navigateTo('admin'); mobileMenuOpen = false">
        <span class="mm-icon">⚙️</span><span>管理</span>
      </div>
      <div class="mobile-menu-item" @click="profilePanel = true; mobileMenuOpen = false">
        <span class="mm-icon">👤</span><span>我的</span>
      </div>
<!--      <div class="mobile-menu-item" @click="themeStore.toggle(); mobileMenuOpen = false">
        <span class="mm-icon">{{ isDark ? '☀️' : '🌙' }}</span><span>主题</span>
      </div> -->
    </div>
  </el-drawer>

  <Transition name="panel-slide">
    <div v-if="profilePanel" class="profile-panel" @click.self="profilePanel = false">
      <div class="pp-content">
        <div class="pp-header">
          <el-upload :show-file-list="false" :http-request="uploadAvatar" accept="image/*" class="pp-avatar-upload">
            <el-avatar :size="56" shape="square" :src="user?.avatar" :style="avatarStyle(user?.nickname)" class="pp-avatar-clickable">{{ firstChar(user?.nickname) }}</el-avatar>
            <div class="pp-avatar-overlay">换头像</div>
          </el-upload>
          <div class="pp-info">
            <div class="pp-name" @click="openProfile">{{ user?.nickname }} <el-icon :size="14"><ArrowRight /></el-icon></div>
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
        <div class="pp-menu-item" @click="openProfile"><el-icon :size="18"><UserFilled /></el-icon> 个人中心</div>
        <div class="pp-menu-item" @click="toggleThemeFromPanel"><el-icon :size="18"><Moon /></el-icon> 个性主题 <span class="pp-tag">{{ isDark ? '深色' : '浅色' }}</span></div>
        <div v-if="isAdmin" class="pp-menu-item" @click="openAdmin"><el-icon :size="18"><Setting /></el-icon> 系统管理</div>
        <div class="pp-divider"></div>
        <div class="pp-menu-item" @click="profilePanel = false"><el-icon :size="18"><QuestionFilled /></el-icon> 客服与帮助</div>
        <div class="pp-menu-item" @click="profilePanel = false"><el-icon :size="18"><InfoFilled /></el-icon> 关于</div>
        <div class="pp-divider"></div>
        <div class="pp-menu-item pp-logout" @click="logout"><el-icon :size="18"><SwitchButton /></el-icon> 退出登录</div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowDown,
  ArrowRight,
  InfoFilled,
  Moon,
  QuestionFilled,
  Setting,
  SwitchButton,
  UserFilled
} from '@element-plus/icons-vue'
import { apiUpdateProfile, apiUpload } from '../api'
import { useCollabStore } from '../store/collab'
import { useThemeStore } from '../store/theme'
import { useUserStore } from '../store/user'

const props = defineProps({
  activeKey: {
    type: String,
    required: true
  },
  chatUnread: {
    type: Number,
    default: null
  }
})

const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()
const collabStore = useCollabStore()

const profilePanel = ref(false)
const mobileMenuOpen = ref(false)

const user = computed(() => userStore.user)
const isAdmin = computed(() => userStore.isAdmin)
const isDark = computed(() => themeStore.dark)
const messageUnread = computed(() => typeof props.chatUnread === 'number' ? props.chatUnread : collabStore.messageUnread)
const myStatusClass = computed(() => normalizePresence(user.value?.chatStatus))
const statusText = computed(() => presenceLabelMap[myStatusClass.value] || '在线')

const statusOptions = [
  { value: 'online', label: '在线' },
  { value: 'busy', label: '忙碌' },
  { value: 'away', label: '离开' },
  { value: 'offline', label: '离线' }
]

const presenceValueMap = { online: 1, busy: 2, away: 3, offline: 4 }
const presenceLabelMap = { online: '在线', busy: '忙碌', away: '离开', offline: '离线' }

onMounted(async () => {
  if (!userStore.user && userStore.token) {
    try { await userStore.fetchInfo() } catch {}
  }
})

function navigateTo(key) {
  profilePanel.value = false
  if (key === 'chat') router.push('/chat')
  else if (key === 'documents') router.push('/documents')
  else if (key === 'work') router.push({ path: '/chat', query: { tab: 'work' } })
  else if (key === 'contacts') router.push({ path: '/chat', query: { tab: 'contacts' } })
  else if (key === 'atme') router.push({ path: '/chat', query: { filter: 'atme' } })
  else if (key === 'calendar') router.push('/calendar')
  else if (key === 'single') router.push({ path: '/chat', query: { filter: 'single' } })
  else if (key === 'group') router.push({ path: '/chat', query: { filter: 'group' } })
  else if (key === 'notice') router.push('/notice')
  else if (key === 'mailbox') router.push('/mailbox')
  else if (key === 'todo') router.push('/todo')
  else if (key === 'ding') router.push('/ding')
  else if (key === 'favorites') router.push('/favorites')
  else if (key === 'approval') router.push('/approval')
  else if (key === 'admin-dashboard') router.push('/admin/dashboard')
  else if (key === 'admin') router.push('/admin')
}

async function uploadAvatar(option) {
  const fd = new FormData()
  fd.append('file', option.file)
  const file = await apiUpload(fd)
  const updated = await apiUpdateProfile({ avatar: file.url })
  userStore.setUser(updated)
  ElMessage.success('头像已更新')
}

function normalizePresence(value) {
  if (value === 2 || value === 'busy') return 'busy'
  if (value === 3 || value === 'away') return 'away'
  if (value === 4 || value === 'offline') return 'offline'
  return 'online'
}

async function changeMyStatus(status) {
  const nextValue = presenceValueMap[status]
  if (!user.value || !nextValue || user.value.chatStatus === nextValue) return
  const updated = await apiUpdateProfile({ chatStatus: nextValue })
  userStore.setUser(updated)
  ElMessage.success('状态已保存')
}

function openProfile() {
  profilePanel.value = false
  router.push('/profile')
}

function toggleThemeFromPanel() {
  themeStore.toggle()
  profilePanel.value = false
}

function openAdmin() {
  profilePanel.value = false
  router.push('/admin')
}

function logout() {
  profilePanel.value = false
  userStore.logout()
  router.push('/login')
}

function formatCounter(value) {
  return value > 99 ? '99+' : String(value)
}

function firstChar(name) {
  return name ? name.charAt(0) : '?'
}

function avatarStyle(name) {
  const colors = ['#1677ff', '#52c41a', '#fa8c16', '#eb2f96', '#722ed1', '#13c2c2']
  let hash = 0
  for (const ch of (name || '?')) hash += ch.charCodeAt(0)
  return { background: colors[hash % colors.length], color: '#fff', fontSize: '16px', fontWeight: 500 }
}
</script>

<style scoped>
.side-nav {
  width: 130px;
  background: var(--dt-list-bg);
  border-right: 1px solid var(--dt-border);
  display: flex;
  flex-direction: column;
  user-select: none;
  flex-shrink: 0;
}

/* 移动端隐藏桌面导航 */
.mobile-bottom-tab { display: none; }
@media (max-width: 768px) {
  .desktop-nav { display: none; }
  .mobile-bottom-tab {
    display: flex;
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    height: var(--dt-bottom-tab-height);
    background: var(--dt-list-bg);
    border-top: 1px solid var(--dt-border);
    z-index: 800;
    padding-bottom: env(safe-area-inset-bottom, 0);
  }
  .tab-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 2px;
    font-size: 10px;
    color: var(--dt-text-secondary);
    cursor: pointer;
    position: relative;
    -webkit-tap-highlight-color: transparent;
  }
  .tab-item.active { color: var(--dt-primary); }
  .tab-icon { font-size: 20px; line-height: 1; }
  .tab-label { font-size: 10px; }
  .tab-badge {
    position: absolute;
    top: 2px;
    right: 50%;
    transform: translateX(14px);
    min-width: 16px;
    height: 16px;
    line-height: 16px;
    text-align: center;
    border-radius: 8px;
    background: #f56c6c;
    color: #fff;
    font-size: 9px;
    padding: 0 4px;
  }
}

/* 移动端更多菜单 */
.mobile-menu-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  padding: 12px 8px 24px;
}
.mobile-menu-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 4px;
  font-size: 12px;
  color: var(--dt-text);
  border-radius: 10px;
  cursor: pointer;
  position: relative;
  transition: background .15s;
}
.mobile-menu-item:active { background: var(--dt-hover); }
.mm-icon { font-size: 24px; }
.mm-badge {
  position: absolute;
  top: 6px;
  right: 6px;
  min-width: 16px;
  height: 16px;
  line-height: 16px;
  text-align: center;
  border-radius: 8px;
  background: #f56c6c;
  color: #fff;
  font-size: 9px;
  padding: 0 4px;
}
.org-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 14px 12px 10px;
  cursor: pointer;
  border-bottom: 1px solid var(--dt-border);
}
.org-header :deep(.el-avatar) {
  border-radius: 6px;
  flex-shrink: 0;
}
.org-name {
  flex: 1;
  font-size: 12px;
  font-weight: 600;
  color: var(--dt-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.org-header .el-icon {
  color: var(--dt-text-secondary);
}
.nav-avatar-wrap {
  position: relative;
  display: inline-flex;
}
.nav-status-dot {
  position: absolute;
  right: -2px;
  bottom: -2px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  border: 1.5px solid var(--dt-list-bg);
}
.nav-status-dot.online { background: #52c41a; }
.nav-status-dot.busy { background: #fa541c; }
.nav-status-dot.away { background: #faad14; }
.nav-status-dot.offline { background: #bfbfbf; }

.nav-list {
  flex: 1;
  overflow-y: auto;
  padding: 6px 0;
}
.nav-list::-webkit-scrollbar { width: 3px; }
.nav-list::-webkit-scrollbar-thumb { background: rgba(0,0,0,.1); border-radius: 3px; }

.nav-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 14px;
  font-size: 13px;
  color: var(--dt-text);
  cursor: pointer;
  transition: all .15s;
  position: relative;
}
.nav-row:hover { background: var(--dt-hover); }
.nav-row.active {
  color: var(--dt-primary);
  font-weight: 600;
  background: var(--dt-active);
}
.nav-row.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  background: var(--dt-primary);
  border-radius: 0 3px 3px 0;
}
.nav-icon {
  width: 20px;
  text-align: center;
  font-size: 14px;
  flex-shrink: 0;
}
.nav-badge {
  margin-left: auto;
  min-width: 18px;
  height: 18px;
  line-height: 18px;
  text-align: center;
  border-radius: 9px;
  background: #f56c6c;
  color: #fff;
  font-size: 10px;
  padding: 0 5px;
}
.nav-sep {
  height: 1px;
  background: var(--dt-border);
  margin: 6px 14px;
}
.nav-footer {
  border-top: 1px solid var(--dt-border);
  padding: 4px 0;
}
.nav-logout { color: #f56c6c !important; }

.profile-panel {
  position: fixed;
  inset: 0;
  z-index: 9000;
  background: rgba(0,0,0,.25);
}
.pp-content {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 280px;
  background: var(--dt-bg);
  box-shadow: 4px 0 24px rgba(0,0,0,.18);
  padding: 24px 20px;
  overflow-y: auto;
}
@media (max-width: 768px) {
  .pp-content {
    width: 100%;
    max-width: 320px;
  }
}
.pp-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 18px;
}
.pp-header :deep(.el-avatar) {
  border-radius: 12px;
  flex-shrink: 0;
}
.pp-info { flex: 1; }
.pp-name {
  font-size: 18px;
  font-weight: 600;
  color: var(--dt-text);
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}
.pp-name:hover { color: var(--dt-primary); }
.pp-dept {
  font-size: 12px;
  color: var(--dt-text-secondary);
  margin-top: 4px;
}
.pp-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 16px;
  background: var(--dt-hover);
  font-size: 13px;
  color: var(--dt-text);
  cursor: pointer;
  margin-bottom: 12px;
}
.pp-status:hover { background: var(--dt-active); }
.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}
.status-dot.online { background: #52c41a; }
.status-dot.busy { background: #fa541c; }
.status-dot.away { background: #faad14; }
.status-dot.offline { background: #bfbfbf; }
.status-option-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
  margin-right: 8px;
  vertical-align: middle;
}
.status-option-dot.online { background: #52c41a; }
.status-option-dot.busy { background: #fa541c; }
.status-option-dot.away { background: #faad14; }
.status-option-dot.offline { background: #bfbfbf; }
.pp-divider {
  height: 1px;
  background: var(--dt-border);
  margin: 12px 0;
}
.pp-menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 10px;
  border-radius: 8px;
  font-size: 14px;
  color: var(--dt-text);
  cursor: pointer;
  transition: background .15s;
}
.pp-menu-item:hover { background: var(--dt-hover); }
.pp-menu-item .el-icon { color: var(--dt-text-secondary); }
.pp-tag {
  margin-left: auto;
  font-size: 11px;
  padding: 1px 8px;
  border-radius: 10px;
  background: var(--dt-hover);
  color: var(--dt-text-secondary);
}
.pp-logout { color: #f56c6c; }
.pp-logout .el-icon { color: #f56c6c; }

.panel-slide-enter-active, .panel-slide-leave-active { transition: all .25s ease; }
.panel-slide-enter-active .pp-content, .panel-slide-leave-active .pp-content { transition: transform .25s ease; }
.panel-slide-enter-from { opacity: 0; }
.panel-slide-enter-from .pp-content { transform: translateX(-100%); }
.panel-slide-leave-to { opacity: 0; }
.panel-slide-leave-to .pp-content { transform: translateX(-100%); }
</style>

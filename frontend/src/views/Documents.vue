<template>
  <div class="documents-shell">
    <AppSideNav active-key="documents" />

    <div class="documents-page">
      <div class="documents-top">
        <div class="documents-top-inner">
          <div class="page-title-cluster">
            <el-button class="page-back-btn" :icon="Back" @click="router.push('/chat')">返回消息</el-button>
            <div class="page-title-block">
              <div class="page-section-label">协同应用</div>
              <div class="page-heading-row">
                <span class="ptitle">文档中心</span>
              </div>
            </div>
          </div>
          <div class="page-top-actions">
            <el-button plain :icon="Refresh" @click="loadFiles">刷新</el-button>
            <el-upload
              :show-file-list="false"
              :http-request="handleUpload"
              accept=".pdf,.doc,.docx,.xls,.xlsx,.csv,.ppt,.pptx,.zip,.md,.txt"
            >
              <el-button type="primary" :icon="Upload">上传文件</el-button>
            </el-upload>
          </div>
        </div>
      </div>

      <div class="documents-body">
      <div class="stat-row">
        <div class="stat-card blue">
          <div class="stat-num">{{ mergedFiles.length }}</div>
          <div class="stat-label">我的文档</div>
          <div class="stat-tip">已接入上传、下载、复制链接</div>
        </div>
        <div class="stat-card green">
          <div class="stat-num">{{ sharedCount }}</div>
          <div class="stat-label">共享中</div>
          <div class="stat-tip">正在被团队复用的材料</div>
        </div>
        <div class="stat-card orange">
          <div class="stat-num">{{ pendingCount }}</div>
          <div class="stat-label">待处理</div>
          <div class="stat-tip">待评审或待回收的内容</div>
        </div>
        <div class="stat-card violet">
          <div class="stat-num">{{ recentCount }}</div>
          <div class="stat-label">近 7 天更新</div>
          <div class="stat-tip">最近补充的文件</div>
        </div>
      </div>

      <div v-if="sceneCases.length" class="scene-strip">
        <div v-for="scene in sceneCases" :key="scene.title" class="scene-card">
          <div class="scene-card-top">
            <div>
              <div class="scene-title">{{ scene.title }}</div>
              <div class="scene-desc">{{ scene.description }}</div>
            </div>
            <el-tag size="small" :type="scene.tagType" effect="plain">{{ scene.tag }}</el-tag>
          </div>
          <div class="scene-file">{{ scene.fileName }}</div>
          <div class="scene-actions">
            <el-button size="small" type="primary" @click="openScene(scene)">打开用例</el-button>
            <el-button size="small" @click="runSceneAction(scene)">{{ scene.actionLabel }}</el-button>
          </div>
        </div>
      </div>
      <div v-else class="scene-empty">上传文档后，这里会自动出现可直接演示的协作用例。</div>

      <div class="workspace-panel" :class="{ 'detail-active': detailActive }">
        <div class="workspace-left">
          <div class="toolbar-row">
            <el-tabs v-model="activeTab" class="documents-tabs">
              <el-tab-pane label="全部" name="all" />
              <el-tab-pane label="PDF" name="pdf" />
              <el-tab-pane label="Word" name="word" />
              <el-tab-pane label="表格" name="sheet" />
              <el-tab-pane label="其他" name="other" />
            </el-tabs>
            <el-input
              v-model="keyword"
              class="doc-search"
              clearable
              placeholder="搜索文件名、场景或负责人"
              :prefix-icon="Search"
            />
          </div>

          <div class="file-list">
            <div v-if="!mergedFiles.length" class="file-empty-state">
              <el-empty description="还没有上传文档" :image-size="88">
                <el-upload
                  :show-file-list="false"
                  :http-request="handleUpload"
                  accept=".pdf,.doc,.docx,.xls,.xlsx,.csv,.ppt,.pptx,.zip,.md,.txt"
                >
                  <el-button type="primary">上传第一个文件</el-button>
                </el-upload>
              </el-empty>
            </div>
            <div
              v-for="item in displayFiles"
              :key="item.id"
              class="file-row"
              :class="{ active: currentFile?.id === item.id }"
              @click="selectFile(item)"
            >
              <div class="file-main">
                <div class="file-badge" :class="`theme-${badgeMeta(item.name).theme}`">
                  <div class="file-badge-sheet">
                    <span class="file-badge-fold"></span>
                    <span class="file-badge-type">{{ badgeMeta(item.name).badge }}</span>
                    <span class="file-badge-ext">{{ badgeMeta(item.name).ext }}</span>
                  </div>
                </div>
                <div class="file-info">
                  <div class="file-title-row">
                    <div class="file-title">{{ item.name }}</div>
                    <el-tag size="small" :type="statusType(item.status)" effect="plain">{{ item.status }}</el-tag>
                  </div>
                  <div class="file-desc">{{ item.description }}</div>
                  <div class="file-meta-row">
                    <span>{{ item.owner }}</span>
                    <span>{{ item.scene }}</span>
                    <span>{{ formatSize(item.size) }}</span>
                    <span>{{ formatTime(item.updateTime || item.createTime) }}</span>
                  </div>
                </div>
              </div>
              <div class="file-side">
                <div class="file-usage">{{ item.activity }}</div>
                <div class="file-side-caption">{{ item.history?.[0]?.time || formatTime(item.updateTime || item.createTime) }}</div>
                <div class="file-side-actions">
                  <button type="button" class="ghost-btn" @click.stop="downloadFile(item)">下载</button>
                  <button type="button" class="ghost-btn" @click.stop="copyLink(item)">复制链接</button>
                </div>
              </div>
            </div>
            <el-empty v-if="mergedFiles.length && !displayFiles.length" description="没有符合条件的文件" :image-size="88" />
          </div>
        </div>

        <div class="workspace-right">
          <template v-if="currentFile">
            <div class="preview-head">
              <button type="button" class="detail-back-btn" @click="closeDetail"><el-icon><ArrowLeft /></el-icon> 返回</button>
              <div class="preview-main">
                <div class="preview-badge" :class="`theme-${badgeMeta(currentFile.name).theme}`">
                  <div class="file-badge-sheet">
                    <span class="file-badge-fold"></span>
                    <span class="file-badge-type">{{ badgeMeta(currentFile.name).badge }}</span>
                    <span class="file-badge-ext">{{ badgeMeta(currentFile.name).ext }}</span>
                  </div>
                </div>
                <div>
                  <div class="preview-title">{{ currentFile.name }}</div>
                  <div class="preview-desc">{{ currentFile.description }}</div>
                </div>
              </div>
              <el-tag size="small" :type="statusType(currentFile.status)">{{ currentFile.status }}</el-tag>
            </div>

            <div class="preview-grid">
              <div class="preview-metric">
                <span class="metric-label">负责人</span>
                <span class="metric-value">{{ currentFile.owner }}</span>
              </div>
              <div class="preview-metric">
                <span class="metric-label">协作场景</span>
                <span class="metric-value">{{ currentFile.scene }}</span>
              </div>
              <div class="preview-metric">
                <span class="metric-label">文件大小</span>
                <span class="metric-value">{{ formatSize(currentFile.size) }}</span>
              </div>
              <div class="preview-metric">
                <span class="metric-label">最近更新</span>
                <span class="metric-value">{{ formatTime(currentFile.updateTime || currentFile.createTime) }}</span>
              </div>
              <div class="preview-metric">
                <span class="metric-label">分享次数</span>
                <span class="metric-value">{{ currentFile.shareCount || 0 }}</span>
              </div>
            </div>

            <div class="preview-actions">
              <el-button type="primary" @click="downloadFile(currentFile)">下载文件</el-button>
              <el-button @click="copyLink(currentFile)">复制链接</el-button>
              <el-button @click="shareFile(currentFile)">分享</el-button>
              <el-button @click="lockFile(currentFile)">锁版</el-button>
              <el-button @click="recycleFile(currentFile)">回收</el-button>
              <el-button @click="openEditDialog(currentFile)">重命名 / 设置</el-button>
              <el-button @click="filterByCurrentType(currentFile)">筛选同类</el-button>
              <el-button type="danger" plain @click="removeFile(currentFile)">删除</el-button>
            </div>

            <div class="preview-section">
              <div class="section-title">可直接使用的用例</div>
              <div class="example-list">
                <div v-for="example in currentFile.examples" :key="example.title" class="example-card">
                  <div class="example-title">{{ example.title }}</div>
                  <div class="example-desc">{{ example.description }}</div>
                  <div class="example-footer">
                    <el-tag size="small" effect="plain">{{ example.tag }}</el-tag>
                    <el-button size="small" text type="primary" @click="runFileExample(example, currentFile)">{{ example.actionLabel }}</el-button>
                  </div>
                </div>
              </div>
            </div>

            <div class="preview-section">
              <div class="section-title">处理提醒</div>
              <div class="check-list">
                <div v-for="tip in currentFile.checklist" :key="tip" class="check-item">{{ tip }}</div>
              </div>
            </div>

            <div class="preview-section">
              <div class="section-title">最近操作</div>
              <div class="history-list">
                <div v-for="item in currentFile.history.slice(0, 5)" :key="`${item.time}-${item.text}`" class="history-item">
                  <span class="history-time">{{ item.time }}</span>
                  <span class="history-text">{{ item.text }}</span>
                </div>
              </div>
            </div>
          </template>
          <div v-else class="preview-empty">
            <el-empty description="请选择一个文件查看详情" :image-size="88" />
          </div>
        </div>
      </div>
      </div>
    </div>
  </div>

  <el-dialog v-model="editDialogOpen" title="编辑文件" width="520px">
    <el-form label-width="88px">
      <el-form-item label="文件名">
        <el-input v-model="editForm.name" placeholder="请输入文件名" />
      </el-form-item>
      <el-form-item label="协作场景">
        <el-input v-model="editForm.scene" placeholder="例如：排期评审 / 客户复盘" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="editForm.status" placeholder="请选择状态" style="width: 100%">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="说明">
        <el-input
          v-model="editForm.description"
          type="textarea"
          :rows="4"
          placeholder="补充当前文件用途、处理提醒或上下文"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="editDialogOpen = false">取消</el-button>
      <el-button type="primary" :loading="editSaving" @click="submitEdit">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="shareDialogOpen" title="分享到聊天" width="560px">
    <div class="share-dialog">
      <div v-if="shareSourceFile" class="share-file-head">
        <div class="share-file-badge" :class="`theme-${badgeMeta(shareSourceFile.name).theme}`">
          <div class="file-badge-sheet">
            <span class="file-badge-fold"></span>
            <span class="file-badge-type">{{ badgeMeta(shareSourceFile.name).badge }}</span>
            <span class="file-badge-ext">{{ badgeMeta(shareSourceFile.name).ext }}</span>
          </div>
        </div>
        <div class="share-file-info">
          <div class="share-file-name">{{ shareSourceFile.name }}</div>
          <div class="share-file-desc">{{ shareSourceFile.scene }} · {{ formatSize(shareSourceFile.size) }}</div>
        </div>
      </div>

      <div v-if="quickShareSessions.length" class="share-quick-strip">
        <div class="share-quick-label">最近会话</div>
        <div class="share-quick-list">
          <button
            v-for="session in quickShareSessions"
            :key="session.id"
            type="button"
            class="share-quick-btn"
            :disabled="shareSending"
            @click="quickShareToSession(session)"
          >
            <span class="share-quick-name">{{ sessionDisplayName(session) }}</span>
            <span class="share-quick-meta">{{ session.type === 2 ? '群聊' : '单聊' }}</span>
          </button>
        </div>
      </div>

      <el-input
        v-model="shareSearch"
        class="share-search"
        clearable
        placeholder="搜索聊天会话"
        :prefix-icon="Search"
      />

      <div class="share-session-list">
        <button
          v-for="session in filteredShareSessions"
          :key="session.id"
          type="button"
          class="share-session-row"
          :class="{ active: shareTargetId === session.id }"
          @click="shareTargetId = session.id"
        >
          <div class="share-session-main">
            <div class="share-session-title-row">
              <span class="share-session-title">{{ sessionDisplayName(session) }}</span>
              <el-tag size="small" effect="plain" :type="session.type === 2 ? 'success' : 'info'">
                {{ session.type === 2 ? '群聊' : '单聊' }}
              </el-tag>
            </div>
            <div class="share-session-preview">{{ sessionPreview(session) }}</div>
          </div>
          <div class="share-session-side">
            <span class="share-session-time">{{ formatTime(session.lastTime) }}</span>
            <span v-if="session.unread" class="share-session-unread">{{ session.unread > 99 ? '99+' : session.unread }}</span>
          </div>
        </button>
        <el-empty v-if="!filteredShareSessions.length" description="没有可发送的聊天会话" :image-size="72" />
      </div>
    </div>
    <template #footer>
      <el-button @click="shareDialogOpen = false">取消</el-button>
      <el-button :loading="shareSending" :disabled="!activeShareTarget || !shareSourceFile" @click="confirmShareToChat(false)">
        仅发送
      </el-button>
      <el-button type="primary" :loading="shareSending" :disabled="!activeShareTarget || !shareSourceFile" @click="confirmShareToChat(true)">
        发送并打开
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Back, Refresh, Search, Upload, ArrowLeft } from '@element-plus/icons-vue'
import AppSideNav from '../components/AppSideNav.vue'
import { useUserPreferenceStore } from '../store/userPreference'
import {
  apiFileDelete,
  apiFileList,
  apiFileLock,
  apiFileRecycle,
  apiFileShare,
  apiFileUpdate,
  apiSendMessage,
  apiSessions,
  apiUpload,
  apiUserList
} from '../api'

const router = useRouter()
const route = useRoute()
const preferenceStore = useUserPreferenceStore()
const activeTab = ref('all')
const keyword = ref('')
const rawFiles = ref([])
const selectedFileId = ref(null)
const detailActive = ref(false)
const documentsStateReady = ref(false)
const users = ref([])
const editDialogOpen = ref(false)
const editSaving = ref(false)
const shareDialogOpen = ref(false)
const shareSending = ref(false)
const shareSearch = ref('')
const shareSessions = ref([])
const shareTargetId = ref(null)
const shareSourceFile = ref(null)
const editForm = ref({
  id: null,
  name: '',
  scene: '',
  description: '',
  status: '可用'
})

const statusOptions = [
  { label: '已共享', value: '已共享' },
  { label: '待评审', value: '待评审' },
  { label: '待回收', value: '待回收' },
  { label: '已锁版', value: '已锁版' },
  { label: '可下载', value: '可下载' },
  { label: '可用', value: '可用' }
]

const userNameMap = computed(() => {
  const map = new Map()
  users.value.forEach(user => {
    map.set(user.id, user.nickname || user.username || '协作成员')
  })
  return map
})

const mergedFiles = computed(() => rawFiles.value.map(hydrateFile))

const displayFiles = computed(() => mergedFiles.value.filter(file => {
  const theme = badgeMeta(file.name).theme
  const matchesTab = (
    activeTab.value === 'all' ||
    (activeTab.value === 'pdf' && theme === 'pdf') ||
    (activeTab.value === 'word' && theme === 'word') ||
    (activeTab.value === 'sheet' && theme === 'excel') ||
    (activeTab.value === 'other' && !['pdf', 'word', 'excel'].includes(theme))
  )
  const text = `${file.name} ${file.scene} ${file.owner} ${file.description}`.toLowerCase()
  const matchesKeyword = !keyword.value.trim() || text.includes(keyword.value.trim().toLowerCase())
  return matchesTab && matchesKeyword
}))
const currentFile = computed(() => displayFiles.value.find(file => file.id === selectedFileId.value) || displayFiles.value[0] || null)
const quickShareSessions = computed(() => shareSessions.value.slice(0, 3))
const filteredShareSessions = computed(() => {
  const keyword = shareSearch.value.trim().toLowerCase()
  return shareSessions.value.filter(session => {
    if (!keyword) return true
    return `${sessionDisplayName(session)} ${sessionPreview(session)}`.toLowerCase().includes(keyword)
  })
})
const activeShareTarget = computed(() => filteredShareSessions.value.find(session => session.id === shareTargetId.value) || null)
const recentCount = computed(() => mergedFiles.value.filter(file => {
  const time = new Date(file.updateTime || file.createTime).getTime()
  return !Number.isNaN(time) && Date.now() - time <= 7 * 24 * 3600 * 1000
}).length)
const sharedCount = computed(() => mergedFiles.value.filter(file => ['已共享', '已锁版', '可下载'].includes(file.status)).length)
const pendingCount = computed(() => mergedFiles.value.filter(file => ['待评审', '待回收'].includes(file.status)).length)
const sceneCases = computed(() => mergedFiles.value.slice(0, 3).map(buildSceneCase).filter(Boolean))

watch(() => preferenceStore.loaded, (loaded) => {
  if (!loaded || documentsStateReady.value) return
  const state = preferenceStore.getPageState('documents')
  if (typeof state.activeTab === 'string') activeTab.value = state.activeTab
  if (typeof state.keyword === 'string') keyword.value = state.keyword
  if (Number.isFinite(Number(state.selectedFileId)) && Number(state.selectedFileId) > 0) {
    selectedFileId.value = Number(state.selectedFileId)
  }
  documentsStateReady.value = true
}, { immediate: true })

watch(displayFiles, (list) => {
  if (!list.length) {
    selectedFileId.value = null
    return
  }
  if (!list.some(file => file.id === selectedFileId.value)) {
    selectedFileId.value = resolveRouteFileId() || list[0].id
  }
}, { immediate: true })

watch(() => route.query.file, () => {
  applyRouteFileSelection()
})

watch(filteredShareSessions, (list) => {
  if (!shareDialogOpen.value) return
  if (!list.length) {
    shareTargetId.value = null
    return
  }
  if (!list.some(session => session.id === shareTargetId.value)) {
    shareTargetId.value = list[0].id
  }
})

watch([activeTab, keyword, selectedFileId], () => {
  if (!documentsStateReady.value || !preferenceStore.loaded) return
  preferenceStore.setPageState('documents', {
    activeTab: activeTab.value,
    keyword: keyword.value,
    selectedFileId: selectedFileId.value
  })
})

onMounted(() => {
  loadInitialData()
})

async function loadInitialData() {
  await Promise.allSettled([
    loadFiles(),
    loadUsers()
  ])
  applyRouteFileSelection()
}

async function loadFiles() {
  try {
    const data = await apiFileList({ limit: 20 })
    rawFiles.value = Array.isArray(data) ? data : []
  } catch {
    rawFiles.value = []
  }
}

async function loadUsers() {
  try {
    const data = await apiUserList()
    users.value = Array.isArray(data) ? data : []
  } catch {
    users.value = []
  }
}

async function handleUpload(option) {
  try {
    const formData = new FormData()
    formData.append('file', option.file)
    const file = await apiUpload(formData)
    option.onSuccess?.()
    ElMessage.success('文件已上传')
    activeTab.value = 'all'
    keyword.value = ''
    await loadFiles()
    if (file?.id) selectedFileId.value = file.id
  } catch (error) {
    option.onError?.(error)
  }
}

function hydrateFile(file) {
  const status = normalizeStatus(file.status)
  const history = Array.isArray(file.history) ? file.history : []
  return {
    ...file,
    owner: userNameMap.value.get(file.uploaderId) || '当前账号',
    scene: file.scene || defaultScene(file),
    description: file.description || defaultDescription(file),
    status,
    activity: buildActivity({ ...file, history }, status),
    examples: buildExamples(file, status),
    checklist: buildChecklist(file, status),
    history
  }
}

function badgeMeta(name) {
  const ext = ((name || '').split('.').pop() || '').toLowerCase()
  if (ext === 'pdf') return { badge: 'PDF', theme: 'pdf', ext: 'PDF' }
  if (['doc', 'docx'].includes(ext)) return { badge: 'WORD', theme: 'word', ext: ext.toUpperCase() }
  if (['xls', 'xlsx', 'csv'].includes(ext)) return { badge: 'EXCEL', theme: 'excel', ext: ext.toUpperCase() }
  if (['ppt', 'pptx'].includes(ext)) return { badge: 'PPT', theme: 'ppt', ext: ext.toUpperCase() }
  if (['zip', 'rar', '7z'].includes(ext)) return { badge: 'ZIP', theme: 'zip', ext: ext.toUpperCase() }
  return { badge: (ext || 'FILE').toUpperCase(), theme: 'file', ext: (ext || 'FILE').toUpperCase() }
}

function defaultScene(file) {
  const theme = badgeMeta(file.name).theme
  if (theme === 'pdf') return '周报同步'
  if (theme === 'word') return '排期评审'
  if (theme === 'excel') return '数据回传'
  if (theme === 'ppt') return '客户复盘'
  if (theme === 'zip') return '资源交付'
  return '通用协作'
}

function defaultDescription(file) {
  const theme = badgeMeta(file.name).theme
  const name = file.name || '协作文档'
  if (theme === 'pdf') return `${name} 适合会前同步、归档和正式版本分发。`
  if (theme === 'word') return `${name} 适合做方案、排期或纪要的多人评审。`
  if (theme === 'excel') return `${name} 适合数据回传、补数和进度分析。`
  if (theme === 'ppt') return `${name} 适合客户复盘、方案演示和会前锁版。`
  if (theme === 'zip') return `${name} 适合批量交付素材、压缩包和成套资源。`
  return `${name} 支持上传、下载和链接分发，可直接用于协作。`
}

function normalizeStatus(status) {
  return statusOptions.some(item => item.value === status) ? status : '可用'
}

function buildActivity(file, status) {
  if ((file.shareCount || 0) > 0) {
    return `已分享 ${file.shareCount} 次`
  }
  if (Array.isArray(file.history) && file.history[0]?.text) {
    return file.history[0].text
  }
  return {
    '已共享': '链接可直接分发',
    '待评审': '等待评审确认',
    '待回收': '等待回收或补数',
    '已锁版': '当前版本已锁定',
    '可下载': '适合批量交付',
    '可用': '可直接下载或转发'
  }[status] || '可直接使用'
}

function buildExamples(file, status) {
  const theme = badgeMeta(file.name).theme
  if (theme === 'word') {
    return [
      { title: '发到评审群预读', description: '直接挑一个聊天会话发送文件，减少会中逐段解释。', tag: 'Word', action: 'share', actionLabel: '发送到聊天' },
      { title: '直接进入设置', description: '需要改状态、重命名或补说明时，直接走后端保存。', tag: status, action: 'edit', actionLabel: '编辑文件' }
    ]
  }
  if (theme === 'excel') {
    return [
      { title: '下载继续处理', description: '先下载到本地，再做筛选、透视表或二次分析。', tag: '表格', action: 'download', actionLabel: '下载表格文件' },
      { title: '集中处理数据类文件', description: '切到表格标签，批量处理回传或补数材料。', tag: '筛选', action: 'filter-sheet', actionLabel: '筛选表格文件' }
    ]
  }
  if (theme === 'ppt') {
    return [
      { title: '先锁版再对外发出', description: '演示稿对外发送前，先把状态锁成最终版本。', tag: status, action: 'lock', actionLabel: '锁版当前文件' },
      { title: '会前下载备份', description: '投屏或外部汇报前，建议先保留一份本地备份。', tag: '下载', action: 'download', actionLabel: '下载演示稿' }
    ]
  }
  if (theme === 'zip') {
    return [
      { title: '发给协作人', description: '挑一个聊天会话直接发送资源包，减少重复单独传图。', tag: '链接', action: 'share', actionLabel: '发送到聊天' },
      { title: '本地解压检查', description: '下载后确认命名、目录和资源完整性。', tag: '下载', action: 'download', actionLabel: '下载压缩包' }
    ]
  }
  return [
    { title: '直接分发材料', description: '把文件作为消息发到聊天会话，让协作人直接接收。', tag: status, action: 'share', actionLabel: '发送到聊天' },
    { title: '本地留档', description: '下载到本地继续处理或离线备份。', tag: '下载', action: 'download', actionLabel: '下载文件' }
  ]
}

function buildChecklist(file, status) {
  const theme = badgeMeta(file.name).theme
  const generic = ['改完名称、场景或状态后会直接写回后端。', '对外分发前建议确认当前链接和版本说明。']
  if (status === '待评审') {
    return ['评审完成后记得切到已共享或已锁版。', '引用到群聊时建议同步负责人和截止时间。']
  }
  if (status === '待回收') {
    return ['回收前先确认是否还有人在使用当前版本。', '如果是数据文件，建议补一句数据口径说明。']
  }
  if (theme === 'ppt') {
    return ['对外演示前再确认客户名称和日期页。', '锁版后不要继续覆盖同一份演示稿。']
  }
  if (theme === 'zip') {
    return ['发出前确认资源目录结构和命名完整。', '有新版本时建议在群里补一句更新说明。']
  }
  return generic
}

function buildSceneCase(file) {
  if (!file) return null
  return {
    title: file.scene,
    description: file.description,
    fileName: file.name,
    tag: file.status,
    tagType: statusType(file.status),
    action: buildSceneAction(file),
    actionLabel: buildSceneActionLabel(file)
  }
}

function buildSceneAction(file) {
  if (file.status === '待评审') return 'edit'
  if (file.status === '已锁版') return 'download'
  if (file.status === '已共享') return 'share'
  if (file.status === '待回收') return 'edit'
  return 'copy'
}

function buildSceneActionLabel(file) {
  if (file.status === '待评审') return '打开设置'
  if (file.status === '已锁版') return '下载备份'
  if (file.status === '已共享') return '发送到聊天'
  if (file.status === '待回收') return '编辑状态'
  return '复制链接'
}

function statusType(status) {
  return {
    '已共享': 'success',
    '可下载': 'success',
    '已锁版': 'info',
    '待评审': 'warning',
    '待回收': 'danger',
    '可用': 'primary'
  }[status] || 'info'
}

function formatSize(size) {
  if (!size) return ''
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

function formatTime(time) {
  if (!time) return ''
  const date = new Date(time)
  if (Number.isNaN(date.getTime())) return time
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mi = String(date.getMinutes()).padStart(2, '0')
  return `${mm}-${dd} ${hh}:${mi}`
}

function sessionDisplayName(session) {
  return session?.name || (session?.type === 2 ? '未命名群聊' : '未命名单聊')
}

function sessionPreview(session) {
  return session?.lastMsg || '发送后会以文件消息出现在该会话中'
}

function selectFile(file) {
  selectedFileId.value = file.id
  if (window.innerWidth <= 768) {
    detailActive.value = true
  }
}

function closeDetail() {
  detailActive.value = false
}

function resolveRouteFileId() {
  const id = Number(route.query.file)
  return Number.isFinite(id) && id > 0 ? id : null
}

function applyRouteFileSelection() {
  const routeFileId = resolveRouteFileId()
  if (!routeFileId) return
  const matched = mergedFiles.value.find(file => file.id === routeFileId)
  if (matched) {
    selectedFileId.value = matched.id
  }
}

function findFileByName(name) {
  return mergedFiles.value.find(file => file.name === name) || null
}

function openScene(scene) {
  const file = findFileByName(scene.fileName)
  if (!file) return
  selectFile(file)
}

async function runSceneAction(scene) {
  const file = findFileByName(scene.fileName)
  if (!file) return
  selectFile(file)
  await runNamedAction(scene.action, file)
}

async function runFileExample(example, file) {
  await runNamedAction(example.action, file)
}

async function runNamedAction(action, file) {
  if (!file) return
  if (action === 'download') downloadFile(file)
  else if (action === 'copy') await copyLink(file)
  else if (action === 'share') await shareFile(file)
  else if (action === 'edit') openEditDialog(file)
  else if (action === 'filter-word') activeTab.value = 'word'
  else if (action === 'filter-sheet') activeTab.value = 'sheet'
  else if (action === 'select') selectFile(file)
  else if (action === 'lock') await lockFile(file)
  else if (action === 'recycle') await recycleFile(file)
}

function filterByCurrentType(file) {
  const theme = badgeMeta(file.name).theme
  if (theme === 'pdf') activeTab.value = 'pdf'
  else if (theme === 'word') activeTab.value = 'word'
  else if (theme === 'excel') activeTab.value = 'sheet'
  else activeTab.value = 'other'
}

function downloadFile(file) {
  triggerDownload(file.url, file.name)
}

async function copyLink(file, options = {}) {
  const link = `${window.location.origin}${file.url}`
  try {
    await navigator.clipboard.writeText(link)
    if (!options.silent) ElMessage.success('链接已复制')
    return true
  } catch {
    if (!options.silent) ElMessage.warning(link)
    return false
  }
}

async function shareFile(file) {
  if (!file?.id) return
  shareSourceFile.value = file
  shareSearch.value = ''
  shareTargetId.value = null
  shareDialogOpen.value = true
  await loadShareSessions()
}

async function loadShareSessions() {
  const data = await apiSessions()
  shareSessions.value = Array.isArray(data) ? data : []
  shareTargetId.value = filteredShareSessions.value[0]?.id || null
}

async function quickShareToSession(session) {
  await sendFileToSession(session, false)
}

async function confirmShareToChat(openAfterSend) {
  const file = shareSourceFile.value
  const target = activeShareTarget.value
  if (!file?.id || !target?.id) return
  await sendFileToSession(target, openAfterSend)
}

async function sendFileToSession(target, openAfterSend) {
  const file = shareSourceFile.value
  if (!file?.id || !target?.id) return
  shareSending.value = true
  try {
    await apiSendMessage({
      sessionId: target.id,
      contentType: 3,
      content: file.name,
      extra: JSON.stringify({
        url: file.url,
        size: file.size,
        fileId: file.id,
        scene: file.scene,
        description: file.description,
        status: '已共享',
        shareCount: (file.shareCount || 0) + 1,
        source: 'documents'
      })
    })
    const updated = await apiFileShare(file.id, { targetName: sessionDisplayName(target) })
    upsertRawFile(updated)
    selectedFileId.value = updated.id
    shareDialogOpen.value = false
    if (openAfterSend) {
      router.push({ path: '/chat', query: { session: String(target.id) } })
      ElMessage.success(`已发送到 ${sessionDisplayName(target)}，正在打开会话`)
    } else {
      ElMessage.success(`已发送到 ${sessionDisplayName(target)}`)
    }
  } finally {
    shareSending.value = false
  }
}

async function lockFile(file) {
  const updated = await apiFileLock(file.id)
  upsertRawFile(updated)
  selectedFileId.value = updated.id
  ElMessage.success('已锁版')
}

async function recycleFile(file) {
  const updated = await apiFileRecycle(file.id)
  upsertRawFile(updated)
  selectedFileId.value = updated.id
  ElMessage.success('已标记待回收')
}

function openEditDialog(file) {
  if (!file) return
  editForm.value = {
    id: file.id,
    name: file.name || '',
    scene: file.scene || '',
    description: file.description || '',
    status: normalizeStatus(file.status)
  }
  editDialogOpen.value = true
}

async function submitEdit() {
  if (!editForm.value.id) return
  if (!editForm.value.name.trim()) {
    ElMessage.warning('请输入文件名')
    return
  }
  editSaving.value = true
  try {
    const updated = await apiFileUpdate(editForm.value.id, {
      name: editForm.value.name,
      scene: editForm.value.scene,
      description: editForm.value.description,
      status: editForm.value.status
    })
    upsertRawFile(updated)
    selectedFileId.value = updated.id
    editDialogOpen.value = false
    ElMessage.success('文件已保存')
  } finally {
    editSaving.value = false
  }
}

async function removeFile(file) {
  if (!file?.id) return
  try {
    await ElMessageBox.confirm(`确定删除「${file.name}」吗？`, '删除文件', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await apiFileDelete(file.id)
    rawFiles.value = rawFiles.value.filter(item => item.id !== file.id)
    if (selectedFileId.value === file.id) {
      selectedFileId.value = displayFiles.value[0]?.id || null
    }
    ElMessage.success('文件已删除')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') throw error
  }
}

function upsertRawFile(file) {
  const next = [...rawFiles.value]
  const index = next.findIndex(item => item.id === file.id)
  if (index >= 0) next[index] = file
  else next.unshift(file)
  rawFiles.value = next
}

function triggerDownload(url, name) {
  const link = document.createElement('a')
  link.href = url
  link.download = name || ''
  document.body.appendChild(link)
  link.click()
  link.remove()
}
</script>

<style scoped>
.documents-shell {
  height: 100%;
  display: flex;
  background: var(--dt-bg);
}
.documents-page {
  flex: 1;
  min-width: 0;
  height: 100%;
  background: #f3f5f8;
  display: flex;
  flex-direction: column;
}
.documents-top {
  padding: 16px 24px 14px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96) 0%, rgba(247, 249, 252, 0.92) 100%);
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(18px);
  flex-shrink: 0;
}
.documents-top-inner {
  width: 100%;
  max-width: 1260px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}
.page-title-cluster {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 14px;
}
.page-title-block { min-width: 0; }
.page-section-label {
  margin-bottom: 4px;
  color: #98a2b3;
  font-size: 11px;
  font-weight: 600;
}
.page-heading-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.page-back-btn {
  height: 36px;
  padding: 0 14px;
  border-radius: 10px;
  border-color: rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.82);
  color: #344054;
  flex-shrink: 0;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.04);
}
.page-back-btn:hover {
  color: #1677ff;
  border-color: rgba(22, 119, 255, 0.22);
  background: #fff;
}
.page-top-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}
.page-top-actions :deep(.el-button) {
  height: 36px;
  padding: 0 16px;
  border-radius: 10px;
  font-weight: 600;
}
.ptitle {
  font-size: 20px;
  line-height: 1.15;
  font-weight: 700;
  color: #1d2129;
}
.documents-body {
  flex: 1;
  overflow-y: auto;
  padding: 18px 24px 24px;
  max-width: 1220px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
}
.stat-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}
.stat-card {
  background: #fff;
  border-radius: 10px;
  padding: 18px 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.stat-card.blue { border-top: 3px solid #1677ff; }
.stat-card.green { border-top: 3px solid #52c41a; }
.stat-card.orange { border-top: 3px solid #fa8c16; }
.stat-card.violet { border-top: 3px solid #722ed1; }
.stat-num {
  font-size: 26px;
  font-weight: 700;
  color: #1d2129;
}
.stat-label {
  margin-top: 6px;
  font-size: 13px;
  color: #4b5563;
}
.stat-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.6;
}
.scene-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}
.scene-empty,
.scene-card,
.workspace-panel {
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.scene-empty {
  margin-bottom: 16px;
  padding: 18px 20px;
  font-size: 13px;
  color: #6b7280;
  line-height: 1.7;
}
.scene-card {
  padding: 16px;
}
.scene-card-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}
.scene-title {
  font-size: 15px;
  font-weight: 600;
  color: #1d2129;
}
.scene-desc {
  margin-top: 6px;
  font-size: 13px;
  color: #6b7280;
  line-height: 1.6;
}
.scene-file {
  margin-top: 12px;
  font-size: 13px;
  color: #1677ff;
}
.scene-actions {
  display: flex;
  gap: 8px;
  margin-top: 14px;
}
.workspace-panel {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(320px, .9fr);
  gap: 0;
  overflow: hidden;
}
.workspace-left {
  border-right: 1px solid #f0f0f0;
  min-width: 0;
}
.toolbar-row {
  padding: 0 18px 14px;
  border-bottom: 1px solid #f0f0f0;
}
.documents-tabs :deep(.el-tabs__header) {
  margin-bottom: 8px;
}
.doc-search {
  max-width: 320px;
}
.file-list {
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.file-row {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 15px 16px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  cursor: pointer;
  background: linear-gradient(180deg, #ffffff 0%, #fbfcff 100%);
  transition: transform .18s, border-color .18s, box-shadow .18s, background .18s;
}
.file-row:hover,
.file-row.active {
  background: #f7faff;
  border-color: #cfe1ff;
  box-shadow: 0 10px 22px rgba(22, 119, 255, 0.08);
}
.file-row:hover {
  transform: translateY(-1px);
}
.file-row.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 12px;
  bottom: 12px;
  width: 3px;
  border-radius: 999px;
  background: #1677ff;
}
.file-main {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  min-width: 0;
  flex: 1;
}
.file-badge,
.preview-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.file-badge {
  width: 62px;
}
.preview-badge {
  width: 86px;
}
.file-badge-sheet {
  position: relative;
  width: 100%;
  min-height: 42px;
  border-radius: 12px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: #fff;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.06);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 10px 10px 8px;
  box-sizing: border-box;
}
.preview-badge .file-badge-sheet {
  min-height: 54px;
  padding: 12px 12px 10px;
}
.file-badge-sheet::before {
  content: '';
  position: absolute;
  inset: 0 0 auto;
  height: 8px;
  opacity: 0.95;
}
.file-badge-fold {
  position: absolute;
  top: 0;
  right: 0;
  width: 14px;
  height: 14px;
  background: rgba(255, 255, 255, 0.78);
  clip-path: polygon(100% 0, 0 0, 100% 100%);
}
.file-badge-type {
  position: relative;
  z-index: 1;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0;
}
.preview-badge .file-badge-type {
  font-size: 12px;
}
.file-badge-ext {
  position: relative;
  z-index: 1;
  margin-top: 3px;
  font-size: 10px;
  color: #7a8599;
  line-height: 1;
}
.theme-pdf .file-badge-sheet::before { background: linear-gradient(90deg, #ffb1b1, #ff5a5f); }
.theme-word .file-badge-sheet::before { background: linear-gradient(90deg, #8bb8ff, #2f54eb); }
.theme-excel .file-badge-sheet::before { background: linear-gradient(90deg, #9ee274, #237804); }
.theme-ppt .file-badge-sheet::before { background: linear-gradient(90deg, #ffc47b, #d46b08); }
.theme-zip .file-badge-sheet::before { background: linear-gradient(90deg, #d1b3ff, #722ed1); }
.theme-file .file-badge-sheet::before { background: linear-gradient(90deg, #bfbfbf, #595959); }
.theme-pdf .file-badge-type { color: #d9363e; }
.theme-word .file-badge-type { color: #1d4ed8; }
.theme-excel .file-badge-type { color: #1f7a35; }
.theme-ppt .file-badge-type { color: #c96a06; }
.theme-zip .file-badge-type { color: #6d28d9; }
.theme-file .file-badge-type { color: #4b5563; }
.file-info,
.preview-main > div:last-child {
  min-width: 0;
}
.file-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.file-title,
.preview-title {
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
  word-break: break-word;
}
.preview-title {
  font-size: 18px;
}
.file-desc,
.preview-desc {
  margin-top: 5px;
  font-size: 13px;
  color: #6b7280;
  line-height: 1.6;
}
.file-meta-row {
  margin-top: 8px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  font-size: 12px;
  color: #8c8c8c;
}
.file-side {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 7px;
}
.file-usage {
  font-size: 12px;
  color: #4b5563;
  font-weight: 600;
}
.file-side-caption {
  font-size: 11px;
  color: #8c8c8c;
}
.file-side-actions,
.preview-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.file-side-actions {
  opacity: 0.72;
  transition: opacity .18s, transform .18s;
}
.file-row:hover .file-side-actions,
.file-row.active .file-side-actions {
  opacity: 1;
  transform: translateY(-1px);
}
.ghost-btn {
  height: 28px;
  padding: 0 10px;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: #4b5563;
  font-size: 12px;
  cursor: pointer;
  transition: all .18s;
}
.ghost-btn:hover {
  color: #1677ff;
  border-color: #bcd4ff;
  background: #fff;
}
.workspace-right {
  padding: 20px;
  background: linear-gradient(180deg, #fcfcfd 0%, #f8fbff 100%);
}
.detail-back-btn {
  display: none;
}
.preview-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}
.preview-main {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}
.preview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}
.preview-metric {
  padding: 12px 14px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #f0f0f0;
}
.metric-label {
  display: block;
  font-size: 12px;
  color: #8c8c8c;
}
.metric-value {
  display: block;
  margin-top: 6px;
  font-size: 14px;
  color: #1d2129;
  font-weight: 600;
}
.preview-actions {
  margin-top: 18px;
}
.preview-section {
  margin-top: 22px;
}
.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
  margin-bottom: 12px;
}
.example-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.example-card {
  padding: 14px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #f0f0f0;
}
.example-title {
  font-size: 13px;
  font-weight: 600;
  color: #1d2129;
}
.example-desc {
  margin-top: 6px;
  font-size: 13px;
  color: #6b7280;
  line-height: 1.6;
}
.example-footer {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.check-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.check-item {
  padding: 11px 12px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #f0f0f0;
  font-size: 13px;
  color: #4b5563;
}
.history-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.history-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 11px 12px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #f0f0f0;
}
.history-time {
  min-width: 70px;
  font-size: 12px;
  color: #98a2b3;
  flex-shrink: 0;
}
.history-text {
  font-size: 13px;
  color: #4b5563;
  line-height: 1.5;
}
.share-dialog {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.share-file-head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border-radius: 10px;
  background: linear-gradient(180deg, #fbfcff 0%, #f5f8ff 100%);
  border: 1px solid #e8eefb;
}
.share-file-badge {
  width: 64px;
  flex-shrink: 0;
}
.share-file-info {
  min-width: 0;
}
.share-file-name {
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
  word-break: break-word;
}
.share-file-desc {
  margin-top: 4px;
  font-size: 12px;
  color: #667085;
}
.share-search {
  width: 100%;
}
.share-quick-strip {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.share-quick-label {
  font-size: 12px;
  font-weight: 600;
  color: #667085;
}
.share-quick-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}
.share-quick-btn {
  padding: 12px 12px 10px;
  border-radius: 10px;
  border: 1px solid #e8eefb;
  background: linear-gradient(180deg, #ffffff 0%, #f7faff 100%);
  display: flex;
  flex-direction: column;
  gap: 4px;
  text-align: left;
  cursor: pointer;
  transition: border-color .18s, box-shadow .18s, transform .18s;
}
.share-quick-btn:hover:not(:disabled) {
  border-color: #cfe1ff;
  box-shadow: 0 8px 18px rgba(22, 119, 255, 0.08);
  transform: translateY(-1px);
}
.share-quick-btn:disabled {
  cursor: default;
  opacity: 0.6;
}
.share-quick-name {
  font-size: 13px;
  font-weight: 600;
  color: #1d2129;
  word-break: break-word;
}
.share-quick-meta {
  font-size: 11px;
  color: #98a2b3;
}
.share-session-list {
  max-height: 360px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.share-session-row {
  width: 100%;
  padding: 14px 16px;
  border-radius: 10px;
  border: 1px solid #eef2f6;
  background: #fff;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  text-align: left;
  cursor: pointer;
  transition: border-color .18s, box-shadow .18s, transform .18s, background .18s;
}
.share-session-row:hover,
.share-session-row.active {
  border-color: #cfe1ff;
  background: #f7faff;
  box-shadow: 0 10px 22px rgba(22, 119, 255, 0.08);
}
.share-session-row:hover {
  transform: translateY(-1px);
}
.share-session-main,
.share-session-side {
  min-width: 0;
}
.share-session-main {
  flex: 1;
}
.share-session-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.share-session-title {
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
  word-break: break-word;
}
.share-session-preview {
  margin-top: 6px;
  font-size: 12px;
  color: #667085;
  line-height: 1.5;
}
.share-session-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  flex-shrink: 0;
}
.share-session-time {
  font-size: 12px;
  color: #98a2b3;
}
.share-session-unread {
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 999px;
  background: #1677ff;
  color: #fff;
  font-size: 11px;
  line-height: 20px;
  text-align: center;
}
.preview-empty {
  min-height: 520px;
  display: flex;
  align-items: center;
  justify-content: center;
}
@media (max-width: 1100px) {
  .stat-row,
  .scene-strip,
  .workspace-panel,
  .preview-grid {
    grid-template-columns: 1fr;
  }
  .workspace-left {
    border-right: 0;
    border-bottom: 1px solid #f0f0f0;
  }
}
@media (max-width: 768px) {
  .stat-row {
    grid-template-columns: 1fr 1fr;
  }
  .share-quick-list {
    grid-template-columns: 1fr;
  }
  .file-row {
    flex-direction: column;
    align-items: stretch;
  }
  .file-side {
    align-items: flex-start;
  }
  .documents-top {
    padding: 10px 12px 8px;
  }
  .page-back-btn {
    height: 32px;
    padding: 0 10px;
    font-size: 13px;
  }
  .ptitle {
    font-size: 17px;
  }
  .page-section-label {
    display: none;
  }
  .documents-body {
    padding: 10px 12px;
    padding-bottom: calc(12px + var(--dt-bottom-tab-height));
  }
  .workspace-panel {
    flex-direction: column;
  }
  .workspace-left {
    width: 100%;
    border-right: none;
    border-bottom: 1px solid #f0f0f0;
  }
  .workspace-right {
    display: none;
  }
  .workspace-panel.detail-active {
    position: fixed;
    inset: 0;
    z-index: 100;
    overflow-y: auto;
    background: #fff;
  }
  .workspace-panel.detail-active .workspace-left {
    display: none;
  }
  .workspace-panel.detail-active .workspace-right {
    display: block;
  }
  .detail-back-btn {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 6px 12px;
    border: none;
    border-radius: 8px;
    background: #f0f2f5;
    color: #1d2129;
    font-size: 14px;
    cursor: pointer;
    flex-shrink: 0;
  }
  .detail-back-btn:hover {
    background: #e5e6eb;
  }
  .preview-head {
    gap: 8px;
  }
}
@media (max-width: 960px) {
  .documents-top {
    padding: 14px 16px 12px;
  }
  .documents-top-inner {
    align-items: flex-start;
    flex-direction: column;
  }
  .page-title-cluster,
  .page-top-actions {
    width: 100%;
  }
  .page-top-actions {
    flex-wrap: wrap;
  }
  .documents-body {
    padding: 16px;
  }
}
</style>

<template>
  <div class="mailbox-shell">
    <AppSideNav active-key="mailbox" />

    <div class="mailbox-page">
      <div class="mailbox-top">
        <div class="mailbox-top-inner">
          <div class="page-title-cluster">
            <el-button class="page-back-btn" :icon="Back" @click="router.push('/chat')">返回消息</el-button>
            <div class="page-title-block">
              <div class="page-section-label">协同应用</div>
              <div class="page-heading-row">
                <span class="ptitle">企业邮箱</span>
                <span class="top-live-chip">{{ mailboxSyncText }}</span>
              </div>
            </div>
          </div>
          <div class="page-top-actions">
            <el-button type="primary" @click="openCompose">写邮件</el-button>
            <el-button plain @click="resetSamples">恢复样例</el-button>
          </div>
        </div>
      </div>

      <div class="mailbox-body">
      <div class="stat-row">
        <div class="stat-card metric-unread">
          <div class="stat-num">{{ unreadCount }}</div>
          <div class="stat-label">未读邮件</div>
          <div class="stat-tip">优先处理今天需要确认的事项</div>
        </div>
        <div class="stat-card metric-starred">
          <div class="stat-num">{{ starredCount }}</div>
          <div class="stat-label">星标邮件</div>
          <div class="stat-tip">通常是还要追踪的高优先级沟通</div>
        </div>
        <div class="stat-card metric-today">
          <div class="stat-num">{{ todayCount }}</div>
          <div class="stat-label">今天新到</div>
          <div class="stat-tip">可直接打开阅读区处理</div>
        </div>
        <div class="stat-card metric-draft">
          <div class="stat-num">{{ draftCount }}</div>
          <div class="stat-label">回复草稿</div>
          <div class="stat-tip">支持保存草稿和套用回复示例</div>
        </div>
      </div>

      <button v-if="latestMailboxNotice" type="button" class="live-strip mailbox" @click="focusLatestMail">
        <span class="live-strip-badge">实时提醒</span>
        <div class="live-strip-main">
          <div class="live-strip-title">{{ latestMailboxNotice.title }}</div>
          <div class="live-strip-desc">{{ latestMailboxNotice.summary }}</div>
        </div>
        <span class="live-strip-meta">{{ mailboxSyncText }}</span>
      </button>

      <div class="mailbox-layout" :class="{ 'detail-active': detailActive }">
        <div class="mail-list-wrap">
          <div class="mail-toolbar">
            <el-tabs v-model="activeTab" class="mail-tabs">
              <el-tab-pane label="全部" name="all" />
              <el-tab-pane label="未读" name="unread" />
              <el-tab-pane label="星标" name="starred" />
              <el-tab-pane label="我发出的" name="sent" />
            </el-tabs>
            <el-input
              v-model="keyword"
              class="mail-search"
              clearable
              placeholder="搜索发件人、主题或标签"
              :prefix-icon="Search"
            />
          </div>

          <div class="mail-list">
            <div
              v-for="item in visibleMails"
              :key="item.id"
              class="mail-item"
              :class="{ active: currentMail?.id === item.id, unread: item.unread }"
              @click="selectMail(item)"
            >
              <div class="mail-item-left">
                <div class="mail-avatar" :style="avatarStyle(item.sender)">{{ firstChar(item.sender) }}</div>
              </div>
              <div class="mail-item-main">
                <div class="mail-item-head">
                  <div class="mail-sender-row">
                    <span class="mail-sender">{{ displaySender(item) }}</span>
                    <span class="priority-dot" :class="`priority-${item.priority}`"></span>
                  </div>
                  <div class="mail-head-actions">
                    <span class="mail-time">{{ item.time }}</span>
                    <button type="button" class="icon-btn" @click.stop="toggleStar(item)">{{ item.starred ? '★' : '☆' }}</button>
                  </div>
                </div>
                <div class="mail-subject">{{ item.subject }}</div>
                <div class="mail-preview">{{ item.preview }}</div>
                <div class="mail-tags">
                  <el-tag v-if="item.direction === 'sent'" size="small" type="info" effect="plain">我发出的</el-tag>
                  <el-tag size="small" effect="plain">{{ item.tag }}</el-tag>
                  <el-tag size="small" effect="plain" :type="priorityType(item.priority)">{{ priorityLabel(item.priority) }}</el-tag>
                  <span v-if="item.attachments?.length" class="attach-count">{{ item.attachments.length }} 个附件</span>
                </div>
              </div>
            </div>
            <el-empty v-if="!visibleMails.length" description="当前分类没有邮件" :image-size="84" />
          </div>
        </div>

        <div class="mail-detail-wrap">
          <template v-if="currentMail">
            <div class="detail-head">
              <button type="button" class="detail-back-btn" @click="closeDetail"><el-icon><ArrowLeft /></el-icon> 返回</button>
              <div class="detail-header-main">
                <div class="detail-pills">
                  <span v-if="currentMail.direction === 'sent'" class="state-pill sent">我发出的</span>
                  <span class="state-pill" :class="{ unread: currentMail.unread }">{{ currentMail.unread ? '未读' : '已读' }}</span>
                  <span v-if="currentMail.starred" class="state-pill star">星标关注</span>
                </div>
                <div class="detail-title">{{ currentMail.subject }}</div>
                <div class="detail-meta">{{ mailMetaText(currentMail) }}</div>
              </div>
              <div class="detail-actions">
                <el-button v-if="currentMail.direction !== 'sent'" text @click="toggleRead(currentMail)">{{ currentMail.unread ? '标记已读' : '标记未读' }}</el-button>
                <el-button text @click="toggleStar(currentMail)">{{ currentMail.starred ? '取消星标' : '设为星标' }}</el-button>
                <el-button text type="danger" @click="archiveMail(currentMail)">归档</el-button>
              </div>
            </div>

            <div class="summary-panel">
              <div class="summary-item">
                <span class="summary-label">发件人</span>
                <span class="summary-value">{{ currentMail.direction === 'sent' ? '我' : currentMail.sender }}</span>
              </div>
              <div class="summary-item">
                <span class="summary-label">收件人</span>
                <span class="summary-value">{{ currentMail.recipients.join('、') }}</span>
              </div>
              <div class="summary-item">
                <span class="summary-label">抄送</span>
                <span class="summary-value">{{ currentMail.cc.join('、') || '无' }}</span>
              </div>
              <div class="summary-item">
                <span class="summary-label">标签 / 优先级</span>
                <span class="summary-value">
                  <el-tag size="small" effect="plain">{{ currentMail.tag }}</el-tag>
                  <el-tag size="small" effect="plain" :type="priorityType(currentMail.priority)">{{ priorityLabel(currentMail.priority) }}</el-tag>
                </span>
              </div>
            </div>

            <div class="detail-section">
              <div class="section-title">邮件正文</div>
              <div class="detail-content">
                <p v-for="(paragraph, index) in mailParagraphs(currentMail.content)" :key="index" class="content-paragraph">
                  {{ paragraph }}
                </p>
              </div>
            </div>

            <div v-if="currentMail.attachments?.length" class="detail-section">
              <div class="section-title">附件</div>
              <div class="attach-grid">
                <div v-for="file in currentMail.attachments" :key="file.name" class="attach-item">
                  <div class="attach-left">
                    <div class="attach-badge" :class="`theme-${attachmentMeta(file.name).theme}`">
                      {{ attachmentMeta(file.name).label }}
                    </div>
                    <div>
                      <div class="attach-name">{{ file.name }}</div>
                      <div class="attach-sub">{{ attachmentMeta(file.name).hint }}</div>
                    </div>
                  </div>
                  <div class="attach-actions">
                    <span class="attach-size">{{ file.size }}</span>
                    <el-button text type="primary" @click="downloadAttachment(file)">下载</el-button>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="currentMail.useCases?.length" class="detail-section">
              <div class="section-title">快捷处理用例</div>
              <div class="usecase-grid">
                <div v-for="item in currentMail.useCases" :key="item.title" class="usecase-card">
                  <div class="usecase-title">{{ item.title }}</div>
                  <div class="usecase-desc">{{ item.description }}</div>
                  <div class="usecase-footer">
                    <el-tag size="small" effect="plain">{{ item.tag }}</el-tag>
                    <el-button size="small" text type="primary" @click="applyQuickReply(item)">套用</el-button>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="canReplyCurrentMail" class="detail-section">
              <div class="section-title">回复草稿</div>
              <div class="reply-panel">
                <div class="reply-tools">
                  <div class="reply-hint">支持先保存草稿，也可以先套用上面的回复用例再继续编辑。</div>
                  <div class="reply-shortcuts">
                    <button type="button" class="mini-action" @click="insertSignature">插入签名</button>
                    <button type="button" class="mini-action" @click="clearDraft">清空草稿</button>
                  </div>
                </div>
                <el-input
                  v-model="replyDraft"
                  type="textarea"
                  :rows="5"
                  placeholder="输入回复内容，支持先保存草稿再处理。"
                  @input="syncDraft"
                />
                <div class="reply-footer">
                  <span class="draft-tip">{{ draftStatusText }}</span>
                  <span class="draft-count">{{ replyDraft.length }} 字</span>
                </div>
              </div>
              <div class="reply-actions">
                <el-button @click="saveDraft">保存草稿</el-button>
                <el-button type="primary" @click="sendReply">发送回复</el-button>
              </div>
            </div>

            <div v-else class="detail-section">
              <div class="section-title">发送状态</div>
              <div class="detail-note">
                {{ currentMail.direction === 'sent' ? '这封邮件已经发出，可切换不同账号查看收件效果。' : '这类系统邮件暂不支持直接回复。' }}
              </div>
            </div>

            <div class="detail-section">
              <div class="section-title">处理记录</div>
              <div class="history-list">
                <div v-for="record in currentMail.history" :key="`${record.time}-${record.text}`" class="history-item">
                  <span class="history-time">{{ record.time }}</span>
                  <span class="history-text">{{ record.text }}</span>
                </div>
              </div>
            </div>
          </template>
          <div v-else class="detail-empty">
            <el-empty description="暂无邮件" :image-size="88" />
          </div>
        </div>
      </div>
      </div>
    </div>

    <el-dialog v-model="composeVisible" title="写邮件" width="720px" destroy-on-close>
      <div class="compose-grid">
      <div class="compose-row">
        <div class="compose-field wide">
          <div class="compose-label">收件人</div>
          <el-select
            v-model="composeForm.recipientIds"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            placeholder="选择收件人"
          >
            <el-option
              v-for="user in contacts"
              :key="user.id"
              :label="contactLabel(user)"
              :value="user.id"
            />
          </el-select>
        </div>
      </div>
      <div class="compose-row">
        <div class="compose-field wide">
          <div class="compose-label">抄送</div>
          <el-select
            v-model="composeForm.ccIds"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            placeholder="可选抄送人"
          >
            <el-option
              v-for="user in contacts"
              :key="`cc-${user.id}`"
              :label="contactLabel(user)"
              :value="user.id"
            />
          </el-select>
        </div>
      </div>
      <div class="compose-row">
        <div class="compose-field wide">
          <div class="compose-label">主题</div>
          <el-input v-model="composeForm.subject" placeholder="输入邮件主题" maxlength="80" show-word-limit />
        </div>
      </div>
      <div class="compose-row dual">
        <div class="compose-field">
          <div class="compose-label">分类</div>
          <el-input v-model="composeForm.tag" placeholder="如：项目 / 研发 / 客户" maxlength="20" />
        </div>
        <div class="compose-field">
          <div class="compose-label">优先级</div>
          <el-select v-model="composeForm.priority">
            <el-option label="普通" value="medium" />
            <el-option label="高优" value="high" />
            <el-option label="低优" value="low" />
          </el-select>
        </div>
      </div>
      <div class="compose-row">
        <div class="compose-field wide">
          <div class="compose-label">正文</div>
          <el-input
            v-model="composeForm.content"
            type="textarea"
            :rows="9"
            maxlength="2000"
            show-word-limit
            placeholder="输入邮件正文，发送后会写入对方邮箱。"
          />
        </div>
      </div>
      <div class="compose-row">
        <div class="compose-field wide">
          <div class="compose-label">附件</div>
          <div class="compose-attach-box">
            <el-upload
              :show-file-list="false"
              multiple
              :limit="6"
              :http-request="uploadMailAttachment"
              :before-upload="beforeAttachmentUpload"
            >
              <el-button plain :loading="attachmentUploading">上传附件</el-button>
            </el-upload>
            <span class="compose-upload-tip">支持真实上传，发送后收件人可直接下载。</span>
          </div>
          <div v-if="attachmentFiles.length" class="compose-attachment-list">
            <div v-for="file in attachmentFiles" :key="file.id" class="compose-attachment-item">
              <div class="compose-attachment-main">
                <div class="compose-attachment-name">{{ file.name }}</div>
                <div class="compose-attachment-meta">{{ formatComposeAttachment(file) }}</div>
              </div>
              <button type="button" class="compose-attachment-remove" @click="removeAttachment(file)">移除</button>
            </div>
          </div>
        </div>
      </div>
    </div>
      <template #footer>
        <div class="compose-footer">
        <span class="compose-tip">发送后会同步写入收件人邮箱，并在当前账号生成“我发出的”记录。</span>
        <div class="compose-actions">
          <el-button @click="composeVisible = false">取消</el-button>
          <el-button type="primary" :loading="sendingCompose" @click="submitCompose">发送</el-button>
        </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, Search, ArrowLeft } from '@element-plus/icons-vue'
import AppSideNav from '../components/AppSideNav.vue'
import { useCollabStore } from '../store/collab'
import { useUserPreferenceStore } from '../store/userPreference'
import {
  apiMailboxArchive,
  apiMailboxList,
  apiMailboxOpen,
  apiMailboxReply,
  apiMailboxReset,
  apiMailboxSend,
  apiMailboxSaveDraft,
  apiMailboxUpdateRead,
  apiMailboxUpdateStar,
  apiUpload
} from '../api'
import { apiUserList } from '../api'

const router = useRouter()
const collabStore = useCollabStore()
const preferenceStore = useUserPreferenceStore()
const activeTab = ref('all')
const keyword = ref('')
const mails = ref([])
const currentId = ref(null)
const detailActive = ref(false)
const mailboxStateReady = ref(false)
const replyDraft = ref('')
const draftDirty = ref(false)
const contacts = ref([])
const composeVisible = ref(false)
const sendingCompose = ref(false)
const attachmentFiles = ref([])
const attachmentUploadCount = ref(0)
const composeForm = ref(createComposeForm())

const visibleMails = computed(() => mails.value.filter(mail => {
  if (mail.archived) return false
  if (activeTab.value === 'sent' && mail.direction !== 'sent') return false
  if (activeTab.value === 'unread' && !mail.unread) return false
  if (activeTab.value === 'starred' && !mail.starred) return false
  if (!keyword.value.trim()) return true
  const text = `${mail.sender} ${mail.subject} ${mail.preview} ${mail.tag} ${(mail.recipients || []).join(' ')} ${(mail.cc || []).join(' ')}`.toLowerCase()
  return text.includes(keyword.value.trim().toLowerCase())
}))
const currentMail = computed(() => visibleMails.value.find(mail => mail.id === currentId.value) || visibleMails.value[0] || null)
const unreadCount = computed(() => mails.value.filter(mail => !mail.archived && mail.unread).length)
const starredCount = computed(() => mails.value.filter(mail => !mail.archived && mail.starred).length)
const todayCount = computed(() => mails.value.filter(mail => !mail.archived && String(mail.time || '').startsWith('今天')).length)
const draftCount = computed(() => mails.value.filter(mail => !mail.archived && (mail.draft || '').trim()).length)
const canReplyCurrentMail = computed(() => Boolean(currentMail.value && currentMail.value.direction !== 'sent' && currentMail.value.senderId))
const latestMailboxNotice = computed(() => collabStore.workNotices.find(item => item.category === 'mailbox') || null)
const mailboxSyncText = computed(() => collabStore.mailboxUnread ? `${collabStore.mailboxUnread} 封未读` : '已实时同步')
const draftStatusText = computed(() => {
  if (!currentMail.value) return '当前没有已保存的草稿'
  if (draftDirty.value) return '当前草稿尚未保存到后端'
  return currentMail.value.draft?.trim() ? '草稿已保存到后端' : '当前没有已保存的草稿'
})

watch(() => preferenceStore.loaded, (loaded) => {
  if (!loaded || mailboxStateReady.value) return
  const state = preferenceStore.getPageState('mailbox')
  if (typeof state.activeTab === 'string') activeTab.value = state.activeTab
  if (typeof state.keyword === 'string') keyword.value = state.keyword
  if (Number.isFinite(Number(state.currentId)) && Number(state.currentId) > 0) {
    currentId.value = Number(state.currentId)
  }
  mailboxStateReady.value = true
}, { immediate: true })

watch(visibleMails, (list) => {
  if (!list.length) {
    currentId.value = null
    return
  }
  if (!list.some(mail => mail.id === currentId.value)) currentId.value = list[0].id
}, { immediate: true })

watch(currentMail, (mail) => {
  replyDraft.value = mail?.draft || ''
  draftDirty.value = false
}, { immediate: true })

watch(() => latestMailboxNotice.value?.id, async (noticeId, previousId) => {
  if (!noticeId || noticeId === previousId) return
  await loadMails()
})

watch([activeTab, keyword, currentId], () => {
  if (!mailboxStateReady.value || !preferenceStore.loaded) return
  preferenceStore.setPageState('mailbox', {
    activeTab: activeTab.value,
    keyword: keyword.value,
    currentId: currentId.value
  })
})

onMounted(() => {
  loadMails(false)
  loadContacts()
})

async function loadMails(preserveSelection = true) {
  const selectedId = preserveSelection ? currentId.value : null
  const data = await apiMailboxList()
  mails.value = Array.isArray(data) ? data : []
  if (selectedId && mails.value.some(mail => mail.id === selectedId)) {
    currentId.value = selectedId
    return
  }
  currentId.value = mails.value[0]?.id || null
}

async function loadContacts() {
  const data = await apiUserList()
  contacts.value = Array.isArray(data) ? data : []
}

async function selectMail(mail) {
  currentId.value = mail.id
  if (window.innerWidth <= 768) {
    detailActive.value = true
  }
  if (!mail.unread) return
  mail.unread = 0
  try {
    await apiMailboxOpen(mail.id)
  } catch (error) {
    mail.unread = 1
  }
}

function closeDetail() {
  detailActive.value = false
}

async function focusLatestMail() {
  const notice = latestMailboxNotice.value
  if (!notice) return
  activeTab.value = 'all'
  if (!mails.value.some(mail => mail.id === notice.refId)) {
    await loadMails()
  }
  const target = mails.value.find(mail => mail.id === notice.refId)
  if (!target) return
  await selectMail(target)
}

async function toggleStar(mail) {
  const starred = !Boolean(mail.starred)
  await apiMailboxUpdateStar(mail.id, starred)
  await loadMails()
}

async function toggleRead(mail) {
  const unread = !Boolean(mail.unread)
  await apiMailboxUpdateRead(mail.id, unread)
  await loadMails()
}

async function archiveMail(mail) {
  await apiMailboxArchive(mail.id)
  await loadMails()
  ElMessage.success('邮件已归档')
}

function applyQuickReply(item) {
  replyDraft.value = item.template || ''
  draftDirty.value = true
}

function syncDraft() {
  draftDirty.value = true
}

async function saveDraft() {
  if (!currentMail.value) return
  await apiMailboxSaveDraft(currentMail.value.id, replyDraft.value)
  await loadMails()
  ElMessage.success('草稿已保存')
}

async function sendReply() {
  if (!currentMail.value) return
  if (!canReplyCurrentMail.value) {
    ElMessage.warning('这封邮件暂不支持直接回复')
    return
  }
  if (!replyDraft.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  await apiMailboxReply(currentMail.value.id, replyDraft.value.trim())
  await loadMails()
  ElMessage.success('回复已发送')
}

function insertSignature() {
  const signature = '洪辰新\n技术研发部'
  if (!replyDraft.value.trim()) replyDraft.value = signature
  else if (!replyDraft.value.includes(signature)) replyDraft.value = `${replyDraft.value.trim()}\n\n${signature}`
  draftDirty.value = true
}

function clearDraft() {
  replyDraft.value = ''
  draftDirty.value = true
  ElMessage.success('已清空编辑区')
}

function mailParagraphs(content) {
  return String(content || '').split('\n\n').filter(Boolean)
}

function attachmentMeta(name) {
  const ext = ((name || '').split('.').pop() || '').toLowerCase()
  if (ext === 'pdf') return { label: 'PDF', theme: 'pdf', hint: '适合直接查看结论版材料' }
  if (['doc', 'docx'].includes(ext)) return { label: 'WORD', theme: 'word', hint: '文档类附件，适合继续编辑' }
  if (['xls', 'xlsx', 'csv'].includes(ext)) return { label: 'EXCEL', theme: 'excel', hint: '数据表格，适合核对时间和数值' }
  if (['ppt', 'pptx'].includes(ext)) return { label: 'PPT', theme: 'ppt', hint: '演示材料，适合会前预览' }
  return { label: (ext || 'FILE').toUpperCase(), theme: 'file', hint: '通用附件，可下载到本地查看' }
}

function downloadAttachment(file) {
  if (file.url) {
    triggerAttachmentDownload(file.url, file.name)
    return
  }
  const blob = new Blob([file.content || `${file.name} 示例附件`], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = file.name
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

function triggerAttachmentDownload(url, name) {
  const link = document.createElement('a')
  link.href = url
  link.download = name || ''
  link.rel = 'noopener'
  document.body.appendChild(link)
  link.click()
  link.remove()
}

async function resetSamples() {
  await apiMailboxReset()
  await loadMails(false)
  ElMessage.success('样例已恢复')
}

function createComposeForm() {
  return {
    recipientIds: [],
    ccIds: [],
    attachmentIds: [],
    subject: '',
    tag: '协作',
    priority: 'medium',
    content: ''
  }
}

function openCompose() {
  composeForm.value = createComposeForm()
  attachmentFiles.value = []
  composeVisible.value = true
}

const attachmentUploading = computed(() => attachmentUploadCount.value > 0)

async function submitCompose() {
  if (!composeForm.value.recipientIds.length) {
    ElMessage.warning('请至少选择一位收件人')
    return
  }
  if (!composeForm.value.subject.trim()) {
    ElMessage.warning('请输入邮件主题')
    return
  }
  if (!composeForm.value.content.trim()) {
    ElMessage.warning('请输入邮件正文')
    return
  }
  if (attachmentUploading.value) {
    ElMessage.warning('附件还在上传中，请稍后再发送')
    return
  }
  sendingCompose.value = true
  try {
    await apiMailboxSend({
      ...composeForm.value,
      subject: composeForm.value.subject.trim(),
      content: composeForm.value.content.trim(),
      tag: composeForm.value.tag.trim()
    })
    composeVisible.value = false
    activeTab.value = 'sent'
    await loadMails(false)
    ElMessage.success('邮件已发送')
  } finally {
    sendingCompose.value = false
  }
}

function beforeAttachmentUpload(file) {
  if (!file) return false
  if (file.size > 100 * 1024 * 1024) {
    ElMessage.error('附件大小不能超过 100MB')
    return false
  }
  return true
}

async function uploadMailAttachment(option) {
  const fd = new FormData()
  fd.append('file', option.file)
  attachmentUploadCount.value += 1
  try {
    const uploaded = await apiUpload(fd, { timeout: 120000 })
    attachmentFiles.value = [
      ...attachmentFiles.value,
      {
        id: uploaded.id,
        name: uploaded.name,
        size: uploaded.size,
        url: uploaded.url,
        type: uploaded.type
      }
    ]
    composeForm.value.attachmentIds = attachmentFiles.value.map(file => file.id)
    option.onSuccess?.(uploaded)
    ElMessage.success(`已上传附件：${uploaded.name}`)
  } catch (error) {
    option.onError?.(error)
    throw error
  } finally {
    attachmentUploadCount.value = Math.max(0, attachmentUploadCount.value - 1)
  }
}

function removeAttachment(file) {
  attachmentFiles.value = attachmentFiles.value.filter(item => item.id !== file.id)
  composeForm.value.attachmentIds = attachmentFiles.value.map(item => item.id)
}

function formatComposeAttachment(file) {
  const meta = attachmentMeta(file.name)
  const size = typeof file.size === 'number' ? formatAttachmentBytes(file.size) : file.size
  return [meta.label, size].filter(Boolean).join(' · ')
}

function formatAttachmentBytes(size) {
  if (!size) return ''
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

function firstChar(name) {
  return name ? name.charAt(0) : '?'
}

function avatarStyle(name) {
  const colors = ['#1677ff', '#52c41a', '#fa8c16', '#722ed1', '#13c2c2', '#eb2f96']
  let hash = 0
  for (const ch of name || '?') hash += ch.charCodeAt(0)
  return { background: colors[hash % colors.length] }
}

function priorityLabel(priority) {
  return { high: '高优', medium: '普通', low: '低优' }[priority] || '普通'
}

function priorityType(priority) {
  return { high: 'danger', medium: 'warning', low: 'info' }[priority] || 'info'
}

function contactLabel(user) {
  const role = user.jobTitle ? ` · ${user.jobTitle}` : ''
  return `${user.nickname}${role}`
}

function displaySender(mail) {
  if (mail.direction === 'sent') return `发给 ${summarizeNames(mail.recipients)}`
  return mail.sender
}

function summarizeNames(names = []) {
  if (!names.length) return '未填写'
  if (names.length === 1) return names[0]
  return `${names[0]} 等 ${names.length} 人`
}

function mailMetaText(mail) {
  if (!mail) return ''
  if (mail.direction === 'sent') return `你发出 · ${mail.time}`
  return `${mail.sender} · ${mail.senderRole} · ${mail.time}`
}
</script>

<style scoped>
.mailbox-shell {
  height: 100%;
  display: flex;
  background: var(--dt-bg);
}
.mailbox-page {
  flex: 1;
  min-width: 0;
  height: 100%;
  background: #f3f5f8;
  display: flex;
  flex-direction: column;
}
.mailbox-top {
  padding: 16px 24px 14px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96) 0%, rgba(247, 249, 252, 0.92) 100%);
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(18px);
  flex-shrink: 0;
}
.mailbox-top-inner {
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
.page-title-block {
  min-width: 0;
}
.page-section-label {
  margin-bottom: 4px;
  color: #98a2b3;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0;
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
.top-live-chip {
  display: inline-flex;
  align-items: center;
  height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: #eef4ff;
  color: #245fcb;
  font-size: 12px;
  font-weight: 600;
}
.mailbox-body {
  flex: 1;
  overflow-y: auto;
  padding: 18px 24px 24px;
  max-width: 1260px;
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
.live-strip {
  width: 100%;
  margin-bottom: 16px;
  border: 1px solid #dbe7ff;
  border-radius: 12px;
  background: linear-gradient(90deg, #f6f9ff 0%, #ffffff 100%);
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  cursor: pointer;
  transition: border-color .18s, box-shadow .18s, transform .18s;
}
.live-strip:hover {
  border-color: #b8d0ff;
  box-shadow: 0 10px 24px rgba(22, 119, 255, 0.08);
  transform: translateY(-1px);
}
.live-strip.mailbox {
  border-left: 4px solid #5f6cf8;
}
.live-strip-badge {
  flex-shrink: 0;
  min-width: 68px;
  height: 28px;
  border-radius: 999px;
  background: #e8eeff;
  color: #4557d8;
  font-size: 12px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.live-strip-main {
  min-width: 0;
  flex: 1;
}
.live-strip-title {
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
}
.live-strip-desc {
  margin-top: 4px;
  font-size: 12px;
  color: #6b7280;
  line-height: 1.5;
}
.live-strip-meta {
  flex-shrink: 0;
  color: #4557d8;
  font-size: 12px;
  font-weight: 600;
}
@media (max-width: 960px) {
  .mailbox-top {
    padding: 14px 16px 12px;
  }
  .mailbox-top-inner {
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
  .mailbox-body {
    padding: 16px;
  }
}
.stat-card,
.mail-list-wrap,
.mail-detail-wrap {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, #ffffff 100%);
  border: 1px solid rgba(15, 23, 42, 0.07);
  border-radius: 14px;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.04), 0 1px 2px rgba(15, 23, 42, 0.03);
}
.stat-card {
  position: relative;
  overflow: hidden;
  min-height: 112px;
  padding: 18px 20px 16px;
}
.stat-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 3px;
  background: var(--metric-accent, rgba(95, 108, 248, 0.3));
}
.metric-unread {
  --metric-accent: #5f6cf8;
  background: linear-gradient(180deg, rgba(95, 108, 248, 0.08) 0%, #ffffff 34%);
}
.metric-starred {
  --metric-accent: #f59e0b;
  background: linear-gradient(180deg, rgba(245, 158, 11, 0.08) 0%, #ffffff 34%);
}
.metric-today {
  --metric-accent: #1677ff;
  background: linear-gradient(180deg, rgba(22, 119, 255, 0.08) 0%, #ffffff 34%);
}
.metric-draft {
  --metric-accent: #7c3aed;
  background: linear-gradient(180deg, rgba(124, 58, 237, 0.08) 0%, #ffffff 34%);
}
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
.mailbox-layout {
  display: grid;
  grid-template-columns: 390px minmax(0, 1fr);
  gap: 16px;
  min-height: 720px;
}
.mail-list-wrap {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}
.mail-toolbar {
  padding: 0 18px 14px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.92) 0%, rgba(255, 255, 255, 0.98) 100%);
}
.mail-tabs :deep(.el-tabs__header) {
  margin-bottom: 8px;
}
.mail-search {
  max-width: 320px;
}
.mail-list {
  flex: 1;
  overflow-y: auto;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.mail-item {
  position: relative;
  display: flex;
  gap: 12px;
  padding: 15px 16px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 12px;
  background: linear-gradient(180deg, #ffffff 0%, #fcfdff 100%);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.03);
  cursor: pointer;
  transition: transform .18s, box-shadow .18s, border-color .18s, background .18s;
}
.mail-item.unread {
  background: linear-gradient(180deg, rgba(247, 250, 255, 0.96) 0%, #ffffff 100%);
}
.mail-item:hover {
  transform: translateY(-1px);
  background: #ffffff;
  border-color: rgba(22, 119, 255, 0.16);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
}
.mail-item.active {
  background: linear-gradient(180deg, rgba(246, 249, 255, 0.98) 0%, #ffffff 100%);
  border-color: rgba(22, 119, 255, 0.24);
  box-shadow: 0 14px 30px rgba(22, 119, 255, 0.08);
}
.mail-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 14px;
  bottom: 14px;
  width: 3px;
  border-radius: 999px;
  background: #1677ff;
}
.mail-item.unread .mail-sender,
.mail-item.unread .mail-subject {
  color: #1677ff;
}
.mail-avatar {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 700;
}
.mail-item-main {
  min-width: 0;
  flex: 1;
}
.mail-item-head,
.mail-sender-row,
.mail-head-actions,
.mail-tags,
.detail-head,
.detail-actions,
.reply-actions,
.usecase-footer {
  display: flex;
  align-items: center;
}
.mail-item-head,
.detail-head,
.usecase-footer {
  justify-content: space-between;
}
.mail-sender-row {
  gap: 8px;
}
.mail-head-actions {
  gap: 10px;
}
.mail-sender {
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
}
.priority-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}
.priority-high { background: #ff4d4f; }
.priority-medium { background: #faad14; }
.priority-low { background: #8c8c8c; }
.mail-time,
.mail-preview,
.attach-count,
.detail-meta,
.attach-size,
.history-time {
  font-size: 12px;
  color: #8c8c8c;
}
.icon-btn {
  border: 0;
  background: transparent;
  color: #fa8c16;
  cursor: pointer;
  font-size: 16px;
  line-height: 1;
}
.mail-subject,
.detail-title,
.section-title,
.usecase-title,
.attach-name {
  font-weight: 600;
  color: #1d2129;
}
.mail-subject {
  margin-top: 7px;
  font-size: 14px;
}
.detail-title {
  font-size: 20px;
}
.mail-preview {
  margin-top: 6px;
  line-height: 1.6;
}
.mail-tags {
  gap: 8px;
  margin-top: 10px;
  flex-wrap: wrap;
}
.mail-detail-wrap {
  min-height: 0;
  padding: 24px 26px;
}
.detail-header-main {
  min-width: 0;
}
.detail-pills {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}
.state-pill {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  background: #f4f5f7;
  color: #6b7280;
  font-size: 12px;
  border: 1px solid #eceef2;
}
.state-pill.unread {
  background: rgba(22, 119, 255, 0.08);
  color: #1677ff;
  border-color: rgba(22, 119, 255, 0.16);
}
.state-pill.star {
  background: rgba(250, 140, 22, 0.09);
  color: #d46b08;
  border-color: rgba(250, 140, 22, 0.18);
}
.state-pill.sent {
  background: rgba(91, 102, 246, 0.08);
  color: #5b66f6;
  border-color: rgba(91, 102, 246, 0.18);
}
.detail-head {
  gap: 16px;
  flex-wrap: wrap;
}
.detail-actions {
  gap: 8px;
  flex-wrap: wrap;
}
.detail-meta {
  margin-top: 8px;
}
.summary-panel {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}
.summary-item {
  padding: 12px 14px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 10px;
  background: linear-gradient(180deg, #ffffff 0%, #fafbfc 100%);
}
.summary-label {
  display: block;
  font-size: 12px;
  color: #8c8c8c;
}
.summary-value {
  display: block;
  margin-top: 6px;
  font-size: 13px;
  color: #374151;
  line-height: 1.6;
}
.detail-content {
  padding: 18px 20px;
  border-radius: 10px;
  background: linear-gradient(180deg, #ffffff 0%, #fbfcff 100%);
  border: 1px solid rgba(15, 23, 42, 0.06);
  font-size: 14px;
  color: #374151;
  line-height: 1.8;
}
.detail-note {
  padding: 14px 16px;
  border-radius: 10px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  background: linear-gradient(180deg, #ffffff 0%, #fafbfc 100%);
  font-size: 13px;
  color: #5f6b7a;
  line-height: 1.7;
}
.content-paragraph {
  margin: 0;
}
.content-paragraph + .content-paragraph {
  margin-top: 12px;
}
.detail-section {
  margin-top: 22px;
}
.section-title {
  font-size: 14px;
  margin-bottom: 12px;
}
.attach-grid,
.usecase-grid,
.history-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.attach-item,
.usecase-card,
.history-item {
  padding: 14px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 10px;
  background: linear-gradient(180deg, #ffffff 0%, #fbfcff 100%);
}
.attach-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.attach-left,
.attach-actions,
.reply-tools,
.reply-shortcuts,
.reply-footer {
  display: flex;
  align-items: center;
}
.attach-left {
  gap: 12px;
  min-width: 0;
}
.attach-actions {
  gap: 12px;
  flex-shrink: 0;
}
.attach-badge {
  min-width: 58px;
  height: 42px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  background: #f5f6f8;
  color: #4b5563;
  border: 1px solid rgba(15, 23, 42, 0.06);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.85);
}
.attach-badge.theme-pdf { background: rgba(255, 77, 79, 0.08); color: #d9363e; }
.attach-badge.theme-word { background: rgba(47, 84, 235, 0.08); color: #1d4ed8; }
.attach-badge.theme-excel { background: rgba(35, 120, 4, 0.08); color: #237804; }
.attach-badge.theme-ppt { background: rgba(212, 107, 8, 0.08); color: #c96a06; }
.attach-badge.theme-file { background: rgba(89, 89, 89, 0.08); color: #4b5563; }
.attach-sub {
  margin-top: 5px;
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.5;
}
.usecase-desc,
.history-text {
  font-size: 13px;
  color: #6b7280;
  line-height: 1.6;
}
.usecase-desc {
  margin-top: 6px;
}
.reply-panel {
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 10px;
  background: linear-gradient(180deg, #ffffff 0%, #fbfcff 100%);
  padding: 14px;
}
.reply-tools,
.reply-footer {
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.reply-tools {
  margin-bottom: 12px;
}
.reply-hint,
.draft-tip,
.draft-count {
  font-size: 12px;
  color: #8c8c8c;
}
.reply-shortcuts {
  gap: 8px;
}
.mini-action {
  height: 28px;
  padding: 0 10px;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  background: #fff;
  color: #4b5563;
  font-size: 12px;
  cursor: pointer;
  transition: all .18s;
}
.mini-action:hover {
  color: #1677ff;
  border-color: #cfe1ff;
  background: #f7faff;
}
.reply-footer {
  margin-top: 12px;
}
.usecase-footer {
  gap: 12px;
  margin-top: 12px;
}
.reply-actions {
  gap: 8px;
  margin-top: 12px;
  justify-content: flex-end;
}
.history-item {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}
.history-time {
  min-width: 74px;
}
.compose-grid {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.compose-row.dual {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 180px;
  gap: 14px;
}
.compose-field {
  min-width: 0;
}
.compose-field :deep(.el-input),
.compose-field :deep(.el-select) {
  width: 100%;
}
.compose-label {
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}
.compose-attach-box {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.compose-upload-tip {
  font-size: 12px;
  color: #8c8c8c;
}
.compose-attachment-list {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.compose-attachment-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid #edf0f3;
  border-radius: 10px;
  background: #fbfcff;
}
.compose-attachment-main {
  min-width: 0;
}
.compose-attachment-name {
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
  word-break: break-all;
}
.compose-attachment-meta {
  margin-top: 4px;
  font-size: 12px;
  color: #8c8c8c;
}
.compose-attachment-remove {
  border: none;
  background: transparent;
  color: #f56c6c;
  font-size: 12px;
  cursor: pointer;
  flex-shrink: 0;
}
.compose-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.compose-tip {
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.6;
}
.compose-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.detail-empty {
  min-height: 620px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.detail-back-btn {
  display: none;
}
@media (max-width: 1100px) {
  .stat-row,
  .mailbox-layout,
  .summary-panel,
  .compose-row.dual {
    grid-template-columns: 1fr;
  }
  .live-strip {
    align-items: flex-start;
    flex-wrap: wrap;
  }
  .live-strip-meta {
    width: 100%;
  }
}
@media (max-width: 768px) {
  .stat-row {
    grid-template-columns: 1fr 1fr;
  }
  .mailbox-top {
    padding: 10px 12px 8px;
  }
  .mailbox-top-inner {
    flex-direction: column;
    align-items: flex-start;
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
  .mailbox-body {
    padding-bottom: var(--dt-bottom-tab-height);
  }
  .mailbox-layout {
    flex-direction: column;
  }
  .mail-list-wrap {
    width: 100%;
  }
  .mail-detail-wrap {
    display: none;
  }
  .mailbox-layout.detail-active {
    position: fixed;
    inset: 0;
    z-index: 100;
    overflow-y: auto;
    background: #fff;
  }
  .mailbox-layout.detail-active .mail-list-wrap {
    display: none;
  }
  .mailbox-layout.detail-active .mail-detail-wrap {
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
    margin-bottom: 8px;
  }
  .detail-back-btn:hover {
    background: #e5e6eb;
  }
}
</style>

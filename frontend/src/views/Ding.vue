<template>
  <div class="ding-shell">
    <AppSideNav active-key="ding" />

    <div class="ding-page">
      <div class="ding-top">
        <div class="ding-top-inner">
          <div class="page-title-cluster">
            <el-button class="page-back-btn" :icon="Back" @click="router.push('/chat')">返回消息</el-button>
            <div class="page-title-block">
              <div class="page-section-label">协同应用</div>
              <div class="page-heading-row">
                <span class="ptitle">DING 提醒</span>
                <span class="top-live-chip">{{ dingSyncText }}</span>
              </div>
            </div>
          </div>
          <div class="page-top-actions">
            <el-button type="primary" @click="openCompose">发 DING</el-button>
            <el-button plain @click="resetSamples">恢复样例</el-button>
          </div>
        </div>
      </div>

      <div class="ding-body">
      <div class="stat-row">
        <div class="stat-card orange">
          <div class="stat-num">{{ pendingCount }}</div>
          <div class="stat-label">待确认</div>
          <div class="stat-tip">适合先点“确认收到”，再进入处理</div>
        </div>
        <div class="stat-card blue">
          <div class="stat-num">{{ processingCount }}</div>
          <div class="stat-label">处理中</div>
          <div class="stat-tip">已接手但还没有完结的提醒</div>
        </div>
        <div class="stat-card green">
          <div class="stat-num">{{ doneCount }}</div>
          <div class="stat-label">已完成</div>
          <div class="stat-tip">可直接用于演示处理闭环</div>
        </div>
        <div class="stat-card red">
          <div class="stat-num">{{ urgentCount }}</div>
          <div class="stat-label">紧急事项</div>
          <div class="stat-tip">今天必须处理或已多次催办</div>
        </div>
      </div>

      <button v-if="latestDingNotice" type="button" class="live-strip ding" @click="focusLatestDing">
        <span class="live-strip-badge">实时提醒</span>
        <div class="live-strip-main">
          <div class="live-strip-title">{{ latestDingNotice.title }}</div>
          <div class="live-strip-desc">{{ latestDingNotice.summary }}</div>
        </div>
        <span class="live-strip-meta">{{ dingSyncText }}</span>
      </button>

      <div class="scenario-strip">
        <div v-for="scene in scenarioCases" :key="scene.title" class="scenario-card">
          <div class="scenario-title">{{ scene.title }}</div>
          <div class="scenario-desc">{{ scene.description }}</div>
          <div class="scenario-target">{{ scene.targetTitle }}</div>
          <div class="scenario-actions">
            <el-button size="small" type="primary" @click="focusScenario(scene)">打开提醒</el-button>
            <el-button size="small" @click="applyScenario(scene)">{{ scene.actionLabel }}</el-button>
          </div>
        </div>
      </div>

      <div class="ding-layout" :class="{ 'detail-active': detailActive }">
        <div class="ding-list-wrap">
          <div class="ding-toolbar">
            <el-tabs v-model="activeTab" class="ding-tabs">
              <el-tab-pane label="待我处理" name="todo" />
              <el-tab-pane label="我发出的" name="sent" />
              <el-tab-pane label="已完成" name="done" />
            </el-tabs>
            <el-input
              v-model="keyword"
              class="ding-search"
              clearable
              placeholder="搜索标题、发起人或接收人"
              :prefix-icon="Search"
            />
          </div>

          <div class="ding-list">
            <div
              v-for="item in visibleList"
              :key="item.id"
              class="ding-item"
              :class="[`priority-${item.priority}`, `status-${item.status}`, { active: currentDing?.id === item.id }]"
              @click="selectDing(item)"
            >
              <div class="ding-item-head">
                <div class="ding-item-title">{{ item.title }}</div>
                <el-tag size="small" :type="statusTag(item.status)">{{ statusLabel(item.status) }}</el-tag>
              </div>
              <div class="ding-item-meta">
                <span>{{ item.direction === 'received' ? `发起人：${item.owner}` : `接收人：${item.target}` }}</span>
                <span>{{ item.deadline }}</span>
              </div>
              <div class="ding-item-desc">{{ item.content }}</div>
              <div class="ding-progress">
                <span class="progress-dot done"></span>
                <span class="progress-bar" :class="{ done: item.status !== 'pending' }"></span>
                <span class="progress-dot" :class="{ done: item.status !== 'pending' }"></span>
                <span class="progress-bar" :class="{ done: item.status === 'done' }"></span>
                <span class="progress-dot" :class="{ done: item.status === 'done' }"></span>
                <span class="progress-text">{{ progressText(item) }}</span>
              </div>
              <div class="ding-item-footer">
                <div class="ding-chip-row">
                  <el-tag size="small" effect="plain" :type="typeTag(item.type)">{{ typeLabel(item.type) }}</el-tag>
                  <el-tag size="small" effect="plain" :type="priorityTag(item.priority)">{{ priorityLabel(item.priority) }}</el-tag>
                </div>
                <span class="remind-tip">催办 {{ item.remindCount }} 次</span>
              </div>
            </div>
            <el-empty v-if="!visibleList.length" description="当前分类没有 DING" :image-size="88" />
          </div>
        </div>

        <div class="ding-detail-wrap" :class="currentDing ? `status-${currentDing.status}` : ''">
          <template v-if="currentDing">
            <div class="detail-head">
              <button type="button" class="detail-back-btn" @click="closeDetail"><el-icon><ArrowLeft /></el-icon> 返回</button>
              <div>
                <div class="detail-title">{{ currentDing.title }}</div>
                <div class="detail-meta">{{ currentDing.sentAt }} · {{ currentDing.direction === 'received' ? `${currentDing.owner} 发起` : `发给 ${currentDing.target}` }}</div>
              </div>
              <div class="detail-tags">
                <el-tag size="small" :type="typeTag(currentDing.type)" effect="plain">{{ typeLabel(currentDing.type) }}</el-tag>
                <el-tag size="small" :type="statusTag(currentDing.status)">{{ statusLabel(currentDing.status) }}</el-tag>
                <el-tag size="small" :type="priorityTag(currentDing.priority)" effect="plain">{{ priorityLabel(currentDing.priority) }}</el-tag>
              </div>
            </div>

            <div class="detail-banner" :class="`status-${currentDing.status}`">
              <div class="banner-title">{{ detailBannerTitle(currentDing) }}</div>
              <div class="banner-desc">{{ detailBannerDesc(currentDing) }}</div>
            </div>

            <div class="step-panel">
              <div class="step-row">
                <div class="step-item" :class="{ done: true }">已发送</div>
                <div class="step-line" :class="{ done: currentDing.status !== 'pending' }"></div>
                <div class="step-item" :class="{ done: currentDing.status !== 'pending' }">已确认</div>
                <div class="step-line" :class="{ done: currentDing.status === 'done' }"></div>
                <div class="step-item" :class="{ done: currentDing.status === 'done' }">已完成</div>
              </div>
            </div>

            <div class="summary-grid">
              <div class="summary-item">
                <span class="summary-label">截止时间</span>
                <span class="summary-value">{{ currentDing.deadline }}</span>
              </div>
              <div class="summary-item">
                <span class="summary-label">催办次数</span>
                <span class="summary-value">{{ currentDing.remindCount }} 次</span>
              </div>
              <div class="summary-item">
                <span class="summary-label">最后更新</span>
                <span class="summary-value">{{ currentDing.updatedAt }}</span>
              </div>
              <div class="summary-item">
                <span class="summary-label">处理场景</span>
                <span class="summary-value">{{ currentDing.scene }}</span>
              </div>
            </div>

            <div class="detail-content">{{ currentDing.content }}</div>

            <div class="detail-actions">
              <el-button
                v-if="currentDing.direction === 'received' && currentDing.status === 'pending'"
                type="primary"
                @click="confirmReceive(currentDing)"
              >确认收到</el-button>
              <el-button
                v-if="currentDing.status !== 'done'"
                type="success"
                @click="markDone(currentDing)"
              >标记完成</el-button>
              <el-button
                v-if="currentDing.direction === 'sent' && currentDing.status !== 'done'"
                @click="sendReminder(currentDing)"
              >再催一下</el-button>
            </div>

            <div class="detail-section">
              <div class="section-title">快捷处理</div>
              <div class="usecase-list">
                <div v-for="item in currentDing.useCases" :key="item.title" class="usecase-card">
                  <div class="usecase-title">{{ item.title }}</div>
                  <div class="usecase-desc">{{ item.description }}</div>
                  <div class="usecase-footer">
                    <el-tag size="small" effect="plain">{{ item.tag }}</el-tag>
                    <el-button size="small" text type="primary" @click="runUseCase(item, currentDing)">{{ item.actionLabel }}</el-button>
                  </div>
                </div>
              </div>
            </div>

            <div class="detail-section">
              <div class="section-title">处理备注</div>
              <el-input
                v-model="detailNote"
                type="textarea"
                :rows="4"
                placeholder="可以在这里记录这条 DING 的处理说明。"
              />
              <div class="note-actions">
                <el-button @click="saveNote">保存备注</el-button>
              </div>
            </div>

            <div class="detail-section">
              <div class="section-title">处理记录</div>
              <div class="history-list">
                <div v-for="record in currentDing.history" :key="`${record.time}-${record.text}`" class="history-item">
                  <span class="history-time">{{ record.time }}</span>
                  <span class="history-text">{{ record.text }}</span>
                </div>
              </div>
            </div>
          </template>
          <div v-else class="detail-empty">
            <el-empty description="暂无 DING" :image-size="88" />
          </div>
        </div>
      </div>
    </div>
  </div>
  </div>

  <el-dialog v-model="composeVisible" title="发 DING" width="720px" destroy-on-close>
    <div class="compose-grid">
      <div class="compose-row">
        <div class="compose-field wide">
          <div class="compose-label">接收人</div>
          <el-select v-model="composeForm.targetUserId" filterable placeholder="选择接收人">
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
          <div class="compose-label">标题</div>
          <el-input v-model="composeForm.title" placeholder="输入 DING 标题" maxlength="80" show-word-limit />
        </div>
      </div>
      <div class="compose-row triple">
        <div class="compose-field">
          <div class="compose-label">类型</div>
          <el-select v-model="composeForm.type">
            <el-option label="任务" value="task" />
            <el-option label="复核" value="review" />
            <el-option label="反馈" value="feedback" />
            <el-option label="审批" value="approval" />
          </el-select>
        </div>
        <div class="compose-field">
          <div class="compose-label">优先级</div>
          <el-select v-model="composeForm.priority">
            <el-option label="普通" value="medium" />
            <el-option label="紧急" value="high" />
            <el-option label="低优" value="low" />
          </el-select>
        </div>
        <div class="compose-field">
          <div class="compose-label">场景</div>
          <el-input v-model="composeForm.scene" placeholder="如：材料确认 / 研发跟进" maxlength="20" />
        </div>
      </div>
      <div class="compose-row">
        <div class="compose-field wide">
          <div class="compose-label">截止时间</div>
          <el-date-picker
            v-model="composeForm.deadlineTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="选择截止时间"
          />
        </div>
      </div>
      <div class="compose-row">
        <div class="compose-field wide">
          <div class="compose-label">说明</div>
          <el-input
            v-model="composeForm.content"
            type="textarea"
            :rows="8"
            maxlength="2000"
            show-word-limit
            placeholder="输入 DING 说明，发送后会进入对方“待我处理”。"
          />
        </div>
      </div>
    </div>
    <template #footer>
      <div class="compose-footer">
        <span class="compose-tip">发送后会同步写入对方待办，并在当前账号的“我发出的”里留痕。</span>
        <div class="compose-actions">
          <el-button @click="composeVisible = false">取消</el-button>
          <el-button type="primary" :loading="sendingCompose" @click="submitCompose">发送</el-button>
        </div>
      </div>
    </template>
  </el-dialog>
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
  apiDingConfirm,
  apiDingDone,
  apiDingList,
  apiDingRemind,
  apiDingReset,
  apiDingSend,
  apiDingSaveNote
} from '../api'
import { apiUserList } from '../api'

const router = useRouter()
const collabStore = useCollabStore()
const preferenceStore = useUserPreferenceStore()
const activeTab = ref('todo')
const keyword = ref('')
const dingList = ref([])
const currentId = ref(null)
const detailActive = ref(false)
const dingStateReady = ref(false)
const detailNote = ref('')
const contacts = ref([])
const composeVisible = ref(false)
const sendingCompose = ref(false)
const composeForm = ref(createComposeForm())

const visibleList = computed(() => dingList.value.filter(item => {
  const matchesTab = (
    (activeTab.value === 'todo' && item.direction === 'received' && item.status !== 'done') ||
    (activeTab.value === 'sent' && item.direction === 'sent') ||
    (activeTab.value === 'done' && item.status === 'done')
  )
  if (!matchesTab) return false
  if (!keyword.value.trim()) return true
  const text = `${item.title} ${item.owner} ${item.target} ${item.content}`.toLowerCase()
  return text.includes(keyword.value.trim().toLowerCase())
}))
const currentDing = computed(() => visibleList.value.find(item => item.id === currentId.value) || visibleList.value[0] || null)
const pendingCount = computed(() => dingList.value.filter(item => item.status === 'pending').length)
const processingCount = computed(() => dingList.value.filter(item => item.status === 'confirmed').length)
const doneCount = computed(() => dingList.value.filter(item => item.status === 'done').length)
const urgentCount = computed(() => dingList.value.filter(item => item.priority === 'high').length)
const latestDingNotice = computed(() => collabStore.workNotices.find(item => item.category === 'ding') || null)
const dingSyncText = computed(() => {
  if (collabStore.dingUrgent) return `${collabStore.dingPending} 待处理 · ${collabStore.dingUrgent} 紧急`
  return collabStore.dingPending ? `${collabStore.dingPending} 条待处理` : '已实时同步'
})
const scenarioCases = computed(() => [
  {
    title: '会前催材料确认',
    description: '适合“待我处理”里的复盘材料确认场景，先确认收到，再补一句处理说明。',
    targetTitle: '季度复盘材料今天 18:00 前确认',
    action: 'confirm',
    actionLabel: '直接确认'
  },
  {
    title: '发出后再催一次',
    description: '适合你发出 DING 后，对方迟迟没给反馈的情况。',
    targetTitle: '候选人终面反馈回传',
    action: 'remind',
    actionLabel: '再催一下'
  },
  {
    title: '已处理后做闭环',
    description: '适合事项已经结束，直接演示“标记完成”的闭环动作。',
    targetTitle: '云服务续费审批补充说明',
    action: 'complete',
    actionLabel: '标记完成'
  }
])

watch(() => preferenceStore.loaded, (loaded) => {
  if (!loaded || dingStateReady.value) return
  const state = preferenceStore.getPageState('ding')
  if (typeof state.activeTab === 'string') activeTab.value = state.activeTab
  if (typeof state.keyword === 'string') keyword.value = state.keyword
  if (Number.isFinite(Number(state.currentId)) && Number(state.currentId) > 0) {
    currentId.value = Number(state.currentId)
  }
  dingStateReady.value = true
}, { immediate: true })

watch(visibleList, (list) => {
  if (!list.length) {
    currentId.value = null
    return
  }
  if (!list.some(item => item.id === currentId.value)) currentId.value = list[0].id
}, { immediate: true })

watch(currentDing, (item) => {
  detailNote.value = item?.note || ''
}, { immediate: true })

watch(() => latestDingNotice.value?.id, async (noticeId, previousId) => {
  if (!noticeId || noticeId === previousId) return
  await loadList()
})

watch([activeTab, keyword, currentId], () => {
  if (!dingStateReady.value || !preferenceStore.loaded) return
  preferenceStore.setPageState('ding', {
    activeTab: activeTab.value,
    keyword: keyword.value,
    currentId: currentId.value
  })
})

onMounted(() => {
  loadList(false)
  loadContacts()
})

async function loadList(preserveSelection = true) {
  const selectedId = preserveSelection ? currentId.value : null
  const data = await apiDingList()
  dingList.value = Array.isArray(data) ? data : []
  if (selectedId && dingList.value.some(item => item.id === selectedId)) {
    currentId.value = selectedId
    return
  }
  currentId.value = dingList.value[0]?.id || null
}

async function loadContacts() {
  const data = await apiUserList()
  contacts.value = Array.isArray(data) ? data : []
}

function selectDing(item) {
  currentId.value = item.id
  if (window.innerWidth <= 768) {
    detailActive.value = true
  }
}

function closeDetail() {
  detailActive.value = false
}

async function focusLatestDing() {
  const notice = latestDingNotice.value
  if (!notice) return
  activeTab.value = 'todo'
  if (!dingList.value.some(item => item.id === notice.refId)) {
    await loadList()
  }
  const target = dingList.value.find(item => item.id === notice.refId)
  if (!target) return
  activeTab.value = target.status === 'done' ? 'done' : (target.direction === 'sent' ? 'sent' : 'todo')
  currentId.value = target.id
}

function focusScenario(scene) {
  const target = dingList.value.find(item => item.title === scene.targetTitle)
  if (!target) return
  activeTab.value = scene.action === 'complete' && target.status === 'done' ? 'done' : (target.direction === 'sent' ? 'sent' : 'todo')
  currentId.value = target.id
}

async function applyScenario(scene) {
  const target = dingList.value.find(item => item.title === scene.targetTitle)
  if (!target) return
  currentId.value = target.id
  await runAction(scene.action, target)
}

async function confirmReceive(item) {
  if (item.status !== 'pending') return
  await apiDingConfirm(item.id)
  await loadList()
  ElMessage.success('已确认收到')
}

async function markDone(item) {
  if (item.status === 'done') return
  await apiDingDone(item.id)
  await loadList()
  ElMessage.success('已标记完成')
}

async function sendReminder(item) {
  await apiDingRemind(item.id)
  await loadList()
  ElMessage.success('已再次催办')
}

async function runUseCase(useCase, item) {
  await runAction(useCase.action, item, useCase.template)
}

async function runAction(action, item, template) {
  if (!item) return
  if (action === 'confirm') await confirmReceive(item)
  else if (action === 'complete') await markDone(item)
  else if (action === 'remind') await sendReminder(item)
  else if (action === 'note') {
    currentId.value = item.id
    detailNote.value = template || item.note || ''
  } else if (action === 'focus') {
    currentId.value = item.id
  }
}

async function saveNote() {
  if (!currentDing.value) return
  await apiDingSaveNote(currentDing.value.id, detailNote.value)
  await loadList()
  ElMessage.success('备注已保存')
}

async function resetSamples() {
  await apiDingReset()
  await loadList(false)
  ElMessage.success('样例已恢复')
}

function createComposeForm() {
  const defaultDeadline = new Date(Date.now() + 4 * 60 * 60 * 1000)
  const local = new Date(defaultDeadline.getTime() - defaultDeadline.getTimezoneOffset() * 60000)
  return {
    targetUserId: null,
    title: '',
    type: 'task',
    priority: 'medium',
    scene: '协同跟进',
    deadlineTime: local.toISOString().slice(0, 19),
    content: ''
  }
}

function openCompose() {
  composeForm.value = createComposeForm()
  composeVisible.value = true
}

async function submitCompose() {
  if (!composeForm.value.targetUserId) {
    ElMessage.warning('请选择接收人')
    return
  }
  if (!composeForm.value.title.trim()) {
    ElMessage.warning('请输入 DING 标题')
    return
  }
  if (!composeForm.value.content.trim()) {
    ElMessage.warning('请输入 DING 说明')
    return
  }
  sendingCompose.value = true
  try {
    await apiDingSend({
      ...composeForm.value,
      title: composeForm.value.title.trim(),
      content: composeForm.value.content.trim(),
      scene: composeForm.value.scene.trim()
    })
    composeVisible.value = false
    activeTab.value = 'sent'
    await loadList(false)
    ElMessage.success('DING 已发出')
  } finally {
    sendingCompose.value = false
  }
}

function typeLabel(type) {
  return {
    review: '复核',
    task: '任务',
    feedback: '反馈',
    approval: '审批'
  }[type] || '提醒'
}

function typeTag(type) {
  return {
    review: 'primary',
    task: 'success',
    feedback: 'warning',
    approval: 'danger'
  }[type] || 'info'
}

function statusLabel(status) {
  return {
    pending: '待确认',
    confirmed: '处理中',
    done: '已完成'
  }[status] || '待确认'
}

function statusTag(status) {
  return {
    pending: 'warning',
    confirmed: 'primary',
    done: 'success'
  }[status] || 'info'
}

function progressText(item) {
  if (item.status === 'done') return '已完成闭环'
  if (item.status === 'confirmed') return item.direction === 'received' ? '你已确认，正在处理' : '对方已确认，等待结果'
  return item.direction === 'received' ? '待你确认收到' : '待对方确认回执'
}

function detailBannerTitle(item) {
  if (item.status === 'done') return '这条 DING 已经闭环完成'
  if (item.status === 'confirmed') return '当前已进入处理中状态'
  return item.direction === 'received' ? '这条 DING 正等待你确认收到' : '这条 DING 还在等待对方回执'
}

function detailBannerDesc(item) {
  if (item.status === 'done') return '适合直接演示“从发起到结束”的完整链路，也可以继续补充备注留痕。'
  if (item.status === 'confirmed') return '可以继续追加备注、催办或在处理结束后手动标记完成。'
  return item.direction === 'received'
    ? '建议先点“确认收到”，再在备注里写清预计完成时间。'
    : '如果事项比较紧急，可以直接点“再催一下”增强提醒反馈。'
}

function priorityLabel(priority) {
  return { high: '紧急', medium: '普通', low: '低优' }[priority] || '普通'
}

function priorityTag(priority) {
  return { high: 'danger', medium: 'warning', low: 'info' }[priority] || 'info'
}

function contactLabel(user) {
  const role = user.jobTitle ? ` · ${user.jobTitle}` : ''
  return `${user.nickname}${role}`
}
</script>

<style scoped>
.ding-shell {
  height: 100%;
  display: flex;
  background: var(--dt-bg);
}
.ding-page {
  flex: 1;
  min-width: 0;
  height: 100%;
  background: #f3f5f8;
  display: flex;
  flex-direction: column;
}
.ding-top {
  padding: 16px 24px 14px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96) 0%, rgba(247, 249, 252, 0.92) 100%);
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(18px);
  flex-shrink: 0;
}
.ding-top-inner {
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
  color: #ff9f1a;
  border-color: rgba(255, 159, 26, 0.24);
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
  background: #fff1df;
  color: #b05600;
  font-size: 12px;
  font-weight: 600;
}
.ding-body {
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
  border: 1px solid #ffe0b3;
  border-radius: 12px;
  background: linear-gradient(90deg, #fff9f0 0%, #ffffff 100%);
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  cursor: pointer;
  transition: border-color .18s, box-shadow .18s, transform .18s;
}
.live-strip:hover {
  border-color: #ffc67a;
  box-shadow: 0 10px 24px rgba(255, 159, 26, 0.12);
  transform: translateY(-1px);
}
.live-strip.ding {
  border-left: 4px solid #ff9f1a;
}
.live-strip-badge {
  flex-shrink: 0;
  min-width: 68px;
  height: 28px;
  border-radius: 999px;
  background: #fff0d9;
  color: #b05600;
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
  color: #b05600;
  font-size: 12px;
  font-weight: 600;
}
@media (max-width: 960px) {
  .ding-top {
    padding: 14px 16px 12px;
  }
  .ding-top-inner {
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
  .ding-body {
    padding: 16px;
  }
}
.stat-card,
.scenario-card,
.ding-list-wrap,
.ding-detail-wrap {
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
.stat-card.orange {
  border-top: 3px solid #fa8c16;
  background: linear-gradient(180deg, rgba(250, 140, 22, 0.08) 0%, #ffffff 34%);
}
.stat-card.blue {
  border-top: 3px solid #1677ff;
  background: linear-gradient(180deg, rgba(22, 119, 255, 0.08) 0%, #ffffff 34%);
}
.stat-card.green {
  border-top: 3px solid #52c41a;
  background: linear-gradient(180deg, rgba(82, 196, 26, 0.08) 0%, #ffffff 34%);
}
.stat-card.red {
  border-top: 3px solid #ff4d4f;
  background: linear-gradient(180deg, rgba(255, 77, 79, 0.08) 0%, #ffffff 34%);
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
.scenario-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}
.scenario-card {
  padding: 16px;
  transition: transform .18s, box-shadow .18s, border-color .18s;
}
.scenario-card:hover {
  transform: translateY(-1px);
  border-color: rgba(22, 119, 255, 0.14);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
}
.scenario-title,
.ding-item-title,
.detail-title,
.section-title,
.usecase-title {
  font-weight: 600;
  color: #1d2129;
}
.scenario-title { font-size: 15px; }
.scenario-desc,
.ding-item-desc,
.detail-content,
.usecase-desc,
.history-text {
  line-height: 1.6;
  color: #6b7280;
}
.scenario-desc {
  margin-top: 6px;
  font-size: 13px;
}
.scenario-target {
  margin-top: 12px;
  font-size: 13px;
  color: #1677ff;
}
.scenario-actions,
.detail-actions,
.note-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.scenario-actions {
  margin-top: 14px;
}
.ding-layout {
  display: grid;
  grid-template-columns: 400px minmax(0, 1fr);
  gap: 16px;
  min-height: 760px;
}
.ding-list-wrap {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}
.ding-toolbar {
  padding: 0 18px 14px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.92) 0%, rgba(255, 255, 255, 0.98) 100%);
}
.ding-tabs :deep(.el-tabs__header) {
  margin-bottom: 8px;
}
.ding-search {
  max-width: 320px;
}
.ding-list {
  flex: 1;
  overflow-y: auto;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.ding-item {
  position: relative;
  padding: 15px 16px;
  border-radius: 12px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  cursor: pointer;
  transition: transform .18s, box-shadow .18s, border-color .18s, background .18s;
  border-left-width: 4px;
  background: linear-gradient(180deg, #ffffff 0%, #fbfcff 100%);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.03);
}
.ding-item.priority-high { border-left-color: #ff4d4f; }
.ding-item.priority-medium { border-left-color: #faad14; }
.ding-item.priority-low { border-left-color: #8c8c8c; }
.ding-item.status-pending { background: linear-gradient(180deg, rgba(255, 250, 242, 0.98) 0%, #ffffff 100%); }
.ding-item.status-confirmed { background: linear-gradient(180deg, rgba(246, 250, 255, 0.98) 0%, #ffffff 100%); }
.ding-item.status-done {
  background: linear-gradient(180deg, rgba(245, 252, 247, 0.98) 0%, #ffffff 100%);
  border-color: rgba(82, 196, 26, 0.18);
}
.ding-item.status-done .ding-item-title {
  color: #237804;
}
.ding-item:hover {
  transform: translateY(-1px);
  background: #ffffff;
  border-color: rgba(22, 119, 255, 0.16);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.05);
}
.ding-item.active {
  background: linear-gradient(180deg, rgba(246, 249, 255, 0.98) 0%, #ffffff 100%);
  border-color: rgba(22, 119, 255, 0.24);
  box-shadow: 0 14px 28px rgba(22, 119, 255, 0.08);
}
.ding-item.active::before {
  content: '';
  position: absolute;
  right: 14px;
  top: 16px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #1677ff;
  box-shadow: 0 0 0 3px rgba(22, 119, 255, 0.1);
}
.ding-item-head,
.ding-item-meta,
.ding-item-footer,
.ding-chip-row,
.detail-head,
.detail-tags,
.usecase-footer {
  display: flex;
  align-items: center;
}
.ding-item-head,
.ding-item-footer,
.detail-head,
.usecase-footer {
  justify-content: space-between;
}
.ding-item-meta,
.ding-chip-row,
.detail-tags {
  gap: 8px;
  flex-wrap: wrap;
}
.ding-item-title { font-size: 14px; }
.ding-item-meta,
.remind-tip,
.detail-meta,
.summary-label,
.history-time {
  font-size: 12px;
  color: #8c8c8c;
}
.ding-item-meta {
  margin-top: 8px;
}
.ding-item-desc {
  margin-top: 8px;
  font-size: 13px;
}
.ding-progress {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 10px;
}
.progress-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #d9d9d9;
  flex-shrink: 0;
}
.progress-dot.done {
  background: #1677ff;
}
.progress-bar {
  width: 22px;
  height: 2px;
  border-radius: 999px;
  background: #e5e7eb;
  flex-shrink: 0;
}
.progress-bar.done {
  background: #91caff;
}
.progress-text {
  margin-left: 4px;
  font-size: 12px;
  color: #8c8c8c;
}
.ding-item-footer {
  margin-top: 10px;
  gap: 12px;
}
.ding-detail-wrap {
  min-height: 0;
  padding: 24px 26px;
}
.ding-detail-wrap.status-done {
  background: linear-gradient(180deg, #ffffff 0%, #f7fcf8 100%);
}
.detail-head {
  gap: 16px;
  flex-wrap: wrap;
}
.detail-title { font-size: 20px; }
.detail-meta {
  margin-top: 8px;
}
.detail-banner {
  margin-top: 18px;
  padding: 14px 16px;
  border-radius: 10px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  background: linear-gradient(180deg, #fafcff 0%, #ffffff 100%);
}
.detail-banner.status-pending {
  border-color: rgba(250, 173, 20, 0.24);
  background: rgba(250, 173, 20, 0.05);
}
.detail-banner.status-confirmed {
  border-color: rgba(22, 119, 255, 0.18);
  background: rgba(22, 119, 255, 0.05);
}
.detail-banner.status-done {
  border-color: rgba(82, 196, 26, 0.2);
  background: rgba(82, 196, 26, 0.06);
}
.banner-title {
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
}
.banner-desc {
  margin-top: 6px;
  font-size: 13px;
  color: #6b7280;
  line-height: 1.6;
}
.step-panel {
  margin-top: 14px;
  padding: 14px 16px;
  border-radius: 10px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  background: linear-gradient(180deg, #ffffff 0%, #fbfcff 100%);
}
.step-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.step-item {
  min-width: 72px;
  height: 30px;
  border-radius: 999px;
  border: 1px solid #d9d9d9;
  color: #8c8c8c;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #fff;
}
.step-item.done {
  background: rgba(22, 119, 255, .08);
  border-color: rgba(22, 119, 255, .2);
  color: #1677ff;
}
.step-line {
  flex: 1;
  height: 1px;
  background: #e5e7eb;
}
.step-line.done {
  background: #9ec5ff;
}
.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}
.summary-item {
  padding: 12px 14px;
  border-radius: 10px;
  background: linear-gradient(180deg, #ffffff 0%, #fafbfc 100%);
  border: 1px solid rgba(15, 23, 42, 0.06);
}
.summary-label {
  display: block;
}
.summary-value {
  display: block;
  margin-top: 6px;
  font-size: 13px;
  color: #374151;
  font-weight: 600;
  line-height: 1.6;
}
.detail-content {
  margin-top: 18px;
  padding: 18px 20px;
  border-radius: 10px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  background: linear-gradient(180deg, #ffffff 0%, #fafbfc 100%);
  font-size: 14px;
  white-space: pre-wrap;
}
.detail-actions {
  margin-top: 18px;
}
.detail-section {
  margin-top: 22px;
}
.section-title {
  font-size: 14px;
  margin-bottom: 12px;
}
.usecase-list,
.history-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.usecase-card,
.history-item {
  padding: 14px;
  border-radius: 10px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  background: linear-gradient(180deg, #ffffff 0%, #fbfcff 100%);
}
.usecase-desc {
  margin-top: 6px;
  font-size: 13px;
}
.usecase-footer {
  margin-top: 12px;
}
.note-actions {
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
  .scenario-strip,
  .ding-layout,
  .summary-grid {
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
  .ding-top {
    padding: 10px 12px 8px;
  }
  .ding-top-inner {
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
  .ding-body {
    padding-bottom: var(--dt-bottom-tab-height);
  }
  .ding-layout {
    flex-direction: column;
  }
  .ding-list-wrap {
    width: 100%;
  }
  .ding-detail-wrap {
    display: none;
  }
  .ding-layout.detail-active {
    position: fixed;
    inset: 0;
    z-index: 100;
    overflow-y: auto;
    background: #fff;
  }
  .ding-layout.detail-active .ding-list-wrap {
    display: none;
  }
  .ding-layout.detail-active .ding-detail-wrap {
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
  .scenario-strip {
    grid-template-columns: 1fr;
  }
}
</style>

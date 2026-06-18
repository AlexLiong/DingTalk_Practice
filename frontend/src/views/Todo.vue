<template>
  <div class="todo-shell">
    <AppSideNav active-key="todo" />

    <div class="todo-page">
      <div class="todo-top">
        <div class="todo-top-inner">
          <div class="page-title-cluster">
            <el-button class="page-back-btn" :icon="Back" @click="router.push('/chat')">返回消息</el-button>
            <div class="page-title-block">
              <div class="page-section-label">协同应用</div>
              <div class="page-heading-row">
                <span class="ptitle">待办任务</span>
              </div>
            </div>
          </div>
          <div class="page-top-actions">
            <el-button type="primary" :icon="Plus" @click="openAdd">新建待办</el-button>
          </div>
        </div>
      </div>

      <div class="todo-body">
      <!-- 统计卡片 -->
      <div class="stat-row">
        <div class="stat-card orange">
          <Clock class="stat-icon" />
          <div class="stat-info">
            <div class="stat-num">{{ stats.pending ?? 0 }}</div>
            <div class="stat-label">待办</div>
          </div>
        </div>
        <div class="stat-card blue">
          <Finished class="stat-icon" />
          <div class="stat-info">
            <div class="stat-num">{{ stats.inProgress ?? 0 }}</div>
            <div class="stat-label">进行中</div>
          </div>
        </div>
        <div class="stat-card green">
          <Check class="stat-icon" />
          <div class="stat-info">
            <div class="stat-num">{{ stats.done ?? 0 }}</div>
            <div class="stat-label">已完成</div>
          </div>
        </div>
      </div>

      <!-- Tab 筛选 -->
      <el-tabs v-model="activeTab" @tab-change="load" class="todo-tabs">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane label="待办" name="0" />
        <el-tab-pane label="进行中" name="1" />
        <el-tab-pane label="已完成" name="2" />
      </el-tabs>

      <!-- 列表 -->
      <div v-loading="loading" class="todo-list">
        <el-empty v-if="!filteredList.length && !loading" description="暂无数据" />
        <div v-for="item in filteredList" :key="item.id" class="todo-card">
          <div class="todo-header">
            <span class="todo-title">{{ item.title }}</span>
            <el-tag size="small" :type="priorityType(item.priority)">
              {{ priorityLabel(item.priority) }}
            </el-tag>
            <el-tag size="small" :type="statusType(item.status)" class="status-tag">
              {{ statusLabel(item.status) }}
            </el-tag>
          </div>
          <div class="todo-meta">
            <span v-if="item.assigneeName">负责人: {{ item.assigneeName }}</span>
            <span v-if="item.dueDate">截止: {{ item.dueDate }}</span>
          </div>
          <div class="todo-actions">
            <el-button
              v-if="item.status === 0"
              size="small"
              type="primary"
              @click="changeStatus(item, 1)"
            >开始</el-button>
            <el-button
              v-if="item.status === 1"
              size="small"
              type="success"
              @click="changeStatus(item, 2)"
            >完成</el-button>
            <el-button size="small" type="danger" plain @click="remove(item)">删除</el-button>
          </div>
        </div>
      </div>
      </div>
    </div>

    <el-dialog v-model="dialog" title="新建待办" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="请输入待办标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="3" placeholder="详细描述" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="form.priority" style="width: 100%">
            <el-option label="高" :value="1" />
            <el-option label="中" :value="2" />
            <el-option label="低" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="form.assigneeId" filterable placeholder="选择负责人" style="width: 100%">
            <el-option v-for="u in users" :key="u.id" :label="u.nickname || u.username" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker v-model="form.dueDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Back, Plus, Check, Clock, Finished } from '@element-plus/icons-vue'
import AppSideNav from '../components/AppSideNav.vue'
import { apiTodoList, apiTodoStats, apiTodoAdd, apiTodoUpdateStatus, apiTodoDelete, apiUserList } from '../api'
import { useUserPreferenceStore } from '../store/userPreference'

const router = useRouter()
const preferenceStore = useUserPreferenceStore()
const loading = ref(false)
const list = ref([])
const stats = ref({})
const users = ref([])
const activeTab = ref('all')
const todoStateReady = ref(false)
const dialog = ref(false)
const form = reactive({ title: '', content: '', priority: 2, assigneeId: null, dueDate: '' })

const filteredList = computed(() => {
  if (activeTab.value === 'all') return list.value
  return list.value.filter(t => String(t.status) === activeTab.value)
})

watch(() => preferenceStore.loaded, (loaded) => {
  if (!loaded || todoStateReady.value) return
  const state = preferenceStore.getPageState('todo')
  if (typeof state.activeTab === 'string') activeTab.value = state.activeTab
  todoStateReady.value = true
}, { immediate: true })

watch(activeTab, () => {
  if (!todoStateReady.value || !preferenceStore.loaded) return
  preferenceStore.setPageState('todo', { activeTab: activeTab.value })
})

onMounted(async () => {
  users.value = await apiUserList()
  await loadStats()
  await load()
})

async function load() {
  loading.value = true
  try {
    const params = {}
    if (activeTab.value !== 'all') params.status = Number(activeTab.value)
    const data = await apiTodoList(params)
    list.value = Array.isArray(data) ? data : (data.records || data.list || [])
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try { stats.value = await apiTodoStats() } catch { stats.value = {} }
}

function openAdd() {
  Object.assign(form, { title: '', content: '', priority: 2, assigneeId: null, dueDate: '' })
  dialog.value = true
}

async function submit() {
  if (!form.title) { ElMessage.warning('请输入标题'); return }
  await apiTodoAdd({ ...form })
  ElMessage.success('创建成功')
  dialog.value = false
  await load()
  await loadStats()
}

async function changeStatus(item, status) {
  await apiTodoUpdateStatus(item.id, status)
  item.status = status
  ElMessage.success('状态已更新')
  await loadStats()
}

async function remove(item) {
  await ElMessageBox.confirm(`确定删除「${item.title}」?`, '提示', { type: 'warning' })
  await apiTodoDelete(item.id)
  ElMessage.success('已删除')
  await load()
  await loadStats()
}

function priorityLabel(p) { return { 1: '高', 2: '中', 3: '低' }[p] || '中' }
function priorityType(p) { return { 1: 'danger', 2: 'warning', 3: 'info' }[p] || 'info' }
function statusLabel(s) { return { 0: '待办', 1: '进行中', 2: '已完成' }[s] || '待办' }
function statusType(s) { return { 0: 'warning', 1: 'primary', 2: 'success' }[s] || 'info' }
</script>

<style scoped>
.todo-shell {
  height: 100%;
  display: flex;
  background: var(--dt-bg);
}
.todo-page {
  flex: 1;
  min-width: 0;
  height: 100%;
  background: #f3f5f8;
  display: flex;
  flex-direction: column;
}
.todo-top {
  padding: 16px 24px 14px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96) 0%, rgba(247, 249, 252, 0.92) 100%);
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(18px);
  flex-shrink: 0;
}
.todo-top-inner {
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
.todo-body {
  flex: 1;
  overflow-y: auto;
  padding: 18px 24px 24px;
  max-width: 860px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
}

/* 统计卡片 */
.stat-row { display: flex; gap: 16px; margin-bottom: 16px; }
.stat-card {
  flex: 1;
  background: #fff;
  border-radius: 10px;
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.stat-icon { width: 36px; height: 36px; }
.stat-card.orange .stat-icon { color: #fa8c16; }
.stat-card.blue .stat-icon { color: #1677ff; }
.stat-card.green .stat-icon { color: #52c41a; }
.stat-num { font-size: 24px; font-weight: 700; color: #1d2129; }
.stat-label { font-size: 13px; color: #8c8c8c; margin-top: 2px; }

.todo-tabs { background: #fff; border-radius: 10px; padding: 4px 16px 0; margin-bottom: 12px; }

/* 列表 */
.todo-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.todo-card {
  background: #fff;
  border-radius: 10px;
  padding: 16px 20px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.todo-header { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.todo-title { font-size: 15px; font-weight: 600; color: #1d2129; }
.status-tag { margin-left: auto; }
.todo-meta {
  display: flex;
  gap: 20px;
  margin-top: 10px;
  font-size: 13px;
  color: #8c8c8c;
}
.todo-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  justify-content: flex-end;
}
@media (max-width: 960px) {
  .todo-top {
    padding: 14px 16px 12px;
  }
  .todo-top-inner {
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
  .todo-body {
    padding: 16px;
  }
}
</style>

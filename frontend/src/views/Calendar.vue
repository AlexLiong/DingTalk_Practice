<template>
  <div class="calendar-shell">
    <AppSideNav active-key="calendar" />

    <div class="calendar-page">
      <div class="calendar-top">
        <div class="calendar-top-inner">
          <div class="page-title-cluster">
            <el-button class="page-back-btn" :icon="Back" @click="$router.push('/chat')">返回消息</el-button>
            <div class="page-title-block">
              <div class="page-section-label">协同应用</div>
              <div class="page-heading-row">
                <span class="ptitle">日程日历</span>
              </div>
            </div>
          </div>
          <div class="page-top-actions">
            <el-button type="primary" :icon="Plus" @click="openAdd">新建日程</el-button>
          </div>
        </div>
      </div>

      <div class="calendar-body">
      <!-- Left: Calendar Grid -->
      <div class="calendar-left">
        <div class="month-nav">
          <el-button text @click="changeMonth(-1)">&lt;</el-button>
          <span class="month-label">{{ year }}年{{ month + 1 }}月</span>
          <el-button text @click="changeMonth(1)">&gt;</el-button>
          <el-button size="small" style="margin-left:8px" @click="goToday">今天</el-button>
        </div>

        <div class="cal-grid">
          <div class="cal-header" v-for="d in weekDays" :key="d">{{ d }}</div>
          <div
            v-for="(cell, idx) in calendarCells"
            :key="idx"
            class="cal-cell"
            :class="{
              'other-month': !cell.current,
              'is-today': cell.isToday,
              'is-selected': cell.date === selectedDate
            }"
            @click="selectDate(cell)"
          >
            <span class="cell-num">{{ cell.day }}</span>
            <span v-if="cell.current && dateHasSchedule(cell.date)" class="dot"></span>
          </div>
        </div>
      </div>

      <!-- Right: Schedule List -->
      <div class="calendar-right">
        <div class="right-header">
          <span>{{ selectedDateLabel }}</span>
          <span class="sch-count">{{ daySchedules.length }} 个日程</span>
        </div>

        <div v-if="daySchedules.length === 0" class="empty-state">
          <el-empty description="该日暂无日程" :image-size="80" />
        </div>

        <div class="sch-list" v-else>
          <div
            v-for="item in daySchedules"
            :key="item.id"
            class="sch-card"
            :style="{ borderLeftColor: item.color || '#1677ff' }"
          >
            <div class="sch-title">{{ item.title }}</div>
            <div v-if="item.content" class="sch-content">{{ item.content }}</div>
            <div class="sch-time">
              <template v-if="item.allDay">全天</template>
              <template v-else>{{ formatTime(item.startTime) }} - {{ formatTime(item.endTime) }}</template>
            </div>
            <el-button
              class="sch-del"
              text
              type="danger"
              size="small"
              @click="handleDelete(item)"
            >删除</el-button>
          </div>
        </div>
      </div>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="新建日程" width="460px" :close-on-click-modal="false">
      <el-form :model="addForm" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="addForm.title" placeholder="请输入日程标题" maxlength="50" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="addForm.content" type="textarea" :rows="3" placeholder="日程详情（可选）" />
        </el-form-item>
        <el-form-item label="全天">
          <el-checkbox v-model="addForm.allDay">全天日程</el-checkbox>
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="addForm.startTime"
            :type="addForm.allDay ? 'date' : 'datetime'"
            placeholder="选择开始时间"
            style="width:100%"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item v-if="!addForm.allDay" label="结束时间">
          <el-date-picker
            v-model="addForm.endTime"
            type="datetime"
            placeholder="选择结束时间"
            style="width:100%"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="颜色">
          <div class="color-picker">
            <span
              v-for="c in presetColors"
              :key="c"
              class="color-dot"
              :style="{ background: c }"
              :class="{ active: addForm.color === c }"
              @click="addForm.color = c"
            ></span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAdd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Back, Plus } from '@element-plus/icons-vue'
import AppSideNav from '../components/AppSideNav.vue'
import { apiScheduleList, apiScheduleAdd, apiScheduleDelete } from '../api'

const router = useRouter()
const weekDays = ['日', '一', '二', '三', '四', '五', '六']
const presetColors = ['#1677ff', '#52c41a', '#fa8c16', '#eb2f96', '#722ed1']

const now = new Date()
const year = ref(now.getFullYear())
const month = ref(now.getMonth())
const selectedDate = ref(formatDate(now))
const schedules = ref([])
const dialogVisible = ref(false)
const addForm = reactive({
  title: '',
  content: '',
  startTime: '',
  endTime: '',
  allDay: false,
  color: '#1677ff'
})

/* ---------- calendar grid computation ---------- */
const calendarCells = computed(() => {
  const cells = []
  const firstDay = new Date(year.value, month.value, 1)
  const startWeekday = firstDay.getDay()
  const daysInMonth = new Date(year.value, month.value + 1, 0).getDate()
  const prevMonthDays = new Date(year.value, month.value, 0).getDate()
  const today = formatDate(new Date())

  // Previous month padding
  for (let i = startWeekday - 1; i >= 0; i--) {
    const d = prevMonthDays - i
    const m = month.value === 0 ? 11 : month.value - 1
    const y = month.value === 0 ? year.value - 1 : year.value
    cells.push({ day: d, current: false, date: buildDate(y, m, d), isToday: false })
  }

  // Current month
  for (let d = 1; d <= daysInMonth; d++) {
    const dateStr = buildDate(year.value, month.value, d)
    cells.push({ day: d, current: true, date: dateStr, isToday: dateStr === today })
  }

  // Next month padding
  const remaining = 42 - cells.length
  for (let d = 1; d <= remaining; d++) {
    const m = month.value === 11 ? 0 : month.value + 1
    const y = month.value === 11 ? year.value + 1 : year.value
    cells.push({ day: d, current: false, date: buildDate(y, m, d), isToday: false })
  }

  return cells
})

const selectedDateLabel = computed(() => {
  if (!selectedDate.value) return ''
  const [y, m, d] = selectedDate.value.split('-')
  return `${parseInt(m)}月${parseInt(d)}日`
})

const daySchedules = computed(() => {
  if (!selectedDate.value) return []
  return schedules.value.filter(s => {
    const sDate = (s.startTime || '').substring(0, 10)
    return sDate === selectedDate.value
  })
})

/* ---------- schedule date index for dots ---------- */
const scheduleDateSet = computed(() => {
  const set = new Set()
  schedules.value.forEach(s => {
    const d = (s.startTime || '').substring(0, 10)
    if (d) set.add(d)
  })
  return set
})

function dateHasSchedule(dateStr) {
  return scheduleDateSet.value.has(dateStr)
}

/* ---------- helpers ---------- */
function formatDate(d) {
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

function buildDate(y, m, d) {
  return `${y}-${String(m + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`
}

function formatTime(t) {
  if (!t) return ''
  return t.length >= 16 ? t.substring(11, 16) : t
}

function changeMonth(delta) {
  let m = month.value + delta
  let y = year.value
  if (m < 0) { m = 11; y-- }
  if (m > 11) { m = 0; y++ }
  month.value = m
  year.value = y
  loadSchedules()
}

function goToday() {
  const t = new Date()
  year.value = t.getFullYear()
  month.value = t.getMonth()
  selectedDate.value = formatDate(t)
  loadSchedules()
}

function selectDate(cell) {
  selectedDate.value = cell.date
  if (!cell.current) {
    const [y, m] = cell.date.split('-').map(Number)
    year.value = y
    month.value = m - 1
    loadSchedules()
  }
}

/* ---------- API ---------- */
async function loadSchedules() {
  try {
    const startOfMonth = buildDate(year.value, month.value, 1)
    const daysInMonth = new Date(year.value, month.value + 1, 0).getDate()
    const endOfMonth = buildDate(year.value, month.value, daysInMonth)
    const res = await apiScheduleList({ startDate: startOfMonth, endDate: endOfMonth })
    schedules.value = Array.isArray(res) ? res : (res.list || res.data || [])
  } catch {
    schedules.value = []
  }
}

function openAdd() {
  addForm.title = ''
  addForm.content = ''
  addForm.allDay = false
  addForm.color = '#1677ff'
  // default start time to selected date 09:00
  addForm.startTime = selectedDate.value + ' 09:00:00'
  addForm.endTime = selectedDate.value + ' 10:00:00'
  dialogVisible.value = true
}

async function submitAdd() {
  if (!addForm.title.trim()) { ElMessage.warning('请输入日程标题'); return }
  if (!addForm.startTime) { ElMessage.warning('请选择开始时间'); return }
  if (!addForm.allDay && !addForm.endTime) { ElMessage.warning('请选择结束时间'); return }

  try {
    await apiScheduleAdd({
      title: addForm.title,
      content: addForm.content,
      startTime: addForm.startTime,
      endTime: addForm.allDay ? addForm.startTime : addForm.endTime,
      allDay: addForm.allDay,
      color: addForm.color
    })
    ElMessage.success('日程已创建')
    dialogVisible.value = false
    loadSchedules()
  } catch {
    ElMessage.error('创建失败')
  }
}

async function handleDelete(item) {
  try {
    await ElMessageBox.confirm(`确定删除日程「${item.title}」？`, '提示', { type: 'warning' })
    await apiScheduleDelete(item.id)
    ElMessage.success('已删除')
    loadSchedules()
  } catch { /* cancelled */ }
}

onMounted(() => {
  loadSchedules()
})
</script>

<style scoped>
.calendar-shell {
  height: 100%;
  display: flex;
  background: var(--dt-bg);
}
.calendar-page {
  flex: 1;
  min-width: 0;
  height: 100%;
  background: #f3f5f8;
  display: flex;
  flex-direction: column;
}

.calendar-top {
  padding: 16px 24px 14px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96) 0%, rgba(247, 249, 252, 0.92) 100%);
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(18px);
}
.calendar-top-inner {
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

/* ---------- body layout ---------- */
.calendar-body {
  flex: 1;
  display: flex;
  gap: 18px;
  padding: 18px 24px 24px;
  max-width: 1260px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
  overflow: hidden;
}

/* ---------- left: calendar grid ---------- */
.calendar-left {
  width: 420px;
  min-width: 360px;
  background: #fff;
  border-radius: 8px;
  padding: 18px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
}

.month-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 14px;
}
.month-label { font-size: 16px; font-weight: 600; min-width: 110px; text-align: center; }

.cal-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 0;
}
.cal-header {
  text-align: center;
  font-size: 13px;
  color: #888;
  padding: 6px 0 10px;
  font-weight: 500;
}
.cal-cell {
  position: relative;
  text-align: center;
  padding: 10px 0;
  cursor: pointer;
  border: 1px solid transparent;
  border-radius: 6px;
  transition: background 0.15s;
}
.cal-cell:hover { background: #f5f7fa; }
.cal-cell.other-month .cell-num { color: #c0c4cc; }
.cal-cell.is-today .cell-num {
  background: #1677ff;
  color: #fff;
  border-radius: 50%;
  display: inline-block;
  width: 28px;
  height: 28px;
  line-height: 28px;
}
.cal-cell.is-selected {
  border-color: #1677ff;
  background: #e6f4ff;
}
.cell-num {
  font-size: 14px;
  display: inline-block;
  width: 28px;
  height: 28px;
  line-height: 28px;
}
.dot {
  position: absolute;
  bottom: 3px;
  left: 50%;
  transform: translateX(-50%);
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #1677ff;
}

/* ---------- right: schedule list ---------- */
.calendar-right {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  padding: 18px 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.right-header {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 600;
}
.sch-count { font-size: 13px; color: #999; font-weight: 400; }

.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.sch-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.sch-card {
  position: relative;
  background: #fafafa;
  border-left: 4px solid #1677ff;
  border-radius: 6px;
  padding: 14px 16px;
  transition: box-shadow 0.15s;
}
.sch-card:hover { box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08); }
.sch-title { font-size: 15px; font-weight: 600; margin-bottom: 4px; }
.sch-content { font-size: 13px; color: #666; margin-bottom: 4px; }
.sch-time { font-size: 12px; color: #999; }
.sch-del { position: absolute; top: 12px; right: 10px; }

/* ---------- color picker ---------- */
.color-picker { display: flex; gap: 10px; }
.color-dot {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  cursor: pointer;
  border: 2px solid transparent;
  transition: border-color 0.15s, transform 0.15s;
}
.color-dot:hover { transform: scale(1.15); }
.color-dot.active { border-color: #303133; transform: scale(1.15); }
@media (max-width: 960px) {
  .calendar-top {
    padding: 14px 16px 12px;
  }
  .calendar-top-inner {
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
  .calendar-body {
    padding: 16px;
  }
}
</style>

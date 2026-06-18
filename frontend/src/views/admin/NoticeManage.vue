<template>
  <div class="page-card">
    <!-- 工具栏 -->
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="搜索标题" :prefix-icon="Search"
                clearable style="width: 220px" @keyup.enter="load" @clear="load" />
      <el-button type="primary" :icon="Search" @click="load">查询</el-button>
      <div class="spacer"></div>
      <el-button type="primary" :icon="Plus" @click="openAdd">发布通知</el-button>
    </div>

    <!-- 表格 -->
    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column type="index" label="#" width="50" />
      <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
      <el-table-column label="类型" width="80">
        <template #default="{ row }">
          <el-tag size="small" :type="row.type === 1 ? 'primary' : 'warning'">
            {{ row.type === 1 ? '通知' : '公告' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="优先级" width="80">
        <template #default="{ row }">
          <el-tag size="small" :type="row.priority === 1 ? 'danger' : 'info'">
            {{ row.priority === 1 ? '紧急' : '普通' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="publisherName" label="发布人" width="100" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '已发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="已读" width="130">
        <template #default="{ row }">
          <div class="read-cell">
            <span class="read-text">{{ row.readCount ?? 0 }}/{{ row.totalCount ?? 0 }}</span>
            <el-progress
              :percentage="row.totalCount ? Math.round((row.readCount || 0) / row.totalCount * 100) : 0"
              :stroke-width="6"
              :show-text="false"
              style="flex: 1; min-width: 50px"
            />
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pager" background layout="total, prev, pager, next"
                   :total="total" :page-size="query.pageSize" :current-page="query.pageNum"
                   @current-change="onPage" />

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialog" :title="form.id ? '编辑通知' : '发布通知'" width="600px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="通知标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="5" placeholder="通知内容" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="通知" :value="1" />
            <el-option label="公告" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="form.priority" style="width: 100%">
            <el-option label="紧急" :value="1" />
            <el-option label="普通" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0"
                     active-text="发布" inactive-text="草稿" />
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
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import { apiNoticeAdminList, apiNoticeAdd, apiNoticeUpdate, apiNoticeDelete } from '../../api'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '' })
const dialog = ref(false)
const form = reactive({ id: null, title: '', content: '', type: 1, priority: 2, status: 1 })

onMounted(() => { load() })

async function load() {
  loading.value = true
  try {
    const data = await apiNoticeAdminList(query)
    if (Array.isArray(data)) {
      list.value = data
      total.value = data.length
    } else {
      list.value = data.records || data.list || []
      total.value = data.total || 0
    }
  } finally {
    loading.value = false
  }
}

function onPage(p) { query.pageNum = p; load() }

function openAdd() {
  Object.assign(form, { id: null, title: '', content: '', type: 1, priority: 2, status: 1 })
  dialog.value = true
}

function openEdit(row) {
  Object.assign(form, { ...row })
  dialog.value = true
}

async function submit() {
  if (!form.title) { ElMessage.warning('请输入标题'); return }
  if (form.id) {
    await apiNoticeUpdate({ ...form })
  } else {
    await apiNoticeAdd({ ...form })
  }
  ElMessage.success('保存成功')
  dialog.value = false
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除通知「${row.title}」?`, '提示', { type: 'warning' })
  await apiNoticeDelete(row.id)
  ElMessage.success('已删除')
  load()
}
</script>

<style scoped>
.page-card { background: #fff; border-radius: 10px; padding: 18px; }
.toolbar { display: flex; align-items: center; gap: 10px; margin-bottom: 16px; }
.spacer { flex: 1; }
.pager { margin-top: 16px; display: flex; justify-content: flex-end; }
.read-cell { display: flex; align-items: center; gap: 8px; }
.read-text { font-size: 12px; color: #666; white-space: nowrap; }
</style>

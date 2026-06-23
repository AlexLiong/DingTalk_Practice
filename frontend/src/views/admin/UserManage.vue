<template>
  <div class="page-card">
    <!-- 搜索栏 -->
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="搜索姓名" :prefix-icon="Search"
                clearable style="width: 220px" @keyup.enter="load" @clear="load" />
      <el-select v-model="query.deptId" placeholder="全部部门" clearable
                 style="width: 160px" @change="load">
        <el-option v-for="d in flatDepts" :key="d.id" :label="d.label" :value="d.id" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="load">查询</el-button>
      <div class="spacer"></div>
      <el-button v-if="hasPerm('system:user:add')" type="primary" :icon="Plus" @click="openAdd">新增用户</el-button>
    </div>

    <!-- 表格 -->
    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column type="index" label="#" width="50" />
      <el-table-column label="头像" width="70">
        <template #default="{ row }">
          <el-avatar :size="34" shape="square" :style="avatarStyle(row.nickname)">{{ firstChar(row.nickname) }}</el-avatar>
        </template>
      </el-table-column>
      <el-table-column prop="username" label="账号" width="110" />
      <el-table-column prop="nickname" label="姓名" width="100" />
      <el-table-column prop="deptName" label="部门" width="100" />
      <el-table-column prop="jobTitle" label="职位" width="120" />
      <el-table-column prop="mobile" label="手机" width="130" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="200" fixed="right">
        <template #default="{ row }">
          <el-button v-if="hasPerm('system:user:edit')" link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="hasPerm('system:user:resetPwd')" link type="warning" @click="resetPwd(row)">重置密码</el-button>
          <el-button v-if="hasPerm('system:user:remove')" link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pager" background layout="total, prev, pager, next"
                   :total="total" :page-size="query.pageSize" :current-page="query.pageNum"
                   @current-change="onPage" />

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialog" :title="form.id ? '编辑用户' : '新增用户'" width="520px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="账号" v-if="!form.id">
          <el-input v-model="form.username" placeholder="登录账号" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.nickname" placeholder="姓名" />
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="form.deptId" placeholder="选择部门" style="width:100%">
            <el-option v-for="d in flatDepts" :key="d.id" :label="d.label" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="职位"><el-input v-model="form.jobTitle" /></el-form-item>
        <el-form-item label="手机"><el-input v-model="form.mobile" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple placeholder="分配角色" style="width:100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
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
import { useUserStore } from '../../store/user'
import {
  apiAdminUserPage, apiAdminUserAdd, apiAdminUserUpdate, apiAdminUserDelete,
  apiAdminUserResetPwd, apiAdminUserRoles, apiRoleAll, apiDeptTree
} from '../../api'

const userStore = useUserStore()
const hasPerm = (c) => userStore.hasPerm(c)

const list = ref([])
const total = ref(0)
const loading = ref(false)
const roles = ref([])
const flatDepts = ref([])
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', deptId: null })
const dialog = ref(false)
const form = reactive({ id: null, username: '', nickname: '', deptId: null, jobTitle: '', mobile: '', email: '', gender: 1, status: 1, roleIds: [] })

onMounted(async () => {
  roles.value = await apiRoleAll()
  flattenDepts(await apiDeptTree())
  load()
})

function flattenDepts(tree, prefix = '') {
  flatDepts.value = []
  const walk = (nodes, pre) => {
    nodes.forEach(n => {
      flatDepts.value.push({ id: n.id, label: pre + n.name })
      if (n.children?.length) walk(n.children, pre + '　')
    })
  }
  walk(tree, prefix)
}

async function load() {
  loading.value = true
  try {
    const data = await apiAdminUserPage(query)
    list.value = data.records
    total.value = data.total
  } finally { loading.value = false }
}

function onPage(p) { query.pageNum = p; load() }

function openAdd() {
  Object.assign(form, { id: null, username: '', nickname: '', deptId: null, jobTitle: '', mobile: '', email: '', gender: 1, status: 1, roleIds: [] })
  dialog.value = true
}

async function openEdit(row) {
  Object.assign(form, { ...row, roleIds: [] })
  form.roleIds = await apiAdminUserRoles(row.id)
  dialog.value = true
}

async function submit() {
  if (form.id) await apiAdminUserUpdate(form)
  else await apiAdminUserAdd(form)
  ElMessage.success('保存成功')
  dialog.value = false
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除用户 ${row.nickname}?`, '提示', { type: 'warning' })
  await apiAdminUserDelete(row.id)
  ElMessage.success('已删除')
  load()
}

async function resetPwd(row) {
  const { value } = await ElMessageBox.prompt('请输入新密码', '重置密码', {
    inputValue: 'Boz@2026', inputPlaceholder: '新密码'
  })
  await apiAdminUserResetPwd(row.id, value)
  ElMessage.success('密码已重置')
}

function firstChar(name) { return name ? name.charAt(0) : '?' }
function avatarStyle(name) {
  const colors = ['#1677ff', '#52c41a', '#fa8c16', '#eb2f96', '#722ed1', '#13c2c2']
  let hash = 0
  for (const c of (name || '?')) hash += c.charCodeAt(0)
  return { background: colors[hash % colors.length], color: '#fff', fontSize: '14px' }
}
</script>

<style scoped>
.page-card { background: #fff; border-radius: 10px; padding: 18px; }
.toolbar { display: flex; align-items: center; gap: 10px; margin-bottom: 16px; }
.spacer { flex: 1; }
.pager { margin-top: 16px; display: flex; justify-content: flex-end; }

@media (max-width: 768px) {
  .page-card {
    padding: 12px;
  }
  .toolbar {
    flex-wrap: wrap;
    gap: 8px;
  }
  .toolbar :deep(.el-input),
  .toolbar :deep(.el-select) {
    width: 100% !important;
  }
  .toolbar :deep(.el-button) {
    flex: 1;
    min-width: calc(50% - 4px);
  }
  .spacer {
    display: none;
  }
  .pager {
    justify-content: center;
  }
}
</style>

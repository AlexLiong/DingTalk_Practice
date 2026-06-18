<template>
  <div class="page-card">
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="搜索角色名" :prefix-icon="Search"
                clearable style="width: 220px" @keyup.enter="load" @clear="load" />
      <el-button type="primary" :icon="Search" @click="load">查询</el-button>
      <div class="spacer"></div>
      <el-button v-if="hasPerm('system:role:add')" type="primary" :icon="Plus" @click="openAdd">新增角色</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column type="index" label="#" width="50" />
      <el-table-column prop="name" label="角色名" width="140" />
      <el-table-column prop="roleKey" label="权限字符" width="140" />
      <el-table-column prop="remark" label="备注" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button v-if="hasPerm('system:role:edit')" link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="hasPerm('system:role:edit')" link type="success" @click="openPerm(row)">分配权限</el-button>
          <el-button v-if="hasPerm('system:role:remove')" link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pager" background layout="total, prev, pager, next"
                   :total="total" :page-size="query.pageSize" :current-page="query.pageNum"
                   @current-change="onPage" />

    <!-- 新增/编辑 -->
    <el-dialog v-model="dialog" :title="form.id ? '编辑角色' : '新增角色'" width="460px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="角色名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="权限字符"><el-input v-model="form.roleKey" placeholder="如 staff" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配权限 -->
    <el-dialog v-model="permDialog" title="分配权限" width="420px">
      <el-tree ref="treeRef" :data="menuTree" show-checkbox node-key="id"
               :props="{ label: 'name', children: 'children' }" default-expand-all />
      <template #footer>
        <el-button @click="permDialog = false">取消</el-button>
        <el-button type="primary" @click="savePerm">保存</el-button>
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
  apiRolePage, apiRoleAdd, apiRoleUpdate, apiRoleDelete,
  apiRoleMenus, apiMenuTree
} from '../../api'

const userStore = useUserStore()
const hasPerm = (c) => userStore.hasPerm(c)

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '' })
const dialog = ref(false)
const form = reactive({ id: null, name: '', roleKey: '', sort: 0, remark: '', status: 1 })

const permDialog = ref(false)
const menuTree = ref([])
const treeRef = ref(null)
const currentRoleId = ref(null)

onMounted(load)

async function load() {
  loading.value = true
  try {
    const data = await apiRolePage(query)
    list.value = data.records
    total.value = data.total
  } finally { loading.value = false }
}

function onPage(p) { query.pageNum = p; load() }

function openAdd() {
  Object.assign(form, { id: null, name: '', roleKey: '', sort: 0, remark: '', status: 1 })
  dialog.value = true
}
function openEdit(row) {
  Object.assign(form, { ...row })
  dialog.value = true
}
async function submit() {
  if (form.id) await apiRoleUpdate(form)
  else await apiRoleAdd(form)
  ElMessage.success('保存成功')
  dialog.value = false
  load()
}
async function remove(row) {
  await ElMessageBox.confirm(`确定删除角色 ${row.name}?`, '提示', { type: 'warning' })
  await apiRoleDelete(row.id)
  ElMessage.success('已删除')
  load()
}

async function openPerm(row) {
  currentRoleId.value = row.id
  if (!menuTree.value.length) menuTree.value = await apiMenuTree()
  permDialog.value = true
  const checked = await apiRoleMenus(row.id)
  // 仅设置叶子节点选中, 避免父子联动把半选当全选
  setTimeout(() => {
    treeRef.value.setCheckedKeys([])
    checked.forEach(id => {
      const node = treeRef.value.getNode(id)
      if (node && (!node.childNodes || node.childNodes.length === 0)) {
        treeRef.value.setChecked(id, true, false)
      }
    })
    // 对有子节点的也设置(覆盖目录级)
    checked.forEach(id => treeRef.value.setChecked(id, true, false))
  }, 50)
}

async function savePerm() {
  const checked = treeRef.value.getCheckedKeys()
  const halfChecked = treeRef.value.getHalfCheckedKeys()
  const menuIds = [...checked, ...halfChecked]
  await apiRoleUpdate({ id: currentRoleId.value, menuIds })
  ElMessage.success('权限已保存')
  permDialog.value = false
}
</script>

<style scoped>
.page-card { background: #fff; border-radius: 10px; padding: 18px; }
.toolbar { display: flex; align-items: center; gap: 10px; margin-bottom: 16px; }
.spacer { flex: 1; }
.pager { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>

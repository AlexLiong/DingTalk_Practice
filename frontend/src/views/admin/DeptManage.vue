<template>
  <div class="page-card">
    <div class="toolbar">
      <span class="title">组织架构 / 部门管理</span>
      <div class="spacer"></div>
      <el-button v-if="hasPerm('system:dept:add')" type="primary" :icon="Plus" @click="openAdd(null)">新增顶级部门</el-button>
    </div>

    <el-table :data="tree" v-loading="loading" row-key="id" border default-expand-all
              :tree-props="{ children: 'children' }">
      <el-table-column prop="name" label="部门名称" width="240" />
      <el-table-column prop="leader" label="负责人" width="140" />
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="200">
        <template #default="{ row }">
          <el-button v-if="hasPerm('system:dept:add')" link type="primary" @click="openAdd(row)">新增子部门</el-button>
          <el-button v-if="hasPerm('system:dept:edit')" link type="success" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="hasPerm('system:dept:remove')" link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="form.id ? '编辑部门' : '新增部门'" width="440px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="上级部门">
          <el-tree-select v-model="form.parentId" :data="treeSelectData" check-strictly
                          :props="{ label: 'name', children: 'children', value: 'id' }"
                          placeholder="顶级部门" style="width:100%" />
        </el-form-item>
        <el-form-item label="部门名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="负责人"><el-input v-model="form.leader" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
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
import { reactive, ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useUserStore } from '../../store/user'
import { apiDeptTree, apiDeptAdd, apiDeptUpdate, apiDeptDelete } from '../../api'

const userStore = useUserStore()
const hasPerm = (c) => userStore.hasPerm(c)

const tree = ref([])
const loading = ref(false)
const dialog = ref(false)
const form = reactive({ id: null, parentId: 0, name: '', leader: '', sort: 0, status: 1 })

const treeSelectData = computed(() => [{ id: 0, name: '顶级部门', children: tree.value }])

onMounted(load)

async function load() {
  loading.value = true
  try { tree.value = await apiDeptTree() } finally { loading.value = false }
}

function openAdd(parent) {
  Object.assign(form, { id: null, parentId: parent ? parent.id : 0, name: '', leader: '', sort: 0, status: 1 })
  dialog.value = true
}
function openEdit(row) {
  Object.assign(form, { ...row, parentId: row.parentId || 0 })
  dialog.value = true
}
async function submit() {
  if (form.id) await apiDeptUpdate(form)
  else await apiDeptAdd(form)
  ElMessage.success('保存成功')
  dialog.value = false
  load()
}
async function remove(row) {
  await ElMessageBox.confirm(`确定删除部门 ${row.name}?`, '提示', { type: 'warning' })
  await apiDeptDelete(row.id)
  ElMessage.success('已删除')
  load()
}
</script>

<style scoped>
.page-card { background: #fff; border-radius: 10px; padding: 18px; }
.toolbar { display: flex; align-items: center; margin-bottom: 16px; }
.title { font-size: 16px; font-weight: 600; }
.spacer { flex: 1; }
</style>

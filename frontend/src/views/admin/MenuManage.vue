<template>
  <div class="page-card">
    <div class="toolbar">
      <span class="title">菜单管理</span>
      <div class="spacer"></div>
      <el-button v-if="hasPerm('system:menu:add')" type="primary" :icon="Plus" @click="openAdd(null)">新增顶级菜单</el-button>
    </div>

    <el-table :data="tree" v-loading="loading" row-key="id" border default-expand-all
              :tree-props="{ children: 'children' }">
      <el-table-column prop="name" label="菜单名称" width="200" />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag size="small" :type="typeTag(row.type)">{{ typeName(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="path" label="路由路径" width="160" />
      <el-table-column prop="perms" label="权限标识" width="180" />
      <el-table-column prop="sort" label="排序" width="70" />
      <el-table-column label="操作" min-width="200">
        <template #default="{ row }">
          <el-button v-if="hasPerm('system:menu:add') && row.type !== 3" link type="primary" @click="openAdd(row)">新增子项</el-button>
          <el-button v-if="hasPerm('system:menu:edit')" link type="success" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="hasPerm('system:menu:remove')" link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="form.id ? '编辑菜单' : '新增菜单'" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="上级菜单">
          <el-tree-select v-model="form.parentId" :data="treeSelectData" check-strictly
                          :props="{ label: 'name', children: 'children', value: 'id' }"
                          placeholder="顶级菜单" clearable style="width:100%" />
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">目录</el-radio>
            <el-radio :value="2">菜单</el-radio>
            <el-radio :value="3">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="路由" v-if="form.type !== 3"><el-input v-model="form.path" placeholder="如 /admin/user" /></el-form-item>
        <el-form-item label="图标" v-if="form.type !== 3"><el-input v-model="form.icon" placeholder="如 User" /></el-form-item>
        <el-form-item label="权限标识"><el-input v-model="form.perms" placeholder="如 system:user:add" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
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
import { apiMenuTree, apiMenuAdd, apiMenuUpdate, apiMenuDelete } from '../../api'

const userStore = useUserStore()
const hasPerm = (c) => userStore.hasPerm(c)

const tree = ref([])
const loading = ref(false)
const dialog = ref(false)
const form = reactive({ id: null, parentId: null, type: 2, name: '', path: '', icon: '', perms: '', sort: 0 })

const treeSelectData = computed(() => [{ id: 0, name: '顶级菜单', children: tree.value }])

onMounted(load)

async function load() {
  loading.value = true
  try { tree.value = await apiMenuTree() } finally { loading.value = false }
}

function typeName(t) { return t === 1 ? '目录' : t === 2 ? '菜单' : '按钮' }
function typeTag(t) { return t === 1 ? 'warning' : t === 2 ? 'primary' : 'info' }

function openAdd(parent) {
  Object.assign(form, { id: null, parentId: parent ? parent.id : 0, type: parent ? 2 : 1, name: '', path: '', icon: '', perms: '', sort: 0 })
  dialog.value = true
}
function openEdit(row) {
  Object.assign(form, { ...row, parentId: row.parentId || 0 })
  dialog.value = true
}
async function submit() {
  if (form.id) await apiMenuUpdate(form)
  else await apiMenuAdd(form)
  ElMessage.success('保存成功')
  dialog.value = false
  load()
}
async function remove(row) {
  await ElMessageBox.confirm(`确定删除菜单 ${row.name}?`, '提示', { type: 'warning' })
  await apiMenuDelete(row.id)
  ElMessage.success('已删除')
  load()
}
</script>

<style scoped>
.page-card { background: #fff; border-radius: 10px; padding: 18px; }
.toolbar { display: flex; align-items: center; margin-bottom: 16px; }
.title { font-size: 16px; font-weight: 600; }
.spacer { flex: 1; }

@media (max-width: 768px) {
  .page-card {
    padding: 12px;
  }
  .toolbar {
    flex-wrap: wrap;
    gap: 8px;
  }
  .toolbar :deep(.el-button) {
    flex: 1;
    min-width: calc(50% - 4px);
  }
  .spacer {
    display: none;
  }
}
</style>

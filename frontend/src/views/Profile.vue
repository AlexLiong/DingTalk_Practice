<template>
  <div class="profile-page">
    <div class="profile-top">
      <el-button text :icon="Back" @click="$router.push('/chat')">返回</el-button>
      <span class="ptitle">个人中心</span>
    </div>

    <div class="profile-body">
      <el-card class="left-card">
        <div class="avatar-box">
          <el-avatar :size="90" shape="square" :src="form.avatar" :style="avatarStyle(form.nickname)">
            {{ firstChar(form.nickname) }}
          </el-avatar>
          <el-upload :show-file-list="false" :http-request="uploadAvatar" accept="image/*">
            <el-button size="small" :icon="Camera" style="margin-top:12px">更换头像</el-button>
          </el-upload>
        </div>
        <div class="info-line"><span>账号</span><b>{{ form.username }}</b></div>
        <div class="info-line"><span>部门</span><b>{{ form.deptName || '-' }}</b></div>
        <div class="info-line"><span>职位</span><b>{{ form.jobTitle || '-' }}</b></div>
      </el-card>

      <el-card class="right-card">
        <el-tabs v-model="tab">
          <el-tab-pane label="基本资料" name="info">
            <el-form :model="form" label-width="80px" style="max-width:420px">
              <el-form-item label="姓名"><el-input v-model="form.nickname" /></el-form-item>
              <el-form-item label="手机"><el-input v-model="form.mobile" /></el-form-item>
              <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
              <el-form-item label="性别">
                <el-radio-group v-model="form.gender">
                  <el-radio :value="1">男</el-radio>
                  <el-radio :value="2">女</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="saveInfo">保存资料</el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="修改密码" name="pwd">
            <el-form :model="pwd" label-width="90px" style="max-width:420px">
              <el-form-item label="原密码"><el-input v-model="pwd.oldPassword" type="password" show-password /></el-form-item>
              <el-form-item label="新密码"><el-input v-model="pwd.newPassword" type="password" show-password /></el-form-item>
              <el-form-item label="确认密码"><el-input v-model="pwd.confirm" type="password" show-password /></el-form-item>
              <el-form-item>
                <el-button type="primary" @click="savePwd">修改密码</el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Back, Camera } from '@element-plus/icons-vue'
import { useUserStore } from '../store/user'
import { apiUpdateProfile, apiChangePassword, apiUpload } from '../api'
import { useUserPreferenceStore } from '../store/userPreference'

const userStore = useUserStore()
const preferenceStore = useUserPreferenceStore()
const tab = ref('info')
const profileStateReady = ref(false)
const form = reactive({ username: '', nickname: '', mobile: '', email: '', gender: 1, avatar: '', deptName: '', jobTitle: '' })
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })

watch(() => preferenceStore.loaded, (loaded) => {
  if (!loaded || profileStateReady.value) return
  const state = preferenceStore.getPageState('profile')
  if (typeof state.tab === 'string') tab.value = state.tab
  profileStateReady.value = true
}, { immediate: true })

watch(tab, () => {
  if (!profileStateReady.value || !preferenceStore.loaded) return
  preferenceStore.setPageState('profile', { tab: tab.value })
})

onMounted(async () => {
  if (!userStore.user) await userStore.fetchInfo()
  Object.assign(form, userStore.user)
})

async function saveInfo() {
  const u = await apiUpdateProfile(form)
  userStore.setUser(u)
  ElMessage.success('资料已更新')
}

async function savePwd() {
  if (pwd.newPassword !== pwd.confirm) { ElMessage.warning('两次密码不一致'); return }
  await apiChangePassword({ oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
  ElMessage.success('密码修改成功')
  pwd.oldPassword = pwd.newPassword = pwd.confirm = ''
}

async function uploadAvatar(opt) {
  const fd = new FormData()
  fd.append('file', opt.file)
  const f = await apiUpload(fd)
  form.avatar = f.url
  const u = await apiUpdateProfile(form)
  userStore.setUser(u)
  ElMessage.success('头像已更新')
}

function firstChar(name) { return name ? name.charAt(0) : '?' }
function avatarStyle(name) {
  const colors = ['#1677ff', '#52c41a', '#fa8c16', '#eb2f96', '#722ed1', '#13c2c2']
  let hash = 0
  for (const c of (name || '?')) hash += c.charCodeAt(0)
  return { background: colors[hash % colors.length], color: '#fff', fontSize: '30px' }
}
</script>

<style scoped>
.profile-page { height: 100%; background: #f0f2f5; display: flex; flex-direction: column; }
.profile-top {
  height: 56px; background: #fff; border-bottom: 1px solid #ededed;
  display: flex; align-items: center; gap: 12px; padding: 0 20px;
}
.ptitle { font-size: 16px; font-weight: 600; }
.profile-body { flex: 1; display: flex; gap: 18px; padding: 20px; }
.left-card { width: 280px; }
.right-card { flex: 1; }
.avatar-box { display: flex; flex-direction: column; align-items: center; padding: 10px 0 20px; border-bottom: 1px solid #f0f0f0; }
.info-line { display: flex; justify-content: space-between; padding: 12px 4px; border-bottom: 1px solid #f5f5f5; font-size: 14px; }
.info-line span { color: #999; }

/* ========== 移动端适配 ========== */
@media (max-width: 768px) {
  .profile-body {
    flex-direction: column;
    padding: 12px;
  }
  .left-card {
    width: 100%;
  }
  .right-card {
    width: 100%;
  }
  .profile-top {
    height: 48px;
    padding: 0 12px;
  }
}
</style>

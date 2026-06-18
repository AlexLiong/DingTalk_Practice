<template>
  <div class="login-page">
    <!-- 背景装饰 -->
    <div class="bg-deco deco1"></div>
    <div class="bg-deco deco2"></div>
    <div class="bg-deco deco3"></div>

    <div class="login-box">
      <!-- 左侧品牌区 -->
      <div class="login-left">
        <div class="brand">
          <div class="logo">
            <svg viewBox="0 0 24 24" width="36" height="36" fill="#1677ff">
              <path d="M19.5 3h-15A1.5 1.5 0 003 4.5v15A1.5 1.5 0 004.5 21h15a1.5 1.5 0 001.5-1.5v-15A1.5 1.5 0 0019.5 3zm-3.2 8.3c-.5 1.3-1.6 2.9-3.2 4.6l.4-2.4-1.6.3c.6-1 1.1-2 1.3-2.6-1.6.4-3 .9-4.1 1.4.6-1.7 2.4-3 5-3.6-.3-.4-.7-.7-1.2-1 .9-.1 1.8.1 2.5.5 1.4.3 1.1 1.8.9 2.8z"/>
            </svg>
          </div>
          <h1>企业协作平台</h1>
          <p class="slogan">高效沟通 · 智能协同 · 让工作更简单</p>
          <ul class="feature-list">
            <li><span class="dot"></span>即时消息 · 单聊群聊实时同步</li>
            <li><span class="dot"></span>组织通讯录 · 一键发起会话</li>
            <li><span class="dot"></span>工作台 · 公告待办一站集成</li>
          </ul>
        </div>
      </div>

      <!-- 右侧登录区 -->
      <div class="login-right">
        <div class="form-wrap">
          <h2>{{ isRegister ? '注册账号' : '欢迎登录' }}</h2>
          <p class="sub">{{ isRegister ? '创建一个新账号' : '请输入账号信息登录系统' }}</p>

          <form autocomplete="off" @submit.prevent="submit">
            <div class="field">
              <el-input v-model="form.username" size="large" placeholder="请输入用户名"
                        :prefix-icon="User" autocomplete="off" />
            </div>
            <div class="field">
              <el-input v-model="form.password" type="password" size="large"
                        placeholder="请输入密码" :prefix-icon="Lock"
                        autocomplete="new-password" show-password
                        @keyup.enter="submit" />
            </div>
            <el-button type="primary" size="large" class="submit-btn"
                       :loading="loading" @click="submit">
              {{ isRegister ? '注 册' : '登 录' }}
            </el-button>
          </form>

          <div class="switch">
            <span @click="isRegister = !isRegister">
              {{ isRegister ? '已有账号? 返回登录' : '没有账号? 立即注册' }}
            </span>
          </div>

          <div class="tip">
            <div class="tip-title">演示账号 <span class="tip-hint">（点击卡片快速填入）</span></div>
            <div class="tip-accounts">
              <div v-for="a in demoAccounts" :key="a.username"
                   class="acct-card" @click="quickFill(a.username)">
                <div class="acct-main">
                  <span class="acct-name">{{ a.name }}</span>
                  <span class="acct-job">{{ a.job }}</span>
                </div>
                <span class="acct-user">{{ a.username }}</span>
              </div>
            </div>
            <div class="tip-pwd">密码均为 <b>Boz@2026</b></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '../store/user'
import { apiRegister } from '../api'

const router = useRouter()
const userStore = useUserStore()
const isRegister = ref(false)
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const demoAccounts = [
  { username: 'admin',       name: '洪辰新', job: '技术总监' },
  { username: 'chenyuqi',    name: '陈豫琪', job: '产品经理' },
  { username: 'zhouleheng',  name: '周乐恒', job: '前端工程师' },
  { username: 'liangqinwei', name: '梁秦玮', job: 'UI设计师' },
  { username: 'liuyulin',    name: '刘玉林', job: '后端工程师' },
  { username: 'jiangzezhi',  name: '姜泽之', job: 'HR主管' }
]

function quickFill(username) {
  form.username = username
  form.password = 'Boz@2026'
}

async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    if (isRegister.value) {
      await apiRegister(form)
      ElMessage.success('注册成功, 请登录')
      isRegister.value = false
    } else {
      await userStore.login(form)
      ElMessage.success('登录成功')
      router.push('/chat')
    }
  } catch (e) {
    // 错误已由拦截器提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #eef3fb 0%, #dbe7fb 100%);
  position: relative;
  overflow: hidden;
}
/* 背景光斑装饰 */
.bg-deco { position: absolute; border-radius: 50%; filter: blur(60px); opacity: 0.5; }
.deco1 { width: 380px; height: 380px; background: #1677ff; top: -120px; left: -100px; }
.deco2 { width: 300px; height: 300px; background: #69b1ff; bottom: -100px; right: -80px; }
.deco3 { width: 220px; height: 220px; background: #91caff; top: 50%; left: 40%; opacity: 0.3; }

.login-box {
  position: relative;
  z-index: 1;
  width: 880px;
  min-height: 540px;
  background: #fff;
  border-radius: 16px;
  display: flex;
  overflow: hidden;
  box-shadow: 0 24px 80px rgba(22, 119, 255, 0.18);
}

/* 左侧 */
.login-left {
  width: 46%;
  background: linear-gradient(150deg, #2b8bff 0%, #1677ff 55%, #0c4cdb 100%);
  color: #fff;
  display: flex;
  align-items: center;
  padding: 0 44px;
  position: relative;
}
.login-left::after {
  content: '';
  position: absolute; inset: 0;
  background: radial-gradient(circle at 80% 20%, rgba(255,255,255,.18), transparent 40%);
}
.brand { position: relative; z-index: 1; }
.logo {
  width: 64px; height: 64px;
  background: #fff;
  border-radius: 16px;
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 24px;
  box-shadow: 0 8px 24px rgba(0,0,0,.15);
}
.brand h1 { font-size: 26px; font-weight: 600; margin-bottom: 12px; }
.slogan { font-size: 14px; opacity: 0.9; margin-bottom: 36px; }
.feature-list { font-size: 13px; line-height: 2.4; opacity: 0.95; }
.feature-list .dot {
  display: inline-block; width: 6px; height: 6px;
  background: #fff; border-radius: 50%; margin-right: 10px;
  vertical-align: middle;
}

/* 右侧 */
.login-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 48px;
}
.form-wrap { width: 100%; max-width: 320px; }
.login-right h2 { font-size: 24px; color: #1a1a1a; margin-bottom: 8px; }
.sub { font-size: 13px; color: #999; margin-bottom: 32px; }
.field { margin-bottom: 20px; }
.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  letter-spacing: 4px;
  margin-top: 4px;
  border-radius: 8px;
}
.switch { margin-top: 16px; text-align: center; }
.switch span { color: var(--dt-primary); cursor: pointer; font-size: 13px; }
.switch span:hover { text-decoration: underline; }
.tip {
  margin-top: 28px;
  padding: 14px 16px;
  background: #f6f9ff;
  border: 1px solid #e3edff;
  border-radius: 10px;
  font-size: 12px;
  color: #888;
}
.tip-title { color: #1677ff; font-weight: 600; margin-bottom: 10px; }
.tip-hint { color: #b0b8c4; font-weight: 400; font-size: 11px; }
.tip-accounts {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  margin-bottom: 10px;
}
.acct-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 10px;
  background: #fff;
  border: 1px solid #e3edff;
  border-radius: 8px;
  cursor: pointer;
  transition: all .18s;
}
.acct-card:hover {
  border-color: #1677ff;
  box-shadow: 0 2px 8px rgba(22, 119, 255, .15);
  transform: translateY(-1px);
}
.acct-main { display: flex; flex-direction: column; line-height: 1.3; min-width: 0; }
.acct-name { color: #1a1a1a; font-weight: 600; font-size: 12px; }
.acct-job { color: #8a94a6; font-size: 11px; }
.acct-user {
  color: #1677ff;
  font-size: 11px;
  font-family: 'Menlo', 'Consolas', monospace;
  background: #f0f6ff;
  padding: 1px 6px;
  border-radius: 4px;
  margin-left: 6px;
  white-space: nowrap;
}
.tip-pwd { color: #999; }
</style>

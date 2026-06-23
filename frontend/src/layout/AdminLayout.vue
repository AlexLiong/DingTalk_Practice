<template>
  <div class="admin-layout">
    <!-- 侧边栏 -->
    <aside class="sidebar">
      <div class="logo">
        <div class="logo-icon">钉</div>
        <span class="logo-text">管理后台</span>
      </div>
      <el-menu :default-active="activeMenu" router class="side-menu"
               background-color="#1f2733" text-color="#b3b8c0" active-text-color="#fff">
        <template v-for="m in menus" :key="m.id">
          <el-sub-menu v-if="m.children && m.children.length" :index="'sub-' + m.id">
            <template #title>
              <el-icon><component :is="iconMap[m.icon] || 'Menu'" /></el-icon>
              <span>{{ m.name }}</span>
            </template>
            <el-menu-item v-for="c in m.children" :key="c.id" :index="c.path">
              <el-icon><component :is="iconMap[c.icon] || 'Document'" /></el-icon>
              <span>{{ c.name }}</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="m.path">
            <el-icon><component :is="iconMap[m.icon] || 'Menu'" /></el-icon>
            <span>{{ m.name }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </aside>

    <!-- 主区 -->
    <div class="main">
      <header class="topbar">
        <div class="crumb">
          <el-icon><Location /></el-icon>
          <span>{{ currentTitle }}</span>
        </div>
        <div class="actions">
          <el-button text :icon="ChatDotRound" @click="$router.push('/chat')">返回协作</el-button>
          <el-dropdown @command="onCommand">
            <span class="user-drop">
              <el-avatar :size="30" shape="square" :style="avatarStyle(user?.nickname)">
                {{ firstChar(user?.nickname) }}
              </el-avatar>
              <span class="uname">{{ user?.nickname }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Setting, User, Avatar, Menu, OfficeBuilding, Document,
  Location, ChatDotRound, ArrowDown,DataBoard
} from '@element-plus/icons-vue'
import { useUserStore } from '../store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const user = computed(() => userStore.user)
const menus = computed(() => userStore.menus)

const iconMap = { Setting, User, Avatar, Menu, OfficeBuilding, Document,DataBoard }

const activeMenu = computed(() => route.path)
const titleMap = {
  '/admin/user': '用户管理', '/admin/role': '角色管理',
  '/admin/menu': '菜单管理', '/admin/dept': '部门管理'
}
const currentTitle = computed(() => titleMap[route.path] || '管理后台')

onMounted(async () => {
  if (!userStore.user) await userStore.fetchInfo()
})

function onCommand(cmd) {
  if (cmd === 'profile') router.push('/profile')
  else if (cmd === 'logout') { userStore.logout(); router.push('/login') }
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
.admin-layout { height: 100%; display: flex; background: #f0f2f5; }
.sidebar { width: 220px; background: #1f2733; display: flex; flex-direction: column; }
.logo {
  height: 58px; display: flex; align-items: center; gap: 10px;
  padding: 0 20px; color: #fff; border-bottom: 1px solid rgba(255,255,255,.08);
}
.logo-icon {
  width: 32px; height: 32px; background: var(--dt-primary); color: #fff;
  border-radius: 8px; display: flex; align-items: center; justify-content: center;
  font-weight: bold; font-size: 18px;
}
.logo-text { font-size: 16px; font-weight: 600; }
.side-menu { flex: 1; border-right: none; }
.main { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.topbar {
  height: 58px; background: #fff; border-bottom: 1px solid #ededed;
  display: flex; align-items: center; justify-content: space-between; padding: 0 22px;
}
.crumb { display: flex; align-items: center; gap: 6px; color: #555; font-size: 15px; font-weight: 500; }
.actions { display: flex; align-items: center; gap: 12px; }
.user-drop { display: flex; align-items: center; gap: 8px; cursor: pointer; outline: none; }
.uname { font-size: 14px; color: #333; }
.content { flex: 1; overflow-y: auto; padding: 18px; }
</style>

<template>
  <router-view />
</template>

<script setup>
import { onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from './store/user'
import { useCollabStore } from './store/collab'
import { useThemeStore } from './store/theme'
import { useUserPreferenceStore } from './store/userPreference'
import { connectWs, disconnectWs } from './utils/ws'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const collabStore = useCollabStore()
const themeStore = useThemeStore()
const preferenceStore = useUserPreferenceStore()
const routePersistenceReady = ref(false)

watch(() => userStore.token, async (token) => {
  disconnectWs()
  collabStore.reset()
  routePersistenceReady.value = false
  if (!token) {
    preferenceStore.reset()
    return
  }

  await preferenceStore.load()
  if (preferenceStore.theme) {
    themeStore.init(preferenceStore.theme)
  } else {
    themeStore.init()
    await preferenceStore.setTheme(themeStore.themeName, { immediate: true })
  }

  const lastRoute = preferenceStore.lastRoute
  const isEntryRoute = route.path === '/login' || route.fullPath === '/' || route.fullPath === '/chat'
  if (isEntryRoute && lastRoute && lastRoute !== route.fullPath && !lastRoute.startsWith('/login')) {
    try {
      await router.replace(lastRoute)
    } catch {
      /* keep current route when stored route is no longer valid */
    }
  }

  await collabStore.loadSummary()
  routePersistenceReady.value = true
  connectWs(token, null, null, null, onWorkNotice, onSessionUnread)
}, { immediate: true })

watch(() => route.fullPath, (fullPath) => {
  if (!userStore.token || !routePersistenceReady.value) return
  if (!fullPath || route.path === '/login') return
  preferenceStore.setLastRoute(fullPath)
})

onUnmounted(() => {
  disconnectWs()
})

function mapNoticeLevel(level) {
  if (level === 'success') return 'success'
  if (level === 'warning') return 'warning'
  return 'info'
}

function onWorkNotice(notice) {
  if (!notice) return
  collabStore.receiveNotice(notice)
  ElMessage({
    message: `${notice.title} · ${notice.summary}`,
    type: mapNoticeLevel(notice.level)
  })
}

function onSessionUnread(payload) {
  collabStore.setMessageUnread(payload?.messageUnread)
}
</script>

import { defineStore } from 'pinia'
import { apiUserPreference, apiUpdateUserPreference } from '../api'

let saveTimer = null

function normalizeRecord(value) {
  return value && typeof value === 'object' && !Array.isArray(value) ? value : {}
}

export const useUserPreferenceStore = defineStore('userPreference', {
  state: () => ({
    loaded: false,
    loading: false,
    theme: '',
    lastRoute: '',
    viewState: {},
    connectedOnline: false,
    lastOnlineAt: '',
    lastOfflineAt: '',
    lastActiveAt: '',
    pendingPatch: {}
  }),
  actions: {
    reset() {
      if (saveTimer) {
        clearTimeout(saveTimer)
        saveTimer = null
      }
      this.loaded = false
      this.loading = false
      this.theme = ''
      this.lastRoute = ''
      this.viewState = {}
      this.connectedOnline = false
      this.lastOnlineAt = ''
      this.lastOfflineAt = ''
      this.lastActiveAt = ''
      this.pendingPatch = {}
    },
    applyPayload(payload = {}) {
      this.theme = payload.theme === 'dark' ? 'dark' : (payload.theme === 'light' ? 'light' : this.theme)
      this.lastRoute = typeof payload.lastRoute === 'string' ? payload.lastRoute : ''
      this.viewState = normalizeRecord(payload.viewState)
      this.connectedOnline = Boolean(payload.connectedOnline)
      this.lastOnlineAt = payload.lastOnlineAt || ''
      this.lastOfflineAt = payload.lastOfflineAt || ''
      this.lastActiveAt = payload.lastActiveAt || ''
    },
    snapshot() {
      return {
        theme: this.theme,
        lastRoute: this.lastRoute,
        viewState: this.viewState,
        connectedOnline: this.connectedOnline,
        lastOnlineAt: this.lastOnlineAt,
        lastOfflineAt: this.lastOfflineAt,
        lastActiveAt: this.lastActiveAt
      }
    },
    async load(force = false) {
      if (this.loading) return this.snapshot()
      if (this.loaded && !force) return this.snapshot()

      this.loading = true
      try {
        const payload = await apiUserPreference()
        this.applyPayload(payload)
      } catch {
        /* keep local defaults and let the app continue */
      } finally {
        this.loading = false
        this.loaded = true
      }
      return this.snapshot()
    },
    getPageState(pageKey) {
      return normalizeRecord(this.viewState?.[pageKey])
    },
    setTheme(theme, options = {}) {
      this.theme = theme === 'dark' ? 'dark' : 'light'
      return this.queueSave({ theme: this.theme }, options)
    },
    setLastRoute(lastRoute, options = {}) {
      this.lastRoute = typeof lastRoute === 'string' ? lastRoute : ''
      return this.queueSave({ lastRoute: this.lastRoute || null }, options)
    },
    setPageState(pageKey, pageState, options = {}) {
      const current = this.getPageState(pageKey)
      const nextPageState = options.replace
        ? normalizeRecord(pageState)
        : { ...current, ...normalizeRecord(pageState) }
      this.viewState = {
        ...this.viewState,
        [pageKey]: nextPageState
      }
      return this.queueSave({ viewState: this.viewState }, options)
    },
    queueSave(patch = {}, options = {}) {
      this.pendingPatch = {
        ...this.pendingPatch,
        ...patch
      }
      if (!this.loaded && !options.allowBeforeLoad) return this.snapshot()

      if (options.immediate) {
        return this.flushSave()
      }

      if (saveTimer) clearTimeout(saveTimer)
      saveTimer = setTimeout(() => {
        this.flushSave().catch(() => {})
      }, options.delay ?? 320)
      return this.snapshot()
    },
    async flushSave() {
      if (!Object.keys(this.pendingPatch).length) return this.snapshot()
      const payload = { ...this.pendingPatch }
      this.pendingPatch = {}
      const next = await apiUpdateUserPreference(payload)
      this.applyPayload(next)
      return this.snapshot()
    }
  }
})

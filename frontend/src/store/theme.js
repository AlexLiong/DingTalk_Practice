import { defineStore } from 'pinia'
import { useUserPreferenceStore } from './userPreference'

export const useThemeStore = defineStore('theme', {
  state: () => ({
    dark: localStorage.getItem('theme') === 'dark'
  }),
  getters: {
    themeName: (state) => (state.dark ? 'dark' : 'light')
  },
  actions: {
    toggle() {
      return this.setTheme(this.dark ? 'light' : 'dark')
    },
    setTheme(theme, options = {}) {
      this.dark = theme === 'dark'
      localStorage.setItem('theme', this.dark ? 'dark' : 'light')
      this.apply()
      if (options.persist === false) return
      const preferenceStore = useUserPreferenceStore()
      preferenceStore.setTheme(this.themeName, { immediate: options.immediate === true })
    },
    apply() {
      document.documentElement.classList.toggle('dark', this.dark)
    },
    init(theme) {
      if (theme === 'dark' || theme === 'light') {
        this.dark = theme === 'dark'
        localStorage.setItem('theme', theme)
      }
      this.apply()
    }
  }
})

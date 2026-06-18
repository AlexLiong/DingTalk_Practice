import { defineStore } from 'pinia'
import { apiLogin, apiUserInfo } from '../api'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: null,
    permissions: [],
    menus: []
  }),
  getters: {
    isAdmin: (state) => state.permissions && state.permissions.length > 0
  },
  actions: {
    async login(form) {
      const data = await apiLogin(form)
      this.token = data.token
      this.user = data.user
      this.permissions = data.permissions || []
      this.menus = data.menus || []
      localStorage.setItem('token', data.token)
      return data
    },
    async fetchInfo() {
      const data = await apiUserInfo()
      this.user = data.user
      this.permissions = data.permissions || []
      this.menus = data.menus || []
      return this.user
    },
    hasPerm(code) {
      return this.permissions.includes(code)
    },
    setUser(u) {
      this.user = u
    },
    logout() {
      this.token = ''
      this.user = null
      this.permissions = []
      this.menus = []
      localStorage.removeItem('token')
    }
  }
})

import { defineStore } from 'pinia'
import { apiDingList, apiMailboxList, apiSessions } from '../api'

function countMailboxUnread(mails = []) {
  return mails.filter(mail => mail.direction !== 'sent' && !mail.archived && mail.unread).length
}

function countMessageUnread(sessions = []) {
  return sessions.reduce((sum, session) => sum + (session.unread || 0), 0)
}

function countDingPending(dings = []) {
  return dings.filter(ding => ding.direction === 'received' && ding.status !== 'done').length
}

function countDingUrgent(dings = []) {
  return dings.filter(ding => ding.direction === 'received' && ding.status !== 'done' && ding.priority === 'high').length
}

function normalizeNotice(notice) {
  return {
    ...notice,
    id: `${notice.category || 'work'}-${notice.action || 'notice'}-${notice.refId || 'x'}-${notice.createTime || Date.now()}`
  }
}

export const useCollabStore = defineStore('collab', {
  state: () => ({
    messageUnread: 0,
    mailboxUnread: 0,
    dingPending: 0,
    dingUrgent: 0,
    workNotices: [],
    loaded: false
  }),
  getters: {
    latestWorkNotice: (state) => state.workNotices[0] || null
  },
  actions: {
    async loadSummary() {
      try {
        const [sessions, mails, dings] = await Promise.all([apiSessions(), apiMailboxList(), apiDingList()])
        this.messageUnread = countMessageUnread(Array.isArray(sessions) ? sessions : [])
        this.mailboxUnread = countMailboxUnread(Array.isArray(mails) ? mails : [])
        this.dingPending = countDingPending(Array.isArray(dings) ? dings : [])
        this.dingUrgent = countDingUrgent(Array.isArray(dings) ? dings : [])
      } catch {
        this.messageUnread = 0
        this.mailboxUnread = 0
        this.dingPending = 0
        this.dingUrgent = 0
      } finally {
        this.loaded = true
      }
    },
    setMessageUnread(value) {
      this.messageUnread = Math.max(0, Number(value) || 0)
    },
    setMessageUnreadFromSessions(sessions = []) {
      this.messageUnread = countMessageUnread(Array.isArray(sessions) ? sessions : [])
    },
    applyNoticeCounts(notice) {
      if (typeof notice?.messageUnread === 'number') this.messageUnread = notice.messageUnread
      if (typeof notice?.mailboxUnread === 'number') this.mailboxUnread = notice.mailboxUnread
      if (typeof notice?.dingPending === 'number') this.dingPending = notice.dingPending
      if (typeof notice?.dingUrgent === 'number') this.dingUrgent = notice.dingUrgent
    },
    receiveNotice(notice) {
      if (!notice) return
      this.applyNoticeCounts(notice)
      const normalized = normalizeNotice(notice)
      this.workNotices = [
        normalized,
        ...this.workNotices.filter(item => item.id !== normalized.id)
      ].slice(0, 8)
    },
    reset() {
      this.messageUnread = 0
      this.mailboxUnread = 0
      this.dingPending = 0
      this.dingUrgent = 0
      this.workNotices = []
      this.loaded = false
    }
  }
})

import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

let client = null
let boundToken = ''

const listeners = {
  message: new Set(),
  receipt: new Set(),
  online: new Set(),
  workNotice: new Set(),
  sessionUnread: new Set(),
  kick: new Set()
}

function addListener(type, handler) {
  if (typeof handler === 'function') {
    if (!listeners[type].has(handler)) {
      listeners[type].add(handler)
    }
  }
}

function removeListener(type, handler) {
  if (typeof handler === 'function') listeners[type].delete(handler)
}

function emit(type, payload) {
  console.log(`[WS] emit ${type} listeners=${listeners[type].size}`, payload)
  listeners[type].forEach((handler) => {
    try { handler(payload) } catch (error) { console.error(`[WS] ${type} handler error`, error) }
  })
}

function makeSubscriptions(c) {
  console.log('[WS] subscribing to destinations')
  // 消息
  c.subscribe('/user/queue/message', (frame) => {
    try {
      const data = JSON.parse(frame.body)
      console.log('[WS] /user/queue/message received', data)
      emit('message', data)
    } catch (e) { console.error('[WS] message parse error', e) }
  })
  // 已读回执
  c.subscribe('/user/queue/receipt', (frame) => {
    try {
      const data = JSON.parse(frame.body)
      console.log('[WS] /user/queue/receipt received', data)
      emit('receipt', data)
    } catch (e) { console.error('[WS] receipt parse error', e) }
  })
  // 在线状态广播
  c.subscribe('/topic/online-status', (frame) => {
    try {
      const data = JSON.parse(frame.body)
      console.log('[WS] /topic/online-status received', data)
      emit('online', data)
    } catch (e) { console.error('[WS] online parse error', e) }
  })
  // 工作台协同提醒
  c.subscribe('/user/queue/work-notice', (frame) => {
    try {
      const data = JSON.parse(frame.body)
      console.log('[WS] /user/queue/work-notice received', data)
      emit('workNotice', data)
    } catch (e) { console.error('[WS] work notice parse error', e) }
  })
  // 会话未读总数
  c.subscribe('/user/queue/session-unread', (frame) => {
    try {
      const data = JSON.parse(frame.body)
      console.log('[WS] /user/queue/session-unread received', data)
      emit('sessionUnread', data)
    } catch (e) { console.error('[WS] session unread parse error', e) }
  })
  // 管理员踢人下线消息
  c.subscribe('/user/queue/kick', (frame) => {
    try {
      const data = JSON.parse(frame.body)
      console.log('[WS] /user/queue/kick received', data)
      emit('kick', data)
    } catch (e) { console.error('[WS] kick parse error', e) }
  })
}

export function connectWs(token, onMessage, onReceipt, onOnlineStatus, onWorkNotice, onSessionUnread, onKick) {
  if (!token) {
    console.warn('[WS] connect called without token')
    return null
  }

  // 注册监听器（无论是否复用连接，都要确保监听器存在）
  addListener('message', onMessage)
  addListener('receipt', onReceipt)
  addListener('online', onOnlineStatus)
  addListener('workNotice', onWorkNotice)
  addListener('sessionUnread', onSessionUnread)
  addListener('kick', onKick)

  // 若已存在同一 token 的活跃连接，直接复用
  if (client && boundToken === token && client.active) {
    console.log('[WS] reusing existing connection for same token')
    return client
  }

  // 先清理旧连接
  if (client) {
    try { client.deactivate() } catch (e) { console.warn('[WS] deactivate old client failed', e) }
    client = null
  }
  boundToken = token

  client = new Client({
    webSocketFactory: () => new SockJS('/ws'),
    connectHeaders: { Authorization: `Bearer ${token}` },
    reconnectDelay: 3000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    debug: (str) => {
      // 仅记录关键调试信息，避免刷屏
      if (str && (str.includes('connected') || str.includes('DISCONNECT') || str.includes('ERROR'))) {
        console.log('[WS] debug', str)
      }
    },
    onConnect: () => {
      console.log('[WS] CONNECTED - subscribing to queues')
      makeSubscriptions(client)
    },
    onDisconnect: () => {
      console.log('[WS] DISCONNECTED - will auto-reconnect')
    },
    onStompError: (frame) => {
      console.error('[WS] STOMP error', frame.headers, frame.body)
    },
    onWebSocketClose: (event) => {
      console.log('[WS] WebSocket closed', event?.code, event?.reason)
    },
    onWebSocketError: (event) => {
      console.error('[WS] WebSocket error', event)
    }
  })

  client.activate()
  return client
}

export function removeWsListeners(onMessage, onReceipt, onOnlineStatus, onWorkNotice, onSessionUnread, onKick) {
  removeListener('message', onMessage)
  removeListener('receipt', onReceipt)
  removeListener('online', onOnlineStatus)
  removeListener('workNotice', onWorkNotice)
  removeListener('sessionUnread', onSessionUnread)
  removeListener('kick', onKick)
}

export function disconnectWs() {
  if (client) {
    try { client.deactivate() } catch (e) { console.warn('[WS] deactivate failed', e) }
    client = null
  }
  boundToken = ''
  Object.values(listeners).forEach(set => set.clear())
}

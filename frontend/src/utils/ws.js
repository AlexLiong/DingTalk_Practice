import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

let client = null
let boundToken = ''

const listeners = {
  message: new Set(),
  receipt: new Set(),
  online: new Set(),
  workNotice: new Set(),
  sessionUnread: new Set()
}

function addListener(type, handler) {
  if (typeof handler === 'function') listeners[type].add(handler)
}

function removeListener(type, handler) {
  if (typeof handler === 'function') listeners[type].delete(handler)
}

function emit(type, payload) {
  listeners[type].forEach((handler) => {
    try { handler(payload) } catch (error) { console.error(`[WS] ${type} handler error`, error) }
  })
}

export function connectWs(token, onMessage, onReceipt, onOnlineStatus, onWorkNotice, onSessionUnread) {
  if (!token) return null
  addListener('message', onMessage)
  addListener('receipt', onReceipt)
  addListener('online', onOnlineStatus)
  addListener('workNotice', onWorkNotice)
  addListener('sessionUnread', onSessionUnread)

  if (client && boundToken === token) return client

  if (client) {
    client.deactivate()
    client = null
  }
  boundToken = token

  client = new Client({
    webSocketFactory: () => new SockJS('/ws'),
    connectHeaders: { Authorization: `Bearer ${token}` },
    reconnectDelay: 3000,
    debug: () => {},
    onConnect: () => {
      console.log('[WS] connected')
      // 消息
      client.subscribe('/user/queue/message', (frame) => {
        try { emit('message', JSON.parse(frame.body)) }
        catch (e) { console.error('[WS] parse error', e) }
      })
      // 已读回执
      client.subscribe('/user/queue/receipt', (frame) => {
        try { emit('receipt', JSON.parse(frame.body)) }
        catch (e) { console.error('[WS] receipt parse error', e) }
      })
      // 在线状态广播
      client.subscribe('/topic/online-status', (frame) => {
        try { emit('online', JSON.parse(frame.body)) }
        catch (e) { console.error('[WS] online parse error', e) }
      })
      // 工作台协同提醒
      client.subscribe('/user/queue/work-notice', (frame) => {
        try { emit('workNotice', JSON.parse(frame.body)) }
        catch (e) { console.error('[WS] work notice parse error', e) }
      })
      // 会话未读总数
      client.subscribe('/user/queue/session-unread', (frame) => {
        try { emit('sessionUnread', JSON.parse(frame.body)) }
        catch (e) { console.error('[WS] session unread parse error', e) }
      })
    },
    onStompError: (frame) => console.error('[WS] stomp error', frame),
    onWebSocketClose: () => console.log('[WS] closed')
  })

  client.activate()
  return client
}

export function removeWsListeners(onMessage, onReceipt, onOnlineStatus, onWorkNotice, onSessionUnread) {
  removeListener('message', onMessage)
  removeListener('receipt', onReceipt)
  removeListener('online', onOnlineStatus)
  removeListener('workNotice', onWorkNotice)
  removeListener('sessionUnread', onSessionUnread)
}

export function disconnectWs() {
  if (client) {
    client.deactivate()
    client = null
  }
  boundToken = ''
  Object.values(listeners).forEach(set => set.clear())
}

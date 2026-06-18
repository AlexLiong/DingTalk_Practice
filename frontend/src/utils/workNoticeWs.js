import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

let client = null
let boundToken = ''

export function connectWorkNoticeWs(token, onWorkNotice) {
  if (!token) return null
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
      client.subscribe('/user/queue/work-notice', (frame) => {
        try { onWorkNotice && onWorkNotice(JSON.parse(frame.body)) }
        catch (error) { console.error('[WorkNoticeWS] parse error', error) }
      })
    },
    onStompError: (frame) => console.error('[WorkNoticeWS] stomp error', frame),
    onWebSocketClose: () => console.log('[WorkNoticeWS] closed')
  })

  client.activate()
  return client
}

export function disconnectWorkNoticeWs() {
  if (client) {
    client.deactivate()
    client = null
  }
  boundToken = ''
}

// AppointUnified V3 — Queue API + WebSocket client
import { Client, IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { api } from '@/lib/api'
import { QueueStatus, WsMessage } from '@/types/v3'

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api'
const WS_URL  = process.env.NEXT_PUBLIC_WS_URL  || `${API_URL}/ws`

const getToken = () =>
  typeof window !== 'undefined' ? localStorage.getItem('au_access') : null

const authHeaders = () => ({
  Authorization: `Bearer ${getToken()}`,
  'Content-Type': 'application/json',
})

// ─── REST Queue APIs ──────────────────────────────────────────────────────────

export const queueApi = {
  /** Get live queue board (public — no auth) */
  getStatus: (professionalId: string) =>
    api.get<{ data: QueueStatus }>(`/queue/${professionalId}/status`),

  /** Get live queue board for authenticated professional */
  getMyStatus: () =>
    api.get<{ data: QueueStatus }>('/queue/my-status'),

  /** Get own queue position for an appointment */
  getMyPosition: (appointmentId: string) =>
    api.get(`/queue/my-position/${appointmentId}`, { headers: authHeaders() }),

  /** Join queue after booking */
  joinQueue: (appointmentId: string) =>
    api.post(`/queue/join/${appointmentId}`, {}, { headers: authHeaders() }),

  // ─── Professional ─────────────────────────────────────────────────────────

  callNext: () =>
    api.post('/queue/next', {}, { headers: authHeaders() }),

  triggerDelay: (delayMinutes: number, reason?: string) =>
    api.post('/queue/delay', { delayMinutes, reason }, { headers: authHeaders() }),

  pauseQueue: (reason?: string) =>
    api.post('/queue/pause', { reason }, { headers: authHeaders() }),

  resumeQueue: () =>
    api.post('/queue/resume', {}, { headers: authHeaders() }),

  insertEmergency: (appointmentId: string, justification: string) =>
    api.post('/queue/emergency', { appointmentId, justification }, { headers: authHeaders() }),

  broadcast: (message: string, messageType: string) =>
    api.post('/queue/broadcast', { message, messageType }, { headers: authHeaders() }),

  // ─── Preferences (Feature 3) ──────────────────────────────────────────────

  getPreferences: () =>
    api.get('/queue/preferences', { headers: authHeaders() }),

  updatePreferences: (prefs: Partial<{
    notifyAtPosition: number
    preferSmsOverPush: boolean
    autoCheckInEnabled: boolean
    showRealtimeEta: boolean
  }>) =>
    api.patch('/queue/preferences', prefs, { headers: authHeaders() }),
}

// ─── WebSocket client factory ─────────────────────────────────────────────────

/**
 * Creates and manages a STOMP-over-SockJS connection to the V3 queue.
 *
 * Usage:
 *   const sub = subscribeToQueue(professionalId, (msg) => setQueueState(msg.data))
 *   // later:
 *   sub.disconnect()
 */
export function subscribeToQueue(
  professionalId: string,
  onMessage: (msg: WsMessage) => void,
  onConnected?: () => void
): { disconnect: () => void } {
  const token = getToken()

  const client = new Client({
    webSocketFactory: () => new SockJS(WS_URL),
    connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
    reconnectDelay: 3000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,

    onConnect: () => {
      onConnected?.()
      client.subscribe(`/topic/queue/${professionalId}`, (frame: IMessage) => {
        try {
          const msg: WsMessage = JSON.parse(frame.body)
          onMessage(msg)
        } catch (e) {
          console.error('[Queue WS] Parse error:', e)
        }
      })
    },

    onStompError: (frame) => {
      console.error('[Queue WS] STOMP error:', frame.headers['message'])
    },
  })

  client.activate()
  return { disconnect: () => client.deactivate() }
}

/**
 * Subscribe to own private queue position updates.
 * /user/queue/{appointmentId} — only sent to the specific client.
 */
export function subscribeToMyPosition(
  appointmentId: string,
  onMessage: (msg: WsMessage) => void
): { disconnect: () => void } {
  const token = getToken()

  const client = new Client({
    webSocketFactory: () => new SockJS(WS_URL),
    connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
    reconnectDelay: 3000,

    onConnect: () => {
      client.subscribe(`/user/queue/${appointmentId}`, (frame: IMessage) => {
        try {
          onMessage(JSON.parse(frame.body))
        } catch (e) {
          console.error('[Private WS] Parse error:', e)
        }
      })
    },
  })

  client.activate()
  return { disconnect: () => client.deactivate() }
}

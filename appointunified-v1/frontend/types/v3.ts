// AppointUnified V3 — Queue System TypeScript Types

export type QueueTokenStatus = 'WAITING' | 'CALLED' | 'IN_PROGRESS' | 'COMPLETED' | 'SKIPPED'
export type BroadcastType = 'INFO' | 'WARNING' | 'DELAY' | 'UPDATE'

// ─── Core queue types ─────────────────────────────────────────────────────────

export interface QueueTokenView {
  tokenId: string
  appointmentId: string
  tokenNumber: number
  position: number
  estimatedWaitMins: number | null
  status: QueueTokenStatus
  calledAt?: string
  createdAt: string
}

export interface QueueStatus {
  professionalId: string
  currentlyServing: number | null
  waitingCount: number
  paused: boolean
  cumulativeDelayMins: number
  avgServiceMins: number
  waitingTokens: QueueTokenView[]
}

export interface DelayResult {
  delayMinutes: number
  tokensAffected: number
  reason: string | null
}

// ─── WebSocket message types ──────────────────────────────────────────────────

export type WsMessageType = 'QUEUE_UPDATE' | 'BROADCAST' | 'TOKEN_CALLED' | 'DELAY_TRIGGERED'

export interface WsMessage {
  type: WsMessageType
  data?: QueueStatus
  message?: string
  messageType?: BroadcastType
}

// ─── Queue preferences ────────────────────────────────────────────────────────

export interface QueuePreferences {
  notifyAtPosition: number
  preferSmsOverPush: boolean
  autoCheckInEnabled: boolean
  showRealtimeEta: boolean
}

// ─── Analytics snapshot (Feature 1) ──────────────────────────────────────────

export interface QueueAnalyticsSnapshot {
  snapshotAt: string
  queueLength: number
  currentWaitMins: number
  totalServedToday: number
  noShowsToday: number
  avgServiceMins: number | null
  paused: boolean
  cumulativeDelayMins: number
}

// ─── Emergency slot (Feature 4) ──────────────────────────────────────────────

export interface EmergencyEntry {
  id: string
  appointmentId: string
  justification: string
  insertedAt: string
}

// ─── Broadcast (Feature 5) ───────────────────────────────────────────────────

export interface QueueBroadcastItem {
  id: string
  message: string
  messageType: BroadcastType
  sentToCount: number
  createdAt: string
}

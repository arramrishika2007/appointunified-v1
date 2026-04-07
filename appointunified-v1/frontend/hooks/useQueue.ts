// AppointUnified V3 — useQueue hook
'use client'

import { useEffect, useRef, useState, useCallback } from 'react'
import { queueApi, subscribeToQueue, subscribeToMyPosition } from '@/lib/api-v3'
import { QueueStatus, QueueTokenView, WsMessage, BroadcastType } from '@/types/v3'

// ─── Hook: live queue board ───────────────────────────────────────────────────

export function useQueueStatus(professionalId: string | null) {
  const [status, setStatus] = useState<QueueStatus | null>(null)
  const [loading, setLoading] = useState(true)
  const [connected, setConnected] = useState(false)
  const [broadcasts, setBroadcasts] = useState<{ message: string; type: BroadcastType; ts: number }[]>([])
  const wsRef = useRef<{ disconnect: () => void } | null>(null)

  // Initial HTTP fetch
  useEffect(() => {
    if (!professionalId) return
    setLoading(true)
    queueApi.getStatus(professionalId)
      .then(res => setStatus(res.data.data))
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [professionalId])

  // WebSocket subscription
  useEffect(() => {
    if (!professionalId) return

    wsRef.current = subscribeToQueue(
      professionalId,
      (msg: WsMessage) => {
        if (msg.type === 'QUEUE_UPDATE' && msg.data) {
          setStatus(msg.data)
        }
        if (msg.type === 'BROADCAST' && msg.message) {
          setBroadcasts(prev => [
            { message: msg.message!, type: msg.messageType ?? 'INFO', ts: Date.now() },
            ...prev.slice(0, 4), // keep last 5
          ])
        }
      },
      () => setConnected(true)
    )

    return () => { wsRef.current?.disconnect(); setConnected(false) }
  }, [professionalId])

  const dismissBroadcast = useCallback((ts: number) => {
    setBroadcasts(prev => prev.filter(b => b.ts !== ts))
  }, [])

  return { status, loading, connected, broadcasts, dismissBroadcast }
}

// ─── Hook: own position in queue ─────────────────────────────────────────────

export function useMyQueuePosition(appointmentId: string | null) {
  const [token, setToken] = useState<QueueTokenView | null>(null)
  const [loading, setLoading] = useState(true)
  const wsRef = useRef<{ disconnect: () => void } | null>(null)

  useEffect(() => {
    if (!appointmentId) return
    queueApi.getMyPosition(appointmentId)
      .then(res => setToken(res.data.data))
      .catch(() => {})
      .finally(() => setLoading(false))

    wsRef.current = subscribeToMyPosition(appointmentId, (msg: WsMessage) => {
      if (msg.type === 'QUEUE_UPDATE' && msg.data) {
        const mine = msg.data.waitingTokens.find(t => t.appointmentId === appointmentId)
        if (mine) setToken(mine)
      }
      if (msg.type === 'TOKEN_CALLED') {
        setToken(prev => prev ? { ...prev, status: 'CALLED' } : prev)
      }
    })

    return () => wsRef.current?.disconnect()
  }, [appointmentId])

  return { token, loading }
}

// ─── Hook: professional queue control ────────────────────────────────────────

export function useProfessionalQueue(professionalId: string | null) {
  const { status, loading, connected, broadcasts, dismissBroadcast } = useQueueStatus(professionalId)
  const [actionLoading, setActionLoading] = useState(false)

  const callNext = useCallback(async () => {
    setActionLoading(true)
    try { await queueApi.callNext() }
    finally { setActionLoading(false) }
  }, [])

  const triggerDelay = useCallback(async (minutes: number, reason?: string) => {
    setActionLoading(true)
    try { await queueApi.triggerDelay(minutes, reason) }
    finally { setActionLoading(false) }
  }, [])

  const pause = useCallback(async (reason?: string) => {
    await queueApi.pauseQueue(reason)
  }, [])

  const resume = useCallback(async () => {
    await queueApi.resumeQueue()
  }, [])

  const sendBroadcast = useCallback(async (message: string, type: string) => {
    await queueApi.broadcast(message, type)
  }, [])

  return {
    status, loading, connected, broadcasts, dismissBroadcast,
    actionLoading, callNext, triggerDelay, pause, resume, sendBroadcast
  }
}

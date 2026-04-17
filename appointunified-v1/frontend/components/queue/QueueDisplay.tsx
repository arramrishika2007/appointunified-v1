'use client'

import { useEffect, useState } from 'react'
import { AlertTriangle, CheckCircle2, Clock, Pause, Users, Wifi, WifiOff } from 'lucide-react'
import { QueueStatus, QueueTokenView, BroadcastType } from '@/types/v3'
import { cn } from '@/lib/utils'

// ─── Live Queue Board (shows all waiting tokens — public display) ─────────────

interface QueueBoardProps {
  status: QueueStatus
  connected: boolean
  myAppointmentId?: string
}

export function QueueBoard({ status, connected, myAppointmentId }: QueueBoardProps) {
  return (
    <div className="space-y-4">
      {/* Status header */}
      <div className="card p-4 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className={cn(
            'h-3 w-3 rounded-full',
            status.paused ? 'bg-amber-400' : 'bg-emerald-500 animate-pulse'
          )} />
          <div>
            <p className="font-semibold text-slate-900 text-sm">
              {status.paused ? 'Queue Paused' : 'Queue Active'}
            </p>
            <p className="text-xs text-slate-500">
              {status.waitingCount} waiting · Est. {status.avgServiceMins} min/client
              {status.cumulativeDelayMins > 0 && (
                <span className="text-amber-600 ml-1">· +{status.cumulativeDelayMins}m delay</span>
              )}
            </p>
          </div>
        </div>
        <div className="flex items-center gap-1.5 text-xs text-slate-400">
          {connected
            ? <><Wifi size={13} className="text-emerald-500" /> Live</>
            : <><WifiOff size={13} className="text-slate-400" /> Reconnecting…</>
          }
        </div>
      </div>

      {/* Currently serving */}
      {status.currentlyServing && (
        <div className="card p-4 border-brand-300 bg-brand-50 flex items-center gap-3">
          <div className="h-10 w-10 rounded-xl bg-brand-600 flex items-center justify-center text-white font-bold text-lg">
            {status.currentlyServing}
          </div>
          <div>
            <p className="text-xs text-brand-500 font-medium uppercase tracking-wide">Now Serving</p>
            <p className="text-xl font-black text-brand-700">Token #{status.currentlyServing}</p>
          </div>
        </div>
      )}

      {/* Waiting list */}
      {status.waitingTokens.length === 0 ? (
        <div className="card p-8 text-center">
          <CheckCircle2 size={32} className="mx-auto text-slate-300 mb-2" />
          <p className="text-slate-500 text-sm">Queue is empty</p>
        </div>
      ) : (
        <div className="space-y-2">
          {status.waitingTokens.map(t => {
            const isMe = t.appointmentId === myAppointmentId
            return (
              <QueueTokenRow key={t.tokenId} token={t} isMe={isMe} />
            )
          })}
        </div>
      )}
    </div>
  )
}

// ─── Single token row ─────────────────────────────────────────────────────────

function QueueTokenRow({ token, isMe }: { token: QueueTokenView; isMe: boolean }) {
  return (
    <div className={cn(
      'card p-4 flex items-center gap-4 transition-all',
      isMe ? 'border-brand-400 bg-brand-50' : 'border-slate-200'
    )}>
      <div className={cn(
        'h-10 w-10 rounded-xl flex items-center justify-center font-bold text-lg flex-shrink-0',
        isMe ? 'bg-brand-600 text-white' : 'bg-slate-100 text-slate-700'
      )}>
        {token.tokenNumber}
      </div>
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2">
          <p className={cn('text-sm font-semibold', isMe ? 'text-brand-700' : 'text-slate-700')}>
            {isMe ? 'You - ' : ''}Position #{token.position}
          </p>
        </div>
        {token.estimatedWaitMins != null && (
          <p className="text-xs text-slate-400 flex items-center gap-1">
            <Clock size={10} />
            ~{token.estimatedWaitMins} min wait
          </p>
        )}
      </div>
      {isMe && (
        <span className="badge bg-brand-100 text-brand-700 text-xs">Your token</span>
      )}
    </div>
  )
}

// ─── My Queue Position Widget (compact, for dashboard) ───────────────────────

interface MyPositionWidgetProps {
  token: QueueTokenView
  professionalName: string
  onRefresh?: () => void
}

export function MyPositionWidget({ token, professionalName }: MyPositionWidgetProps) {
  const [elapsed, setElapsed] = useState(0)

  useEffect(() => {
    const interval = setInterval(() => setElapsed(e => e + 1), 60000) // update every minute
    return () => clearInterval(interval)
  }, [])

  if (token.status === 'CALLED') {
    return (
      <div className="card p-5 border-emerald-300 bg-emerald-50 text-center animate-pulse-soft">
        <div className="mb-2 flex justify-center">
          <CheckCircle2 size={28} className="text-emerald-600" />
        </div>
        <p className="text-lg font-black text-emerald-700">It&apos;s your turn!</p>
        <p className="text-sm text-emerald-600 mt-1">
          {professionalName} is ready for you. Please proceed now.
        </p>
      </div>
    )
  }

  if (token.status === 'WAITING') {
    return (
      <div className="card p-5">
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-semibold text-slate-900 flex items-center gap-2">
            <Users size={16} className="text-brand-600" /> Your Queue Position
          </h3>
          <span className="text-xs text-slate-400">Token #{token.tokenNumber}</span>
        </div>

        {/* Big position number */}
        <div className="text-center py-4">
          <div className="text-6xl font-black text-brand-600 leading-none">
            #{token.position}
          </div>
          <p className="text-sm text-slate-500 mt-2">in queue at {professionalName}</p>
        </div>

        {/* ETA */}
        {token.estimatedWaitMins != null && (
          <div className="mt-4 rounded-xl bg-slate-50 p-3 text-center">
            <p className="text-xs text-slate-400 mb-0.5">Estimated wait</p>
            <p className="text-2xl font-bold text-slate-900">
              ~{token.estimatedWaitMins} min
            </p>
          </div>
        )}

        {/* Progress bar */}
        <div className="mt-4">
          <div className="flex justify-between text-xs text-slate-400 mb-1">
            <span>Waiting</span><span>Your turn</span>
          </div>
          <div className="h-2 bg-slate-100 rounded-full overflow-hidden">
            <div
              className="h-full bg-brand-500 rounded-full transition-all duration-1000"
              style={{
                width: token.position <= 1 ? '90%' : `${Math.max(5, 100 - (token.position - 1) * 20)}%`
              }}
            />
          </div>
        </div>
      </div>
    )
  }

  return null
}

// ─── Broadcast Banner (Feature 5) ────────────────────────────────────────────

const BROADCAST_STYLE: Record<BroadcastType, { bg: string; icon: string; color: string }> = {
  INFO:    { bg: 'bg-blue-50 border-blue-200', icon: 'INFO', color: 'text-blue-700' },
  WARNING: { bg: 'bg-amber-50 border-amber-200', icon: 'WARN', color: 'text-amber-700' },
  DELAY:   { bg: 'bg-orange-50 border-orange-200', icon: 'DELAY', color: 'text-orange-700' },
  UPDATE:  { bg: 'bg-emerald-50 border-emerald-200', icon: 'UPDATE', color: 'text-emerald-700' },
}

interface BroadcastBannerProps {
  message: string
  type: BroadcastType
  onDismiss: () => void
}

export function BroadcastBanner({ message, type, onDismiss }: BroadcastBannerProps) {
  const style = BROADCAST_STYLE[type]
  return (
    <div className={cn('rounded-xl border p-3 flex items-center gap-3 animate-slide-up', style.bg)}>
      <span className={cn('flex-shrink-0 rounded-md border border-current/20 px-1.5 py-0.5 text-[10px] font-semibold leading-none', style.color)}>{style.icon}</span>
      <p className={cn('text-sm flex-1', style.color)}>{message}</p>
      <button onClick={onDismiss} className="text-slate-400 hover:text-slate-600 text-xs flex-shrink-0">✕</button>
    </div>
  )
}

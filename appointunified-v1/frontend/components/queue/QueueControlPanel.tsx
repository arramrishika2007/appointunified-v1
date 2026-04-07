'use client'

import { useState } from 'react'
import {
  AlertTriangle, ChevronDown, Loader2, MessageSquare,
  Pause, Play, SkipForward, Zap
} from 'lucide-react'
import { cn } from '@/lib/utils'
import toast from 'react-hot-toast'

interface Props {
  professionalId: string
  isPaused: boolean
  waitingCount: number
  onCallNext: () => Promise<void>
  onTriggerDelay: (minutes: number, reason?: string) => Promise<void>
  onPause: (reason?: string) => Promise<void>
  onResume: () => Promise<void>
  onBroadcast: (message: string, type: string) => Promise<void>
}

const DELAY_PRESETS = [5, 10, 15, 20, 30, 45, 60]
const BROADCAST_TYPES = [
  { value: 'INFO',    label: 'ℹ️ Info' },
  { value: 'WARNING', label: '⚠️ Warning' },
  { value: 'DELAY',   label: '⏰ Delay' },
  { value: 'UPDATE',  label: '📢 Update' },
]

export function QueueControlPanel({
  isPaused, waitingCount,
  onCallNext, onTriggerDelay, onPause, onResume, onBroadcast
}: Props) {
  const [loading, setLoading] = useState<string | null>(null)
  const [showDelayPanel, setShowDelayPanel] = useState(false)
  const [showBroadcastPanel, setShowBroadcastPanel] = useState(false)
  const [customDelay, setCustomDelay] = useState('')
  const [delayReason, setDelayReason] = useState('')
  const [broadcastMsg, setBroadcastMsg] = useState('')
  const [broadcastType, setBroadcastType] = useState('INFO')
  const [showPauseInput, setShowPauseInput] = useState(false)
  const [pauseReason, setPauseReason] = useState('')

  const run = async (key: string, fn: () => Promise<void>, successMsg: string) => {
    setLoading(key)
    try {
      await fn()
      toast.success(successMsg)
    } catch (err: unknown) {
      toast.error(
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message
          ?? 'Action failed'
      )
    } finally {
      setLoading(null)
    }
  }

  const handleDelay = async (minutes: number) => {
    if (minutes < 1 || minutes > 240) { toast.error('Delay must be 1–240 minutes'); return }
    await run('delay', () => onTriggerDelay(minutes, delayReason || undefined),
      `Delay of ${minutes} min applied. All clients notified.`)
    setShowDelayPanel(false)
    setDelayReason('')
    setCustomDelay('')
  }

  const handleBroadcast = async () => {
    if (!broadcastMsg.trim()) { toast.error('Enter a message'); return }
    await run('broadcast', () => onBroadcast(broadcastMsg, broadcastType),
      'Message broadcast to all waiting clients.')
    setShowBroadcastPanel(false)
    setBroadcastMsg('')
  }

  const handlePause = async () => {
    await run('pause', () => onPause(pauseReason || undefined), 'Queue paused')
    setShowPauseInput(false)
    setPauseReason('')
  }

  return (
    <div className="card p-5 space-y-4">
      <h2 className="font-semibold text-slate-900 flex items-center gap-2">
        <Zap size={16} className="text-brand-600" /> Queue Control
      </h2>

      {/* Primary actions */}
      <div className="grid grid-cols-2 gap-3">

        {/* Call Next */}
        <button
          onClick={() => run('next', onCallNext, 'Next client called!')}
          disabled={loading !== null || waitingCount === 0 || isPaused}
          className="btn-primary py-3 text-sm flex-col gap-1 h-auto disabled:opacity-40"
        >
          {loading === 'next'
            ? <Loader2 size={18} className="animate-spin" />
            : <SkipForward size={18} />
          }
          <span>Call Next</span>
          {waitingCount > 0 && <span className="text-xs text-brand-200">#{waitingCount} waiting</span>}
        </button>

        {/* Pause / Resume */}
        {isPaused ? (
          <button
            onClick={() => run('pause', onResume, 'Queue resumed!')}
            disabled={loading !== null}
            className="btn-secondary py-3 text-sm flex-col gap-1 h-auto border-emerald-300 text-emerald-700 hover:bg-emerald-50"
          >
            {loading === 'pause' ? <Loader2 size={18} className="animate-spin" /> : <Play size={18} />}
            <span>Resume Queue</span>
          </button>
        ) : (
          <button
            onClick={() => setShowPauseInput(!showPauseInput)}
            disabled={loading !== null}
            className="btn-secondary py-3 text-sm flex-col gap-1 h-auto border-amber-300 text-amber-700 hover:bg-amber-50"
          >
            <Pause size={18} />
            <span>Pause Queue</span>
          </button>
        )}
      </div>

      {/* Pause reason input */}
      {showPauseInput && !isPaused && (
        <div className="rounded-xl bg-amber-50 border border-amber-200 p-3 space-y-2 animate-slide-up">
          <input
            value={pauseReason}
            onChange={e => setPauseReason(e.target.value)}
            className="input text-sm"
            placeholder="Reason (e.g. Lunch break, 30 min)"
          />
          <div className="flex gap-2">
            <button onClick={handlePause} disabled={loading === 'pause'}
              className="btn-primary text-xs px-3 py-1.5">
              {loading === 'pause' ? <Loader2 size={12} className="animate-spin" /> : null}
              Confirm Pause
            </button>
            <button onClick={() => setShowPauseInput(false)} className="btn-secondary text-xs px-3 py-1.5">
              Cancel
            </button>
          </div>
        </div>
      )}

      {/* Secondary actions */}
      <div className="grid grid-cols-2 gap-2">
        <button
          onClick={() => setShowDelayPanel(!showDelayPanel)}
          className={cn('btn-secondary text-xs py-2', showDelayPanel && 'bg-orange-50 border-orange-300 text-orange-700')}
        >
          ⏰ Add Delay <ChevronDown size={12} />
        </button>
        <button
          onClick={() => setShowBroadcastPanel(!showBroadcastPanel)}
          className={cn('btn-secondary text-xs py-2', showBroadcastPanel && 'bg-blue-50 border-blue-300 text-blue-700')}
        >
          📢 Broadcast <ChevronDown size={12} />
        </button>
      </div>

      {/* Delay panel */}
      {showDelayPanel && (
        <div className="rounded-xl bg-orange-50 border border-orange-200 p-4 space-y-3 animate-slide-up">
          <p className="text-xs font-semibold text-orange-700">How many minutes delay?</p>
          <div className="flex flex-wrap gap-2">
            {DELAY_PRESETS.map(m => (
              <button
                key={m}
                onClick={() => handleDelay(m)}
                disabled={loading === 'delay'}
                className="rounded-lg border border-orange-300 bg-white px-3 py-1.5 text-sm font-semibold text-orange-700 hover:bg-orange-100 transition-colors"
              >
                +{m}m
              </button>
            ))}
          </div>
          <div className="flex gap-2">
            <input
              type="number"
              value={customDelay}
              onChange={e => setCustomDelay(e.target.value)}
              className="input text-sm w-24"
              placeholder="Custom"
              min={1} max={240}
            />
            <input
              value={delayReason}
              onChange={e => setDelayReason(e.target.value)}
              className="input text-sm flex-1"
              placeholder="Reason (optional)"
            />
            {customDelay && (
              <button
                onClick={() => handleDelay(parseInt(customDelay))}
                disabled={loading === 'delay'}
                className="btn-primary text-xs px-3"
              >
                Apply
              </button>
            )}
          </div>
        </div>
      )}

      {/* Broadcast panel (Feature 5) */}
      {showBroadcastPanel && (
        <div className="rounded-xl bg-blue-50 border border-blue-200 p-4 space-y-3 animate-slide-up">
          <p className="text-xs font-semibold text-blue-700">Send a message to everyone waiting</p>
          <div className="flex gap-2">
            {BROADCAST_TYPES.map(bt => (
              <button
                key={bt.value}
                onClick={() => setBroadcastType(bt.value)}
                className={cn(
                  'rounded-lg border px-2 py-1 text-xs font-medium transition-all',
                  broadcastType === bt.value
                    ? 'border-brand-500 bg-brand-50 text-brand-700'
                    : 'border-slate-200 text-slate-500'
                )}
              >
                {bt.label}
              </button>
            ))}
          </div>
          <textarea
            value={broadcastMsg}
            onChange={e => setBroadcastMsg(e.target.value)}
            rows={2}
            className="input resize-none text-sm"
            placeholder="Your message to all clients in queue…"
            maxLength={300}
          />
          <div className="flex gap-2">
            <button onClick={handleBroadcast} disabled={loading === 'broadcast'}
              className="btn-primary text-xs px-3 py-1.5">
              {loading === 'broadcast' ? <Loader2 size={12} className="animate-spin" /> : <MessageSquare size={12} />}
              Send to {waitingCount} client{waitingCount !== 1 ? 's' : ''}
            </button>
            <button onClick={() => setShowBroadcastPanel(false)} className="btn-secondary text-xs px-3 py-1.5">
              Cancel
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

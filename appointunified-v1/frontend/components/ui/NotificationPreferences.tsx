'use client'

import { useEffect, useState } from 'react'
import { Loader2 } from 'lucide-react'
import { api } from '@/lib/api'
import { cn } from '@/lib/utils'
import toast from 'react-hot-toast'

interface Prefs {
  emailEnabled: boolean
  smsEnabled: boolean
  whatsappEnabled: boolean
  pushEnabled: boolean
  reminderHours: number
}

export function NotificationPreferences() {
  const [prefs, setPrefs] = useState<Prefs | null>(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    api.get('/notifications/preferences')
      .then(res => setPrefs(res.data.data))
      .catch(() => {})
  }, [])

  const toggle = async (field: keyof Prefs) => {
    if (!prefs) return
    const updated = { ...prefs, [field]: !prefs[field as keyof Prefs] }
    setPrefs(updated)
    setSaving(true)
    try {
      await api.patch('/notifications/preferences', { [field]: !prefs[field as keyof Prefs] })
    } catch {
      setPrefs(prefs) // revert
      toast.error('Failed to save preference')
    } finally {
      setSaving(false)
    }
  }

  const setReminderHours = async (hours: number) => {
    if (!prefs) return
    const updated = { ...prefs, reminderHours: hours }
    setPrefs(updated)
    try {
      await api.patch('/notifications/preferences', { reminderHours: hours })
      toast.success('Reminder preference saved')
    } catch {
      setPrefs(prefs)
      toast.error('Failed to save')
    }
  }

  if (!prefs) {
    return (
      <div className="flex items-center gap-2 py-4 text-slate-400 text-sm">
        <Loader2 size={16} className="animate-spin" /> Loading preferences…
      </div>
    )
  }

  const CHANNELS: { field: keyof Prefs; label: string; desc: string; icon: string }[] = [
    { field: 'emailEnabled',    label: 'Email',     desc: 'Confirmations, reminders, cancellations', icon: '📧' },
    { field: 'smsEnabled',      label: 'SMS',       desc: 'Quick text alerts for time-sensitive updates', icon: '💬' },
    { field: 'whatsappEnabled', label: 'WhatsApp',  desc: 'Queue updates and appointment reminders', icon: '📱' },
    { field: 'pushEnabled',     label: 'Push',      desc: 'Browser and mobile push notifications', icon: '🔔' },
  ]

  return (
    <div className="space-y-5">
      <div className="card p-5">
        <h3 className="font-semibold text-slate-900 mb-4 flex items-center gap-2">
          Notification Channels
          {saving && <Loader2 size={13} className="animate-spin text-slate-400" />}
        </h3>
        <div className="space-y-3">
          {CHANNELS.map(ch => (
            <div key={ch.field} className="flex items-center justify-between">
              <div className="flex items-start gap-3">
                <span className="text-lg leading-none mt-0.5">{ch.icon}</span>
                <div>
                  <p className="text-sm font-medium text-slate-900">{ch.label}</p>
                  <p className="text-xs text-slate-500">{ch.desc}</p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => toggle(ch.field)}
                className={cn(
                  'relative h-5 w-9 rounded-full transition-colors flex-shrink-0',
                  prefs[ch.field] ? 'bg-brand-500' : 'bg-slate-300'
                )}
                aria-label={`Toggle ${ch.label}`}
                title={`Toggle ${ch.label}`}
              >
                <span className={cn(
                  'absolute top-0.5 h-4 w-4 rounded-full bg-white shadow transition-transform',
                  prefs[ch.field] ? 'translate-x-4' : 'translate-x-0.5'
                )} />
              </button>
            </div>
          ))}
        </div>
      </div>

      <div className="card p-5">
        <h3 className="font-semibold text-slate-900 mb-4">Reminder Timing</h3>
        <p className="text-xs text-slate-500 mb-3">When should we remind you before an appointment?</p>
        <div className="grid grid-cols-3 gap-2">
          {[
            { value: 24,  label: '24 hours' },
            { value: 2,   label: '2 hours'  },
            { value: 0,   label: '30 mins'  },
          ].map(opt => (
            <button
              key={opt.value}
              onClick={() => setReminderHours(opt.value)}
              className={cn(
                'rounded-lg border-2 py-2.5 text-sm font-medium transition-all',
                prefs.reminderHours === opt.value
                  ? 'border-brand-500 bg-brand-50 text-brand-700'
                  : 'border-slate-200 text-slate-600 hover:border-slate-300'
              )}
            >
              {opt.label}
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}

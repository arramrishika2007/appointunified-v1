'use client'

import { useEffect, useState } from 'react'
import { Loader2, Save } from 'lucide-react'
import { Navbar } from '@/components/layout/Navbar'
import { queueApi } from '@/lib/api-v3'
import { QueuePreferences } from '@/types/v3'
import { cn } from '@/lib/utils'
import toast from 'react-hot-toast'

export default function QueuePreferencesPage() {
  const [prefs, setPrefs] = useState<QueuePreferences | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    queueApi.getPreferences()
      .then(res => setPrefs(res.data.data))
      .finally(() => setLoading(false))
  }, [])

  const save = async () => {
    if (!prefs) return
    setSaving(true)
    try {
      await queueApi.updatePreferences(prefs)
      toast.success('Queue preferences saved')
    } catch { toast.error('Failed to save') }
    finally { setSaving(false) }
  }

  if (loading || !prefs) {
    return (
      <>
        <Navbar />
        <div className="flex justify-center py-16"><Loader2 size={24} className="animate-spin text-brand-600" /></div>
      </>
    )
  }

  return (
    <>
      <Navbar />
      <main className="min-h-screen bg-slate-50">
        <div className="container-page py-8 max-w-xl">
          <h1 className="text-2xl font-bold text-slate-900 mb-2">Queue Preferences</h1>
          <p className="text-slate-500 text-sm mb-6">
            Control how you receive queue notifications and updates.
          </p>

          <div className="space-y-4">

            {/* Notify position */}
            <div className="card p-5">
              <h2 className="font-semibold text-slate-900 mb-1">Alert me when I&apos;m this close</h2>
              <p className="text-xs text-slate-500 mb-4">
                Receive a notification when you are this many positions away from being called.
              </p>
              <div className="flex gap-2">
                {[1, 2, 3, 5, 10].map(pos => (
                  <button
                    key={pos}
                    onClick={() => setPrefs(p => p ? { ...p, notifyAtPosition: pos } : p)}
                    className={cn(
                      'rounded-xl border-2 w-14 h-14 text-lg font-bold transition-all',
                      prefs.notifyAtPosition === pos
                        ? 'border-brand-500 bg-brand-50 text-brand-700'
                        : 'border-slate-200 text-slate-600 hover:border-slate-300'
                    )}
                  >
                    #{pos}
                  </button>
                ))}
              </div>
            </div>

            {/* Toggles */}
            <div className="card p-5 space-y-4">
              {[
                {
                  field: 'showRealtimeEta' as const,
                  label: 'Show real-time ETA',
                  desc: 'Display estimated wait time on the queue tracker page.'
                },
                {
                  field: 'autoCheckInEnabled' as const,
                  label: 'Auto check-in',
                  desc: 'Automatically mark yourself as arrived when you open the queue tracker.'
                },
                {
                  field: 'preferSmsOverPush' as const,
                  label: 'Prefer SMS over push',
                  desc: 'Use SMS instead of push notification for queue alerts (if SMS is enabled).'
                },
              ].map(item => (
                <div key={item.field} className="flex items-center justify-between gap-4">
                  <div>
                    <p className="text-sm font-medium text-slate-900">{item.label}</p>
                    <p className="text-xs text-slate-500">{item.desc}</p>
                  </div>
                  <button
                    onClick={() => setPrefs(p => p ? { ...p, [item.field]: !p[item.field] } : p)}
                    className={cn(
                      'relative h-5 w-9 rounded-full transition-colors flex-shrink-0',
                      prefs[item.field] ? 'bg-brand-500' : 'bg-slate-300'
                    )}
                  >
                    <span className={cn(
                      'absolute top-0.5 h-4 w-4 rounded-full bg-white shadow transition-transform',
                      prefs[item.field] ? 'translate-x-4' : 'translate-x-0.5'
                    )} />
                  </button>
                </div>
              ))}
            </div>

            <button onClick={save} disabled={saving} className="btn-primary w-full py-3">
              {saving ? <Loader2 size={16} className="animate-spin" /> : <Save size={16} />}
              Save Preferences
            </button>
          </div>
        </div>
      </main>
    </>
  )
}

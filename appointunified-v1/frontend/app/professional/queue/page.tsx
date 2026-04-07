'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { Activity, Clock, Users } from 'lucide-react'
import { Navbar } from '@/components/layout/Navbar'
import { useAuthStore } from '@/lib/store'
import { useProfessionalQueue } from '@/hooks/useQueue'
import { QueueBoard, BroadcastBanner } from '@/components/queue/QueueDisplay'
import { QueueControlPanel } from '@/components/queue/QueueControlPanel'
import { queueApi } from '@/lib/api-v3'
import { cn } from '@/lib/utils'

// In a real app, fetch professionalId from /professionals/me
// For demonstration, we read it from the auth store (professional's profile ID)
// This page is only accessible to verified professionals

export default function ProfessionalQueuePage() {
  const router = useRouter()
  const { isAuthenticated, user } = useAuthStore()
  const [professionalId, setProfessionalId] = useState<string>('')

  useEffect(() => {
    if (!isAuthenticated || user?.role !== 'PROFESSIONAL') return
    queueApi.getMyStatus()
      .then((res) => setProfessionalId(res.data.data.professionalId))
      .catch(() => setProfessionalId(''))
  }, [isAuthenticated, user])

  useEffect(() => {
    if (!isAuthenticated || user?.role !== 'PROFESSIONAL') {
      router.push('/dashboard')
    }
  }, [isAuthenticated, user, router])

  const {
    status, loading, connected, broadcasts, dismissBroadcast,
    callNext, triggerDelay, pause, resume, sendBroadcast
  } = useProfessionalQueue(professionalId || null)

  if (!isAuthenticated || !user) return null

  return (
    <>
      <Navbar />
      <main className="min-h-screen bg-slate-50">
        <div className="container-page py-8 max-w-4xl">

          <div className="flex items-center justify-between mb-6">
            <div>
              <h1 className="text-2xl font-bold text-slate-900 flex items-center gap-2">
                <Activity size={22} className="text-brand-600" /> Queue Control
              </h1>
              <p className="text-slate-500 text-sm mt-1">Manage your live patient/client queue</p>
            </div>
            <div className={cn(
              'flex items-center gap-1.5 text-xs font-medium px-3 py-1.5 rounded-full border',
              connected ? 'bg-emerald-50 border-emerald-200 text-emerald-700' :
                         'bg-slate-50 border-slate-200 text-slate-500'
            )}>
              <div className={cn('h-2 w-2 rounded-full', connected ? 'bg-emerald-500 animate-pulse' : 'bg-slate-300')} />
              {connected ? 'Live' : 'Connecting…'}
            </div>
          </div>

          {/* Broadcast banners */}
          {broadcasts.map(b => (
            <div key={b.ts} className="mb-3">
              <BroadcastBanner message={b.message} type={b.type} onDismiss={() => dismissBroadcast(b.ts)} />
            </div>
          ))}

          {/* Stats row */}
          {status && (
            <div className="grid grid-cols-3 gap-4 mb-6">
              {[
                { label: 'Waiting',   value: status.waitingCount,         icon: <Users size={16} />,  color: 'bg-blue-50 text-blue-600'  },
                { label: 'Serving',   value: status.currentlyServing ?? '—', icon: <Activity size={16} />, color: 'bg-brand-50 text-brand-600' },
                { label: 'Total Delay', value: `+${status.cumulativeDelayMins}m`, icon: <Clock size={16} />, color: 'bg-orange-50 text-orange-600' },
              ].map(s => (
                <div key={s.label} className="card p-4">
                  <div className={cn('inline-flex h-8 w-8 items-center justify-center rounded-lg mb-2', s.color)}>
                    {s.icon}
                  </div>
                  <p className="text-2xl font-bold text-slate-900">{s.value}</p>
                  <p className="text-xs text-slate-500">{s.label}</p>
                </div>
              ))}
            </div>
          )}

          <div className="grid lg:grid-cols-2 gap-6">

            {/* Control panel */}
            <QueueControlPanel
              professionalId={professionalId}
              isPaused={status?.paused ?? false}
              waitingCount={status?.waitingCount ?? 0}
              onCallNext={callNext}
              onTriggerDelay={triggerDelay}
              onPause={pause}
              onResume={resume}
              onBroadcast={sendBroadcast}
            />

            {/* Live queue board */}
            <div>
              <h2 className="font-semibold text-slate-900 mb-4">Live Queue</h2>
              {loading ? (
                <div className="space-y-3">
                  {[1,2,3].map(i => (
                    <div key={i} className="card p-4 flex gap-3">
                      <div className="skeleton h-10 w-10 rounded-xl" />
                      <div className="flex-1 space-y-2"><div className="skeleton h-3 w-24" /><div className="skeleton h-3 w-16" /></div>
                    </div>
                  ))}
                </div>
              ) : status ? (
                <QueueBoard status={status} connected={connected} />
              ) : (
                <div className="card p-8 text-center">
                  <p className="text-slate-400 text-sm">
                    {professionalId
                      ? 'Queue not started yet. Call your first client to begin.'
                      : 'Loading queue…'}
                  </p>
                </div>
              )}
            </div>
          </div>
        </div>
      </main>
    </>
  )
}

'use client'

import { useSearchParams } from 'next/navigation'
import { ArrowLeft, Wifi, WifiOff } from 'lucide-react'
import Link from 'next/link'
import { Navbar } from '@/components/layout/Navbar'
import { useQueueStatus, useMyQueuePosition } from '@/hooks/useQueue'
import { QueueBoard, MyPositionWidget, BroadcastBanner } from '@/components/queue/QueueDisplay'
import { cn } from '@/lib/utils'

export default function QueueTrackerPage() {
  const searchParams = useSearchParams()
  const professionalId  = searchParams.get('professional')
  const appointmentId   = searchParams.get('appointment')
  const professionalName = searchParams.get('name') ?? 'Provider'

  const { status, loading, connected, broadcasts, dismissBroadcast } =
    useQueueStatus(professionalId)

  const { token } = useMyQueuePosition(appointmentId)

  if (!professionalId) {
    return (
      <>
        <Navbar />
        <div className="container-page py-20 text-center">
          <p className="text-slate-500">Invalid queue link. No professional ID specified.</p>
        </div>
      </>
    )
  }

  return (
    <>
      <Navbar />
      <main className="min-h-screen bg-slate-50">
        <div className="container-page py-8 max-w-lg">

          {/* Header */}
          <div className="flex items-center gap-3 mb-6">
            <Link href="/dashboard/bookings" className="btn-ghost p-2">
              <ArrowLeft size={18} />
            </Link>
            <div className="flex-1">
              <h1 className="text-xl font-bold text-slate-900">Live Queue</h1>
              <p className="text-sm text-slate-500">{professionalName}</p>
            </div>
            <div className={cn(
              'flex items-center gap-1.5 text-xs font-medium px-2.5 py-1 rounded-full',
              connected ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-500'
            )}>
              {connected
                ? <><Wifi size={11} /> Live</>
                : <><WifiOff size={11} /> Connecting…</>
              }
            </div>
          </div>

          {/* Broadcast banners (Feature 5) */}
          {broadcasts.map(b => (
            <div key={b.ts} className="mb-3">
              <BroadcastBanner
                message={b.message}
                type={b.type}
                onDismiss={() => dismissBroadcast(b.ts)}
              />
            </div>
          ))}

          {/* My position (if in queue) */}
          {token && appointmentId && (
            <div className="mb-5">
              <MyPositionWidget
                token={token}
                professionalName={professionalName}
              />
            </div>
          )}

          {/* Full queue board */}
          {loading ? (
            <div className="space-y-3">
              {[1,2,3].map(i => (
                <div key={i} className="card p-4 flex gap-3 items-center">
                  <div className="skeleton h-10 w-10 rounded-xl" />
                  <div className="flex-1 space-y-2">
                    <div className="skeleton h-3 w-32" />
                    <div className="skeleton h-3 w-20" />
                  </div>
                </div>
              ))}
            </div>
          ) : status ? (
            <QueueBoard
              status={status}
              connected={connected}
              myAppointmentId={appointmentId ?? undefined}
            />
          ) : (
            <div className="card p-8 text-center">
              <p className="text-slate-400">Queue not available. Check back soon.</p>
            </div>
          )}

          {/* Disclaimer */}
          <p className="text-center text-xs text-slate-400 mt-6">
            Queue updates in real time. Stay on this page to track your position.
          </p>
        </div>
      </main>
    </>
  )
}

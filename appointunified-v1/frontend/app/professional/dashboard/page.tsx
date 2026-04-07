'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { Calendar, CheckCircle2, Clock, Loader2, TrendingUp, Users, Zap, AlertCircle } from 'lucide-react'
import Link from 'next/link'
import { Navbar } from '@/components/layout/Navbar'
import { useAuthStore } from '@/lib/store'
import { appointmentsApi, professionalsApi } from '@/lib/api'
import { verificationApi } from '@/lib/api-v2'
import { AppointmentSummary, AvailabilityMood } from '@/types'
import { cn, formatDateTime, MOOD_CONFIG, STATUS_CONFIG } from '@/lib/utils'
import toast from 'react-hot-toast'

const MOODS: AvailabilityMood[] = ['AVAILABLE', 'RUNNING_LATE', 'TAKING_BREAKS', 'BUSY', 'DO_NOT_DISTURB']

export default function ProfessionalDashboardPage() {
  const router = useRouter()
  const { isAuthenticated, user, hasHydrated } = useAuthStore()
  const [appointments, setAppointments] = useState<AppointmentSummary[]>([])
  const [verificationStatus, setVerificationStatus] = useState<string>('APPROVED') // assume approved before load
  const [loading, setLoading] = useState(true)
  const [currentMood, setCurrentMood] = useState<AvailabilityMood>('AVAILABLE')
  const [moodNote, setMoodNote] = useState('')
  const [moodUpdating, setMoodUpdating] = useState(false)
  const [showMoodPanel, setShowMoodPanel] = useState(false)

  useEffect(() => {
    if (!hasHydrated) return
    if (!isAuthenticated) { router.push('/auth/login'); return }
    if (user?.role !== 'PROFESSIONAL') { router.push('/dashboard'); return }

    Promise.all([
      appointmentsApi.getMyAppointments({ size: 50, sort: 'startTime,asc' }).catch(err => {
        console.error('Failed to load appointments:', err)
        return { data: { data: { content: [] } } }
      }),
      verificationApi.getStatus().catch(err => {
        console.error('Failed to load verification status:', err)
        return null
      })
    ])
      .then(([apptRes, verifRes]) => {
        const now = new Date().toISOString()
        const appts = apptRes?.data?.data?.content || []
        setAppointments(
          appts.filter((a: AppointmentSummary) => a.startTime > now)
        )
        if (verifRes?.data?.data) {
          setVerificationStatus(verifRes.data.data.verificationStatus)
        }
      })
      .catch(err => console.error("Critical dashboard load failure:", err))
      .finally(() => setLoading(false))
  }, [hasHydrated, isAuthenticated, router, user])

  const handleMoodUpdate = async (mood: AvailabilityMood) => {
    setMoodUpdating(true)
    setCurrentMood(mood)
    try {
      await professionalsApi.updateMood({ mood, note: moodNote || undefined })
      toast.success(`Status updated to "${MOOD_CONFIG[mood].label}"`)
      setShowMoodPanel(false)
    } catch {
      toast.error('Failed to update mood status')
    } finally {
      setMoodUpdating(false)
    }
  }

  const todayAppts = appointments.filter(a => {
    const d = new Date(a.startTime)
    const today = new Date()
    return d.getDate() === today.getDate() &&
           d.getMonth() === today.getMonth() &&
           d.getFullYear() === today.getFullYear()
  })

  const stats = [
    { label: "Today's Bookings", value: todayAppts.length,       icon: <Calendar size={18} />,       color: 'bg-pastel-peach/30 text-orange-600' },
    { label: 'Upcoming Total',   value: appointments.length,     icon: <Clock size={18} />,          color: 'bg-pastel-purple/30 text-purple-700' },
    { label: 'Completed',        value: '—',                     icon: <CheckCircle2 size={18} />,   color: 'bg-pastel-mint/40 text-emerald-700' },
  ]

  const moodCfg = MOOD_CONFIG[currentMood]

  return (
    <div className="min-h-screen bg-primary">
      <Navbar />
      <main className="pt-32 pb-12">
        {(!hasHydrated || loading) ? (
          <div className="flex justify-center items-center min-h-[50vh]">
            <Loader2 size={32} className="animate-spin text-brand-500" />
          </div>
        ) : (
        <div className="container-page max-w-5xl">

          {/* Header */}
          <div className="flex items-start justify-between mb-6 gap-4">
            <div>
              <h1 className="text-2xl font-bold text-slate-900">Professional Dashboard</h1>
              <p className="text-slate-500 text-sm mt-1">Manage your schedule and appointments</p>
            </div>

            {/* Mood Status Widget — NEW V1 FEATURE 2 */}
            <div className="relative">
              <button
                onClick={() => setShowMoodPanel(!showMoodPanel)}
                className="flex items-center gap-2.5 rounded-xl border-2 border-slate-200 bg-white px-4 py-2.5 hover:border-brand-300 transition-all shadow-sm"
              >
                <span className={cn('h-2.5 w-2.5 rounded-full', moodCfg.dot)} />
                <span className={cn('text-sm font-semibold', moodCfg.color)}>{moodCfg.label}</span>
                <span className="text-slate-400 text-xs ml-1">▾</span>
              </button>

              {showMoodPanel && (
                <>
                  <div className="fixed inset-0 z-30" onClick={() => setShowMoodPanel(false)} />
                  <div className="absolute right-0 top-full mt-2 w-72 z-40 card shadow-xl p-4 animate-fade-in">
                    <p className="text-xs font-semibold text-slate-500 mb-3">Set your live status</p>

                    <div className="space-y-1.5 mb-3">
                      {MOODS.map(mood => {
                        const cfg = MOOD_CONFIG[mood]
                        return (
                          <button
                            key={mood}
                            onClick={() => handleMoodUpdate(mood)}
                            disabled={moodUpdating}
                            className={cn(
                              'w-full flex items-center gap-2.5 rounded-lg px-3 py-2.5 text-sm text-left transition-all',
                              currentMood === mood ? 'bg-slate-100 font-semibold' : 'hover:bg-slate-50'
                            )}
                          >
                            <span className={cn('h-2 w-2 rounded-full flex-shrink-0', cfg.dot)} />
                            <span className={cn('flex-1', cfg.color)}>{cfg.label}</span>
                            {currentMood === mood && <span className="text-emerald-500 text-xs">✓</span>}
                          </button>
                        )
                      })}
                    </div>

                    <div>
                      <label className="label text-xs">Optional note (shown to clients)</label>
                      <input
                        value={moodNote}
                        onChange={e => setMoodNote(e.target.value)}
                        className="input text-xs py-2"
                        placeholder="e.g. Running 15 min late, please wait…"
                        maxLength={150}
                      />
                    </div>
                  </div>
                </>
              )}
            </div>
          </div>

          {verificationStatus !== 'APPROVED' ? (
            <div className="mb-6 rounded-2xl bg-amber-50 p-5 border border-amber-200 flex sm:flex-row flex-col items-start sm:items-center justify-between gap-4">
              <div>
                <h3 className="text-amber-800 font-bold flex items-center gap-2">
                  <AlertCircle size={18} /> Profile Verification {verificationStatus}
                </h3>
                <p className="text-amber-700 text-sm mt-1">
                  You must complete your document verification to become visible to clients and accept bookings.
                </p>
              </div>
              <Link href="/professional/verification" className="btn-primary flex-shrink-0 text-sm px-4 py-2">
                Verify Now
              </Link>
            </div>
          ) : (
            <div className="mb-6 rounded-2xl bg-emerald-50 p-4 border border-emerald-200 flex items-center gap-3 shadow-[0_2px_10px_rgba(16,185,129,0.05)]">
              <div className="h-10 w-10 shrink-0 rounded-full bg-emerald-100 flex items-center justify-center">
                <CheckCircle2 className="text-emerald-600" size={20} />
              </div>
              <div>
                <h3 className="text-emerald-800 font-bold text-sm border-b border-emerald-200/50 pb-1 mb-1 inline-block">Profile Verified & Active</h3>
                <p className="text-emerald-700 text-xs leading-relaxed max-w-2xl">
                  Your professional identity has been verified by the admin team. You are now fully visible to clients on the platform and can receive bookings.
                </p>
              </div>
            </div>
          )}

          {/* Stats */}
          <div className="grid grid-cols-3 gap-4 mb-8">
            {stats.map(s => (
              <div key={s.label} className="card p-5">
                <div className={cn('inline-flex h-9 w-9 items-center justify-center rounded-lg mb-3', s.color)}>
                  {s.icon}
                </div>
                <p className="text-2xl font-bold text-slate-900">{s.value}</p>
                <p className="text-xs text-slate-500 mt-0.5">{s.label}</p>
              </div>
            ))}
          </div>

          {/* Today's appointments */}
          <div className="mb-8">
            <h2 className="font-bold text-slate-900 mb-4">Today&apos;s Schedule</h2>
            {loading ? (
              <div className="space-y-3">
                {[1,2].map(i => <div key={i} className="card p-4 space-y-2">
                  <div className="skeleton h-4 w-48" /> <div className="skeleton h-3 w-32" />
                </div>)}
              </div>
            ) : todayAppts.length === 0 ? (
              <div className="card p-8 text-center">
                <Calendar size={32} className="mx-auto text-slate-300 mb-2" />
                <p className="text-slate-500 text-sm">No appointments today</p>
              </div>
            ) : (
              <div className="space-y-3">
                {todayAppts.map(a => {
                  const s = STATUS_CONFIG[a.status]
                  return (
                    <div key={a.id} className="card p-5 flex items-center gap-4">
                      <div className="text-center flex-shrink-0 w-14">
                        <p className="text-xs text-slate-400">
                          {new Date(a.startTime).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' })}
                        </p>
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="font-semibold text-slate-900 truncate">{a.client.fullName}</p>
                        <p className="text-sm text-slate-500 truncate">{a.service.name}</p>
                      </div>
                      <div className="flex items-center gap-2">
                        <span className={cn('badge text-xs', s.bg, s.color)}>{s.label}</span>
                        {a.status === 'SCHEDULED' && (
                          <button
                            onClick={async () => {
                              await appointmentsApi.complete(a.id)
                              setAppointments(prev => prev.filter(x => x.id !== a.id))
                              toast.success('Marked as completed')
                            }}
                            className="btn-primary px-3 py-1.5 text-xs"
                          >
                            Complete
                          </button>
                        )}
                      </div>
                    </div>
                  )
                })}
              </div>
            )}
          </div>

          {/* Upcoming this week */}
          {appointments.filter(a => {
            const d = new Date(a.startTime)
            const now = new Date()
            const weekFromNow = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000)
            return d > now && d <= weekFromNow
          }).length > 0 && (
            <div>
              <h2 className="font-bold text-slate-900 mb-4">Upcoming This Week</h2>
              <div className="space-y-3">
                {appointments
                  .filter(a => {
                    const d = new Date(a.startTime)
                    const now = new Date()
                    const weekFromNow = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000)
                    return d > now && d <= weekFromNow &&
                           !(d.getDate() === now.getDate() && d.getMonth() === now.getMonth())
                  })
                  .map(a => (
                    <div key={a.id} className="card p-4 flex items-center gap-4">
                      <div className="flex-1 min-w-0">
                        <p className="font-medium text-slate-900 truncate">{a.client.fullName}</p>
                        <p className="text-sm text-slate-500">{formatDateTime(a.startTime)} · {a.service.name}</p>
                      </div>
                    </div>
                  ))
                }
              </div>
            </div>
          )}

        </div>
        )}
      </main>
    </div>
  )
}

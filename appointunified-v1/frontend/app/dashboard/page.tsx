'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import Link from 'next/link'
import { ArrowRight, Calendar, Clock, GitBranch, Star, TrendingUp, Zap, HeartPulse, Landmark, Settings2, ShieldAlert } from 'lucide-react'
import { Navbar } from '@/components/layout/Navbar'
import { useAuthStore } from '@/lib/store'
import { appointmentsApi, professionalsApi, usersApi } from '@/lib/api'
import { AppointmentSummary, ProfessionalSummary, RiskSummary } from '@/types'
import { cn, formatDateTime, formatDuration, SECTOR_CONFIG, STATUS_CONFIG } from '@/lib/utils'

export default function DashboardPage() {
  const router = useRouter()
  const { isAuthenticated, user } = useAuthStore()
  const [upcoming, setUpcoming] = useState<AppointmentSummary[]>([])
  const [recommended, setRecommended] = useState<ProfessionalSummary[]>([])
  const [riskSummary, setRiskSummary] = useState<RiskSummary | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!isAuthenticated) { router.push('/auth/login'); return }

    Promise.allSettled([
      appointmentsApi.getMyAppointments({ size: 3, sort: 'startTime,asc' }),
      // Smart Recommendation: fetch top-rated providers across all sectors as fallback
      professionalsApi.search({ size: 4, sort: 'ratingAvg,desc', verificationStatus: 'APPROVED' }),
      usersApi.getMyRiskSummary(),
    ]).then(([apptRes, recRes, riskRes]) => {
      const now = new Date().toISOString()

      if (apptRes.status === 'fulfilled') {
        setUpcoming(
          apptRes.value.data.data.content.filter((a: AppointmentSummary) =>
            ['SCHEDULED', 'IN_QUEUE'].includes(a.status) && a.startTime > now
          )
        )
      } else {
        setUpcoming([])
      }

      if (recRes.status === 'fulfilled') {
        setRecommended(recRes.value.data.data.content)
      } else {
        setRecommended([])
      }

      if (riskRes.status === 'fulfilled') {
        setRiskSummary(riskRes.value.data.data)
      }
    }).finally(() => setLoading(false))
  }, [isAuthenticated, router])

  if (!isAuthenticated || !user) return null

  const greeting = () => {
    const hour = new Date().getHours()
    if (hour < 12) return 'Good morning'
    if (hour < 17) return 'Good afternoon'
    return 'Good evening'
  }

  return (
    <>
      <Navbar />
      <main className="min-h-screen bg-slate-50">
        <div className="container-page py-8 max-w-5xl">

          {/* Welcome */}
          <div className="mb-8">
            <h1 className="text-2xl font-bold text-slate-900">
              {greeting()}, {user.fullName.split(' ')[0]} <span className="text-accent underline decoration-wavy ml-1">!</span>
            </h1>
            <p className="text-slate-500 text-sm mt-1">Here is your appointment overview</p>
          </div>

          {/* Quick actions */}
          <div className="grid grid-cols-3 gap-3 mb-8">
            {[
              { href: '/explore/healthcare', icon: <HeartPulse className="text-pastel-pink" />, label: 'Healthcare', color: 'bg-pastel-pink/20 hover:bg-pastel-pink/30 border-pastel-pink/30 shadow-sm' },
              { href: '/explore/government', icon: <Landmark className="text-pastel-purple" />, label: 'Government', color: 'bg-pastel-purple/20 hover:bg-pastel-purple/30 border-pastel-purple/30 shadow-sm' },
              { href: '/explore/services',   icon: <Settings2 className="text-pastel-mint" />,  label: 'Services',   color: 'bg-pastel-mint/30 hover:bg-pastel-mint/40 border-pastel-mint/30 shadow-sm' },
            ].map((item) => (
              <Link key={item.href} href={item.href}
                className={cn('card-hover flex flex-col items-center justify-center p-4 border rounded-2xl transition-all', item.color)}>
                <div className="mb-2">{item.icon}</div>
                <p className="text-xs font-bold text-text-primary">{item.label}</p>
              </Link>
            ))}
          </div>

          <div className="mb-8">
            <div className="flex items-center gap-4 text-xs font-medium">
              <Link
                href="/dashboard/queue"
                className="inline-flex items-center gap-1 text-brand-600 hover:underline"
              >
                Queue Preferences
              </Link>
              <Link
                href="/dashboard/workflows"
                className="inline-flex items-center gap-1 text-brand-600 hover:underline"
              >
                <GitBranch size={12} /> Workflow Plans
              </Link>
            </div>
          </div>

          {riskSummary && (
            <div className="mb-8 card p-4 flex items-center justify-between gap-3">
              <div>
                <p className="text-xs text-slate-500 mb-1">Behavior Risk</p>
                <p className="text-sm font-semibold text-slate-900 flex items-center gap-2">
                  <ShieldAlert size={14} className="text-amber-600" />
                  {riskSummary.riskLevel} ({riskSummary.score.toFixed(2)})
                </p>
              </div>
              <p className="text-xs text-slate-500">
                No-shows: {riskSummary.noShows} · Late cancels: {riskSummary.lastMinuteCancellations}
              </p>
            </div>
          )}

          <div className="grid lg:grid-cols-3 gap-6">

            {/* Left: Upcoming appointments */}
            <div className="lg:col-span-2 space-y-4">
              <div className="flex items-center justify-between">
                <h2 className="font-bold text-slate-900 flex items-center gap-2">
                  <Calendar size={16} className="text-brand-600" /> Upcoming
                </h2>
                <Link href="/dashboard/bookings" className="text-xs text-brand-600 hover:underline flex items-center gap-1">
                  View all <ArrowRight size={12} />
                </Link>
              </div>

              {loading ? (
                <div className="space-y-3">
                  {[1,2].map(i => <div key={i} className="card p-5 space-y-2">
                    <div className="skeleton h-4 w-48" />
                    <div className="skeleton h-3 w-32" />
                    <div className="skeleton h-3 w-24" />
                  </div>)}
                </div>
              ) : upcoming.length === 0 ? (
                <div className="card p-8 text-center">
                  <Calendar size={32} className="mx-auto text-slate-300 mb-3" />
                  <p className="font-medium text-slate-700 mb-1">No upcoming appointments</p>
                  <p className="text-xs text-slate-400 mb-4">Book from any sector below</p>
                  <Link href="/explore/healthcare" className="btn-primary text-sm">Book now</Link>
                </div>
              ) : (
                upcoming.map((a) => {
                  const s = STATUS_CONFIG[a.status]
                  return (
                    <div key={a.id} className="card p-5 flex gap-4">
                      <div className="h-12 w-12 rounded-xl bg-brand-100 flex items-center justify-center flex-shrink-0 text-brand-700 font-bold text-lg overflow-hidden">
                        {a.professional.avatarUrl
                          ? <img src={a.professional.avatarUrl} alt="" className="h-full w-full object-cover" />
                          : a.professional.displayName[0]}
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 mb-0.5">
                          <p className="font-semibold text-slate-900 truncate">{a.professional.displayName}</p>
                          <span className={cn('badge text-[10px]', s.bg, s.color)}>{s.label}</span>
                        </div>
                        <p className="text-sm text-slate-500 truncate">{a.service.name}</p>
                        <div className="flex items-center gap-3 mt-1.5 text-xs text-slate-400">
                          <span className="flex items-center gap-1"><Clock size={10} />{formatDateTime(a.startTime)}</span>
                          <span>{formatDuration(a.service.durationMinutes)}</span>
                        </div>
                      </div>
                    </div>
                  )
                })
              )}
            </div>

            {/* Right: Recommended (V1 Feature 1 — Smart Slot Recs) */}
            <div>
              <div className="flex items-center gap-2 mb-4">
                <Zap size={15} className="text-amber-500" />
                <h2 className="font-bold text-slate-900">Recommended for You</h2>
              </div>
              <div className="space-y-3">
                {loading
                  ? [1,2,3].map(i => <div key={i} className="card p-4 space-y-2">
                      <div className="skeleton h-4 w-32" />
                      <div className="skeleton h-3 w-20" />
                    </div>)
                  : recommended.map((p) => {
                    const sec = SECTOR_CONFIG[p.sector]
                    return (
                      <Link key={p.id} href={`/provider/${p.id}`} className="card-hover p-4 flex gap-3">
                        <div className="h-10 w-10 rounded-lg bg-brand-100 flex items-center justify-center text-brand-700 font-bold text-sm flex-shrink-0 overflow-hidden">
                          {p.avatarUrl ? <img src={p.avatarUrl} alt="" className="h-full w-full object-cover" /> : p.displayName[0]}
                        </div>
                        <div className="min-w-0 flex-1">
                          <p className="text-sm font-semibold text-slate-900 truncate">{p.displayName}</p>
                          <p className="text-xs text-slate-500 truncate">{p.specialty || sec.label}</p>
                          <div className="flex items-center gap-1.5 mt-1">
                            <Star size={10} className="text-amber-500 fill-current" />
                            <span className="text-xs font-medium text-slate-700">{p.ratingAvg?.toFixed(1)}</span>
                            <span className={cn('text-[9px] font-medium ml-1', sec.color)}>{sec.icon}</span>
                          </div>
                        </div>
                      </Link>
                    )
                  })
                }
              </div>
            </div>
          </div>
        </div>
      </main>
    </>
  )
}

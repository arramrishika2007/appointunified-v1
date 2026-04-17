'use client'

import { useEffect, useState } from 'react'
import Link from 'next/link'
import { BadgeCheck, BriefcaseBusiness, Loader2, Star, CalendarDays } from 'lucide-react'
import { ProfessionalShell } from '@/components/layout/ProfessionalShell'
import { appointmentsApi, professionalsApi } from '@/lib/api'
import { useAuthStore } from '@/lib/store'
import { AppointmentSummary, ProfessionalDetail } from '@/types'

export default function ProfessionalProfilePage() {
  const { isAuthenticated, user, hasHydrated } = useAuthStore()
  const [loading, setLoading] = useState(true)
  const [profile, setProfile] = useState<ProfessionalDetail | null>(null)
  const [upcomingCount, setUpcomingCount] = useState(0)

  useEffect(() => {
    if (!hasHydrated) return
    if (!isAuthenticated || user?.role !== 'PROFESSIONAL') return

    Promise.all([
      professionalsApi.getMyProfile(),
      appointmentsApi.getMyProfessionalAppointments({ size: 100, sort: 'startTime,asc' }),
    ])
      .then(([profileRes, apptRes]) => {
        const data = profileRes.data.data as ProfessionalDetail
        setProfile(data)
        const appointments = (apptRes.data?.data?.content ?? []) as AppointmentSummary[]
        const now = Date.now()
        setUpcomingCount(
          appointments.filter(
            (appt) =>
              ['SCHEDULED', 'IN_QUEUE', 'IN_PROGRESS'].includes(appt.status) &&
              new Date(appt.startTime).getTime() >= now,
          ).length,
        )
      })
      .finally(() => setLoading(false))
  }, [hasHydrated, isAuthenticated, user?.role])

  if (!hasHydrated || loading) {
    return (
      <ProfessionalShell>
        <main className="min-h-screen bg-slate-50">
          <div className="container-page flex justify-center py-20">
            <Loader2 size={32} className="animate-spin text-brand-600" />
          </div>
        </main>
      </ProfessionalShell>
    )
  }

  return (
    <ProfessionalShell>
      <main className="min-h-screen bg-slate-50 p-6">
        <div className="mx-auto max-w-6xl space-y-6">
          <header className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">Public professional profile</p>
                <h1 className="mt-2 text-2xl font-bold text-slate-900">{profile?.displayName || 'Professional'}</h1>
                <p className="mt-1 text-sm text-slate-600">{profile?.specialty || profile?.sector || 'Profile data loaded from backend'}</p>
              </div>
              <div className="flex items-center gap-2 rounded-full bg-emerald-50 px-3 py-1 text-sm font-semibold text-emerald-700">
                <BadgeCheck size={16} /> {profile?.verificationStatus || 'PENDING'}
              </div>
            </div>
          </header>

          <section className="grid gap-6 lg:grid-cols-[1fr_360px]">
            <div className="space-y-6">
              <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
                <h2 className="mb-4 text-lg font-semibold text-slate-900">Credentials</h2>
                <ul className="space-y-3 text-sm text-slate-700">
                  <li>License Number: {profile?.licenseNumber || '—'}</li>
                  <li>Experience: {profile?.yearsExperience ?? '—'} years</li>
                  <li>Specialization: {profile?.specialty || '—'}</li>
                  <li>Rating: {profile?.ratingAvg?.toFixed(1) || '0.0'} / 5</li>
                </ul>
              </div>

              <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
                <div className="mb-4 flex items-center justify-between gap-3">
                  <h2 className="text-lg font-semibold text-slate-900">Services</h2>
                  <Link href="/dashboard/professional/settings" className="text-sm font-medium text-brand-600 hover:underline">Manage services</Link>
                </div>
                <div className="overflow-x-auto">
                  <table className="min-w-full text-left text-sm">
                    <thead>
                      <tr className="border-b border-slate-200 text-slate-500">
                        <th className="py-2 pr-3">Service</th>
                        <th className="py-2 pr-3">Duration</th>
                        <th className="py-2 pr-3">Fee</th>
                        <th className="py-2">Status</th>
                      </tr>
                    </thead>
                    <tbody className="text-slate-700">
                      {(profile?.services ?? []).length === 0 ? (
                        <tr>
                          <td className="py-3 text-slate-500" colSpan={4}>No services found. Add services from settings.</td>
                        </tr>
                      ) : profile!.services.map((service) => (
                        <tr key={service.id} className="border-b border-slate-100 last:border-b-0">
                          <td className="py-3 pr-3">{service.name}</td>
                          <td className="py-3 pr-3">{service.durationMinutes} mins</td>
                          <td className="py-3 pr-3">{service.price != null ? `Rs. ${service.price}` : '—'}</td>
                          <td className="py-3"><span className={service.isActive ? 'text-emerald-600' : 'text-slate-400'}>{service.isActive ? 'Active' : 'Inactive'}</span></td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>

            <aside className="space-y-6">
              <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
                <h3 className="mb-3 text-lg font-semibold text-slate-900">Live Summary</h3>
                <div className="space-y-3 text-sm text-slate-700">
                  <p className="inline-flex items-center gap-2"><Star size={15} className="text-amber-500" /> Rating: <strong>{profile?.ratingAvg?.toFixed(1) || '0.0'}</strong></p>
                  <p className="inline-flex items-center gap-2"><CalendarDays size={15} className="text-brand-600" /> Upcoming bookings: <strong>{upcomingCount}</strong></p>
                  <p className="inline-flex items-center gap-2"><BriefcaseBusiness size={15} className="text-slate-600" /> Completed: <strong>{profile?.totalCompleted ?? 0}</strong></p>
                </div>
              </div>
              <Link
                href="/dashboard/professional/settings"
                className="inline-flex rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800"
              >
                Open Professional Settings
              </Link>
            </aside>
          </section>
        </div>
      </main>
    </ProfessionalShell>
  )
}

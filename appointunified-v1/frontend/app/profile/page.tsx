'use client'

import { useEffect, useMemo, useState } from 'react'
import { useAuthStore } from '@/lib/store'
import Link from 'next/link'
import { ProfilePictureUploader } from '@/components/profile/ProfilePictureUploader'
import { UserShell } from '@/components/layout/UserShell'
import { appointmentsApi, reviewsApi, usersApi } from '@/lib/api'
import { AppointmentSummary } from '@/types'

type ProfileStats = {
  bookings: number
  completed: number
  noShows: number
  reviews: number
  behaviorScore: number | null
}

export default function UserProfilePage() {
  const { user, updateUser } = useAuthStore()
  const [joinedDate, setJoinedDate] = useState('')
  const [stats, setStats] = useState<ProfileStats>({
    bookings: 0,
    completed: 0,
    noShows: 0,
    reviews: 0,
    behaviorScore: null,
  })

  useEffect(() => {
    let mounted = true

    const loadStats = async () => {
      try {
        const [appointmentsRes, riskRes, reviewsCountRes] = await Promise.all([
          appointmentsApi.getMyAppointments({ page: 0, size: 200 }),
          usersApi.getMyRiskSummary(),
          reviewsApi.getMyReviewsCount(),
        ])

        const appointmentsPage = appointmentsRes.data?.data
        const appointments: AppointmentSummary[] = Array.isArray(appointmentsPage?.content)
          ? appointmentsPage.content
          : []
        const bookings = typeof appointmentsPage?.totalElements === 'number'
          ? appointmentsPage.totalElements
          : appointments.length

        const completed = appointments.filter((item) => item.status === 'COMPLETED').length
        const noShows = appointments.filter((item) => item.status === 'NO_SHOW').length

        const behaviorScore = typeof riskRes.data?.data?.score === 'number'
          ? riskRes.data.data.score
          : null
        const reviews = Number(reviewsCountRes.data?.data ?? 0)

        if (!mounted) return
        setStats({ bookings, completed, noShows, reviews, behaviorScore })
      } catch {
        if (!mounted) return
      }
    }

    void loadStats()
    return () => {
      mounted = false
    }
  }, [])

  useEffect(() => {
    const sourceDate = user && 'createdAt' in user && user.createdAt
      ? new Date(user.createdAt as string)
      : new Date()

    // Avoid SSR/CSR locale mismatches by formatting only after mount.
    setJoinedDate(
      sourceDate.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
      })
    )
  }, [user])

  const behaviorLabel = useMemo(() => {
    const score = stats.behaviorScore
    if (score == null) return 'Unavailable'
    if (score >= 85) return 'Excellent'
    if (score >= 70) return 'Good'
    if (score >= 50) return 'Average'
    return 'Needs Improvement'
  }, [stats.behaviorScore])

  const behaviorLabelClass = useMemo(() => {
    const score = stats.behaviorScore
    if (score == null) return 'text-slate-500'
    if (score >= 85) return 'text-green-600'
    if (score >= 70) return 'text-emerald-600'
    if (score >= 50) return 'text-amber-600'
    return 'text-rose-600'
  }, [stats.behaviorScore])

  return (
    <UserShell>
      <main className="min-h-screen bg-slate-50 p-6">
        <div className="mx-auto grid max-w-5xl gap-6 lg:grid-cols-[320px_1fr]">
        <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <ProfilePictureUploader
            currentAvatarUrl={user?.avatarUrl}
            fullName={user?.fullName || 'User'}
            onUploadSuccess={payload => {
              updateUser({
                ...(user || {}),
                avatarUrl: payload.avatarUrl,
              })
            }}
          />
          <div className="mt-6 text-center">
            <h1 className="text-xl font-semibold text-slate-900">{user?.fullName || 'Guest User'}</h1>
            <p className="text-sm text-slate-500">{user?.phone}</p>
            <p className="mt-2 text-xs text-slate-400">Joined {joinedDate || '...'}</p>
          </div>
        </section>

        <section className="space-y-6">
          <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
            <StatCard label="Bookings" value={String(stats.bookings)} />
            <StatCard label="Completed" value={String(stats.completed)} />
            <StatCard label="No-Shows" value={String(stats.noShows)} />
            <StatCard label="Reviews" value={String(stats.reviews)} />
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <h2 className="mb-2 text-lg font-semibold text-slate-900">Behavior Score</h2>
            <p className="text-sm text-slate-600">Your reliability and punctuality score based on appointment history.</p>
            <div className="mt-4 flex items-end gap-3">
              <span className="text-4xl font-bold text-slate-900">{stats.behaviorScore ?? '--'}</span>
              <span className={`mb-1 text-sm ${behaviorLabelClass}`}>{behaviorLabel}</span>
            </div>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <h2 className="mb-4 text-lg font-semibold text-slate-900">Quick Actions</h2>
            <div className="flex flex-wrap gap-3">
              <Link href="/bookings" className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800">
                View Bookings
              </Link>
              <Link href="/settings" className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100">
                Account Settings
              </Link>
              <Link href="/notifications" className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100">
                Notifications
              </Link>
            </div>
          </div>
        </section>
        </div>
      </main>
    </UserShell>
  )
}

function StatCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-4 text-center shadow-sm">
      <p className="text-xs uppercase tracking-wide text-slate-500">{label}</p>
      <p className="mt-1 text-2xl font-semibold text-slate-900">{value}</p>
    </div>
  )
}

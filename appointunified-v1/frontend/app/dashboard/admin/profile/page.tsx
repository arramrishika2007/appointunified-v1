'use client'

import { useEffect, useState } from 'react'
import Link from 'next/link'
import { AdminShell } from '@/components/layout/AdminShell'
import { api } from '@/lib/api'
import { verificationApi } from '@/lib/api-v2'
import { useAuthStore } from '@/lib/store'
import { Loader2, Shield, BadgeCheck, ClipboardList, Bell } from 'lucide-react'

type MeUser = {
  fullName: string
  email?: string
  phone: string
  role: string
  sector?: string
  avatarUrl?: string
  createdAt?: string
}

export default function AdminProfilePage() {
  const { isAuthenticated, user, hasHydrated } = useAuthStore()
  const [loading, setLoading] = useState(true)
  const [me, setMe] = useState<MeUser | null>(null)
  const [pendingCount, setPendingCount] = useState(0)
  const [recentActions, setRecentActions] = useState(0)

  useEffect(() => {
    if (!hasHydrated) return
    if (!isAuthenticated || !['ADMIN', 'SUPER_ADMIN'].includes(user?.role ?? '')) return

    Promise.all([
      api.get('/users/me'),
      verificationApi.getPending({ page: 0, size: 200 }),
      verificationApi.getVerificationDecisions(200),
    ])
      .then(([meRes, pendingRes, decisionsRes]) => {
        setMe(meRes.data.data)
        setPendingCount(pendingRes.data?.data?.content?.length ?? 0)
        const decisions = decisionsRes.data?.data ?? []
        setRecentActions(decisions.reduce((sum: number, sector: { decisions?: unknown[] }) => sum + (sector.decisions?.length ?? 0), 0))
      })
      .finally(() => setLoading(false))
  }, [hasHydrated, isAuthenticated, user?.role])

  if (!hasHydrated || loading) {
    return (
      <AdminShell>
        <main className="min-h-screen bg-slate-50">
          <div className="container-page flex justify-center py-20">
            <Loader2 size={32} className="animate-spin text-brand-600" />
          </div>
        </main>
      </AdminShell>
    )
  }

  return (
    <AdminShell>
      <main className="min-h-screen bg-slate-50">
        <div className="container-page py-8 max-w-5xl space-y-6">
          <header className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <div className="flex items-start justify-between gap-4">
              <div>
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">Admin identity</p>
                <h1 className="mt-2 text-2xl font-bold text-slate-900">{me?.fullName || 'Admin'}</h1>
                <p className="mt-1 text-sm text-slate-600">{me?.email || me?.phone || 'No account details available'}</p>
              </div>
              <Shield className="text-brand-600" size={24} />
            </div>
          </header>

          <section className="grid gap-6 md:grid-cols-2">
            <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
              <h2 className="mb-3 text-lg font-semibold text-slate-900">Account Details</h2>
              <ul className="space-y-2 text-sm text-slate-700">
                <li>Name: {me?.fullName || '—'}</li>
                <li>Email: {me?.email || '—'}</li>
                <li>Phone: {me?.phone || '—'}</li>
                <li>Role: {me?.role || 'ADMIN'}</li>
                <li>Sector: {me?.sector || '—'}</li>
              </ul>
            </div>

            <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
              <h2 className="mb-3 text-lg font-semibold text-slate-900">Live Activity Summary</h2>
              <ul className="space-y-2 text-sm text-slate-700">
                <li>Pending verifications: {pendingCount}</li>
                <li>Decision records visible: {recentActions}</li>
                <li>Notifications: sync via shared header</li>
              </ul>
            </div>
          </section>

          <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <h2 className="mb-3 text-lg font-semibold text-slate-900">Quick Actions</h2>
            <div className="flex flex-wrap gap-3">
              <Link href="/dashboard/admin/settings" className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800">
                Open Admin Settings
              </Link>
              <Link href="/dashboard/admin/verifications" className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100 inline-flex items-center gap-2">
                <ClipboardList size={15} /> Review Queue
              </Link>
              <Link href="/notifications" className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100 inline-flex items-center gap-2">
                <Bell size={15} /> View Notifications
              </Link>
            </div>
          </div>
        </div>
      </main>
    </AdminShell>
  )
}

'use client'

import { useEffect, useState } from 'react'
import Link from 'next/link'
import { Shield, Loader2, Bell, FileText, Users } from 'lucide-react'
import { SuperAdminShell } from '@/components/layout/SuperAdminShell'
import { api } from '@/lib/api'
import { verificationApi } from '@/lib/api-v2'
import { useAuthStore } from '@/lib/store'

type MeUser = {
  fullName: string
  email?: string
  phone: string
  role: string
  sector?: string
  createdAt?: string
}

export default function SuperAdminProfilePage() {
  const { isAuthenticated, user, hasHydrated } = useAuthStore()
  const [loading, setLoading] = useState(true)
  const [me, setMe] = useState<MeUser | null>(null)
  const [decisionCount, setDecisionCount] = useState(0)

  useEffect(() => {
    if (!hasHydrated) return
    if (!isAuthenticated || user?.role !== 'SUPER_ADMIN') return

    Promise.all([
      api.get('/users/me'),
      verificationApi.getVerificationDecisions(500),
    ])
      .then(([meRes, decisionsRes]) => {
        setMe(meRes.data.data)
        const sectors = decisionsRes.data?.data ?? []
        setDecisionCount(sectors.reduce((sum: number, sector: { decisions?: unknown[] }) => sum + (sector.decisions?.length ?? 0), 0))
      })
      .finally(() => setLoading(false))
  }, [hasHydrated, isAuthenticated, user?.role])

  if (!hasHydrated || loading) {
    return (
      <SuperAdminShell>
        <main className="min-h-screen bg-slate-50">
          <div className="container-page flex justify-center py-20">
            <Loader2 size={32} className="animate-spin text-brand-600" />
          </div>
        </main>
      </SuperAdminShell>
    )
  }

  return (
    <SuperAdminShell>
      <main className="min-h-screen bg-slate-50 p-6">
        <div className="mx-auto max-w-5xl space-y-6">
          <header className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <div className="flex items-start justify-between gap-4">
              <div>
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">Super-admin identity</p>
                <h1 className="mt-2 text-2xl font-bold text-slate-900">{me?.fullName || 'Super Admin'}</h1>
                <p className="mt-1 text-sm text-slate-600">{me?.email || me?.phone || 'Account data loaded from backend'}</p>
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
                <li>Role: {me?.role || 'SUPER_ADMIN'}</li>
              </ul>
            </div>

            <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
              <h2 className="mb-3 text-lg font-semibold text-slate-900">Platform Summary</h2>
              <ul className="space-y-2 text-sm text-slate-700">
                <li>Verification decisions visible: {decisionCount}</li>
                <li>Announcements and notifications are handled via the shared header</li>
                <li>System-wide access enabled</li>
              </ul>
            </div>
          </section>

          <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <h2 className="mb-3 text-lg font-semibold text-slate-900">Quick Actions</h2>
            <div className="flex flex-wrap gap-3">
              <Link href="/super-admin/dashboard" className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800 inline-flex items-center gap-2">
                <Users size={15} /> Open KPI Board
              </Link>
              <Link href="/dashboard/superadmin/analytics" className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100 inline-flex items-center gap-2">
                <FileText size={15} /> Analytics
              </Link>
              <Link href="/notifications" className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100 inline-flex items-center gap-2">
                <Bell size={15} /> Notifications
              </Link>
            </div>
          </div>
        </div>
      </main>
    </SuperAdminShell>
  )
}

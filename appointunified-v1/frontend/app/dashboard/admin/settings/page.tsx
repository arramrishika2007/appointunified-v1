'use client'

import { useState } from 'react'

type Tab = 'profile' | 'notifications' | 'sector'

export default function AdminSettingsPage() {
  const [tab, setTab] = useState<Tab>('profile')

  return (
    <main className="min-h-screen bg-slate-50 p-6">
      <div className="mx-auto max-w-5xl space-y-6">
        <header className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <h1 className="text-2xl font-bold text-slate-900">Admin Settings</h1>
          <p className="mt-1 text-sm text-slate-600">Tune admin profile, alerting, and sector-level platform controls.</p>
        </header>

        <div className="flex gap-2 rounded-2xl border border-slate-200 bg-white p-3 shadow-sm">
          <TabButton id="profile" tab={tab} setTab={setTab} label="Profile" />
          <TabButton id="notifications" tab={tab} setTab={setTab} label="Notifications" />
          <TabButton id="sector" tab={tab} setTab={setTab} label="Sector Config" />
        </div>

        <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          {tab === 'profile' && (
            <Setting title="Profile">
              Update admin identity details and operational ownership metadata.
            </Setting>
          )}
          {tab === 'notifications' && (
            <Setting title="Notifications">
              Configure escalation alerts, moderation queue thresholds, and delivery channels.
            </Setting>
          )}
          {tab === 'sector' && (
            <Setting title="Sector Configuration">
              Manage policy flags and defaults for Healthcare, Government, and Services sectors.
            </Setting>
          )}
        </section>
      </div>
    </main>
  )
}

function TabButton({
  id,
  tab,
  setTab,
  label,
}: {
  id: Tab
  tab: Tab
  setTab: (tab: Tab) => void
  label: string
}) {
  return (
    <button
      onClick={() => setTab(id)}
      className={`rounded-lg px-4 py-2 text-sm font-medium transition ${
        tab === id ? 'bg-slate-900 text-white' : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
      }`}
    >
      {label}
    </button>
  )
}

function Setting({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div>
      <h2 className="text-lg font-semibold text-slate-900">{title}</h2>
      <p className="mt-1 text-sm text-slate-600">{children}</p>
      <div className="mt-4 rounded-lg border border-dashed border-slate-300 bg-slate-50 p-4 text-sm text-slate-500">
        Detailed controls are active and ready for policy binding.
      </div>
    </div>
  )
}

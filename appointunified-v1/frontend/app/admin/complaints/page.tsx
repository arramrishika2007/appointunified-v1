'use client'

import { useEffect, useRef, useState } from 'react'
import { useRouter } from 'next/navigation'
import { format } from 'date-fns'
import { Loader2, MessageSquareWarning } from 'lucide-react'
import { AdminShell } from '@/components/layout/AdminShell'
import { useAuthStore } from '@/lib/store'
import { complaintApi } from '@/lib/api-v2'
import { Complaint, ComplaintPriority } from '@/types/v2'
import { PRIORITY_CONFIG } from '@/lib/utils-v2'
import { cn } from '@/lib/utils'
import toast from 'react-hot-toast'

const STATUS_TABS = ['OPEN', 'INVESTIGATING', 'RESOLVED', 'DISMISSED']

export default function AdminComplaintsPage() {
  const router = useRouter()
  const { isAuthenticated, user } = useAuthStore()
  const [complaints, setComplaints] = useState<Complaint[]>([])
  const [loading, setLoading] = useState(true)
  const [activeTab, setActiveTab] = useState('OPEN')
  const [actionLoading, setActionLoading] = useState<string | null>(null)
  const [resolutionText, setResolutionText] = useState('')
  const [showResolution, setShowResolution] = useState<string | null>(null)
  const [suspendOnResolve, setSuspendOnResolve] = useState(false)
  const refreshTimerRef = useRef<number | null>(null)

  useEffect(() => {
    if (!isAuthenticated || !['ADMIN', 'SUPER_ADMIN'].includes(user?.role ?? '')) {
      router.push('/dashboard')
      return
    }
  }, [isAuthenticated, user, router])

  useEffect(() => {
    const loadComplaints = async (silent = false) => {
      if (!silent) {
        setLoading(true)
      }
      try {
        const res = await complaintApi.list(activeTab)
        setComplaints(res.data.data.content ?? [])
      } catch {
        setComplaints([])
      } finally {
        if (!silent) {
          setLoading(false)
        }
      }
    }

    void loadComplaints(false)

    refreshTimerRef.current = window.setInterval(() => {
      void complaintApi.list(activeTab)
        .then((res) => setComplaints(res.data.data.content ?? []))
        .catch(() => {})
    }, 15000)

    return () => {
      if (refreshTimerRef.current) {
        window.clearInterval(refreshTimerRef.current)
      }
    }
  }, [activeTab])

  const handleResolve = async (id: string) => {
    if (!resolutionText.trim()) {
      toast.error('Enter resolution notes')
      return
    }
    setActionLoading(id)
    try {
      await complaintApi.resolve(id, resolutionText, suspendOnResolve)
      setComplaints((prev) => prev.filter((c) => c.id !== id))
      setShowResolution(null)
      setResolutionText('')
      setSuspendOnResolve(false)
      toast.success(suspendOnResolve ? 'Complaint resolved. Professional suspended.' : 'Complaint resolved.')
    } catch {
      toast.error('Failed to resolve')
    } finally {
      setActionLoading(null)
    }
  }

  const handleDismiss = async (id: string) => {
    const reason = prompt('Dismissal reason:')
    if (!reason) return
    setActionLoading(id)
    try {
      await complaintApi.dismiss(id, reason)
      setComplaints((prev) => prev.filter((c) => c.id !== id))
      toast.success('Complaint dismissed')
    } catch {
      toast.error('Failed to dismiss')
    } finally {
      setActionLoading(null)
    }
  }

  return (
    <AdminShell>
      <main className="min-h-screen bg-slate-50">
        <div className="container-page py-8 max-w-4xl">
          <div className="flex items-center gap-3 mb-6">
            <div className="h-10 w-10 rounded-xl bg-red-100 flex items-center justify-center">
              <MessageSquareWarning size={20} className="text-red-600" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-slate-900">Complaints</h1>
              <p className="text-sm text-slate-500">Trust & Safety complaint management</p>
            </div>
          </div>

          <div className="flex gap-1 bg-slate-100 p-1 rounded-xl mb-6 w-fit">
            {STATUS_TABS.map((tab) => (
              <button
                key={tab}
                onClick={() => setActiveTab(tab)}
                className={cn(
                  'rounded-lg px-4 py-2 text-xs font-semibold transition-all capitalize',
                  activeTab === tab ? 'bg-white shadow-sm text-slate-900' : 'text-slate-500'
                )}
              >
                {tab.toLowerCase()}
              </button>
            ))}
          </div>

          {loading ? (
            <div className="flex justify-center py-16">
              <Loader2 size={28} className="animate-spin text-brand-600" />
            </div>
          ) : complaints.length === 0 ? (
            <div className="card p-12 text-center">
              <p className="text-slate-400">No {activeTab.toLowerCase()} complaints.</p>
            </div>
          ) : (
            <div className="space-y-4">
              {complaints.map((c) => {
                const priorityCfg = PRIORITY_CONFIG[c.priority as ComplaintPriority]
                return (
                  <div key={c.id} className="card p-5">
                    <div className="flex items-start justify-between gap-4 mb-3">
                      <div>
                        <div className="flex items-center gap-2 mb-1 flex-wrap">
                          <span className="font-semibold text-slate-900">{c.category}</span>
                          <span className={cn('badge text-[10px]', priorityCfg.bg, priorityCfg.color)}>{c.priority}</span>
                          <span className="badge bg-slate-100 text-slate-600 text-[10px]">{c.status}</span>
                        </div>
                        <p className="text-sm text-slate-500">
                          Against: <strong className="text-slate-700">{c.professionalName}</strong>
                          &nbsp;· {format(new Date(c.createdAt), 'MMM d, yyyy')}
                        </p>
                      </div>
                    </div>

                    <p className="text-sm text-slate-700 bg-slate-50 rounded-lg p-3 mb-4 leading-relaxed">{c.description}</p>

                    {c.status === 'OPEN' && (
                      <>
                        <div className="flex gap-2">
                          <button onClick={() => setShowResolution(showResolution === c.id ? null : c.id)} className="btn-primary text-xs px-3 py-1.5">
                            Resolve
                          </button>
                          <button onClick={() => handleDismiss(c.id)} disabled={actionLoading === c.id} className="btn-secondary text-xs px-3 py-1.5">
                            {actionLoading === c.id ? <Loader2 size={12} className="animate-spin" /> : null}
                            Dismiss
                          </button>
                          <a href={`/admin/professionals?id=${c.professionalId}`} className="btn-ghost text-xs px-3 py-1.5">
                            View Professional
                          </a>
                        </div>

                        {showResolution === c.id && (
                          <div className="mt-4 rounded-xl bg-amber-50 border border-amber-200 p-4 animate-fade-in space-y-3">
                            <div>
                              <label className="label text-xs">Resolution notes</label>
                              <textarea
                                value={resolutionText}
                                onChange={(event) => setResolutionText(event.target.value)}
                                rows={3}
                                className="input resize-none text-sm"
                                placeholder="Document what action was taken and why…"
                              />
                            </div>
                            <label className="flex items-center gap-2 text-sm text-red-700 cursor-pointer">
                              <input
                                type="checkbox"
                                checked={suspendOnResolve}
                                onChange={(event) => setSuspendOnResolve(event.target.checked)}
                                className="rounded border-red-300 text-red-600"
                              />
                              Also suspend this professional&apos;s account
                            </label>
                            <div className="flex gap-2">
                              <button onClick={() => handleResolve(c.id)} disabled={actionLoading === c.id} className="btn-primary text-xs px-3 py-1.5">
                                {actionLoading === c.id ? <Loader2 size={12} className="animate-spin" /> : null}
                                Confirm Resolution
                              </button>
                              <button onClick={() => setShowResolution(null)} className="btn-secondary text-xs px-3 py-1.5">
                                Cancel
                              </button>
                            </div>
                          </div>
                        )}
                      </>
                    )}

                    {c.resolutionNotes && (
                      <div className="mt-3 rounded-lg bg-emerald-50 border border-emerald-200 p-3">
                        <p className="text-xs font-semibold text-emerald-700 mb-1">Resolution</p>
                        <p className="text-xs text-emerald-700">{c.resolutionNotes}</p>
                      </div>
                    )}
                  </div>
                )
              })}
            </div>
          )}
        </div>
      </main>
    </AdminShell>
  )
}

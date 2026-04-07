'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { format } from 'date-fns'
import { CalendarX, Loader2, Plus, Trash2 } from 'lucide-react'
import { Navbar } from '@/components/layout/Navbar'
import { api } from '@/lib/api'
import { useAuthStore } from '@/lib/store'
import { cn } from '@/lib/utils'
import toast from 'react-hot-toast'

interface ExceptionEntry {
  id: string
  exceptionDate: string
  exceptionType: 'HOLIDAY' | 'PARTIAL' | 'EXTRA_HOURS'
  startTime?: string
  endTime?: string
  reason?: string
}

const TYPE_CONFIG = {
  HOLIDAY:     { label: '🔴 Holiday (Full day off)', color: 'bg-red-50 border-red-200 text-red-700' },
  PARTIAL:     { label: '🟡 Partial (Reduced hours)', color: 'bg-amber-50 border-amber-200 text-amber-700' },
  EXTRA_HOURS: { label: '🟢 Extra Hours (Extended)', color: 'bg-emerald-50 border-emerald-200 text-emerald-700' },
}

export default function ExceptionsPage() {
  const router = useRouter()
  const { isAuthenticated, user } = useAuthStore()
  const [exceptions, setExceptions] = useState<ExceptionEntry[]>([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  const [form, setForm] = useState({
    exceptionDate: '',
    exceptionType: 'HOLIDAY',
    startTime: '',
    endTime: '',
    reason: '',
  })

  useEffect(() => {
    if (!isAuthenticated || user?.role !== 'PROFESSIONAL') {
      router.push('/dashboard'); return
    }
    api.get('/availability/exceptions/me')
      .then(res => setExceptions(res.data.data))
      .finally(() => setLoading(false))
  }, [isAuthenticated, user, router])

  const handleAdd = async () => {
    if (!form.exceptionDate) { toast.error('Date is required'); return }
    setSubmitting(true)
    try {
      const res = await api.post('/availability/exceptions', {
        exceptionDate: form.exceptionDate,
        exceptionType: form.exceptionType,
        startTime:     form.exceptionType !== 'HOLIDAY' ? form.startTime || undefined : undefined,
        endTime:       form.exceptionType !== 'HOLIDAY' ? form.endTime   || undefined : undefined,
        reason:        form.reason || undefined,
      })
      setExceptions(prev => [...prev, res.data.data].sort((a, b) =>
        a.exceptionDate.localeCompare(b.exceptionDate)))
      setForm({ exceptionDate: '', exceptionType: 'HOLIDAY', startTime: '', endTime: '', reason: '' })
      setShowForm(false)
      toast.success('Exception added')
    } catch (err: unknown) {
      toast.error((err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Failed to add exception')
    } finally {
      setSubmitting(false)
    }
  }

  const handleDelete = async (id: string) => {
    if (!confirm('Remove this exception?')) return
    try {
      await api.delete(`/availability/exceptions/${id}`)
      setExceptions(prev => prev.filter(e => e.id !== id))
      toast.success('Exception removed')
    } catch { toast.error('Failed to remove') }
  }

  return (
    <>
      <Navbar />
      <main className="min-h-screen bg-slate-50">
        <div className="container-page py-8 max-w-2xl">

          <div className="flex items-center justify-between mb-6">
            <div>
              <h1 className="text-2xl font-bold text-slate-900 flex items-center gap-2">
                <CalendarX size={22} className="text-brand-600" /> Holiday Calendar
              </h1>
              <p className="text-slate-500 text-sm mt-1">
                Mark dates as holidays, partial days, or extra hours. Clients won&apos;t see slots on holiday dates.
              </p>
            </div>
            <button onClick={() => setShowForm(!showForm)} className="btn-primary">
              <Plus size={16} /> Add Exception
            </button>
          </div>

          {/* Add form */}
          {showForm && (
            <div className="card p-6 mb-6 animate-slide-up">
              <h2 className="font-semibold text-slate-900 mb-4">New Exception</h2>
              <div className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="label">Date <span className="text-red-500">*</span></label>
                    <input
                      type="date"
                      value={form.exceptionDate}
                      min={new Date().toISOString().split('T')[0]}
                      onChange={e => setForm(f => ({ ...f, exceptionDate: e.target.value }))}
                      className="input"
                      aria-label="Exception date"
                      title="Exception date"
                    />
                  </div>
                  <div>
                    <label className="label">Type</label>
                    <select
                      value={form.exceptionType}
                      onChange={e => setForm(f => ({ ...f, exceptionType: e.target.value }))}
                      className="input"
                      aria-label="Exception type"
                      title="Exception type"
                    >
                      <option value="HOLIDAY">Holiday (Full day off)</option>
                      <option value="PARTIAL">Partial (Reduced hours)</option>
                      <option value="EXTRA_HOURS">Extra Hours</option>
                    </select>
                  </div>
                </div>

                {form.exceptionType !== 'HOLIDAY' && (
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="label">Start time</label>
                      <input type="time" value={form.startTime} onChange={e => setForm(f => ({ ...f, startTime: e.target.value }))} className="input" aria-label="Start time" title="Start time" />
                    </div>
                    <div>
                      <label className="label">End time</label>
                      <input type="time" value={form.endTime} onChange={e => setForm(f => ({ ...f, endTime: e.target.value }))} className="input" aria-label="End time" title="End time" />
                    </div>
                  </div>
                )}

                <div>
                  <label className="label">Reason <span className="text-slate-400 font-normal">(shown to clients)</span></label>
                  <input
                    value={form.reason}
                    onChange={e => setForm(f => ({ ...f, reason: e.target.value }))}
                    className="input"
                    placeholder="e.g. National holiday, Conference, Training day"
                    maxLength={255}
                  />
                </div>

                <div className="flex gap-2">
                  <button onClick={handleAdd} disabled={submitting} className="btn-primary">
                    {submitting ? <Loader2 size={15} className="animate-spin" /> : <Plus size={15} />}
                    Add
                  </button>
                  <button onClick={() => setShowForm(false)} className="btn-secondary">Cancel</button>
                </div>
              </div>
            </div>
          )}

          {/* List */}
          {loading ? (
            <div className="flex justify-center py-12">
              <Loader2 size={28} className="animate-spin text-brand-600" />
            </div>
          ) : exceptions.length === 0 ? (
            <div className="card p-10 text-center">
              <CalendarX size={40} className="mx-auto text-slate-300 mb-3" />
              <p className="font-medium text-slate-700 mb-1">No exceptions set</p>
              <p className="text-sm text-slate-400">Add holidays or special schedules to keep your calendar accurate.</p>
            </div>
          ) : (
            <div className="space-y-3">
              {exceptions.map(ex => {
                const cfg = TYPE_CONFIG[ex.exceptionType]
                return (
                  <div key={ex.id} className={cn('card p-4 flex items-center justify-between gap-4 border', cfg.color)}>
                    <div>
                      <div className="flex items-center gap-2 mb-0.5">
                        <span className="font-semibold text-slate-900">
                          {format(new Date(ex.exceptionDate), 'EEEE, MMMM d, yyyy')}
                        </span>
                        <span className={cn('badge text-xs', cfg.color)}>{ex.exceptionType}</span>
                      </div>
                      {ex.exceptionType !== 'HOLIDAY' && ex.startTime && (
                        <p className="text-xs text-slate-500">{ex.startTime} – {ex.endTime}</p>
                      )}
                      {ex.reason && <p className="text-xs text-slate-500 mt-0.5">{ex.reason}</p>}
                    </div>
                    <button onClick={() => handleDelete(ex.id)} className="btn-ghost text-red-500 hover:text-red-700 hover:bg-red-50 flex-shrink-0" aria-label={`Delete exception for ${ex.exceptionDate}`} title={`Delete exception for ${ex.exceptionDate}`}>
                      <Trash2 size={15} />
                    </button>
                  </div>
                )
              })}
            </div>
          )}
        </div>
      </main>
    </>
  )
}

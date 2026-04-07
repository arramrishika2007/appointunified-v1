'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { Clock, Loader2, Plus, Trash2 } from 'lucide-react'
import { Navbar } from '@/components/layout/Navbar'
import { useAuthStore } from '@/lib/store'
import { api } from '@/lib/api'
import { ServiceSummary } from '@/types'
import { cn, formatCurrency } from '@/lib/utils'
import toast from 'react-hot-toast'

export default function ServicesManagementPage() {
  const router = useRouter()
  const { isAuthenticated, user } = useAuthStore()
  const [services, setServices] = useState<ServiceSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [professionalId, setProfessionalId] = useState<string | null>(null)

  const [form, setForm] = useState({
    name: '', description: '', durationMinutes: 30,
    price: '', requiresDocuments: false, virtual: false,
  })

  useEffect(() => {
    if (!isAuthenticated || user?.role !== 'PROFESSIONAL') { router.push('/dashboard'); return }

    // Get own professional ID first
    api.get('/professionals/me').then(res => {
      const pid = res.data.data?.id
      if (!pid) return
      setProfessionalId(pid)
      return api.get(`/services/professional/${pid}`)
    }).then(res => {
      if (res) setServices(res.data.data)
    }).finally(() => setLoading(false))
  }, [isAuthenticated, user, router])

  const handleAdd = async () => {
    if (!form.name) { toast.error('Service name is required'); return }
    setSubmitting(true)
    try {
      const res = await api.post('/services', {
        name: form.name,
        description: form.description || undefined,
        durationMinutes: Number(form.durationMinutes),
        price: form.price ? Number(form.price) : undefined,
        requiresDocuments: form.requiresDocuments,
        virtual: form.virtual,
      })
      setServices(prev => [...prev, res.data.data])
      setForm({ name: '', description: '', durationMinutes: 30, price: '', requiresDocuments: false, virtual: false })
      setShowForm(false)
      toast.success('Service added')
    } catch (err: unknown) {
      toast.error((err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Failed to add service')
    } finally {
      setSubmitting(false)
    }
  }

  const handleToggle = async (id: string, isActive: boolean) => {
    try {
      await api.patch(`/services/${id}`, { active: !isActive })
      setServices(prev => prev.map(s => s.id === id ? { ...s, isActive: !isActive } : s))
      toast.success(isActive ? 'Service deactivated' : 'Service activated')
    } catch { toast.error('Failed to update service') }
  }

  const handleDelete = async (id: string) => {
    if (!confirm('Deactivate this service? Existing bookings won\'t be affected.')) return
    try {
      await api.delete(`/services/${id}`)
      setServices(prev => prev.filter(s => s.id !== id))
      toast.success('Service removed')
    } catch { toast.error('Failed to remove') }
  }

  return (
    <>
      <Navbar />
      <main className="min-h-screen bg-slate-50">
        <div className="container-page py-8 max-w-2xl">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h1 className="text-2xl font-bold text-slate-900">My Services</h1>
              <p className="text-slate-500 text-sm mt-1">Manage what clients can book with you</p>
            </div>
            <button onClick={() => setShowForm(!showForm)} className="btn-primary">
              <Plus size={16} /> Add Service
            </button>
          </div>

          {/* Add form */}
          {showForm && (
            <div className="card p-6 mb-6 animate-slide-up">
              <h2 className="font-semibold text-slate-900 mb-4">New Service</h2>
              <div className="space-y-4">
                <div>
                  <label className="label">Service Name <span className="text-red-500">*</span></label>
                  <input value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                    className="input" placeholder="e.g. General Consultation" />
                </div>
                <div>
                  <label className="label">Description</label>
                  <textarea value={form.description} onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
                    rows={2} className="input resize-none" placeholder="What does this service include?" />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="label">Duration (minutes)</label>
                    <select value={form.durationMinutes} onChange={e => setForm(f => ({ ...f, durationMinutes: Number(e.target.value) }))}
                        className="input" aria-label="Service duration" title="Service duration">
                      {[15, 20, 30, 45, 60, 90, 120].map(v => <option key={v} value={v}>{v} min</option>)}
                    </select>
                  </div>
                  <div>
                    <label className="label">Price (₹) <span className="text-slate-400 font-normal">optional</span></label>
                    <input type="number" value={form.price} onChange={e => setForm(f => ({ ...f, price: e.target.value }))}
                      className="input" placeholder="500" min="0" />
                  </div>
                </div>
                <div className="flex gap-6">
                  <label className="flex items-center gap-2 text-sm text-slate-700 cursor-pointer">
                    <input type="checkbox" checked={form.requiresDocuments}
                      onChange={e => setForm(f => ({ ...f, requiresDocuments: e.target.checked }))}
                      className="rounded border-slate-300 text-brand-600" />
                    Requires documents
                  </label>
                  <label className="flex items-center gap-2 text-sm text-slate-700 cursor-pointer">
                    <input type="checkbox" checked={form.virtual}
                      onChange={e => setForm(f => ({ ...f, virtual: e.target.checked }))}
                      className="rounded border-slate-300 text-brand-600" />
                    Virtual / Online
                  </label>
                </div>
                <div className="flex gap-2 pt-1">
                  <button onClick={handleAdd} disabled={submitting} className="btn-primary">
                    {submitting ? <Loader2 size={15} className="animate-spin" /> : <Plus size={15} />}
                    Add Service
                  </button>
                  <button onClick={() => setShowForm(false)} className="btn-secondary">Cancel</button>
                </div>
              </div>
            </div>
          )}

          {/* List */}
          {loading ? (
            <div className="flex justify-center py-12"><Loader2 size={28} className="animate-spin text-brand-600" /></div>
          ) : services.length === 0 ? (
            <div className="card p-10 text-center">
              <p className="font-medium text-slate-700 mb-1">No services yet</p>
              <p className="text-sm text-slate-400">Add your first service so clients can start booking.</p>
            </div>
          ) : (
            <div className="space-y-3">
              {services.map(s => (
                <div key={s.id} className={cn('card p-5 flex items-center gap-4', !s.isActive && 'opacity-50')}>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-0.5">
                      <p className="font-semibold text-slate-900">{s.name}</p>
                      {s.isVirtual && <span className="badge bg-blue-50 text-blue-600 text-xs">🖥️ Virtual</span>}
                      {s.requiresDocuments && <span className="badge bg-amber-50 text-amber-700 text-xs">📄 Docs</span>}
                    </div>
                    {s.description && <p className="text-sm text-slate-500 truncate">{s.description}</p>}
                    <div className="flex items-center gap-3 mt-1 text-xs text-slate-400">
                      <span className="flex items-center gap-1"><Clock size={11} /> {s.durationMinutes} min</span>
                      <span className="font-medium text-slate-700">{formatCurrency(s.price)}</span>
                    </div>
                  </div>
                  <div className="flex items-center gap-2 flex-shrink-0">
                    <button onClick={() => handleToggle(s.id, s.isActive)}
                      className={cn('text-xs font-semibold px-3 py-1.5 rounded-lg transition-colors',
                        s.isActive ? 'bg-slate-100 hover:bg-slate-200 text-slate-700' : 'bg-emerald-100 hover:bg-emerald-200 text-emerald-700')}>
                      {s.isActive ? 'Deactivate' : 'Activate'}
                    </button>
                    <button onClick={() => handleDelete(s.id)} className="btn-ghost text-red-400 hover:text-red-600 px-2" aria-label={`Delete service ${s.name}`} title={`Delete service ${s.name}`}>
                      <Trash2 size={15} />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </main>
    </>
  )
}

'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { CheckCircle2, Loader2, Shield, XCircle, Search, Clock, Users } from 'lucide-react'
import { format } from 'date-fns'
import { SuperAdminShell } from '@/components/layout/SuperAdminShell'
import { useAuthStore } from '@/lib/store'
import { verificationApi } from '@/lib/api-v2'
import clsx from 'clsx'

const SECTOR_ICONS: Record<string, string> = {
  HEALTHCARE: '🏥',
  GOVERNMENT: '🏛️',
  SERVICES: '⚙️',
}

interface DecisionAuditItem {
  professionalId: string
  professionalName: string
  decision: string
  reason: string
  decidedAt: string
  adminName: string
  adminEmail: string
  specialty: string
  licenseNumber: string
}

interface SectorDecisionSummary {
  sector: string
  approvedCount: number
  rejectedCount: number
  decisions: DecisionAuditItem[]
}

export default function SuperAdminVerificationsPage() {
  const router = useRouter()
  const { isAuthenticated, user, hasHydrated } = useAuthStore()
  const [data, setData] = useState<SectorDecisionSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')

  useEffect(() => {
    if (!hasHydrated) return
    if (!isAuthenticated || user?.role !== 'SUPER_ADMIN') {
      router.push('/dashboard')
      return
    }
    loadData()
  }, [hasHydrated, isAuthenticated, user, router])

  const loadData = () => {
    setLoading(true)
    verificationApi.getVerificationDecisions(500)
      .then(res => setData(res.data.data))
      .catch(err => console.error(err))
      .finally(() => setLoading(false))
  }

  // Flatten and sort by decidedAt purely for the unified table
  const allDecisions = data.flatMap(d => d.decisions).sort((a, b) => new Date(b.decidedAt).getTime() - new Date(a.decidedAt).getTime());

  const filteredDecisions = allDecisions.filter(d => 
    d.professionalName.toLowerCase().includes(search.toLowerCase()) || 
    d.adminName.toLowerCase().includes(search.toLowerCase()) ||
    d.decision.toLowerCase().includes(search.toLowerCase())
  )

  if (!hasHydrated) return null

  return (
    <SuperAdminShell>
      <div className="min-h-screen bg-slate-50">
      
      <main className="container-page py-8 max-w-6xl">
        <div className="flex items-center gap-3 mb-6">
          <div className="h-10 w-10 rounded-xl bg-purple-100 flex items-center justify-center">
            <Shield size={20} className="text-purple-600" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-slate-900">Verification Decisions Audit</h1>
            <p className="text-sm text-slate-500">Super Admin view of all professional approval and rejection actions</p>
          </div>
        </div>

        {/* Stats Row */}
        {!loading && data.length > 0 && (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
             <div className="card p-5">
                <div className="flex items-center justify-between mb-2">
                  <h3 className="text-sm font-medium text-slate-500">Total Decisions</h3>
                  <div className="h-8 w-8 rounded-lg bg-blue-100 flex items-center justify-center text-blue-600"><Users size={16} /></div>
                </div>
                <p className="text-2xl font-bold text-slate-900">{allDecisions.length}</p>
             </div>
             <div className="card p-5">
                <div className="flex items-center justify-between mb-2">
                  <h3 className="text-sm font-medium text-slate-500">Approved</h3>
                  <div className="h-8 w-8 rounded-lg bg-emerald-100 flex items-center justify-center text-emerald-600"><CheckCircle2 size={16} /></div>
                </div>
                <p className="text-2xl font-bold text-slate-900">{allDecisions.filter(d => d.decision === 'APPROVED').length}</p>
             </div>
             <div className="card p-5">
                <div className="flex items-center justify-between mb-2">
                  <h3 className="text-sm font-medium text-slate-500">Rejected</h3>
                  <div className="h-8 w-8 rounded-lg bg-red-100 flex items-center justify-center text-red-600"><XCircle size={16} /></div>
                </div>
                <p className="text-2xl font-bold text-slate-900">{allDecisions.filter(d => d.decision === 'REJECTED').length}</p>
             </div>
          </div>
        )}

        <div className="card overflow-hidden">
          <div className="p-4 border-b border-slate-200 bg-white">
            <div className="relative max-w-sm">
              <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="input py-2 pl-9 text-sm"
                placeholder="Search professional or admin..."
              />
            </div>
          </div>

          {loading ? (
             <div className="p-16 flex justify-center"><Loader2 className="animate-spin text-purple-600" size={32} /></div>
          ) : filteredDecisions.length === 0 ? (
             <div className="p-16 text-center text-slate-400">
               <Shield className="mx-auto mb-3 opacity-20" size={48} />
               <p>No verification decisions found in the audit log.</p>
             </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm whitespace-nowrap">
                <thead className="bg-slate-50 text-slate-500">
                  <tr>
                    <th className="px-5 py-3 font-medium">Professional</th>
                    <th className="px-5 py-3 font-medium">Decision</th>
                    <th className="px-5 py-3 font-medium min-w-[250px]">Reason / Note</th>
                    <th className="px-5 py-3 font-medium">Admin</th>
                    <th className="px-5 py-3 font-medium">Date</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 bg-white">
                  {filteredDecisions.map(d => (
                    <tr key={`${d.professionalId}-${d.decidedAt}`} className="hover:bg-slate-50">
                      <td className="px-5 py-3">
                         <p className="font-medium text-slate-900">{d.professionalName}</p>
                         <p className="text-xs text-slate-500 mt-0.5">{d.specialty} · {d.licenseNumber}</p>
                      </td>
                      <td className="px-5 py-3">
                         {d.decision === 'APPROVED' ? (
                           <span className="badge bg-emerald-100 text-emerald-700 inline-flex items-center gap-1">
                             <CheckCircle2 size={12} /> Approved
                           </span>
                         ) : (
                           <span className="badge bg-red-100 text-red-700 inline-flex items-center gap-1">
                             <XCircle size={12} /> Rejected
                           </span>
                         )}
                      </td>
                      <td className="px-5 py-3 whitespace-normal">
                        <p className="text-slate-600 text-sm max-w-sm" title={d.reason}>{d.reason || '—'}</p>
                      </td>
                      <td className="px-5 py-3">
                        <p className="font-medium text-slate-900">{d.adminName}</p>
                        <p className="text-xs text-slate-400 mt-0.5">{d.adminEmail}</p>
                      </td>
                      <td className="px-5 py-3 text-slate-500">
                         <div className="flex items-center gap-1 text-xs">
                           <Clock size={12} />
                           {format(new Date(d.decidedAt), 'MMM d, yyyy h:mm a')}
                         </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </main>
      </div>
    </SuperAdminShell>
  )
}

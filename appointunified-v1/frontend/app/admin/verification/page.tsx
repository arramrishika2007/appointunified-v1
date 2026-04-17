'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { CheckCircle2, ExternalLink, Loader2, Search, Shield, XCircle, FileText, User } from 'lucide-react'
import { format } from 'date-fns'
import { AdminShell } from '@/components/layout/AdminShell'
import { useAuthStore } from '@/lib/store'
import { verificationApi } from '@/lib/api-v2'
import { PendingDocumentView } from '@/types/v2'
import { DOC_TYPE_CONFIG } from '@/lib/utils-v2'
import toast from 'react-hot-toast'
import clsx from 'clsx'

const SECTOR_ICONS: Record<string, string> = {
  HEALTHCARE: '🏥',
  GOVERNMENT: '🏛️',
  SERVICES: '⚙️',
}

export default function AdminVerificationPage() {
  const router = useRouter()
  const { isAuthenticated, user } = useAuthStore()
  const [docs, setDocs] = useState<PendingDocumentView[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [submitting, setSubmitting] = useState(false)
  
  const [selectedProfId, setSelectedProfId] = useState<string | null>(null)
  const [decisionNote, setDecisionNote] = useState('')

  useEffect(() => {
    if (!isAuthenticated || !['ADMIN', 'SUPER_ADMIN'].includes(user?.role ?? '')) {
      router.push('/dashboard')
      return
    }
    loadPending()
  }, [isAuthenticated, user, router])

  const loadPending = () => {
    setLoading(true)
    verificationApi
      .getPending()
      .then((res) => setDocs(res.data.data.content ?? []))
      .catch(() => setDocs([]))
      .finally(() => setLoading(false))
  }

  const handleDecision = async (decision: 'APPROVE' | 'REJECT') => {
    if (!selectedProfId) return
    if (!decisionNote.trim()) {
      toast.error('Please enter a reason for your decision')
      return
    }
    
    setSubmitting(true)
    try {
      if (decision === 'APPROVE') {
        await verificationApi.approveProfessional(selectedProfId, decisionNote)
        toast.success('Professional approved successfully')
      } else {
        await verificationApi.rejectProfessional(selectedProfId, decisionNote)
        toast.success('Professional rejected successfully')
      }
      setDocs((prev) => prev.filter((d) => d.professionalId !== selectedProfId))
      setSelectedProfId(null)
      setDecisionNote('')
    } catch (err: unknown) {
      toast.error(
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        `Failed to ${decision.toLowerCase()} professional`
      )
    } finally {
      setSubmitting(false)
    }
  }

  const filtered = docs.filter(
    (d) =>
      d.professionalName.toLowerCase().includes(search.toLowerCase()) ||
      d.sector.toLowerCase().includes(search.toLowerCase()) ||
      d.docType.toLowerCase().includes(search.toLowerCase())
  )

  const grouped = filtered.reduce(
    (acc, doc) => {
      if (!acc[doc.professionalId]) {
        acc[doc.professionalId] = { id: doc.professionalId, name: doc.professionalName, sector: doc.sector, docs: [] }
      }
      acc[doc.professionalId].docs.push(doc)
      return acc
    },
    {} as Record<string, { id: string; name: string; sector: string; docs: PendingDocumentView[] }>
  )

  const groupedList = Object.values(grouped)
  const selectedProf = selectedProfId ? grouped[selectedProfId] : null

  return (
    <AdminShell>
      <div className="min-h-screen bg-slate-50 flex flex-col h-[calc(100vh-4rem)]">
      
      {/* Set dynamic height subtracting navbar roughly 72px */}
      <main className="flex-1 flex overflow-hidden lg:flex-row flex-col">
        
        {/* Left Side: List */}
        <div className="w-full lg:w-1/3 xl:w-1/4 bg-white border-r border-slate-200 flex flex-col overflow-hidden h-full">
          <div className="p-4 border-b border-slate-100 flex-shrink-0">
            <h1 className="text-xl font-bold text-slate-900 inline-flex items-center gap-2 w-full">
              <Shield className="text-brand-600" size={20} />
              Review Queue
              <span className="badge bg-amber-100 text-amber-700 text-xs ml-auto">{groupedList.length} pending</span>
            </h1>
            <div className="relative mt-4">
              <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="input py-2 pl-9 text-sm"
                placeholder="Search professionals..."
              />
            </div>
          </div>
          
          <div className="flex-1 overflow-y-auto">
            {loading ? (
              <div className="flex justify-center p-8"><Loader2 size={24} className="animate-spin text-brand-600" /></div>
            ) : groupedList.length === 0 ? (
              <div className="p-8 text-center text-slate-500 text-sm">
                <CheckCircle2 className="mx-auto text-emerald-300 mb-2" size={32} />
                No pending verifications
              </div>
            ) : (
              <ul className="divide-y divide-slate-100">
                {groupedList.map((prof) => (
                  <li 
                    key={prof.id} 
                    onClick={() => { setSelectedProfId(prof.id); setDecisionNote(''); }}
                    className={clsx(
                      "p-4 cursor-pointer hover:bg-slate-50 transition-colors border-l-4",
                      selectedProfId === prof.id ? "bg-slate-50 border-brand-500" : "border-transparent"
                    )}
                  >
                    <div className="flex items-center justify-between">
                      <div className="min-w-0 pr-3">
                        <p className="font-semibold text-slate-900 truncate">{prof.name}</p>
                        <p className="text-xs text-slate-500 flex items-center gap-1 mt-0.5">
                          <span>{SECTOR_ICONS[prof.sector]}</span> {prof.sector}
                        </p>
                      </div>
                      <div className="text-right flex-shrink-0">
                        <span className="text-[10px] font-semibold text-slate-500 bg-slate-100 px-2 py-0.5 rounded">
                          {prof.docs.length} docs
                        </span>
                      </div>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
        
        {/* Right Side: Details & Decision */}
        <div className="flex-1 bg-slate-50 flex flex-col overflow-hidden relative h-full">
          {!selectedProf ? (
            <div className="m-auto text-center text-slate-400">
              <User className="mx-auto mb-3 opacity-30" size={48} />
              <p>Select a professional from the queue to review</p>
            </div>
          ) : (
            <>
              {/* Professional Overview Header */}
              <div className="bg-white p-6 border-b border-slate-200 flex-shrink-0">
                <div className="flex items-start justify-between">
                   <div>
                     <h2 className="text-2xl font-bold text-slate-900">{selectedProf.name}</h2>
                     <p className="text-slate-500 text-sm mt-1 flex items-center gap-2">
                       <span className="text-lg">{SECTOR_ICONS[selectedProf.sector]}</span>
                       {selectedProf.sector} Professional
                     </p>
                   </div>
                </div>
              </div>
              
              {/* Documents Area */}
              <div className="flex-1 overflow-y-auto p-6 space-y-6">
                <h3 className="font-semibold text-slate-800 text-sm flex items-center gap-2">
                  <FileText size={16} className="text-slate-500" />
                  Submitted Documents
                </h3>
                  
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
                  {selectedProf.docs.map(doc => {
                    const typeCfg = DOC_TYPE_CONFIG[doc.docType]
                    const isImage = doc.docUrl.match(/\.(jpg|jpeg|png|gif|webp)$/i)
                    
                    return (
                      <div key={doc.docId} className="card overflow-hidden flex flex-col bg-white shadow-sm border border-slate-200">
                        <div className="p-3 border-b border-slate-100 bg-slate-50">
                          <p className="font-medium text-slate-900 text-sm">{typeCfg?.label ?? doc.docType}</p>
                          <p className="text-[10px] text-slate-500 mt-0.5">
                            Uploaded {format(new Date(doc.submittedAt), 'MMM d, yyyy h:mm a')}
                          </p>
                        </div>
                        <div className="p-3 flex-1 flex flex-col items-center justify-center bg-slate-50/50 min-h-[200px]">
                          {isImage ? (
                            <img src={doc.docUrl} alt={doc.docType} className="max-h-48 object-contain rounded border border-slate-200" />
                          ) : (
                            <div className="text-center">
                              <FileText className="mx-auto mb-2 text-slate-300" size={40} />
                              <a 
                                href={doc.docUrl} 
                                target="_blank" 
                                rel="noreferrer" 
                                className="inline-flex items-center gap-1 text-sm text-brand-600 bg-brand-50 px-3 py-1.5 rounded-lg font-medium hover:bg-brand-100 transition-colors"
                              >
                                <ExternalLink size={14} /> View External Document
                              </a>
                            </div>
                          )}
                        </div>
                        {isImage && (
                          <div className="p-2 border-t border-slate-100 bg-white text-center">
                            <a href={doc.docUrl} target="_blank" rel="noreferrer" className="text-xs text-brand-600 font-medium hover:underline inline-flex items-center gap-1">
                              <ExternalLink size={12} /> Open original image
                            </a>
                          </div>
                        )}
                      </div>
                    )
                  })}
                </div>
              </div>
              
              {/* Decision Panel (Sticky Bottom) */}
              <div className="bg-white border-t border-slate-200 p-6 shadow-[0_-4px_10px_rgba(0,0,0,0.02)] z-10 flex-shrink-0">
                <h3 className="font-semibold text-slate-800 text-sm mb-3">Final Review Decision</h3>
                
                <div className="space-y-4">
                  <div>
                    <label className="label text-xs">Reason / Note <span className="text-red-500">*</span></label>
                    <textarea 
                      value={decisionNote}
                      onChange={e => setDecisionNote(e.target.value)}
                      rows={2}
                      className="input text-sm w-full bg-slate-50"
                      placeholder="Add a required note for approval or rejection (visible to professional)..."
                    />
                  </div>
                  
                  <div className="flex items-center gap-3">
                    <button 
                      onClick={() => handleDecision('APPROVE')} 
                      disabled={submitting || !decisionNote.trim()}
                      className="flex-1 bg-emerald-500 hover:bg-emerald-600 text-white py-2.5 rounded-xl font-semibold shadow-sm transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                    >
                      {submitting ? <Loader2 size={16} className="animate-spin" /> : <CheckCircle2 size={16} />}
                      Approve Professional
                    </button>
                    
                    <button 
                      onClick={() => handleDecision('REJECT')} 
                      disabled={submitting || !decisionNote.trim()}
                      className="flex-1 bg-red-100 hover:bg-red-200 text-red-700 py-2.5 rounded-xl font-semibold transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                    >
                      {submitting ? <Loader2 size={16} className="animate-spin" /> : <XCircle size={16} />}
                      Reject Professional
                    </button>
                  </div>
                </div>
              </div>
            </>
          )}
        </div>
        
      </main>
    </div>
    </AdminShell>
  )
}

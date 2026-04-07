'use client'

import { useEffect, useState } from 'react'
import { usePathname, useRouter } from 'next/navigation'
import { CheckCircle, ExternalLink, FileText, Loader2, Search, ShieldAlert, XCircle } from 'lucide-react'
import { Navbar } from '@/components/layout/Navbar'
import { useAuthStore } from '@/lib/store'
import { api } from '@/lib/api'
import { cn } from '@/lib/utils'
import { format } from 'date-fns'
import toast from 'react-hot-toast'

interface PendingProfessional {
  id: string
  displayName: string
  sector: string
  specialty?: string
  licenseNumber?: string
  verificationStatus: string
  phone: string
  email?: string
  submittedAt: string
}

interface VerificationDocument {
  id: string
  docType: string
  docUrl: string
  mimeType?: string
  status: string
  submittedAt: string
  reviewNotes?: string
  reviewedAt?: string
}

interface VerificationDetail {
  id: string
  displayName: string
  sector: string
  specialty?: string
  licenseNumber?: string
  verificationStatus: string
  phone: string
  email?: string
  submittedAt: string
  documents: VerificationDocument[]
}

function pickPreferredDocumentId(documents: VerificationDocument[]): string | null {
  if (!documents.length) return null
  const idProof = documents.find(doc => {
    const type = (doc.docType || '').toUpperCase()
    return type.includes('ID') || type.includes('PROOF') || type.includes('LICENSE')
  })
  return idProof?.id ?? documents[0].id
}

function isPlaceholderDocumentUrl(url?: string): boolean {
  if (!url) return true
  const normalized = url.toLowerCase()
  return normalized.includes('/image/upload/v1312461204/sample.jpg') || normalized.includes('sample.jpg?mock=')
}

const SECTOR_ICONS: Record<string, string> = {
  HEALTHCARE: 'Med',
  GOVERNMENT: 'Gov',
  SERVICES:   'Svc',
}

const STATUS_COLORS: Record<string, string> = {
  PENDING:   'bg-amber-100 text-amber-700',
  APPROVED:  'bg-emerald-100 text-emerald-700',
  REJECTED:  'bg-red-100 text-red-700',
  SUSPENDED: 'bg-slate-100 text-slate-700',
}

export default function AdminDashboardPage() {
  const router = useRouter()
  const pathname = usePathname()
  const { isAuthenticated, user, hasHydrated } = useAuthStore()
  const [professionals, setProfessionals] = useState<PendingProfessional[]>([])
  const [loading, setLoading] = useState(true)
  const [statusFilter, setStatusFilter] = useState('PENDING')
  const [search, setSearch] = useState('')
  const [actionLoading, setActionLoading] = useState<string | null>(null)
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [detail, setDetail] = useState<VerificationDetail | null>(null)
  const [selectedDocumentId, setSelectedDocumentId] = useState<string | null>(null)

  useEffect(() => {
    if (!hasHydrated) return

    const isSuperAdminRoute = pathname.startsWith('/super-admin')
    const hasAccess = isSuperAdminRoute
      ? user?.role === 'SUPER_ADMIN'
      : ['ADMIN', 'SUPER_ADMIN'].includes(user?.role ?? '')

    if (!isAuthenticated || !hasAccess) {
      router.push('/dashboard')
    }
  }, [hasHydrated, isAuthenticated, user, router, pathname])

  useEffect(() => {
    setLoading(true)
    api.get(`/admin/verifications?status=${statusFilter}&size=50`)
      .then(res => {
        const rows: PendingProfessional[] = res.data.data.content
        setProfessionals(rows)

        if (rows.length === 0) {
          setSelectedId(null)
          setDetail(null)
          setSelectedDocumentId(null)
          return
        }

        const stillVisible = selectedId && rows.some(p => p.id === selectedId)
        const nextSelected = stillVisible ? selectedId : rows[0].id
        setSelectedId(nextSelected)
      })
      .catch(() => setProfessionals([]))
      .finally(() => setLoading(false))
  }, [statusFilter])

  useEffect(() => {
    if (!selectedId) {
      setDetail(null)
      setSelectedDocumentId(null)
      return
    }

    setDetailLoading(true)
    api.get(`/admin/verifications/${selectedId}`)
      .then(res => {
        const payload: VerificationDetail = res.data.data
        setDetail(payload)
        const validDocs = payload.documents.filter(doc => !isPlaceholderDocumentUrl(doc.docUrl))
        setSelectedDocumentId(pickPreferredDocumentId(validDocs))
      })
      .catch(() => {
        setDetail(null)
        setSelectedDocumentId(null)
        toast.error('Failed to load verification details')
      })
      .finally(() => setDetailLoading(false))
  }, [selectedId])

  const handleApprove = async (id: string, name: string) => {
    setActionLoading(id)
    try {
      await api.patch(`/admin/verifications/${id}/approve`)
      setProfessionals(prev => prev.filter(p => p.id !== id))
      if (selectedId === id) {
        setSelectedId(null)
        setDetail(null)
        setSelectedDocumentId(null)
      }
      toast.success(`${name} approved and can now accept bookings`)
    } catch { toast.error('Approval failed') }
    finally { setActionLoading(null) }
  }

  const handleReject = async (id: string, name: string) => {
    const reason = prompt(`Rejection reason for ${name}:`)
    if (!reason) return
    setActionLoading(id)
    try {
      await api.patch(`/admin/verifications/${id}/reject`, { reason })
      setProfessionals(prev => prev.filter(p => p.id !== id))
      if (selectedId === id) {
        setSelectedId(null)
        setDetail(null)
        setSelectedDocumentId(null)
      }
      toast.success(`${name} rejected`)
    } catch { toast.error('Rejection failed') }
    finally { setActionLoading(null) }
  }

  const filtered = professionals.filter(p =>
    p.displayName.toLowerCase().includes(search.toLowerCase()) ||
    p.sector.toLowerCase().includes(search.toLowerCase()) ||
    (p.licenseNumber ?? '').toLowerCase().includes(search.toLowerCase())
  ).sort((a, b) => new Date(b.submittedAt).getTime() - new Date(a.submittedAt).getTime())

  const STATUSES = ['PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED']
  const reviewableDocuments = detail?.documents.filter(doc => !isPlaceholderDocumentUrl(doc.docUrl)) ?? []
  const selectedDocument = reviewableDocuments.find(doc => doc.id === selectedDocumentId)
    ?? reviewableDocuments.find(doc => doc.id === pickPreferredDocumentId(reviewableDocuments))
    ?? reviewableDocuments[0]
  const selectedDocUrl = selectedDocument?.docUrl
  const isPdf = (selectedDocument?.mimeType || '').toLowerCase().includes('pdf') ||
    (selectedDocUrl || '').toLowerCase().includes('.pdf')
  const isImage = (selectedDocument?.mimeType || '').toLowerCase().startsWith('image/') ||
    /\.(png|jpe?g|webp|gif|bmp)$/i.test(selectedDocUrl || '')
  const isSuperAdminRoute = pathname.startsWith('/super-admin')
  const pageTitle = isSuperAdminRoute ? 'Super Admin — Verification Queue' : 'Admin — Verification Queue'
  const pageSubtitle = isSuperAdminRoute
    ? 'Super-admin access to review and approve professional credentials'
    : 'Review and approve professional credentials'

  if (!hasHydrated) {
    return (
      <>
        <Navbar />
        <main className="min-h-screen bg-slate-50">
          <div className="container-page py-16 flex justify-center">
            <Loader2 size={28} className="animate-spin text-brand-600" />
          </div>
        </main>
      </>
    )
  }

  return (
    <>
      <Navbar />
      <main className="min-h-screen bg-slate-50 pt-20 sm:pt-24">
        <div className="container-page py-6 sm:py-8">

          {/* Header */}
          <div className="mb-6 flex flex-wrap items-start gap-3 sm:items-center">
            <div className="mt-0.5 h-10 w-10 shrink-0 rounded-xl bg-red-100 flex items-center justify-center sm:mt-0">
              <ShieldAlert size={20} className="text-red-600" />
            </div>
            <div className="min-w-0 flex-1">
              <h1 className="break-words text-xl font-bold text-slate-900 sm:text-2xl">{pageTitle}</h1>
              <p className="text-sm text-slate-500">{pageSubtitle}</p>
            </div>
          </div>

          {/* Filters */}
          <div className="flex flex-col sm:flex-row gap-3 mb-6">
            {/* Status tabs */}
            <div className="flex gap-1 bg-slate-100 p-1 rounded-xl">
              {STATUSES.map(s => (
                <button
                  key={s}
                  onClick={() => setStatusFilter(s)}
                  className={cn(
                    'rounded-lg px-3 py-1.5 text-xs font-semibold transition-all capitalize',
                    statusFilter === s ? 'bg-white shadow-sm text-slate-900' : 'text-slate-500'
                  )}
                >
                  {s.toLowerCase()}
                </button>
              ))}
            </div>

            {/* Search */}
            <div className="relative flex-1 max-w-sm">
              <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                value={search}
                onChange={e => setSearch(e.target.value)}
                className="input pl-8 text-sm"
                placeholder="Search by name, sector, or license…"
              />
            </div>
          </div>

          {/* Table */}
          {loading ? (
            <div className="flex justify-center py-16">
              <Loader2 size={28} className="animate-spin text-brand-600" />
            </div>
          ) : filtered.length === 0 ? (
            <div className="card p-12 text-center">
              <CheckCircle size={36} className="mx-auto text-slate-300 mb-3" />
              <p className="font-medium text-slate-700">
                {search ? 'No results match your search' : `No ${statusFilter.toLowerCase()} applications`}
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1.1fr)_minmax(0,0.9fr)]">
              <section className="card p-3 sm:p-4">
                <div className="space-y-3">
                  {filtered.map(p => (
                    <button
                      key={p.id}
                      type="button"
                      onClick={() => setSelectedId(p.id)}
                      className={cn(
                        'w-full rounded-xl border p-4 text-left transition-colors',
                        selectedId === p.id
                          ? 'border-brand-500 bg-brand-50/60'
                          : 'border-slate-200 bg-white hover:bg-slate-50'
                      )}
                    >
                      <div className="flex flex-wrap items-start justify-between gap-3">
                        <div>
                          <p className="font-semibold text-slate-900">{p.displayName}</p>
                          <p className="text-xs text-slate-500">{p.specialty || p.sector}</p>
                        </div>
                        <span className={cn('badge text-xs', STATUS_COLORS[p.verificationStatus])}>{p.verificationStatus}</span>
                      </div>
                      <div className="mt-3 grid grid-cols-1 gap-2 text-xs text-slate-600 sm:grid-cols-2">
                        <p><span className="font-medium text-slate-700">Sector:</span> {SECTOR_ICONS[p.sector]} {p.sector}</p>
                        <p><span className="font-medium text-slate-700">License:</span> {p.licenseNumber || '—'}</p>
                        <p><span className="font-medium text-slate-700">Phone:</span> {p.phone}</p>
                        <p><span className="font-medium text-slate-700">Submitted:</span> {format(new Date(p.submittedAt), 'MMM d, yyyy')}</p>
                      </div>
                    </button>
                  ))}
                </div>
              </section>

              <section className="card p-4">
                {detailLoading ? (
                  <div className="flex min-h-[420px] items-center justify-center">
                    <Loader2 size={24} className="animate-spin text-brand-600" />
                  </div>
                ) : !detail ? (
                  <div className="flex min-h-[420px] flex-col items-center justify-center gap-2 text-center">
                    <FileText className="text-slate-300" size={34} />
                    <p className="text-sm text-slate-500">Select a professional to review documents.</p>
                  </div>
                ) : (
                  <div className="space-y-4">
                    <div className="flex flex-wrap items-start justify-between gap-3">
                      <div>
                        <h2 className="text-lg font-semibold text-slate-900">{detail.displayName}</h2>
                        <p className="text-sm text-slate-500">{detail.specialty || detail.sector}</p>
                        {detail.email && <p className="text-xs text-slate-500">{detail.email}</p>}
                      </div>
                      <span className={cn('badge text-xs', STATUS_COLORS[detail.verificationStatus])}>{detail.verificationStatus}</span>
                    </div>

                    <div className="flex flex-wrap gap-2">
                      {detail.verificationStatus === 'PENDING' && (
                        <>
                          <button
                            onClick={() => handleApprove(detail.id, detail.displayName)}
                            disabled={actionLoading === detail.id}
                            className="inline-flex items-center gap-1 rounded-lg bg-emerald-500 px-3 py-1.5 text-xs font-semibold text-white transition-colors hover:bg-emerald-600 disabled:opacity-50"
                          >
                            {actionLoading === detail.id ? <Loader2 size={12} className="animate-spin" /> : <CheckCircle size={12} />}
                            Approve
                          </button>
                          <button
                            onClick={() => handleReject(detail.id, detail.displayName)}
                            disabled={actionLoading === detail.id}
                            className="inline-flex items-center gap-1 rounded-lg bg-red-100 px-3 py-1.5 text-xs font-semibold text-red-700 transition-colors hover:bg-red-200 disabled:opacity-50"
                          >
                            <XCircle size={12} /> Reject
                          </button>
                        </>
                      )}
                    </div>

                    <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                      {reviewableDocuments.map(doc => (
                        <button
                          type="button"
                          key={doc.id}
                          onClick={() => setSelectedDocumentId(doc.id)}
                          className={cn(
                            'rounded-lg border px-3 py-2 text-left text-xs transition-colors',
                            selectedDocument?.id === doc.id
                              ? 'border-brand-500 bg-brand-50/70'
                              : 'border-slate-200 bg-white hover:bg-slate-50'
                          )}
                        >
                          <p className="font-semibold text-slate-800">{doc.docType}</p>
                          <p className="text-slate-500">{format(new Date(doc.submittedAt), 'MMM d, yyyy p')}</p>
                        </button>
                      ))}
                    </div>

                    {selectedDocUrl ? (
                      <div className="overflow-hidden rounded-xl border border-slate-200 bg-white">
                        <div className="flex items-center justify-between border-b border-slate-200 px-3 py-2 text-xs">
                          <span className="font-medium text-slate-700">{selectedDocument?.docType || 'Document preview'}</span>
                          <a
                            href={selectedDocUrl}
                            target="_blank"
                            rel="noreferrer"
                            className="inline-flex items-center gap-1 text-brand-700 hover:text-brand-800"
                          >
                            Open
                            <ExternalLink size={12} />
                          </a>
                        </div>
                        {isPdf ? (
                          <iframe
                            src={selectedDocUrl}
                            className="h-[520px] w-full"
                            title="Verification document preview"
                          />
                        ) : isImage ? (
                          <div className="max-h-[520px] overflow-auto bg-slate-50 p-3">
                            <img
                              src={selectedDocUrl}
                              alt={selectedDocument?.docType || 'Verification document'}
                              className="mx-auto h-auto max-h-[500px] w-auto rounded-lg border border-slate-200 bg-white object-contain"
                            />
                          </div>
                        ) : (
                          <div className="flex h-[240px] items-center justify-center p-4 text-sm text-slate-500">
                            Inline preview unavailable for this file type. Use Open to view it.
                          </div>
                        )}
                      </div>
                    ) : (
                      <p className="text-sm text-slate-500">
                        {detail.documents.length > 0
                          ? 'No valid uploaded document URLs available for preview yet.'
                          : 'No documents submitted for this professional yet.'}
                      </p>
                    )}
                  </div>
                )}
              </section>
            </div>
          )}
        </div>
      </main>
    </>
  )
}

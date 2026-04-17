'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { AlertCircle, CheckCircle2, Clock, FileText, Loader2, Lock, RefreshCw } from 'lucide-react'
import { useAuthStore } from '@/lib/store'
import { verificationApi } from '@/lib/api-v2'
import { VerificationStatus, VerificationDocument, BadgeTier } from '@/types/v2'
import { DOC_TYPE_CONFIG, SECTOR_REQUIRED_DOCS } from '@/lib/utils-v2'
import { DocumentUploader } from '@/components/verification/DocumentUploader'
import { BadgeCard } from '@/components/verification/VerifiedBadge'
import { cn, formatDateShort } from '@/lib/utils'

const DOC_STATUS_CFG = {
  PENDING: { icon: <Clock size={14} />, color: 'text-amber-600', bg: 'bg-amber-50', label: 'Under Review' },
  APPROVED: { icon: <CheckCircle2 size={14} />, color: 'text-emerald-600', bg: 'bg-emerald-50', label: 'Approved' },
  REJECTED: { icon: <AlertCircle size={14} />, color: 'text-red-600', bg: 'bg-red-50', label: 'Rejected' },
}

type DocStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

const normalizeDocStatus = (value: string | undefined): DocStatus | null => {
  const normalized = (value ?? '').trim().toUpperCase()
  if (normalized === 'PENDING' || normalized === 'APPROVED' || normalized === 'REJECTED') {
    return normalized
  }
  return null
}

export default function VerificationStatusPage() {
  const router = useRouter()
  const { isAuthenticated, user, hasHydrated } = useAuthStore()
  const [status, setStatus] = useState<VerificationStatus | null>(null)
  const [loading, setLoading] = useState(true)
  const [activeUpload, setActiveUpload] = useState<string | null>(null)

  useEffect(() => {
    if (!hasHydrated) return
    if (!isAuthenticated || user?.role !== 'PROFESSIONAL') {
      router.push('/professional/dashboard')
      return
    }
    loadStatus()
  }, [hasHydrated, isAuthenticated, user, router])

  const loadStatus = () => {
    setLoading(true)
    verificationApi
      .getStatus()
      .then((res) => setStatus(res.data.data))
      .finally(() => setLoading(false))
  }

  if (!hasHydrated || loading) {
    return (
      <>
        <div className="flex justify-center items-center min-h-[60vh]">
          <Loader2 size={28} className="animate-spin text-brand-600" />
        </div>
      </>
    )
  }

  if (!status) return null

  const sector = (user?.sector ?? 'HEALTHCARE').toUpperCase()
  const requiredDocs = SECTOR_REQUIRED_DOCS[sector] ?? []

  const latestDocByType = new Map<string, VerificationDocument>()
  for (const doc of status.documents) {
    const existing = latestDocByType.get(doc.docType)
    if (!existing) {
      latestDocByType.set(doc.docType, doc)
      continue
    }
    if (new Date(doc.submittedAt).getTime() > new Date(existing.submittedAt).getTime()) {
      latestDocByType.set(doc.docType, doc)
    }
  }

  const effectiveStatusByDocType = new Map<string, DocStatus>()
  latestDocByType.forEach((doc, docType) => {
    const normalized = normalizeDocStatus(doc.status)
    if (normalized) {
      effectiveStatusByDocType.set(docType, normalized)
    }
  })

  const approvedDocTypes = new Set(
    Array.from(effectiveStatusByDocType.entries())
      .filter(([, value]) => value === 'APPROVED')
      .map(([docType]) => docType)
  )
  const pendingDocTypes = new Set(
    Array.from(effectiveStatusByDocType.entries())
      .filter(([, value]) => value === 'PENDING')
      .map(([docType]) => docType)
  )
  const rejectedDocTypes = new Set(
    Array.from(effectiveStatusByDocType.entries())
      .filter(([, value]) => value === 'REJECTED')
      .map(([docType]) => docType)
  )

  const progress =
    requiredDocs.length > 0
      ? Math.round((requiredDocs.filter((dt) => approvedDocTypes.has(dt)).length / requiredDocs.length) * 100)
      : 0

  const statusBannerCfg =
    {
      PENDING: {
        bg: 'bg-amber-50  border-amber-200',
        icon: <Clock size={18} className="text-amber-700" />,
        text: 'Under review — our team will verify within 24–48 hours.',
      },
      APPROVED: {
        bg: 'bg-emerald-50 border-emerald-200',
        icon: <CheckCircle2 size={18} className="text-emerald-700" />,
        text: 'Verified! You can now accept bookings.',
      },
      REJECTED: {
        bg: 'bg-red-50   border-red-200',
        icon: <AlertCircle size={18} className="text-red-700" />,
        text: 'Verification rejected. Review the feedback below and re-submit.',
      },
      SUSPENDED: {
        bg: 'bg-orange-50 border-orange-200',
        icon: <Lock size={18} className="text-orange-700" />,
        text: 'Account suspended. Contact support to resolve.',
      },
    }[status.verificationStatus] ?? { bg: 'bg-slate-50 border-slate-200', icon: <FileText size={18} className="text-slate-700" />, text: '' }

  return (
    <>
      <main className="min-h-screen bg-slate-50">
        <div className="container-page py-8 max-w-3xl">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h1 className="text-2xl font-bold text-slate-900">Verification Centre</h1>
              <p className="text-slate-500 text-sm mt-1">Submit and track your credential verification</p>
            </div>
            <button onClick={loadStatus} className="btn-ghost">
              <RefreshCw size={15} /> Refresh
            </button>
          </div>

          <div className={cn('card p-4 mb-6 border flex items-center gap-3', statusBannerCfg.bg)}>
            <span aria-hidden>{statusBannerCfg.icon}</span>
            <div>
              <p className="font-semibold text-slate-900">{status.verificationStatus}</p>
              <p className="text-sm text-slate-600">{statusBannerCfg.text}</p>
            </div>
          </div>

          <div className="mb-6">
            <BadgeCard tier={status.badgeTier as BadgeTier} />
            {status.verificationExpiresAt && (
              <p className="text-xs text-slate-400 mt-2 ml-1">Verification valid until {formatDateShort(status.verificationExpiresAt)}</p>
            )}
          </div>

          <div className="card p-5 mb-6">
            <div className="flex items-center justify-between mb-2">
              <h2 className="font-semibold text-slate-900">Document Checklist</h2>
              <span className="text-sm font-semibold text-brand-600">{progress}% complete</span>
            </div>
            <progress
              value={progress}
              max={100}
              className="w-full h-2 mb-4 rounded-full overflow-hidden [&::-webkit-progress-bar]:bg-slate-100 [&::-webkit-progress-value]:bg-brand-500 [&::-moz-progress-bar]:bg-brand-500"
            />

            <div className="space-y-3">
              {requiredDocs.map((docType) => {
                const cfg = DOC_TYPE_CONFIG[docType]
                const isApproved = approvedDocTypes.has(docType)
                const isPending = pendingDocTypes.has(docType)
                const isRejected = rejectedDocTypes.has(docType)

                return (
                  <div key={docType}>
                    <div className="flex items-center justify-between mb-1.5">
                      <div className="flex items-center gap-2">
                        <div
                          className={cn(
                            'h-5 w-5 rounded-full flex items-center justify-center text-white text-xs',
                            isApproved ? 'bg-emerald-500' : isPending ? 'bg-amber-400' : isRejected ? 'bg-red-500' : 'bg-slate-300'
                          )}
                        >
                          {isApproved ? 'OK' : isPending ? '...' : isRejected ? 'X' : 'O'}
                        </div>
                        <span className="text-sm font-medium text-slate-900">{cfg.label}</span>
                        {!isApproved && !isPending && (
                          <span className="badge bg-red-100 text-red-600 text-[10px]">Required</span>
                        )}
                      </div>
                      {isPending && <span className="text-xs text-amber-600 font-medium">Under review</span>}
                      {isRejected && <span className="text-xs text-red-600 font-medium">Rejected</span>}
                      {isApproved && <span className="text-xs text-emerald-600 font-medium">Verified</span>}
                    </div>

                    {!isApproved && !isPending && (
                      <div className="ml-7">
                        {activeUpload === docType ? (
                          <DocumentUploader
                            docType={docType}
                            onUploaded={() => {
                              setActiveUpload(null)
                              setTimeout(loadStatus, 1500)
                            }}
                          />
                        ) : (
                          <button
                            onClick={() => setActiveUpload(docType)}
                            className="text-xs text-brand-600 hover:underline font-medium flex items-center gap-1"
                          >
                            <FileText size={12} /> Upload {cfg.label}
                          </button>
                        )}
                      </div>
                    )}
                  </div>
                )
              })}
            </div>
          </div>

          {status.documents.length > 0 && (
            <div className="card p-5">
              <h2 className="font-semibold text-slate-900 mb-4">Submitted Documents</h2>
              <div className="space-y-3">
                {status.documents.map((doc: VerificationDocument) => {
                  const normalizedStatus = normalizeDocStatus(doc.status)
                  const sc = (normalizedStatus ? DOC_STATUS_CFG[normalizedStatus] : DOC_STATUS_CFG.PENDING)
                  const typeCfg = DOC_TYPE_CONFIG[doc.docType]
                  return (
                    <div key={doc.id} className={cn('flex items-start justify-between gap-3 rounded-lg p-3 border', sc.bg)}>
                      <div className="min-w-0">
                        <p className="text-sm font-semibold text-slate-900">{typeCfg?.label ?? doc.docType}</p>
                        <p className="text-xs text-slate-500">Submitted {formatDateShort(doc.submittedAt)}</p>
                        {doc.reviewNotes && <p className="text-xs text-slate-600 mt-1 italic">Note: {doc.reviewNotes}</p>}
                      </div>
                      <div className={cn('flex items-center gap-1 text-xs font-semibold flex-shrink-0', sc.color)}>
                        {sc.icon} {sc.label}
                      </div>
                    </div>
                  )
                })}
              </div>
            </div>
          )}

          <div className="card p-5 mt-5">
            <h2 className="font-semibold text-slate-900 mb-1 flex items-center gap-2">
              <FileText size={16} className="text-brand-600" /> Upload Additional Documents
            </h2>
            <p className="text-xs text-slate-500 mb-4">You can upload any extra supporting documents here.</p>

            <div className="grid grid-cols-2 gap-2 mb-4">
              {Object.keys(DOC_TYPE_CONFIG).map((dt) => (
                <button
                  key={dt}
                  onClick={() => setActiveUpload(activeUpload === dt ? null : dt)}
                  className={cn(
                    'text-xs rounded-lg border py-1.5 px-2.5 text-left transition-all',
                    activeUpload === dt
                      ? 'border-brand-500 bg-brand-50 text-brand-700'
                      : 'border-slate-200 text-slate-600 hover:border-slate-300'
                  )}
                >
                  {DOC_TYPE_CONFIG[dt].label}
                </button>
              ))}
            </div>

            {activeUpload && !requiredDocs.includes(activeUpload) && (
              <DocumentUploader
                docType={activeUpload}
                onUploaded={() => {
                  setActiveUpload(null)
                  setTimeout(loadStatus, 1500)
                }}
              />
            )}
          </div>
        </div>
      </main>
    </>
  )
}

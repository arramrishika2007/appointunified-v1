'use client'

import { useEffect, useState } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import Link from 'next/link'
import { Calendar, Check, Clock, Copy, Download, ExternalLink, GitBranch, Loader2, RefreshCw, Trash2, Video, X } from 'lucide-react'
import { Navbar } from '@/components/layout/Navbar'
import { appointmentsApi, waitlistApi } from '@/lib/api'
import { useAuthStore } from '@/lib/store'
import { AppointmentSummary, DraftSummary, WaitlistSummary } from '@/types'
import { cn, formatDateTime, formatDuration, STATUS_CONFIG } from '@/lib/utils'
import toast from 'react-hot-toast'

type Tab = 'upcoming' | 'past' | 'drafts' | 'waitlist'

export default function MyBookingsPage() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const { isAuthenticated, user } = useAuthStore()

  const [tab, setTab] = useState<Tab>('upcoming')
  const [appointments, setAppointments] = useState<AppointmentSummary[]>([])
  const [drafts, setDrafts] = useState<DraftSummary[]>([])
  const [waitlistEntries, setWaitlistEntries] = useState<WaitlistSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [shareLoading, setShareLoading] = useState<string | null>(null)
  const [paymentLoading, setPaymentLoading] = useState<string | null>(null)
  const [workflowLoading, setWorkflowLoading] = useState<string | null>(null)

  const justBooked = searchParams.get('booked')

  useEffect(() => {
    if (!isAuthenticated) router.push('/auth/login')
  }, [isAuthenticated, router])

  useEffect(() => {
    setLoading(true)
    Promise.all([
      appointmentsApi.getMyAppointments({ size: 50 }),
      appointmentsApi.getMyDrafts(),
      waitlistApi.getMine(),
    ]).then(([apptRes, draftRes, waitlistRes]) => {
      setAppointments(apptRes.data.data.content)
      setDrafts(draftRes.data.data)
      setWaitlistEntries(waitlistRes.data.data)
    }).finally(() => setLoading(false))
  }, [])

  const handleConfirmDeposit = async (id: string) => {
    setPaymentLoading(id)
    try {
      const res = await appointmentsApi.confirmDeposit(id)
      const updated = res.data.data
      setAppointments((prev) => prev.map((a) => a.id === id ? { ...a, depositStatus: updated.depositStatus } : a))
      toast.success('Deposit marked as paid')
    } catch {
      toast.error('Could not confirm deposit')
    } finally {
      setPaymentLoading(null)
    }
  }

  const handleRemoveWaitlist = async (id: string) => {
    try {
      await waitlistApi.cancel(id)
      setWaitlistEntries((prev) => prev.filter((w) => w.id !== id))
      toast.success('Removed from waitlist')
    } catch {
      toast.error('Could not remove waitlist entry')
    }
  }

  const now = new Date().toISOString()
  const upcomingAppts = appointments.filter((a) =>
    ['SCHEDULED', 'IN_QUEUE', 'IN_PROGRESS'].includes(a.status) && a.startTime > now
  ).sort((a, b) => a.startTime.localeCompare(b.startTime))

  const pastAppts = appointments.filter((a) =>
    !['SCHEDULED', 'IN_QUEUE', 'IN_PROGRESS'].includes(a.status) || a.startTime <= now
  ).sort((a, b) => b.startTime.localeCompare(a.startTime))

  const handleCancel = async (id: string) => {
    if (!confirm('Cancel this appointment?')) return
    try {
      await appointmentsApi.cancel(id, { reason: 'Cancelled by user' })
      setAppointments((prev) => prev.map((a) => a.id === id ? { ...a, status: 'CANCELLED' as const } : a))
      toast.success('Appointment cancelled')
    } catch {
      toast.error('Could not cancel. Please try again.')
    }
  }

  const handleShare = async (id: string) => {
    setShareLoading(id)
    try {
      const res = await appointmentsApi.getShareInfo(id)
      const { shareUrl } = res.data.data
      await navigator.clipboard.writeText(shareUrl)
      toast.success('Share link copied to clipboard!')
    } catch {
      toast.error('Could not get share link.')
    } finally {
      setShareLoading(null)
    }
  }

  const handleIcal = (id: string) => {
    window.open(`${process.env.NEXT_PUBLIC_API_URL}/appointments/${id}/ical`, '_blank')
  }

  const handleDeleteDraft = async (id: string) => {
    try {
      await appointmentsApi.deleteDraft(id)
      setDrafts((prev) => prev.filter((d) => d.id !== id))
      toast.success('Draft deleted')
    } catch {
      toast.error('Failed to delete draft')
    }
  }

  const handleOpenWorkflow = async (appointmentId: string) => {
    setWorkflowLoading(appointmentId)
    try {
      const res = await appointmentsApi.getWorkflowContext(appointmentId)
      const instanceId = res.data.data.instanceId
      router.push(`/bookings/workflow/${instanceId}`)
    } catch {
      toast.error('No workflow context available for this appointment yet')
    } finally {
      setWorkflowLoading(null)
    }
  }

  const handleResumeDraft = (draft: DraftSummary) => {
    if (draft.professionalId) {
      router.push(`/booking/${draft.professionalId}${draft.serviceId ? `?service=${draft.serviceId}` : ''}`)
    }
  }

  const TABS: { id: Tab; label: string; count?: number }[] = [
    { id: 'upcoming', label: 'Upcoming', count: upcomingAppts.length },
    { id: 'past',     label: 'Past' },
    { id: 'drafts',   label: 'Saved Drafts', count: drafts.length },
    { id: 'waitlist', label: 'Waitlist', count: waitlistEntries.length },
  ]

  return (
    <>
      <Navbar />
      <main className="min-h-screen bg-slate-50">
        <div className="container-page py-8 max-w-3xl">

          <div className="flex items-center justify-between mb-6">
            <div>
              <h1 className="text-2xl font-bold text-slate-900">My Bookings</h1>
              <p className="text-slate-500 text-sm mt-1">Manage all your appointments</p>
            </div>
            <Link href="/dashboard/workflows" className="btn-ghost text-xs px-3 py-1.5">
              <GitBranch size={13} /> Workflows
            </Link>
          </div>

          {/* Just booked toast banner */}
          {justBooked && (
            <div className="card p-4 mb-6 border-emerald-300 bg-emerald-50 flex items-center gap-3">
              <div className="h-8 w-8 rounded-full bg-emerald-500 flex items-center justify-center flex-shrink-0">
                <Check size={16} className="text-white" />
              </div>
              <div className="flex-1">
                <p className="text-sm font-semibold text-emerald-800">Appointment confirmed!</p>
                <p className="text-xs text-emerald-600">Check your email for confirmation details.</p>
              </div>
            </div>
          )}

          {/* Tabs */}
          <div className="flex gap-1 mb-6 bg-slate-100 p-1 rounded-xl">
            {TABS.map((t) => (
              <button
                key={t.id}
                onClick={() => setTab(t.id)}
                className={cn(
                  'flex-1 rounded-lg py-2 text-sm font-medium transition-all',
                  tab === t.id ? 'bg-white shadow-sm text-slate-900' : 'text-slate-500 hover:text-slate-700'
                )}
              >
                {t.label}
                {t.count !== undefined && t.count > 0 && (
                  <span className={cn('ml-1.5 rounded-full px-1.5 py-0.5 text-[10px] font-bold',
                    tab === t.id ? 'bg-brand-100 text-brand-700' : 'bg-slate-200 text-slate-500'
                  )}>
                    {t.count}
                  </span>
                )}
              </button>
            ))}
          </div>

          {loading ? (
            <div className="flex justify-center py-16">
              <Loader2 size={28} className="animate-spin text-brand-600" />
            </div>
          ) : (
            <>
              {/* Upcoming */}
              {tab === 'upcoming' && (
                <div className="space-y-4">
                  {upcomingAppts.length === 0 ? (
                    <div className="card p-12 text-center">
                      <Calendar size={40} className="mx-auto text-slate-300 mb-3" />
                      <h3 className="font-semibold text-slate-700 mb-1">No upcoming appointments</h3>
                      <p className="text-sm text-slate-400 mb-4">Browse providers and book your first appointment.</p>
                      <button onClick={() => router.push('/explore/healthcare')} className="btn-primary">
                        Browse Providers
                      </button>
                    </div>
                  ) : (
                    upcomingAppts.map((a) => (
                      <AppointmentCard
                        key={a.id}
                        appt={a}
                        onCancel={handleCancel}
                        onShare={handleShare}
                        onIcal={handleIcal}
                        onConfirmDeposit={handleConfirmDeposit}
                        onOpenWorkflow={handleOpenWorkflow}
                        shareLoading={shareLoading === a.id}
                        paymentLoading={paymentLoading === a.id}
                        workflowLoading={workflowLoading === a.id}
                      />
                    ))
                  )}
                </div>
              )}

              {/* Past */}
              {tab === 'past' && (
                <div className="space-y-4">
                  {pastAppts.length === 0 ? (
                    <div className="card p-12 text-center">
                      <p className="text-slate-400">No past appointments.</p>
                    </div>
                  ) : (
                    pastAppts.map((a) => (
                      <AppointmentCard
                        key={a.id}
                        appt={a}
                        onCancel={handleCancel}
                        onShare={handleShare}
                        onIcal={handleIcal}
                        onConfirmDeposit={handleConfirmDeposit}
                        onOpenWorkflow={handleOpenWorkflow}
                        shareLoading={shareLoading === a.id}
                        paymentLoading={paymentLoading === a.id}
                        workflowLoading={workflowLoading === a.id}
                        isPast
                      />
                    ))
                  )}
                </div>
              )}

              {tab === 'waitlist' && (
                <div className="space-y-4">
                  {waitlistEntries.length === 0 ? (
                    <div className="card p-12 text-center">
                      <p className="text-slate-400">No active waitlist entries.</p>
                    </div>
                  ) : (
                    waitlistEntries.map((entry) => (
                      <div key={entry.id} className="card p-5 flex items-center justify-between gap-4">
                        <div>
                          <p className="font-semibold text-slate-900">{entry.professionalName}</p>
                          <p className="text-sm text-slate-500">{entry.serviceName || 'Any service'}</p>
                          <p className="text-xs text-slate-400 mt-1">
                            {entry.notified ? 'Notified' : 'Waiting'} · Added {new Date(entry.createdAt).toLocaleDateString()}
                          </p>
                        </div>
                        <button
                          onClick={() => handleRemoveWaitlist(entry.id)}
                          className="btn-ghost text-red-500 hover:text-red-700"
                        >
                          <X size={14} /> Remove
                        </button>
                      </div>
                    ))
                  )}
                </div>
              )}

              {/* Drafts — NEW V1 FEATURE 3 */}
              {tab === 'drafts' && (
                <div className="space-y-4">
                  {drafts.length === 0 ? (
                    <div className="card p-12 text-center">
                      <p className="text-slate-400 mb-2">No saved drafts.</p>
                      <p className="text-xs text-slate-400">When you start a booking but don&apos;t complete it, it&apos;s saved here automatically.</p>
                    </div>
                  ) : (
                    drafts.map((draft) => (
                      <div key={draft.id} className="card p-5 flex items-center justify-between gap-4">
                        <div className="min-w-0">
                          <p className="font-semibold text-slate-900 truncate">
                            {draft.professionalName || 'Unknown Provider'}
                          </p>
                          {draft.serviceName && (
                            <p className="text-sm text-slate-500 truncate">{draft.serviceName}</p>
                          )}
                          <p className="text-xs text-slate-400 mt-1">
                            Step {draft.stepReached} of 4 · Expires {new Date(draft.expiresAt).toLocaleDateString()}
                          </p>
                        </div>
                        <div className="flex items-center gap-2 flex-shrink-0">
                          <button
                            onClick={() => handleResumeDraft(draft)}
                            className="btn-primary px-3 py-1.5 text-xs"
                            aria-label={`Resume draft for ${draft.professionalName || 'provider'}`}
                            title={`Resume draft for ${draft.professionalName || 'provider'}`}
                          >
                            <RefreshCw size={13} /> Resume
                          </button>
                          <button
                            onClick={() => handleDeleteDraft(draft.id)}
                            className="btn-ghost text-red-500 hover:text-red-700 px-2"
                            aria-label={`Delete draft for ${draft.professionalName || 'provider'}`}
                            title={`Delete draft for ${draft.professionalName || 'provider'}`}
                          >
                            <Trash2 size={15} />
                          </button>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              )}
            </>
          )}
        </div>
      </main>
    </>
  )
}

// ─── Appointment Card ────────────────────────────────────────────────────────

interface CardProps {
  appt: AppointmentSummary
  onCancel: (id: string) => void
  onShare: (id: string) => void
  onIcal: (id: string) => void
  onConfirmDeposit: (id: string) => void
  onOpenWorkflow: (id: string) => void
  shareLoading?: boolean
  paymentLoading?: boolean
  workflowLoading?: boolean
  isPast?: boolean
}

function AppointmentCard({ appt, onCancel, onShare, onIcal, onConfirmDeposit, onOpenWorkflow, shareLoading, paymentLoading, workflowLoading, isPast }: CardProps) {
  const statusCfg = STATUS_CONFIG[appt.status]
  const canCancel = !isPast && ['SCHEDULED'].includes(appt.status)
  const canTrackQueue = !isPast && ['IN_QUEUE', 'IN_PROGRESS'].includes(appt.status)
  const canConfirmDeposit = !isPast && appt.status === 'SCHEDULED' && appt.depositStatus !== 'CONFIRMED'
  const canJoinMeeting = !isPast && appt.virtual && !!appt.meetingToken && ['SCHEDULED', 'IN_PROGRESS'].includes(appt.status)

  return (
    <div className="card p-5 animate-fade-in">
      <div className="flex items-start justify-between gap-3 mb-4">
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2 flex-wrap mb-1">
            <h3 className="font-semibold text-slate-900 truncate">{appt.professional.displayName}</h3>
            <span className={cn('badge', statusCfg.bg, statusCfg.color)}>{statusCfg.label}</span>
          </div>
          <p className="text-sm text-slate-500">{appt.service.name}</p>
        </div>
        <div className="text-right flex-shrink-0">
          {appt.service.price != null && (
            <p className="font-bold text-slate-900">₹{appt.service.price.toLocaleString()}</p>
          )}
        </div>
      </div>

      <div className="flex items-center gap-4 text-sm text-slate-600 mb-4">
        <span className="flex items-center gap-1.5">
          <Calendar size={13} className="text-slate-400" />
          {formatDateTime(appt.startTime)}
        </span>
        <span className="flex items-center gap-1.5">
          <Clock size={13} className="text-slate-400" />
          {formatDuration(appt.service.durationMinutes)}
        </span>
        {appt.virtual && <span className="text-brand-600 text-xs font-medium">🖥️ Virtual</span>}
      </div>

      {/* Action buttons */}
      <div className="flex items-center gap-2 flex-wrap">
        {canTrackQueue && (
          <Link
            href={`/queue?professional=${appt.professional.id}&appointment=${appt.id}&name=${encodeURIComponent(appt.professional.displayName)}`}
            className="btn-ghost text-xs px-3 py-1.5"
          >
            <ExternalLink size={13} /> Track Queue
          </Link>
        )}

        {canJoinMeeting && (
          <Link
            href={`/meeting/join?token=${encodeURIComponent(appt.meetingToken as string)}`}
            className="btn-ghost text-xs px-3 py-1.5"
          >
            <Video size={13} /> Join Meeting
          </Link>
        )}

        {canConfirmDeposit && (
          <button
            onClick={() => onConfirmDeposit(appt.id)}
            disabled={paymentLoading}
            className="btn-ghost text-xs px-3 py-1.5"
          >
            {paymentLoading ? <Loader2 size={13} className="animate-spin" /> : <Check size={13} />}
            I Paid Deposit
          </button>
        )}

        {!isPast && (
          <button
            onClick={() => onOpenWorkflow(appt.id)}
            disabled={workflowLoading}
            className="btn-ghost text-xs px-3 py-1.5"
          >
            {workflowLoading ? <Loader2 size={13} className="animate-spin" /> : <GitBranch size={13} />}
            Workflow
          </button>
        )}

        {/* iCal download — NEW V1 FEATURE 4 */}
        <button
          onClick={() => onIcal(appt.id)}
          className="btn-ghost text-xs px-3 py-1.5"
          aria-label={`Add appointment with ${appt.professional.displayName} to calendar`}
          title={`Add appointment with ${appt.professional.displayName} to calendar`}
        >
          <Download size={13} /> Add to Calendar
        </button>

        {/* Share link — NEW V1 FEATURE 4 */}
        <button
          onClick={() => onShare(appt.id)}
          disabled={shareLoading}
          className="btn-ghost text-xs px-3 py-1.5"
          aria-label={`Copy share link for appointment with ${appt.professional.displayName}`}
          title={`Copy share link for appointment with ${appt.professional.displayName}`}
        >
          {shareLoading ? <Loader2 size={13} className="animate-spin" /> : <Copy size={13} />}
          Copy Share Link
        </button>

        {canCancel && (
                          <button
            onClick={() => onCancel(appt.id)}
            className="btn-ghost text-xs px-3 py-1.5 text-red-500 hover:text-red-700 hover:bg-red-50 ml-auto"
                            aria-label={`Cancel appointment with ${appt.professional.displayName}`}
                            title={`Cancel appointment with ${appt.professional.displayName}`}
          >
            <X size={13} /> Cancel
          </button>
        )}
      </div>
    </div>
  )
}

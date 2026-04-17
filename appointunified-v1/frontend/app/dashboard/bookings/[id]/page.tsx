'use client'

import { useEffect, useMemo, useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import Link from 'next/link'
import type { AxiosError } from 'axios'
import {
  ArrowLeft,
  Calendar,
  Check,
  Clock,
  Copy,
  ExternalLink,
  GitBranch,
  Loader2,
  MapPin,
  MessageSquare,
  Monitor,
  ShieldCheck,
  Video,
} from 'lucide-react'
import { UserShell } from '@/components/layout/UserShell'
import { LocationMapModal } from '@/components/booking/LocationMapModal'
import { appointmentsApi } from '@/lib/api'
import { useAuthStore } from '@/lib/store'
import { AppointmentSummary } from '@/types'
import { cn, formatCurrency, formatDateTime, formatDuration, STATUS_CONFIG } from '@/lib/utils'
import toast from 'react-hot-toast'

const DEPOSIT_CONFIRMABLE_STATUSES: AppointmentSummary['status'][] = [
  'PENDING_DEPOSIT',
  'DEPOSIT_PAID',
  'CONFIRMED',
  'SCHEDULED',
]

export default function BookingDetailPage() {
  const params = useParams()
  const router = useRouter()
  const { isAuthenticated, hasHydrated } = useAuthStore()
  const appointmentId = params.id as string

  const [loading, setLoading] = useState(true)
  const [appointment, setAppointment] = useState<AppointmentSummary | null>(null)
  const [shareLoading, setShareLoading] = useState(false)
  const [paymentLoading, setPaymentLoading] = useState(false)
  const [workflowLoading, setWorkflowLoading] = useState(false)
  const [mapOpen, setMapOpen] = useState(false)

  useEffect(() => {
    if (!hasHydrated) return
    if (!isAuthenticated) {
      router.push(`/auth/login?redirect=/dashboard/bookings/${appointmentId}`)
    }
  }, [appointmentId, hasHydrated, isAuthenticated, router])

  useEffect(() => {
    if (!isAuthenticated) return

    appointmentsApi.getMyAppointments({ size: 100 })
      .then((response) => {
        const items = response.data.data.content ?? []
        const match = items.find((item: AppointmentSummary) => item.id === appointmentId) ?? null
        setAppointment(match)
      })
      .catch(() => setAppointment(null))
      .finally(() => setLoading(false))
  }, [appointmentId, isAuthenticated])

  const statusCfg = appointment ? STATUS_CONFIG[appointment.status] : null
  const isUpcoming = useMemo(() => {
    if (!appointment) return false
    return ['SCHEDULED', 'IN_QUEUE', 'IN_PROGRESS'].includes(appointment.status) && appointment.startTime > new Date().toISOString()
  }, [appointment])

  const handleCopyShareLink = async () => {
    if (!appointment) return
    setShareLoading(true)
    try {
      const response = await appointmentsApi.getShareInfo(appointment.id)
      await navigator.clipboard.writeText(response.data.data.shareUrl)
      toast.success('Share link copied')
    } catch {
      toast.error('Could not copy share link')
    } finally {
      setShareLoading(false)
    }
  }

  const handleConfirmDeposit = async () => {
    if (!appointment) return
    setPaymentLoading(true)
    try {
      const response = await appointmentsApi.confirmDeposit(appointment.id)
      const updated = response.data.data
      setAppointment((prev) => prev ? { ...prev, depositStatus: updated.depositStatus } : prev)
      toast.success('Deposit marked as paid')
    } catch {
      toast.error('Could not confirm deposit')
    } finally {
      setPaymentLoading(false)
    }
  }

  const handleOpenWorkflow = async () => {
    if (!appointment) return
    setWorkflowLoading(true)
    try {
      const response = await appointmentsApi.getWorkflowContext(appointment.id)
      const context = response.data.data
      if (context?.instanceId) {
        router.push(`/bookings/workflow/${context.instanceId}`)
      } else {
        toast('This appointment is not linked to a workflow yet.')
        router.push('/dashboard/workflows')
      }
    } catch (error) {
      const status = (error as AxiosError | undefined)?.response?.status
      if (status === 404) {
        toast('This appointment is not linked to a workflow yet.')
        router.push('/dashboard/workflows')
      } else {
        toast.error('No workflow context available yet')
      }
    } finally {
      setWorkflowLoading(false)
    }
  }

  if (loading || !hasHydrated) {
    return (
      <UserShell>
        <main className="min-h-screen bg-slate-50">
          <div className="container-page flex justify-center py-20">
            <Loader2 size={32} className="animate-spin text-brand-600" />
          </div>
        </main>
      </UserShell>
    )
  }

  if (!appointment) {
    return (
      <UserShell>
        <main className="min-h-screen bg-slate-50">
          <div className="container-page py-10 max-w-3xl">
            <button onClick={() => router.push('/dashboard/bookings')} className="btn-ghost mb-5 -ml-2 text-slate-500 hover:text-slate-900">
              <ArrowLeft size={16} /> Back to bookings
            </button>

            <div className="card p-8 text-center">
              <p className="text-lg font-semibold text-slate-900">Appointment not found</p>
              <p className="mt-2 text-sm text-slate-500">The booking may have been deleted or you may not have access to it.</p>
            </div>
          </div>
        </main>
      </UserShell>
    )
  }

  const isVirtual = appointment.virtual || Boolean(appointment.meetLink)
  const canOpenMap = !isVirtual && appointment.clientLat != null && appointment.clientLon != null

  return (
    <UserShell>
      <main className="min-h-screen bg-slate-50">
        <div className="container-page py-8 max-w-5xl space-y-6">
          <button onClick={() => router.push('/dashboard/bookings')} className="btn-ghost -ml-2 text-slate-500 hover:text-slate-900">
            <ArrowLeft size={16} /> Back to bookings
          </button>

          <section className="grid gap-6 lg:grid-cols-[1.6fr_1fr]">
            <div className="space-y-6">
              <div className="card overflow-hidden border-slate-200 shadow-sm">
                <div className="bg-gradient-to-r from-slate-900 via-slate-800 to-brand-700 px-6 py-6 text-white">
                  <div className="flex flex-wrap items-center gap-3">
                    {statusCfg && (
                      <span className={cn('rounded-full px-3 py-1 text-xs font-semibold', statusCfg.bg, statusCfg.color)}>
                        {statusCfg.label}
                      </span>
                    )}
                    <span className={cn('rounded-full px-3 py-1 text-xs font-semibold', isVirtual ? 'bg-white/15 text-white' : 'bg-white/15 text-white')}>
                      {isVirtual ? 'Online' : 'Offline'} booking
                    </span>
                    <span className="rounded-full bg-white/15 px-3 py-1 text-xs font-semibold text-white">
                      {appointment.priority}
                    </span>
                  </div>
                  <h1 className="mt-4 text-3xl font-bold">{appointment.professional.displayName}</h1>
                  <p className="mt-1 text-sm text-white/80">{appointment.service.name}</p>
                </div>

                <div className="grid gap-4 p-6 sm:grid-cols-2">
                  <DetailRow icon={<Calendar size={16} />} label="Date" value={formatDateTime(appointment.startTime)} />
                  <DetailRow icon={<Clock size={16} />} label="Duration" value={formatDuration(appointment.service.durationMinutes)} />
                  <DetailRow icon={<ShieldCheck size={16} />} label="Deposit" value={appointment.depositStatus ?? 'PENDING'} />
                  <DetailRow icon={<ShieldCheck size={16} />} label="Final payment" value={appointment.finalPaymentStatus ?? 'PENDING'} />
                </div>
              </div>

              <div className="card p-6">
                <h2 className="text-lg font-semibold text-slate-900">Booking timeline</h2>
                <div className="mt-4 space-y-4">
                  <TimelineItem title="Booked" description={formatDateTime(appointment.createdAt)} active />
                  <TimelineItem title="Appointment scheduled" description={formatDateTime(appointment.startTime)} active={isUpcoming || appointment.status !== 'DRAFT'} />
                  <TimelineItem title="Completed or closed" description={appointment.status === 'COMPLETED' ? 'Appointment completed' : 'Pending outcome'} active={['COMPLETED', 'CANCELLED', 'NO_SHOW', 'EXPIRED'].includes(appointment.status)} />
                </div>
              </div>

              <div className="card p-6">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <h2 className="text-lg font-semibold text-slate-900">Actions</h2>
                  <p className="text-xs text-slate-500">Use the live backend actions instead of a mock checkout.</p>
                </div>

                <div className="mt-4 flex flex-wrap gap-2">
                  {appointment.virtual && appointment.meetLink && (
                    <a href={appointment.meetLink} target="_blank" rel="noreferrer" className="btn-primary text-xs px-3 py-2">
                      <Video size={13} /> Join meeting
                    </a>
                  )}
                  {appointment.meetingToken && (
                    <Link href={`/meeting/join?token=${encodeURIComponent(appointment.meetingToken)}`} className="btn-ghost text-xs px-3 py-2">
                      <Monitor size={13} /> Join room
                    </Link>
                  )}
                  <button onClick={handleCopyShareLink} disabled={shareLoading} className="btn-ghost text-xs px-3 py-2">
                    {shareLoading ? <Loader2 size={13} className="animate-spin" /> : <Copy size={13} />} Share
                  </button>
                  <button onClick={handleOpenWorkflow} disabled={workflowLoading} className="btn-ghost text-xs px-3 py-2">
                    {workflowLoading ? <Loader2 size={13} className="animate-spin" /> : <GitBranch size={13} />} Workflow
                  </button>
                  {canOpenMap && (
                    <button onClick={() => setMapOpen(true)} className="btn-ghost text-xs px-3 py-2">
                      <MapPin size={13} /> View Map
                    </button>
                  )}
                  {isVirtual && DEPOSIT_CONFIRMABLE_STATUSES.includes(appointment.status) && appointment.depositStatus !== 'CONFIRMED' && (
                    <button onClick={handleConfirmDeposit} disabled={paymentLoading} className="btn-ghost text-xs px-3 py-2">
                      {paymentLoading ? <Loader2 size={13} className="animate-spin" /> : <Check size={13} />} I Paid Deposit
                    </button>
                  )}
                  {isVirtual && (
                    <Link href={`/dashboard/bookings/${appointment.id}/payment`} className="btn-ghost text-xs px-3 py-2">
                      <ExternalLink size={13} /> Payment center
                    </Link>
                  )}
                  <Link href={`/dashboard/bookings/${appointment.id}/receipt`} className="btn-ghost text-xs px-3 py-2">
                    <ExternalLink size={13} /> Receipt
                  </Link>
                  <Link href={`/dashboard/support?appointmentId=${appointment.id}`} className="btn-ghost text-xs px-3 py-2">
                    <MessageSquare size={13} /> Support
                  </Link>
                </div>
              </div>
            </div>

            <aside className="space-y-6">
              <div className="card p-6">
                <h2 className="text-lg font-semibold text-slate-900">Cost summary</h2>
                <div className="mt-4 space-y-3 text-sm">
                  <SummaryRow label="Service fee" value={formatCurrency(appointment.service.price)} />
                  <SummaryRow label="Priority fee" value={formatCurrency(appointment.premiumFee)} />
                  <SummaryRow label="Deposit status" value={appointment.depositStatus ?? 'PENDING'} />
                  <SummaryRow label="Final payment status" value={appointment.finalPaymentStatus ?? 'PENDING'} />
                  <div className="border-t border-slate-100 pt-3">
                    <SummaryRow label="Total" value={formatCurrency(appointment.totalAmount ?? appointment.service.price ?? 0)} strong />
                  </div>
                </div>
              </div>

              <div className="card p-6">
                <h2 className="text-lg font-semibold text-slate-900">Appointment details</h2>
                <div className="mt-4 space-y-4 text-sm text-slate-600">
                  <p><span className="font-medium text-slate-900">Provider:</span> {appointment.professional.displayName}</p>
                  <p><span className="font-medium text-slate-900">Service:</span> {appointment.service.name}</p>
                  <p><span className="font-medium text-slate-900">Client:</span> {appointment.client.fullName}</p>
                  <p><span className="font-medium text-slate-900">Mode:</span> {isVirtual ? 'Online' : 'Offline'}</p>
                  {appointment.notes && <p><span className="font-medium text-slate-900">Notes:</span> {appointment.notes}</p>}
                  {appointment.meetLink && (
                    <p className="break-all"><span className="font-medium text-slate-900">Meeting link:</span> {appointment.meetLink}</p>
                  )}
                </div>
              </div>
            </aside>
          </section>

          <LocationMapModal
            open={mapOpen}
            onClose={() => setMapOpen(false)}
            provider={appointment.clientLat != null && appointment.clientLon != null ? { lat: Number(appointment.clientLat), lng: Number(appointment.clientLon) } : null}
            title={`${appointment.professional.displayName} location`}
            subtitle="This map stays inside the app instead of opening Google Maps."
          />
        </div>
      </main>
    </UserShell>
  )
}

function DetailRow({ icon, label, value }: { icon: React.ReactNode; label: string; value: string }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
      <div className="flex items-start gap-3">
        <div className="mt-0.5 rounded-xl bg-white p-2 text-brand-600 shadow-sm">{icon}</div>
        <div>
          <p className="text-xs font-medium uppercase tracking-wide text-slate-400">{label}</p>
          <p className="mt-1 text-sm font-semibold text-slate-900">{value}</p>
        </div>
      </div>
    </div>
  )
}

function SummaryRow({ label, value, strong }: { label: string; value: string; strong?: boolean }) {
  return (
    <div className="flex items-center justify-between gap-4">
      <span className="text-slate-500">{label}</span>
      <span className={strong ? 'text-base font-semibold text-slate-900' : 'font-medium text-slate-900'}>{value}</span>
    </div>
  )
}

function TimelineItem({ title, description, active }: { title: string; description: string; active?: boolean }) {
  return (
    <div className="flex gap-3">
      <div className={cn('mt-1 h-3 w-3 rounded-full', active ? 'bg-emerald-500' : 'bg-slate-300')} />
      <div>
        <p className="text-sm font-semibold text-slate-900">{title}</p>
        <p className="text-sm text-slate-500">{description}</p>
      </div>
    </div>
  )
}
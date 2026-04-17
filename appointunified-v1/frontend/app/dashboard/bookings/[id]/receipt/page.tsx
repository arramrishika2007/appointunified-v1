'use client'

import { useEffect, useMemo, useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import Link from 'next/link'
import { ArrowLeft, Calendar, Download, ExternalLink, Loader2, MapPin, Monitor, Printer, ReceiptText } from 'lucide-react'
import { UserShell } from '@/components/layout/UserShell'
import { appointmentsApi } from '@/lib/api'
import { useAuthStore } from '@/lib/store'
import { AppointmentSummary } from '@/types'
import { cn, formatCurrency, formatDateTime, formatDuration, STATUS_CONFIG } from '@/lib/utils'
import toast from 'react-hot-toast'
import jsPDF from 'jspdf'

export default function BookingReceiptPage() {
  const params = useParams()
  const router = useRouter()
  const { isAuthenticated, hasHydrated } = useAuthStore()
  const appointmentId = params.id as string

  const [loading, setLoading] = useState(true)
  const [appointment, setAppointment] = useState<AppointmentSummary | null>(null)
  const [downloadingForm, setDownloadingForm] = useState(false)

  useEffect(() => {
    if (!hasHydrated) return
    if (!isAuthenticated) {
      router.push(`/auth/login?redirect=/dashboard/bookings/${appointmentId}/receipt`)
    }
  }, [appointmentId, hasHydrated, isAuthenticated, router])

  useEffect(() => {
    if (!isAuthenticated) return

    appointmentsApi.getMyAppointments({ size: 100 })
      .then((response) => {
        const items = response.data.data.content ?? []
        setAppointment(items.find((item: AppointmentSummary) => item.id === appointmentId) ?? null)
      })
      .catch(() => setAppointment(null))
      .finally(() => setLoading(false))
  }, [appointmentId, isAuthenticated])

  const statusCfg = appointment ? STATUS_CONFIG[appointment.status] : null
  const isVirtual = useMemo(() => Boolean(appointment?.virtual || appointment?.meetLink), [appointment])

  const downloadBookingForm = async () => {
    if (!appointment) return
    setDownloadingForm(true)
    try {
      const response = await appointmentsApi.getBookingForm(appointment.id)
      const form = response.data?.data
      const content = form?.content || 'Booking form not available.'
      const doc = new jsPDF({ unit: 'pt', format: 'a4' })
      doc.setFontSize(12)
      doc.text(`Booking Form - ${appointment.id}`, 40, 50)
      doc.setFontSize(10)
      const lines = doc.splitTextToSize(content, 515)
      doc.text(lines, 40, 72)
      doc.save(`booking-form-${appointment.id}.pdf`)
    } catch {
      toast.error('Could not generate booking form right now. Please try again.')
    } finally {
      setDownloadingForm(false)
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
              <p className="text-lg font-semibold text-slate-900">Receipt not found</p>
              <p className="mt-2 text-sm text-slate-500">The appointment may not exist in your account any longer.</p>
            </div>
          </div>
        </main>
      </UserShell>
    )
  }

  return (
    <UserShell>
      <main className="min-h-screen bg-slate-50">
        <div className="container-page py-8 max-w-4xl space-y-6">
          <div className="flex items-center justify-between gap-3">
            <button onClick={() => router.push(`/dashboard/bookings/${appointment.id}`)} className="btn-ghost -ml-2 text-slate-500 hover:text-slate-900">
              <ArrowLeft size={16} /> Back to appointment
            </button>
            <button onClick={() => window.print()} className="btn-ghost text-xs px-3 py-2">
              <Printer size={13} /> Print
            </button>
          </div>

          <section className="card overflow-hidden border-slate-200 bg-white print:shadow-none">
            <div className="bg-slate-900 px-6 py-6 text-white">
              <div className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wide text-white/70">
                <ReceiptText size={14} /> Receipt / invoice
              </div>
              <h1 className="mt-3 text-2xl font-bold">{appointment.professional.displayName}</h1>
              <p className="mt-1 text-sm text-white/75">{appointment.service.name} · {formatDateTime(appointment.startTime)}</p>
            </div>

            <div className="grid gap-6 p-6 md:grid-cols-[1.3fr_0.7fr]">
              <div className="space-y-6">
                <div className="grid gap-4 sm:grid-cols-2">
                  <ReceiptField label="Status" value={statusCfg?.label ?? appointment.status} />
                  <ReceiptField label="Mode" value={isVirtual ? 'Online' : 'Offline'} />
                  <ReceiptField label="Provider" value={appointment.professional.displayName} />
                  <ReceiptField label="Client" value={appointment.client.fullName} />
                  <ReceiptField label="Booking token" value={(appointment.meetingToken || appointment.shareToken || appointment.id).toString().slice(0, 12)} />
                  <ReceiptField label="Distance" value={appointment.distanceMeters != null ? `${(appointment.distanceMeters / 1000).toFixed(2)} km` : 'N/A'} />
                </div>

                <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                  <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500">Charges</h2>
                  <div className="mt-4 space-y-3 text-sm">
                    <LineItem label="Service fee" value={formatCurrency(appointment.service.price)} />
                    <LineItem label="Priority fee" value={formatCurrency(appointment.premiumFee)} />
                    <LineItem label="Deposit status" value={appointment.depositStatus ?? 'PENDING'} />
                    <LineItem label="Final payment status" value={appointment.finalPaymentStatus ?? 'PENDING'} />
                    <div className="border-t border-slate-200 pt-3">
                      <LineItem label="Total" value={formatCurrency(appointment.totalAmount ?? appointment.service.price ?? 0)} strong />
                    </div>
                  </div>
                </div>

                {appointment.notes && (
                  <div className="rounded-2xl border border-slate-200 p-4">
                    <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500">Notes</h2>
                    <p className="mt-2 text-sm text-slate-600">{appointment.notes}</p>
                  </div>
                )}
              </div>

              <aside className="space-y-4">
                <div className="rounded-2xl border border-slate-200 bg-white p-4">
                  <p className="text-xs font-medium uppercase tracking-wide text-slate-400">Calendar</p>
                  <div className="mt-2 flex items-start gap-3">
                    <Calendar size={16} className="mt-0.5 text-brand-600" />
                    <div>
                      <p className="text-sm font-semibold text-slate-900">{formatDateTime(appointment.startTime)}</p>
                      <p className="text-sm text-slate-500">Duration {formatDuration(appointment.service.durationMinutes)}</p>
                    </div>
                  </div>
                </div>

                <div className="rounded-2xl border border-slate-200 bg-white p-4">
                  <p className="text-xs font-medium uppercase tracking-wide text-slate-400">Location</p>
                  <div className="mt-2 flex items-start gap-3">
                    {isVirtual ? <Monitor size={16} className="mt-0.5 text-brand-600" /> : <MapPin size={16} className="mt-0.5 text-brand-600" />}
                    <div>
                      <p className="text-sm font-semibold text-slate-900">{isVirtual ? 'Online consultation' : 'Offline consultation'}</p>
                      <p className="text-sm text-slate-500">{isVirtual ? 'Join the meeting from the appointment detail page.' : 'Travel information is shown in the booking detail page.'}</p>
                    </div>
                  </div>
                </div>

                <div className="rounded-2xl border border-slate-200 bg-white p-4">
                  <p className="text-xs font-medium uppercase tracking-wide text-slate-400">Actions</p>
                  <div className="mt-3 flex flex-col gap-2">
                    <button onClick={downloadBookingForm} disabled={downloadingForm} className="btn-ghost text-xs px-3 py-2">
                      {downloadingForm ? <Loader2 size={13} className="animate-spin" /> : <Download size={13} />} Download AI Booking Form
                    </button>
                    {!isVirtual && appointment.clientLat != null && appointment.clientLon != null && (
                      <button
                        onClick={() => {
                          const q = `${appointment.clientLat},${appointment.clientLon}`
                          window.open(`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(q)}`, '_blank', 'noopener,noreferrer')
                        }}
                        className="btn-ghost text-xs px-3 py-2"
                      >
                        <ExternalLink size={13} /> View Map
                      </button>
                    )}
                    {isVirtual && (
                      <Link href={`/dashboard/bookings/${appointment.id}/payment`} className="btn-ghost text-xs px-3 py-2">
                        <Download size={13} /> Payment center
                      </Link>
                    )}
                    <Link href={`/dashboard/support?appointmentId=${appointment.id}`} className="btn-ghost text-xs px-3 py-2">
                      <Download size={13} /> Support
                    </Link>
                  </div>
                </div>
              </aside>
            </div>
          </section>
        </div>
      </main>
    </UserShell>
  )
}

function ReceiptField({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4">
      <p className="text-xs font-medium uppercase tracking-wide text-slate-400">{label}</p>
      <p className="mt-1 text-sm font-semibold text-slate-900">{value}</p>
    </div>
  )
}

function LineItem({ label, value, strong }: { label: string; value: string; strong?: boolean }) {
  return (
    <div className="flex items-center justify-between gap-4">
      <span className="text-slate-500">{label}</span>
      <span className={strong ? 'text-base font-semibold text-slate-900' : 'font-medium text-slate-900'}>{value}</span>
    </div>
  )
}
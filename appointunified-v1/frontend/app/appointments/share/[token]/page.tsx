'use client'

import { useEffect, useState } from 'react'
import { useParams } from 'next/navigation'
import { Calendar, CalendarDown, CheckCircle2, Clock, Link2, Loader2, MapPin, Monitor } from 'lucide-react'
import Link from 'next/link'
import { appointmentsApi } from '@/lib/api'
import { AppointmentSummary } from '@/types'
import { formatDateTime, formatDuration, formatCurrency, STATUS_CONFIG } from '@/lib/utils'
import { cn } from '@/lib/utils'

export default function SharedAppointmentPage() {
  const params = useParams()
  const token = params.token as string

  const [appt, setAppt] = useState<AppointmentSummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    appointmentsApi.getByShareToken(token)
      .then((res) => setAppt(res.data.data))
      .catch(() => setError('This appointment link is invalid or has expired.'))
      .finally(() => setLoading(false))
  }, [token])

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-50 flex items-center justify-center">
        <Loader2 size={32} className="animate-spin text-brand-600" />
      </div>
    )
  }

  if (error || !appt) {
    return (
      <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
        <div className="card p-8 max-w-md w-full text-center">
          <div className="mb-4 inline-flex h-12 w-12 items-center justify-center rounded-full bg-slate-100">
            <Link2 size={22} className="text-slate-500" />
          </div>
          <h1 className="text-xl font-bold text-slate-900 mb-2">Link expired</h1>
          <p className="text-slate-500 text-sm mb-6">{error}</p>
          <Link href="/" className="btn-primary">Go to AppointUnified</Link>
        </div>
      </div>
    )
  }

  const statusCfg = STATUS_CONFIG[appt.status]

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
      <div className="w-full max-w-md">

        {/* Header */}
        <div className="text-center mb-6">
          <Link href="/" className="inline-flex items-center gap-2 mb-4">
            <div className="h-8 w-8 rounded-lg bg-brand-600 flex items-center justify-center text-white font-bold text-xs">AU</div>
            <span className="font-bold text-slate-900">Appoint<span className="text-brand-600">Unified</span></span>
          </Link>
          <div className="inline-flex h-16 w-16 items-center justify-center rounded-full bg-emerald-100 mb-3">
            <CheckCircle2 size={32} className="text-emerald-600" />
          </div>
          <h1 className="text-2xl font-bold text-slate-900">Appointment Details</h1>
          <p className="text-slate-500 text-sm mt-1">Shared appointment summary</p>
        </div>

        <div className="card p-6 space-y-5">
          {/* Status */}
          <div className="flex items-center justify-center">
            <span className={cn('badge text-sm px-4 py-1.5', statusCfg.bg, statusCfg.color)}>
              {statusCfg.label}
            </span>
          </div>

          {/* Provider */}
          <div className="rounded-xl bg-slate-50 p-4">
            <p className="text-xs font-medium text-slate-400 mb-1">Provider</p>
            <p className="font-semibold text-slate-900">{appt.professional.displayName}</p>
            {appt.professional.specialty && (
              <p className="text-sm text-slate-500">{appt.professional.specialty}</p>
            )}
          </div>

          {/* Details grid */}
          <div className="space-y-3">
            <div className="flex items-center gap-3">
              <div className="h-9 w-9 rounded-lg bg-brand-50 flex items-center justify-center flex-shrink-0">
                <Calendar size={16} className="text-brand-600" />
              </div>
              <div>
                <p className="text-xs text-slate-400">Date & Time</p>
                <p className="text-sm font-semibold text-slate-900">{formatDateTime(appt.startTime)}</p>
              </div>
            </div>

            <div className="flex items-center gap-3">
              <div className="h-9 w-9 rounded-lg bg-purple-50 flex items-center justify-center flex-shrink-0">
                <Clock size={16} className="text-purple-600" />
              </div>
              <div>
                <p className="text-xs text-slate-400">Service · Duration</p>
                <p className="text-sm font-semibold text-slate-900">
                  {appt.service.name} · {formatDuration(appt.service.durationMinutes)}
                </p>
              </div>
            </div>

            {appt.service.price != null && (
              <div className="flex items-center gap-3">
                <div className="h-9 w-9 rounded-lg bg-emerald-50 flex items-center justify-center flex-shrink-0">
                  <span className="text-emerald-600 text-sm font-bold">₹</span>
                </div>
                <div>
                  <p className="text-xs text-slate-400">Fee</p>
                  <p className="text-sm font-semibold text-slate-900">{formatCurrency(appt.service.price)}</p>
                </div>
              </div>
            )}

            {appt.virtual && appt.meetLink && (
              <div className="flex items-center gap-3">
                <div className="h-9 w-9 rounded-lg bg-blue-50 flex items-center justify-center flex-shrink-0">
                  <Monitor size={16} className="text-blue-600" />
                </div>
                <div>
                  <p className="text-xs text-slate-400">Virtual appointment</p>
                  <a href={appt.meetLink} target="_blank" rel="noreferrer" className="text-sm text-brand-600 underline font-medium">
                    Join meeting
                  </a>
                </div>
              </div>
            )}
          </div>

          <div className="border-t border-slate-100 pt-4 flex flex-col gap-2">
            <a
              href={`${process.env.NEXT_PUBLIC_API_URL}/appointments/${appt.id}/ical`}
              className="btn-secondary w-full text-center inline-flex items-center justify-center gap-2"
            >
              <CalendarDown size={15} /> Download to Calendar (.ics)
            </a>
            <Link href="/auth/signup" className="btn-primary w-full text-center">
              Book your own appointment →
            </Link>
          </div>
        </div>

        <p className="text-center text-xs text-slate-400 mt-4">
          Powered by <Link href="/" className="underline">AppointUnified</Link>
        </p>
      </div>
    </div>
  )
}

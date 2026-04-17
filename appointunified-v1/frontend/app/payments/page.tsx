'use client'

import { useEffect, useMemo, useState } from 'react'
import Link from 'next/link'
import { Loader2 } from 'lucide-react'
import { Navbar } from '@/components/layout/Navbar'
import { appointmentsApi } from '@/lib/api'
import { AppointmentSummary } from '@/types'
import { formatCurrency, formatDateTime } from '@/lib/utils'

type PaymentTab = 'PENDING' | 'COMPLETED' | 'BLOCKED'

export default function PaymentsPage() {
  const [loading, setLoading] = useState(true)
  const [appointments, setAppointments] = useState<AppointmentSummary[]>([])
  const [activeTab, setActiveTab] = useState<PaymentTab>('PENDING')

  useEffect(() => {
    appointmentsApi
      .getMyAppointments({ size: 200, sort: 'startTime,desc' })
      .then((res) => setAppointments(res.data.data.content ?? []))
      .finally(() => setLoading(false))
  }, [])

  const rows = useMemo(() => {
    return appointments.filter((a) => {
      const deposit = a.depositStatus ?? 'PENDING'
      const finalPayment = a.finalPaymentStatus ?? 'PENDING'

      if (activeTab === 'BLOCKED') {
        return deposit === 'DISPUTED' || finalPayment === 'DISPUTED'
      }

      if (activeTab === 'COMPLETED') {
        return deposit === 'CONFIRMED' && finalPayment === 'CONFIRMED'
      }

      return !(deposit === 'CONFIRMED' && finalPayment === 'CONFIRMED') && !(deposit === 'DISPUTED' || finalPayment === 'DISPUTED')
    })
  }, [activeTab, appointments])

  return (
    <>
      <Navbar />
      <main className="min-h-screen bg-slate-50">
        <div className="container-page max-w-5xl py-8">
          <h1 className="text-2xl font-bold text-slate-900">Payments</h1>
          <p className="mt-1 text-sm text-slate-500">Track pending dues and completed payments for appointments.</p>

          <div className="mt-6 flex gap-2">
            {(['PENDING', 'COMPLETED', 'BLOCKED'] as PaymentTab[]).map((tab) => (
              <button
                key={tab}
                onClick={() => setActiveTab(tab)}
                className={`rounded-lg px-3 py-2 text-xs font-semibold ${activeTab === tab ? 'bg-slate-900 text-white' : 'bg-white text-slate-600 border border-slate-200'}`}
              >
                {tab}
              </button>
            ))}
          </div>

          {loading ? (
            <div className="flex justify-center py-16">
              <Loader2 size={28} className="animate-spin text-brand-600" />
            </div>
          ) : rows.length === 0 ? (
            <div className="card mt-6 p-8 text-center text-slate-500">No records in this payment state.</div>
          ) : (
            <div className="mt-6 space-y-3">
              {rows.map((a) => {
                const total = a.service.price ?? 0
                const firstHalf = total / 2
                const secondHalf = total / 2

                return (
                  <article key={a.id} className="card p-4">
                    <div className="flex flex-wrap items-start justify-between gap-3">
                      <div>
                        <p className="font-semibold text-slate-900">{a.professional.displayName}</p>
                        <p className="text-xs text-slate-500">{a.service.name} • {formatDateTime(a.startTime)}</p>
                      </div>
                      <div className="text-right">
                        <p className="text-sm font-bold text-slate-900">{formatCurrency(total)}</p>
                        <p className="text-[11px] text-slate-500">#{a.id.slice(0, 8)}</p>
                      </div>
                    </div>

                    <div className="mt-3 grid gap-2 text-xs text-slate-600 sm:grid-cols-3">
                      <p>First half: <span className="font-medium">{formatCurrency(firstHalf)}</span></p>
                      <p>Second half: <span className="font-medium">{formatCurrency(secondHalf)}</span></p>
                      <p>
                        Status: <span className="font-medium">{a.depositStatus ?? 'PENDING'} / {a.finalPaymentStatus ?? 'PENDING'}</span>
                      </p>
                    </div>

                    <div className="mt-3 flex flex-wrap gap-2">
                      <Link href={`/dashboard/bookings/${a.id}/payment`} className="btn-primary px-3 py-1.5 text-xs">
                        Pay Now
                      </Link>
                      <Link href={`/dashboard/bookings/${a.id}/receipt`} className="btn-secondary px-3 py-1.5 text-xs">
                        View Receipt
                      </Link>
                    </div>
                  </article>
                )
              })}
            </div>
          )}
        </div>
      </main>
    </>
  )
}


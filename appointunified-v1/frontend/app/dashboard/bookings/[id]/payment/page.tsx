'use client'

import { useCallback, useEffect, useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import Link from 'next/link'
import { ArrowLeft, Check, Loader2, ShieldCheck, Sparkles, Receipt, RefreshCw } from 'lucide-react'
import { UserShell } from '@/components/layout/UserShell'
import { appointmentsApi, paymentsApi } from '@/lib/api'
import { useAuthStore } from '@/lib/store'
import { AppointmentSummary } from '@/types'
import { formatCurrency, STATUS_CONFIG } from '@/lib/utils'
import toast from 'react-hot-toast'

const DEPOSIT_CONFIRMABLE_STATUSES: AppointmentSummary['status'][] = [
  'PENDING_DEPOSIT',
  'DEPOSIT_PAID',
  'CONFIRMED',
  'SCHEDULED',
]

const BALANCE_PAYABLE_STATUSES: AppointmentSummary['status'][] = [
  'PENDING_BALANCE',
]

declare global {
  interface Window {
    Razorpay?: new (options: Record<string, unknown>) => { open: () => void }
  }
}

const RAZORPAY_CHECKOUT_SCRIPT = 'https://checkout.razorpay.com/v1/checkout.js'

const ensureRazorpayLoaded = async (): Promise<boolean> => {
  if (typeof window === 'undefined') return false
  if (window.Razorpay) return true

  return new Promise((resolve) => {
    const existingScript = document.querySelector(`script[src="${RAZORPAY_CHECKOUT_SCRIPT}"]`) as HTMLScriptElement | null
    if (existingScript) {
      existingScript.addEventListener('load', () => resolve(true), { once: true })
      existingScript.addEventListener('error', () => resolve(false), { once: true })
      return
    }

    const script = document.createElement('script')
    script.src = RAZORPAY_CHECKOUT_SCRIPT
    script.async = true
    script.onload = () => resolve(true)
    script.onerror = () => resolve(false)
    document.body.appendChild(script)
  })
}

export default function BookingPaymentPage() {
  const params = useParams()
  const router = useRouter()
  const { isAuthenticated, hasHydrated } = useAuthStore()
  const appointmentId = params.id as string

  const [loading, setLoading] = useState(true)
  const [appointment, setAppointment] = useState<AppointmentSummary | null>(null)
  const [processingType, setProcessingType] = useState<'DEPOSIT' | 'BALANCE' | null>(null)
  const [refreshingStatus, setRefreshingStatus] = useState(false)

  useEffect(() => {
    if (!hasHydrated) return
    if (!isAuthenticated) {
      router.push(`/auth/login?redirect=/dashboard/bookings/${appointmentId}/payment`)
    }
  }, [appointmentId, hasHydrated, isAuthenticated, router])

  const fetchAppointment = useCallback(async (showSpinner = true): Promise<boolean> => {
    if (!isAuthenticated) return false
    if (showSpinner) setLoading(true)
    try {
      const response = await appointmentsApi.getById(appointmentId)
      setAppointment(response.data.data ?? null)
      return true
    } catch {
      setAppointment(null)
      return false
    } finally {
      if (showSpinner) setLoading(false)
    }
  }, [appointmentId, isAuthenticated])

  useEffect(() => {
    void fetchAppointment(true)
  }, [fetchAppointment])

  useEffect(() => {
    // Warm up checkout script so click-to-open feels instant.
    void ensureRazorpayLoaded()
  }, [])

  useEffect(() => {
    if (!appointment?.id) return
    router.prefetch(`/dashboard/bookings/${appointment.id}`)
    router.prefetch(`/dashboard/bookings/${appointment.id}/receipt`)
    router.prefetch(`/dashboard/support?appointmentId=${appointment.id}`)
  }, [appointment?.id, router])

  const handlePayDeposit = async () => {
    if (!appointment) return
    setProcessingType('DEPOSIT')
    try {
      const scriptLoaded = await ensureRazorpayLoaded()
      if (!scriptLoaded || !window.Razorpay) {
        toast.error('Could not load Razorpay checkout')
        return
      }

      const orderResponse = await paymentsApi.createDepositOrder(appointment.id)
      const orderDetails = orderResponse.data.data

      const checkout = new window.Razorpay({
        key: orderDetails.keyId,
        amount: Math.round(Number(orderDetails.amount) * 100),
        currency: orderDetails.currency || 'INR',
        name: 'ABS Appointments',
        description: `Deposit for ${appointment.service.name}`,
        order_id: orderDetails.gatewayOrderId,
        handler: async (response: Record<string, string>) => {
          try {
            const verifyResponse = await paymentsApi.verifyDeposit({
              paymentOrderId: orderDetails.paymentOrderId,
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
            })

            const verified = verifyResponse.data.data
            setAppointment((prev) => prev ? {
              ...prev,
              depositStatus: 'CONFIRMED',
              status: (verified.appointmentStatus as AppointmentSummary['status']) || prev.status,
            } : prev)
            toast.success('Deposit payment confirmed')
            void fetchAppointment(false)
          } catch {
            toast.error('Payment verification failed')
          }
        },
        modal: {
          ondismiss: () => {
            toast('Payment was cancelled')
          },
        },
        notes: {
          appointmentId: appointment.id,
          paymentType: 'DEPOSIT',
        },
        theme: {
          color: '#0f766e',
        },
      })

      checkout.open()
    } catch {
      toast.error('Could not start payment right now')
    } finally {
      setProcessingType(null)
    }
  }

  const handlePayBalance = async () => {
    if (!appointment) return
    setProcessingType('BALANCE')
    try {
      const scriptLoaded = await ensureRazorpayLoaded()
      if (!scriptLoaded || !window.Razorpay) {
        toast.error('Could not load Razorpay checkout')
        return
      }

      const orderResponse = await paymentsApi.createBalanceOrder(appointment.id)
      const orderDetails = orderResponse.data.data

      const checkout = new window.Razorpay({
        key: orderDetails.keyId,
        amount: Math.round(Number(orderDetails.amount) * 100),
        currency: orderDetails.currency || 'INR',
        name: 'ABS Appointments',
        description: `Final payment for ${appointment.service.name}`,
        order_id: orderDetails.gatewayOrderId,
        handler: async (response: Record<string, string>) => {
          try {
            const verifyResponse = await paymentsApi.verifyBalance({
              paymentOrderId: orderDetails.paymentOrderId,
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
            })

            const verified = verifyResponse.data.data
            setAppointment((prev) => prev ? {
              ...prev,
              finalPaymentStatus: 'CONFIRMED',
              status: (verified.appointmentStatus as AppointmentSummary['status']) || prev.status,
            } : prev)
            toast.success('Final payment confirmed')
            void fetchAppointment(false)
          } catch {
            toast.error('Payment verification failed')
          }
        },
        modal: {
          ondismiss: () => {
            toast('Payment was cancelled')
          },
        },
        notes: {
          appointmentId: appointment.id,
          paymentType: 'BALANCE',
        },
        theme: {
          color: '#0f766e',
        },
      })

      checkout.open()
    } catch {
      toast.error('Could not start payment right now')
    } finally {
      setProcessingType(null)
    }
  }

  const refreshPaymentStatus = async () => {
    setRefreshingStatus(true)
    try {
      const ok = await fetchAppointment(false)
      if (ok) {
        toast.success('Payment status refreshed')
      } else {
        toast.error('Could not refresh payment status')
      }
    } finally {
      setRefreshingStatus(false)
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
              <p className="text-lg font-semibold text-slate-900">Payment record not found</p>
              <p className="mt-2 text-sm text-slate-500">This appointment may no longer be available in your account.</p>
            </div>
          </div>
        </main>
      </UserShell>
    )
  }

  const statusLabel = STATUS_CONFIG[appointment.status]?.label ?? appointment.status
  const depositDone = appointment.depositStatus === 'CONFIRMED'
  const balanceDone = appointment.finalPaymentStatus === 'CONFIRMED'
  const canPayDeposit = !depositDone && DEPOSIT_CONFIRMABLE_STATUSES.includes(appointment.status)
  const canPayBalance = !balanceDone && BALANCE_PAYABLE_STATUSES.includes(appointment.status)
  const paymentCompleted = depositDone && balanceDone

  return (
    <UserShell>
      <main className="min-h-screen bg-slate-50">
        <div className="container-page py-8 max-w-5xl space-y-6">
          <button onClick={() => router.push(`/dashboard/bookings/${appointment.id}`)} className="btn-ghost -ml-2 text-slate-500 hover:text-slate-900">
            <ArrowLeft size={16} /> Back to appointment
          </button>

          <section className="grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
            <div className="card overflow-hidden border-slate-200">
              <div className="bg-gradient-to-r from-emerald-500 to-teal-600 px-6 py-6 text-white">
                <div className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wide text-white/80">
                  <ShieldCheck size={14} /> Payment center
                </div>
                <h1 className="mt-3 text-3xl font-bold">{appointment.professional.displayName}</h1>
                <p className="mt-1 text-sm text-white/85">{appointment.service.name} · {statusLabel}</p>
              </div>

              <div className="space-y-4 p-6">
                <PaymentLine label="Service fee" value={formatCurrency(appointment.service.price)} />
                <PaymentLine label="Priority fee" value={formatCurrency(appointment.premiumFee)} />
                <PaymentLine label="Deposit status" value={appointment.depositStatus ?? 'PENDING'} />
                <PaymentLine label="Final payment status" value={appointment.finalPaymentStatus ?? 'PENDING'} />
                <div className="border-t border-slate-100 pt-4">
                  <PaymentLine label="Total payable" value={formatCurrency(appointment.totalAmount ?? appointment.service.price ?? 0)} strong />
                </div>

                <div className="rounded-2xl border border-emerald-200 bg-emerald-50 p-4">
                  <div className="flex items-start gap-3">
                    <Sparkles size={18} className="mt-0.5 text-emerald-600" />
                    <div>
                      <p className="text-sm font-semibold text-emerald-900">Live payment flow</p>
                      <p className="mt-1 text-sm text-emerald-800">
                        Deposit payment uses Razorpay Checkout and verifies signatures on the backend before updating your booking status.
                      </p>
                    </div>
                  </div>
                </div>

                <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                  <p className="text-sm font-semibold text-slate-900">Payment steps</p>
                  <ol className="mt-3 space-y-3 text-sm">
                    <li className="flex items-center justify-between gap-3">
                      <span className="text-slate-700">1. Pay deposit (booking hold)</span>
                      <span className={depositDone ? 'text-emerald-700 font-semibold' : 'text-slate-500'}>
                        {depositDone ? 'Done' : 'Pending'}
                      </span>
                    </li>
                    <li className="flex items-center justify-between gap-3">
                      <span className="text-slate-700">2. Attend appointment</span>
                      <span className="text-slate-500">{appointment.status === 'COMPLETED' || appointment.status === 'PENDING_BALANCE' || appointment.status === 'PAID_FULL' ? 'Done' : 'Pending'}</span>
                    </li>
                    <li className="flex items-center justify-between gap-3">
                      <span className="text-slate-700">3. Pay final balance</span>
                      <span className={balanceDone ? 'text-emerald-700 font-semibold' : 'text-slate-500'}>
                        {balanceDone ? 'Done' : 'Pending'}
                      </span>
                    </li>
                  </ol>
                </div>

                <div className="flex flex-wrap gap-2">
                  {canPayDeposit && (
                    <button onClick={handlePayDeposit} disabled={processingType !== null} className="btn-primary text-xs px-3 py-2">
                      {processingType === 'DEPOSIT' ? <Loader2 size={13} className="animate-spin" /> : <Check size={13} />} Pay Deposit (Razorpay)
                    </button>
                  )}
                  {canPayBalance && (
                    <button onClick={handlePayBalance} disabled={processingType !== null} className="btn-primary text-xs px-3 py-2">
                      {processingType === 'BALANCE' ? <Loader2 size={13} className="animate-spin" /> : <Check size={13} />} Pay Final Balance (Razorpay)
                    </button>
                  )}
                  <button onClick={refreshPaymentStatus} disabled={refreshingStatus || processingType !== null} className="btn-ghost text-xs px-3 py-2">
                    {refreshingStatus ? <Loader2 size={13} className="animate-spin" /> : <RefreshCw size={13} />} Refresh Status
                  </button>
                  <Link href={`/dashboard/bookings/${appointment.id}/receipt`} className="btn-ghost text-xs px-3 py-2">
                    <Receipt size={13} /> View receipt
                  </Link>
                </div>

                {paymentCompleted && (
                  <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-3 py-2 text-xs font-medium text-emerald-800">
                    All payments are complete for this booking.
                  </div>
                )}
              </div>
            </div>

            <aside className="space-y-6">
              <div className="card p-6">
                <h2 className="text-lg font-semibold text-slate-900">Payment checklist</h2>
                <ul className="mt-4 space-y-3 text-sm text-slate-600">
                  <li>Confirm deposit when the service requires a booking hold.</li>
                  <li>Review the receipt before the appointment starts.</li>
                  <li>Use the appointment detail page for meeting, workflow, and support actions.</li>
                </ul>
              </div>

              <div className="card p-6">
                <h2 className="text-lg font-semibold text-slate-900">Need help?</h2>
                <p className="mt-2 text-sm text-slate-600">Ask the system assistant about charges, refunds, or booking rules.</p>
                <Link href={`/dashboard/support?appointmentId=${appointment.id}`} className="btn-ghost mt-4 w-full text-xs px-3 py-2">
                  Open support assistant
                </Link>
              </div>
            </aside>
          </section>
        </div>
      </main>
    </UserShell>
  )
}

function PaymentLine({ label, value, strong }: { label: string; value: string; strong?: boolean }) {
  return (
    <div className="flex items-center justify-between gap-4">
      <span className="text-slate-500">{label}</span>
      <span className={strong ? 'text-lg font-semibold text-slate-900' : 'font-medium text-slate-900'}>{value}</span>
    </div>
  )
}
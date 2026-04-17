'use client'

import { useEffect, useState } from 'react'
import { useSearchParams, useRouter } from 'next/navigation'
import { AlertCircle, CheckCircle2, Clock, Loader2, Monitor } from 'lucide-react'
import { Navbar } from '@/components/layout/Navbar'
import { appointmentsApi, paymentsApi } from '@/lib/api'
import { useAuthStore } from '@/lib/store'
import toast from 'react-hot-toast'
import { formatCurrency, formatTime } from '@/lib/utils'

const shouldCollectDeposit = (appointment: any): boolean => {
  const isVirtual = Boolean(appointment?.virtual)
  const depositStatus = appointment?.depositStatus
  return isVirtual && depositStatus !== 'CONFIRMED'
}

declare global {
  interface Window {
    Razorpay?: new (options: Record<string, unknown>) => { open: () => void }
  }
}

export default function BookingConfirmPage() {
  const searchParams = useSearchParams()
  const router = useRouter()
  const { isAuthenticated, user } = useAuthStore()

  const appointmentId = searchParams.get('appointmentId')
  const stripeSessionId = searchParams.get('stripe_session_id')
  const paymentOrderIdFromQuery = searchParams.get('payment_order_id')

  const [appointment, setAppointment] = useState<any>(null)
  const [loading, setLoading] = useState(true)
  const [paymentProcessing, setPaymentProcessing] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [paymentUnavailableReason, setPaymentUnavailableReason] = useState<string | null>(null)
  const [gatewayProvider, setGatewayProvider] = useState<'RAZORPAY' | 'STRIPE' | 'UNKNOWN'>('UNKNOWN')
  const [confirmingCheckout, setConfirmingCheckout] = useState(false)

  useEffect(() => {
    if (!isAuthenticated) {
      router.push(`/auth/login?redirect=/booking/confirm?appointmentId=${appointmentId}`)
    }
  }, [isAuthenticated, router, appointmentId])

  useEffect(() => {
    if (!appointmentId) {
      setError('No appointment specified')
      setLoading(false)
      return
    }

    const loadConfirmation = async () => {
      try {
        setLoading(true)

        const appointmentRes = await appointmentsApi.getById(appointmentId)
        const apt = appointmentRes.data.data

        const paymentConfigRes = await paymentsApi.getConfig()
        const paymentConfig = paymentConfigRes.data.data
        const paymentConfigured = Boolean(paymentConfig?.configured)
        const provider = (paymentConfig?.provider as 'RAZORPAY' | 'STRIPE' | undefined) || 'UNKNOWN'
        setGatewayProvider(provider)

        if (!paymentConfigured && shouldCollectDeposit(apt)) {
          setPaymentUnavailableReason(
            paymentConfig?.reason || 'Online payment is temporarily unavailable on server. You can continue and complete payment later.',
          )
        }

        if (shouldCollectDeposit(apt) && paymentConfigured) {
          try {
            const paymentOrderRes = await paymentsApi.createDepositOrder(appointmentId)
            apt.pendingPayment = paymentOrderRes.data.data
          } catch (paymentErr: unknown) {
            const paymentMsg = (paymentErr as { response?: { data?: { message?: string } } })?.response?.data?.message
            if (paymentMsg?.toLowerCase().includes('not configured')) {
              setPaymentUnavailableReason('Online payment is temporarily unavailable on server. You can continue and complete payment later.')
            }
            // Keep page usable: user can retry from CTA without getting stuck.
          }
        }

        setAppointment(apt)

        if (provider === 'RAZORPAY') {
          const script = document.createElement('script')
          script.src = 'https://checkout.razorpay.com/v1/checkout.js'
          script.async = true
          document.body.appendChild(script)
        }
      } catch (err: unknown) {
        const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Failed to load payment confirmation'
        setError(msg)
        toast.error(msg)
      } finally {
        setLoading(false)
      }
    }

    void loadConfirmation()
  }, [appointmentId])

  useEffect(() => {
    if (!appointmentId || !stripeSessionId || !paymentOrderIdFromQuery) {
      return
    }

    const confirmStripeCheckout = async () => {
      try {
        setConfirmingCheckout(true)
        await paymentsApi.confirmDepositCheckout({
          paymentOrderId: paymentOrderIdFromQuery,
          checkoutSessionId: stripeSessionId,
        })
        toast.success('Payment successful! Your appointment is confirmed.')
        router.replace(`/dashboard/bookings?booked=${appointmentId}`)
      } catch (err: unknown) {
        const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Payment confirmation failed'
        toast.error(msg)
      } finally {
        setConfirmingCheckout(false)
      }
    }

    void confirmStripeCheckout()
  }, [appointmentId, paymentOrderIdFromQuery, router, stripeSessionId])

  const handlePrimaryAction = async () => {
    if (paymentInfo) {
      await handlePayment()
      return
    }

    if (depositRequired) {
      setPaymentProcessing(true)
      try {
        const paymentOrderRes = await paymentsApi.createDepositOrder(appointmentId as string)
        setAppointment((prev: any) => ({ ...prev, pendingPayment: paymentOrderRes.data.data }))
        setPaymentUnavailableReason(null)
        const provider = paymentOrderRes.data.data?.provider
        if (provider === 'STRIPE' || provider === 'RAZORPAY') {
          setGatewayProvider(provider)
        }
        toast.success('Payment order prepared. Click again to pay.')
      } catch (err: unknown) {
        const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Unable to prepare deposit order right now'
        if (msg.toLowerCase().includes('not configured')) {
          setPaymentUnavailableReason('Online payment is temporarily unavailable on server. You can continue and complete payment later.')
        }
        toast.error(msg)
      } finally {
        setPaymentProcessing(false)
      }
      return
    }

    router.push(`/dashboard/bookings?booked=${appointmentId}`)
  }

  const handlePayment = async () => {
    if (!appointment?.pendingPayment) {
      toast.error('No payment information available')
      return
    }

    const paymentData = appointment.pendingPayment

    if (paymentData.provider === 'STRIPE') {
      if (!paymentData.checkoutUrl) {
        toast.error('Stripe checkout URL is missing. Please retry.')
        return
      }
      setPaymentProcessing(true)
      window.location.href = paymentData.checkoutUrl
      return
    }

    if (!window.Razorpay) {
      toast.error('Payment gateway not loaded. Please refresh the page.')
      return
    }

    setPaymentProcessing(true)

    try {
      const options = {
        key: paymentData.keyId,
        amount: Math.round(paymentData.amount * 100),
        currency: paymentData.currency || 'INR',
        order_id: paymentData.gatewayOrderId,
        name: 'AppointUnified',
        description: `${appointment.service?.name || 'Consultation'} - ${appointment.professional?.displayName || ''}`,
        prefill: {
          email: user?.email || '',
          contact: user?.phone || '',
        },
        handler: async (response: any) => {
          try {
            await paymentsApi.verifyDeposit({
              paymentOrderId: paymentData.paymentOrderId,
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
            })

            toast.success('Payment successful! Your appointment is confirmed.')
            router.push(`/dashboard/bookings?booked=${appointmentId}`)
          } catch (err: unknown) {
            const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Payment verification failed'
            toast.error(msg)
            console.error('Payment verification error:', err)
          }
        },
        modal: {
          ondismiss: () => {
            setPaymentProcessing(false)
            toast.error('Payment cancelled')
          },
        },
      }

      const rzp = new window.Razorpay(options)
      rzp.open()
    } catch (err: unknown) {
      const msg = (err as { message?: string })?.message || 'Payment initiation failed'
      toast.error(msg)
      console.error('Payment error:', err)
    } finally {
      setPaymentProcessing(false)
    }
  }

  if (loading) {
    return (
      <>
        <Navbar />
        <div className="container-page py-20 flex flex-col justify-center items-center min-h-screen">
          <Loader2 size={40} className="animate-spin text-brand-600 mb-4" />
          <p className="text-slate-600">Preparing your payment...</p>
        </div>
      </>
    )
  }

  if (error || !appointment) {
    return (
      <>
        <Navbar />
        <div className="container-page py-20 flex flex-col justify-center items-center min-h-screen">
          <AlertCircle size={40} className="text-red-500 mb-4" />
          <p className="text-slate-900 font-semibold mb-2">Unable to Continue</p>
          <p className="text-slate-600 text-center max-w-md">{error || 'Appointment not found'}</p>
          <button onClick={() => router.push('/explore/healthcare')} className="btn-primary mt-6">
            Return to Explore
          </button>
        </div>
      </>
    )
  }

  const paymentInfo = appointment.pendingPayment
  const depositAmount = paymentInfo?.amount || 0
  const totalAmount = appointment?.service?.price || 0
  const depositRequired = shouldCollectDeposit(appointment) && !paymentUnavailableReason
  const securedByLabel = paymentInfo?.provider || gatewayProvider

  return (
    <>
      <Navbar />
      <main className="min-h-screen bg-gradient-to-br from-slate-50 to-blue-50/30">
        <div className="container-page py-8 max-w-2xl">
          <div className="card p-8 shadow-lg mb-8">
            <div className="flex items-center justify-center mb-6">
              <div className="h-16 w-16 rounded-full bg-emerald-100 flex items-center justify-center">
                <CheckCircle2 size={32} className="text-emerald-600" />
              </div>
            </div>
            <h1 className="text-3xl font-bold text-center text-slate-900 mb-2">Appointment Confirmed!</h1>
            <p className="text-center text-slate-600 mb-8">
              {depositRequired
                ? 'Your slot is reserved. Complete the deposit payment to finalize booking.'
                : 'Your slot is reserved. You can continue now.'}
            </p>

            <div className="space-y-4 mb-8 pb-8 border-b border-slate-200">
              <div className="flex items-start gap-4">
                <div className="h-12 w-12 rounded-lg bg-brand-100 flex-shrink-0 flex items-center justify-center text-brand-700 font-bold">
                  {appointment.professional?.displayName?.[0] || 'P'}
                </div>
                <div>
                  <p className="text-xs text-slate-500 uppercase tracking-wide">Professional</p>
                  <p className="font-semibold text-slate-900">{appointment.professional?.displayName}</p>
                  <p className="text-sm text-slate-500">{appointment.professional?.specialty}</p>
                </div>
              </div>

              <div className="flex items-start gap-4">
                <Monitor size={16} className="text-slate-400 mt-1" />
                <div>
                  <p className="text-xs text-slate-500 uppercase tracking-wide">Service</p>
                  <p className="font-semibold text-slate-900">{appointment.service?.name}</p>
                  <p className="text-sm text-slate-500">{appointment.service?.durationMinutes} minutes</p>
                </div>
              </div>

              <div className="flex items-start gap-4">
                <Clock size={16} className="text-slate-400 mt-1" />
                <div>
                  <p className="text-xs text-slate-500 uppercase tracking-wide">Date & Time</p>
                  <p className="font-semibold text-slate-900">{formatTime(appointment.startTime)}</p>
                </div>
              </div>
            </div>

            {paymentInfo && (
              <div className="bg-blue-50 rounded-xl p-6 mb-8">
                <h3 className="font-semibold text-slate-900 mb-4">Payment Summary</h3>

                <div className="space-y-3 mb-4 pb-4 border-b border-blue-200">
                  <div className="flex justify-between items-center">
                    <span className="text-slate-600">Service Total</span>
                    <span className="font-semibold text-slate-900">{formatCurrency(totalAmount)}</span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-slate-600">Now (Deposit)</span>
                    <span className="font-bold text-lg text-brand-600">{formatCurrency(depositAmount)}</span>
                  </div>
                </div>

                <div className="flex items-start gap-2 bg-blue-100 border border-blue-200 rounded-lg p-3">
                  <AlertCircle size={16} className="text-blue-600 flex-shrink-0 mt-0.5" />
                  <p className="text-sm text-blue-700">Pay the deposit now to secure your booking.</p>
                </div>
              </div>
            )}

            {!paymentInfo && (
              <div className="bg-emerald-50 rounded-xl p-6 mb-8 border border-emerald-200">
                <p className="text-emerald-700 font-medium">
                  {paymentUnavailableReason || (depositRequired ? 'Deposit order could not be prepared right now.' : 'No deposit payment is required for this appointment.')}
                </p>
              </div>
            )}

            <button onClick={handlePrimaryAction} disabled={paymentProcessing} className="btn-primary w-full py-4 text-lg font-semibold">
              {paymentProcessing ? (
                <>
                  <Loader2 size={20} className="animate-spin" />
                  Processing Payment...
                </>
              ) : (
                <>
                  {paymentInfo
                    ? paymentInfo.provider === 'STRIPE'
                      ? `Pay ${formatCurrency(depositAmount)} with Stripe`
                      : `Pay ${formatCurrency(depositAmount)} to Confirm`
                    : depositRequired
                      ? 'Retry Deposit Setup'
                      : 'Continue'}
                </>
              )}
            </button>

            {confirmingCheckout && (
              <p className="text-xs text-center text-slate-500 mt-4">Confirming checkout status...</p>
            )}

            <p className="text-xs text-center text-slate-500 mt-2">
              Secured by {securedByLabel === 'STRIPE' ? 'Stripe' : securedByLabel === 'RAZORPAY' ? 'Razorpay' : 'Payment Gateway'}
            </p>
          </div>
        </div>
      </main>
    </>
  )
}

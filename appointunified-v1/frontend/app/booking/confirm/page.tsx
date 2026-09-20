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
  const [paymentUnavailableReason, setPaymentUnavailableReason] = useState<string |

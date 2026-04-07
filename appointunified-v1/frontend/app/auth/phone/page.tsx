'use client'

import { useEffect, useMemo, useRef, useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { ConfirmationResult, RecaptchaVerifier, signInWithPhoneNumber } from 'firebase/auth'
import { Loader2, Phone, ShieldCheck } from 'lucide-react'
import { firebaseAuth, isFirebaseConfigured } from '@/lib/firebase'
import { generateFcmToken } from '@/lib/firebase-messaging'
import { authApi } from '@/lib/api'
import { useAuthStore } from '@/lib/store'
import { TokenPair } from '@/types'
import toast from 'react-hot-toast'

export default function PhoneLoginPage() {
  const router = useRouter()
  const { login } = useAuthStore()
  const [phone, setPhone] = useState('')
  const [otp, setOtp] = useState('')
  const [step, setStep] = useState<'phone' | 'otp'>('phone')
  const [loading, setLoading] = useState(false)
  const [confirmation, setConfirmation] = useState<ConfirmationResult | null>(null)
  const recaptchaRef = useRef<RecaptchaVerifier | null>(null)

  const canUseFirebase = useMemo(() => isFirebaseConfigured && !!firebaseAuth, [])

  useEffect(() => {
    if (!canUseFirebase || !firebaseAuth || typeof window === 'undefined') {
      return
    }

    if (recaptchaRef.current) {
      return
    }

    recaptchaRef.current = new RecaptchaVerifier(firebaseAuth, 'recaptcha-container', {
      size: 'invisible',
    })

    recaptchaRef.current.render().catch(() => {})

    return () => {
      recaptchaRef.current?.clear()
      recaptchaRef.current = null
    }
  }, [canUseFirebase])

  const sendOtp = async () => {
    if (!firebaseAuth || !recaptchaRef.current) {
      toast.error('Firebase is not configured.')
      return
    }

    if (!phone.trim()) {
      toast.error('Enter a phone number in E.164 format')
      return
    }

    setLoading(true)
    try {
      const result = await signInWithPhoneNumber(firebaseAuth, phone.trim(), recaptchaRef.current)
      setConfirmation(result)
      setStep('otp')
      toast.success('OTP sent to your phone')
    } catch (error) {
      console.error(error)
      toast.error('Could not send OTP')
      recaptchaRef.current?.clear()
      recaptchaRef.current = null
    } finally {
      setLoading(false)
    }
  }

  const verifyOtp = async () => {
    if (!confirmation) {
      toast.error('Request OTP first')
      return
    }

    if (!otp.trim()) {
      toast.error('Enter the OTP')
      return
    }

    setLoading(true)
    try {
      const credential = await confirmation.confirm(otp.trim())
      const idToken = await credential.user.getIdToken()
      const fcmToken = await generateFcmToken()

      const res = await authApi.firebaseLogin({
        idToken,
        fcmToken: fcmToken || undefined,
        platform: 'web',
        deviceName: typeof navigator !== 'undefined' ? navigator.userAgent : undefined,
      })

      const tokenPair = res.data.data as TokenPair
      login(tokenPair)
      toast.success('Signed in successfully')

      if (tokenPair.user.role === 'PROFESSIONAL') {
        router.push('/professional/dashboard')
      } else if (tokenPair.user.role === 'SUPER_ADMIN') {
        router.push('/super-admin/dashboard')
      } else if (['ADMIN', 'SUPER_ADMIN'].includes(tokenPair.user.role)) {
        router.push('/admin/dashboard')
      } else {
        router.push('/dashboard')
      }
    } catch (error) {
      console.error(error)
      toast.error('OTP verification failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <Link href="/" className="inline-flex items-center gap-2.5 mb-4">
            <div className="h-10 w-10 rounded-xl bg-brand-600 flex items-center justify-center text-white font-bold">AU</div>
            <span className="text-2xl font-bold text-slate-900">Appoint<span className="text-brand-600">Unified</span></span>
          </Link>
          <h1 className="text-2xl font-bold text-slate-900">Phone OTP login</h1>
          <p className="text-slate-500 text-sm mt-1">Sign in with Firebase phone verification</p>
        </div>

        {!canUseFirebase ? (
          <div className="card p-6 text-center">
            <ShieldCheck size={24} className="mx-auto text-brand-600 mb-2" />
            <p className="font-semibold text-slate-900">Firebase is not configured</p>
            <p className="text-sm text-slate-500 mt-1">Set the Firebase web env vars to enable phone OTP login.</p>
            <Link href="/auth/login" className="btn-secondary mt-4">Back to password login</Link>
          </div>
        ) : (
          <div className="card p-8 space-y-5">
            {step === 'phone' && (
              <>
                <div>
                  <label className="label">Phone number</label>
                  <input
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    className="input"
                    placeholder="+919876543210"
                    inputMode="tel"
                  />
                </div>

                <button onClick={sendOtp} disabled={loading} className="btn-primary w-full py-3">
                  {loading ? <Loader2 size={18} className="animate-spin" /> : <Phone size={16} />}
                  Send OTP
                </button>
              </>
            )}

            {step === 'otp' && (
              <>
                <div>
                  <label className="label">Verification code</label>
                  <input
                    value={otp}
                    onChange={(e) => setOtp(e.target.value)}
                    className="input"
                    placeholder="123456"
                    inputMode="numeric"
                    maxLength={6}
                  />
                </div>

                <button onClick={verifyOtp} disabled={loading} className="btn-primary w-full py-3">
                  {loading ? <Loader2 size={18} className="animate-spin" /> : 'Verify and continue'}
                </button>

                <button onClick={() => setStep('phone')} className="btn-ghost w-full">
                  Change phone number
                </button>
              </>
            )}

            <div id="recaptcha-container" />

            <p className="text-xs text-slate-400 text-center">
              After verification, the app exchanges your Firebase ID token for a local AppointUnified session.
            </p>
          </div>
        )}
      </div>
    </div>
  )
}
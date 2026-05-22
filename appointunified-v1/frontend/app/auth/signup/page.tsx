'use client'

import { Suspense, useState } from 'react'
import Link from 'next/link'
import { useRouter, useSearchParams } from 'next/navigation'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { ChevronRight, Eye, EyeOff, Loader2, Activity, Calendar, Stethoscope, MapPin, Check } from 'lucide-react'
import { authApi } from '@/lib/api'
import { LocationAutocomplete, LocationData } from '@/components/ui/LocationAutocomplete'
import { useAuthStore } from '@/lib/store'
import { TokenPair } from '@/types'
import { cn } from '@/lib/utils'
import toast from 'react-hot-toast'

const schema = z.object({
  fullName: z.string().min(2, 'Name must be at least 2 characters'),
  phone: z.string().regex(/^\+[1-9]\d{6,14}$/, 'Enter phone in format: +919876543210'),
  email: z.string().email('Invalid email').optional().or(z.literal('')),
  password: z.string().min(6, 'At least 6 characters required'),
  role: z.enum(['PUBLIC', 'PROFESSIONAL']),
  city: z.string().optional(),
  address: z.string().optional(),
  latitude: z.number().optional(),
  longitude: z.number().optional(),
})
type FormData = z.infer<typeof schema>

const ROLE_OPTIONS = [
  { value: 'PUBLIC', icon: <Calendar size={18} className="text-accent" />, label: 'I want to book', desc: 'Search and book verified professionals' },
  { value: 'PROFESSIONAL', icon: <Stethoscope size={18} className="text-accent-mint" />, label: 'I am a professional', desc: 'Get listed and manage your schedule' },
]

function SignupContent() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const { login } = useAuthStore()
  const [showPassword, setShowPassword] = useState(false)
  const [locationState, setLocationState] = useState<LocationData | null>(null)

  const defaultRole = searchParams.get('role') === 'professional' ? 'PROFESSIONAL' : 'PUBLIC'

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { role: defaultRole },
  })

  const selectedRole = watch('role')

  const onSubmit = async (data: FormData) => {
    try {
      const payload = {
        ...data,
        email: data.email || undefined,
        city: locationState?.city,
        address: locationState?.address,
        latitude: locationState?.lat,
        longitude: locationState?.lon,
      }
      const res = await authApi.signUp(payload)
      const tokenPair = res.data.data as TokenPair
      login(tokenPair)
      toast.success(`Welcome to AppointUnified, ${tokenPair.user.fullName.split(' ')[0]}!`)

      if (tokenPair.user.role === 'PROFESSIONAL') {
        router.push('/professional/onboarding')
      } else {
        router.push('/dashboard')
      }
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Signup failed. Please try again.'
      toast.error(msg)
    }
  }

  return (
    <div className="min-h-screen bg-primary flex items-center justify-center p-4 relative overflow-hidden py-12">
      <div className="absolute top-[-5%] right-[-10%] w-[600px] h-[600px] rounded-full bg-pastel-pink/30 blur-[140px] pointer-events-none animate-float-slow" />
      <div className="absolute bottom-[-10%] left-[-10%] w-[500px] h-[500px] rounded-full bg-pastel-yellow/30 blur-[120px] pointer-events-none" />

      <div className="w-full max-w-lg relative z-10">
        <div className="text-center mb-10">
          <Link href="/" className="inline-flex items-center gap-3 mb-6 group">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-accent to-accent-light text-white shadow-soft group-hover:shadow-float transition-all duration-300 group-hover:-translate-y-0.5">
              <Activity size={24} strokeWidth={2.5} />
            </div>
            <span className="text-2xl font-extrabold tracking-tight text-text-primary">
              Appoint<span className="text-accent">Unified</span>
            </span>
          </Link>
          <h1 className="text-3xl font-extrabold text-text-primary tracking-tight">Create your account</h1>
          <p className="text-text-secondary text-sm font-medium mt-2">Free forever. Connect to the unified grid.</p>
        </div>

        <div className="card p-8 bg-white/70 backdrop-blur-xl border border-white">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
            <div>
              <label className="label">I am signing up as…</label>
              <div className="grid grid-cols-2 gap-3">
                {ROLE_OPTIONS.map((opt) => (
                  <button
                    key={opt.value}
                    type="button"
                    onClick={() => setValue('role', opt.value as 'PUBLIC' | 'PROFESSIONAL')}
                    className={cn(
                      'rounded-2xl border-2 p-4 text-left transition-all duration-300',
                      selectedRole === opt.value
                        ? 'border-accent bg-accent-glow/20 shadow-soft'
                        : 'border-border bg-white hover:border-accent-light hover:shadow-soft'
                    )}
                  >
                    <div className="flex items-center gap-2 mb-1">
                      {opt.icon}
                      <div className="text-sm font-extrabold text-text-primary">{opt.label}</div>
                    </div>
                    <div className="text-xs font-medium text-text-secondary mt-1">{opt.desc}</div>
                  </button>
                ))}
              </div>
              <input type="hidden" {...register('role')} />
            </div>

            <div>
              <label className="label">Full name</label>
              <input
                {...register('fullName')}
                className={cn('input', errors.fullName && 'input-error')}
                placeholder="Priya Sharma"
              />
              {errors.fullName && <p className="error-text">{errors.fullName.message}</p>}
            </div>

            <div>
              <label className="label">Phone number <span className="text-accent-warm">*</span></label>
              <input
                {...register('phone')}
                className={cn('input', errors.phone && 'input-error')}
                placeholder="+919876543210"
              />
              {errors.phone && <p className="error-text">{errors.phone.message}</p>}
            </div>

            <div>
              <label className="label">Email <span className="text-text-muted font-medium ml-1">(optional)</span></label>
              <input
                {...register('email')}
                type="email"
                className={cn('input', errors.email && 'input-error')}
                placeholder="you@example.com"
              />
              {errors.email && <p className="error-text">{errors.email.message}</p>}
            </div>

            <div>
              <label className="label">Password</label>
              <div className="relative">
                <input
                  {...register('password')}
                  type={showPassword ? 'text' : 'password'}
                  className={cn('input pr-11', errors.password && 'input-error')}
                  placeholder="Min. 6 characters"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-text-muted hover:text-accent transition-colors"
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
              {errors.password && <p className="error-text">{errors.password.message}</p>}
            </div>

            <div className="pt-2">
              <label className="label mb-2 flex items-center gap-1.5 text-text-primary">
                <MapPin size={16} className="text-accent" /> Set Base Location <span className="text-text-muted font-normal ml-1">(optional)</span>
              </label>
              <LocationAutocomplete
                onLocationSelect={(loc) => setLocationState(loc)}
                placeholder="Search city or address..."
              />
              {locationState && (
                <p className="text-xs text-accent-mint mt-2 font-medium flex items-center gap-1">
                  <Check size={12} strokeWidth={3} /> Location saved securely
                </p>
              )}
            </div>

            <button type="submit" disabled={isSubmitting} className="btn-primary w-full py-3.5 mt-2">
              {isSubmitting
                ? <Loader2 size={20} className="animate-spin" />
                : <><span className="text-base tracking-wide">Create account</span> <ChevronRight size={20} strokeWidth={2.5} /></>
              }
            </button>
          </form>

          <div className="mt-8 pt-6 border-t border-border text-center text-sm font-medium text-text-secondary">
            Already have an account?{' '}
            <Link href="/auth/login" className="text-accent font-bold hover:text-accent-light transition-colors">Sign in here</Link>
          </div>
        </div>

        <p className="text-center text-xs font-medium text-text-muted mt-8 uppercase tracking-widest">
          <Link href="/terms" className="hover:text-text-primary transition-colors">Terms of Service</Link>
          <span className="mx-3">•</span>
          <Link href="/privacy" className="hover:text-text-primary transition-colors">Privacy Policy</Link>
        </p>
      </div>
    </div>
  )
}

export default function SignupPage() {
  return (
    <Suspense fallback={<div>Loading...</div>}>
      <SignupContent />
    </Suspense>
  )
}
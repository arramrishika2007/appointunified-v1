'use client'

import { Suspense, useEffect, useState } from 'react'
import Link from 'next/link'
import { useRouter, useSearchParams } from 'next/navigation'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Eye, EyeOff, Loader2, Activity } from 'lucide-react'
import { authApi } from '@/lib/api'
import { useAuthStore } from '@/lib/store'
import { TokenPair } from '@/types'
import { cn } from '@/lib/utils'
import toast from 'react-hot-toast'

const schema = z.object({
  identifier: z.string().min(1, 'Phone or email is required'),
  password: z.string().min(1, 'Password is required'),
})
type FormData = z.infer<typeof schema>

function LoginContent() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const accessRole = searchParams.get('role')
  const prefillIdentifier = searchParams.get('identifier')
  const prefillPassword = searchParams.get('password')
  const { login } = useAuthStore()
  const [showPassword, setShowPassword] = useState(false)

  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({ resolver: zodResolver(schema) })

  useEffect(() => {
    if (prefillIdentifier) {
      setValue('identifier', prefillIdentifier, { shouldValidate: true })
    }
    if (prefillPassword) {
      setValue('password', prefillPassword, { shouldValidate: true })
    }

    if (!prefillIdentifier && !prefillPassword) {
      return
    }

    const nextParams = new URLSearchParams(searchParams.toString())
    nextParams.delete('identifier')
    nextParams.delete('password')
    const nextQuery = nextParams.toString()
    router.replace(nextQuery ? `/auth/login?${nextQuery}` : '/auth/login')
  }, [prefillIdentifier, prefillPassword, router, searchParams, setValue])

  const onSubmit = async (data: FormData) => {
    try {
      const res = await authApi.login(data)
      const tokenPair = res.data.data as TokenPair

      if (accessRole === 'SUPER_ADMIN' && tokenPair.user.role !== 'SUPER_ADMIN') {
        toast.error('This account is not a super admin account.')
        return
      }

      if (accessRole === 'ADMIN' && !['ADMIN', 'SUPER_ADMIN'].includes(tokenPair.user.role)) {
        toast.error('This account does not have admin access.')
        return
      }

      login(tokenPair)
      toast.success(`Welcome back, ${tokenPair.user.fullName.split(' ')[0]}!`)

      if (tokenPair.user.role === 'PROFESSIONAL') {
        router.push('/professional/dashboard')
      } else if (tokenPair.user.role === 'SUPER_ADMIN') {
        router.push('/super-admin/dashboard')
      } else if (['ADMIN', 'SUPER_ADMIN'].includes(tokenPair.user.role)) {
        router.push('/admin/dashboard')
      } else {
        router.push('/dashboard')
      }
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Login failed. Please try again.'
      toast.error(msg)
    }
  }

  return (
    <div className="min-h-screen bg-primary flex items-center justify-center p-4 relative overflow-hidden">
      <div className="absolute top-[-10%] left-[-5%] w-[500px] h-[500px] rounded-full bg-pastel-purple/30 blur-[130px] pointer-events-none animate-float-slow" />
      <div className="absolute bottom-[-10%] right-[-10%] w-[450px] h-[450px] rounded-full bg-pastel-yellow/30 blur-[120px] pointer-events-none" />

      <div className="w-full max-w-sm relative z-10">
        <div className="text-center mb-8">
          <Link href="/" className="inline-flex items-center gap-3 mb-6 group">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-accent to-accent-light text-white shadow-soft group-hover:shadow-float transition-all duration-300 group-hover:-translate-y-0.5">
              <Activity size={24} strokeWidth={2.5} />
            </div>
            <span className="text-2xl font-extrabold tracking-tight text-text-primary">
              Appoint<span className="text-accent">Unified</span>
            </span>
          </Link>
          <h1 className="text-3xl font-extrabold text-text-primary tracking-tight">Welcome back</h1>
          <p className="text-text-secondary text-sm font-medium mt-2">Sign in to your intelligent schedule</p>
          {accessRole && (
            <p className="mt-3 inline-flex items-center rounded-full bg-slate-100 px-3 py-1 text-xs font-bold uppercase tracking-widest text-slate-600">
              {accessRole === 'SUPER_ADMIN' ? 'Super admin access' : 'Admin access'}
            </p>
          )}
        </div>

        <div className="card p-8 bg-white/70 backdrop-blur-xl border border-white">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
            <div>
              <label className="label">Phone number or email</label>
              <input
                {...register('identifier')}
                className={cn('input', errors.identifier && 'input-error')}
                placeholder="+91 98765 43210 or you@email.com"
                autoComplete="username"
              />
              {errors.identifier && <p className="error-text">{errors.identifier.message}</p>}
            </div>

            <div>
              <div className="flex items-center justify-between mb-2">
                <label className="label mb-0">Password</label>
                <Link href="/auth/forgot-password" className="text-xs font-bold text-accent hover:text-accent-light transition-colors">
                  Forgot password?
                </Link>
              </div>
              <div className="relative">
                <input
                  {...register('password')}
                  type={showPassword ? 'text' : 'password'}
                  className={cn('input pr-11', errors.password && 'input-error')}
                  placeholder="••••••••"
                  autoComplete="current-password"
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

            <button type="submit" disabled={isSubmitting} className="btn-primary w-full py-3.5">
              {isSubmitting ? <Loader2 size={20} className="animate-spin" /> : 'Sign in'}
            </button>
          </form>

          <div className="mt-8 text-center text-sm font-medium text-text-secondary">
            Don&apos;t have an account?{' '}
            <Link href="/auth/signup" className="text-accent font-bold hover:text-accent-light transition-colors">
              Sign up free
            </Link>
          </div>

          <div className="mt-6 pt-6 border-t border-border text-center text-sm font-medium text-text-secondary">
            Or use{' '}
            <Link href="/auth/phone" className="text-text-primary font-bold hover:text-accent transition-colors">
              phone OTP login
            </Link>
          </div>
        </div>

        <p className="text-center text-xs font-medium text-text-muted mt-8 uppercase tracking-widest">
          <Link href="/terms" className="hover:text-text-primary transition-colors">Terms</Link>
          <span className="mx-3">•</span>
          <Link href="/privacy" className="hover:text-text-primary transition-colors">Policy</Link>
        </p>
      </div>
    </div>
  )
}

export default function LoginPage() {
  return (
    <Suspense fallback={<div>Loading...</div>}>
      <LoginContent />
    </Suspense>
  )
}
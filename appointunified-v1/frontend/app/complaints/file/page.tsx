'use client'

import { useState } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { AlertTriangle, Loader2 } from 'lucide-react'
import { Navbar } from '@/components/layout/Navbar'
import { complaintApi } from '@/lib/api-v2'
import { COMPLAINT_CATEGORIES } from '@/lib/utils-v2'
import { cn } from '@/lib/utils'
import toast from 'react-hot-toast'

const schema = z.object({
  category: z.string().min(1, 'Please select a category'),
  description: z.string().min(30, 'Please describe the issue in at least 30 characters').max(2000),
})

type FormData = z.infer<typeof schema>

export default function FileComplaintPage() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const professionalId = searchParams.get('professional')
  const appointmentId = searchParams.get('appointment')
  const [selectedCategory, setSelectedCategory] = useState('')

  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
  })

  const onSubmit = async (data: FormData) => {
    if (!professionalId) {
      toast.error('No professional specified')
      return
    }
    try {
      await complaintApi.file({
        professionalId,
        appointmentId: appointmentId ?? undefined,
        category: data.category,
        description: data.description,
      })
      toast.success('Complaint filed. Our team will review within 72 hours.')
      router.push('/dashboard/bookings')
    } catch (err: unknown) {
      toast.error(
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
          'Failed to file complaint. Please try again.'
      )
    }
  }

  return (
    <>
      <Navbar />
      <main className="min-h-screen bg-slate-50">
        <div className="container-page py-8 max-w-xl">
          <div className="flex items-center gap-3 mb-6">
            <div className="h-10 w-10 rounded-xl bg-red-100 flex items-center justify-center">
              <AlertTriangle size={20} className="text-red-600" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-slate-900">File a Complaint</h1>
              <p className="text-slate-500 text-sm mt-0.5">Reviewed within 72 hours by our Trust & Safety team</p>
            </div>
          </div>

          <div className="card p-6">
            <div className="rounded-xl bg-amber-50 border border-amber-200 p-4 mb-6 text-sm text-amber-800">
              <strong>⚠️ Important:</strong> Complaints are serious. False or malicious reports may result in action against your
              account. Only file if you have a genuine concern.
            </div>

            <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
              <div>
                <label className="label">What is your complaint about?</label>
                <div className="space-y-2">
                  {COMPLAINT_CATEGORIES.map((cat) => (
                    <button
                      key={cat.value}
                      type="button"
                      onClick={() => {
                        setSelectedCategory(cat.value)
                        setValue('category', cat.value)
                      }}
                      className={cn(
                        'w-full rounded-xl border-2 p-3 text-left transition-all',
                        selectedCategory === cat.value ? 'border-brand-500 bg-brand-50' : 'border-slate-200 hover:border-slate-300'
                      )}
                    >
                      <div className="text-sm font-semibold text-slate-900">{cat.label}</div>
                      <div className="text-xs text-slate-500 mt-0.5">{cat.desc}</div>
                    </button>
                  ))}
                </div>
                <input type="hidden" {...register('category')} />
                {errors.category && <p className="error-text mt-1">{errors.category.message}</p>}
              </div>

              <div>
                <label className="label">Describe what happened</label>
                <textarea
                  {...register('description')}
                  rows={5}
                  className={cn('input resize-none', errors.description && 'input-error')}
                  placeholder="Please be as specific as possible — dates, times, what was said or done, any evidence you have…"
                />
                {errors.description && <p className="error-text">{errors.description.message}</p>}
              </div>

              <button type="submit" disabled={isSubmitting || !selectedCategory} className="btn-danger w-full py-3">
                {isSubmitting ? (
                  <>
                    <Loader2 size={16} className="animate-spin" /> Submitting…
                  </>
                ) : (
                  '🚨 Submit Complaint'
                )}
              </button>
            </form>
          </div>
        </div>
      </main>
    </>
  )
}

'use client'

import { useEffect, useMemo, useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { CheckCircle2, CircleDashed, Clock3, Loader2 } from 'lucide-react'
import toast from 'react-hot-toast'
import { Navbar } from '@/components/layout/Navbar'
import { workflowsApi } from '@/lib/api'
import { useAuthStore } from '@/lib/store'
import { WorkflowInstance, WorkflowStepProgress } from '@/types'
import { cn } from '@/lib/utils'

interface WorkflowInstancePageProps {
  params: {
    instanceId: string
  }
}

export default function WorkflowInstancePage({ params }: WorkflowInstancePageProps) {
  const router = useRouter()
  const { isAuthenticated } = useAuthStore()

  const [loading, setLoading] = useState(true)
  const [instance, setInstance] = useState<WorkflowInstance | null>(null)

  useEffect(() => {
    if (!isAuthenticated) {
      router.push('/auth/login')
      return
    }

    setLoading(true)
    workflowsApi
      .getMyInstances()
      .then((res) => {
        const found = (res.data.data as WorkflowInstance[]).find((item) => item.id === params.instanceId)
        if (!found) {
          toast.error('Workflow instance not found')
          router.push('/dashboard/workflows')
          return
        }
        setInstance(found)
      })
      .catch(() => {
        toast.error('Could not load workflow instance')
      })
      .finally(() => setLoading(false))
  }, [isAuthenticated, params.instanceId, router])

  const progress = useMemo(() => {
    if (!instance) return 0
    const completed = instance.steps.filter((s) => !!s.completedAt).length
    return Math.round((completed / Math.max(instance.totalSteps, 1)) * 100)
  }, [instance])
  const completedCount = useMemo(() => {
    if (!instance) return 0
    return instance.steps.filter((s) => !!s.completedAt).length
  }, [instance])

  const buildBookLink = (step: WorkflowStepProgress) => {
    if (!step.professionalId) return null
    const search = new URLSearchParams()
    if (step.serviceId) search.set('service', step.serviceId)
    search.set('workflowInstance', params.instanceId)
    search.set('workflowStep', String(step.order))
    search.set('workflow', '1')
    const suffix = search.toString()
    return `/booking/${step.professionalId}${suffix ? `?${suffix}` : ''}`
  }

  if (!isAuthenticated) return null

  return (
    <>
      <Navbar />
      <main className="min-h-screen bg-slate-50">
        <div className="container-page max-w-4xl py-8">
          {loading ? (
            <div className="flex justify-center py-20">
              <Loader2 size={30} className="animate-spin text-brand-600" />
            </div>
          ) : !instance ? (
            <div className="card p-8 text-center">
              <p className="text-slate-500">Workflow instance unavailable.</p>
              <Link href="/dashboard/workflows" className="mt-3 inline-flex text-sm text-brand-600 hover:underline">
                Back to workflows
              </Link>
            </div>
          ) : (
            <>
              <div className="mb-6 flex items-start justify-between gap-3">
                <div>
                  <h1 className="text-2xl font-bold text-slate-900">{instance.workflowName}</h1>
                  <p className="mt-1 text-sm text-slate-500">Status: {instance.status.replace('_', ' ')}</p>
                </div>
                <Link href="/dashboard/workflows" className="btn-ghost text-xs px-3 py-1.5">
                  All Workflows
                </Link>
              </div>

              <div className="mb-6 grid grid-cols-1 gap-3 sm:grid-cols-3">
                <div className="card p-4">
                  <p className="text-[11px] uppercase tracking-wide text-slate-500">Progress</p>
                  <p className="mt-1 text-2xl font-bold text-brand-700">{progress}%</p>
                </div>
                <div className="card p-4">
                  <p className="text-[11px] uppercase tracking-wide text-slate-500">Current Step</p>
                  <p className="mt-1 text-2xl font-bold text-slate-900">{instance.currentStep}/{instance.totalSteps}</p>
                </div>
                <div className="card p-4">
                  <p className="text-[11px] uppercase tracking-wide text-slate-500">Completed Steps</p>
                  <p className="mt-1 text-2xl font-bold text-emerald-700">{completedCount}</p>
                </div>
              </div>

              <div className="card mb-6 p-4">
                <div className="mb-2 flex items-center justify-between gap-3 text-sm">
                  <p className="font-medium text-slate-900">Progress</p>
                  <p className="text-slate-500">{progress}%</p>
                </div>
                <progress
                  className="h-2 w-full overflow-hidden rounded-full [&::-webkit-progress-bar]:rounded-full [&::-webkit-progress-bar]:bg-slate-200 [&::-webkit-progress-value]:rounded-full [&::-webkit-progress-value]:bg-emerald-500"
                  max={100}
                  value={progress}
                />
              </div>

              <div className="space-y-3">
                {instance.steps.map((step) => {
                  const completed = !!step.completedAt
                  const active = instance.status === 'IN_PROGRESS' && step.order === instance.currentStep
                  const bookingLink = buildBookLink(step)

                  return (
                    <div key={step.order} className={cn('card border-l-4 p-4', active ? 'border-l-brand-500 ring-1 ring-brand-300' : completed ? 'border-l-emerald-500' : 'border-l-slate-200')}>
                      <div className="mb-2 flex items-start justify-between gap-3">
                        <div className="flex items-center gap-2">
                          {completed ? (
                            <CheckCircle2 size={16} className="text-emerald-600" />
                          ) : active ? (
                            <Clock3 size={16} className="text-brand-600" />
                          ) : (
                            <CircleDashed size={16} className="text-slate-400" />
                          )}
                          <p className="font-semibold text-slate-900">Step {step.order}: {step.label || `Step ${step.order}`}</p>
                        </div>
                        <span className={cn(
                          'rounded-full px-2 py-0.5 text-[10px] font-semibold',
                          completed ? 'bg-emerald-100 text-emerald-700' : active ? 'bg-brand-100 text-brand-700' : 'bg-slate-100 text-slate-500'
                        )}>
                          {completed ? 'Completed' : active ? 'Current' : 'Pending'}
                        </span>
                      </div>

                      {step.appointmentStatus && (
                        <p className="mb-2 text-xs text-slate-500">Appointment status: {step.appointmentStatus}</p>
                      )}

                      <div className="flex items-center gap-2">
                        {active && !step.appointmentId && bookingLink && (
                          <Link href={bookingLink} className="btn-primary px-3 py-1.5 text-xs">
                            Book Next Step
                          </Link>
                        )}

                        {step.appointmentId && (
                          <Link href="/dashboard/bookings" className="btn-ghost px-3 py-1.5 text-xs">
                            View Linked Appointment
                          </Link>
                        )}
                      </div>
                    </div>
                  )
                })}
              </div>
            </>
          )}
        </div>
      </main>
    </>
  )
}

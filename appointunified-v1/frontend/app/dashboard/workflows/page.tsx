'use client'

import { useEffect, useMemo, useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { CalendarPlus2, CheckCircle2, GitBranch, Loader2, PlayCircle } from 'lucide-react'
import toast from 'react-hot-toast'
import { Navbar } from '@/components/layout/Navbar'
import { workflowsApi } from '@/lib/api'
import { useAuthStore } from '@/lib/store'
import { WorkflowDefinition, WorkflowInstance } from '@/types'
import { cn } from '@/lib/utils'

export default function WorkflowsPage() {
  const router = useRouter()
  const { isAuthenticated, user } = useAuthStore()

  const [loading, setLoading] = useState(true)
  const [definitions, setDefinitions] = useState<WorkflowDefinition[]>([])
  const [instances, setInstances] = useState<WorkflowInstance[]>([])
  const [startLoading, setStartLoading] = useState<string | null>(null)

  useEffect(() => {
    if (!isAuthenticated) {
      router.push('/auth/login')
      return
    }

    setLoading(true)
    Promise.all([
      workflowsApi.list(user?.sector ? { sector: user.sector } : undefined),
      workflowsApi.getMyInstances(),
    ])
      .then(([defsRes, instRes]) => {
        setDefinitions(defsRes.data.data)
        setInstances(instRes.data.data)
      })
      .catch(() => {
        toast.error('Could not load workflows')
      })
      .finally(() => setLoading(false))
  }, [isAuthenticated, router, user?.sector])

  const activeInstances = useMemo(
    () => instances.filter((item) => item.status === 'IN_PROGRESS'),
    [instances]
  )

  const startWorkflow = async (workflowId: string) => {
    setStartLoading(workflowId)
    try {
      const res = await workflowsApi.start(workflowId)
      const created = res.data.data as WorkflowInstance
      setInstances((prev) => [created, ...prev])
      toast.success('Workflow started')
      router.push(`/bookings/workflow/${created.id}`)
    } catch {
      toast.error('Could not start workflow')
    } finally {
      setStartLoading(null)
    }
  }

  if (!isAuthenticated || !user) return null

  return (
    <>
      <Navbar />
      <main className="min-h-screen bg-slate-50">
        <div className="container-page max-w-5xl py-8">
          <div className="mb-8 flex items-start justify-between gap-3">
            <div>
              <h1 className="text-2xl font-bold text-slate-900">Workflow Plans</h1>
              <p className="mt-1 text-sm text-slate-500">Start guided multi-step plans and track your progress.</p>
            </div>
            <Link href="/dashboard/bookings" className="btn-ghost text-xs px-3 py-1.5">
              <CalendarPlus2 size={13} /> My Bookings
            </Link>
          </div>

          {loading ? (
            <div className="flex justify-center py-20">
              <Loader2 size={30} className="animate-spin text-brand-600" />
            </div>
          ) : (
            <div className="grid gap-8 lg:grid-cols-2">
              <section>
                <div className="mb-3 flex items-center gap-2">
                  <GitBranch size={16} className="text-brand-600" />
                  <h2 className="font-semibold text-slate-900">Available Plans</h2>
                </div>

                <div className="space-y-3">
                  {definitions.length === 0 ? (
                    <div className="card p-6 text-sm text-slate-500">No workflows available right now.</div>
                  ) : (
                    definitions.map((definition) => (
                      <div key={definition.id} className="card p-4">
                        <div className="mb-2 flex items-start justify-between gap-3">
                          <div>
                            <p className="font-semibold text-slate-900">{definition.name}</p>
                            <p className="mt-0.5 text-xs text-slate-500">{definition.sector}</p>
                          </div>
                          <span className="rounded-full bg-brand-100 px-2 py-0.5 text-[10px] font-semibold text-brand-700">
                            {definition.steps.length} steps
                          </span>
                        </div>
                        {definition.description && (
                          <p className="mb-3 text-sm text-slate-600">{definition.description}</p>
                        )}
                        <button
                          onClick={() => startWorkflow(definition.id)}
                          disabled={startLoading === definition.id}
                          className="btn-primary px-3 py-1.5 text-xs"
                        >
                          {startLoading === definition.id ? <Loader2 size={13} className="animate-spin" /> : <PlayCircle size={13} />}
                          Start Plan
                        </button>
                      </div>
                    ))
                  )}
                </div>
              </section>

              <section>
                <div className="mb-3 flex items-center gap-2">
                  <CheckCircle2 size={16} className="text-emerald-600" />
                  <h2 className="font-semibold text-slate-900">Active Instances</h2>
                </div>

                <div className="space-y-3">
                  {activeInstances.length === 0 ? (
                    <div className="card p-6 text-sm text-slate-500">No active workflow instances.</div>
                  ) : (
                    activeInstances.map((instance) => {
                      const done = instance.steps.filter((s) => !!s.completedAt).length
                      const pct = Math.round((done / Math.max(instance.totalSteps, 1)) * 100)
                      return (
                        <div key={instance.id} className="card p-4">
                          <div className="mb-2 flex items-center justify-between gap-3">
                            <p className="font-semibold text-slate-900">{instance.workflowName}</p>
                            <span className="text-xs font-medium text-slate-500">Step {instance.currentStep}/{instance.totalSteps}</span>
                          </div>

                          <progress
                            className={cn('mb-3 h-2 w-full overflow-hidden rounded-full [&::-webkit-progress-bar]:rounded-full [&::-webkit-progress-bar]:bg-slate-200 [&::-webkit-progress-value]:rounded-full [&::-webkit-progress-value]:bg-emerald-500')}
                            max={100}
                            value={pct}
                          />

                          <div className="flex items-center justify-between gap-3 text-xs">
                            <p className="text-slate-500">Next: {instance.nextStep?.label || 'Completed'}</p>
                            <Link href={`/bookings/workflow/${instance.id}`} className="text-brand-600 hover:underline">
                              Open Tracker
                            </Link>
                          </div>
                        </div>
                      )
                    })
                  )}
                </div>
              </section>
            </div>
          )}
        </div>
      </main>
    </>
  )
}

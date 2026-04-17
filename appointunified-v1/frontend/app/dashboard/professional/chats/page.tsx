'use client'

import { useEffect, useMemo } from 'react'
import { useRouter } from 'next/navigation'
import Link from 'next/link'
import useSWR from 'swr'
import { ArrowRight, Loader2, MessageSquare, RefreshCw, Sparkles } from 'lucide-react'
import { ChatThreadList } from '@/components/chat/ChatThreadList'
import { chatApi } from '@/lib/api'
import { useAuthStore } from '@/lib/store'
import { ChatThreadItem } from '@/types'

export default function ProfessionalChatsPage() {
  const router = useRouter()
  const { hasHydrated, isAuthenticated } = useAuthStore()
  const shouldFetchThreads = hasHydrated && isAuthenticated

  useEffect(() => {
    if (hasHydrated && !isAuthenticated) {
      router.replace('/auth/login?redirect=/dashboard/professional/chats')
    }
  }, [hasHydrated, isAuthenticated, router])

  const { data: threads, isLoading, error, mutate } = useSWR<ChatThreadItem[]>(
    shouldFetchThreads ? '/chat/threads' : null,
    () => chatApi.getThreads().then((res) => res.data ?? []),
    {
      refreshInterval: 5000,
      revalidateOnFocus: true,
    }
  )

  const hasThreads = useMemo(() => (threads?.length ?? 0) > 0, [threads])

  if (!hasHydrated || !shouldFetchThreads) {
    return (
      <main className="flex min-h-[calc(100vh-4rem)] items-center justify-center bg-slate-50 px-4">
        <div className="flex items-center gap-3 text-slate-500">
          <Loader2 className="h-5 w-5 animate-spin text-brand-600" />
          Loading professional chats...
        </div>
      </main>
    )
  }

  return (
    <main className="min-h-[calc(100vh-4rem)] bg-gradient-to-b from-slate-50 to-slate-100">
      <div className="container-page max-w-7xl py-8">
        <div className="mb-8 flex flex-wrap items-start justify-between gap-4">
          <div className="flex-1">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-brand-600">Professional Inbox</p>
            <h1 className="mt-2 text-3xl font-bold text-slate-900 sm:text-4xl">Client Conversations</h1>
            <p className="mt-3 max-w-2xl text-base text-slate-600">
              Keep appointment chats inside your professional workspace so you can respond quickly and stay on top of client updates.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => mutate()}
              disabled={isLoading}
              className="inline-flex items-center gap-2 rounded-full border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 transition hover:border-brand-300 hover:text-brand-700 disabled:opacity-50"
            >
              <RefreshCw className={`h-4 w-4 ${isLoading ? 'animate-spin' : ''}`} />
              Refresh
            </button>
            <Link
              href="/dashboard/professional/appointments"
              className="inline-flex items-center gap-2 rounded-full bg-slate-900 px-4 py-2 text-sm font-semibold text-white transition hover:bg-slate-800 hover:shadow-lg"
            >
              Appointments
              <ArrowRight className="h-4 w-4" />
            </Link>
          </div>
        </div>

        {error && (
          <div className="mb-6 flex items-center gap-2 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
            <span>⚠️</span>
            <span>Unable to load chats. Please refresh and try again.</span>
          </div>
        )}

        <div className="mt-6 grid gap-6 lg:grid-cols-[380px_minmax(0,1fr)]">
          <ChatThreadList
            threads={threads ?? []}
            loading={isLoading}
            appointmentPathBase="/dashboard/professional/chats"
          />

          <section className="flex min-h-[32rem] flex-col overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm">
            <div className="border-b border-slate-200 bg-gradient-to-r from-slate-50 to-brand-50 px-6 py-6">
              <div className="flex items-center gap-3">
                <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-brand-600 text-white">
                  <MessageSquare className="h-6 w-6" />
                </div>
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Professional chat</p>
                  <h2 className="mt-1 text-xl font-bold text-slate-900">Select a conversation</h2>
                </div>
              </div>
            </div>

            {isLoading ? (
              <div className="flex flex-1 items-center justify-center px-6 py-12">
                <div className="flex items-center gap-3 text-slate-500">
                  <Loader2 className="h-5 w-5 animate-spin text-brand-600" />
                  <span>Loading conversations...</span>
                </div>
              </div>
            ) : hasThreads ? (
              <div className="flex flex-1 items-center justify-center px-8 py-12 text-center">
                <div className="max-w-md">
                  <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-gradient-to-br from-brand-100 to-brand-50 text-brand-600">
                    <Sparkles className="h-7 w-7" />
                  </div>
                  <h3 className="mt-5 text-xl font-semibold text-slate-900">Conversations ready</h3>
                  <p className="mt-3 text-sm leading-6 text-slate-500">
                    Open any booked appointment to read and reply. New messages refresh automatically.
                  </p>
                </div>
              </div>
            ) : (
              <div className="flex flex-1 items-center justify-center px-8 py-12 text-center">
                <div className="max-w-md">
                  <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-slate-100 text-slate-400">
                    <MessageSquare className="h-7 w-7" />
                  </div>
                  <h3 className="mt-5 text-xl font-semibold text-slate-900">No chats yet</h3>
                  <p className="mt-3 text-sm leading-6 text-slate-500">
                    Once a client books with you, their conversation appears here.
                  </p>
                </div>
              </div>
            )}
          </section>
        </div>
      </div>
    </main>
  )
}

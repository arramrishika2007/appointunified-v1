'use client'

import { FormEvent, KeyboardEvent, useEffect, useMemo, useRef, useState } from 'react'
import { useSearchParams } from 'next/navigation'
import Link from 'next/link'
import { Bot, Loader2, MessageSquare, Send, Sparkles } from 'lucide-react'
import { UserShell } from '@/components/layout/UserShell'
import { systemChatApi } from '@/lib/api'
import { useAuthStore } from '@/lib/store'
import { SystemChatMessage } from '@/types'
import toast from 'react-hot-toast'

const QUICK_PROMPTS = [
  'Explain my booking status',
  'What does deposit confirmed mean?',
  'How do I join a virtual appointment?',
  'What happens if I need to reschedule?',
]

export default function SupportAssistantPage() {
  const searchParams = useSearchParams()
  const { isAuthenticated, hasHydrated } = useAuthStore()
  const appointmentId = searchParams.get('appointmentId') ?? undefined

  const [question, setQuestion] = useState('')
  const [loading, setLoading] = useState(false)
  const [messages, setMessages] = useState<SystemChatMessage[]>([])
  const messagesEndRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!hasHydrated) return
    if (!isAuthenticated) {
      window.location.href = '/auth/login?redirect=/dashboard/support'
    }
  }, [hasHydrated, isAuthenticated])

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, loading])

  const contextLabel = useMemo(() => {
    if (appointmentId) return `Appointment ${appointmentId.slice(0, 8)}`
    return 'General support'
  }, [appointmentId])

  const sendQuestion = async (prompt?: string) => {
    const nextQuestion = (prompt ?? question).trim()
    if (!nextQuestion) return

    const token = typeof window !== 'undefined' ? localStorage.getItem('au_access') : null
    if (!token) {
      toast.error('Session expired. Please sign in again.')
      window.location.href = '/auth/login?redirect=/dashboard/support'
      return
    }

    setMessages((prev) => [
      ...prev,
      { question: nextQuestion, answer: '', status: 'PENDING', timestamp: new Date().toISOString() },
    ])

    setLoading(true)
    try {
      const response = await systemChatApi.askQuestion(nextQuestion, appointmentId ? 'appointment' : 'general', appointmentId)
      const answer = response.data?.data?.answer ?? response.data?.answer ?? 'No answer was returned.'
      setMessages((prev) => [
        ...prev.slice(0, -1),
        { question: nextQuestion, answer, status: 'SUCCESS', timestamp: new Date().toISOString() },
      ])
      setQuestion('')
    } catch (error: any) {
      const status = error?.response?.status
      const backendMessage = error?.response?.data?.message ?? error?.response?.data?.error

      if (status === 401) {
        const fallback = 'Session expired. Please sign in again to continue using support assistant.'
        setMessages((prev) => [
          ...prev.slice(0, -1),
          { question: nextQuestion, answer: fallback, status: 'OFFLINE', timestamp: new Date().toISOString() },
        ])
        toast.error('Session expired. Please sign in again.')
        window.location.href = '/auth/login?redirect=/dashboard/support'
        return
      }

      const fallback = typeof backendMessage === 'string' && backendMessage.trim().length > 0
        ? backendMessage
        : 'Support request failed. Please retry in a few seconds.'
      setMessages((prev) => [
        ...prev.slice(0, -1),
        { question: nextQuestion, answer: fallback, status: 'OFFLINE', timestamp: new Date().toISOString() },
      ])
      toast.error('Support request failed')
    } finally {
      setLoading(false)
    }
  }

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    void sendQuestion()
  }

  const handleQuestionKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault()
      if (!loading) {
        void sendQuestion()
      }
    }
  }

  return (
    <UserShell>
      <main className="min-h-screen bg-slate-50">
        <div className="container-page py-8 max-w-4xl space-y-6">
          <section className="overflow-hidden rounded-[2rem] border border-slate-200 bg-slate-900 text-white shadow-xl">
            <div className="grid gap-6 px-6 py-8 md:grid-cols-[1.3fr_0.7fr] md:px-8">
              <div>
                <div className="inline-flex items-center gap-2 rounded-full bg-white/10 px-3 py-1 text-xs font-semibold uppercase tracking-wide text-white/80">
                  <Sparkles size={13} /> System assistant
                </div>
                <h1 className="mt-4 text-3xl font-bold">Ask about bookings, payments, or app behavior</h1>
                <p className="mt-2 max-w-2xl text-sm text-white/75">
                  This page connects to the live system-chat endpoint and can answer context-aware questions about the current booking or general product rules.
                </p>
              </div>

              <div className="rounded-3xl border border-white/10 bg-white/5 p-5">
                <p className="text-xs font-semibold uppercase tracking-wide text-white/60">Context</p>
                <p className="mt-2 text-lg font-semibold">{contextLabel}</p>
                <p className="mt-1 text-sm text-white/70">
                  {appointmentId ? 'The assistant can use the appointment id as context.' : 'No appointment context selected.'}
                </p>
              </div>
            </div>
          </section>

          <section className="grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
            <div className="card p-6">
              <form onSubmit={handleSubmit} className="space-y-4">
                <div>
                  <label className="label flex items-center gap-2">
                    <MessageSquare size={14} /> What do you need help with?
                  </label>
                  <textarea
                    value={question}
                    onChange={(event) => setQuestion(event.target.value)}
                    onKeyDown={handleQuestionKeyDown}
                    rows={4}
                    className="input resize-none"
                    placeholder="Example: Why is my deposit still pending?"
                  />
                  <p className="mt-2 text-xs text-slate-500">Press Enter to send. Use Shift+Enter for a new line.</p>
                </div>

                <div className="flex flex-wrap gap-2">
                  {QUICK_PROMPTS.map((prompt) => (
                    <button
                      key={prompt}
                      type="button"
                      onClick={() => void sendQuestion(prompt)}
                      className="btn-ghost text-xs px-3 py-2"
                      disabled={loading}
                    >
                      {prompt}
                    </button>
                  ))}
                </div>

                <button type="submit" disabled={loading} className="btn-primary w-full py-3 text-base">
                  {loading ? <Loader2 size={16} className="animate-spin" /> : <Send size={16} />} Ask assistant
                </button>
              </form>

              <div className="mt-6 max-h-[540px] space-y-3 overflow-y-auto rounded-2xl border border-slate-100 bg-slate-50/80 p-3">
                {messages.length === 0 ? (
                  <div className="rounded-2xl border border-dashed border-slate-200 bg-white p-8 text-center text-sm text-slate-500">
                    Your answers will appear here.
                  </div>
                ) : (
                  messages.map((message, index) => (
                    <article key={`${message.timestamp}-${index}`} className="space-y-3">
                      <div className="ml-auto w-fit max-w-[90%] rounded-2xl rounded-br-md bg-brand-600 px-4 py-3 text-sm text-white shadow-sm">
                        {message.question}
                      </div>
                      <div className="mr-auto max-w-[92%] rounded-2xl rounded-bl-md border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 shadow-sm">
                        {message.status === 'PENDING' ? (
                          <span className="inline-flex items-center gap-2 text-slate-500">
                            <Loader2 size={14} className="animate-spin" /> Thinking...
                          </span>
                        ) : (
                          message.answer
                        )}
                      </div>
                    </article>
                  ))
                )}
                <div ref={messagesEndRef} />
              </div>
            </div>

            <aside className="space-y-6">
              <div className="card p-6">
                <div className="flex items-center gap-2 text-sm font-semibold text-slate-900">
                  <Bot size={16} className="text-brand-600" /> What the assistant can do
                </div>
                <ul className="mt-4 space-y-3 text-sm text-slate-600">
                  <li>Explain booking status and lifecycle steps.</li>
                  <li>Summarize payment and receipt details.</li>
                  <li>Guide you to chat, workflow, or support pages.</li>
                  <li>Answer general platform policy questions.</li>
                </ul>
              </div>

              <div className="card p-6">
                <h2 className="text-lg font-semibold text-slate-900">Useful pages</h2>
                <div className="mt-4 flex flex-col gap-2">
                  <Link href="/dashboard/bookings" className="btn-ghost text-xs px-3 py-2">My bookings</Link>
                  <Link href="/notifications" className="btn-ghost text-xs px-3 py-2">Notifications</Link>
                  <Link href="/dashboard/workflows" className="btn-ghost text-xs px-3 py-2">Workflows</Link>
                </div>
              </div>
            </aside>
          </section>
        </div>
      </main>
    </UserShell>
  )
}
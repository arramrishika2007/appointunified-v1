'use client'

import Link from 'next/link'
import { useParams } from 'next/navigation'
import { useRouter } from 'next/navigation'
import { useEffect, useMemo, useRef, useState } from 'react'
import useSWR from 'swr'
import { ArrowLeft, Loader2, MessageSquare, SendHorizontal, ShieldCheck } from 'lucide-react'
import { ChatThreadList } from '@/components/chat/ChatThreadList'
import { chatApi } from '@/lib/api'
import { useAuthStore } from '@/lib/store'
import { ChatMessage, ChatThreadItem } from '@/types'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

export default function ProfessionalChatConversationPage() {
  const router = useRouter()
  const params = useParams()
  const appointmentId = params?.appointmentId as string
  const messagesEndRef = useRef<HTMLDivElement>(null)

  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [messageText, setMessageText] = useState('')
  const [loadingMessages, setLoadingMessages] = useState(true)
  const [sending, setSending] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [isTyping, setIsTyping] = useState(false)
  const typingTimeoutRef = useRef<NodeJS.Timeout>()
  const { hasHydrated, isAuthenticated, user } = useAuthStore()

  const currentUserId = user?.id

  const upsertMessage = (incoming: ChatMessage) => {
    setMessages((prev) => {
      const existingIndex = prev.findIndex((msg) => msg.id === incoming.id)
      const next =
        existingIndex >= 0
          ? prev.map((msg, index) => (index === existingIndex ? incoming : msg))
          : [...prev, incoming]

      return [...next].sort(
        (a, b) => new Date(a.sentAt).getTime() - new Date(b.sentAt).getTime()
      )
    })
  }

  useEffect(() => {
    if (hasHydrated && !isAuthenticated) {
      router.replace(`/auth/login?redirect=/dashboard/professional/chats/${appointmentId}`)
    }
  }, [appointmentId, hasHydrated, isAuthenticated, router])

  const shouldFetchThreads = hasHydrated && isAuthenticated

  const { data: threads, isLoading: loadingThreads, mutate: mutateThreads } = useSWR<ChatThreadItem[]>(
    shouldFetchThreads ? '/chat/threads' : null,
    () => chatApi.getThreads().then((res) => res.data ?? []),
    {
      refreshInterval: 5000,
      revalidateOnFocus: true,
    }
  )

  const activeThread = useMemo(
    () => threads?.find((thread) => thread.appointmentId === appointmentId) ?? null,
    [threads, appointmentId]
  )

  useEffect(() => {
    if (!appointmentId) return

    const loadChatHistory = async () => {
      try {
        setLoadingMessages(true)
        setError(null)
        const response = await chatApi.getChatHistory(appointmentId, 0, 50)
        const history = (response.data || []).sort(
          (a: ChatMessage, b: ChatMessage) => new Date(a.sentAt).getTime() - new Date(b.sentAt).getTime()
        )
        setMessages(history)
      } catch (err) {
        console.error('Failed to load chat history:', err)
        setError('Failed to load messages. Please try again.')
      } finally {
        setLoadingMessages(false)
      }
    }

    loadChatHistory()

    const socketUrl = process.env.NEXT_PUBLIC_API_URL
      ? `${process.env.NEXT_PUBLIC_API_URL}/ws`
      : 'http://localhost:8080/ws'

    const stompClient = new Client({
      webSocketFactory: () => new SockJS(socketUrl),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        stompClient.subscribe(`/topic/chat/${appointmentId}`, (message) => {
          if (message.body) {
            const newMsg = JSON.parse(message.body)
            upsertMessage(newMsg)
          }
        })
      },
      onStompError: (frame) => {
        console.error('Broker reported error: ' + frame.headers['message'])
      },
    })

    stompClient.activate()

    return () => {
      stompClient.deactivate()
    }
  }, [appointmentId])

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  useEffect(() => {
    if (messages.length > 0 && currentUserId) {
      const unreadMessages = messages.filter((message) => message.receiverId === currentUserId && !message.isRead)
      if (unreadMessages.length > 0) {
        chatApi.markAsRead(appointmentId).catch((err) =>
          console.error('Failed to mark as read:', err)
        )
      }
    }
  }, [messages, currentUserId, appointmentId])

  const handleSendMessage = async () => {
    if (!messageText.trim() || !appointmentId) return

    try {
      setSending(true)
      const response = await chatApi.sendMessage(appointmentId, messageText)
      upsertMessage(response.data)
      setMessageText('')
      setError(null)
      mutateThreads()
    } catch (err) {
      console.error('Failed to send message:', err)
      setError('Failed to send message. Please try again.')
    } finally {
      setSending(false)
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSendMessage()
    }
  }

  if (!appointmentId) {
    return (
      <div className="flex min-h-[calc(100vh-4rem)] items-center justify-center">
        <p className="text-slate-500">Invalid appointment ID</p>
      </div>
    )
  }

  if (!hasHydrated || !shouldFetchThreads) {
    return (
      <div className="flex min-h-[calc(100vh-4rem)] items-center justify-center bg-slate-50">
        <div className="flex items-center gap-3 text-slate-500">
          <Loader2 className="h-5 w-5 animate-spin text-brand-600" />
          Loading professional conversation...
        </div>
      </div>
    )
  }

  return (
    <main className="min-h-[calc(100vh-4rem)] bg-slate-50">
      <div className="container-page max-w-7xl py-6">
        <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-brand-600">Professional chat</p>
            <h1 className="mt-2 text-3xl font-bold text-slate-900">Client Conversation</h1>
            <p className="mt-2 max-w-2xl text-sm text-slate-500">
              Continue this appointment conversation while staying in your professional dashboard workflow.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <Link
              href="/dashboard/professional/chats"
              className="inline-flex items-center gap-2 rounded-full border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 transition hover:border-brand-300 hover:text-brand-700"
            >
              <ArrowLeft className="h-4 w-4" />
              All chats
            </Link>
          </div>
        </div>

        <div className="grid gap-6 xl:grid-cols-[360px_minmax(0,1fr)]">
          <ChatThreadList
            threads={threads ?? []}
            activeAppointmentId={appointmentId}
            loading={loadingThreads}
            appointmentPathBase="/dashboard/professional/chats"
          />

          <section className="flex min-h-[42rem] flex-col overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm">
            <div className="border-b border-slate-200 px-6 py-5">
              <div className="flex flex-wrap items-center justify-between gap-4">
                <div className="flex items-center gap-3">
                  <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-brand-50 text-brand-700">
                    <MessageSquare className="h-5 w-5" />
                  </div>
                  <div className="min-w-0">
                    <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Booked appointment</p>
                    <h2 className="truncate text-lg font-semibold text-slate-900">
                      {activeThread?.otherParticipantName || 'Appointment chat'}
                    </h2>
                    <p className="truncate text-sm text-slate-500">
                      {activeThread
                        ? `${activeThread.serviceName} • ${activeThread.appointmentStatus.replace('_', ' ')} • ${activeThread.virtual ? 'Virtual' : 'In person'}`
                        : `Appointment ${appointmentId.slice(0, 8)}...`}
                    </p>
                  </div>
                </div>

                {activeThread?.otherParticipantRole && (
                  <span className="inline-flex items-center gap-2 rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600">
                    <ShieldCheck className="h-3.5 w-3.5 text-emerald-600" />
                    {activeThread.otherParticipantRole === 'PROFESSIONAL' ? 'Booked professional' : 'Booked client'}
                  </span>
                )}
              </div>
            </div>

            <div className="flex-1 overflow-y-auto px-4 py-5 sm:px-6">
              {loadingMessages && (
                <div className="flex h-full items-center justify-center py-16">
                  <Loader2 size={28} className="animate-spin text-brand-600" />
                </div>
              )}

              {!loadingMessages && messages.length === 0 && (
                <div className="flex h-full items-center justify-center py-16 text-center">
                  <div className="max-w-md">
                    <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-gradient-to-br from-brand-100 to-brand-50 text-brand-600">
                      <MessageSquare className="h-7 w-7" />
                    </div>
                    <h3 className="mt-5 text-xl font-semibold text-slate-900">Start the conversation</h3>
                    <p className="mt-3 text-sm leading-6 text-slate-500">
                      Send a message to update your client about this appointment.
                    </p>
                  </div>
                </div>
              )}

              {!loadingMessages && messages.length > 0 && (
                <div className="space-y-3">
                  {messages.map((message, index) => {
                    const isMine = message.senderId === currentUserId
                    const prevMessage = index > 0 ? messages[index - 1] : null
                    const nextMessage = index < messages.length - 1 ? messages[index + 1] : null

                    const isSameUserAsNext = nextMessage?.senderId === message.senderId
                    const showAvatar = !isSameUserAsNext

                    const senderInitials =
                      message.senderName
                        .split(' ')
                        .filter(Boolean)
                        .slice(0, 2)
                        .map((part) => part[0]?.toUpperCase())
                        .join('') || '?'

                    const isSameUserAsPrev = prevMessage?.senderId === message.senderId

                    return (
                      <div
                        key={message.id}
                        className={`flex gap-3 ${isMine ? 'justify-end' : 'justify-start'} ${isSameUserAsPrev ? 'mt-1' : 'mt-3'}`}
                      >
                        {!isMine && showAvatar && (
                          <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-slate-200 text-xs font-semibold text-slate-700">
                            {senderInitials}
                          </div>
                        )}
                        {!isMine && !showAvatar && <div className="w-8 shrink-0" />}

                        <div className={`flex flex-col ${isMine ? 'items-end' : 'items-start'}`}>
                          <div
                            className={`max-w-[78vw] rounded-2xl px-4 py-3 shadow-sm sm:max-w-sm lg:max-w-xl ${
                              isMine
                                ? 'rounded-br-md bg-brand-600 text-white'
                                : 'rounded-bl-md border border-slate-200 bg-white text-slate-900'
                            }`}
                          >
                            {message.filtered && (
                              <div className={`mb-2 text-xs font-medium ${isMine ? 'text-brand-100' : 'text-amber-700'}`}>
                                Filtered: {message.filterReason}
                              </div>
                            )}
                            <p className="break-words whitespace-pre-wrap text-sm leading-relaxed">{message.message}</p>
                          </div>
                          <span className={`mt-1 text-xs font-medium ${isMine ? 'text-slate-500' : 'text-slate-400'}`}>
                            {new Date(message.sentAt).toLocaleTimeString([], {
                              hour: '2-digit',
                              minute: '2-digit',
                            })}
                            {message.isRead && isMine && <span className="ml-2">✓✓</span>}
                          </span>
                        </div>
                      </div>
                    )
                  })}

                  {isTyping && (
                    <div className="flex gap-3">
                      <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-slate-200 text-xs font-semibold text-slate-700">
                        {activeThread?.otherParticipantName
                          .split(' ')
                          .filter(Boolean)
                          .slice(0, 2)
                          .map((part) => part[0]?.toUpperCase())
                          .join('') || '?'}
                      </div>
                      <div className="rounded-2xl rounded-bl-md border border-slate-200 bg-white px-4 py-3">
                        <div className="flex items-center gap-1">
                          <span className="inline-block h-2 w-2 animate-bounce rounded-full bg-slate-400 [animation-delay:0ms]" />
                          <span className="inline-block h-2 w-2 animate-bounce rounded-full bg-slate-400 [animation-delay:150ms]" />
                          <span className="inline-block h-2 w-2 animate-bounce rounded-full bg-slate-400 [animation-delay:300ms]" />
                        </div>
                      </div>
                    </div>
                  )}

                  <div ref={messagesEndRef} />
                </div>
              )}
            </div>

            {error && <div className="border-t border-rose-200 bg-rose-50 px-6 py-3 text-sm text-rose-700">{error}</div>}

            <div className="border-t border-slate-200 bg-white px-4 py-4 sm:px-6">
              <div className="space-y-2">
                <div className="flex gap-3">
                  <input
                    type="text"
                    value={messageText}
                    onChange={(e) => {
                      setMessageText(e.target.value)
                      setIsTyping(true)

                      if (typingTimeoutRef.current) {
                        clearTimeout(typingTimeoutRef.current)
                      }

                      typingTimeoutRef.current = setTimeout(() => {
                        setIsTyping(false)
                      }, 1000)
                    }}
                    onKeyDown={handleKeyDown}
                    placeholder="Type your message..."
                    disabled={sending}
                    maxLength={500}
                    className="flex-1 rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-brand-400 focus:ring-2 focus:ring-brand-100 disabled:bg-slate-50 disabled:text-slate-400"
                  />
                  <button
                    onClick={handleSendMessage}
                    disabled={!messageText.trim() || sending}
                    className="inline-flex items-center justify-center gap-2 rounded-xl bg-brand-600 px-4 py-3 text-sm font-semibold text-white transition hover:bg-brand-700 hover:shadow-lg disabled:cursor-not-allowed disabled:bg-slate-300"
                  >
                    {sending ? <Loader2 className="h-4 w-4 animate-spin" /> : <SendHorizontal className="h-4 w-4" />}
                    <span className="hidden sm:inline">{sending ? 'Sending...' : 'Send'}</span>
                  </button>
                </div>
                <div className="flex items-center justify-between px-1">
                  <span className="text-xs text-slate-500">{messageText.length}/500</span>
                  <span className="text-xs text-slate-400">Press Enter to send</span>
                </div>
              </div>
            </div>
          </section>
        </div>
      </div>
    </main>
  )
}

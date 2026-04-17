'use client'

import Link from 'next/link'
import { formatDistanceToNow } from 'date-fns'
import { ChevronRight, Clock3, MessageSquare, Sparkles } from 'lucide-react'
import { ChatThreadItem } from '@/types'

interface ChatThreadListProps {
  threads: ChatThreadItem[]
  activeAppointmentId?: string
  loading?: boolean
  appointmentPathBase?: string
}

const statusStyles: Record<string, string> = {
  SCHEDULED: 'bg-sky-100 text-sky-700',
  IN_QUEUE: 'bg-amber-100 text-amber-800',
  IN_PROGRESS: 'bg-emerald-100 text-emerald-700',
  COMPLETED: 'bg-slate-100 text-slate-600',
  CANCELLED: 'bg-rose-100 text-rose-700',
  NO_SHOW: 'bg-rose-100 text-rose-700',
  EXPIRED: 'bg-slate-100 text-slate-500',
}

function getInitials(name: string) {
  return name
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('') || '?'
}

function formatThreadTime(value?: string) {
  if (!value) return ''
  try {
    return formatDistanceToNow(new Date(value), { addSuffix: true })
  } catch {
    return ''
  }
}

export function ChatThreadList({
  threads,
  activeAppointmentId,
  loading,
  appointmentPathBase = '/chat',
}: ChatThreadListProps) {
  return (
    <aside className="flex min-h-[32rem] flex-col overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm">
      <div className="border-b border-slate-200 bg-gradient-to-br from-slate-950 to-slate-800 px-5 py-5 text-white">
        <div className="flex items-center gap-3">
          <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-white/10 ring-1 ring-white/15">
            <MessageSquare className="h-5 w-5" />
          </div>
          <div>
            <p className="text-xs uppercase tracking-[0.24em] text-slate-300">Booked Chats</p>
            <h2 className="mt-1 text-lg font-semibold">Conversations</h2>
          </div>
        </div>
        <p className="mt-4 max-w-sm text-sm text-slate-300">
          Every booked appointment appears here automatically so you can jump straight into the appointment chat.
        </p>
      </div>

      <div className="flex-1 overflow-y-auto bg-slate-50/70">
        {loading ? (
          <div className="space-y-3 p-4">
            {Array.from({ length: 4 }).map((_, index) => (
              <div key={index} className="rounded-2xl border border-slate-200 bg-white p-4">
                <div className="animate-pulse">
                  <div className="flex items-center gap-3">
                    <div className="h-12 w-12 rounded-2xl bg-slate-200" />
                    <div className="flex-1 space-y-2">
                      <div className="h-4 w-2/3 rounded bg-slate-200" />
                      <div className="h-3 w-1/2 rounded bg-slate-200" />
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : threads.length === 0 ? (
          <div className="flex h-full items-center justify-center p-8 text-center">
            <div className="max-w-sm">
              <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-slate-100 text-slate-400">
                <Sparkles className="h-7 w-7" />
              </div>
              <h3 className="mt-4 text-base font-semibold text-slate-900">No booked chats yet</h3>
              <p className="mt-2 text-sm text-slate-500">
                Once an appointment is booked, it will appear here and you can open the conversation right away.
              </p>
            </div>
          </div>
        ) : (
          <div className="space-y-2 p-3">
            {threads.map((thread) => {
              const isActive = thread.appointmentId === activeAppointmentId
              const initials = getInitials(thread.otherParticipantName)
              const timeLabel = formatThreadTime(thread.lastMessageAt)
              const statusBadge = statusStyles[thread.appointmentStatus] || 'bg-slate-100 text-slate-600'

              return (
                <Link
                  key={thread.appointmentId}
                  href={`${appointmentPathBase}/${thread.appointmentId}`}
                  className={`block rounded-2xl border p-4 transition-all ${
                    isActive
                      ? 'border-brand-300 bg-brand-50 shadow-sm'
                      : 'border-transparent bg-white hover:border-slate-200 hover:bg-slate-50'
                  }`}
                >
                  <div className="flex items-start gap-3">
                    <div className="relative shrink-0">
                      {thread.otherParticipantAvatarUrl ? (
                        <img
                          src={thread.otherParticipantAvatarUrl}
                          alt={thread.otherParticipantName}
                          className="h-12 w-12 rounded-2xl object-cover ring-1 ring-slate-200"
                        />
                      ) : (
                        <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-900 text-sm font-semibold text-white">
                          {initials}
                        </div>
                      )}
                      {thread.unreadCount > 0 && (
                        <span className="absolute -right-1 -top-1 flex h-5 min-w-5 items-center justify-center rounded-full bg-brand-600 px-1.5 text-[11px] font-semibold text-white">
                          {thread.unreadCount}
                        </span>
                      )}
                    </div>

                    <div className="min-w-0 flex-1">
                      <div className="flex items-start justify-between gap-2">
                        <div className="min-w-0">
                          <p className="truncate font-semibold text-slate-900">{thread.otherParticipantName}</p>
                          <p className="mt-0.5 truncate text-xs text-slate-500">
                            {thread.serviceName} • {thread.otherParticipantRole === 'PROFESSIONAL' ? 'Booked professional' : 'Booked client'}
                          </p>
                        </div>
                        <span className="shrink-0 rounded-full bg-slate-100 px-2.5 py-1 text-[11px] font-medium text-slate-600">
                          {timeLabel || 'Just now'}
                        </span>
                      </div>

                      <div className="mt-3 flex items-center gap-2">
                        <span className={`rounded-full px-2.5 py-1 text-[11px] font-medium ${statusBadge}`}>
                          {thread.appointmentStatus.replaceAll('_', ' ')}
                        </span>
                        {thread.virtual && (
                          <span className="rounded-full bg-indigo-100 px-2.5 py-1 text-[11px] font-medium text-indigo-700">
                            Virtual
                          </span>
                        )}
                      </div>

                      <p className="mt-3 overflow-hidden text-sm text-slate-600 [display:-webkit-box] [-webkit-box-orient:vertical] [-webkit-line-clamp:2]">
                        {thread.previewText}
                      </p>
                    </div>

                    <ChevronRight className={`mt-2 h-4 w-4 shrink-0 ${isActive ? 'text-brand-600' : 'text-slate-300'}`} />
                  </div>

                  <div className="mt-3 flex items-center justify-between text-xs text-slate-500">
                    <span className="flex items-center gap-1.5">
                      <Clock3 className="h-3.5 w-3.5" />
                      {thread.startTime ? new Date(thread.startTime).toLocaleDateString([], { month: 'short', day: 'numeric' }) : 'Booked chat'}
                    </span>
                    {isActive && <span className="font-medium text-brand-700">Open conversation</span>}
                  </div>
                </Link>
              )
            })}
          </div>
        )}
      </div>
    </aside>
  )
}
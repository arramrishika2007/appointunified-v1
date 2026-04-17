'use client'

import { useEffect, useMemo, useRef, useState } from 'react'
import { AlertCircle, RefreshCw, Inbox, CheckCheck } from 'lucide-react'
import { notificationsApi } from '@/lib/api'
import { NotificationItem, NotificationType } from '@/types'
import { NotificationPreferences } from '@/components/ui/NotificationPreferences'

const typeColors: Record<NotificationType, string> = {
  APPOINTMENT: 'bg-blue-100 text-blue-700',
  QUEUE: 'bg-orange-100 text-orange-700',
  PAYMENT: 'bg-green-100 text-green-700',
  CHAT: 'bg-indigo-100 text-indigo-700',
  SYSTEM: 'bg-gray-100 text-gray-700',
  VERIFICATION: 'bg-purple-100 text-purple-700',
  WAITLIST: 'bg-pink-100 text-pink-700',
}

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState<NotificationItem[]>([])
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [selectedType, setSelectedType] = useState<'ALL' | NotificationType>('ALL')
  const refreshTimerRef = useRef<number | null>(null)

  const loadNotifications = async (silent = false) => {
    try {
      if (!silent) {
        setLoading(true)
        setError(null)
      } else {
        setRefreshing(true)
      }
      const response = await notificationsApi.getMyNotifications(0, 100)
      const pageData = response.data?.data
      const content = pageData?.content || []
      setNotifications(content)
      if (silent) {
        setError(null)
      }
    } catch {
      if (!silent) {
        setError('Unable to load notifications right now. Please try again.')
      }
    } finally {
      if (!silent) {
        setLoading(false)
      } else {
        setRefreshing(false)
      }
    }
  }

  useEffect(() => {
    void loadNotifications(false)

    refreshTimerRef.current = window.setInterval(() => {
      void loadNotifications(true)
    }, 15000)

    return () => {
      if (refreshTimerRef.current) {
        window.clearInterval(refreshTimerRef.current)
      }
    }
  }, [])

  const filteredNotifications = useMemo(() => {
    if (selectedType === 'ALL') return notifications
    return notifications.filter(n => n.type === selectedType)
  }, [notifications, selectedType])

  const groupedByDate = useMemo(() => {
    const groups: Record<string, NotificationItem[]> = {}
    filteredNotifications.forEach(notification => {
      const dateKey = new Date(notification.createdAt).toLocaleDateString()
      if (!groups[dateKey]) groups[dateKey] = []
      groups[dateKey].push(notification)
    })
    return groups
  }, [filteredNotifications])

  const markAsRead = async (id: string) => {
    try {
      await notificationsApi.markAsRead(id)
      setNotifications(prev =>
        prev.map(n => (n.id === id ? { ...n, isRead: true, readAt: new Date().toISOString() } : n))
      )
    } catch {
      setError('Failed to mark notification as read')
    }
  }

  const markAllAsRead = async () => {
    try {
      await notificationsApi.markAllAsRead()
      setNotifications(prev => prev.map(n => ({ ...n, isRead: true, readAt: new Date().toISOString() })))
    } catch {
      setError('Failed to mark all notifications as read')
    }
  }

  const unreadCount = notifications.filter(n => !n.isRead).length

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-b from-slate-50 to-slate-100 p-6">
        <div className="mx-auto max-w-4xl rounded-2xl border border-slate-200 bg-white p-8 shadow-sm">
          <div className="flex items-center gap-3 text-slate-700">
            <RefreshCw className="h-5 w-5 animate-spin" />
            <span>Loading notifications...</span>
          </div>
        </div>
      </div>
    )
  }

  return (
    <main className="min-h-screen bg-gradient-to-b from-slate-50 to-slate-100 px-6 py-6">
      <div className="mx-auto max-w-4xl">
        <div className="mb-6 flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-slate-800">Notifications</h1>
            <p className="text-sm text-slate-600">
              {unreadCount} unread of {notifications.length} total{refreshing ? ' · syncing' : ''}
            </p>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => void loadNotifications(false)}
              className="inline-flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
            >
              <RefreshCw className="h-4 w-4" />
              Refresh
            </button>
            <button
              onClick={markAllAsRead}
              disabled={unreadCount === 0}
              className="inline-flex items-center gap-2 rounded-lg bg-slate-800 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:cursor-not-allowed disabled:bg-slate-400"
            >
              <CheckCheck className="h-4 w-4" />
              Mark All Read
            </button>
          </div>
        </div>

        <div className="mb-6 flex flex-wrap gap-2">
          <FilterButton label="All" active={selectedType === 'ALL'} onClick={() => setSelectedType('ALL')} />
          {Object.keys(typeColors).map(type => (
            <FilterButton
              key={type}
              label={type}
              active={selectedType === type}
              onClick={() => setSelectedType(type as NotificationType)}
            />
          ))}
        </div>

        {error && (
          <div className="mb-4 flex items-start gap-3 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-amber-800">
            <AlertCircle className="mt-0.5 h-5 w-5 shrink-0" />
            <div>
              <p className="font-medium">Notifications could not be refreshed</p>
              <p className="text-sm text-amber-700">{error}</p>
            </div>
          </div>
        )}

        {!error && Object.keys(groupedByDate).length === 0 && (
          <div className="rounded-2xl border border-slate-200 bg-white p-10 text-center text-slate-500 shadow-sm">
            <Inbox className="mx-auto mb-3 h-10 w-10 text-slate-300" />
            <p className="text-base font-medium text-slate-700">No notifications yet</p>
            <p className="mt-1 text-sm text-slate-500">You’ll see booking, payment, queue, and system updates here.</p>
          </div>
        )}

        <div className="space-y-6">
          {Object.entries(groupedByDate).map(([date, items]) => (
            <section key={date}>
              <h2 className="mb-2 text-sm font-semibold text-slate-600">{date}</h2>
              <div className="space-y-2">
                {items.map(notification => (
                  <article
                    key={notification.id}
                    className={`rounded-xl border bg-white p-4 shadow-sm transition ${
                      notification.isRead ? 'border-slate-200' : 'border-blue-300 ring-1 ring-blue-100'
                    }`}
                  >
                    <div className="mb-2 flex items-start justify-between gap-2">
                      <div>
                        <h3 className="font-semibold text-slate-800">{notification.title}</h3>
                        <p className="mt-1 text-sm text-slate-600">{notification.message}</p>
                      </div>
                      <span className={`rounded-full px-2 py-1 text-xs font-medium ${typeColors[notification.type]}`}>
                        {notification.type}
                      </span>
                    </div>

                    <div className="flex items-center justify-between">
                      <span className="text-xs text-slate-500">
                        {new Date(notification.createdAt).toLocaleTimeString([], {
                          hour: '2-digit',
                          minute: '2-digit',
                        })}
                      </span>
                      <div className="flex items-center gap-2">
                        {!notification.isRead && (
                          <button
                            onClick={() => markAsRead(notification.id)}
                            className="text-xs font-medium text-blue-600 hover:text-blue-700"
                          >
                            Mark Read
                          </button>
                        )}
                        {notification.actionUrl && (
                          <a
                            href={notification.actionUrl}
                            className="text-xs font-medium text-slate-700 hover:text-slate-900"
                          >
                            Open
                          </a>
                        )}
                      </div>
                    </div>
                  </article>
                ))}
              </div>
            </section>
          ))}
        </div>

        <section className="mt-8">
          <NotificationPreferences />
        </section>
      </div>
    </main>
  )
}

function FilterButton({
  label,
  active,
  onClick,
}: {
  label: string
  active: boolean
  onClick: () => void
}) {
  return (
    <button
      onClick={onClick}
      className={`rounded-full px-3 py-1 text-xs font-medium transition ${
        active
          ? 'bg-slate-800 text-white'
          : 'bg-white text-slate-700 hover:bg-slate-100 border border-slate-200'
      }`}
    >
      {label}
    </button>
  )
}

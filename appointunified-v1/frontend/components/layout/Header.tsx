'use client'

import { useEffect, useRef, useState } from 'react'
import Image from 'next/image'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import useSWR from 'swr'
import { Bell, Menu, ChevronDown, LogOut, Settings, UserCircle2, MessageSquare } from 'lucide-react'
import { chatApi, notificationsApi } from '@/lib/api'
import { useAuthStore } from '@/lib/store'
import { cn, getInitials } from '@/lib/utils'

interface HeaderProps {
  collapsed: boolean
  onToggleCollapse: () => void
  onOpenMobileNav: () => void
  navRole?: 'USER' | 'PROFESSIONAL' | 'ADMIN' | 'SUPER_ADMIN'
}

export function Header({ collapsed, onToggleCollapse, onOpenMobileNav, navRole = 'USER' }: HeaderProps) {
  const router = useRouter()
  const { user, logout, isAuthenticated, hasHydrated } = useAuthStore()
  const [menuOpen, setMenuOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)
  const canFetchCounts = hasHydrated && isAuthenticated && typeof window !== 'undefined' && !!localStorage.getItem('au_access')

  const { data: notificationCount = 0 } = useSWR<number>(
    canFetchCounts ? '/api/notifications/me/unread-count' : null,
    () => notificationsApi.getUnreadCount().then((res) => Number(res.data?.data?.unreadCount ?? res.data?.data ?? 0)),
    {
    refreshInterval: 15000,
    revalidateOnFocus: true,
    }
  )

  const { data: chatCount = 0 } = useSWR<number>(
    canFetchCounts ? '/api/chat/unread-count' : null,
    () => chatApi.getUnreadCount().then((res) => Number(res.data?.unreadCount ?? res.data?.data?.unreadCount ?? 0)),
    {
    refreshInterval: 15000,
    revalidateOnFocus: true,
    }
  )

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setMenuOpen(false)
      }
    }

    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const handleLogout = () => {
    logout()
    router.push('/auth/login')
  }

  const roleLabel =
    navRole === 'PROFESSIONAL'
      ? 'Professional'
      : navRole === 'ADMIN'
        ? 'Admin'
        : navRole === 'SUPER_ADMIN'
          ? 'Super Admin'
          : 'User'
  const homeHref =
    navRole === 'PROFESSIONAL'
      ? '/professional/dashboard'
      : navRole === 'ADMIN'
        ? '/admin/dashboard'
        : navRole === 'SUPER_ADMIN'
          ? '/super-admin/dashboard'
          : '/dashboard'
  const profileHref =
    navRole === 'PROFESSIONAL'
      ? '/dashboard/professional/profile'
      : navRole === 'ADMIN'
        ? '/dashboard/admin/profile'
        : navRole === 'SUPER_ADMIN'
          ? '/dashboard/superadmin/profile'
          : '/profile'
  const settingsHref =
    navRole === 'PROFESSIONAL'
      ? '/dashboard/professional/settings'
      : navRole === 'ADMIN'
        ? '/dashboard/admin/settings'
        : navRole === 'SUPER_ADMIN'
          ? '/dashboard/superadmin/config'
          : '/settings'
  const notificationsHref =
    navRole === 'PROFESSIONAL'
      ? '/dashboard/professional/notifications'
      : '/notifications'
  const chatHref =
    navRole === 'PROFESSIONAL'
      ? '/dashboard/professional/chats'
      : '/chat'

  return (
    <header className="fixed inset-x-0 top-0 z-50 h-16 border-b border-slate-200 bg-white/95 backdrop-blur-xl shadow-sm">
      <div className={cn('flex h-full items-center justify-between gap-3 px-3 sm:px-4 lg:px-6')}>
        <div className="flex items-center gap-2">
          <button onClick={onOpenMobileNav} className="rounded-xl border border-slate-200 p-2 text-slate-500 lg:hidden" aria-label="Open navigation">
            <Menu size={18} />
          </button>
          <button onClick={onToggleCollapse} className="hidden rounded-xl border border-slate-200 p-2 text-slate-500 lg:inline-flex" aria-label="Toggle navigation width">
            <Menu size={18} />
          </button>
          <Link href={homeHref} className="hidden items-center gap-2 sm:flex">
            <div className="flex h-10 w-10 items-center justify-center overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
              <Image src="/brand-logo.svg" alt="ABS logo" width={34} height={34} className="h-8 w-8 object-contain" priority />
            </div>
            <div>
              <p className="text-sm font-semibold text-slate-900">AppointUnified</p>
              <p className="text-[11px] uppercase tracking-[0.24em] text-slate-400">{roleLabel}</p>
            </div>
          </Link>
        </div>

        <div className="flex items-center gap-2 sm:gap-3">
          <span className="hidden rounded-full bg-brand-50 px-2.5 py-1 text-[11px] font-semibold text-brand-700 sm:inline-flex">{roleLabel}</span>
          <Link href={notificationsHref} className="relative rounded-xl border border-slate-200 p-2.5 text-slate-600 hover:bg-slate-50" aria-label="Open notifications">
            <Bell size={17} />
            {notificationCount > 0 && <span className="absolute right-1.5 top-1.5 h-2.5 w-2.5 rounded-full bg-rose-500" />}
          </Link>
          <Link href={chatHref} className="relative rounded-xl border border-slate-200 p-2.5 text-slate-600 hover:bg-slate-50" aria-label="Open chats">
            <MessageSquare size={17} />
            {chatCount > 0 && <span className="absolute right-1.5 top-1.5 h-2.5 w-2.5 rounded-full bg-brand-500" />}
          </Link>

          <div ref={menuRef} className="relative">
            <button onClick={() => setMenuOpen((open) => !open)} className="flex items-center gap-2 rounded-2xl border border-slate-200 bg-white px-2.5 py-2 text-left shadow-sm hover:bg-slate-50">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-brand-100 text-brand-700 font-semibold">
                {user?.avatarUrl ? (
                  <img src={user.avatarUrl} alt={user.fullName} className="h-full w-full rounded-xl object-cover" />
                ) : (
                  getInitials(user?.fullName || 'User')
                )}
              </div>
              <div className="hidden sm:block">
                <p className="text-sm font-semibold text-slate-900">{user?.fullName || 'User'}</p>
                <p className="text-[11px] text-slate-500">{roleLabel}</p>
              </div>
              <ChevronDown size={14} className="hidden text-slate-400 sm:block" />
            </button>

            {menuOpen && (
              <div className="absolute right-0 mt-2 w-56 rounded-2xl border border-slate-200 bg-white p-2 shadow-xl">
                <div className="px-3 py-2">
                  <p className="text-sm font-semibold text-slate-900">{user?.fullName || 'User'}</p>
                  <p className="text-xs text-slate-500">{user?.email || user?.phone || 'Account'}</p>
                  <span className="mt-2 inline-flex rounded-full bg-brand-50 px-2.5 py-1 text-[11px] font-semibold text-brand-700">{roleLabel}</span>
                </div>
                <div className="my-2 h-px bg-slate-100" />
                <Link href={profileHref} className="flex items-center gap-2 rounded-xl px-3 py-2 text-sm text-slate-700 hover:bg-slate-50" onClick={() => setMenuOpen(false)}>
                  <UserCircle2 size={16} /> Profile
                </Link>
                <Link href={settingsHref} className="flex items-center gap-2 rounded-xl px-3 py-2 text-sm text-slate-700 hover:bg-slate-50" onClick={() => setMenuOpen(false)}>
                  <Settings size={16} /> Settings
                </Link>
                <div className="my-2 h-px bg-slate-100" />
                <button onClick={handleLogout} className="flex w-full items-center gap-2 rounded-xl px-3 py-2 text-sm text-slate-700 hover:bg-rose-50 hover:text-rose-600">
                  <LogOut size={16} /> Logout
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  )
}

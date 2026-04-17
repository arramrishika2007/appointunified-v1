'use client'

import Link from 'next/link'
import { usePathname, useRouter } from 'next/navigation'
import { Bell, BookOpen, ChevronLeft, ChevronRight, HelpCircle, Home, LogOut, MessageSquare, Settings, Workflow, Wallet, CalendarClock } from 'lucide-react'
import { useAuthStore } from '@/lib/store'
import { cn } from '@/lib/utils'

interface SideNavProps {
  collapsed: boolean
  onToggleCollapse: () => void
  mobileOpen?: boolean
  onCloseMobile?: () => void
  navRole?: 'USER' | 'PROFESSIONAL' | 'ADMIN' | 'SUPER_ADMIN'
}

const USER_NAV_SECTIONS = [
  {
    title: 'Discover',
    items: [
      { href: '/home', label: 'Home', icon: Home },
      { href: '/healthcare', label: 'Healthcare', icon: BookOpen },
      { href: '/government', label: 'Government', icon: CalendarClock },
      { href: '/services', label: 'Services', icon: Workflow },
    ],
  },
  {
    title: 'My Activity',
    items: [
      { href: '/bookings', label: 'My Bookings', icon: BookOpen },
      { href: '/queue', label: 'My Queue', icon: CalendarClock },
      { href: '/dashboard/workflows', label: 'My Workflows', icon: Workflow },
      { href: '/payments', label: 'Payments', icon: Wallet },
    ],
  },
  {
    title: 'Communication',
    items: [
      { href: '/chat', label: 'Chats', icon: MessageSquare },
      { href: '/dashboard/support', label: 'Support Bot', icon: HelpCircle },
      { href: '/notifications', label: 'Notifications', icon: Bell },
    ],
  },
]

const PROFESSIONAL_NAV_SECTIONS = [
  {
    title: 'Operations',
    items: [
      { href: '/professional/dashboard', label: 'Dashboard', icon: Home },
      { href: '/professional/queue', label: 'Queue Control', icon: CalendarClock },
      { href: '/professional/services', label: 'Services', icon: Workflow },
      { href: '/professional/exceptions', label: 'Schedule Exceptions', icon: CalendarClock },
    ],
  },
  {
    title: 'Professional',
    items: [
      { href: '/dashboard/professional/profile', label: 'Profile', icon: BookOpen },
      { href: '/dashboard/professional/settings', label: 'Settings', icon: Settings },
      { href: '/professional/verification', label: 'Verification', icon: HelpCircle },
    ],
  },
  {
    title: 'Communication',
    items: [
      { href: '/dashboard/professional/chats', label: 'Chats', icon: MessageSquare },
      { href: '/notifications', label: 'Notifications', icon: Bell },
    ],
  },
]

const ADMIN_NAV_SECTIONS = [
  {
    title: 'Verification',
    items: [
      { href: '/admin/dashboard', label: 'Verification Queue', icon: HelpCircle },
      { href: '/admin/verification', label: 'Review Docs', icon: BookOpen },
      { href: '/admin/complaints', label: 'Complaints', icon: HelpCircle },
    ],
  },
  {
    title: 'Admin',
    items: [
      { href: '/dashboard/admin/analytics', label: 'Analytics', icon: Workflow },
      { href: '/dashboard/admin/exports', label: 'Exports', icon: Wallet },
      { href: '/notifications', label: 'Notifications', icon: Bell },
    ],
  },
]

const SUPER_ADMIN_NAV_SECTIONS = [
  {
    title: 'Platform',
    items: [
      { href: '/super-admin/dashboard', label: 'KPI Board', icon: Home },
      { href: '/super-admin/verifications', label: 'Verification Audit', icon: HelpCircle },
      { href: '/dashboard/superadmin/fraud', label: 'Fraud Console', icon: MessageSquare },
      { href: '/dashboard/superadmin/flags', label: 'Feature Flags', icon: Workflow },
    ],
  },
  {
    title: 'System',
    items: [
      { href: '/dashboard/superadmin/analytics', label: 'Analytics', icon: CalendarClock },
      { href: '/dashboard/superadmin/exports', label: 'Exports', icon: Wallet },
      { href: '/notifications', label: 'Notifications', icon: Bell },
    ],
  },
]

export function SideNav({ collapsed, onToggleCollapse, mobileOpen = false, onCloseMobile, navRole = 'USER' }: SideNavProps) {
  const pathname = usePathname()
  const router = useRouter()
  const { logout, user } = useAuthStore()
  const navSections =
    navRole === 'PROFESSIONAL'
      ? PROFESSIONAL_NAV_SECTIONS
      : navRole === 'ADMIN'
        ? ADMIN_NAV_SECTIONS
        : navRole === 'SUPER_ADMIN'
          ? SUPER_ADMIN_NAV_SECTIONS
          : USER_NAV_SECTIONS
  const roleLabel =
    navRole === 'PROFESSIONAL'
      ? 'Professional'
      : navRole === 'ADMIN'
        ? 'Admin'
        : navRole === 'SUPER_ADMIN'
          ? 'Super Admin'
          : 'User'
  const settingsHref =
    navRole === 'PROFESSIONAL'
      ? '/dashboard/professional/settings'
      : navRole === 'ADMIN'
        ? '/dashboard/admin/settings'
        : navRole === 'SUPER_ADMIN'
          ? '/dashboard/superadmin/config'
          : '/settings'

  const handleLogout = () => {
    logout()
    router.push('/auth/login')
  }

  const shellWidth = collapsed ? 'w-16' : 'w-60'

  return (
    <>
      <aside
        className={cn(
          'fixed left-0 top-16 z-40 hidden h-[calc(100vh-4rem)] border-r border-slate-200 bg-white/95 backdrop-blur-xl shadow-sm lg:flex flex-col transition-all duration-300',
          shellWidth
        )}
      >
        <div className="flex items-center justify-between gap-2 border-b border-slate-100 px-4 py-4">
          {!collapsed ? (
            <div>
              <p className="text-sm font-semibold text-slate-900">AppointUnified</p>
              <p className="text-[11px] uppercase tracking-[0.24em] text-slate-400">{roleLabel}</p>
            </div>
          ) : (
            <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-400">AU</p>
          )}

          <button
            onClick={onToggleCollapse}
            className="rounded-lg border border-slate-200 p-2 text-slate-500 hover:bg-slate-50 hover:text-slate-900"
            aria-label={collapsed ? 'Expand navigation' : 'Collapse navigation'}
          >
            {collapsed ? <ChevronRight size={16} /> : <ChevronLeft size={16} />}
          </button>
        </div>

        <nav className="flex-1 overflow-y-auto px-3 py-4">
          {navSections.map((section) => (
            <div key={section.title} className="mb-5 last:mb-0">
              {!collapsed && (
                <p className="px-3 pb-2 text-[11px] font-semibold uppercase tracking-[0.22em] text-slate-400">
                  {section.title}
                </p>
              )}
              <div className="space-y-1">
                {section.items.map((item) => {
                  const active = pathname === item.href || pathname.startsWith(`${item.href}/`)
                  const Icon = item.icon

                  return (
                    <Link
                      key={item.href}
                      href={item.href}
                      title={collapsed ? item.label : undefined}
                      className={cn(
                        'flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-all',
                        active ? 'border-l-4 border-brand-600 bg-brand-50 text-brand-700' : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900',
                        collapsed && 'justify-center px-2'
                      )}
                    >
                      <Icon size={16} />
                      {!collapsed && <span>{item.label}</span>}
                    </Link>
                  )
                })}
              </div>
            </div>
          ))}
        </nav>

        <div className="mt-auto border-t border-slate-100 p-3">
          <div className={cn('mb-3 rounded-2xl bg-slate-50 p-3', collapsed && 'p-2 text-center')}>
            {!collapsed ? (
              <>
                <p className="text-sm font-semibold text-slate-900">{user?.fullName || 'User'}</p>
                <p className="text-xs text-slate-500">Role: {roleLabel}</p>
              </>
            ) : (
              <p className="text-xs font-semibold text-slate-500">{roleLabel}</p>
            )}
          </div>

          <div className="space-y-1">
            <Link href={settingsHref} className={cn('flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium text-slate-600 hover:bg-slate-100 hover:text-slate-900', collapsed && 'justify-center px-2')}>
              <Settings size={16} />
              {!collapsed && <span>Settings</span>}
            </Link>
            <button onClick={handleLogout} className={cn('flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium text-slate-600 hover:bg-red-50 hover:text-red-600', collapsed && 'justify-center px-2')}>
              <LogOut size={16} />
              {!collapsed && <span>Logout</span>}
            </button>
          </div>
        </div>
      </aside>

      {mobileOpen && (
        <>
          <button aria-label="Close navigation" onClick={onCloseMobile} className="fixed inset-0 z-40 bg-slate-950/40 lg:hidden" />
          <aside className="fixed left-0 top-16 z-50 flex h-[calc(100vh-4rem)] w-72 flex-col border-r border-slate-200 bg-white shadow-2xl lg:hidden">
            <div className="flex items-center justify-between border-b border-slate-100 px-4 py-4">
              <div>
                <p className="text-sm font-semibold text-slate-900">AppointUnified</p>
                <p className="text-[11px] uppercase tracking-[0.24em] text-slate-400">{roleLabel}</p>
              </div>
              <button onClick={onCloseMobile} title="Close navigation" aria-label="Close navigation" className="rounded-lg border border-slate-200 p-2 text-slate-500">
                <ChevronLeft size={16} />
              </button>
            </div>
            <nav className="flex-1 overflow-y-auto px-3 py-4">
              {navSections.map((section) => (
                <div key={section.title} className="mb-5">
                  <p className="px-3 pb-2 text-[11px] font-semibold uppercase tracking-[0.22em] text-slate-400">{section.title}</p>
                  <div className="space-y-1">
                    {section.items.map((item) => {
                      const active = pathname === item.href || pathname.startsWith(`${item.href}/`)
                      const Icon = item.icon

                      return (
                        <Link
                          key={item.href}
                          href={item.href}
                          onClick={onCloseMobile}
                          className={cn(
                            'flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-all',
                            active ? 'border-l-4 border-brand-600 bg-brand-50 text-brand-700' : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
                          )}
                        >
                          <Icon size={16} />
                          <span>{item.label}</span>
                        </Link>
                      )
                    })}
                  </div>
                </div>
              ))}
            </nav>
            <div className="border-t border-slate-100 p-3">
              <Link href={settingsHref} onClick={onCloseMobile} className="flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium text-slate-600 hover:bg-slate-100 hover:text-slate-900">
                <Settings size={16} /> Settings
              </Link>
              {navRole === 'USER' && (
                <Link href="/dashboard/support" onClick={onCloseMobile} className="mt-1 flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium text-slate-600 hover:bg-slate-100 hover:text-slate-900">
                  <HelpCircle size={16} /> Support Bot
                </Link>
              )}
              <button onClick={handleLogout} className="mt-1 flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium text-slate-600 hover:bg-red-50 hover:text-red-600">
                <LogOut size={16} /> Logout
              </button>
            </div>
          </aside>
        </>
      )}
    </>
  )
}

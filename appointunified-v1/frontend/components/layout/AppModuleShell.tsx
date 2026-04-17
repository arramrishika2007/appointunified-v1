'use client'

import { ReactNode } from 'react'
import { usePathname } from 'next/navigation'
import { useAuthStore } from '@/lib/store'
import { UserShell } from '@/components/layout/UserShell'
import { ProfessionalShell } from '@/components/layout/ProfessionalShell'
import { AdminShell } from '@/components/layout/AdminShell'
import { SuperAdminShell } from '@/components/layout/SuperAdminShell'
import { PushNotificationBootstrap } from '@/components/notifications/PushNotificationBootstrap'

const USER_PREFIXES = [
  '/dashboard',
  '/bookings',
  '/chat',
  '/notifications',
  '/profile',
  '/settings',
  '/home',
  '/healthcare',
  '/government',
  '/services',
  '/explore',
  '/payments',
  '/queue',
]

const PROFESSIONAL_PREFIXES = [
  '/professional/dashboard',
  '/professional/queue',
  '/professional/services',
  '/professional/exceptions',
  '/professional/verification',
  '/dashboard/professional',
]

const ADMIN_PREFIXES = [
  '/admin',
  '/dashboard/admin',
]

const SUPER_ADMIN_PREFIXES = [
  '/super-admin',
  '/dashboard/superadmin',
]

function matchesPrefix(pathname: string, prefixes: string[]) {
  return prefixes.some((prefix) => pathname === prefix || pathname.startsWith(`${prefix}/`))
}

export function AppModuleShell({ children }: { children: ReactNode }) {
  const pathname = usePathname()
  const { hasHydrated, user } = useAuthStore()

  if (pathname.startsWith('/auth') || pathname.startsWith('/meeting') || pathname.startsWith('/provider')) {
    return <>{children}</>
  }

  if (!hasHydrated) {
    return <>{children}</>
  }

  if (pathname.startsWith('/notifications')) {
    if (user?.role === 'SUPER_ADMIN') {
      return (
        <>
          <PushNotificationBootstrap />
          <SuperAdminShell>{children}</SuperAdminShell>
        </>
      )
    }

    if (user?.role === 'ADMIN') {
      return (
        <>
          <PushNotificationBootstrap />
          <AdminShell>{children}</AdminShell>
        </>
      )
    }

    if (user?.role === 'PROFESSIONAL') {
      return (
        <>
          <PushNotificationBootstrap />
          <ProfessionalShell>{children}</ProfessionalShell>
        </>
      )
    }

    return (
      <>
        <PushNotificationBootstrap />
        <UserShell>{children}</UserShell>
      </>
    )
  }

  if (matchesPrefix(pathname, SUPER_ADMIN_PREFIXES)) {
    return (
      <>
        <PushNotificationBootstrap />
        <SuperAdminShell>{children}</SuperAdminShell>
      </>
    )
  }

  if (matchesPrefix(pathname, ADMIN_PREFIXES)) {
    return (
      <>
        <PushNotificationBootstrap />
        <AdminShell>{children}</AdminShell>
      </>
    )
  }

  if (matchesPrefix(pathname, PROFESSIONAL_PREFIXES)) {
    return (
      <>
        <PushNotificationBootstrap />
        <ProfessionalShell>{children}</ProfessionalShell>
      </>
    )
  }

  if (matchesPrefix(pathname, USER_PREFIXES)) {
    return (
      <>
        <PushNotificationBootstrap />
        <UserShell>{children}</UserShell>
      </>
    )
  }

  return (
    <>
      <PushNotificationBootstrap />
      {children}
    </>
  )
}

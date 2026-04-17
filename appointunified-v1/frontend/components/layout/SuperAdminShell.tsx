'use client'

import { ReactNode, useEffect, useState } from 'react'
import { Header } from '@/components/layout/Header'
import { SideNav } from '@/components/layout/SideNav'
import { ShellProvider, useShellRole } from '@/components/layout/ShellContext'

export function SuperAdminShell({ children }: { children: ReactNode }) {
  const activeShellRole = useShellRole()
  const [collapsed, setCollapsed] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)

  useEffect(() => {
    const saved = window.localStorage.getItem('au_superadmin_nav_collapsed')
    if (saved) {
      setCollapsed(saved === '1')
    }
  }, [])

  useEffect(() => {
    window.localStorage.setItem('au_superadmin_nav_collapsed', collapsed ? '1' : '0')
  }, [collapsed])

  useEffect(() => {
    document.body.style.overflow = mobileOpen ? 'hidden' : ''
    return () => {
      document.body.style.overflow = ''
    }
  }, [mobileOpen])

  if (activeShellRole === 'SUPER_ADMIN') {
    return <>{children}</>
  }

  return (
    <ShellProvider role="SUPER_ADMIN">
      <div className="min-h-screen bg-slate-50">
        <Header
          collapsed={collapsed}
          onToggleCollapse={() => setCollapsed((value) => !value)}
          onOpenMobileNav={() => setMobileOpen(true)}
          navRole="SUPER_ADMIN"
        />
        <SideNav
          collapsed={collapsed}
          onToggleCollapse={() => setCollapsed((value) => !value)}
          mobileOpen={mobileOpen}
          onCloseMobile={() => setMobileOpen(false)}
          navRole="SUPER_ADMIN"
        />
        <div className={collapsed ? 'lg:pl-16' : 'lg:pl-60'}>
          <div className="pt-16">{children}</div>
        </div>
      </div>
    </ShellProvider>
  )
}

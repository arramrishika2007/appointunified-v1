'use client'

import { useEffect, useState } from 'react'

/**
 * Thin hydration guard — wraps any client component that reads from
 * localStorage (auth store, tokens) to prevent SSR/hydration mismatch.
 * Renders nothing on the server, children on the client.
 */
export function ClientOnly({ children, fallback = null }: {
  children: React.ReactNode
  fallback?: React.ReactNode
}) {
  const [mounted, setMounted] = useState(false)
  useEffect(() => setMounted(true), [])
  return mounted ? <>{children}</> : <>{fallback}</>
}

/**
 * Full-page loading skeleton used while checking auth state.
 */
export function PageSkeleton() {
  return (
    <div className="min-h-screen bg-slate-50 animate-pulse">
      {/* Navbar skeleton */}
      <div className="h-16 bg-white border-b border-slate-200 flex items-center px-6 gap-4">
        <div className="h-8 w-8 rounded-lg bg-slate-200" />
        <div className="h-4 w-32 rounded bg-slate-200" />
        <div className="ml-auto flex gap-3">
          <div className="h-8 w-20 rounded-lg bg-slate-200" />
          <div className="h-8 w-28 rounded-lg bg-slate-200" />
        </div>
      </div>
      {/* Content skeleton */}
      <div className="container-page py-10 space-y-4">
        <div className="h-8 w-48 rounded bg-slate-200" />
        <div className="h-4 w-72 rounded bg-slate-200" />
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5 mt-8">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="rounded-2xl bg-white border border-slate-200 p-5 space-y-3">
              <div className="flex gap-3">
                <div className="h-14 w-14 rounded-xl bg-slate-200" />
                <div className="flex-1 space-y-2">
                  <div className="h-4 w-3/4 rounded bg-slate-200" />
                  <div className="h-3 w-1/2 rounded bg-slate-200" />
                </div>
              </div>
              <div className="h-8 w-full rounded-lg bg-slate-200" />
              <div className="h-10 w-full rounded-lg bg-slate-200" />
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

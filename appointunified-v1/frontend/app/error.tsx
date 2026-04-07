'use client'

import { useEffect } from 'react'
import Link from 'next/link'

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string }
  reset: () => void
}) {
  useEffect(() => {
    console.error('App error:', error)
  }, [error])

  return (
    <main className="min-h-[70vh] flex items-center justify-center p-4">
      <div className="text-center max-w-sm">
        <div className="text-5xl mb-4">⚠️</div>
        <h1 className="text-2xl font-bold text-slate-900 mb-2">Something went wrong</h1>
        <p className="text-slate-500 text-sm mb-8">
          An unexpected error occurred. Please try again or go back home.
        </p>
        <div className="flex flex-col sm:flex-row gap-3 justify-center">
          <button onClick={reset} className="btn-primary">Try again</button>
          <Link href="/" className="btn-secondary">Go home</Link>
        </div>
        {process.env.NODE_ENV === 'development' && (
          <details className="mt-6 text-left text-xs text-red-600 bg-red-50 rounded-lg p-3">
            <summary className="cursor-pointer font-medium mb-1">Error details</summary>
            <pre className="whitespace-pre-wrap break-all">{error.message}</pre>
          </details>
        )}
      </div>
    </main>
  )
}

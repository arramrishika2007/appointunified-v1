'use client'

import { FormEvent, useEffect, useMemo, useState } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { Loader2, Video } from 'lucide-react'
import { appointmentsApi } from '@/lib/api'

const TOKEN_PATTERN = /^[A-Za-z0-9]{6,12}$/

export default function JoinMeetingPage() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const [token, setToken] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const fromQuery = searchParams.get('token')
    if (fromQuery) {
      setToken(fromQuery)
    }
  }, [searchParams])

  const normalizedToken = useMemo(() => token.trim().toUpperCase(), [token])
  const isValid = TOKEN_PATTERN.test(normalizedToken)

  const onSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!isValid) {
      return
    }

    setSubmitting(true)
    setError(null)

    try {
      const res = await appointmentsApi.validateMeetingToken(normalizedToken)
      const data = res.data?.data

      if (data?.canJoin) {
        router.push(`/meeting/${normalizedToken}`)
        return
      }

      setError(data?.reason || 'This meeting token is not joinable right now.')
    } catch (err: any) {
      const message = err?.response?.data?.message || 'Invalid meeting token. Please check and try again.'
      setError(message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="min-h-screen bg-slate-950 text-white flex items-center justify-center px-4">
      <section className="w-full max-w-md rounded-2xl border border-slate-800 bg-slate-900/70 p-6 shadow-2xl">
        <div className="flex items-center gap-3 mb-4">
          <div className="h-10 w-10 rounded-xl bg-blue-500/20 text-blue-300 flex items-center justify-center">
            <Video size={20} />
          </div>
          <div>
            <h1 className="text-xl font-semibold">Join Meeting</h1>
            <p className="text-sm text-slate-400">Enter your appointment token to join.</p>
          </div>
        </div>

        <form onSubmit={onSubmit} className="space-y-3">
          <label htmlFor="meeting-token" className="text-sm text-slate-300 block">
            Meeting token
          </label>
          <input
            id="meeting-token"
            value={token}
            onChange={(e) => setToken(e.target.value)}
            placeholder="A3X9KL2M"
            autoComplete="off"
            className="w-full rounded-xl border border-slate-700 bg-slate-950 px-3 py-2 text-white placeholder:text-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          {!isValid && normalizedToken.length > 0 && (
            <p className="text-xs text-rose-400">Use 6 to 12 letters or numbers.</p>
          )}
          {error && <p className="text-xs text-rose-400">{error}</p>}

          <button
            type="submit"
            disabled={!isValid || submitting}
            className="w-full rounded-xl bg-blue-600 hover:bg-blue-500 disabled:bg-blue-800/60 disabled:cursor-not-allowed px-3 py-2 font-medium inline-flex items-center justify-center gap-2"
          >
            {submitting ? <Loader2 size={16} className="animate-spin" /> : <Video size={16} />}
            Join Call
          </button>
        </form>
      </section>
    </main>
  )
}

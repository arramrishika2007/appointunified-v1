'use client'

import { SWRConfig } from 'swr'
import { api } from '@/lib/api'

const globalFetcher = (url: string) => api.get(url).then(r => r.data.data)

/**
 * Wraps the app in SWR's global config so all useSWR() calls
 * share the same fetcher, deduping interval, and error retry settings.
 */
export function SWRProvider({ children }: { children: React.ReactNode }) {
  return (
    <SWRConfig
      value={{
        fetcher: globalFetcher,
        dedupingInterval: 5000,
        revalidateOnFocus: false,
        shouldRetryOnError: false,
        onError: (error) => {
          // Swallow 401s — the axios interceptor handles redirect
          if (error?.response?.status === 401) return
          if (process.env.NODE_ENV === 'development') {
            console.error('[SWR error]', error)
          }
        },
      }}
    >
      {children}
    </SWRConfig>
  )
}

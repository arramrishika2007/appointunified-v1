'use client'

import { X } from 'lucide-react'
import dynamic from 'next/dynamic'

const OfflineLiveMap = dynamic(() => import('@/components/booking/OfflineLiveMap'), { ssr: false })

type LatLng = { lat: number; lng: number }

type LocationMapModalProps = {
  open: boolean
  onClose: () => void
  provider?: LatLng | null
  client?: LatLng | null
  title?: string
  subtitle?: string
}

export function LocationMapModal({ open, onClose, provider, client, title = 'Map view', subtitle }: LocationMapModalProps) {
  if (!open) return null

  return (
    <div className="fixed inset-0 z-[60] flex items-center justify-center bg-slate-950/70 px-4 py-6 backdrop-blur-sm">
      <div className="w-full max-w-4xl overflow-hidden rounded-3xl bg-white shadow-2xl">
        <div className="flex items-start justify-between gap-4 border-b border-slate-200 px-5 py-4">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-400">Live map</p>
            <h2 className="mt-1 text-lg font-semibold text-slate-900">{title}</h2>
            {subtitle && <p className="mt-1 text-sm text-slate-500">{subtitle}</p>}
          </div>
          <button onClick={onClose} className="rounded-full border border-slate-200 p-2 text-slate-500 hover:bg-slate-100 hover:text-slate-900" aria-label="Close map">
            <X size={18} />
          </button>
        </div>

        <div className="p-4">
          {provider ? (
            <OfflineLiveMap provider={provider} client={client} />
          ) : client ? (
            <OfflineLiveMap client={client} />
          ) : (
            <div className="flex h-64 items-center justify-center rounded-xl border border-dashed border-slate-200 text-sm text-slate-500">
              Map data unavailable.
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
'use client'

import React, { useState, useEffect, useRef } from 'react'
import { MapPin, Loader2, X } from 'lucide-react'
import { cn } from '@/lib/utils'

export interface LocationData {
  address: string
  city?: string
  lat: number
  lon: number
}

interface LocationAutocompleteProps {
  onLocationSelect: (loc: LocationData | null) => void
  placeholder?: string
  className?: string
  defaultValue?: string
}

export function LocationAutocomplete({ onLocationSelect, placeholder = 'Search location...', className, defaultValue = '' }: LocationAutocompleteProps) {
  const [query, setQuery] = useState(defaultValue)
  const [results, setResults] = useState<any[]>([])
  const [loading, setLoading] = useState(false)
  const [open, setOpen] = useState(false)
  // Track if the current query is the result of a selection to prevent re-fetching
  const [isSelected, setIsSelected] = useState(false)
  
  const wrapperRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target as Node)) {
        setOpen(false)
      }
    }
    document.addEventListener("mousedown", handleClickOutside)
    return () => document.removeEventListener("mousedown", handleClickOutside)
  }, [])

  useEffect(() => {
    if (!query) {
      setResults([])
      setOpen(false)
      return
    }

    if (isSelected) {
      setIsSelected(false)
      return
    }

    const timer = setTimeout(async () => {
      setLoading(true)
      try {
        const res = await fetch(`https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(query)}&addressdetails=1&limit=5`)
        const data = await res.json()
        setResults(data)
        setOpen(data.length > 0)
      } catch (err) {
        console.error('Failed to parse location', err)
      } finally {
        setLoading(false)
      }
    }, 600)

    return () => clearTimeout(timer)
  }, [query])

  const handleSelect = (item: any) => {
    const address = item.display_name
    const city = item.address?.city || item.address?.town || item.address?.village || item.address?.county
    const lat = parseFloat(item.lat)
    const lon = parseFloat(item.lon)

    setIsSelected(true)
    setQuery(address)
    setOpen(false)
    onLocationSelect({ address, city, lat, lon })
  }

  const clear = () => {
    setQuery('')
    setResults([])
    setOpen(false)
    onLocationSelect(null)
  }

  return (
    <div className="relative w-full" ref={wrapperRef}>
      <div className="relative">
        <MapPin size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
        <input
          type="text"
          value={query}
          onChange={(e) => {
            setQuery(e.target.value)
            if (e.target.value === '') onLocationSelect(null)
          }}
          onFocus={() => { if (results.length > 0) setOpen(true) }}
          placeholder={placeholder}
          className={cn("input pl-9 pr-10", className)}
        />
        {loading && (
          <Loader2 size={16} className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 animate-spin" />
        )}
        {!loading && query && (
          <button type="button" onClick={clear} className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 transition-colors">
            <X size={16} />
          </button>
        )}
      </div>

      {open && results.length > 0 && (
        <div className="absolute z-50 w-full mt-1 bg-white rounded-xl shadow-lg border border-slate-100 max-h-60 overflow-y-auto overflow-hidden">
          {results.map((r, i) => (
            <button
              key={i}
              type="button"
              onClick={() => handleSelect(r)}
              className="w-full text-left px-4 py-3 hover:bg-slate-50 border-b border-slate-50 last:border-0 transition-colors flex items-start gap-3"
            >
              <MapPin size={16} className="text-brand-500 mt-0.5 flex-shrink-0" />
              <div>
                <p className="text-sm font-medium text-slate-800 line-clamp-1">{r.display_name.split(',')[0]}</p>
                <p className="text-xs text-slate-500 line-clamp-1 mt-0.5">{r.display_name}</p>
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

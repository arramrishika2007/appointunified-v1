'use client'

import { useEffect, useState, useCallback } from 'react'
import { useParams, useRouter, useSearchParams } from 'next/navigation'
import dynamic from 'next/dynamic'
import { List, Loader2, LocateFixed, Map, Search } from 'lucide-react'
import { ProfessionalCard } from '@/components/provider/ProfessionalCard'
import { professionalsApi } from '@/lib/api'
import { ProfessionalSummary, Sector } from '@/types'
import { useAuthStore } from '@/lib/store'
import { cn, SECTOR_CONFIG } from '@/lib/utils'

const ProvidersMap = dynamic(() => import('@/components/provider/ProvidersMap'), { ssr: false })

const SORT_OPTIONS = [
  { value: 'ratingAvg,desc', label: 'Top Rated' },
  { value: 'consultationFee,asc', label: 'Price: Low to High' },
  { value: 'consultationFee,desc', label: 'Price: High to Low' },
]

export default function ExplorePage() {
  const params = useParams()
  const searchParams = useSearchParams()
  const router = useRouter()

  const sectorSlug = (params.sector as string)?.toUpperCase() as Sector
  const sectorConfig = SECTOR_CONFIG[sectorSlug]
  const { isAuthenticated } = useAuthStore()

  const [professionals, setProfessionals] = useState<ProfessionalSummary[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)

  const [query, setQuery] = useState('')
  const [city, setCity] = useState('')
  const [sort, setSort] = useState('ratingAvg,desc')
  const [viewMode, setViewMode] = useState<'list' | 'map'>('list')
  const [userCoords, setUserCoords] = useState<{ lat: number; lng: number } | null>(null)
  const [locating, setLocating] = useState(false)

  const fetchProfessionals = useCallback(async () => {
    setLoading(true)
    try {
      const res = await professionalsApi.search({
        sector: sectorSlug,
        city: city || undefined,
        query: query || undefined,
        verificationStatus: 'APPROVED',
        page,
        size: 12,
        sort,
      })
      const pageData = res.data.data
      setProfessionals(pageData.content)
      setTotalPages(pageData.totalPages)
      setTotalElements(pageData.totalElements)
    } catch {
      setProfessionals([])
    } finally {
      setLoading(false)
    }
  }, [sectorSlug, city, query, page, sort])

  const locateAndFetchNearby = async () => {
    if (!navigator.geolocation) return
    setLocating(true)
    navigator.geolocation.getCurrentPosition(
      async (position) => {
        const lat = position.coords.latitude
        const lng = position.coords.longitude
        setUserCoords({ lat, lng })
        try {
          const res = await professionalsApi.nearby({ lat, lng, radiusKm: 20, sector: sectorSlug, limit: 100 })
          setProfessionals(res.data.data)
          setTotalElements(res.data.data.length)
          setTotalPages(1)
          setPage(0)
          setViewMode('map')
        } finally {
          setLocating(false)
        }
      },
      () => setLocating(false),
      { enableHighAccuracy: true, timeout: 10000 }
    )
  }

  useEffect(() => {
    if (!isAuthenticated) {
      router.push('/auth/login')
      return
    }
    fetchProfessionals()
  }, [fetchProfessionals, isAuthenticated, router])

  if (!sectorConfig) {
    return (
      <div className="container-page py-20 text-center">
        <h1 className="text-2xl font-bold text-slate-900 mb-2">Sector not found</h1>
        <p className="text-slate-500">Try Healthcare, Government, or Services.</p>
      </div>
    )
  }

  return (
    <main>
        {/* Hero banner */}
        <div className={cn('py-12 border-b border-slate-200', sectorConfig.bg)}>
          <div className="container-page">
            <div className="flex items-center gap-3 mb-2">
              <span className="text-4xl">{sectorConfig.icon}</span>
              <h1 className="text-3xl font-extrabold text-slate-900">{sectorConfig.label}</h1>
            </div>
            <p className="text-slate-600 text-lg">
              Browse {totalElements.toLocaleString()} verified {sectorConfig.label.toLowerCase()} professionals
            </p>
          </div>
        </div>

        <div className="container-page py-8">
          {/* Filters */}
          <div className="flex flex-col sm:flex-row gap-3 mb-8">
            {/* Search */}
            <div className="relative flex-1">
              <Search size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                value={query}
                onChange={(e) => { setQuery(e.target.value); setPage(0) }}
                className="input pl-9"
                placeholder={`Search ${sectorConfig.label.toLowerCase()} providers…`}
              />
            </div>

            {/* City */}
            <input
              value={city}
              onChange={(e) => { setCity(e.target.value); setPage(0) }}
              className="input w-full sm:w-44"
              placeholder="City"
            />

            {/* Sort */}
            <select
              value={sort}
              onChange={(e) => { setSort(e.target.value); setPage(0) }}
              className="input w-full sm:w-48"
              aria-label="Sort providers"
              title="Sort providers"
            >
              {SORT_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>{o.label}</option>
              ))}
            </select>

            <button
              onClick={() => setViewMode((v) => v === 'list' ? 'map' : 'list')}
              className="btn-secondary w-full sm:w-auto"
            >
              {viewMode === 'list' ? <Map size={14} /> : <List size={14} />} {viewMode === 'list' ? 'Map View' : 'List View'}
            </button>

            <button
              onClick={locateAndFetchNearby}
              disabled={locating}
              className="btn-secondary w-full sm:w-auto"
            >
              {locating ? <Loader2 size={14} className="animate-spin" /> : <LocateFixed size={14} />} Nearby
            </button>
          </div>

          {/* Results */}
          {loading ? (
            <div className="grid sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
              {Array.from({ length: 8 }).map((_, i) => (
                <div key={i} className="card p-5 space-y-3">
                  <div className="flex gap-3">
                    <div className="skeleton h-14 w-14 rounded-xl" />
                    <div className="flex-1 space-y-2">
                      <div className="skeleton h-4 w-3/4" />
                      <div className="skeleton h-3 w-1/2" />
                    </div>
                  </div>
                  <div className="skeleton h-8 w-full rounded-lg" />
                  <div className="skeleton h-3 w-full" />
                  <div className="skeleton h-10 w-full rounded-lg" />
                </div>
              ))}
            </div>
          ) : professionals.length === 0 ? (
            <div className="text-center py-20">
              <div className="text-5xl mb-4">{sectorConfig.icon}</div>
              <h3 className="text-lg font-semibold text-slate-900 mb-2">No providers found</h3>
              <p className="text-slate-500 text-sm">Try adjusting your search or filters.</p>
            </div>
          ) : (
            <>
              <div className="flex items-center justify-between mb-4">
                <p className="text-sm text-slate-500">
                  {totalElements.toLocaleString()} provider{totalElements !== 1 ? 's' : ''}
                </p>
              </div>

              {viewMode === 'map' && (
                <div className="card p-3 mb-5 overflow-hidden">
                  <div className="flex items-center justify-between gap-3 px-2 pb-3">
                    <div>
                      <p className="text-sm font-semibold text-slate-900">Map View</p>
                      <p className="text-xs text-slate-500">Showing providers with available location data.</p>
                    </div>
                    <span className="badge text-xs bg-slate-100 text-slate-600">{professionals.length} results</span>
                  </div>
                  <ProvidersMap professionals={professionals} userCoords={userCoords} />
                </div>
              )}

              <div className="grid sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
                {professionals.map((p) => (
                  <ProfessionalCard key={p.id} professional={p} />
                ))}
              </div>

              {/* Pagination */}
              {totalPages > 1 && (
                <div className="flex items-center justify-center gap-2 mt-10">
                  <button
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    disabled={page === 0}
                    className="btn-secondary px-4 py-2 disabled:opacity-40"
                  >
                    Previous
                  </button>
                  <span className="text-sm text-slate-500">
                    Page {page + 1} of {totalPages}
                  </span>
                  <button
                    onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                    disabled={page >= totalPages - 1}
                    className="btn-secondary px-4 py-2 disabled:opacity-40"
                  >
                    Next
                  </button>
                </div>
              )}
            </>
          )}
        </div>
    </main>
  )
}

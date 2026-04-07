import useSWR from 'swr'
import { api } from '@/lib/api'
import { ProfessionalDetail, AppointmentSummary, DraftSummary, PageResponse } from '@/types'

// ─── Fetcher ──────────────────────────────────────────────────────────────────

const fetcher = (url: string) => api.get(url).then(r => r.data.data)

// ─── Professional hooks ───────────────────────────────────────────────────────

export function useProfessional(id: string | null) {
  const { data, error, isLoading, mutate } = useSWR<ProfessionalDetail>(
    id ? `/professionals/${id}` : null,
    fetcher,
    { revalidateOnFocus: false }
  )
  return { professional: data, error, isLoading, mutate }
}

export function useProfessionals(params: Record<string, string | number | undefined>) {
  const query = new URLSearchParams(
    Object.entries(params)
      .filter(([, v]) => v !== undefined)
      .map(([k, v]) => [k, String(v)])
  ).toString()

  const { data, error, isLoading } = useSWR<PageResponse<ProfessionalDetail>>(
    `/professionals?${query}`,
    fetcher
  )
  return { page: data, error, isLoading }
}

// ─── Appointment hooks ────────────────────────────────────────────────────────

export function useMyAppointments() {
  const { data, error, isLoading, mutate } = useSWR<PageResponse<AppointmentSummary>>(
    '/appointments/me?size=50',
    fetcher
  )
  return { page: data, error, isLoading, mutate }
}

export function useMyDrafts() {
  const { data, error, isLoading, mutate } = useSWR<DraftSummary[]>(
    '/appointments/drafts',
    fetcher
  )
  return { drafts: data ?? [], error, isLoading, mutate }
}

// ─── Recommendation hook (V1 Feature 1) ──────────────────────────────────────

export function useRecommendations(limit = 6) {
  const { data, error, isLoading } = useSWR(
    `/recommendations/slots?limit=${limit}`,
    fetcher,
    { revalidateOnFocus: false, dedupingInterval: 300_000 } // 5-min cache
  )
  return { recommendations: data ?? [], error, isLoading }
}

// ─── Slots hook ───────────────────────────────────────────────────────────────

export function useAvailableSlots(
  professionalId: string | null,
  date: string | null,
  serviceId?: string
) {
  const params = new URLSearchParams({ date: date ?? '' })
  if (serviceId) params.set('serviceId', serviceId)

  const { data, error, isLoading } = useSWR(
    professionalId && date
      ? `/professionals/${professionalId}/slots?${params}`
      : null,
    fetcher,
    { revalidateOnFocus: false }
  )
  return { slots: data ?? [], error, isLoading }
}

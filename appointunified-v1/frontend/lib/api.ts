import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios'
import { TokenPair } from '@/types'

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api'

export const api = axios.create({
  baseURL: API_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
})

// ─── Token storage helpers ─────────────────────────────────────────────────

const getAccessToken = () =>
  typeof window !== 'undefined' ? localStorage.getItem('au_access') : null

const getRefreshToken = () =>
  typeof window !== 'undefined' ? localStorage.getItem('au_refresh') : null

export const setTokens = (tokens: Pick<TokenPair, 'accessToken' | 'refreshToken'>) => {
  localStorage.setItem('au_access', tokens.accessToken)
  localStorage.setItem('au_refresh', tokens.refreshToken)
}

export const clearTokens = () => {
  localStorage.removeItem('au_access')
  localStorage.removeItem('au_refresh')
  localStorage.removeItem('au_user')
}

// ─── Request interceptor: attach JWT ────────────────────────────────────────

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// ─── Response interceptor: auto-refresh on 401 ──────────────────────────────

let isRefreshing = false
let failedQueue: Array<{ resolve: (v: string) => void; reject: (e: unknown) => void }> = []

const processQueue = (error: unknown, token: string | null) => {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) reject(error)
    else resolve(token!)
  })
  failedQueue = []
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean }

    if (error.response?.status === 401 && !originalRequest._retry) {
      const refreshToken = getRefreshToken()
      if (!refreshToken) {
        clearTokens()
        window.location.href = '/auth/login'
        return Promise.reject(error)
      }

      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        }).then((token) => {
          originalRequest.headers.Authorization = `Bearer ${token}`
          return api(originalRequest)
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        const { data } = await axios.post(`${API_URL}/auth/refresh`, { refreshToken })
        const newTokens = data.data as TokenPair
        setTokens(newTokens)
        processQueue(null, newTokens.accessToken)
        originalRequest.headers.Authorization = `Bearer ${newTokens.accessToken}`
        return api(originalRequest)
      } catch (refreshError) {
        processQueue(refreshError, null)
        clearTokens()
        window.location.href = '/auth/login'
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    return Promise.reject(error)
  }
)

// ─── Typed API methods ───────────────────────────────────────────────────────

export const authApi = {
  signUp: (data: object) => api.post('/auth/signup', data),
  login: (data: object) => api.post('/auth/login', data),
  firebaseLogin: (data: object) => api.post('/auth/firebase/login', data),
  logout: () => api.delete('/auth/logout'),
  refresh: (refreshToken: string) => api.post('/auth/refresh', { refreshToken }),
}

export const devicesApi = {
  registerFcmToken: (data: object) => api.post('/devices/fcm', data),
}

export const professionalsApi = {
  search: (params: object) => api.get('/professionals', { params }),
  nearby: (params: { lat: number; lng: number; radiusKm?: number; sector?: string; limit?: number }) =>
    api.get('/professionals/nearby', { params }),
  getById: (id: string) => api.get(`/professionals/${id}`),
  getMyProfile: () => api.get('/professionals/me'),
  getSlots: (id: string, date: string, serviceId?: string) =>
    api.get(`/professionals/${id}/slots`, { params: { date, serviceId } }),
  register: (data: object) => api.post('/professionals/register', data),
  updateProfile: (data: object) => api.patch('/professionals/me', data),
  updateMood: (data: object) => api.patch('/professionals/me/mood', data),
  updateOverbooking: (allowOverbooking: boolean) =>
    api.patch('/professionals/me/overbooking', { allowOverbooking }),
  updateServiceArea: (radiusKm: number, centerLat?: number, centerLng?: number) =>
    api.patch('/professionals/me/service-area', { radiusKm, centerLat, centerLng }),
}

export const geoApi = {
  getTravelTime: (params: { fromLat: number; fromLng: number; toLat: number; toLng: number }) =>
    api.get('/geo/travel-time', { params }),
  routeOptimize: (date?: string) =>
    api.get('/geo/route-optimize', { params: date ? { date } : undefined }),
}

export const appointmentsApi = {
  create: (data: object) => api.post('/appointments', data),
  getMyAppointments: (params?: object) => api.get('/appointments/me', { params }),
  getWorkflowContext: (id: string) => api.get(`/appointments/${id}/workflow`),
  cancel: (id: string, data?: object) => api.put(`/appointments/${id}/cancel`, data || {}),
  reschedule: (id: string, data: object) => api.put(`/appointments/${id}/reschedule`, data),
  complete: (id: string) => api.post(`/appointments/${id}/complete`),
  noShow: (id: string) => api.post(`/appointments/${id}/no-show`),
  confirmDeposit: (id: string) => api.post(`/appointments/${id}/confirm-deposit`),
  verifyFinalPayment: (id: string) => api.post(`/appointments/${id}/verify-final-payment`),
  getShareInfo: (id: string) => api.get(`/appointments/${id}/share`),
  getByShareToken: (token: string) => api.get(`/appointments/share/${token}`),
  validateMeetingToken: (token: string) => api.get(`/appointments/meeting/validate-token/${token}`),
  // Drafts
  saveDraft: (data: object) => api.post('/appointments/drafts', data),
  getMyDrafts: () => api.get('/appointments/drafts'),
  deleteDraft: (id: string) => api.delete(`/appointments/drafts/${id}`),
}

export const usersApi = {
  getMyRiskSummary: () => api.get('/users/me/risk-summary'),
}

export const waitlistApi = {
  join: (data: object) => api.post('/waitlist', data),
  getMine: () => api.get('/waitlist/me'),
  cancel: (id: string) => api.delete(`/waitlist/${id}`),
}

export const workflowsApi = {
  list: (params?: { sector?: string }) => api.get('/workflows', { params }),
  start: (workflowId: string) => api.post('/workflows/start', { workflowId }),
  getMyInstances: () => api.get('/workflows/instances/me'),
}

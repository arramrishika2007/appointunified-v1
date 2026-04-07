// AppointUnified V1 — Shared TypeScript Types

export type UserRole = 'PUBLIC' | 'PROFESSIONAL' | 'ADMIN' | 'SUPER_ADMIN'
export type Sector = 'HEALTHCARE' | 'GOVERNMENT' | 'SERVICES'
export type VerificationStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'SUSPENDED'
export type AppointmentStatus = 'DRAFT' | 'SCHEDULED' | 'IN_QUEUE' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW' | 'EXPIRED'
export type AppointmentPriority = 'NORMAL' | 'PREMIUM' | 'EMERGENCY'
export type AvailabilityMood = 'AVAILABLE' | 'BUSY' | 'RUNNING_LATE' | 'TAKING_BREAKS' | 'DO_NOT_DISTURB'

// ─── Auth ─────────────────────────────────────────────────────────────────────

export interface AuthUser {
  id: string
  fullName: string
  phone: string
  email?: string
  role: UserRole
  avatarUrl?: string
  verified: boolean
  sector?: Sector
}

export interface TokenPair {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: AuthUser
}

// ─── Professional ─────────────────────────────────────────────────────────────

export interface ProfessionalSummary {
  id: string
  displayName: string
  sector: Sector
  specialty?: string
  verificationStatus: VerificationStatus
  ratingAvg: number
  totalReviews: number
  avatarUrl?: string
  consultationFee?: number
  city?: string
  acceptingBookings: boolean
  allowOverbooking?: boolean
  // V1 Feature 2: Mood
  availabilityMood?: AvailabilityMood
  moodNote?: string
  nextAvailableSlot?: string
}

export interface AvailabilityDay {
  weekday: number
  dayName: string
  startTime: string
  endTime: string
  bufferMinutes: number
  slotDurationMins: number
}

export interface ServiceSummary {
  id: string
  name: string
  description?: string
  durationMinutes: number
  price?: number
  isActive: boolean
  requiresDocuments: boolean
  isVirtual: boolean
}

export interface ProfessionalDetail extends ProfessionalSummary {
  bio?: string
  qualification?: string
  yearsExperience?: number
  coverUrl?: string
  licenseNumber?: string
  latitude?: number
  virtual: boolean
  meetingToken?: string
  depositStatus?: 'PENDING' | 'CONFIRMED' | 'DISPUTED' | 'REFUNDED'
  finalPaymentStatus?: 'PENDING' | 'CONFIRMED' | 'DISPUTED' | 'REFUNDED'
  totalAmount?: number
  address?: string
  totalCompleted: number
  services: ServiceSummary[]
  weeklySchedule: AvailabilityDay[]
  joinedAt: string
}

export interface RiskSummary {
  userId: string
  score: number
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH'
  totalCancellations: number
  lastMinuteCancellations: number
  noShows: number
  completions: number
  lastCalculatedAt: string
}

export interface WaitlistSummary {
  id: string
  professionalId: string
  professionalName: string
  serviceId?: string
  serviceName?: string
  notified: boolean
  notifiedAt?: string
  preferredTimeFrom?: string
  preferredTimeTo?: string
  createdAt: string
  expiresAt?: string
}

// ─── Appointments ─────────────────────────────────────────────────────────────

export interface AvailableSlot {
  startTime: string
  endTime: string
  available: boolean
  label: string
}

export interface AppointmentSummary {
  id: string
  status: AppointmentStatus
  priority: AppointmentPriority
  startTime: string
  endTime: string
  notes?: string
  virtual: boolean
  meetLink?: string
  shareToken?: string
  icalUrl?: string
  createdAt: string
  professional: {
    id: string
    displayName: string
    avatarUrl?: string
    sector: Sector
    specialty?: string
  }
  service: {
    id: string
    name: string
    durationMinutes: number
    price?: number
  }
  client: {
    id: string
    fullName: string
    phone: string
    avatarUrl?: string
  }
}

export interface ShareInfo {
  shareUrl: string
  icalUrl: string
  expiresAt: string
}

// V1 Feature 3: Draft
export interface DraftSummary {
  id: string
  professionalId?: string
  professionalName?: string
  serviceId?: string
  serviceName?: string
  stepReached: number
  draftData: Record<string, unknown>
  expiresAt: string
  updatedAt: string
}

// ─── API Response wrapper ────────────────────────────────────────────────────

export interface ApiResponse<T> {
  success: boolean
  message?: string
  data: T
  errors?: Record<string, string>
  timestamp: string
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
}

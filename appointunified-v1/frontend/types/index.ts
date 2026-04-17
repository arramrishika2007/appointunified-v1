// AppointUnified V1 — Shared TypeScript Types

export type UserRole = 'PUBLIC' | 'PROFESSIONAL' | 'ADMIN' | 'SUPER_ADMIN'
export type Sector = 'HEALTHCARE' | 'GOVERNMENT' | 'SERVICES'
export type VerificationStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'SUSPENDED'
export type AppointmentStatus =
  | 'DRAFT'
  | 'PENDING_DEPOSIT'
  | 'DEPOSIT_PAID'
  | 'CONFIRMED'
  | 'SCHEDULED'
  | 'IN_QUEUE'
  | 'IN_MEETING'
  | 'IN_PROGRESS'
  | 'PENDING_BALANCE'
  | 'PAID_FULL'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'NO_SHOW'
  | 'EXPIRED'
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
  latitude?: number
  longitude?: number
  serviceAreaRadiusKm?: number
  distanceKm?: number
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
  upiId?: string
  coverUrl?: string
  licenseNumber?: string
  latitude?: number
  longitude?: number
  serviceAreaRadiusKm?: number
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

export interface TravelTimeSummary {
  distanceKm: number
  durationMinutes: number
  summary: string
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

export interface WorkflowDefinition {
  id: string
  name: string
  sector: Sector
  description?: string
  active: boolean
  createdAt: string
  steps: Array<Record<string, unknown>>
}

export interface WorkflowStepProgress {
  order: number
  label: string
  serviceId?: string
  professionalId?: string
  autoBookNext: boolean
  requiresCompletion: boolean
  appointmentId?: string
  appointmentStatus?: string
  completedAt?: string
}

export interface WorkflowInstance {
  id: string
  workflowId: string
  workflowName: string
  status: 'IN_PROGRESS' | 'COMPLETED' | string
  currentStep: number
  totalSteps: number
  nextStep?: WorkflowStepProgress
  createdAt: string
  steps: WorkflowStepProgress[]
}

export interface AppointmentWorkflowContext {
  appointmentId: string
  instanceId: string | null
  workflowName: string
  stepOrder: number
  stepLabel: string
  instanceStatus: string
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
  meetingToken?: string
  clientLat?: number
  clientLon?: number
  distanceMeters?: number
  depositStatus?: 'PENDING' | 'CONFIRMED' | 'DISPUTED' | 'REFUNDED'
  finalPaymentStatus?: 'PENDING' | 'CONFIRMED' | 'DISPUTED' | 'REFUNDED'
  premiumFee?: number
  totalAmount?: number
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

export interface PaymentOrderDetails {
  paymentOrderId: string
  keyId: string
  provider?: 'RAZORPAY' | 'STRIPE' | string
  gatewayOrderId: string
  checkoutUrl?: string | null
  amount: number
  currency: string
  status: string
}

export interface PaymentVerifyResult {
  paymentOrderId: string
  paymentType: 'DEPOSIT' | 'BALANCE' | string
  paymentStatus: 'PAID' | 'PENDING' | 'FAILED' | string
  appointmentId: string
  appointmentStatus: AppointmentStatus | string
}

// ─── V9: Analytics ────────────────────────────────────────────────────────────

export interface AnalyticsSummary {
  totalBookings: number
  completedBookings: number
  cancelledBookings: number
  noShows: number
  totalRevenue: number
  newUsers: number
  newProfessionals: number
  averageNoShowRate: number
  averageCompletionRate: number
  timeRange: string
  sector?: string
}

export interface NoShowTrend {
  professionalId: string
  professionalName: string
  date: string
  totalAppointments: number
  noShowCount: number
  noShowRate: number
  trend: 'UP' | 'DOWN' | 'STABLE'
}

export interface PeakHoursDataPoint {
  hour: number
  dayOfWeek: number
  dayName: string
  bookingCount: number
  intensity: number
}

export interface RevenueBreakdown {
  serviceType: string
  amount: number
  bookingCount: number
  sector?: string
  percentageOfTotal: number
}

export interface EarningsData {
  date: string
  amount: number
  completedAppointments: number
  averageRating: number
}

export interface KPISummary {
  totalActiveUsers: number
  totalActiveProfessionals: number
  totalBookingsToday: number
  completedBookingsToday: number
  platformRevenueToday: number
  averageNoShowRateToday: number
  openSLABreachesCount: number
  redisQueueDepth: number
  activeWebSocketConnections: number
  generatedAt: string
}

export interface AISuggestion {
  id: string
  suggestionTitle: string
  suggestionDescription: string
  expectedImpact: 'HIGH' | 'MEDIUM' | 'LOW'
  suggestionData?: string
  generatedAt: string
  generatedByModel: string
}

export interface ReportExport {
  id: string
  exportType: 'BOOKINGS' | 'NO_SHOW_REPORT' | 'AUDIT_LOG' | 'VERIFICATION_REPORT'
  fileName: string
  fileUrl?: string
  status: 'GENERATING' | 'READY' | 'FAILED' | 'EXPIRED'
  fileSizeBytes?: number
  errorMessage?: string
  dateFilterStart?: string
  dateFilterEnd?: string
  sectorFilter?: string
  createdAt: string
  expiresAt: string
}

// ─── System Chat (FEATURE B: RAG-powered chatbot) ─────────────────────────────

export interface SystemChatMessage {
  question: string
  answer: string
  status: 'PENDING' | 'SUCCESS' | 'ERROR' | 'OFFLINE'
  timestamp?: string
}

// ─── Chat (FEATURE A: One-to-One Chat) ──────────────────────────────────────

export interface ChatMessage {
  id: string
  appointmentId: string
  senderId: string
  senderName: string
  receiverId: string
  receiverName: string
  message: string
  isRead: boolean
  sentAt: string
  filtered: boolean
  filterReason?: string
}

export interface ChatThreadItem {
  appointmentId: string
  appointmentStatus: AppointmentStatus
  appointmentPriority: AppointmentPriority
  startTime: string
  endTime: string
  virtual: boolean
  serviceName: string
  serviceDurationMinutes?: number
  otherParticipantId: string
  otherParticipantName: string
  otherParticipantAvatarUrl?: string
  otherParticipantRole: 'CLIENT' | 'PROFESSIONAL'
  previewText: string
  lastMessageAt: string
  unreadCount: number
  hasMessages: boolean
}

export interface UnreadCountResponse {
  unreadCount: number
}

// ─── Profile Picture Upload (FEATURE C) ───────────────────────────────────

export interface ProfilePictureUploadConfig {
  cloudName: string
  apiKey: string
  timestamp: number
  folder: string
  signature: string
  fileSizeLimit: string
  acceptedFormats: string
}

export interface ProcessedAvatarResponse {
  avatarUrl: string
  avatarThumbUrl: string
  cloudinaryPublicId: string
}

// ─── Notifications (FEATURE D: Notification Center) ───────────────────────

export type NotificationType =
  | 'APPOINTMENT'
  | 'QUEUE'
  | 'PAYMENT'
  | 'CHAT'
  | 'SYSTEM'
  | 'VERIFICATION'
  | 'WAITLIST'

export interface NotificationItem {
  id: string
  type: NotificationType
  title: string
  message?: string
  actionUrl?: string
  isRead: boolean
  isArchived: boolean
  readAt?: string
  createdAt: string
  updatedAt: string
}

export interface NotificationUnreadCountResponse {
  unreadCount: number
}

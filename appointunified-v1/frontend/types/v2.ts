export type BadgeTier = 'NONE' | 'BRONZE' | 'SILVER' | 'GOLD'
export type VerificationDocStatus = 'PENDING' | 'APPROVED' | 'REJECTED'
export type ComplaintPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'CRITICAL'

export interface VerificationDocument {
  id: string
  docType: string
  docUrl: string
  status: VerificationDocStatus
  reviewNotes?: string
  submittedAt: string
  reviewedAt?: string
  fileSizeBytes?: number
  mimeType?: string
}

export interface VerificationStatus {
  verificationStatus: 'PENDING' | 'APPROVED' | 'REJECTED' | 'SUSPENDED'
  badgeTier: BadgeTier
  verificationExpiresAt?: string
  documents: VerificationDocument[]
}

export interface PendingDocumentView {
  docId: string
  professionalId: string
  professionalName: string
  sector: string
  docType: string
  docUrl: string
  fileSizeBytes?: number
  submittedAt: string
}

export interface Complaint {
  id: string
  professionalId: string
  professionalName: string
  category: string
  description: string
  status: 'OPEN' | 'INVESTIGATING' | 'RESOLVED' | 'DISMISSED'
  priority: ComplaintPriority
  resolutionNotes?: string
  createdAt: string
  resolvedAt?: string
}

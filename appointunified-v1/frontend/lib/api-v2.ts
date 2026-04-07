import { api } from '@/lib/api'

export const cloudinaryApi = {
  getSignature: (folder?: string) => api.get('/cloudinary/signature', { params: { folder } }),
}

export const verificationApi = {
  getStatus: () => api.get('/professional/verification/status'),
  submitDocument: (data: {
    docType: string
    docUrl: string
    docHash?: string
    fileSizeBytes?: number
    mimeType?: string
  }) => api.post('/professional/verification/documents', data),
  getPending: (params?: { page?: number; size?: number }) =>
    api.get('/admin/verification/pending', { params }),
  reviewDocument: (docId: string, decision: 'APPROVED' | 'REJECTED', notes?: string) =>
    api.patch(`/admin/verification/documents/${docId}/review`, { decision, notes }),
  approveProfessional: (professionalId: string, notes: string) =>
    api.patch(`/admin/verification/professionals/${professionalId}/approve`, { notes }),
  rejectProfessional: (professionalId: string, notes: string) =>
    api.patch(`/admin/verification/professionals/${professionalId}/reject`, { notes }),
  getVerificationDecisions: (size?: number) =>
    api.get(`/admin/super-admin/verification-decisions`, { params: { size } }),
}

export const complaintApi = {
  file: (data: {
    professionalId: string
    appointmentId?: string
    category: string
    description: string
    priority?: 'LOW' | 'NORMAL' | 'HIGH' | 'CRITICAL'
  }) => api.post('/complaints', data),
  list: (status = 'OPEN', params?: { page?: number; size?: number }) =>
    api.get('/admin/complaints', { params: { status, ...(params || {}) } }),
  resolve: (id: string, resolutionNotes: string, suspendProfessional = false) =>
    api.patch(`/admin/complaints/${id}/resolve`, { resolutionNotes, suspendProfessional }),
  dismiss: (id: string, reason: string) =>
    api.patch(`/admin/complaints/${id}/dismiss`, { reason }),
}

export const reputationApi = {
  getProfessionalScore: (professionalId: string) =>
    api.get(`/reputation/professionals/${professionalId}`),
}

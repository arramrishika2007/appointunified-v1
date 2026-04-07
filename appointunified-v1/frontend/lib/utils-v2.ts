import { BadgeTier, ComplaintPriority } from '@/types/v2'

export const BADGE_CONFIG: Record<BadgeTier, { label: string; color: string; bg: string; icon: string }> = {
  NONE: { label: 'Unverified', color: 'text-slate-600', bg: 'bg-slate-100', icon: '○' },
  BRONZE: { label: 'Bronze Verified', color: 'text-amber-700', bg: 'bg-amber-100', icon: '🥉' },
  SILVER: { label: 'Silver Verified', color: 'text-slate-700', bg: 'bg-slate-200', icon: '🥈' },
  GOLD: { label: 'Gold Verified', color: 'text-yellow-800', bg: 'bg-yellow-100', icon: '🥇' },
}

export const DOC_TYPE_CONFIG: Record<string, { label: string; hint?: string }> = {
  ID_PROOF: { label: 'Government ID Proof' },
  LICENSE: { label: 'Professional License' },
  CERTIFICATE: { label: 'Certification Document' },
  ADDRESS_PROOF: { label: 'Address Proof' },
  SELFIE: { label: 'Live Selfie Verification' },
  GOVERNMENT_ID: { label: 'Government Employee ID' },
  NMC_CERT: { label: 'NMC Registration Certificate' },
  TRADE_CERT: { label: 'Trade Certificate' },
  OTHER: { label: 'Other Supporting Document' },
}

export const SECTOR_REQUIRED_DOCS: Record<string, string[]> = {
  HEALTHCARE: ['ID_PROOF', 'LICENSE', 'NMC_CERT'],
  GOVERNMENT: ['ID_PROOF', 'GOVERNMENT_ID'],
  SERVICES: ['ID_PROOF', 'TRADE_CERT'],
}

export const PRIORITY_CONFIG: Record<ComplaintPriority, { bg: string; color: string }> = {
  LOW: { bg: 'bg-slate-100', color: 'text-slate-700' },
  NORMAL: { bg: 'bg-blue-100', color: 'text-blue-700' },
  HIGH: { bg: 'bg-amber-100', color: 'text-amber-700' },
  CRITICAL: { bg: 'bg-red-100', color: 'text-red-700' },
}

export const COMPLAINT_CATEGORIES = [
  { value: 'FRAUD', label: 'Fraud or Scam', desc: 'Money scam, fake identity, or deceptive behavior' },
  { value: 'MISCONDUCT', label: 'Misconduct', desc: 'Unprofessional, abusive, or unsafe behavior' },
  { value: 'NO_SHOW', label: 'No Show', desc: 'Professional did not show up for confirmed appointment' },
  { value: 'OTHER', label: 'Other', desc: 'Any other serious issue not listed above' },
]

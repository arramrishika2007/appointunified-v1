import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'
import { format, formatDistanceToNow, isToday, isTomorrow } from 'date-fns'
import { AvailabilityMood, AppointmentStatus, Sector } from '@/types'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

// ─── Date/time helpers ───────────────────────────────────────────────────────

export function formatDateTime(iso: string): string {
  const date = new Date(iso)
  if (isToday(date)) return `Today at ${format(date, 'h:mm a')}`
  if (isTomorrow(date)) return `Tomorrow at ${format(date, 'h:mm a')}`
  return format(date, 'EEE, MMM d · h:mm a')
}

export function formatDateShort(iso: string): string {
  return format(new Date(iso), 'MMM d, yyyy')
}

export function formatTimeOnly(iso: string): string {
  return format(new Date(iso), 'h:mm a')
}

export function timeAgo(iso: string): string {
  return formatDistanceToNow(new Date(iso), { addSuffix: true })
}

export function formatDuration(minutes: number): string {
  if (minutes < 60) return `${minutes}m`
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return m > 0 ? `${h}h ${m}m` : `${h}h`
}

import { createElement } from 'react'
import { HeartPulse, Landmark, Settings2, CheckCircle2, XCircle, Clock, Coffee, Ban } from 'lucide-react'

// ─── Sector helpers ──────────────────────────────────────────────────────────

export const SECTOR_CONFIG: Record<Sector, { label: string; icon: React.ReactNode; color: string; bg: string }> = {
  HEALTHCARE: { label: 'Healthcare', icon: createElement(HeartPulse, { size: 14 }), color: 'text-accent', bg: 'bg-accent/10' },
  GOVERNMENT: { label: 'Government', icon: createElement(Landmark, { size: 14 }), color: 'text-accent-light',   bg: 'bg-subtle'   },
  SERVICES:   { label: 'Services',   icon: createElement(Settings2, { size: 14 }),  color: 'text-accent-mint', bg: 'bg-accent-mint/10'  },
}

// ─── Mood helpers (V1 Feature 2) ─────────────────────────────────────────────

export const MOOD_CONFIG: Record<AvailabilityMood, { label: string; color: string; dot: string; emoji: React.ReactNode }> = {
  AVAILABLE:      { label: 'Available',      color: 'text-accent-mint', dot: 'bg-accent-mint', emoji: createElement(CheckCircle2, { size: 14 }) },
  BUSY:           { label: 'Busy',           color: 'text-accent-warm',     dot: 'bg-accent-warm',     emoji: createElement(XCircle, { size: 14 }) },
  RUNNING_LATE:   { label: 'Running Late',   color: 'text-accent',   dot: 'bg-accent',   emoji: createElement(Clock, { size: 14 }) },
  TAKING_BREAKS:  { label: 'On Break',       color: 'text-accent-light',     dot: 'bg-accent-light',     emoji: createElement(Coffee, { size: 14 }) },
  DO_NOT_DISTURB: { label: 'Do Not Disturb', color: 'text-text-muted',   dot: 'bg-text-muted',   emoji: createElement(Ban, { size: 14 }) },
}

// ─── Status helpers ──────────────────────────────────────────────────────────

export const STATUS_CONFIG: Record<AppointmentStatus, { label: string; color: string; bg: string }> = {
  DRAFT:       { label: 'Draft',       color: 'text-slate-600',  bg: 'bg-slate-100'  },
  SCHEDULED:   { label: 'Scheduled',   color: 'text-blue-700',   bg: 'bg-blue-100'   },
  IN_QUEUE:    { label: 'In Queue',    color: 'text-purple-700', bg: 'bg-purple-100' },
  IN_PROGRESS: { label: 'In Progress', color: 'text-amber-700',  bg: 'bg-amber-100'  },
  COMPLETED:   { label: 'Completed',   color: 'text-emerald-700',bg: 'bg-emerald-100'},
  CANCELLED:   { label: 'Cancelled',   color: 'text-red-700',    bg: 'bg-red-100'    },
  NO_SHOW:     { label: 'No-Show',     color: 'text-orange-700', bg: 'bg-orange-100' },
  EXPIRED:     { label: 'Expired',     color: 'text-slate-500',  bg: 'bg-slate-100'  },
}

// ─── Currency ────────────────────────────────────────────────────────────────

export function formatCurrency(amount?: number, currency = '₹'): string {
  if (amount == null) return 'Free'
  return `${currency}${amount.toLocaleString('en-IN')}`
}

// ─── Misc ────────────────────────────────────────────────────────────────────

export function getInitials(name: string): string {
  return name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2)
}

export function weekdayName(day: number): string {
  return ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'][day] ?? ''
}

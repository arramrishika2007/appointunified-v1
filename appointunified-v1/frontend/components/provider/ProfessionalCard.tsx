'use client'

import Link from 'next/link'
import { MapPin, Star } from 'lucide-react'
import { ProfessionalSummary } from '@/types'
import { cn, formatCurrency, getInitials, MOOD_CONFIG, SECTOR_CONFIG } from '@/lib/utils'

interface Props {
  professional: ProfessionalSummary
}

export function ProfessionalCard({ professional: p }: Props) {
  const sector = SECTOR_CONFIG[p.sector]
  const mood = p.availabilityMood ? MOOD_CONFIG[p.availabilityMood] : null

  return (
    <Link
      href={`/provider/${p.id}`}
      className="card-hover p-5 flex flex-col gap-4 animate-fade-in"
    >
      {/* Header: Avatar + Name + Verification */}
      <div className="flex gap-3 items-start">
        <div className="relative flex-shrink-0">
          <div className="h-14 w-14 rounded-xl bg-brand-100 flex items-center justify-center text-brand-700 font-bold text-lg overflow-hidden">
            {p.avatarUrl
              ? <img src={p.avatarUrl} alt={p.displayName} className="h-full w-full object-cover" />
              : getInitials(p.displayName)
            }
          </div>
          {/* Sector icon badge */}
          <span className="absolute -bottom-1 -right-1 text-base leading-none">{sector.icon}</span>
        </div>

        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-1.5 flex-wrap">
            <h3 className="font-semibold text-slate-900 truncate">{p.displayName}</h3>
            {p.verificationStatus === 'APPROVED' && (
              <span className="badge-verified text-[10px] px-1.5">✓ Verified</span>
            )}
          </div>

          {p.specialty && (
            <p className="text-sm text-slate-500 truncate">{p.specialty}</p>
          )}

          <div className={cn('text-[11px] font-medium mt-0.5', sector.color)}>
            {sector.label}
          </div>
        </div>
      </div>

      {/* Mood status — NEW V1 FEATURE 2 */}
      {mood && (
        <div className="flex items-center gap-2 rounded-lg bg-slate-50 px-3 py-2">
          <span className={cn('h-2 w-2 rounded-full', mood.dot)} />
          <span className={cn('text-xs font-medium', mood.color)}>{mood.label}</span>
          {p.moodNote && (
            <span className="text-xs text-slate-400 truncate">· {p.moodNote}</span>
          )}
        </div>
      )}

      {/* Stats row */}
      <div className="flex items-center justify-between text-sm">
        <div className="flex items-center gap-1 text-amber-500">
          <Star size={13} fill="currentColor" />
          <span className="font-semibold text-slate-800">{p.ratingAvg?.toFixed(1) || '—'}</span>
          <span className="text-slate-400 text-xs">({p.totalReviews})</span>
        </div>

        {p.city && (
          <div className="flex items-center gap-1 text-slate-400 text-xs">
            <MapPin size={11} />
            <span>{p.city}</span>
          </div>
        )}

        <div className="text-sm font-semibold text-slate-900">
          {formatCurrency(p.consultationFee)}
        </div>
      </div>

      {/* Next slot */}
      {p.nextAvailableSlot && (
        <div className="flex items-center gap-1.5 text-xs text-brand-600 font-medium border-t border-slate-100 pt-3">
          <span className="h-1.5 w-1.5 rounded-full bg-brand-500" />
          Next: {p.nextAvailableSlot}
        </div>
      )}

      {/* CTA */}
      <div className={cn(
        'text-center py-2 rounded-lg text-sm font-semibold transition-colors',
        p.acceptingBookings
          ? 'bg-brand-600 text-white hover:bg-brand-700'
          : 'bg-slate-100 text-slate-400 cursor-not-allowed'
      )}>
        {p.acceptingBookings ? 'Book Appointment' : 'Not accepting bookings'}
      </div>
    </Link>
  )
}

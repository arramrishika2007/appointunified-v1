import Link from 'next/link'
import { ProfessionalSummary } from '@/types'
import { VerifiedBadge } from '@/components/verification/VerifiedBadge'

export function ProfessionalCardV2({ professional }: { professional: ProfessionalSummary & { badgeTier?: 'NONE' | 'BRONZE' | 'SILVER' | 'GOLD' } }) {
  return (
    <div className="card p-4">
      <div className="flex items-start justify-between gap-3 mb-2">
        <div>
          <p className="font-semibold text-slate-900">{professional.displayName}</p>
          <p className="text-xs text-slate-500">{professional.specialty || professional.sector}</p>
        </div>
        <VerifiedBadge tier={professional.badgeTier || 'NONE'} />
      </div>

      <div className="text-xs text-slate-600 mb-3">
        Rating {professional.ratingAvg?.toFixed ? professional.ratingAvg.toFixed(1) : professional.ratingAvg} · {professional.totalReviews} reviews
      </div>

      <Link href={`/provider/${professional.id}`} className="btn-secondary text-xs px-3 py-1.5">
        View Profile
      </Link>
    </div>
  )
}

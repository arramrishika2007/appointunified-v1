import { BadgeTier } from '@/types/v2'
import { BADGE_CONFIG } from '@/lib/utils-v2'
import { cn } from '@/lib/utils'

interface VerifiedBadgeProps {
  tier: BadgeTier
  className?: string
}

export function VerifiedBadge({ tier, className }: VerifiedBadgeProps) {
  const cfg = BADGE_CONFIG[tier]
  return (
    <span className={cn('inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-semibold', cfg.bg, cfg.color, className)}>
      <span>{cfg.icon}</span>
      <span>{cfg.label}</span>
    </span>
  )
}

export function BadgeCard({ tier }: { tier: BadgeTier }) {
  const cfg = BADGE_CONFIG[tier]
  return (
    <div className={cn('card p-4 border flex items-center justify-between', cfg.bg)}>
      <div>
        <p className="text-xs text-slate-500">Current Trust Tier</p>
        <p className={cn('text-lg font-bold', cfg.color)}>{cfg.label}</p>
      </div>
      <div className="text-3xl" aria-hidden>
        {cfg.icon}
      </div>
    </div>
  )
}

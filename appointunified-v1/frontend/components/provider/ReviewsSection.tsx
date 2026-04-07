'use client'

import { useEffect, useState } from 'react'
import { Flag, Star, ThumbsUp } from 'lucide-react'
import { api } from '@/lib/api'
import { cn, getInitials, timeAgo } from '@/lib/utils'

interface Review {
  id: string
  reviewerName: string
  reviewerAvatar?: string
  rating: number
  comment?: string
  helpfulCount: number
  createdAt: string
}

interface Props {
  professionalId: string
}

export function ReviewsSection({ professionalId }: Props) {
  const [reviews, setReviews] = useState<Review[]>([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  useEffect(() => {
    setLoading(true)
    api.get(`/reviews/professional/${professionalId}?page=${page}&size=5`)
      .then(res => {
        setReviews(prev => page === 0 ? res.data.data.content : [...prev, ...res.data.data.content])
        setTotalPages(res.data.data.totalPages)
      })
      .finally(() => setLoading(false))
  }, [professionalId, page])

  const handleHelpful = async (id: string) => {
    await api.post(`/reviews/${id}/helpful`)
    setReviews(prev => prev.map(r => r.id === id ? { ...r, helpfulCount: r.helpfulCount + 1 } : r))
  }

  const handleFlag = async (id: string) => {
    if (!confirm('Flag this review as inappropriate?')) return
    await api.post(`/reviews/${id}/flag`)
  }

  const avgRating = reviews.length
    ? reviews.reduce((s, r) => s + r.rating, 0) / reviews.length
    : 0

  return (
    <div className="card p-5">
      <div className="flex items-center justify-between mb-5">
        <h2 className="font-semibold text-slate-900">
          Reviews <span className="text-slate-400 font-normal text-sm">({reviews.length})</span>
        </h2>
        {avgRating > 0 && (
          <div className="flex items-center gap-1.5">
            {Array.from({ length: 5 }, (_, i) => (
              <Star
                key={i}
                size={14}
                className={i < Math.round(avgRating) ? 'text-amber-500 fill-current' : 'text-slate-300'}
              />
            ))}
            <span className="text-sm font-semibold text-slate-700 ml-1">{avgRating.toFixed(1)}</span>
          </div>
        )}
      </div>

      {loading && reviews.length === 0 ? (
        <div className="space-y-4">
          {[1, 2, 3].map(i => (
            <div key={i} className="space-y-2">
              <div className="flex items-center gap-2">
                <div className="skeleton h-8 w-8 rounded-full" />
                <div className="skeleton h-3 w-32" />
              </div>
              <div className="skeleton h-4 w-full" />
              <div className="skeleton h-4 w-3/4" />
            </div>
          ))}
        </div>
      ) : reviews.length === 0 ? (
        <p className="text-sm text-slate-400 text-center py-6">No reviews yet. Be the first!</p>
      ) : (
        <div className="divide-y divide-slate-100">
          {reviews.map(r => (
            <div key={r.id} className="py-4 first:pt-0">
              <div className="flex items-start gap-3">
                {/* Avatar */}
                <div className="h-8 w-8 rounded-full bg-brand-100 flex items-center justify-center text-brand-700 text-xs font-bold flex-shrink-0 overflow-hidden">
                  {r.reviewerAvatar
                    ? <img src={r.reviewerAvatar} alt="" className="h-full w-full object-cover" />
                    : getInitials(r.reviewerName)
                  }
                </div>

                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between gap-2 mb-1">
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-semibold text-slate-900">{r.reviewerName}</span>
                      <div className="flex">
                        {Array.from({ length: 5 }, (_, i) => (
                          <Star
                            key={i}
                            size={11}
                            className={i < r.rating ? 'text-amber-500 fill-current' : 'text-slate-200'}
                          />
                        ))}
                      </div>
                    </div>
                    <span className="text-xs text-slate-400 flex-shrink-0">{timeAgo(r.createdAt)}</span>
                  </div>

                  {r.comment && (
                    <p className="text-sm text-slate-600 leading-relaxed mb-2">{r.comment}</p>
                  )}

                  <div className="flex items-center gap-3">
                    <button
                      onClick={() => handleHelpful(r.id)}
                      className="flex items-center gap-1 text-xs text-slate-400 hover:text-slate-600 transition-colors"
                    >
                      <ThumbsUp size={11} />
                      Helpful {r.helpfulCount > 0 && `(${r.helpfulCount})`}
                    </button>
                    <button
                      onClick={() => handleFlag(r.id)}
                      className="flex items-center gap-1 text-xs text-slate-400 hover:text-red-500 transition-colors"
                    >
                      <Flag size={11} />
                      Report
                    </button>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {page < totalPages - 1 && (
        <button
          onClick={() => setPage(p => p + 1)}
          disabled={loading}
          className="btn-secondary w-full mt-4 text-sm"
        >
          {loading ? 'Loading…' : 'Load more reviews'}
        </button>
      )}
    </div>
  )
}

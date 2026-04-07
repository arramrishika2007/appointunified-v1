'use client'

import { useState } from 'react'
import { Loader2, Star } from 'lucide-react'
import { api } from '@/lib/api'
import { cn } from '@/lib/utils'
import toast from 'react-hot-toast'

interface Props {
  appointmentId: string
  professionalName: string
  onSubmitted?: () => void
}

export function SubmitReview({ appointmentId, professionalName, onSubmitted }: Props) {
  const [rating, setRating] = useState(0)
  const [hovered, setHovered] = useState(0)
  const [comment, setComment] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [done, setDone] = useState(false)

  const handleSubmit = async () => {
    if (rating === 0) { toast.error('Please select a rating'); return }
    setSubmitting(true)
    try {
      await api.post('/reviews', {
        appointmentId,
        rating,
        comment: comment.trim() || undefined,
      })
      setDone(true)
      toast.success('Thank you for your review!')
      onSubmitted?.()
    } catch (err: unknown) {
      toast.error(
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message
          ?? 'Could not submit review'
      )
    } finally {
      setSubmitting(false)
    }
  }

  if (done) {
    return (
      <div className="card p-5 text-center border-emerald-200 bg-emerald-50">
        <p className="text-2xl mb-1">⭐</p>
        <p className="font-semibold text-emerald-800">Review submitted!</p>
        <p className="text-xs text-emerald-600 mt-0.5">Thanks for helping others find great providers.</p>
      </div>
    )
  }

  const LABELS = ['', 'Poor', 'Fair', 'Good', 'Very Good', 'Excellent']

  return (
    <div className="card p-5 border-brand-200 bg-brand-50/40">
      <p className="text-sm font-semibold text-slate-900 mb-1">
        How was your appointment with {professionalName}?
      </p>
      <p className="text-xs text-slate-500 mb-4">Your review helps others make better decisions.</p>

      {/* Star selector */}
      <div className="flex items-center gap-1 mb-1">
        {[1, 2, 3, 4, 5].map(star => (
          <button
            key={star}
            type="button"
            onClick={() => setRating(star)}
            onMouseEnter={() => setHovered(star)}
            onMouseLeave={() => setHovered(0)}
            className="transition-transform hover:scale-110"
            aria-label={`Set rating to ${star} star${star === 1 ? '' : 's'}`}
            title={`Set rating to ${star} star${star === 1 ? '' : 's'}`}
          >
            <Star
              size={28}
              className={cn(
                'transition-colors',
                star <= (hovered || rating)
                  ? 'text-amber-400 fill-current'
                  : 'text-slate-300'
              )}
            />
          </button>
        ))}
        {(hovered || rating) > 0 && (
          <span className="ml-2 text-sm font-medium text-amber-600">
            {LABELS[hovered || rating]}
          </span>
        )}
      </div>

      {/* Comment */}
      <textarea
        value={comment}
        onChange={e => setComment(e.target.value)}
        rows={3}
        className="input resize-none mt-3 mb-3"
        placeholder="Tell others what you thought… (optional)"
        maxLength={1000}
      />

      <button
        onClick={handleSubmit}
        disabled={submitting || rating === 0}
        className="btn-primary w-full"
        aria-label={`Submit review for ${professionalName}`}
        title={`Submit review for ${professionalName}`}
      >
        {submitting ? <Loader2 size={15} className="animate-spin" /> : '★ Submit Review'}
      </button>
    </div>
  )
}

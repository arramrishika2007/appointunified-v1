'use client'

import { useEffect, useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { format } from 'date-fns'
import { ArrowLeft, Calendar, Clock, DollarSign, MapPin, Share2, Star } from 'lucide-react'
import Link from 'next/link'
import { Navbar } from '@/components/layout/Navbar'
import { professionalsApi } from '@/lib/api'
import { ProfessionalDetail } from '@/types'
import { cn, formatCurrency, getInitials, MOOD_CONFIG, SECTOR_CONFIG, weekdayName } from '@/lib/utils'

export default function ProviderProfilePage() {
  const params = useParams()
  const router = useRouter()
  const id = params.id as string

  const [provider, setProvider] = useState<ProfessionalDetail | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    professionalsApi.getById(id)
      .then((res) => setProvider(res.data.data))
      .catch(() => router.push('/explore/healthcare'))
      .finally(() => setLoading(false))
  }, [id, router])

  if (loading) {
    return (
      <>
        <Navbar />
        <div className="container-page py-10 space-y-4">
          <div className="skeleton h-56 rounded-2xl" />
          <div className="skeleton h-8 w-64" />
          <div className="skeleton h-4 w-48" />
        </div>
      </>
    )
  }

  if (!provider) return null

  const sector = SECTOR_CONFIG[provider.sector]
  const mood = provider.availabilityMood ? MOOD_CONFIG[provider.availabilityMood] : null

  return (
    <>
      <Navbar />
      <main className="min-h-screen bg-slate-50">

        {/* Cover */}
        <div className="relative h-52 bg-gradient-to-br from-brand-400 via-brand-500 to-brand-700">
          <div className="absolute inset-0 bg-white/10 backdrop-blur-[2px]"></div>
          {provider.coverUrl && (
            <img src={provider.coverUrl} alt="" className="absolute inset-0 h-full w-full object-cover opacity-50 mix-blend-overlay" />
          )}
          <div className="container-page h-full flex items-end pb-0 relative z-10">
            <button onClick={() => router.back()} className="absolute top-6 left-4 sm:left-8 flex items-center gap-1.5 text-white/90 hover:text-white text-sm font-medium transition-all hover:-translate-x-1 drop-shadow-md">
              <ArrowLeft size={16} /> Back
            </button>
          </div>
        </div>

        <div className="container-page">
          {/* Avatar + header */}
          <div className="flex flex-col sm:flex-row sm:items-end gap-4 -mt-12 mb-8">
            <div className="relative z-20 h-24 w-24 rounded-2xl border-[3px] border-white/80 bg-white/80 backdrop-blur-xl flex items-center justify-center text-brand-700 font-bold text-3xl overflow-hidden shadow-[0_8px_30px_rgb(0,0,0,0.08)] flex-shrink-0">
              {provider.avatarUrl
                ? <img src={provider.avatarUrl} alt={provider.displayName} className="h-full w-full object-cover" />
                : getInitials(provider.displayName)
              }
            </div>

            <div className="flex-1">
              <div className="flex flex-wrap items-center gap-2 mb-1">
                <h1 className="text-2xl font-bold text-slate-900">{provider.displayName}</h1>
                {provider.verificationStatus === 'APPROVED' && (
                  <span className="badge-verified">✓ Verified</span>
                )}
                <span className={cn('badge text-xs', sector.bg, sector.color)}>
                  {sector.icon} {sector.label}
                </span>
              </div>
              {provider.specialty && (
                <p className="text-slate-600">{provider.specialty}</p>
              )}
              {provider.licenseNumber && (
                <p className="text-xs text-slate-400 mt-0.5">Reg. #{provider.licenseNumber}</p>
              )}
            </div>

            <div className="flex gap-2">
              <button className="btn-secondary gap-2">
                <Share2 size={15} /> Share
              </button>
              {provider.acceptingBookings && (
                <Link href={`/booking/${provider.id}`} className="btn-primary">
                  Book Appointment
                </Link>
              )}
            </div>
          </div>

          <div className="grid lg:grid-cols-3 gap-6">

            {/* Left column */}
            <div className="lg:col-span-2 space-y-5">

              {/* Mood Status — NEW V1 FEATURE 2 */}
              {mood && (
                <div className={cn('card p-4 flex items-center gap-3')}>
                  <div className={cn('h-3 w-3 rounded-full', mood.dot)} />
                  <div>
                    <span className={cn('text-sm font-semibold', mood.color)}>{mood.label}</span>
                    {provider.moodNote && (
                      <p className="text-xs text-slate-500 mt-0.5">{provider.moodNote}</p>
                    )}
                  </div>
                </div>
              )}

              {/* Stats */}
              <div className="card p-5 grid grid-cols-3 gap-4 text-center divide-x divide-slate-100 backdrop-blur-md bg-white shadow-[0_4px_24px_rgb(0,0,0,0.02)]">
                <div>
                  <div className="flex items-center justify-center gap-1 text-amber-500 text-lg font-bold">
                    <Star size={16} fill="currentColor" />
                    {provider.ratingAvg?.toFixed(1) || '—'}
                  </div>
                  <p className="text-xs text-slate-500 mt-0.5">{provider.totalReviews} reviews</p>
                </div>
                <div>
                  <div className="text-lg font-bold text-slate-900">{provider.totalCompleted}</div>
                  <p className="text-xs text-slate-500 mt-0.5">Completed</p>
                </div>
                <div>
                  <div className="text-lg font-bold text-slate-900">{provider.yearsExperience ?? '—'}y</div>
                  <p className="text-xs text-slate-500 mt-0.5">Experience</p>
                </div>
              </div>

              {/* About */}
              {provider.bio && (
                <div className="card p-5 shadow-[0_4px_24px_rgb(0,0,0,0.02)]">
                  <h2 className="font-semibold text-slate-900 mb-2">About</h2>
                  <p className="text-sm text-slate-600 leading-relaxed">{provider.bio}</p>
                </div>
              )}

              {/* Qualification */}
              {provider.qualification && (
                <div className="card p-5">
                  <h2 className="font-semibold text-slate-900 mb-2">Qualifications</h2>
                  <p className="text-sm text-slate-600 leading-relaxed">{provider.qualification}</p>
                </div>
              )}

              {/* Services */}
              {provider.services && provider.services.length > 0 && (
                <div className="card p-5 shadow-[0_4px_24px_rgb(0,0,0,0.02)]">
                  <h2 className="font-semibold text-slate-900 mb-4 tracking-tight">Services</h2>
                  <div className="space-y-3">
                    {provider.services.map((svc) => (
                      <div key={svc.id} className="group flex flex-col sm:flex-row sm:items-center justify-between p-4 rounded-xl border border-slate-100 bg-white hover:bg-brand-50/30 hover:border-brand-200 hover:shadow-sm transition-all hover:-translate-y-0.5 gap-4 sm:gap-2">
                        <div className="flex-1">
                          <p className="text-sm font-bold text-slate-900 group-hover:text-brand-700 transition-colors">{svc.name}</p>
                          {svc.description && <p className="text-xs text-slate-500 mt-1 line-clamp-2 leading-relaxed">{svc.description}</p>}
                          <div className="flex items-center gap-3 mt-2 text-[11px] text-slate-400 font-medium">
                            <span className="flex items-center gap-1"><Clock size={11} /> {svc.durationMinutes}m</span>
                            {svc.isVirtual && <span>🖥️ Virtual</span>}
                          </div>
                        </div>
                        <div className="flex items-center gap-3">
                          <span className="font-semibold text-slate-900 text-sm">{formatCurrency(svc.price)}</span>
                          {provider.acceptingBookings && (
                            <Link href={`/booking/${provider.id}?service=${svc.id}`} className="btn-primary px-3 py-1.5 text-xs">
                              Book
                            </Link>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>

            {/* Right column */}
            <div className="space-y-5">

              {/* Quick info */}
              <div className="card p-5 space-y-3">
                <h2 className="font-semibold text-slate-900 mb-1">Details</h2>
                {provider.consultationFee != null && (
                  <div className="flex items-center gap-2.5 text-sm text-slate-600">
                    <DollarSign size={15} className="text-slate-400" />
                    {formatCurrency(provider.consultationFee)} consultation
                  </div>
                )}
                {provider.city && (
                  <div className="flex items-center gap-2.5 text-sm text-slate-600">
                    <MapPin size={15} className="text-slate-400" />
                    {provider.city}
                    {provider.address && <span className="text-slate-400">· {provider.address}</span>}
                  </div>
                )}
                {provider.joinedAt && (
                  <div className="flex items-center gap-2.5 text-sm text-slate-600">
                    <Calendar size={15} className="text-slate-400" />
                    Joined {format(new Date(provider.joinedAt), 'MMM yyyy')}
                  </div>
                )}
              </div>

              {/* Weekly schedule */}
              {provider.weeklySchedule && provider.weeklySchedule.length > 0 && (
                <div className="card p-5">
                  <h2 className="font-semibold text-slate-900 mb-3">Weekly Schedule</h2>
                  <div className="space-y-2">
                    {Array.from({ length: 7 }, (_, i) => {
                      const daySchedule = provider.weeklySchedule.find((d) => d.weekday === i)
                      return (
                        <div key={i} className="flex items-center justify-between text-sm">
                          <span className="text-slate-500 w-8">{weekdayName(i)}</span>
                          {daySchedule ? (
                            <span className="text-slate-700 font-medium">
                              {daySchedule.startTime} – {daySchedule.endTime}
                            </span>
                          ) : (
                            <span className="text-slate-300">Closed</span>
                          )}
                        </div>
                      )
                    })}
                  </div>
                </div>
              )}

              {/* Book CTA */}
              {provider.acceptingBookings ? (
                <Link href={`/booking/${provider.id}`} className="btn-primary w-full py-3 text-base">
                  Book Appointment
                </Link>
              ) : (
                <div className="card p-4 text-center">
                  <p className="text-sm text-slate-500">Not accepting bookings right now.</p>
                </div>
              )}
            </div>

          </div>
        </div>
      </main>
    </>
  )
}

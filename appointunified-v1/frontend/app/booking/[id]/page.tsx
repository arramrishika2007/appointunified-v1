'use client'

import { useEffect, useState } from 'react'
import { useParams, useRouter, useSearchParams } from 'next/navigation'
import dynamic from 'next/dynamic'
import { addDays, format, isBefore, startOfDay, startOfMonth } from 'date-fns'
import { ArrowLeft, Calendar, Check, Clock, ExternalLink, FileText, Loader2, MapPin, MessageSquare, Monitor, Wifi } from 'lucide-react'
import Link from 'next/link'
import { Navbar } from '@/components/layout/Navbar'
import { appointmentsApi, geoApi, professionalsApi, waitlistApi } from '@/lib/api'
import { useAuthStore } from '@/lib/store'
import { AvailableSlot, ProfessionalDetail, ServiceSummary } from '@/types'
import { cn, formatCurrency, formatTimeOnly } from '@/lib/utils'
import toast from 'react-hot-toast'

import { BespokeBookingCalendar } from '@/components/booking/BespokeBookingCalendar'
import { LocationMapModal } from '@/components/booking/LocationMapModal'

const OfflineLiveMap = dynamic(() => import('@/components/booking/OfflineLiveMap'), { ssr: false })

const STEPS = ['Service', 'Schedule', 'Confirm']

export default function BookingPage() {
  const params = useParams()
  const searchParams = useSearchParams()
  const router = useRouter()
  const { isAuthenticated, user } = useAuthStore()

  const professionalId = params.id as string
  const preselectedServiceId = searchParams.get('service')
  const workflowInstanceId = searchParams.get('workflowInstance') || searchParams.get('workflowInstanceId')
  const workflowStepRaw = searchParams.get('workflowStep')
  const workflowStepOrder = workflowStepRaw ? Number.parseInt(workflowStepRaw, 10) : undefined
  const normalizedWorkflowStepOrder = workflowStepOrder !== undefined && Number.isFinite(workflowStepOrder)
    ? workflowStepOrder
    : undefined

  const [provider, setProvider] = useState<ProfessionalDetail | null>(null)
  const [selectedService, setSelectedService] = useState<ServiceSummary | null>(null)
  const [selectedDate, setSelectedDate] = useState<Date | null>(null)
  const [selectedSlot, setSelectedSlot] = useState<AvailableSlot | null>(null)
  const [notes, setNotes] = useState('')
  const [slots, setSlots] = useState<AvailableSlot[]>([])
  const [slotsLoading, setSlotsLoading] = useState(false)
  const [step, setStep] = useState(1)
  const [meetingMode, setMeetingMode] = useState<'ONLINE' | 'OFFLINE'>('OFFLINE')
  const [submitting, setSubmitting] = useState(false)
  const [showWaitlistCta, setShowWaitlistCta] = useState(false)
  const [joiningWaitlist, setJoiningWaitlist] = useState(false)
  const [travelInfo, setTravelInfo] = useState<{ distanceKm: number; durationMinutes: number } | null>(null)
  const [userLocation, setUserLocation] = useState<{ lat: number; lng: number } | null>(null)
  const [travelLoading, setTravelLoading] = useState(false)
  const [mapOpen, setMapOpen] = useState(false)
  const providerServices = Array.isArray(provider?.services) ? provider.services : []

  // Redirect if not authenticated
  useEffect(() => {
    if (!isAuthenticated) {
      router.push(`/auth/login?redirect=/booking/${professionalId}`)
    }
  }, [isAuthenticated, professionalId, router])

  // Load provider
  useEffect(() => {
    professionalsApi.getById(professionalId)
      .then((res) => {
        const raw = res.data.data as ProfessionalDetail
        const p: ProfessionalDetail = {
          ...raw,
          services: Array.isArray(raw?.services) ? raw.services : [],
        }
        setProvider(p)
        if (preselectedServiceId) {
          const svc = p.services.find((s) => s.id === preselectedServiceId)
          if (svc) {
            setSelectedService(svc)
            setMeetingMode(svc.isVirtual ? 'ONLINE' : 'OFFLINE')
            setStep(2)
          }
        }
      })
      .catch(() => {
        toast.error('Unable to load provider profile right now.')
        router.push('/explore/healthcare')
      })
  }, [professionalId, preselectedServiceId, router])

  const [currentMonth, setCurrentMonth] = useState<Date>(startOfMonth(new Date()))

  // Load slots when date selected
  useEffect(() => {
    if (!selectedDate || !selectedService) return
    setSlotsLoading(true)
    setSlots([]) // clear old slots
    professionalsApi
      .getSlots(professionalId, format(selectedDate, 'yyyy-MM-dd'), selectedService.id)
      .then((res) => setSlots(res.data?.data || []))
      .finally(() => setSlotsLoading(false))
  }, [selectedDate, selectedService, professionalId])

  // Auto-save draft when step changes (NEW V1 FEATURE 3)
  useEffect(() => {
    if (step < 2) return
    appointmentsApi.saveDraft({
      professionalId,
      serviceId: selectedService?.id,
      stepReached: step,
      draftData: {
        selectedDate: selectedDate?.toISOString(),
        selectedSlot,
        notes,
      },
    }).catch(() => {}) // silent — draft is best-effort
  }, [step, selectedService, selectedDate, selectedSlot, notes, professionalId])

  const handleSubmit = async () => {
    if (!selectedService || !selectedSlot) return
    setSubmitting(true)
    setShowWaitlistCta(false)
    try {
      const res = await appointmentsApi.create({
        professionalId,
        serviceId: selectedService.id,
        startTime: selectedSlot.startTime,
        notes: notes || undefined,
        virtual: meetingMode === 'ONLINE',
        clientLat: meetingMode === 'OFFLINE' ? userLocation?.lat : undefined,
        clientLon: meetingMode === 'OFFLINE' ? userLocation?.lng : undefined,
        workflowInstanceId: workflowInstanceId || undefined,
        workflowStepOrder: normalizedWorkflowStepOrder,
      })
      
      const appointmentId = res.data.data.id
      const appointmentData = res.data.data
      
      // Check if this is an online booking that requires payment
      if (meetingMode === 'ONLINE') {
        // Redirect to confirmation page with payment flow
        router.push(`/booking/confirm?appointmentId=${appointmentId}`)
        toast.success('Appointment booked! Proceeding to payment.')
      } else {
        // Offline booking - no payment required
        toast.success('Appointment booked! Confirmation sent to your email.')
        router.push(`/dashboard/bookings?booked=${appointmentId}`)
      }
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Booking failed. Please try again.'
      toast.error(msg)
      if (msg.toLowerCase().includes('no longer available') || msg.toLowerCase().includes('not available')) {
        setShowWaitlistCta(true)
      }
    } finally {
      setSubmitting(false)
    }
  }

  const handleJoinWaitlist = async () => {
    if (!selectedService) return
    setJoiningWaitlist(true)
    try {
      await waitlistApi.join({
        professionalId,
        serviceId: selectedService.id,
      })
      toast.success('Added to waitlist. We will notify you when a slot opens.')
      setShowWaitlistCta(false)
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Could not join waitlist right now.'
      toast.error(msg)
    } finally {
      setJoiningWaitlist(false)
    }
  }

  const fetchTravelEstimate = async (lat: number, lng: number) => {
    if (!provider?.latitude || !provider?.longitude) return
    try {
      const res = await geoApi.getTravelTime({
        fromLat: lat,
        fromLng: lng,
        toLat: Number(provider.latitude),
        toLng: Number(provider.longitude),
      })
      setTravelInfo({
        distanceKm: Number(res.data.data.distanceKm),
        durationMinutes: Number(res.data.data.durationMinutes),
      })
    } catch {
      toast.error('Could not estimate travel time')
    }
  }

  const estimateTravel = async () => {
    if (!provider?.latitude || !provider?.longitude || !navigator.geolocation) return
    setTravelLoading(true)
    navigator.geolocation.getCurrentPosition(
      async (pos) => {
        const lat = pos.coords.latitude
        const lng = pos.coords.longitude
        setUserLocation({ lat, lng })
        try {
          await fetchTravelEstimate(lat, lng)
        } catch {
          toast.error('Could not estimate travel time')
        } finally {
          setTravelLoading(false)
        }
      },
      () => setTravelLoading(false),
      { enableHighAccuracy: true, timeout: 10000 }
    )
  }

  useEffect(() => {
    if (step !== 2) return
    if (!selectedService || meetingMode === 'ONLINE') return
    if (!provider?.latitude || !provider?.longitude) return

    void estimateTravel()
    const timer = setInterval(() => {
      void estimateTravel()
    }, 30000)

    return () => clearInterval(timer)
  }, [step, selectedService, meetingMode, provider?.latitude, provider?.longitude])

  const openProviderMap = () => {
    if (!provider?.latitude || !provider?.longitude) return
    setMapOpen(true)
  }

  if (!provider) {
    return (
      <>
        <Navbar />
        <div className="container-page py-20 flex justify-center">
          <Loader2 size={32} className="animate-spin text-brand-600" />
        </div>
      </>
    )
  }

  // Build selectable dates (no longer strictly needed for UI, but kept for logic if any)
  const today = startOfDay(new Date())

  return (
    <>
      <Navbar />
      <main className="min-h-screen bg-slate-50">
        <div className="container-page py-8 max-w-5xl">

          {/* Back */}
          <button onClick={() => router.back()} className="btn-ghost mb-6 -ml-2 text-slate-500 hover:text-slate-900 transition-colors">
            <ArrowLeft size={16} /> Back to Provider
          </button>

          {/* Progress steps */}
          <div className="flex items-center gap-2 mb-8">
            {STEPS.map((label, i) => {
              const num = i + 1
              const done = num < step
              const active = num === step
              return (
                <div key={label} className="flex items-center gap-2 flex-1">
                  <div className={cn(
                    'flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-full text-xs font-bold transition-all',
                    done   ? 'bg-emerald-500 text-white' :
                    active ? 'bg-brand-600 text-white' :
                             'bg-slate-200 text-slate-500'
                  )}>
                    {done ? <Check size={12} /> : num}
                  </div>
                  <span className={cn(
                    'text-xs font-medium hidden sm:block',
                    active ? 'text-brand-700' : done ? 'text-emerald-600' : 'text-slate-400'
                  )}>{label}</span>
                  {i < STEPS.length - 1 && (
                    <div className={cn('h-px flex-1', done ? 'bg-emerald-300' : 'bg-slate-200')} />
                  )}
                </div>
              )
            })}
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
            <div className="lg:col-span-2">
              <div className="card md:p-8 p-5 shadow-[0_8px_30px_rgb(0,0,0,0.04)] animate-in fade-in duration-300">

            {/* STEP 1: Select Service */}
            {step === 1 && (
              <div>
                <h2 className="text-xl font-bold text-slate-900 mb-5">Select a Service</h2>
                {providerServices.filter((s) => s.isActive).length === 0 ? (
                  <p className="text-slate-500">No services available.</p>
                ) : (
                  <div className="space-y-3">
                    {providerServices.filter((s) => s.isActive).map((svc) => (
                      <button
                        key={svc.id}
                        onClick={() => {
                          setSelectedService(svc)
                          setMeetingMode(svc.isVirtual ? 'ONLINE' : 'OFFLINE')
                          setStep(2)
                        }}
                        className="w-full relative group rounded-xl border-2 border-slate-100 hover:border-brand-400 bg-white hover:bg-brand-50/20 p-5 text-left transition-all hover:-translate-y-0.5 shadow-sm hover:shadow-md"
                      >
                        <div className="flex items-start justify-between">
                          <div>
                            <p className="font-semibold text-slate-900 group-hover:text-brand-700 transition-colors">{svc.name}</p>
                            {svc.description && <p className="text-sm text-slate-500 mt-1 line-clamp-2 leading-relaxed">{svc.description}</p>}
                            <div className="flex items-center gap-3 mt-3 text-[11px] text-slate-400 font-medium">
                              <span className="flex items-center gap-1"><Clock size={12} /> {svc.durationMinutes} min</span>
                              {svc.isVirtual ? (
                                <span className="inline-flex items-center gap-1 text-brand-600"><Wifi size={11} /> Online</span>
                              ) : (
                                <span className="inline-flex items-center gap-1 text-slate-500"><MapPin size={11} /> Offline</span>
                              )}
                              {svc.requiresDocuments && <span className="inline-flex items-center gap-1"><FileText size={11} /> Documents req.</span>}
                            </div>
                          </div>
                          <span className="font-bold text-slate-900 ml-4 bg-slate-50 px-3 py-1.5 rounded-lg group-hover:bg-brand-100 group-hover:text-brand-700 transition-colors">{formatCurrency(svc.price)}</span>
                        </div>
                      </button>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* STEP 2: Schedule (Custom Split Calendar) */}
            {step === 2 && (
              <div>
                <div className="flex items-center justify-between mb-5">
                  <h2 className="text-xl font-bold text-slate-900">Select Date & Time</h2>
                  <button onClick={() => setStep(1)} className="btn-ghost text-xs">Change service</button>
                </div>
                <p className="text-sm text-slate-500 mb-5">Service: <span className="font-medium text-slate-800">{selectedService?.name}</span></p>

                {selectedService && (
                  <div className="mb-5 space-y-3 rounded-xl border border-slate-200 bg-white p-3 text-xs font-medium">
                    <div className="inline-flex rounded-full border border-slate-200 bg-slate-50 p-1">
                      <button
                        type="button"
                        onClick={() => setMeetingMode('ONLINE')}
                        className={cn(
                          'inline-flex items-center gap-1.5 rounded-full px-3 py-1 transition-colors',
                          meetingMode === 'ONLINE' ? 'bg-brand-600 text-white' : 'text-slate-600 hover:bg-slate-200'
                        )}
                      >
                        <Monitor size={12} /> Online
                      </button>
                      <button
                        type="button"
                        onClick={() => setMeetingMode('OFFLINE')}
                        className={cn(
                          'inline-flex items-center gap-1.5 rounded-full px-3 py-1 transition-colors',
                          meetingMode === 'OFFLINE' ? 'bg-slate-900 text-white' : 'text-slate-600 hover:bg-slate-200'
                        )}
                      >
                        <MapPin size={12} /> Offline
                      </button>
                    </div>

                    <span className={cn('inline-flex items-center gap-1.5 rounded-full px-3 py-1', meetingMode === 'ONLINE' ? 'bg-brand-100 text-brand-700' : 'bg-slate-100 text-slate-600')}>
                      {meetingMode === 'ONLINE' ? <><Monitor size={12} /> Online booking</> : <><MapPin size={12} /> Offline booking</>}
                    </span>
                    <span className="text-slate-500">
                      {meetingMode === 'ONLINE'
                        ? 'You will get an online meeting link after booking.'
                        : 'Offline mode uses map-led travel checks and skips payment confirmation steps.'}
                    </span>
                  </div>
                )}

                {selectedService && meetingMode === 'OFFLINE' && (
                  <div className="mb-5 rounded-xl border border-slate-200 bg-white p-4">
                    <div className="flex items-center justify-between gap-3">
                      <p className="text-xs text-slate-600 inline-flex items-center gap-1"><MapPin size={12} /> Offline map and travel estimate</p>
                      <div className="flex items-center gap-2">
                        <button onClick={openProviderMap} className="btn-ghost text-xs" disabled={!provider.latitude || !provider.longitude}>
                          <ExternalLink size={12} /> Open Map
                        </button>
                        <button onClick={estimateTravel} className="btn-ghost text-xs" disabled={travelLoading}>
                        {travelLoading ? <><Loader2 size={12} className="animate-spin" /> Estimating…</> : 'Estimate'}
                        </button>
                      </div>
                    </div>
                    {travelInfo && (
                      <p className="text-xs text-slate-500 mt-2">
                        Professional is {travelInfo.distanceKm.toFixed(1)} km away · Estimated travel time: {travelInfo.durationMinutes} mins
                      </p>
                    )}
                    {provider.latitude && provider.longitude && (
                      <div className="mt-3">
                        <OfflineLiveMap
                          provider={{ lat: Number(provider.latitude), lng: Number(provider.longitude) }}
                          client={userLocation}
                        />
                        <p className="mt-2 text-[11px] text-slate-500">Map is the primary view for offline booking. Use Open Map anytime to open the same map inside the app.</p>
                      </div>
                    )}
                  </div>
                )}

                <BespokeBookingCalendar 
                    currentMonth={currentMonth}
                    onMonthChange={setCurrentMonth}
                    selectedDate={selectedDate}
                    onDateSelect={(date) => {
                        setSelectedDate(date)
                        setSelectedSlot(null) // Reset time slot when changing dates
                    }}
                    selectedSlot={selectedSlot}
                    onSlotSelect={setSelectedSlot}
                    slotsForSelectedDate={slots}
                    isLoadingSlots={slotsLoading}
                    onConfirm={() => setStep(3)} // Moving to confirm step
                />
              </div>
            )}

            {/* STEP 3: Confirm */}
            {step === 3 && selectedService && selectedSlot && (
              <div>
                <div className="flex items-center justify-between mb-5">
                  <h2 className="text-xl font-bold text-slate-900">Confirm Booking</h2>
                  <button onClick={() => setStep(2)} className="btn-ghost text-xs">Change time</button>
                </div>

                {/* Summary block hidden on desktop because of split pane */}
                <div className="rounded-xl bg-slate-50 p-5 space-y-3 mb-5 lg:hidden">
                  <div className="flex justify-between text-sm">
                    <span className="text-slate-500">Provider</span>
                    <span className="font-medium text-slate-900">{provider.displayName}</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span className="text-slate-500">Service</span>
                    <span className="font-medium text-slate-900">{selectedService.name}</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span className="text-slate-500">Date</span>
                    <span className="font-medium text-slate-900">{format(new Date(selectedSlot.startTime), 'EEE, MMM d, yyyy')}</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span className="text-slate-500">Time</span>
                    <span className="font-medium text-slate-900">
                      {formatTimeOnly(selectedSlot.startTime)} – {formatTimeOnly(selectedSlot.endTime)}
                    </span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span className="text-slate-500">Duration</span>
                    <span className="font-medium text-slate-900">{selectedService.durationMinutes} min</span>
                  </div>
                  <div className="border-t border-slate-200 pt-3 flex justify-between">
                    <span className="font-semibold text-slate-700">Total</span>
                    <span className="font-bold text-lg text-slate-900">{formatCurrency(selectedService.price)}</span>
                  </div>
                </div>

                {/* Notes */}
                <div className="mb-5">
                  <label className="label flex items-center gap-1.5">
                    <MessageSquare size={14} /> Add a note <span className="font-normal text-slate-400">(optional)</span>
                  </label>
                  <textarea
                    value={notes}
                    onChange={(e) => setNotes(e.target.value)}
                    rows={3}
                    className="input resize-none"
                    placeholder="Reason for visit, documents to bring, etc."
                    maxLength={500}
                  />
                </div>

                <button
                  onClick={handleSubmit}
                  disabled={submitting}
                  className="btn-primary w-full py-3.5 text-base"
                >
                  {submitting
                    ? <><Loader2 size={18} className="animate-spin" /> Booking…</>
                    : 'Confirm Appointment'
                  }
                </button>

                <p className="text-xs text-center text-slate-400 mt-3">
                  {meetingMode === 'ONLINE'
                    ? 'You will receive a confirmation email with meeting details and payment timeline.'
                    : 'You will receive offline booking confirmation with map and visit details. Payment confirmation is skipped for offline mode.'}
                </p>

                {meetingMode === 'OFFLINE' && (
                  <div className="mt-3 flex justify-center">
                    <button onClick={openProviderMap} className="btn-ghost text-xs px-3 py-2" disabled={!provider.latitude || !provider.longitude}>
                      <ExternalLink size={13} /> View Map Anytime
                    </button>
                  </div>
                )}

                {showWaitlistCta && (
                  <div className="mt-4 rounded-xl border border-amber-200 bg-amber-50 p-3">
                    <p className="text-xs text-amber-800 mb-2">
                      This slot was just taken. Join the waitlist for this service and get notified when a slot opens.
                    </p>
                    <button
                      onClick={handleJoinWaitlist}
                      disabled={joiningWaitlist}
                      className="btn-secondary text-xs"
                    >
                      {joiningWaitlist ? <><Loader2 size={14} className="animate-spin" /> Joining…</> : 'Join Waitlist'}
                    </button>
                  </div>
                )}
              </div>
            )}
              </div>
            </div>

            {/* Right Pane - Sticky Summary Tracker */}
            <div className="lg:col-span-1 hidden lg:block">
              <div className="sticky top-24 card p-6 shadow-[0_8px_30px_rgb(0,0,0,0.06)] border-brand-100/50 bg-gradient-to-b from-white to-slate-50/50">
                <h3 className="font-bold text-slate-900 mb-4 pb-4 border-b border-slate-100 leading-none">Booking Summary</h3>
                <div className="flex flex-col gap-4">
                  
                  {/* Provider Info Bubble */}
                  <div className="flex items-center gap-3 bg-white p-3 rounded-xl border border-slate-100 shadow-sm hover:shadow-md transition-all">
                    <div className="h-10 w-10 rounded-lg bg-brand-100 text-brand-700 font-bold flex items-center justify-center overflow-hidden flex-shrink-0">
                      {provider.avatarUrl ? <img src={provider.avatarUrl} alt={provider.displayName} className="h-full w-full object-cover" /> : provider.displayName[0]}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="font-semibold text-slate-900 text-sm truncate">{provider.displayName}</p>
                      <p className="text-xs text-slate-500 truncate">{provider.specialty}</p>
                    </div>
                  </div>

                    <div className="flex items-center gap-2 flex-wrap">
                      <span className={cn('badge text-xs', provider.acceptingBookings ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-500')}>
                        {provider.acceptingBookings ? 'Accepting bookings' : 'Bookings closed'}
                      </span>
                        <span className={cn('badge text-xs', meetingMode === 'ONLINE' ? 'bg-brand-100 text-brand-700' : 'bg-slate-100 text-slate-600')}>
                          {meetingMode}
                      </span>
                    </div>

                  <div className="space-y-4 text-sm px-1 py-2">
                    <div className="flex justify-between items-start gap-4">
                      <span className="text-slate-500 flex-shrink-0">Service</span>
                      <span className="font-semibold text-slate-900 text-right break-words tracking-tight">{selectedService?.name || <span className="text-slate-300 italic font-normal">Pending</span>}</span>
                    </div>
                    <div className="flex justify-between items-start gap-4">
                      <span className="text-slate-500 flex-shrink-0">Date</span>
                      <span className="font-semibold text-slate-900 text-right">{selectedDate ? format(selectedDate, 'MMM d, yyyy') : <span className="text-slate-300 italic font-normal">Pending</span>}</span>
                    </div>
                    <div className="flex justify-between items-start gap-4">
                      <span className="text-slate-500 flex-shrink-0">Time</span>
                      <span className="font-semibold text-slate-900 text-right">{selectedSlot ? `${formatTimeOnly(selectedSlot.startTime)}` : <span className="text-slate-300 italic font-normal">Pending</span>}</span>
                    </div>
                  </div>
                  
                  <div className="pt-4 border-t border-slate-100 flex justify-between items-center mt-2">
                    <span className="text-slate-500 font-medium">Total Cost</span>
                    <span className="font-bold text-xl text-brand-700">{selectedService?.price != null ? formatCurrency(selectedService.price) : '—'}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <LocationMapModal
          open={mapOpen}
          onClose={() => setMapOpen(false)}
          provider={provider?.latitude != null && provider?.longitude != null ? { lat: Number(provider.latitude), lng: Number(provider.longitude) } : null}
          client={userLocation}
          title={`${provider?.displayName || 'Provider'} location`}
          subtitle="This map stays inside the app instead of opening a new tab."
        />
      </main>
    </>
  )
}

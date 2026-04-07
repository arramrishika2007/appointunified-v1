'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Briefcase, Check, ChevronRight, Loader2, MapPin, Activity, LogOut } from 'lucide-react'
import { professionalsApi } from '@/lib/api'
import { geocodeAddress } from '@/lib/geoapify'
import { LocationAutocomplete, LocationData } from '@/components/ui/LocationAutocomplete'
import { useAuthStore } from '@/lib/store'
import { cn } from '@/lib/utils'
import toast from 'react-hot-toast'

const SECTORS = [
  { value: 'HEALTHCARE', label: 'Healthcare', desc: 'Doctor, Specialist, Diagnostic Lab' },
  { value: 'GOVERNMENT', label: 'Government', desc: 'Officer, Department, Public Service' },
  { value: 'SERVICES',   label: 'Services',   desc: 'Technician, Salon, Repair Expert'  },
]

const SPECIALTIES: Record<string, string[]> = {
  HEALTHCARE: [
    'General Physician', 'Cardiologist', 'Dermatologist', 'Pediatrician',
    'Orthopedist', 'Neurologist', 'Psychiatrist', 'Dentist',
    'Gynecologist', 'ENT Specialist', 'Ophthalmologist', 'Diagnostic Lab'
  ],
  GOVERNMENT: [
    'Public Notary', 'Registration Officer', 'Tax Consultant',
    'Municipal Clerk', 'Passport Officer', 'RTO Agent',
    'Legal Advisor', 'Civil Department', 'Public Service'
  ],
  SERVICES: [
    'Electrician', 'Plumber', 'Carpenter', 'Pest Control',
    'Home Cleaning', 'Salon Therapist', 'Makeup Artist',
    'Mechanic', 'Appliance Repair', 'Tutor', 'Lawyer'
  ]
}

const DAYS = ['Mon','Tue','Wed','Thu','Fri','Sat','Sun']

const schema = z.object({
  displayName:     z.string().min(2),
  sector:          z.string().min(1, 'Please select a sector'),
  specialty:       z.string().optional(),
  licenseNumber:   z.string().optional(),
  bio:             z.string().max(500).optional(),
  qualification:   z.string().optional(),
  yearsExperience: z.coerce.number().min(0).max(60).optional(),
  consultationFee: z.coerce.number().min(0).optional(),
  city:            z.string().optional(),
  address:         z.string().optional(),
})
type FormData = z.infer<typeof schema>

interface DaySchedule {
  weekday: number
  enabled: boolean
  startTime: string
  endTime: string
  slotDurationMins: number
  bufferMinutes: number
}

const defaultSchedule = (): DaySchedule[] =>
  DAYS.map((_, i) => ({
    weekday: i,
    enabled: i < 5, // Mon–Fri on by default
    startTime: '09:00',
    endTime: '17:00',
    slotDurationMins: 30,
    bufferMinutes: 5,
  }))

export default function ProfessionalOnboardingPage() {
  const router = useRouter()
  const [step, setStep] = useState(1)
  const [schedule, setSchedule] = useState<DaySchedule[]>(defaultSchedule())
  const [submitting, setSubmitting] = useState(false)
  const [locationState, setLocationState] = useState<LocationData | null>(null)
  
  const { logout } = useAuthStore()

  const { register, handleSubmit, watch, setValue, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
  })

  const selectedSector = watch('sector')
  const specialtyValue = watch('specialty') || ''
  const [showSpecialties, setShowSpecialties] = useState(false)

  const filteredSpecialties = selectedSector && SPECIALTIES[selectedSector] 
    ? SPECIALTIES[selectedSector].filter(s => s.toLowerCase().includes(specialtyValue.toLowerCase())) 
    : []

  const toggleDay = (i: number) => {
    setSchedule(prev => prev.map((d, idx) => idx === i ? { ...d, enabled: !d.enabled } : d))
  }
  const updateDay = (i: number, field: keyof DaySchedule, value: string | number) => {
    setSchedule(prev => prev.map((d, idx) => idx === i ? { ...d, [field]: value } : d))
  }

  const onSubmit = async (data: FormData) => {
    setSubmitting(true)
    try {
      let lat = locationState?.lat
      let lon = locationState?.lon

      // Fallback to geoapify if autocomplete wasn't used or selected
      if (!lat || !lon) {
        const addressQuery = [data.address, data.city].filter(Boolean).join(', ')
        const geocode = addressQuery ? await geocodeAddress(addressQuery) : null
        lat = geocode?.latitude
        lon = geocode?.longitude
      }

      const weeklySchedule = schedule
        .filter(d => d.enabled)
        .map(d => ({
          weekday: d.weekday,
          startTime: d.startTime,
          endTime: d.endTime,
          slotDurationMins: d.slotDurationMins,
          bufferMinutes: d.bufferMinutes,
        }))

      await professionalsApi.register({
        ...data,
        latitude: lat,
        longitude: lon,
        weeklySchedule,
      })
      toast.success('Profile submitted! Please complete document verification.')
      router.push('/professional/verification')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
        || 'Submission failed. Please try again.'
      toast.error(msg)
    } finally {
      setSubmitting(false)
    }
  }

  const STEPS = ['Sector', 'Profile', 'Schedule']

  return (
    <div className="min-h-screen bg-primary relative overflow-hidden py-12">
      {/* Absolute Logout Button */}
      <button 
        onClick={() => {
          logout()
          window.location.href = '/'
        }}
        className="absolute top-6 right-6 sm:top-8 sm:right-8 z-50 flex items-center gap-2 px-4 py-2 bg-white/40 hover:bg-white/90 backdrop-blur-xl border border-white/50 rounded-full text-sm font-extrabold text-text-secondary hover:text-text-primary transition-all duration-300 shadow-sm hover:shadow-float"
      >
        <LogOut size={16} className="text-accent-warm" /> Switch Account
      </button>

      {/* Ambient Orbs - Multi-Pastel Theme */}
      <div className="absolute top-[-5%] right-[-5%] w-[600px] h-[600px] rounded-full bg-pastel-purple/30 blur-[130px] pointer-events-none animate-float-slow" />
      <div className="absolute bottom-[-10%] left-[-10%] w-[500px] h-[500px] rounded-full bg-pastel-mint/30 blur-[120px] pointer-events-none" />

      <div className="container-page max-w-2xl relative z-10">

        {/* Header */}
        <div className="text-center mb-10">
          <div className="inline-flex items-center gap-3 mb-6">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-accent to-accent-light text-white shadow-soft">
              <Activity size={24} strokeWidth={2.5} />
            </div>
          </div>
          <h1 className="text-3xl font-extrabold text-text-primary tracking-tight">Professional Onboarding</h1>
          <p className="text-text-secondary text-sm font-medium mt-2">Let's get your profile set up. Verification takes 24–48 hours.</p>
        </div>

        {/* Progress Tracker */}
        <div className="flex items-center gap-2 mb-10">
          {STEPS.map((label, i) => {
            const num = i + 1
            const done = num < step
            const active = num === step
            return (
              <div key={label} className="flex items-center gap-2 flex-1">
                <div className={cn(
                  'flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-full text-xs font-bold transition-all duration-300 shadow-sm',
                  done   ? 'bg-accent-mint text-white' :
                  active ? 'bg-accent text-white shadow-soft shadow-accent/30' :
                           'bg-elevated border border-border text-text-muted'
                )}>
                  {done ? <Check size={14} strokeWidth={3} /> : num}
                </div>
                <span className={cn('text-xs font-bold hidden sm:block',
                  active ? 'text-accent' : done ? 'text-accent-mint' : 'text-text-muted'
                )}>{label}</span>
                {i < STEPS.length - 1 && <div className={cn('h-1 flex-1 rounded-full transition-colors', done ? 'bg-accent-mint' : 'bg-border')} />}
              </div>
            )
          })}
        </div>

        <div className="card p-8 bg-white/70 backdrop-blur-xl border border-white">

          {/* STEP 1 — Sector */}
          {step === 1 && (
            <div className="animate-fade-in">
              <h2 className="text-2xl font-extrabold text-text-primary tracking-tight mb-2">Which sector are you in?</h2>
              <p className="text-text-secondary text-sm font-medium mb-8">This determines your verification requirements.</p>

              <div className="space-y-4 mb-8">
                {SECTORS.map((sec) => (
                  <button
                    key={sec.value}
                    type="button"
                    onClick={() => setValue('sector', sec.value)}
                    className={cn(
                      'w-full rounded-2xl border-2 p-5 text-left transition-all duration-300',
                      selectedSector === sec.value
                        ? 'border-text-primary bg-text-primary text-white shadow-float'
                        : 'border-border bg-white hover:border-pastel-purple hover:shadow-soft'
                    )}
                  >
                    <div className={cn("text-sm font-extrabold", selectedSector === sec.value ? "text-white" : "text-text-primary")}>{sec.label}</div>
                    <div className={cn("text-xs font-medium mt-1", selectedSector === sec.value ? "text-slate-300" : "text-text-secondary")}>{sec.desc}</div>
                  </button>
                ))}
              </div>
              {errors.sector && <p className="error-text mb-4 text-center">{errors.sector.message}</p>}

              <button
                onClick={() => { if (!selectedSector) { toast.error('Please select a sector'); return } setStep(2) }}
                className="btn-primary w-full py-4 text-base"
              >
                Continue Setup <ChevronRight size={18} strokeWidth={2.5} />
              </button>
            </div>
          )}

          {/* STEP 2 — Profile info */}
          {step === 2 && (
            <form onSubmit={(e) => { e.preventDefault(); setStep(3) }} className="animate-fade-in">
              <div className="flex items-center justify-between mb-8 pb-4 border-b border-border">
                <h2 className="text-2xl font-extrabold text-text-primary tracking-tight">Your Profile</h2>
                <button type="button" onClick={() => setStep(1)} className="btn-ghost text-xs px-4 py-2 font-bold">Back</button>
              </div>

              <div className="space-y-6">
                <div>
                  <label className="label">Display name <span className="text-accent-warm">*</span></label>
                  <input {...register('displayName')} className={cn('input', errors.displayName && 'input-error')} placeholder="Dr. Priya Sharma" />
                  {errors.displayName && <p className="error-text">{errors.displayName.message}</p>}
                </div>

                <div className="grid grid-cols-2 gap-5">
                  <div>
                    <label className="label">Specialty / Role</label>
                    <div className="relative">
                      <input 
                        {...register('specialty')} 
                        className="input relative z-10 bg-transparent" 
                        placeholder="e.g. Cardiologist" 
                        onFocus={() => setShowSpecialties(true)}
                        onBlur={() => setTimeout(() => setShowSpecialties(false), 200)}
                      />
                      {showSpecialties && filteredSpecialties.length > 0 && (
                        <div className="absolute z-50 w-full mt-1 bg-white rounded-xl shadow-lg border border-slate-100 max-h-48 overflow-y-auto overflow-hidden animate-fade-in">
                           {filteredSpecialties.map((sp) => (
                             <div 
                               key={sp} 
                               className="px-4 py-2 hover:bg-slate-50 cursor-pointer text-sm font-medium text-text-primary transition-colors" 
                               onMouseDown={(e) => {
                                 e.preventDefault()
                                 setValue('specialty', sp)
                                 setShowSpecialties(false)
                               }}
                             >
                               {sp}
                             </div>
                           ))}
                        </div>
                      )}
                    </div>
                  </div>
                  <div>
                    <label className="label">License / Reg. No.</label>
                    <input {...register('licenseNumber')} className="input" placeholder="NMC-123456" />
                  </div>
                </div>

                <div>
                  <label className="label">Bio <span className="text-text-muted font-normal ml-1">(optional)</span></label>
                  <textarea {...register('bio')} rows={3} className="input resize-none" placeholder="Brief about yourself…" />
                </div>

                <div className="grid grid-cols-2 gap-5">
                  <div>
                    <label className="label">Years of experience</label>
                    <input {...register('yearsExperience')} type="number" className="input" placeholder="10" />
                  </div>
                  <div>
                    <label className="label">Consultation fee (₹)</label>
                    <input {...register('consultationFee')} type="number" className="input" placeholder="500" />
                  </div>
                </div>

                <div className="sm:col-span-2">
                  <label className="label flex items-center gap-1"><MapPin size={14} className="text-accent" /> Location (City & Address)</label>
                  <LocationAutocomplete 
                    onLocationSelect={(loc) => {
                      if (loc) {
                        setValue('city', loc.city || '')
                        setValue('address', loc.address)
                        setLocationState(loc)
                      } else {
                        setValue('city', '')
                        setValue('address', '')
                        setLocationState(null)
                      }
                    }}
                    placeholder="Search for your clinic, office, or area..."
                  />
                  {locationState && (
                    <p className="text-xs text-accent-mint mt-2 font-medium flex items-center gap-1">
                      <Check size={12} /> Exact geographic coordinates mapped
                    </p>
                  )}
                  {/* Hidden inputs to maintain form state */}
                  <input type="hidden" {...register('city')} />
                  <input type="hidden" {...register('address')} />
                </div>
              </div>

              <button type="submit" className="btn-primary w-full mt-10 py-4 text-base">
                Proceed to Schedule <ChevronRight size={18} strokeWidth={2.5} />
              </button>
            </form>
          )}

          {/* STEP 3 — Weekly schedule + submit */}
          {step === 3 && (
            <form onSubmit={handleSubmit(onSubmit)} className="animate-fade-in">
              <div className="flex items-center justify-between mb-6 pb-4 border-b border-border">
                <h2 className="text-2xl font-extrabold text-text-primary tracking-tight">Weekly Schedule</h2>
                <button type="button" onClick={() => setStep(2)} className="btn-ghost text-xs px-4 py-2 font-bold">Back</button>
              </div>
              <p className="text-sm font-medium text-text-secondary mb-8">Toggle days on/off and set your working hours. You can adjust this later.</p>

              <div className="space-y-4">
                {schedule.map((day, i) => (
                  <div key={i} className={cn(
                    'rounded-2xl border-2 p-5 transition-all duration-300',
                    day.enabled ? 'border-accent/30 bg-accent/5' : 'border-border bg-subtle/50 opacity-80'
                  )}>
                    <div className="flex items-center gap-5 mb-4">
                      <button
                        type="button"
                        onClick={() => toggleDay(i)}
                        className={cn('ios-toggle', day.enabled ? 'enabled' : 'disabled')}
                      >
                        <span className="ios-toggle-thumb" />
                      </button>
                      <span className={cn('text-sm font-extrabold w-10 uppercase tracking-widest', day.enabled ? 'text-text-primary' : 'text-text-muted')}>
                        {DAYS[i]}
                      </span>
                      {day.enabled && (
                        <div className="flex items-center gap-3 flex-1 flex-wrap">
                          <input type="time" title={`${DAYS[i]} start time`} value={day.startTime} onChange={e => updateDay(i, 'startTime', e.target.value)}
                            className="input py-2 px-3 text-xs w-32 font-medium" />
                          <span className="text-text-muted text-xs font-bold">to</span>
                          <input type="time" title={`${DAYS[i]} end time`} value={day.endTime} onChange={e => updateDay(i, 'endTime', e.target.value)}
                            className="input py-2 px-3 text-xs w-32 font-medium" />
                        </div>
                      )}
                    </div>

                    {day.enabled && (
                      <div className="flex items-center gap-6 ml-[4.5rem] mt-2 text-xs font-medium text-text-secondary">
                        <label className="flex items-center gap-2">
                          Slot:
                          <select value={day.slotDurationMins} onChange={e => updateDay(i, 'slotDurationMins', Number(e.target.value))}
                            className="input py-1 px-3 text-xs w-24 cursor-pointer">
                            {[15,20,30,45,60].map(v => <option key={v} value={v}>{v}m</option>)}
                          </select>
                        </label>
                        <label className="flex items-center gap-2">
                          Buffer:
                          <select value={day.bufferMinutes} onChange={e => updateDay(i, 'bufferMinutes', Number(e.target.value))}
                            className="input py-1 px-3 text-xs w-24 cursor-pointer">
                            {[0,5,10,15,20].map(v => <option key={v} value={v}>{v}m</option>)}
                          </select>
                        </label>
                      </div>
                    )}
                  </div>
                ))}
              </div>

              <div className="mt-8 rounded-2xl bg-pastel-yellow/30 border border-pastel-yellow p-5 text-sm">
                <strong className="text-text-primary font-extrabold">Notice: Next steps after submission</strong>
                <ul className="mt-2 space-y-1.5 text-xs text-text-secondary list-none font-medium">
                  <li className="flex gap-2"><span>1.</span> Our team will comprehensively verify your credentials within 24–48 hours.</li>
                  <li className="flex gap-2"><span>2.</span> You will receive an automated approval email from the grid.</li>
                  <li className="flex gap-2"><span>3.</span> You may then provision services and activate live booking flows.</li>
                </ul>
              </div>

              <button type="submit" disabled={submitting} className="btn-primary w-full mt-8 py-4 text-base">
                {submitting
                  ? <><Loader2 size={20} className="animate-spin" /> Submitting for Review…</>
                  : <><Briefcase size={20} /> Transmit Profile Verification</>
                }
              </button>
            </form>
          )}
        </div>
      </div>
    </div>
  )
}

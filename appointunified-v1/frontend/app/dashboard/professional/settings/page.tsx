'use client'

import { FormEvent, useEffect, useMemo, useState } from 'react'
import { useRouter } from 'next/navigation'
import Link from 'next/link'
import { AlertCircle, Check, Loader2, MapPin, Save, Settings2, Sparkles } from 'lucide-react'
import { ProfessionalShell } from '@/components/layout/ProfessionalShell'
import { professionalsApi } from '@/lib/api'
import { useAuthStore } from '@/lib/store'
import { AvailabilityMood, ProfessionalDetail } from '@/types'
import { MOOD_CONFIG } from '@/lib/utils'
import toast from 'react-hot-toast'

type Tab = 'profile' | 'mood' | 'operations' | 'area'

const DEFAULT_MOOD: AvailabilityMood = 'AVAILABLE'

export default function ProfessionalSettingsPage() {
  const router = useRouter()
  const { isAuthenticated, hasHydrated, user } = useAuthStore()

  const [tab, setTab] = useState<Tab>('profile')
  const [loading, setLoading] = useState(true)
  const [savingProfile, setSavingProfile] = useState(false)
  const [savingMood, setSavingMood] = useState(false)
  const [savingOperations, setSavingOperations] = useState(false)
  const [savingArea, setSavingArea] = useState(false)
  const [profile, setProfile] = useState<ProfessionalDetail | null>(null)

  const [displayName, setDisplayName] = useState('')
  const [bio, setBio] = useState('')
  const [qualification, setQualification] = useState('')
  const [yearsExperience, setYearsExperience] = useState('')
  const [consultationFee, setConsultationFee] = useState('')
  const [city, setCity] = useState('')
  const [address, setAddress] = useState('')
  const [latitude, setLatitude] = useState('')
  const [longitude, setLongitude] = useState('')
  const [upiId, setUpiId] = useState('')
  const [acceptingBookings, setAcceptingBookings] = useState(true)
  const [allowOverbooking, setAllowOverbooking] = useState(false)
  const [serviceRadiusKm, setServiceRadiusKm] = useState('10')
  const [centerLat, setCenterLat] = useState('')
  const [centerLng, setCenterLng] = useState('')
  const [mood, setMood] = useState<AvailabilityMood>(DEFAULT_MOOD)
  const [moodNote, setMoodNote] = useState('')

  useEffect(() => {
    if (!hasHydrated) return
    if (!isAuthenticated) {
      router.push('/auth/login?redirect=/dashboard/professional/settings')
      return
    }
    if (user?.role !== 'PROFESSIONAL') {
      router.push('/professional/dashboard')
      return
    }

    professionalsApi.getMyProfile()
      .then((response) => {
        const data = response.data.data as ProfessionalDetail
        setProfile(data)
        setDisplayName(data.displayName ?? '')
        setBio(data.bio ?? '')
        setQualification(data.qualification ?? '')
        setYearsExperience(data.yearsExperience != null ? String(data.yearsExperience) : '')
        setConsultationFee(data.consultationFee != null ? String(data.consultationFee) : '')
        setCity(data.city ?? '')
        setAddress(data.address ?? '')
        setLatitude(data.latitude != null ? String(data.latitude) : '')
        setLongitude(data.longitude != null ? String(data.longitude) : '')
        setUpiId(data.upiId ?? '')
        setAcceptingBookings(data.acceptingBookings)
        setAllowOverbooking(Boolean(data.allowOverbooking))
        setServiceRadiusKm(data.serviceAreaRadiusKm != null ? String(data.serviceAreaRadiusKm) : '10')
        setCenterLat(data.latitude != null ? String(data.latitude) : '')
        setCenterLng(data.longitude != null ? String(data.longitude) : '')
        setMood((data.availabilityMood as AvailabilityMood) || DEFAULT_MOOD)
        setMoodNote(data.moodNote ?? '')
      })
      .catch(() => {
        toast.error('Could not load professional profile')
      })
      .finally(() => setLoading(false))
  }, [hasHydrated, isAuthenticated, router, user?.role])

  const moodConfig = useMemo(() => MOOD_CONFIG[mood], [mood])

  const saveProfile = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setSavingProfile(true)
    try {
      await professionalsApi.updateProfile({
        displayName,
        bio,
        qualification,
        yearsExperience: yearsExperience ? Number(yearsExperience) : undefined,
        consultationFee: consultationFee ? Number(consultationFee) : undefined,
        city,
        address,
        latitude: latitude ? Number(latitude) : undefined,
        longitude: longitude ? Number(longitude) : undefined,
        upiId,
        acceptingBookings,
        serviceAreaRadiusKm: serviceRadiusKm ? Number(serviceRadiusKm) : undefined,
      })
      toast.success('Profile updated')
    } catch {
      toast.error('Could not update profile')
    } finally {
      setSavingProfile(false)
    }
  }

  const saveMood = async () => {
    setSavingMood(true)
    try {
      await professionalsApi.updateMood({ mood, note: moodNote || undefined })
      toast.success('Mood updated')
    } catch {
      toast.error('Could not update mood')
    } finally {
      setSavingMood(false)
    }
  }

  const saveOperations = async () => {
    setSavingOperations(true)
    try {
      await professionalsApi.updateOverbooking(allowOverbooking)
      toast.success('Queue settings updated')
    } catch {
      toast.error('Could not update queue settings')
    } finally {
      setSavingOperations(false)
    }
  }

  const saveServiceArea = async () => {
    setSavingArea(true)
    try {
      await professionalsApi.updateServiceArea(
        Number(serviceRadiusKm || 10),
        centerLat ? Number(centerLat) : undefined,
        centerLng ? Number(centerLng) : undefined,
      )
      toast.success('Service area updated')
    } catch {
      toast.error('Could not update service area')
    } finally {
      setSavingArea(false)
    }
  }

  if (!hasHydrated || loading) {
    return (
      <ProfessionalShell>
        <main className="min-h-screen bg-slate-50">
          <div className="container-page flex justify-center py-20">
            <Loader2 size={32} className="animate-spin text-brand-600" />
          </div>
        </main>
      </ProfessionalShell>
    )
  }

  return (
    <ProfessionalShell>
      <main className="min-h-screen bg-slate-50">
        <div className="container-page py-8 max-w-6xl space-y-6">
          <header className="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">Professional control center</p>
                <h1 className="mt-2 text-3xl font-bold text-slate-900">Professional Settings</h1>
                <p className="mt-1 text-sm text-slate-600">Edit your profile, live mood, queue behavior, and service area from one place.</p>
              </div>
              <Link href="/professional/dashboard" className="btn-ghost text-xs px-3 py-2">
                <Sparkles size={13} /> Open dashboard
              </Link>
            </div>
          </header>

          <div className="flex flex-wrap gap-2 rounded-2xl border border-slate-200 bg-white p-3 shadow-sm">
            {[
              { id: 'profile', label: 'Profile' },
              { id: 'mood', label: 'Mood' },
              { id: 'operations', label: 'Operations' },
              { id: 'area', label: 'Service Area' },
            ].map((item) => (
              <button
                key={item.id}
                onClick={() => setTab(item.id as Tab)}
                className={`rounded-lg px-3 py-2 text-sm font-medium transition ${
                  tab === item.id ? 'bg-slate-900 text-white' : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                }`}
              >
                {item.label}
              </button>
            ))}
          </div>

          <section className="grid gap-6 lg:grid-cols-[1fr_340px]">
            <div className="space-y-6">
              {tab === 'profile' && (
                <form onSubmit={saveProfile} className="card p-6 space-y-5">
                  <div className="flex items-center gap-2 text-sm font-semibold text-slate-900"><Settings2 size={16} className="text-brand-600" /> Public profile</div>
                  <div className="grid gap-4 md:grid-cols-2">
                    <Field label="Display name" value={displayName} onChange={setDisplayName} />
                    <Field label="City" value={city} onChange={setCity} />
                    <Field label="Qualification" value={qualification} onChange={setQualification} />
                    <Field label="Years of experience" value={yearsExperience} onChange={setYearsExperience} type="number" />
                    <Field label="Consultation fee" value={consultationFee} onChange={setConsultationFee} type="number" />
                    <Field label="UPI ID" value={upiId} onChange={setUpiId} />
                  </div>
                  <Field label="Address" value={address} onChange={setAddress} />
                  <TextArea label="Bio" value={bio} onChange={setBio} />
                  <div className="grid gap-4 md:grid-cols-3">
                    <Field label="Latitude" value={latitude} onChange={setLatitude} type="number" step="0.000001" />
                    <Field label="Longitude" value={longitude} onChange={setLongitude} type="number" step="0.000001" />
                    <label className="flex flex-col gap-1 text-sm text-slate-600">
                      <span className="label">Accepting bookings</span>
                      <select value={acceptingBookings ? 'yes' : 'no'} onChange={(event) => setAcceptingBookings(event.target.value === 'yes')} className="input">
                        <option value="yes">Yes</option>
                        <option value="no">No</option>
                      </select>
                    </label>
                  </div>

                  <div className="flex items-center gap-2">
                    <button type="submit" disabled={savingProfile} className="btn-primary text-xs px-3 py-2">
                      {savingProfile ? <Loader2 size={13} className="animate-spin" /> : <Save size={13} />} Save profile
                    </button>
                    <p className="text-xs text-slate-500">Changes are persisted through the existing professional profile endpoint.</p>
                  </div>
                </form>
              )}

              {tab === 'mood' && (
                <div className="card p-6 space-y-5">
                  <div>
                    <div className="flex items-center gap-2 text-sm font-semibold text-slate-900"><Sparkles size={16} className="text-brand-600" /> Live availability mood</div>
                    <p className="mt-1 text-sm text-slate-600">Update the status that clients see on your profile and booking flow.</p>
                  </div>

                  <div className="grid gap-3 md:grid-cols-2">
                    {(['AVAILABLE', 'BUSY', 'RUNNING_LATE', 'TAKING_BREAKS', 'DO_NOT_DISTURB'] as AvailabilityMood[]).map((item) => {
                      const cfg = MOOD_CONFIG[item]
                      return (
                        <button
                          key={item}
                          type="button"
                          onClick={() => setMood(item)}
                          className={`rounded-2xl border px-4 py-4 text-left transition ${mood === item ? 'border-slate-900 bg-slate-900 text-white' : 'border-slate-200 bg-white hover:bg-slate-50'}`}
                        >
                          <div className="flex items-center gap-2">
                            <span className={`h-2.5 w-2.5 rounded-full ${cfg.dot}`} />
                            <span className="font-semibold">{cfg.label}</span>
                          </div>
                        </button>
                      )
                    })}
                  </div>

                  <TextArea label="Mood note" value={moodNote} onChange={setMoodNote} placeholder="Optional note shown to clients" />

                  <div className="flex items-center gap-2">
                    <button type="button" onClick={saveMood} disabled={savingMood} className="btn-primary text-xs px-3 py-2">
                      {savingMood ? <Loader2 size={13} className="animate-spin" /> : <Check size={13} />} Save mood
                    </button>
                    <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-700">
                      {moodConfig.label}
                    </span>
                  </div>
                </div>
              )}

              {tab === 'operations' && (
                <div className="card p-6 space-y-5">
                  <div>
                    <div className="flex items-center gap-2 text-sm font-semibold text-slate-900"><AlertCircle size={16} className="text-brand-600" /> Queue and booking operations</div>
                    <p className="mt-1 text-sm text-slate-600">Control whether you accept bookings and whether smart overbooking is allowed.</p>
                  </div>

                  <div className="flex items-center justify-between rounded-2xl border border-slate-200 bg-slate-50 p-4">
                    <div>
                      <p className="font-semibold text-slate-900">Allow overbooking</p>
                      <p className="text-sm text-slate-500">Let the system hold back additional slots when demand is high.</p>
                    </div>
                    <button
                      type="button"
                      onClick={() => setAllowOverbooking((current) => !current)}
                      className={`rounded-full px-4 py-2 text-sm font-semibold ${allowOverbooking ? 'bg-emerald-600 text-white' : 'bg-slate-200 text-slate-700'}`}
                    >
                      {allowOverbooking ? 'Enabled' : 'Disabled'}
                    </button>
                  </div>

                  <div className="flex items-center gap-2">
                    <button type="button" onClick={saveOperations} disabled={savingOperations} className="btn-primary text-xs px-3 py-2">
                      {savingOperations ? <Loader2 size={13} className="animate-spin" /> : <Save size={13} />} Save operations
                    </button>
                  </div>
                </div>
              )}

              {tab === 'area' && (
                <div className="card p-6 space-y-5">
                  <div>
                    <div className="flex items-center gap-2 text-sm font-semibold text-slate-900"><MapPin size={16} className="text-brand-600" /> Service area</div>
                    <p className="mt-1 text-sm text-slate-600">Set the radius and center point used for offline booking coverage.</p>
                  </div>

                  <div className="grid gap-4 md:grid-cols-3">
                    <Field label="Radius (km)" value={serviceRadiusKm} onChange={setServiceRadiusKm} type="number" min="1" max="100" />
                    <Field label="Center latitude" value={centerLat} onChange={setCenterLat} type="number" step="0.000001" />
                    <Field label="Center longitude" value={centerLng} onChange={setCenterLng} type="number" step="0.000001" />
                  </div>

                  <div className="flex items-center gap-2">
                    <button type="button" onClick={saveServiceArea} disabled={savingArea} className="btn-primary text-xs px-3 py-2">
                      {savingArea ? <Loader2 size={13} className="animate-spin" /> : <Save size={13} />} Save service area
                    </button>
                  </div>
                </div>
              )}
            </div>

            <aside className="space-y-6">
              <div className="card p-6">
                <h2 className="text-lg font-semibold text-slate-900">Live summary</h2>
                <div className="mt-4 space-y-3 text-sm text-slate-600">
                  <SummaryRow label="Profile" value={displayName || 'Unnamed profile'} />
                  <SummaryRow label="Mood" value={moodConfig.label} />
                  <SummaryRow label="Bookings" value={acceptingBookings ? 'Open' : 'Closed'} />
                  <SummaryRow label="Overbooking" value={allowOverbooking ? 'Enabled' : 'Disabled'} />
                  <SummaryRow label="Radius" value={`${serviceRadiusKm || '10'} km`} />
                </div>
              </div>

              <div className="card p-6">
                <h2 className="text-lg font-semibold text-slate-900">Profile preview</h2>
                <p className="mt-2 text-sm text-slate-600">These are the values currently bound to your professional profile endpoint.</p>
                <div className="mt-4 rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-700">
                  <p><span className="font-medium text-slate-900">Qualification:</span> {qualification || 'Not set'}</p>
                  <p className="mt-2"><span className="font-medium text-slate-900">UPI:</span> {upiId || 'Not set'}</p>
                  <p className="mt-2"><span className="font-medium text-slate-900">Location:</span> {city || 'Not set'}</p>
                </div>
              </div>
            </aside>
          </section>
        </div>
      </main>
    </ProfessionalShell>
  )
}

function Field({ label, value, onChange, type = 'text', step, min, max }: { label: string; value: string; onChange: (value: string) => void; type?: string; step?: string; min?: string; max?: string }) {
  return (
    <label className="flex flex-col gap-1 text-sm text-slate-600">
      <span className="label">{label}</span>
      <input value={value} onChange={(event) => onChange(event.target.value)} type={type} step={step} min={min} max={max} className="input" />
    </label>
  )
}

function TextArea({ label, value, onChange, placeholder }: { label: string; value: string; onChange: (value: string) => void; placeholder?: string }) {
  return (
    <label className="flex flex-col gap-1 text-sm text-slate-600">
      <span className="label">{label}</span>
      <textarea value={value} onChange={(event) => onChange(event.target.value)} placeholder={placeholder} rows={4} className="input resize-none" />
    </label>
  )
}

function SummaryRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-4">
      <span>{label}</span>
      <span className="font-semibold text-slate-900">{value}</span>
    </div>
  )
}

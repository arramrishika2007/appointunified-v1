"use client"

import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useState, useEffect } from 'react'
import { ShieldCheck, Activity, BrainCircuit } from 'lucide-react'
import { Navbar } from '@/components/layout/Navbar'
import { HeroParallax } from '@/components/ui/hero-parallax'
import { GlobeFeatureSection } from '@/components/ui/globe-feature-section'
import { Footer } from '@/components/ui/modem-animated-footer'
import { Lock, Twitter, Linkedin, Github, Mail, CalendarCheck, CheckCircle2 } from 'lucide-react'

// Images focused entirely on the perspective of "Appointment Booking" (consultations, forms, clipboards, waiting rooms) across the 3 sectors
const parallaxProducts = [
  // HEALTHCARE - Booking & Consultations
  {
    title: "Patient Consultation",
    link: "/explore/healthcare",
    thumbnail: "https://images.unsplash.com/photo-1551076805-e18690c5e561?q=80&w=2000&auto=format&fit=crop", // Doctor handshake consultation
  },
  {
    title: "Medical Records",
    link: "/explore/healthcare",
    thumbnail: "https://images.unsplash.com/photo-1579684385127-1ef15d508118?q=80&w=2000&auto=format&fit=crop", // Nurse reading tablet/forms
  },
  {
    title: "Clinical Desk",
    link: "/explore/healthcare",
    thumbnail: "https://images.unsplash.com/photo-1505751172876-fa1923c5c528?q=80&w=2000&auto=format&fit=crop", // Medical desk waiting area
  },
  {
    title: "Intake Forms",
    link: "/explore/healthcare",
    thumbnail: "https://images.unsplash.com/photo-1638202993928-7267aad84c31?q=80&w=2000&auto=format&fit=crop", // Typing/writing on clipboard
  },
  {
    title: "Specialist Appointment",
    link: "/explore/healthcare",
    thumbnail: "https://images.unsplash.com/photo-1622253692010-333f2da6031d?q=80&w=2000&auto=format&fit=crop", // Doctor reviewing booking
  },

  // GOVERNMENT - Booking & Verification
  {
    title: "Official Applications",
    link: "/explore/government",
    thumbnail: "https://images.unsplash.com/photo-1554224155-8d04cb21cd6c?q=80&w=2000&auto=format&fit=crop", // Signing official document
  },
  {
    title: "Clerk Appointment",
    link: "/explore/government",
    thumbnail: "https://images.unsplash.com/photo-1450101499163-c8848c66ca85?q=80&w=2000&auto=format&fit=crop", // Contract/Legal desk
  },
  {
    title: "Identity Verification",
    link: "/explore/government",
    thumbnail: "https://images.unsplash.com/photo-1589829085413-56de8ae18c73?q=80&w=2000&auto=format&fit=crop", // Formal seal/documents
  },
  {
    title: "City Hall Waiting",
    link: "/explore/government",
    thumbnail: "https://images.unsplash.com/photo-1497366216548-37526070297c?q=80&w=2000&auto=format&fit=crop", // Public waiting room
  },
  {
    title: "Permit Consultations",
    link: "/explore/government",
    thumbnail: "https://images.unsplash.com/photo-1560250097-0b93528c311a?q=80&w=2000&auto=format&fit=crop", // Handshake official desk
  },

  // SERVICES - Quotes & Booking
  {
    title: "Mechanic Estimates",
    link: "/explore/services",
    thumbnail: "https://images.unsplash.com/photo-1619642751034-765dfdf7c58e?q=80&w=2000&auto=format&fit=crop", // Mechanic clipboard talking to user
  },
  {
    title: "Technician Scheduling",
    link: "/explore/services",
    thumbnail: "https://images.unsplash.com/photo-1581091226825-a6a2a5aee158?q=80&w=2000&auto=format&fit=crop", // Tech checking phone/schedule
  },
  {
    title: "Home Service Quotes",
    link: "/explore/services",
    thumbnail: "https://images.unsplash.com/photo-1581578731548-c64695cc6952?q=80&w=2000&auto=format&fit=crop", // Service writing on clipboard
  },
  {
    title: "Contractor Planning",
    link: "/explore/services",
    thumbnail: "https://images.unsplash.com/photo-1504328345606-18bbc8c9d7d1?q=80&w=2000&auto=format&fit=crop", // Construction blueprint
  },
  {
    title: "Service Confirmation",
    link: "/explore/services",
    thumbnail: "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?q=80&w=2000&auto=format&fit=crop", // Consultation handshake
  },
];


export default function HomePage() {
  const router = useRouter()
  const [gateRole, setGateRole] = useState<'ADMIN' | 'SUPER_ADMIN' | null>(null)
  const [accessCode, setAccessCode] = useState('')
  const [gateError, setGateError] = useState('')
  const [stats, setStats] = useState<any>(null)

  useEffect(() => {
    fetch('http://localhost:8080/public/stats')
      .then(res => res.json())
      .then(data => {
        if (data.success) {
          setStats(data.data)
        }
      })
      .catch(err => console.error("Failed to fetch landing stats", err))
  }, [])

  const openGate = (role: 'ADMIN' | 'SUPER_ADMIN') => {
    setGateRole(role)
    setAccessCode('')
    setGateError('')
  }

  const closeGate = () => {
    setGateRole(null)
    setAccessCode('')
    setGateError('')
  }

  const submitGate = () => {
    if (accessCode !== '7673923505') {
      setGateError('Invalid access code')
      return
    }

    const roleParam = gateRole === 'SUPER_ADMIN' ? 'SUPER_ADMIN' : 'ADMIN'
    closeGate()
    router.push(`/auth/login?role=${roleParam}`)
  }

  const socialLinks = [
    {
      icon: <Twitter className="w-6 h-6" />,
      href: "https://twitter.com",
      label: "Twitter",
    },
    {
      icon: <Linkedin className="w-6 h-6" />,
      href: "https://linkedin.com",
      label: "LinkedIn",
    },
    {
      icon: <Github className="w-6 h-6" />,
      href: "https://github.com",
      label: "GitHub",
    },
    {
      icon: <Mail className="w-6 h-6" />,
      href: "mailto:contact@appointunified.com",
      label: "Email",
    },
  ];

  const navLinks = [
    { label: "Healthcare", href: "/explore/healthcare" },
    { label: "Government", href: "/explore/government" },
    { label: "Services", href: "/explore/services" },
    { label: "Providers", href: "/auth/signup" },
  ];

  return (
    <>
      <Navbar />
      <main className="bg-primary min-h-screen font-sans selection:bg-accent-glow selection:text-text-primary overflow-x-hidden relative pb-1">

        {/* ─── Ambient Glow ──────────────────────────── */}
        <div className="absolute top-[-10%] left-[0%] w-[600px] h-[600px] rounded-full bg-accent-light/15 blur-[120px] pointer-events-none animate-float-slow" />
        <div className="absolute top-[10%] right-[-10%] w-[600px] h-[600px] rounded-full bg-accent/10 blur-[130px] pointer-events-none" />

        {/* ─── Hero Parallax ───────────────── */}
        <HeroParallax products={parallaxProducts} />

        {/* ─── Bento Box Feature Grid ───────────────── */}
        <section className="pt-20 pb-40 px-6 relative z-10 w-full">
          <div className="max-w-7xl mx-auto w-full">

            <div className="text-center mb-16 animate-slide-up">
              <h2 className="text-3xl md:text-5xl font-extrabold tracking-tight text-text-primary mb-5">
                Not a calendar. <span className="text-accent">An engine.</span>
              </h2>
              <p className="text-lg text-text-secondary font-medium max-w-2xl mx-auto">
                Appointments shouldn&apos;t be static text on a screen. Here&apos;s how our architecture actively defends your time.
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-6 auto-rows-[340px]">

              {/* Bento 1: Adaptive Flow (Spans 2 columns on desktop) */}
              <div className="glass-card md:col-span-2 p-8 relative overflow-hidden group hover:-translate-y-2 transition-all duration-300">
                <div className="absolute top-0 right-[-100px] w-[400px] h-[400px] bg-accent-glow/60 rounded-full blur-3xl group-hover:bg-accent-glow transition-colors pointer-events-none" />
                <div className="relative z-10 h-full flex flex-col">
                  <div className="w-14 h-14 bg-white rounded-2xl flex items-center justify-center shadow-sm mb-6 border border-border">
                    <Activity size={28} className="text-accent" />
                  </div>
                  <h3 className="text-2xl font-bold text-text-primary flex-none mb-3">Adaptive Timing</h3>
                  <p className="text-base text-text-secondary font-medium max-w-sm flex-none">
                    When the clinic runs late, your phone pings. We constantly recalculate ETAs so you never sit in a crowded waiting room again.
                  </p>

                  {/* Visual mockup inside bento */}
                  <div className="mt-8 bg-white/70 backdrop-blur-md p-5 rounded-2xl border border-white flex items-center justify-between shadow-soft max-w-md">
                    <div className="flex flex-col gap-1">
                      <span className="text-xs font-bold text-text-muted line-through">10:00 AM Slot</span>
                      <span className="text-sm font-extrabold text-accent flex items-center gap-2">
                        Moved to 10:14 AM <span className="relative flex h-2 w-2"><span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-accent opacity-75"></span><span className="relative inline-flex rounded-full h-2 w-2 bg-accent"></span></span>
                      </span>
                    </div>
                    <div className="bg-accent-warm/15 text-accent-warm px-4 py-1.5 rounded-full text-xs font-bold border border-accent-warm/20">
                      Delay Handled
                    </div>
                  </div>
                </div>
              </div>

              {/* Bento 2: Absolute Validity */}
              <div className="glass-card p-8 relative overflow-hidden bg-white/50 hover:-translate-y-2 transition-all duration-300">
                <div className="relative z-10 h-full flex flex-col items-start transition-transform duration-500">
                  <div className="w-14 h-14 bg-white rounded-2xl flex items-center justify-center shadow-sm mb-6 border border-border">
                    <ShieldCheck size={28} className="text-accent-mint" />
                  </div>
                  <h3 className="text-2xl font-bold text-text-primary mb-3">Cryptographic Trust</h3>
                  <p className="text-sm text-text-secondary font-medium">
                    Providers undergo a 4-tier verification protocol before listing. Fake slots are mathematically eliminated.
                  </p>
                  <div className="mt-auto pt-6 flex justify-center w-full">
                    <div className="w-24 h-24 border-[6px] border-accent-mint/20 rounded-full flex items-center justify-center relative">
                      <div className="w-[84px] h-[84px] border-[6px] border-accent-mint rounded-full border-t-transparent animate-spin" />
                      <CheckCircle2 className="absolute text-accent-mint" size={24} />
                    </div>
                  </div>
                </div>
              </div>

              {/* Bento 3: Zero Interfaces (Spans 1 col) */}
              <div className="glass-card p-8 relative overflow-hidden bg-white/50 hover:-translate-y-2 transition-all duration-300">
                <div className="relative z-10 h-full flex flex-col">
                  <div className="w-14 h-14 bg-white rounded-2xl flex items-center justify-center shadow-sm mb-6 border border-border">
                    <BrainCircuit size={28} className="text-accent-light" />
                  </div>
                  <h3 className="text-2xl font-bold text-text-primary mb-3">AI Prediction</h3>
                  <p className="text-sm text-text-secondary font-medium">
                    We predict the slots you want based on historical data and real-time traffic flows, saving you time scrolling.
                  </p>
                </div>
              </div>

              {/* Bento 4: Sectors (Spans 2 cols) */}
              <div className="glass-card md:col-span-2 p-8 relative overflow-hidden flex flex-col justify-between group hover:-translate-y-2 transition-all duration-300">
                <div className="absolute inset-0 bg-gradient-to-r from-accent to-accent-light opacity-[0.03] pointer-events-none" />

                <div className="relative z-10 h-full flex flex-col justify-between">
                  <div>
                    <h3 className="text-2xl font-bold text-text-primary mb-3">One abstract layer. Every Sector.</h3>
                    <p className="text-base text-text-secondary font-medium max-w-md mb-8">
                      Stop installing 5 different apps. AppointUnified connects Healthcare grids, Government endpoints, and Services into a single fluid interface.
                    </p>
                  </div>
                </div>
              </div>

            </div>
          </div>

          <div className="max-w-7xl mx-auto w-full px-0 sm:px-0 mt-8">
            <GlobeFeatureSection stats={stats} />
          </div>

        </section>

        {/* ─── Modem Animated Footer ─────────────────────────── */}
        <Footer
          brandName="AppointUnified"
          brandDescription="Your time, mathematically perfected. A unified engine across Healthcare, Government, and Services."
          socialLinks={socialLinks}
          navLinks={navLinks}
          brandIcon={<CalendarCheck className="w-8 sm:w-10 md:w-14 h-8 sm:h-10 md:h-14 text-white drop-shadow-md" />}
        />

        <div className="w-full flex justify-center text-[10px] sm:text-xs font-bold text-text-muted tracking-widest uppercase gap-2 sm:gap-4 pb-4">
          <button onClick={() => openGate('ADMIN')} className="hover:text-text-primary transition-colors border-r border-border pr-2 sm:pr-4">Admin Console</button>
          <button onClick={() => openGate('SUPER_ADMIN')} className="hover:text-red-400 text-text-muted transition-colors">Super Access</button>
        </div>

      </main>

      {gateRole && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-slate-950/50 backdrop-blur-sm px-4">
          <div className="w-full max-w-md rounded-3xl border border-white/70 bg-white p-6 shadow-2xl">
            <div className="flex items-start justify-between gap-4 mb-4">
              <div>
                <div className="inline-flex items-center gap-2 rounded-full bg-slate-100 px-3 py-1 text-[11px] font-bold uppercase tracking-widest text-slate-600 mb-3">
                  <Lock size={12} />
                  Admin Access Code
                </div>
                <h2 className="text-2xl font-extrabold tracking-tight text-text-primary">
                  {gateRole === 'SUPER_ADMIN' ? 'Super admin login' : 'Admin login'}
                </h2>
                <p className="mt-2 text-sm text-text-secondary">
                  Enter the 10-digit code to open the credential screen.
                </p>
              </div>
              <button onClick={closeGate} className="rounded-xl px-3 py-2 text-sm font-bold text-text-muted hover:text-text-primary hover:bg-slate-100 transition-colors">
                Close
              </button>
            </div>

            <label className="label">10-digit access code</label>
            <input
              type="password"
              value={accessCode}
              onChange={(e) => setAccessCode(e.target.value.replace(/\D/g, '').slice(0, 10))}
              inputMode="numeric"
              maxLength={10}
              placeholder="••••••••••"
              className="input mt-2 tracking-[0.35em] text-center text-lg font-bold"
            />
            {gateError && <p className="error-text mt-2">{gateError}</p>}

            <div className="mt-5 flex flex-col sm:flex-row gap-3">
              <button onClick={submitGate} className="btn-primary flex-1 py-3.5">
                Continue
              </button>
              <button onClick={closeGate} className="flex-1 rounded-2xl border border-border bg-white py-3.5 text-sm font-bold text-text-primary hover:border-accent transition-colors">
                Cancel
              </button>
            </div>

            <p className="mt-4 text-xs text-text-muted">
              Use the matching admin credentials after the code gate opens the login page.
            </p>
          </div>
        </div>
      )}
    </>
  )
}

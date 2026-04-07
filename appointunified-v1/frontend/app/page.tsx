"use client"

import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useState } from 'react'
import { ArrowRight, HeartPulse, Landmark, Settings2, ShieldCheck, Activity, BrainCircuit, CheckCircle2, Clock, Users, Shield, ShieldAlert, Lock } from 'lucide-react'
import { Navbar } from '@/components/layout/Navbar'

export default function HomePage() {
  const router = useRouter()
  const [gateRole, setGateRole] = useState<'ADMIN' | 'SUPER_ADMIN' | null>(null)
  const [accessCode, setAccessCode] = useState('')
  const [gateError, setGateError] = useState('')

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

  return (
    <>
      <Navbar />
      <main className="bg-primary min-h-screen font-sans selection:bg-accent-glow selection:text-text-primary overflow-hidden relative pb-1">

        {/* ─── Ambient Glow ──────────────────────────── */}
        <div className="absolute top-[-10%] left-[0%] w-[600px] h-[600px] rounded-full bg-accent-light/15 blur-[120px] pointer-events-none animate-float-slow" />
        <div className="absolute top-[10%] right-[-10%] w-[600px] h-[600px] rounded-full bg-accent/10 blur-[130px] pointer-events-none" />

        {/* ─── Hero with App Illusions ───────────────── */}
        <section className="pt-40 md:pt-48 pb-20 relative z-10">
          <div className="max-w-7xl mx-auto px-6 flex flex-col md:flex-row items-center gap-12">
            
            {/* Left: Text Content */}
            <div className="w-full md:w-1/2 flex flex-col items-start text-left animate-slide-up">
              <div className="mb-6 inline-flex items-center gap-2 bg-white/50 backdrop-blur-md border border-white/80 shadow-soft px-4 py-2 rounded-full">
                <span className="h-2 w-2 rounded-full bg-accent animate-pulse-soft" />
                <span className="text-[11px] font-bold tracking-widest uppercase text-accent">Adaptive Scheduling 2.0</span>
              </div>

              <h1 className="text-5xl md:text-7xl font-extrabold leading-[1.05] tracking-tight text-text-primary mb-6">
                Your time, <br />
                <span className="bg-clip-text text-transparent bg-gradient-to-r from-accent to-accent-light">mathematically perfected.</span>
              </h1>

              <p className="text-lg md:text-xl text-text-secondary font-medium leading-relaxed mb-10 max-w-lg">
                Stop waiting in line. Our unified platform calculates precise arrivals for healthcare, government, and services. 
              </p>

              <div className="flex flex-col sm:flex-row gap-4">
                <Link href="/explore/healthcare" className="btn-pastel-primary shadow-float">
                  <span>Start Booking</span>
                  <ArrowRight size={18} strokeWidth={2.5} />
                </Link>
              </div>
            </div>

            {/* Right: App Illusions (Curiosity Builders) */}
            <div className="w-full md:w-1/2 relative h-[400px] md:h-[500px] animate-fade-in [animation-delay:0.2s]">
              
              {/* Illusion 1: Live Queue Card */}
              <div className="absolute top-10 right-10 md:right-20 w-72 glass-card p-5 z-20 animate-float-slow">
                <div className="flex items-center justify-between mb-4">
                  <div className="flex items-center gap-2">
                    <div className="w-2 h-2 rounded-full bg-accent-warm animate-pulse" />
                    <span className="text-xs font-bold text-text-secondary uppercase tracking-wider">Live Status</span>
                  </div>
                  <span className="text-xs font-medium text-text-muted">Dr. Sharma</span>
                </div>
                <div className="flex items-end gap-3 mb-2">
                  <span className="text-4xl font-extrabold text-text-primary">12</span>
                  <span className="text-sm font-medium text-text-secondary mb-1">mins away</span>
                </div>
                <div className="w-full bg-border rounded-full h-1.5 overflow-hidden">
                  <div className="bg-accent-warm h-full w-[80%] rounded-full" />
                </div>
                <p className="text-xs text-text-muted mt-3 font-medium">Running slightly behind schedule. We adjusted your ETA.</p>
              </div>

              {/* Illusion 2: Success Ticket */}
              <div className="absolute bottom-10 left-4 md:left-10 w-64 glass-card p-5 z-30 shadow-float [animation-delay:1s]">
                <div className="w-10 h-10 rounded-full bg-accent-mint/20 flex items-center justify-center mb-3">
                  <CheckCircle2 className="text-accent-mint" size={20} />
                </div>
                <h4 className="text-lg font-bold text-text-primary mb-1">Slot Confirmed</h4>
                <p className="text-sm text-text-secondary font-medium">10:30 AM, Tomorrow</p>
                <div className="mt-4 pt-4 border-t border-border flex items-center justify-between">
                  <span className="text-xs text-text-muted font-semibold uppercase">ID: AP-88X</span>
                  <Activity size={14} className="text-accent" />
                </div>
              </div>

              {/* Background abstract element */}
              <div className="absolute inset-0 flex items-center justify-center pointer-events-none z-10">
                 <div className="w-64 h-64 border border-accent-light/30 rounded-full flex items-center justify-center">
                    <div className="w-48 h-48 border border-accent/20 rounded-full" />
                 </div>
              </div>

            </div>
          </div>
        </section>

        {/* ─── Trust Marquee ─────────────────────────── */}
        <section className="py-8 bg-white/40 border-y border-white/60 relative z-10 overflow-hidden backdrop-blur-sm">
          <div className="flex whitespace-nowrap animate-marquee w-[200%]">
            {/* First Set */}
            <div className="flex items-center gap-16 px-8 text-sm font-bold text-text-secondary tracking-widest uppercase w-1/2 justify-around">
              <span className="flex items-center gap-2"><Clock size={16} className="text-accent"/> 1.2M Hours Saved</span>
              <span className="flex items-center gap-2"><HeartPulse size={16} className="text-accent-mint"/> 450+ Private Clinics</span>
              <span className="flex items-center gap-2"><Landmark size={16} className="text-accent-light"/> 12 Municipal Corp.</span>
              <span className="flex items-center gap-2"><Users size={16} className="text-accent-warm"/> 2.4M Verified Users</span>
            </div>
            {/* Duplicate Set for infinite scroll illusion */}
            <div className="flex items-center gap-16 px-8 text-sm font-bold text-text-secondary tracking-widest uppercase w-1/2 justify-around">
              <span className="flex items-center gap-2"><Clock size={16} className="text-accent"/> 1.2M Hours Saved</span>
              <span className="flex items-center gap-2"><HeartPulse size={16} className="text-accent-mint"/> 450+ Private Clinics</span>
              <span className="flex items-center gap-2"><Landmark size={16} className="text-accent-light"/> 12 Municipal Corp.</span>
              <span className="flex items-center gap-2"><Users size={16} className="text-accent-warm"/> 2.4M Verified Users</span>
            </div>
          </div>
        </section>

        {/* ─── Bento Box Feature Grid ───────────────── */}
        <section className="pt-32 pb-40 px-6 relative z-10">
          <div className="max-w-7xl mx-auto">
            
            <div className="text-center mb-16 animate-slide-up">
              <h2 className="text-3xl md:text-5xl font-extrabold tracking-tight text-text-primary mb-5">
                Not a calendar. <span className="text-accent">An engine.</span>
              </h2>
              <p className="text-lg text-text-secondary font-medium max-w-2xl mx-auto">
                Appointments shouldn't be static text on a screen. Here&apos;s how our architecture actively defends your time.
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
                  
                  <div className="flex flex-wrap gap-4 mt-auto">
                    <Link href="/explore/healthcare" className="flex items-center gap-2 bg-white px-5 py-3.5 rounded-2xl border border-border shadow-soft hover:border-accent hover:shadow-float hover:-translate-y-1 transition-all text-sm font-bold text-text-primary">
                      <HeartPulse size={18} className="text-accent" /> Healthcare
                    </Link>
                    <Link href="/explore/government" className="flex items-center gap-2 bg-white px-5 py-3.5 rounded-2xl border border-border shadow-soft hover:border-accent hover:shadow-float hover:-translate-y-1 transition-all text-sm font-bold text-text-primary">
                      <Landmark size={18} className="text-text-primary" /> Government
                    </Link>
                    <Link href="/explore/services" className="flex items-center gap-2 bg-white px-5 py-3.5 rounded-2xl border border-border shadow-soft hover:border-accent hover:shadow-float hover:-translate-y-1 transition-all text-sm font-bold text-text-primary">
                      <Settings2 size={18} className="text-text-muted" /> Services
                    </Link>
                  </div>
                </div>
              </div>

            </div>
          </div>
        </section>

        {/* ─── Footer ─────────────────────────── */}
        <footer className="pt-20 pb-10 px-6 relative z-10 border-t border-border">
          <div className="max-w-7xl mx-auto flex flex-col items-center">
            
            <h2 className="text-3xl md:text-4xl font-extrabold tracking-tight text-text-primary mb-8">
              Join the flow.
            </h2>
            
            <div className="flex flex-col sm:flex-row gap-4 mb-20">
              <Link href="/auth/signup" className="btn-pastel-primary px-8 py-3 text-base shadow-soft hover:scale-105 transition-transform">
                Start Booking Now
              </Link>
            </div>

            <div className="w-full flex flex-col sm:flex-row justify-between items-center text-xs font-bold text-text-muted tracking-widest uppercase gap-4">
              <span>© {new Date().getFullYear()} APPOINTUNFIED</span>
              <div className="flex gap-6 items-center">
                <Link href="#" className="hover:text-text-primary transition-colors">Privacy</Link>
                <Link href="#" className="hover:text-text-primary transition-colors">Terms</Link>
                <button onClick={() => openGate('ADMIN')} className="hover:text-text-primary transition-colors border-l border-border pl-6">Admin</button>
                <button onClick={() => openGate('SUPER_ADMIN')} className="hover:text-red-400 text-text-muted transition-colors">Super</button>
              </div>
            </div>

          </div>
        </footer>

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

'use client'

import Link from 'next/link'
import { useRouter, usePathname } from 'next/navigation'
import { useState, useEffect } from 'react'
import { Menu, X, Activity } from 'lucide-react'
import { useAuthStore } from '@/lib/store'
import { cn } from '@/lib/utils'

export function Navbar() {
  const { isAuthenticated, logout, user } = useAuthStore()
  const pathname = usePathname()
  const router = useRouter()
  const [menuOpen, setMenuOpen] = useState(false)
  const [scrolled, setScrolled] = useState(false)

  const handleLogout = () => {
    logout()
    window.location.href = '/'
  }

  useEffect(() => {
    const handleScroll = () => setScrolled(window.scrollY > 20)
    window.addEventListener('scroll', handleScroll)
    return () => window.removeEventListener('scroll', handleScroll)
  }, [])

  const navLinks = [
    { href: '/explore/healthcare', label: 'Healthcare' },
    { href: '/explore/government', label: 'Government' },
    { href: '/explore/services', label: 'Services' },
  ]

  const showExploreLinks = !isAuthenticated || user?.role === 'PUBLIC'

  return (
    <nav className={cn(
      "fixed top-0 w-full z-50 transition-all duration-500",
      scrolled ? "bg-white/70 backdrop-blur-xl border-b border-white shadow-soft py-3" : "bg-transparent py-5"
    )}>
      <div className="max-w-6xl mx-auto px-6 w-full flex items-center justify-between">

        {/* Minimal Logo */}
        <Link href="/" className="flex items-center gap-3 group">
          <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-gradient-to-br from-accent to-accent-light text-white shadow-soft group-hover:shadow-float transition-all duration-300 group-hover:-translate-y-0.5">
            <Activity size={20} strokeWidth={2.5} />
          </div>
          <span className="text-[17px] font-extrabold tracking-tight text-text-primary hidden sm:block">
            Appoint<span className="text-accent">Unified</span>
          </span>
        </Link>

        {/* Center Desktop Nav */}
        {showExploreLinks && (
          <div className="hidden md:flex items-center gap-8 bg-white/60 backdrop-blur-md border border-white px-6 py-2 rounded-full shadow-sm">
            {navLinks.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                className={cn(
                  'text-[14px] font-bold transition-colors duration-300',
                  pathname.startsWith(link.href)
                    ? 'text-accent'
                    : 'text-text-secondary hover:text-accent-light'
                )}
              >
                {link.label}
              </Link>
            ))}
          </div>
        )}

        {/* Right Nav */}
        <div className="flex items-center gap-4">
          {isAuthenticated ? (
            <div className="hidden sm:flex items-center gap-3">
              <button onClick={handleLogout} className="text-[14px] font-bold text-text-secondary hover:text-red-500 transition-colors px-2">
                Log Out
              </button>
              {!pathname.includes('dashboard') && (
                <Link 
                  href={user?.role === 'PROFESSIONAL' ? '/professional/dashboard' : user?.role === 'SUPER_ADMIN' ? '/super-admin/dashboard' : user?.role === 'ADMIN' ? '/admin/dashboard' : '/dashboard'} 
                  className="btn-pastel-primary py-2 px-5 text-[13px]"
                >
                  Dashboard
                </Link>
              )}
            </div>
          ) : (
            <>
              <Link href="/auth/login" className="hidden sm:block text-[14px] font-bold text-text-secondary hover:text-accent transition-colors px-4">
                Log In
              </Link>
              <Link href="/auth/signup" className="hidden sm:flex btn-pastel-primary py-2.5 px-6 text-[14px] shadow-soft">
                Get Started
              </Link>
            </>
          )}

          {/* Mobile toggle */}
          <button className="md:hidden text-text-primary p-2 bg-white/60 backdrop-blur-md rounded-xl border border-white shadow-sm" onClick={() => setMenuOpen(!menuOpen)}>
            {menuOpen ? <X size={24} strokeWidth={2} /> : <Menu size={24} strokeWidth={2} />}
          </button>
        </div>

      </div>

      {/* Mobile Nav overlay */}
      {menuOpen && (
        <div className="md:hidden absolute top-full left-0 w-full p-4 animate-fade-in">
          <div className="glass-card w-full flex flex-col gap-4 p-6 bg-white/90">
            {showExploreLinks && navLinks.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                className="text-lg font-bold text-text-primary hover:text-accent transition-colors"
                onClick={() => setMenuOpen(false)}
              >
                {link.label}
              </Link>
            ))}
            {!isAuthenticated ? (
              <div className="flex flex-col gap-3 mt-4 pt-4 border-t border-border">
                <Link href="/auth/login" className="btn-pastel-secondary w-full" onClick={() => setMenuOpen(false)}>Log In</Link>
                <Link href="/auth/signup" className="btn-pastel-primary w-full" onClick={() => setMenuOpen(false)}>Get Started</Link>
              </div>
            ) : (
              <div className="mt-4 pt-4 border-t border-border flex flex-col gap-3">
                {!pathname.includes('dashboard') && (
                  <Link 
                    href={user?.role === 'PROFESSIONAL' ? '/professional/dashboard' : user?.role === 'SUPER_ADMIN' ? '/super-admin/dashboard' : user?.role === 'ADMIN' ? '/admin/dashboard' : '/dashboard'} 
                    className="btn-pastel-primary w-full" 
                    onClick={() => setMenuOpen(false)}
                  >
                    Go to Dashboard
                  </Link>
                )}
                <button onClick={() => { handleLogout(); setMenuOpen(false); }} className="btn-pastel-secondary w-full">Log Out</button>
              </div>
            )}
          </div>
        </div>
      )}
    </nav>
  )
}

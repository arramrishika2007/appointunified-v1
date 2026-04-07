import { NextRequest, NextResponse } from 'next/server'

/**
 * Edge middleware — enforces authentication on protected routes
 * without hitting the API (reads localStorage is not available at edge,
 * so we check the cookie-based token or redirect to login).
 *
 * Strategy: We store a lightweight "au_logged_in" cookie on login
 * (set client-side via the auth store). Middleware checks this cookie
 * to gate protected routes. The real JWT validation still happens
 * server-side in Spring Boot for every API call.
 */

const PROTECTED_PREFIXES = [
  '/dashboard',
  '/booking',
  '/settings',
  '/professional',
  '/admin',
  '/super-admin',
]

const PROFESSIONAL_ONLY = ['/professional']
const ADMIN_ONLY = ['/admin']
const SUPER_ADMIN_ONLY = ['/super-admin']

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl

  const isProtected = PROTECTED_PREFIXES.some(prefix => pathname.startsWith(prefix))
  if (!isProtected) return NextResponse.next()

  // Check lightweight session cookie (set by client after login)
  const loggedIn = request.cookies.get('au_logged_in')?.value === '1'
  const role = request.cookies.get('au_role')?.value ?? ''

  if (!loggedIn) {
    const loginUrl = new URL('/auth/login', request.url)
    loginUrl.searchParams.set('redirect', pathname)
    return NextResponse.redirect(loginUrl)
  }

  // Role guards
  if (ADMIN_ONLY.some(p => pathname.startsWith(p)) &&
      !['ADMIN', 'SUPER_ADMIN'].includes(role)) {
    return NextResponse.redirect(new URL('/dashboard', request.url))
  }

  if (SUPER_ADMIN_ONLY.some(p => pathname.startsWith(p)) && role !== 'SUPER_ADMIN') {
    return NextResponse.redirect(new URL('/dashboard', request.url))
  }

  if (PROFESSIONAL_ONLY.some(p => pathname.startsWith(p)) &&
      role !== 'PROFESSIONAL' &&
      !['ADMIN', 'SUPER_ADMIN'].includes(role)) {
    return NextResponse.redirect(new URL('/dashboard', request.url))
  }

  return NextResponse.next()
}

export const config = {
  // Match all protected paths, skip Next.js internals and static files
  matcher: [
    '/dashboard/:path*',
    '/booking/:path*',
    '/settings/:path*',
    '/professional/:path*',
    '/admin/:path*',
    '/super-admin/:path*',
  ],
}

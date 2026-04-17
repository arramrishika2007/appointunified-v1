import Link from 'next/link'
import { Navbar } from '@/components/layout/Navbar'

export default function NotFound() {
  return (
    <>
      <Navbar />
      <main className="min-h-[70vh] flex items-center justify-center p-4">
        <div className="text-center max-w-sm">
          <div className="text-8xl font-black text-slate-200 mb-4 select-none">404</div>
          <h1 className="text-2xl font-bold text-slate-900 mb-2">Page not found</h1>
          <p className="text-slate-500 text-sm mb-8">
            The page you&apos;re looking for doesn&apos;t exist or has been moved.
          </p>
          <div className="flex flex-col sm:flex-row gap-3 justify-center">
            <Link href="/" className="btn-primary">Go home</Link>
            <Link href="/explore/healthcare" className="btn-secondary">Browse providers</Link>
          </div>
        </div>
      </main>
    </>
  )
}

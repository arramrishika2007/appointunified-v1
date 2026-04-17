'use client'

import { useEffect, useMemo, useState } from 'react'
import { useRouter } from 'next/navigation'
import { CheckCircle2, Loader2, ShieldAlert, XCircle } from 'lucide-react'
import { format } from 'date-fns'
import toast from 'react-hot-toast'
import { SuperAdminShell } from '@/components/layout/SuperAdminShell'
import { api } from '@/lib/api'
import { useAuthStore } from '@/lib/store'
import { cn } from '@/lib/utils'

interface DecisionAuditItem {
	professionalId: string
	professionalName: string
	decision: 'APPROVED' | 'REJECTED'
	reason?: string
	decidedAt: string
	adminName: string
	adminEmail?: string
	specialty?: string
	licenseNumber?: string
}

interface SectorDecisionSummary {
	sector: string
	approvedCount: number
	rejectedCount: number
	decisions: DecisionAuditItem[]
}

const SECTOR_LABELS: Record<string, string> = {
	HEALTHCARE: 'Healthcare',
	GOVERNMENT: 'Government',
	SERVICES: 'Services',
	UNKNOWN: 'Unknown',
}

export default function SuperAdminDashboardPage() {
	const router = useRouter()
	const { isAuthenticated, user, hasHydrated } = useAuthStore()

	const [loading, setLoading] = useState(true)
	const [rows, setRows] = useState<SectorDecisionSummary[]>([])
	const [selectedSector, setSelectedSector] = useState<string>('ALL')

	useEffect(() => {
		if (!hasHydrated) return
		if (!isAuthenticated || user?.role !== 'SUPER_ADMIN') {
			router.push('/dashboard')
		}
	}, [hasHydrated, isAuthenticated, user, router])

	useEffect(() => {
		if (!hasHydrated || !isAuthenticated || user?.role !== 'SUPER_ADMIN') return

		setLoading(true)
		api.get('/admin/super-admin/verification-decisions?size=300')
			.then((res) => {
				const payload: SectorDecisionSummary[] = res.data?.data ?? []
				setRows(payload)
			})
			.catch(() => {
				setRows([])
				toast.error('Failed to load super-admin decision audit')
			})
			.finally(() => setLoading(false))
	}, [hasHydrated, isAuthenticated, user])

	const totals = useMemo(() => {
		return rows.reduce(
			(acc, sector) => {
				acc.approved += sector.approvedCount
				acc.rejected += sector.rejectedCount
				acc.total += sector.decisions.length
				return acc
			},
			{ approved: 0, rejected: 0, total: 0 }
		)
	}, [rows])

	const sectors = useMemo(() => {
		return ['ALL', ...rows.map((s) => s.sector)]
	}, [rows])

	const visibleRows = useMemo(() => {
		if (selectedSector === 'ALL') return rows
		return rows.filter((row) => row.sector === selectedSector)
	}, [rows, selectedSector])

	if (!hasHydrated) {
		return (
			<SuperAdminShell>
				<main className="min-h-screen bg-slate-50">
					<div className="container-page py-16 flex justify-center">
						<Loader2 size={28} className="animate-spin text-brand-600" />
					</div>
				</main>
			</SuperAdminShell>
		)
	}

	return (
		<SuperAdminShell>
			<main className="min-h-screen bg-slate-50">
				<div className="container-page py-6 sm:py-8">
					<div className="mb-6 flex items-start gap-3 sm:items-center">
						<div className="h-10 w-10 shrink-0 rounded-xl bg-red-100 flex items-center justify-center">
							<ShieldAlert size={20} className="text-red-600" />
						</div>
						<div>
							<h1 className="text-xl font-bold text-slate-900 sm:text-2xl">Super Admin Decision Dashboard</h1>
							<p className="text-sm text-slate-500">Track which admin approved or rejected professionals, including decision reasons, grouped by sector.</p>
						</div>
					</div>

					<div className="mb-6 grid gap-3 sm:grid-cols-3">
						<div className="card p-4">
							<p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Total Decisions</p>
							<p className="mt-1 text-2xl font-bold text-slate-900">{totals.total}</p>
						</div>
						<div className="card p-4">
							<p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Approved</p>
							<p className="mt-1 text-2xl font-bold text-emerald-700">{totals.approved}</p>
						</div>
						<div className="card p-4">
							<p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Rejected</p>
							<p className="mt-1 text-2xl font-bold text-red-700">{totals.rejected}</p>
						</div>
					</div>

					<div className="mb-6 flex flex-wrap gap-2">
						{sectors.map((sector) => (
							<button
								key={sector}
								type="button"
								onClick={() => setSelectedSector(sector)}
								className={cn(
									'rounded-lg border px-3 py-1.5 text-xs font-semibold transition-colors',
									selectedSector === sector
										? 'border-brand-500 bg-brand-50 text-brand-700'
										: 'border-slate-200 bg-white text-slate-600 hover:bg-slate-50'
								)}
							>
								{sector === 'ALL' ? 'All sectors' : (SECTOR_LABELS[sector] ?? sector)}
							</button>
						))}
					</div>

					{loading ? (
						<div className="flex justify-center py-16">
							<Loader2 size={28} className="animate-spin text-brand-600" />
						</div>
					) : visibleRows.length === 0 ? (
						<div className="card p-12 text-center">
							<p className="font-medium text-slate-700">No decision records available for this filter.</p>
						</div>
					) : (
						<div className="space-y-4">
							{visibleRows.map((sectorRow) => (
								<section key={sectorRow.sector} className="card p-4 sm:p-5">
									<div className="mb-3 flex flex-wrap items-center justify-between gap-2">
										<h2 className="text-base font-semibold text-slate-900">{SECTOR_LABELS[sectorRow.sector] ?? sectorRow.sector}</h2>
										<div className="flex items-center gap-2 text-xs">
											<span className="rounded-full bg-emerald-100 px-2 py-1 font-semibold text-emerald-700">Approved: {sectorRow.approvedCount}</span>
											<span className="rounded-full bg-red-100 px-2 py-1 font-semibold text-red-700">Rejected: {sectorRow.rejectedCount}</span>
										</div>
									</div>

									<div className="space-y-2">
										{sectorRow.decisions.map((item) => (
											<article key={`${item.professionalId}-${item.decidedAt}`} className="rounded-lg border border-slate-200 bg-white p-3">
												<div className="flex flex-wrap items-start justify-between gap-2">
													<div>
														<p className="font-semibold text-slate-900">{item.professionalName}</p>
														<p className="text-xs text-slate-500">
															{item.specialty || 'No specialty'}
															{item.licenseNumber ? ` • License: ${item.licenseNumber}` : ''}
														</p>
													</div>
													<span className={cn(
														'inline-flex items-center gap-1 rounded-full px-2 py-1 text-xs font-semibold',
														item.decision === 'APPROVED'
															? 'bg-emerald-100 text-emerald-700'
															: 'bg-red-100 text-red-700'
													)}>
														{item.decision === 'APPROVED' ? <CheckCircle2 size={12} /> : <XCircle size={12} />}
														{item.decision}
													</span>
												</div>
												<div className="mt-2 grid gap-1 text-xs text-slate-600 sm:grid-cols-2">
													<p><span className="font-medium text-slate-700">Admin:</span> {item.adminName}</p>
													<p><span className="font-medium text-slate-700">When:</span> {format(new Date(item.decidedAt), 'MMM d, yyyy p')}</p>
													{item.adminEmail && <p><span className="font-medium text-slate-700">Admin email:</span> {item.adminEmail}</p>}
													<p className="sm:col-span-2"><span className="font-medium text-slate-700">Reason:</span> {item.reason?.trim() || 'No reason provided'}</p>
												</div>
											</article>
										))}
									</div>
								</section>
							))}
						</div>
					)}
				</div>
			</main>
		</SuperAdminShell>
	)
}
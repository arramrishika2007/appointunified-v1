'use client'

import { createContext, ReactNode, useContext } from 'react'

export type ShellRole = 'USER' | 'PROFESSIONAL' | 'ADMIN' | 'SUPER_ADMIN' | null

const ShellContext = createContext<ShellRole>(null)

export function ShellProvider({ role, children }: { role: ShellRole; children: ReactNode }) {
  return <ShellContext.Provider value={role}>{children}</ShellContext.Provider>
}

export function useShellRole() {
  return useContext(ShellContext)
}

import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { AuthUser, TokenPair } from '@/types'
import { setTokens, clearTokens } from '@/lib/api'

interface AuthState {
  user: AuthUser | null
  isAuthenticated: boolean
  isLoading: boolean
  hasHydrated: boolean
  login: (tokenPair: TokenPair) => void
  logout: () => void
  updateUser: (user: Partial<AuthUser>) => void
  setLoading: (loading: boolean) => void
}

/** Set a lightweight cookie for edge middleware (no JWT — just presence + role) */
function setSessionCookie(role: string) {
  if (typeof document === 'undefined') return
  const maxAge = 60 * 60 * 24 * 30 // 30 days
  document.cookie = `au_logged_in=1; path=/; max-age=${maxAge}; SameSite=Lax`
  document.cookie = `au_role=${role}; path=/; max-age=${maxAge}; SameSite=Lax`
}

function clearSessionCookies() {
  if (typeof document === 'undefined') return
  document.cookie = 'au_logged_in=; path=/; max-age=0'
  document.cookie = 'au_role=; path=/; max-age=0'
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      isAuthenticated: false,
      isLoading: false,
      hasHydrated: false,

      login: (tokenPair: TokenPair) => {
        setTokens({
          accessToken: tokenPair.accessToken,
          refreshToken: tokenPair.refreshToken,
        })
        setSessionCookie(tokenPair.user.role)
        set({
          user: tokenPair.user,
          isAuthenticated: true,
          isLoading: false,
        })
      },

      logout: () => {
        clearTokens()
        clearSessionCookies()
        set({ user: null, isAuthenticated: false })
      },

      updateUser: (partial: Partial<AuthUser>) =>
        set((state) => ({
          user: state.user ? { ...state.user, ...partial } : null,
        })),

      setLoading: (loading: boolean) => set({ isLoading: loading }),
    }),
    {
      name: 'au_auth',
      partialize: (state) => ({ user: state.user, isAuthenticated: state.isAuthenticated }),
      onRehydrateStorage: () => (state, error) => {
        if (state) {
          state.setLoading(false);
          setTimeout(() => {
            useAuthStore.setState({ hasHydrated: true });
          }, 0);
        }
      },
    }
  )
)

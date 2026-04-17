'use client'

import { useEffect } from 'react'
import { devicesApi } from '@/lib/api'
import { useAuthStore } from '@/lib/store'
import { generateFcmToken } from '@/lib/firebase-messaging'

const TOKEN_STORAGE_KEY = 'au_fcm_token'
const TOKEN_USER_STORAGE_KEY = 'au_fcm_user'

export function PushNotificationBootstrap() {
  const { hasHydrated, isAuthenticated, user } = useAuthStore()

  useEffect(() => {
    if (!hasHydrated || !isAuthenticated || !user?.id) {
      return
    }

    let cancelled = false

    const registerIfAvailable = async () => {
      try {
        // Do not auto-prompt; only bind token if permission is already granted.
        const fcmToken = await generateFcmToken({ requestPermission: false })
        if (!fcmToken || cancelled) {
          return
        }

        const lastToken = localStorage.getItem(TOKEN_STORAGE_KEY)
        const lastUser = localStorage.getItem(TOKEN_USER_STORAGE_KEY)
        if (lastToken === fcmToken && lastUser === user.id) {
          return
        }

        await devicesApi.registerFcmToken({
          fcmToken,
          platform: 'web',
          deviceName: typeof navigator !== 'undefined' ? navigator.userAgent : 'web',
        })

        localStorage.setItem(TOKEN_STORAGE_KEY, fcmToken)
        localStorage.setItem(TOKEN_USER_STORAGE_KEY, user.id)
      } catch {
        // Optional path: failures should not block app usage.
      }
    }

    registerIfAvailable()

    return () => {
      cancelled = true
    }
  }, [hasHydrated, isAuthenticated, user?.id])

  return null
}

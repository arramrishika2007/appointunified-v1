import { getMessaging, getToken, isSupported } from 'firebase/messaging'
import { firebaseApp, isFirebaseConfigured } from '@/lib/firebase'

export async function generateFcmToken(): Promise<string | null> {
  if (!isFirebaseConfigured || !firebaseApp) {
    return null
  }

  if (!(await isSupported())) {
    return null
  }

  if (typeof window === 'undefined' || !('serviceWorker' in navigator) || !('Notification' in window)) {
    return null
  }

  const permission = await Notification.requestPermission()
  if (permission !== 'granted') {
    return null
  }

  const registration = await navigator.serviceWorker.register('/firebase-messaging-sw.js')
  const messaging = getMessaging(firebaseApp)

  const vapidKey = process.env.NEXT_PUBLIC_FIREBASE_VAPID_KEY
  if (!vapidKey) {
    return null
  }

  return getToken(messaging, {
    vapidKey,
    serviceWorkerRegistration: registration,
  })
}

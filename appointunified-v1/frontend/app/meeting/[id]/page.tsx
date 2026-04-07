'use client'

import React, { useEffect, useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { JitsiMeeting } from '@jitsi/react-sdk'
import { useAuthStore } from '@/lib/store'
import { ArrowLeft, Loader2 } from 'lucide-react'

export default function MeetingPage() {
  const params = useParams()
  const router = useRouter()
  const jitsiDomain = process.env.NEXT_PUBLIC_JITSI_DOMAIN || 'meet.jit.si'
  // Since the URL is AppointUnified-<UUID>, we extract the room name directly from the path or params
  const meetingId = params.id as string
  const { user, isAuthenticated } = useAuthStore()
  const [loading, setLoading] = useState(true)

  // Wait a small bit to ensure auth state is loaded
  useEffect(() => {
    const timer = setTimeout(() => setLoading(false), 500)
    return () => clearTimeout(timer)
  }, [])

  if (loading) {
    return (
      <div className="h-screen w-full bg-slate-900 flex items-center justify-center flex-col gap-4 text-white">
        <Loader2 size={32} className="animate-spin text-brand-500" />
        <p>Preparing your virtual meeting room...</p>
      </div>
    )
  }

  return (
    <div className="h-screen w-full bg-slate-900 relative">
      <div className="absolute top-4 left-4 z-10">
        <button 
            onClick={() => router.back()} 
            className="bg-white/10 hover:bg-white/20 text-white px-4 py-2 rounded-lg backdrop-blur-md transition-all text-sm font-medium"
        >
          <span className="inline-flex items-center gap-1.5"><ArrowLeft size={14} /> Back to Dashboard</span>
        </button>
      </div>
      <JitsiMeeting
        domain={jitsiDomain}
        roomName={`AppointUnified-${meetingId}`}
        configOverwrite={{
          startWithAudioMuted: true,
          disableModeratorIndicator: true,
          startScreenSharing: false,
          enableEmailInStats: false,
        }}
        interfaceConfigOverwrite={{
          DISABLE_JOIN_LEAVE_NOTIFICATIONS: true,
        }}
        userInfo={{
          displayName: user?.fullName || 'Guest User',
          email: user?.email || 'guest@example.com',
        }}
        onApiReady={(externalApi) => {
            // Can be used to handle "videoConferenceLeft" events, etc.
            externalApi.addListener('videoConferenceLeft', () => {
                router.push('/dashboard')
            })
        }}
        getIFrameRef={(iframeRef) => {
          iframeRef.style.height = '100%'
          iframeRef.style.width = '100%'
        }}
      />
    </div>
  )
}

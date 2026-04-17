 'use client'

import { useEffect } from 'react'
import { useRouter, useParams } from 'next/navigation'
import { appointmentsApi } from '@/lib/api'

export default function QueueAliasPage() {
  const router = useRouter()
  const params = useParams()
  const appointmentId = params.appointmentId as string

  useEffect(() => {
    let mounted = true

    appointmentsApi.getMyAppointments({ size: 100 }).then((response) => {
      if (!mounted) return

      const appointment = response.data.data.content?.find((item: { id: string; professional?: { id?: string; displayName?: string } }) => item.id === appointmentId)
      if (appointment?.professional?.id) {
        router.replace(
          `/queue?professional=${appointment.professional.id}&appointment=${appointment.id}&name=${encodeURIComponent(appointment.professional.displayName || 'Provider')}`
        )
        return
      }

      router.replace('/queue')
    })

    return () => {
      mounted = false
    }
  }, [appointmentId, router])

  return null
}

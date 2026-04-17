import { redirect } from 'next/navigation'

interface ProfessionalBookingAliasProps {
  params: {
    id: string
  }
}

export default function ProfessionalBookingAliasPage({ params }: ProfessionalBookingAliasProps) {
  redirect(`/booking/${params.id}`)
}

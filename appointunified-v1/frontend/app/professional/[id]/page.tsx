import { redirect } from 'next/navigation'

interface ProfessionalProfileAliasProps {
  params: {
    id: string
  }
}

export default function ProfessionalProfileAliasPage({ params }: ProfessionalProfileAliasProps) {
  redirect(`/provider/${params.id}`)
}

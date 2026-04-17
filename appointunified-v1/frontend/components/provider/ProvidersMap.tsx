'use client'

import { MapContainer, Marker, Popup, TileLayer, CircleMarker } from 'react-leaflet'
import L from 'leaflet'
import { ProfessionalSummary } from '@/types'

type ProvidersMapProps = {
  professionals: ProfessionalSummary[]
  userCoords?: { lat: number; lng: number } | null
}

const MapContainerAny = MapContainer as any
const MarkerAny = Marker as any
const TileLayerAny = TileLayer as any
const CircleMarkerAny = CircleMarker as any

const providerIcon = L.icon({
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41],
})

export default function ProvidersMap({ professionals, userCoords }: ProvidersMapProps) {
  const points = professionals
    .filter((p) => typeof p.latitude === 'number' && typeof p.longitude === 'number')
    .map((p) => ({
      id: p.id,
      name: p.displayName,
      sector: p.sector,
      specialty: p.specialty,
      latitude: p.latitude as number,
      longitude: p.longitude as number,
    }))

  if (points.length === 0) {
    return (
      <div className="rounded-xl border border-slate-200 bg-slate-50 p-6 text-sm text-slate-500">
        Map preview unavailable because provider coordinates are missing.
      </div>
    )
  }

  const bounds: any = points.map((point) => [point.latitude, point.longitude])
  if (userCoords) {
    bounds.push([userCoords.lat, userCoords.lng])
  }

  return (
    <div className="relative aspect-[16/9] overflow-hidden rounded-xl border border-slate-200 bg-slate-100">
      <MapContainerAny bounds={bounds} className="h-full w-full" scrollWheelZoom={false} attributionControl={false}>
        <TileLayerAny url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" attribution="" />

        {points.map((point) => (
          <MarkerAny key={point.id} position={[point.latitude, point.longitude]} icon={providerIcon}>
            <Popup>
              <div className="text-sm">
                <p className="font-semibold text-slate-900">{point.name}</p>
                <p className="text-slate-600">{point.specialty || point.sector}</p>
              </div>
            </Popup>
          </MarkerAny>
        ))}

        {userCoords && (
          <CircleMarkerAny
            center={[userCoords.lat, userCoords.lng]}
            radius={8}
            pathOptions={{ color: '#1d4ed8', fillColor: '#2563eb', fillOpacity: 0.9 }}
          >
            <Popup>You are here</Popup>
          </CircleMarkerAny>
        )}
      </MapContainerAny>
    </div>
  )
}
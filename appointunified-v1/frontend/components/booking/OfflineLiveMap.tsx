'use client'

import { useMemo } from 'react'
import { CircleMarker, MapContainer, Polyline, TileLayer, Tooltip } from 'react-leaflet'

const MapContainerAny = MapContainer as any
const TileLayerAny = TileLayer as any
const CircleMarkerAny = CircleMarker as any
const TooltipAny = Tooltip as any
const PolylineAny = Polyline as any

type LatLng = { lat: number; lng: number }

type OfflineLiveMapProps = {
  provider?: LatLng | null
  client?: LatLng | null
}

export default function OfflineLiveMap({ provider, client }: OfflineLiveMapProps) {
  const center = useMemo<[number, number]>(() => {
    if (provider && client) return [(provider.lat + client.lat) / 2, (provider.lng + client.lng) / 2]
    if (provider) return [provider.lat, provider.lng]
    if (client) return [client.lat, client.lng]
    return [0, 0]
  }, [provider, client])

  const route = useMemo<[number, number][]>(() => {
    if (!provider || !client) return []
    return [
      [client.lat, client.lng],
      [provider.lat, provider.lng],
    ]
  }, [provider, client])

  return (
    <div className="overflow-hidden rounded-xl border border-slate-200">
      <MapContainerAny center={center} zoom={13} className="h-64 w-full" scrollWheelZoom={false} attributionControl={false}>
        <TileLayerAny
          attribution=""
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        {provider && (
          <CircleMarkerAny center={[provider.lat, provider.lng]} radius={9} pathOptions={{ color: '#16a34a', fillColor: '#22c55e', fillOpacity: 0.9 }}>
            <TooltipAny direction="top" offset={[0, -4]} opacity={1}>
              Professional location
            </TooltipAny>
          </CircleMarkerAny>
        )}

        {client && (
          <>
            <CircleMarkerAny center={[client.lat, client.lng]} radius={8} pathOptions={{ color: '#1d4ed8', fillColor: '#3b82f6', fillOpacity: 0.9 }}>
              <TooltipAny direction="top" offset={[0, -4]} opacity={1}>
                Your live location
              </TooltipAny>
            </CircleMarkerAny>
            {provider && <PolylineAny positions={route} pathOptions={{ color: '#0f172a', dashArray: '8 8' }} />}
          </>
        )}
      </MapContainerAny>
    </div>
  )
}

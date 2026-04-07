const GEOAPIFY_API_KEY = process.env.NEXT_PUBLIC_GEOAPIFY_API_KEY || ''

export interface GeocodeResult {
  latitude: number
  longitude: number
  formatted: string
}

export async function geocodeAddress(query: string): Promise<GeocodeResult | null> {
  if (!GEOAPIFY_API_KEY || !query.trim()) {
    return null
  }

  const url = new URL('https://api.geoapify.com/v1/geocode/search')
  url.searchParams.set('text', query.trim())
  url.searchParams.set('format', 'json')
  url.searchParams.set('apiKey', GEOAPIFY_API_KEY)

  const response = await fetch(url.toString())
  if (!response.ok) {
    return null
  }

  const data = await response.json() as {
    results?: Array<{
      lat: number
      lon: number
      formatted?: string
    }>
  }

  const result = data.results?.[0]
  if (!result) {
    return null
  }

  return {
    latitude: result.lat,
    longitude: result.lon,
    formatted: result.formatted || query.trim(),
  }
}

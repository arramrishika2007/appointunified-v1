export type CloudinaryUploadResult = {
  secure_url: string
  bytes?: number
  mimeType?: string
}

const toResult = (payload: { secure_url: string; bytes?: number; resource_type?: string }, file: File): CloudinaryUploadResult => ({
  secure_url: payload.secure_url,
  bytes: payload.bytes,
  mimeType: file.type || (payload.resource_type ? `application/${payload.resource_type}` : undefined),
})

async function uploadUnsignedWithPreset(cloudName: string, file: File, preset: string): Promise<CloudinaryUploadResult> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('upload_preset', preset)
  // Removed folder parameter because passing folder from client in unsigned presets
  // often causes Cloudinary to reject the request with HTTP 400.

  const response = await fetch(`https://api.cloudinary.com/v1_1/${cloudName}/auto/upload`, {
    method: 'POST',
    body: formData,
  })

  if (!response.ok) {
    const errorText = await response.text()
    // Explicitly bubble up the Cloudinary error for the UI to display
    throw new Error(`Cloudinary upload failed: ${errorText}`)
  }

  const payload = await response.json() as { secure_url: string; bytes?: number; resource_type?: string }
  return toResult(payload, file)
}

export async function uploadVerificationDocument(file: File): Promise<CloudinaryUploadResult> {
  const cloudName = process.env.NEXT_PUBLIC_CLOUDINARY_CLOUD_NAME
  const preset = process.env.NEXT_PUBLIC_CLOUDINARY_UPLOAD_PRESET

  if (!cloudName || !preset) {
    throw new Error('Cloudinary environment variables are missing (CLOUD_NAME or UPLOAD_PRESET).')
  }

  // Directly attempt unsigned upload and let it fail loudly if configured incorrectly
  return await uploadUnsignedWithPreset(cloudName, file, preset)
}
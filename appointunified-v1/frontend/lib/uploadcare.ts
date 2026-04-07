const UPLOADCARE_PUBLIC_KEY = process.env.NEXT_PUBLIC_UPLOADCARE_PUBLIC_KEY || ''

export async function uploadCareFile(file: File): Promise<string> {
  if (!UPLOADCARE_PUBLIC_KEY) {
    throw new Error('Uploadcare is not configured')
  }

  const formData = new FormData()
  formData.append('UPLOADCARE_PUB_KEY', UPLOADCARE_PUBLIC_KEY)
  formData.append('UPLOADCARE_STORE', '1')
  formData.append('file', file)

  const response = await fetch('https://upload.uploadcare.com/base/', {
    method: 'POST',
    body: formData,
  })

  if (!response.ok) {
    throw new Error(`Uploadcare upload failed: ${response.status}`)
  }

  const data = await response.json() as { file?: string; uuid?: string }
  const fileId = data.file || data.uuid

  if (!fileId) {
    throw new Error('Uploadcare did not return a file identifier')
  }

  return `https://ucarecdn.com/${fileId}/`
}

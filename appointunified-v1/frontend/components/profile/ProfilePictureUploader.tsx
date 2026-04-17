'use client'

import Image from 'next/image'
import { useCallback, useRef, useState } from 'react'
import { AxiosError } from 'axios'
import { profilePictureApi } from '@/lib/api'
import { ProcessedAvatarResponse } from '@/types'

interface ProfilePictureUploaderProps {
  currentAvatarUrl?: string
  currentAvatarThumbUrl?: string
  fullName?: string
  onUploadSuccess?: (urls: ProcessedAvatarResponse) => void
  onUploadError?: (error: string) => void
}

export function ProfilePictureUploader({
  currentAvatarUrl,
  currentAvatarThumbUrl,
  fullName = 'User',
  onUploadSuccess,
  onUploadError,
}: ProfilePictureUploaderProps) {
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [uploading, setUploading] = useState(false)
  const [previewUrl, setPreviewUrl] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  // Get upload initials from name
  const getInitials = (name: string) => {
    return name
      .split(' ')
      .map(word => word[0])
      .slice(0, 2)
      .join('')
      .toUpperCase()
  }

  const handleFileSelect = useCallback(async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (!file) return

    // Validate file type
    const acceptedTypes = ['image/jpeg', 'image/png', 'image/webp']
    if (!acceptedTypes.includes(file.type)) {
      setError('Please select a JPEG, PNG, or WebP image')
      return
    }

    // Validate file size (5MB max)
    if (file.size > 5 * 1024 * 1024) {
      setError('File size must be less than 5MB')
      return
    }

    try {
      setError(null)
      setUploading(true)

      // Get upload config from backend
      const configResponse = await profilePictureApi.getUploadConfig()
      const config = configResponse.data.data

      // Create preview
      const reader = new FileReader()
      reader.onload = e => {
        setPreviewUrl(e.target?.result as string)
      }
      reader.readAsDataURL(file)

      // Upload to Cloudinary using backend-signed parameters
      const formData = new FormData()
      formData.append('file', file)
      formData.append('folder', config.folder)
      formData.append('api_key', config.apiKey)
      formData.append('timestamp', String(config.timestamp))
      formData.append('signature', config.signature)

      const cloudinaryResponse = await fetch(
        `https://api.cloudinary.com/v1_1/${config.cloudName}/image/upload`,
        {
          method: 'POST',
          body: formData,
        }
      )

      if (!cloudinaryResponse.ok) {
        throw new Error('Failed to upload to Cloudinary')
      }

      const uploadedData = await cloudinaryResponse.json()
      const publicId = uploadedData.public_id

      // Process image on backend
      const processResponse = await profilePictureApi.processUploadedImage(
        publicId,
        file.name
      )

      if (!processResponse.data.data) {
        throw new Error('Failed to process image')
      }

      setPreviewUrl(null)
      onUploadSuccess?.(processResponse.data.data)
    } catch (err) {
      const axiosErr = err as AxiosError<{ message?: string }>
      const status = axiosErr.response?.status
      const backendMessage = axiosErr.response?.data?.message
      const authMessage = status === 401 || status === 403
        ? 'Your session has expired. Please sign in again and retry uploading your picture.'
        : null
      const errorMsg =
        authMessage || backendMessage || (err instanceof Error ? err.message : 'Failed to upload profile picture')
      setError(errorMsg)
      onUploadError?.(errorMsg)
    } finally {
      setUploading(false)
      if (fileInputRef.current) {
        fileInputRef.current.value = ''
      }
    }
  }, [onUploadSuccess, onUploadError])

  const handleDelete = useCallback(async () => {
    try {
      setError(null)
      await profilePictureApi.deleteAvatar()
      setPreviewUrl(null)
      onUploadSuccess?.({ avatarUrl: '', avatarThumbUrl: '', cloudinaryPublicId: '' })
    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : 'Failed to delete avatar'
      setError(errorMsg)
      onUploadError?.(errorMsg)
    }
  }, [onUploadSuccess, onUploadError])

  const displayUrl = previewUrl || currentAvatarUrl
  const initials = getInitials(fullName)

  return (
    <div className="flex flex-col items-center space-y-4">
      {/* Avatar Display */}
      <div className="relative">
        <div className="w-32 h-32 rounded-full bg-gradient-to-br from-blue-400 to-purple-500 flex items-center justify-center overflow-hidden border-4 border-gray-200">
          {displayUrl ? (
            <Image
              src={displayUrl}
              alt="Profile picture"
              fill
              className="object-cover"
              priority
            />
          ) : (
            <span className="text-5xl font-bold text-white">{initials}</span>
          )}
        </div>

        {/* Upload overlay icon */}
        <button
          onClick={() => fileInputRef.current?.click()}
          disabled={uploading}
          className="absolute bottom-0 right-0 bg-blue-500 hover:bg-blue-600 disabled:bg-gray-400 text-white rounded-full p-3 shadow-lg transition-colors"
          title="Upload new picture"
        >
          <svg
            className="w-5 h-5"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z"
            />
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M15 13a3 3 0 11-6 0 3 3 0 016 0z"
            />
          </svg>
        </button>
      </div>

      {/* Hidden file input */}
      <input
        ref={fileInputRef}
        type="file"
        accept="image/jpeg,image/png,image/webp"
        onChange={handleFileSelect}
        className="hidden"
        aria-label="Upload profile picture"
        disabled={uploading}
      />

      {/* Status messages */}
      {uploading && <p className="text-sm text-blue-600">Uploading...</p>}
      {error && <p className="text-sm text-red-600">{error}</p>}

      {/* Delete button */}
      {(currentAvatarUrl || previewUrl) && !uploading && (
        <button
          onClick={handleDelete}
          className="text-sm text-red-600 hover:text-red-700 transition-colors"
        >
          Remove Picture
        </button>
      )}

      {/* Upload info */}
      <p className="text-xs text-gray-500 text-center">
        JPEG, PNG or WebP • Max 5MB
      </p>
    </div>
  )
}

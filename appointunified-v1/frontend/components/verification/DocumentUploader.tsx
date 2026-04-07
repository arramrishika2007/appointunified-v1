'use client'

import { useState } from 'react'
import { Loader2, UploadCloud } from 'lucide-react'
import toast from 'react-hot-toast'
import { verificationApi } from '@/lib/api-v2'
import { uploadVerificationDocument } from '@/lib/cloudinary'

interface DocumentUploaderProps {
  docType: string
  onUploaded?: () => void
}

export function DocumentUploader({ docType, onUploaded }: DocumentUploaderProps) {
  const [docUrl, setDocUrl] = useState('')
  const [mimeType, setMimeType] = useState('')
  const [fileSizeKb, setFileSizeKb] = useState('')
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [loading, setLoading] = useState(false)
  const [uploading, setUploading] = useState(false)

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault()

    let finalDocUrl = docUrl.trim()
    let finalMimeType = mimeType.trim()
    let finalFileSizeBytes = fileSizeKb ? Number(fileSizeKb) * 1024 : undefined

    if (selectedFile) {
      setUploading(true)
      try {
        const uploaded = await uploadVerificationDocument(selectedFile)
        finalDocUrl = uploaded.secure_url
        finalMimeType = finalMimeType || uploaded.mimeType || selectedFile.type
        finalFileSizeBytes = uploaded.bytes ?? selectedFile.size
      } catch (error) {
        toast.error((error as Error)?.message ?? 'Cloudinary upload failed')
        setUploading(false)
        return
      } finally {
        setUploading(false)
      }
    }

    if (!finalDocUrl) {
      toast.error('Upload a file or paste a document URL')
      return
    }

    setLoading(true)
    try {
      await verificationApi.submitDocument({
        docType,
        docUrl: finalDocUrl,
        mimeType: finalMimeType || undefined,
        fileSizeBytes: finalFileSizeBytes,
      })
      toast.success('Document submitted for review')
      setDocUrl('')
      setMimeType('')
      setFileSizeKb('')
      setSelectedFile(null)
      onUploaded?.()
    } catch (error: unknown) {
      const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message
      toast.error(message ?? 'Failed to submit document')
    } finally {
      setLoading(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="rounded-xl border border-slate-200 bg-white p-3 space-y-2">
      <div>
        <label className="label text-xs">Upload File</label>
        <input
          type="file"
          accept="image/*,application/pdf"
          onChange={(event) => setSelectedFile(event.target.files?.[0] ?? null)}
          aria-label="Upload verification document"
          title="Upload verification document"
          className="input text-sm file:mr-3 file:rounded-md file:border-0 file:bg-brand-600 file:px-3 file:py-1.5 file:text-xs file:font-semibold file:text-white hover:file:bg-brand-700"
        />
        <p className="mt-1 text-[11px] text-slate-400">Uploads go to Cloudinary using the configured preset.</p>
      </div>

      <div>
        <label className="label text-xs">Document URL <span className="text-slate-400">(optional fallback)</span></label>
        <input
          value={docUrl}
          onChange={(event) => setDocUrl(event.target.value)}
          className="input text-sm"
          placeholder="https://..."
        />
      </div>

      <div className="grid grid-cols-2 gap-2">
        <div>
          <label className="label text-xs">MIME Type (optional)</label>
          <input
            value={mimeType}
            onChange={(event) => setMimeType(event.target.value)}
            className="input text-sm"
            placeholder="image/jpeg"
          />
        </div>
        <div>
          <label className="label text-xs">Approx Size KB (optional)</label>
          <input
            value={fileSizeKb}
            onChange={(event) => setFileSizeKb(event.target.value)}
            className="input text-sm"
            type="number"
            min={0}
            placeholder="240"
          />
        </div>
      </div>

      <button type="submit" disabled={loading || uploading} className="btn-primary text-xs px-3 py-2">
        {loading || uploading ? <Loader2 size={13} className="animate-spin" /> : <UploadCloud size={13} />}
        Submit Document
      </button>
    </form>
  )
}

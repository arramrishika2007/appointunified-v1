'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Loader2, Save, Shield, Upload, User } from 'lucide-react'
import { UserShell } from '@/components/layout/UserShell'
import { api } from '@/lib/api'
import { uploadCareFile } from '@/lib/uploadcare'
import { useAuthStore } from '@/lib/store'
import { cn } from '@/lib/utils'
import toast from 'react-hot-toast'

const profileSchema = z.object({
  fullName:  z.string().min(2, 'Name must be at least 2 characters'),
  email:     z.string().email('Invalid email').optional().or(z.literal('')),
  avatarUrl: z.string().url().optional().or(z.literal('')),
})

const passwordSchema = z.object({
  currentPassword: z.string().min(1, 'Current password is required'),
  newPassword:     z.string().min(6, 'New password must be at least 6 characters'),
  confirmPassword: z.string(),
}).refine(d => d.newPassword === d.confirmPassword, {
  message: 'Passwords do not match',
  path: ['confirmPassword'],
})

type ProfileForm   = z.infer<typeof profileSchema>
type PasswordForm  = z.infer<typeof passwordSchema>

type Tab = 'profile' | 'security'

export default function SettingsPage() {
  const router = useRouter()
  const { isAuthenticated, user, updateUser } = useAuthStore()
  const [tab, setTab] = useState<Tab>('profile')
  const [uploadingAvatar, setUploadingAvatar] = useState(false)

  useEffect(() => {
    if (!isAuthenticated) router.push('/auth/login')
  }, [isAuthenticated, router])

  const profileForm = useForm<ProfileForm>({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      fullName:  user?.fullName  ?? '',
      email:     user?.email     ?? '',
      avatarUrl: user?.avatarUrl ?? '',
    },
  })

  const passwordForm = useForm<PasswordForm>({ resolver: zodResolver(passwordSchema) })

  const onProfileSave = async (data: ProfileForm) => {
    try {
      const res = await api.patch('/users/me', {
        fullName:  data.fullName,
        email:     data.email     || undefined,
        avatarUrl: data.avatarUrl || undefined,
      })
      updateUser(res.data.data)
      toast.success('Profile updated')
    } catch (err: unknown) {
      toast.error((err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Update failed')
    }
  }

  const onPasswordSave = async (data: PasswordForm) => {
    try {
      await api.patch('/users/me/password', {
        currentPassword: data.currentPassword,
        newPassword:     data.newPassword,
      })
      toast.success('Password changed successfully')
      passwordForm.reset()
    } catch (err: unknown) {
      toast.error((err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Failed to change password')
    }
  }

  const TABS: { id: Tab; label: string; icon: React.ReactNode }[] = [
    { id: 'profile',  label: 'Profile',  icon: <User size={15} />   },
    { id: 'security', label: 'Security', icon: <Shield size={15} /> },
  ]

  return (
    <UserShell>
      <main className="min-h-screen bg-slate-50">
        <div className="container-page py-8 max-w-2xl">

          <h1 className="text-2xl font-bold text-slate-900 mb-6">Account Settings</h1>

          {/* Tabs */}
          <div className="flex gap-1 bg-slate-100 p-1 rounded-xl mb-6 w-fit">
            {TABS.map(t => (
              <button
                key={t.id}
                onClick={() => setTab(t.id)}
                className={cn(
                  'flex items-center gap-1.5 rounded-lg px-4 py-2 text-sm font-medium transition-all',
                  tab === t.id ? 'bg-white shadow-sm text-slate-900' : 'text-slate-500 hover:text-slate-700'
                )}
              >
                {t.icon} {t.label}
              </button>
            ))}
          </div>

          {/* Profile tab */}
          {tab === 'profile' && (
            <div className="card p-7">
              <h2 className="font-semibold text-slate-900 mb-5">Profile Information</h2>
              <form onSubmit={profileForm.handleSubmit(onProfileSave)} className="space-y-4">
                <div>
                  <label className="label">Full Name</label>
                  <input {...profileForm.register('fullName')} className={cn('input', profileForm.formState.errors.fullName && 'input-error')} />
                  {profileForm.formState.errors.fullName && (
                    <p className="error-text">{profileForm.formState.errors.fullName.message}</p>
                  )}
                </div>
                <div>
                  <label className="label">Email <span className="text-slate-400 font-normal">(optional)</span></label>
                  <input {...profileForm.register('email')} type="email" className={cn('input', profileForm.formState.errors.email && 'input-error')} />
                </div>
                <div>
                  <label className="label">Phone</label>
                  <input value={user?.phone ?? ''} disabled title="Phone number" className="input bg-slate-50 text-slate-400 cursor-not-allowed" />
                  <p className="text-xs text-slate-400 mt-1">Phone number cannot be changed.</p>
                </div>
                <div>
                  <label className="label">Avatar URL <span className="text-slate-400 font-normal">(Cloudinary link)</span></label>
                  <input {...profileForm.register('avatarUrl')} className="input" placeholder="https://res.cloudinary.com/..." />
                  <div className="mt-2">
                    <input
                      id="avatar-uploadcare-input"
                      type="file"
                      accept="image/*"
                      className="hidden"
                      onChange={async (event) => {
                        const file = event.target.files?.[0]
                        if (!file) return
                        setUploadingAvatar(true)
                        try {
                          const url = await uploadCareFile(file)
                          profileForm.setValue('avatarUrl', url, { shouldDirty: true })
                          toast.success('Avatar uploaded')
                        } catch (error) {
                          toast.error(error instanceof Error ? error.message : 'Upload failed')
                        } finally {
                          setUploadingAvatar(false)
                          event.target.value = ''
                        }
                      }}
                    />
                    <label htmlFor="avatar-uploadcare-input" className="btn-secondary cursor-pointer text-sm mt-2">
                      {uploadingAvatar ? <Loader2 size={15} className="animate-spin" /> : <Upload size={15} />}
                      Upload via Uploadcare
                    </label>
                  </div>
                </div>
                <button
                  type="submit"
                  disabled={profileForm.formState.isSubmitting}
                  className="btn-primary"
                >
                  {profileForm.formState.isSubmitting
                    ? <Loader2 size={16} className="animate-spin" />
                    : <Save size={16} />
                  }
                  Save Changes
                </button>
              </form>
            </div>
          )}

          {/* Security tab */}
          {tab === 'security' && (
            <div className="space-y-5">
              <div className="card p-7">
                <h2 className="font-semibold text-slate-900 mb-5">Change Password</h2>
                <form onSubmit={passwordForm.handleSubmit(onPasswordSave)} className="space-y-4">
                  <div>
                    <label className="label">Current Password</label>
                    <input {...passwordForm.register('currentPassword')} type="password" className={cn('input', passwordForm.formState.errors.currentPassword && 'input-error')} />
                    {passwordForm.formState.errors.currentPassword && (
                      <p className="error-text">{passwordForm.formState.errors.currentPassword.message}</p>
                    )}
                  </div>
                  <div>
                    <label className="label">New Password</label>
                    <input {...passwordForm.register('newPassword')} type="password" className={cn('input', passwordForm.formState.errors.newPassword && 'input-error')} />
                    {passwordForm.formState.errors.newPassword && (
                      <p className="error-text">{passwordForm.formState.errors.newPassword.message}</p>
                    )}
                  </div>
                  <div>
                    <label className="label">Confirm New Password</label>
                    <input {...passwordForm.register('confirmPassword')} type="password" className={cn('input', passwordForm.formState.errors.confirmPassword && 'input-error')} />
                    {passwordForm.formState.errors.confirmPassword && (
                      <p className="error-text">{passwordForm.formState.errors.confirmPassword.message}</p>
                    )}
                  </div>
                  <button type="submit" disabled={passwordForm.formState.isSubmitting} className="btn-primary">
                    {passwordForm.formState.isSubmitting ? <Loader2 size={16} className="animate-spin" /> : <Shield size={16} />}
                    Update Password
                  </button>
                </form>
              </div>

              <div className="card p-7 border-red-200">
                <h2 className="font-semibold text-red-700 mb-2">Danger Zone</h2>
                <p className="text-sm text-slate-500 mb-4">
                  Deactivating your account will hide your profile and prevent new bookings.
                  You can reactivate by contacting support.
                </p>
                <button
                  onClick={async () => {
                    if (!confirm('Are you sure? This will deactivate your account.')) return
                    try {
                      await api.delete('/users/me')
                      toast.success('Account deactivated.')
                      useAuthStore.getState().logout()
                      router.push('/')
                    } catch { toast.error('Failed to deactivate.') }
                  }}
                  className="btn-danger"
                >
                  Deactivate Account
                </button>
              </div>
            </div>
          )}
        </div>
      </main>
    </UserShell>
  )
}

-- FEATURE C: Profile Picture Upload
-- Add avatar fields to users table for storing profile pictures at different sizes

ALTER TABLE users
ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500),
ADD COLUMN IF NOT EXISTS avatar_thumb_url VARCHAR(500);

-- Create indexes for quick avatar lookups
CREATE INDEX IF NOT EXISTS idx_users_avatar_url ON users(avatar_url) WHERE avatar_url IS NOT NULL;

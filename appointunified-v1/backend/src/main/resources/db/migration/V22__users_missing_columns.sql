-- Add missing user columns required by entity mapping
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS avatar_thumb_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS is_blocked_for_unpaid BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS unpaid_balance DECIMAL(10, 2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS risk_score DECIMAL(3, 2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

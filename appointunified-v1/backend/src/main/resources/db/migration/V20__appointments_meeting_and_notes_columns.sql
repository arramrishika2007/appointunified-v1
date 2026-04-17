-- Add missing appointment communication/notes columns required by entity mapping
ALTER TABLE appointments
    ADD COLUMN IF NOT EXISTS meeting_token VARCHAR(10),
    ADD COLUMN IF NOT EXISTS meet_link VARCHAR(255),
    ADD COLUMN IF NOT EXISTS client_notes TEXT,
    ADD COLUMN IF NOT EXISTS internal_notes TEXT,
    ADD COLUMN IF NOT EXISTS user_notes TEXT,
    ADD COLUMN IF NOT EXISTS cancellation_reason TEXT,
    ADD COLUMN IF NOT EXISTS is_virtual BOOLEAN DEFAULT FALSE;

-- Add missing professional columns required by entity mapping
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'availability_mood') THEN
        CREATE TYPE availability_mood AS ENUM ('AVAILABLE', 'BUSY', 'OFFLINE');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'badge_tier') THEN
        CREATE TYPE badge_tier AS ENUM ('NONE', 'BRONZE', 'SILVER', 'GOLD', 'PLATINUM');
    END IF;
END $$;

ALTER TABLE professionals
    ADD COLUMN IF NOT EXISTS upi_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS cover_url VARCHAR(255),
    ADD COLUMN IF NOT EXISTS bio TEXT,
    ADD COLUMN IF NOT EXISTS qualification TEXT,
    ADD COLUMN IF NOT EXISTS years_experience SMALLINT,
    ADD COLUMN IF NOT EXISTS consultation_fee DECIMAL(10, 2),
    ADD COLUMN IF NOT EXISTS service_area_radius_km DECIMAL(6, 2),
    ADD COLUMN IF NOT EXISTS latitude DECIMAL(10, 7),
    ADD COLUMN IF NOT EXISTS longitude DECIMAL(10, 7),
    ADD COLUMN IF NOT EXISTS city VARCHAR(100),
    ADD COLUMN IF NOT EXISTS address TEXT,
    ADD COLUMN IF NOT EXISTS is_accepting_bookings BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS allow_overbooking BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS availability_mood availability_mood DEFAULT 'AVAILABLE',
    ADD COLUMN IF NOT EXISTS mood_note VARCHAR(200),
    ADD COLUMN IF NOT EXISTS mood_updated_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS badge_tier badge_tier DEFAULT 'NONE',
    ADD COLUMN IF NOT EXISTS badge_awarded_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS badge_reviewed_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS verification_expires_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS reverification_notified BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

-- ============================================================================
-- AppointUnified V1 — Complete Database Schema
-- Migration: V1__initial_schema.sql
-- Features: Core booking + 5 NEW V1 features:
--   1. Smart Slot Recommendation Engine (preference learning)
--   2. Provider Availability Mood Status (live provider context)
--   3. Booking Intent Drafts (save unfinished bookings)
--   4. Appointment Shareable Link / iCal Export
--   5. Provider Business Hours Exceptions (holiday calendar)
-- ============================================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "pg_trgm"; -- For fuzzy search on provider names

-- ─── ENUMS ──────────────────────────────────────────────────────────────────
CREATE TYPE user_role AS ENUM ('PUBLIC', 'PROFESSIONAL', 'ADMIN', 'SUPER_ADMIN');
CREATE TYPE sector AS ENUM ('HEALTHCARE', 'GOVERNMENT', 'SERVICES');
CREATE TYPE verification_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED');
CREATE TYPE appointment_status AS ENUM ('DRAFT', 'SCHEDULED', 'IN_QUEUE', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'NO_SHOW', 'EXPIRED');
CREATE TYPE appointment_priority AS ENUM ('NORMAL', 'PREMIUM', 'EMERGENCY');
CREATE TYPE doc_type AS ENUM ('LICENSE', 'DEGREE', 'ID_PROOF', 'EMPLOYEE_ID', 'EXPERIENCE', 'OTHER');
CREATE TYPE notification_channel AS ENUM ('EMAIL', 'SMS', 'WHATSAPP', 'PUSH');

-- ─── USERS ──────────────────────────────────────────────────────────────────
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) UNIQUE,
    phone           VARCHAR(20) UNIQUE NOT NULL,
    password_hash   TEXT,
    role            user_role NOT NULL DEFAULT 'PUBLIC',
    sector          sector,
    full_name       VARCHAR(255),
    avatar_url      TEXT,
    is_verified     BOOLEAN NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    risk_score      DECIMAL(3,2) NOT NULL DEFAULT 0.00,
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_phone ON users(phone);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);

-- ─── REFRESH TOKENS ─────────────────────────────────────────────────────────
CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  TEXT NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    device_info TEXT,
    ip_address  INET,
    revoked     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash);

-- ─── PROFESSIONALS ──────────────────────────────────────────────────────────
CREATE TABLE professionals (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    display_name            VARCHAR(255) NOT NULL,
    sector                  sector NOT NULL,
    specialty               VARCHAR(255),
    license_number          VARCHAR(100) UNIQUE,
    license_hash            TEXT,
    verification_status     verification_status NOT NULL DEFAULT 'PENDING',
    rating_avg              DECIMAL(3,2) NOT NULL DEFAULT 0.00,
    total_reviews           INTEGER NOT NULL DEFAULT 0,
    total_completed         INTEGER NOT NULL DEFAULT 0,
    avatar_url              TEXT,
    cover_url               TEXT,
    bio                     TEXT,
    qualification           TEXT,
    years_experience        SMALLINT,
    consultation_fee        DECIMAL(10,2),
    service_area_radius_km  DECIMAL(6,2),
    latitude                DECIMAL(10,7),
    longitude               DECIMAL(10,7),
    city                    VARCHAR(100),
    address                 TEXT,
    is_accepting_bookings   BOOLEAN NOT NULL DEFAULT TRUE,
    -- NEW V1 FEATURE: Provider Mood/Status
    availability_mood       VARCHAR(20) DEFAULT 'AVAILABLE'
                                CHECK (availability_mood IN ('AVAILABLE','BUSY','RUNNING_LATE','TAKING_BREAKS','DO_NOT_DISTURB')),
    mood_note               VARCHAR(200),
    mood_updated_at         TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_professionals_sector ON professionals(sector);
CREATE INDEX idx_professionals_verification ON professionals(verification_status);
CREATE INDEX idx_professionals_city ON professionals(city);
CREATE INDEX idx_professionals_name_trgm ON professionals USING gin(display_name gin_trgm_ops);

-- ─── SERVICES ───────────────────────────────────────────────────────────────
CREATE TABLE services (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    professional_id     UUID NOT NULL REFERENCES professionals(id) ON DELETE CASCADE,
    sector              sector NOT NULL,
    name                VARCHAR(255) NOT NULL,
    description         TEXT,
    duration_minutes    INTEGER NOT NULL DEFAULT 30,
    price               DECIMAL(10,2),
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    requires_documents  BOOLEAN NOT NULL DEFAULT FALSE,
    is_virtual          BOOLEAN NOT NULL DEFAULT FALSE,
    max_concurrent      SMALLINT NOT NULL DEFAULT 1,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_services_professional ON services(professional_id);

-- ─── AVAILABILITY (weekly schedule) ────────────────────────────────────────
CREATE TABLE availability (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    professional_id     UUID NOT NULL REFERENCES professionals(id) ON DELETE CASCADE,
    weekday             SMALLINT NOT NULL CHECK (weekday BETWEEN 0 AND 6), -- 0=Mon
    start_time          TIME NOT NULL,
    end_time            TIME NOT NULL,
    buffer_minutes      INTEGER NOT NULL DEFAULT 5,
    slot_duration_mins  INTEGER NOT NULL DEFAULT 30,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(professional_id, weekday)
);

-- NEW V1 FEATURE 5: Provider Business Hours Exceptions (holiday calendar)
CREATE TABLE availability_exceptions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    professional_id     UUID NOT NULL REFERENCES professionals(id) ON DELETE CASCADE,
    exception_date      DATE NOT NULL,
    exception_type      VARCHAR(20) NOT NULL CHECK (exception_type IN ('HOLIDAY','PARTIAL','EXTRA_HOURS')),
    start_time          TIME,  -- null = full day off
    end_time            TIME,
    reason              VARCHAR(255),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(professional_id, exception_date)
);

-- ─── APPOINTMENTS ───────────────────────────────────────────────────────────
CREATE TABLE appointments (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id               UUID NOT NULL REFERENCES users(id),
    professional_id         UUID NOT NULL REFERENCES professionals(id),
    service_id              UUID NOT NULL REFERENCES services(id),
    start_time              TIMESTAMPTZ NOT NULL,
    end_time                TIMESTAMPTZ NOT NULL,
    status                  appointment_status NOT NULL DEFAULT 'SCHEDULED',
    priority                appointment_priority NOT NULL DEFAULT 'NORMAL',
    notes                   TEXT,
    client_notes            TEXT,          -- visible to client
    internal_notes          TEXT,          -- professional-only
    is_virtual              BOOLEAN NOT NULL DEFAULT FALSE,
    meet_link               TEXT,
    cancellation_reason     TEXT,
    cancelled_by            UUID REFERENCES users(id),
    cancelled_at            TIMESTAMPTZ,
    completed_at            TIMESTAMPTZ,
    -- NEW V1 FEATURE 4: Shareable Link
    share_token             VARCHAR(32) UNIQUE DEFAULT encode(gen_random_bytes(16), 'hex'),
    share_expires_at        TIMESTAMPTZ DEFAULT NOW() + INTERVAL '30 days',
    -- Audit
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_appointments_client ON appointments(client_id);
CREATE INDEX idx_appointments_professional ON appointments(professional_id);
CREATE INDEX idx_appointments_start ON appointments(start_time);
CREATE INDEX idx_appointments_status ON appointments(status);
CREATE INDEX idx_appointments_share_token ON appointments(share_token);

-- NEW V1 FEATURE 3: Booking Intent Drafts (save unfinished bookings)
CREATE TABLE booking_drafts (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    professional_id     UUID REFERENCES professionals(id) ON DELETE SET NULL,
    service_id          UUID REFERENCES services(id) ON DELETE SET NULL,
    draft_data          JSONB NOT NULL DEFAULT '{}',  -- selected date, time, notes
    step_reached        SMALLINT NOT NULL DEFAULT 1,  -- which wizard step they stopped at
    expires_at          TIMESTAMPTZ NOT NULL DEFAULT NOW() + INTERVAL '48 hours',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_booking_drafts_user ON booking_drafts(user_id);
CREATE INDEX idx_booking_drafts_expires ON booking_drafts(expires_at);

-- ─── VERIFICATIONS ──────────────────────────────────────────────────────────
CREATE TABLE verifications (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    professional_id     UUID NOT NULL REFERENCES professionals(id) ON DELETE CASCADE,
    doc_type            doc_type NOT NULL,
    doc_url             TEXT NOT NULL,
    doc_hash            TEXT NOT NULL,
    status              verification_status NOT NULL DEFAULT 'PENDING',
    reviewed_by         UUID REFERENCES users(id),
    review_notes        TEXT,
    reviewed_at         TIMESTAMPTZ,
    submitted_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_verifications_professional ON verifications(professional_id);
CREATE INDEX idx_verifications_status ON verifications(status);
CREATE UNIQUE INDEX idx_verifications_doc_hash ON verifications(doc_hash); -- detect duplicates

-- ─── REVIEWS ────────────────────────────────────────────────────────────────
CREATE TABLE reviews (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id      UUID NOT NULL UNIQUE REFERENCES appointments(id),
    reviewer_id         UUID NOT NULL REFERENCES users(id),
    professional_id     UUID NOT NULL REFERENCES professionals(id),
    rating              SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment             TEXT,
    is_flagged          BOOLEAN NOT NULL DEFAULT FALSE,
    flag_reason         TEXT,
    is_visible          BOOLEAN NOT NULL DEFAULT TRUE,
    helpful_count       INTEGER NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reviews_professional ON reviews(professional_id);

-- ─── AUDIT LOGS ─────────────────────────────────────────────────────────────
CREATE TABLE audit_logs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id    UUID REFERENCES users(id),
    action      VARCHAR(100) NOT NULL,
    target_type VARCHAR(50),
    target_id   UUID,
    metadata    JSONB,
    ip_address  INET,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_actor ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_created ON audit_logs(created_at DESC);

-- ─── NOTIFICATION PREFERENCES ───────────────────────────────────────────────
CREATE TABLE notification_preferences (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    email_enabled   BOOLEAN NOT NULL DEFAULT TRUE,
    sms_enabled     BOOLEAN NOT NULL DEFAULT TRUE,
    whatsapp_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    push_enabled    BOOLEAN NOT NULL DEFAULT TRUE,
    reminder_hours  SMALLINT NOT NULL DEFAULT 24, -- hours before appointment
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ─── NOTIFICATION LOG ───────────────────────────────────────────────────────
CREATE TABLE notification_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id),
    appointment_id  UUID REFERENCES appointments(id),
    channel         notification_channel NOT NULL,
    subject         VARCHAR(255),
    body            TEXT,
    sent_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status          VARCHAR(20) NOT NULL DEFAULT 'SENT' CHECK (status IN ('SENT','FAILED','QUEUED'))
);

-- NEW V1 FEATURE 1: Smart Slot Recommendation Engine — User preference learning
CREATE TABLE user_booking_preferences (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    preferred_sector        sector,
    preferred_time_of_day   VARCHAR(20) CHECK (preferred_time_of_day IN ('MORNING','AFTERNOON','EVENING')),
    preferred_weekdays      SMALLINT[] DEFAULT '{}', -- 0-6 array
    preferred_city          VARCHAR(100),
    avg_lead_days           DECIMAL(4,1) DEFAULT 2.0, -- how many days ahead they usually book
    favorite_professionals  UUID[] DEFAULT '{}',
    last_computed_at        TIMESTAMPTZ DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- NEW V1 FEATURE 1: Slot recommendation tracking
CREATE TABLE slot_recommendations (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    professional_id     UUID NOT NULL REFERENCES professionals(id) ON DELETE CASCADE,
    service_id          UUID REFERENCES services(id) ON DELETE SET NULL,
    recommended_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    slot_datetime       TIMESTAMPTZ NOT NULL,
    reason              VARCHAR(100), -- 'PREFERRED_TIME', 'FAVORITE_PROVIDER', 'NEARBY', etc.
    was_booked          BOOLEAN NOT NULL DEFAULT FALSE,
    booked_at           TIMESTAMPTZ
);

CREATE INDEX idx_slot_rec_user ON slot_recommendations(user_id);

-- ─── STORED FUNCTIONS ───────────────────────────────────────────────────────

-- Auto-update updated_at timestamps
CREATE OR REPLACE FUNCTION trigger_set_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER set_timestamp_users
    BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

CREATE TRIGGER set_timestamp_professionals
    BEFORE UPDATE ON professionals FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

CREATE TRIGGER set_timestamp_appointments
    BEFORE UPDATE ON appointments FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

CREATE TRIGGER set_timestamp_services
    BEFORE UPDATE ON services FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

CREATE TRIGGER set_timestamp_booking_drafts
    BEFORE UPDATE ON booking_drafts FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- Recalculate professional rating on review insert/update
CREATE OR REPLACE FUNCTION update_professional_rating()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE professionals
    SET rating_avg = (
        SELECT ROUND(AVG(rating)::numeric, 2)
        FROM reviews
        WHERE professional_id = NEW.professional_id AND is_visible = TRUE
    ),
    total_reviews = (
        SELECT COUNT(*)
        FROM reviews
        WHERE professional_id = NEW.professional_id AND is_visible = TRUE
    )
    WHERE id = NEW.professional_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_rating_on_review
    AFTER INSERT OR UPDATE ON reviews FOR EACH ROW EXECUTE FUNCTION update_professional_rating();

-- ============================================================================
-- AppointUnified V2 — Multi-Sector Trust System
-- Migration: V2__trust_verification_system.sql
--
-- This migration extends V1 with the document verification workflow,
-- OTP verification tracking, and sector-specific credential tables.
-- ============================================================================

-- ─── OTP Verification Log ─────────────────────────────────────────────────
-- Tracks phone OTP sessions (Firebase verifies the actual OTP client-side;
-- this table records successful verifications for audit purposes).
CREATE TABLE otp_verifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID REFERENCES users(id) ON DELETE CASCADE,
    phone           VARCHAR(20) NOT NULL,
    verified        BOOLEAN NOT NULL DEFAULT FALSE,
    firebase_uid    VARCHAR(128),
    verified_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_otp_verifications_user ON otp_verifications(user_id);
CREATE INDEX idx_otp_verifications_phone ON otp_verifications(phone);

-- ─── Healthcare-specific credentials ──────────────────────────────────────
CREATE TABLE healthcare_credentials (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    professional_id     UUID NOT NULL UNIQUE REFERENCES professionals(id) ON DELETE CASCADE,
    nmc_registration    VARCHAR(50),           -- National Medical Commission number
    degree              VARCHAR(50),           -- MBBS, MD, MS, DNB, etc.
    hospital_affiliation TEXT,
    specialization_code VARCHAR(20),
    verified_nmc        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ─── Government-specific credentials ──────────────────────────────────────
CREATE TABLE government_credentials (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    professional_id     UUID NOT NULL UNIQUE REFERENCES professionals(id) ON DELETE CASCADE,
    employee_id         VARCHAR(50) NOT NULL,
    department          VARCHAR(255),
    office_address      TEXT,
    official_email      VARCHAR(255),          -- must end in .gov.in
    verified_employee   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ─── Services-specific credentials ────────────────────────────────────────
CREATE TABLE services_credentials (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    professional_id     UUID NOT NULL UNIQUE REFERENCES professionals(id) ON DELETE CASCADE,
    trade_skill         VARCHAR(100),          -- Plumber, Electrician, etc.
    aadhaar_verified    BOOLEAN NOT NULL DEFAULT FALSE,
    certifications      TEXT[],               -- array of certificate names
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ─── Complaint System ─────────────────────────────────────────────────────
CREATE TABLE complaints (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id         UUID NOT NULL REFERENCES users(id),
    professional_id     UUID NOT NULL REFERENCES professionals(id),
    appointment_id      UUID REFERENCES appointments(id),
    category            VARCHAR(50) NOT NULL,  -- FRAUD, MISCONDUCT, NO_SHOW, OTHER
    description         TEXT NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'OPEN',  -- OPEN, INVESTIGATING, RESOLVED, DISMISSED
    resolved_by         UUID REFERENCES users(id),
    resolution_notes    TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at         TIMESTAMPTZ
);

CREATE INDEX idx_complaints_professional ON complaints(professional_id);
CREATE INDEX idx_complaints_status ON complaints(status);

-- ─── Waitlist (preview for V5) ────────────────────────────────────────────
CREATE TABLE waitlist (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    professional_id     UUID NOT NULL REFERENCES professionals(id) ON DELETE CASCADE,
    service_id          UUID REFERENCES services(id) ON DELETE SET NULL,
    preferred_dates     DATE[] DEFAULT '{}',
    preferred_time_from TIME,
    preferred_time_to   TIME,
    notified            BOOLEAN NOT NULL DEFAULT FALSE,
    notified_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at          TIMESTAMPTZ DEFAULT NOW() + INTERVAL '14 days'
);

CREATE INDEX idx_waitlist_professional ON waitlist(professional_id, notified);

-- ─── Update professionals: add complaint_count ─────────────────────────────
ALTER TABLE professionals ADD COLUMN IF NOT EXISTS complaint_count INTEGER NOT NULL DEFAULT 0;

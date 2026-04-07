-- ============================================================================
-- AppointUnified V2 trust extensions
-- Adds badge tiers, document workflow, reputation scoring, and complaint escalation
-- without mutating already-applied V2 migration.
-- ============================================================================

DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'badge_tier') THEN
    CREATE TYPE badge_tier AS ENUM ('NONE','BRONZE','SILVER','GOLD');
  END IF;
END $$;

ALTER TABLE professionals
    ADD COLUMN IF NOT EXISTS badge_tier badge_tier NOT NULL DEFAULT 'NONE',
    ADD COLUMN IF NOT EXISTS badge_awarded_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS badge_reviewed_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS verification_expires_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS reverification_notified BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS verification_documents (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    professional_id     UUID NOT NULL REFERENCES professionals(id) ON DELETE CASCADE,
    doc_type            VARCHAR(30) NOT NULL,
    doc_url             TEXT NOT NULL,
    doc_hash            TEXT NOT NULL,
    file_size_bytes     INTEGER,
    mime_type           VARCHAR(100),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by         UUID REFERENCES users(id),
    review_notes        TEXT,
    reviewed_at         TIMESTAMPTZ,
    submitted_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_verdocs_professional ON verification_documents(professional_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_verdocs_hash ON verification_documents(doc_hash);

CREATE TABLE IF NOT EXISTS admin_actions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id            UUID NOT NULL REFERENCES users(id),
    action_type         VARCHAR(50) NOT NULL,
    target_professional UUID REFERENCES professionals(id),
    target_document     UUID REFERENCES verification_documents(id),
    notes               TEXT,
    metadata            JSONB,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_admin_actions_professional ON admin_actions(target_professional);

CREATE TABLE IF NOT EXISTS reputation_scores (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    professional_id      UUID NOT NULL UNIQUE REFERENCES professionals(id) ON DELETE CASCADE,
    overall_score        DECIMAL(4,2) NOT NULL DEFAULT 0.00,
    rating_component     DECIMAL(4,2) NOT NULL DEFAULT 0.00,
    completion_component DECIMAL(4,2) NOT NULL DEFAULT 0.00,
    response_component   DECIMAL(4,2) NOT NULL DEFAULT 0.00,
    recency_component    DECIMAL(4,2) NOT NULL DEFAULT 0.00,
    total_appointments   INTEGER NOT NULL DEFAULT 0,
    no_show_rate         DECIMAL(5,4) NOT NULL DEFAULT 0.0000,
    cancellation_rate    DECIMAL(5,4) NOT NULL DEFAULT 0.0000,
    avg_response_hours   DECIMAL(6,2),
    last_computed_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE complaints
    ADD COLUMN IF NOT EXISTS evidence_urls TEXT[] DEFAULT '{}',
    ADD COLUMN IF NOT EXISTS priority VARCHAR(10) NOT NULL DEFAULT 'NORMAL',
    ADD COLUMN IF NOT EXISTS assigned_to UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

CREATE TABLE IF NOT EXISTS complaint_escalations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    complaint_id    UUID NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    escalated_by    UUID NOT NULL REFERENCES users(id),
    from_status     VARCHAR(20),
    to_status       VARCHAR(20),
    reason          TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE OR REPLACE FUNCTION update_complaint_timestamp()
RETURNS TRIGGER AS $$
BEGIN NEW.updated_at = NOW(); RETURN NEW; END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS set_complaint_timestamp ON complaints;
CREATE TRIGGER set_complaint_timestamp
    BEFORE UPDATE ON complaints
    FOR EACH ROW EXECUTE FUNCTION update_complaint_timestamp();

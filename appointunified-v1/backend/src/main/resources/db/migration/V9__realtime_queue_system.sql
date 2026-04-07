-- ============================================================================
-- AppointUnified V3 — Real-Time Queue System
-- Migration: V3__realtime_queue_system.sql
--
-- NEW V3 FEATURE 1: Queue Analytics Snapshots
-- NEW V3 FEATURE 2: Queue Pause Log
-- NEW V3 FEATURE 3: Client Queue Preferences
-- NEW V3 FEATURE 4: Emergency Slot Registry (compliance audit)
-- NEW V3 FEATURE 5: Queue Broadcast Messages
-- ============================================================================

-- Core: Queue Tokens
CREATE TABLE IF NOT EXISTS queue_tokens (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id      UUID NOT NULL UNIQUE REFERENCES appointments(id) ON DELETE CASCADE,
    professional_id     UUID NOT NULL REFERENCES professionals(id) ON DELETE CASCADE,
    token_number        INTEGER NOT NULL,
    position            INTEGER NOT NULL,
    estimated_wait_mins INTEGER,
    status              VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    called_at           TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ,
    skipped_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(professional_id, token_number)
);
CREATE INDEX IF NOT EXISTS idx_queue_tokens_professional ON queue_tokens(professional_id, status);
CREATE INDEX IF NOT EXISTS idx_queue_tokens_appointment  ON queue_tokens(appointment_id);

-- Core: Queue Events (append-only event log)
CREATE TABLE IF NOT EXISTS queue_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    professional_id UUID NOT NULL REFERENCES professionals(id) ON DELETE CASCADE,
    event_type      VARCHAR(30) NOT NULL,
    token_id        UUID REFERENCES queue_tokens(id),
    triggered_by    UUID REFERENCES users(id),
    payload         JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_queue_events_professional ON queue_events(professional_id, created_at DESC);

-- Core: Delay Log
CREATE TABLE IF NOT EXISTS delay_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    professional_id UUID NOT NULL REFERENCES professionals(id) ON DELETE CASCADE,
    delay_minutes   INTEGER NOT NULL,
    reason          VARCHAR(255),
    tokens_affected INTEGER NOT NULL DEFAULT 0,
    triggered_by    UUID REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_delay_log_professional ON delay_log(professional_id, created_at DESC);

-- NEW V3 FEATURE 1: Queue Analytics Snapshots (every 5 min)
CREATE TABLE IF NOT EXISTS queue_analytics_snapshots (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    professional_id       UUID NOT NULL REFERENCES professionals(id) ON DELETE CASCADE,
    snapshot_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    queue_length          INTEGER NOT NULL DEFAULT 0,
    current_wait_mins     INTEGER NOT NULL DEFAULT 0,
    total_served_today    INTEGER NOT NULL DEFAULT 0,
    no_shows_today        INTEGER NOT NULL DEFAULT 0,
    avg_service_mins      DECIMAL(6,2),
    is_paused             BOOLEAN NOT NULL DEFAULT FALSE,
    cumulative_delay_mins INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_queue_snapshots ON queue_analytics_snapshots(professional_id, snapshot_at DESC);

-- NEW V3 FEATURE 2: Queue Pause Log
CREATE TABLE IF NOT EXISTS queue_pause_log (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    professional_id     UUID NOT NULL REFERENCES professionals(id) ON DELETE CASCADE,
    reason              VARCHAR(255),
    paused_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resumed_at          TIMESTAMPTZ,
    pause_duration_secs INTEGER,
    triggered_by        UUID REFERENCES users(id)
);
CREATE INDEX IF NOT EXISTS idx_queue_pause ON queue_pause_log(professional_id);

-- NEW V3 FEATURE 3: Client Queue Preferences
CREATE TABLE IF NOT EXISTS client_queue_preferences (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    notify_at_position   INTEGER NOT NULL DEFAULT 3,
    prefer_sms_over_push BOOLEAN NOT NULL DEFAULT FALSE,
    auto_check_in_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    show_realtime_eta    BOOLEAN NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- NEW V3 FEATURE 4: Emergency Slot Registry
CREATE TABLE IF NOT EXISTS emergency_slot_registry (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id  UUID NOT NULL REFERENCES appointments(id),
    professional_id UUID NOT NULL REFERENCES professionals(id),
    inserted_by     UUID NOT NULL REFERENCES users(id),
    justification   TEXT NOT NULL,
    bumped_tokens   TEXT DEFAULT '{}',
    inserted_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_emergency_professional ON emergency_slot_registry(professional_id, inserted_at DESC);

-- NEW V3 FEATURE 5: Queue Broadcast Messages
CREATE TABLE IF NOT EXISTS queue_broadcasts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    professional_id UUID NOT NULL REFERENCES professionals(id) ON DELETE CASCADE,
    message         TEXT NOT NULL,
    message_type    VARCHAR(20) NOT NULL DEFAULT 'INFO',
    sent_to_count   INTEGER NOT NULL DEFAULT 0,
    sent_by         UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_broadcasts_professional ON queue_broadcasts(professional_id, created_at DESC);

-- Trigger: reorder positions after status change
CREATE OR REPLACE FUNCTION reorder_queue_positions()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE queue_tokens
    SET position = subquery.new_pos
    FROM (
        SELECT id, ROW_NUMBER() OVER (ORDER BY token_number) AS new_pos
        FROM queue_tokens
        WHERE professional_id = NEW.professional_id AND status = 'WAITING'
    ) AS subquery
    WHERE queue_tokens.id = subquery.id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS after_token_status_change ON queue_tokens;
CREATE TRIGGER after_token_status_change
    AFTER UPDATE OF status ON queue_tokens
    FOR EACH ROW
    WHEN (NEW.status != 'WAITING')
    EXECUTE FUNCTION reorder_queue_positions();

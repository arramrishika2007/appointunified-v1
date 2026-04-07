CREATE TABLE IF NOT EXISTS behavior_scores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    score DECIMAL(3,2) NOT NULL DEFAULT 1.00,
    total_cancellations INTEGER NOT NULL DEFAULT 0,
    last_minute_cancellations INTEGER NOT NULL DEFAULT 0,
    no_shows INTEGER NOT NULL DEFAULT 0,
    completions INTEGER NOT NULL DEFAULT 0,
    last_calculated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS cancellation_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id UUID NOT NULL REFERENCES appointments(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    cancelled_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    hours_before_appointment DECIMAL(6,2),
    reason TEXT,
    penalty_applied DECIMAL(4,2) NOT NULL DEFAULT 0.00
);

CREATE INDEX IF NOT EXISTS idx_behavior_scores_user ON behavior_scores(user_id);
CREATE INDEX IF NOT EXISTS idx_cancellation_history_user ON cancellation_history(user_id, cancelled_at DESC);

ALTER TABLE appointments
    ADD COLUMN IF NOT EXISTS booking_type VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED';

ALTER TABLE professionals
    ADD COLUMN IF NOT EXISTS allow_overbooking BOOLEAN NOT NULL DEFAULT FALSE;

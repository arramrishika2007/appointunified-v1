-- V13 — Priority Engine (SLA, Fairness, Premium Tiers)

-- Priority configuration per professional
CREATE TABLE priority_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    professional_id UUID NOT NULL REFERENCES professionals(id) ON DELETE CASCADE,
    max_premium_ratio DECIMAL(3, 2) DEFAULT 0.4,
    emergency_fee_multiplier DECIMAL(3, 2) DEFAULT 0.0,
    premium_fee_multiplier DECIMAL(3, 2) DEFAULT 0.3,
    sla_warn_minutes INTEGER DEFAULT 15,
    sla_escalate_minutes INTEGER DEFAULT 30,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(professional_id)
);

-- SLA configuration per sector
CREATE TABLE sla_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sector VARCHAR(50) NOT NULL,
    priority_tier VARCHAR(50) NOT NULL,
    max_wait_minutes INTEGER NOT NULL,
    escalation_target VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(sector, priority_tier)
);

-- Track SLA breaches
CREATE TABLE sla_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id UUID NOT NULL REFERENCES appointments(id) ON DELETE CASCADE,
    breach_type VARCHAR(50) NOT NULL,
    breached_at TIMESTAMPTZ DEFAULT NOW(),
    resolved_at TIMESTAMPTZ,
    escalated_to UUID REFERENCES users(id),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    INDEX idx_appointment_sla (appointment_id),
    INDEX idx_breach_time (breached_at DESC),
    INDEX idx_escalation (escalated_to)
);

-- Add premium_fee column to track priority booking fees
ALTER TABLE appointments
    ADD COLUMN IF NOT EXISTS premium_fee DECIMAL(10, 2) DEFAULT 0;

CREATE INDEX idx_priority_rules_professional ON priority_rules(professional_id);
CREATE INDEX idx_sla_configs_sector ON sla_configs(sector);
CREATE INDEX idx_appointments_priority ON appointments(priority);

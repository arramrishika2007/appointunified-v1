-- V6 workflow engine

CREATE TABLE IF NOT EXISTS workflows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    sector VARCHAR(30) NOT NULL,
    description TEXT,
    steps JSONB NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS workflow_instances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID NOT NULL REFERENCES workflows(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    current_step INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS workflow_step_appointments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instance_id UUID NOT NULL REFERENCES workflow_instances(id) ON DELETE CASCADE,
    step_order INTEGER NOT NULL,
    appointment_id UUID NOT NULL REFERENCES appointments(id) ON DELETE CASCADE,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (instance_id, step_order)
);

ALTER TABLE appointments
    ADD COLUMN IF NOT EXISTS workflow_instance_id UUID REFERENCES workflow_instances(id);

CREATE INDEX IF NOT EXISTS idx_workflows_sector_active ON workflows(sector, is_active);
CREATE INDEX IF NOT EXISTS idx_workflow_instances_user_status ON workflow_instances(user_id, status);
CREATE INDEX IF NOT EXISTS idx_workflow_step_appointments_instance ON workflow_step_appointments(instance_id, step_order);
CREATE INDEX IF NOT EXISTS idx_workflow_step_appointments_appointment ON workflow_step_appointments(appointment_id);

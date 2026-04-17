-- Add missing appointment payment/status columns required by JPA validation
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'payment_status') THEN
        CREATE TYPE payment_status AS ENUM ('PENDING', 'PAID', 'FAILED', 'REFUNDED');
    END IF;
END $$;

ALTER TABLE appointments
    ADD COLUMN IF NOT EXISTS deposit_status payment_status DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS final_payment_status payment_status DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS premium_fee DECIMAL(10, 2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS user_notes TEXT,
    ADD COLUMN IF NOT EXISTS cancellation_reason TEXT,
    ADD COLUMN IF NOT EXISTS workflow_instance_id UUID,
    ADD COLUMN IF NOT EXISTS total_amount DECIMAL(10, 2);

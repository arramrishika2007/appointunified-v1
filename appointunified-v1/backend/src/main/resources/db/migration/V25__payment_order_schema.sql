-- ============================================================================
-- AppointUnified V1
-- Migration: V25__payment_order_schema.sql
-- Description: Create payment_orders table for tracking deposit and balance payments
-- ============================================================================

CREATE TABLE payment_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id UUID NOT NULL REFERENCES appointments(id),
    user_id UUID NOT NULL REFERENCES users(id),
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    payment_type VARCHAR(20) NOT NULL, -- DEPOSIT, BALANCE
    gateway_order_id VARCHAR(100),
    gateway_payment_id VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payment_orders_appointment ON payment_orders(appointment_id);
CREATE INDEX idx_payment_orders_user ON payment_orders(user_id);
CREATE INDEX idx_payment_orders_gateway_order ON payment_orders(gateway_order_id);

CREATE TRIGGER set_timestamp_payment_orders
    BEFORE UPDATE ON payment_orders FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

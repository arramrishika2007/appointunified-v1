-- ============================================================================
-- AppointUnified V1
-- Migration: V24__appointment_status_online_booking.sql
-- Description: Add online booking payment and meeting states to appointment_status
-- ============================================================================

ALTER TYPE appointment_status ADD VALUE IF NOT EXISTS 'PENDING_DEPOSIT';
ALTER TYPE appointment_status ADD VALUE IF NOT EXISTS 'DEPOSIT_PAID';
ALTER TYPE appointment_status ADD VALUE IF NOT EXISTS 'CONFIRMED';
ALTER TYPE appointment_status ADD VALUE IF NOT EXISTS 'IN_MEETING';
ALTER TYPE appointment_status ADD VALUE IF NOT EXISTS 'PENDING_BALANCE';
ALTER TYPE appointment_status ADD VALUE IF NOT EXISTS 'PAID_FULL';

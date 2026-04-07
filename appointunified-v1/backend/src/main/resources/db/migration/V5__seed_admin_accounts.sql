-- ============================================================================
-- Seed admin accounts used by the landing-page admin gate
-- Password for both accounts: "password123"
-- This migration keeps the seeded credentials available on every environment
-- that applies Flyway migrations.
-- ============================================================================

INSERT INTO users (email, phone, password_hash, role, full_name, is_verified, is_active, created_at, updated_at)
VALUES
  ('admin@test.com',     '+919876543005', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj2oEmbThvAi', 'ADMIN',       'Admin User',       TRUE, TRUE, NOW(), NOW()),
  ('superadmin@test.com', '+919876543006', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj2oEmbThvAi', 'SUPER_ADMIN', 'Super Admin User', TRUE, TRUE, NOW(), NOW())
ON CONFLICT (email) DO UPDATE
SET
  phone = EXCLUDED.phone,
  password_hash = EXCLUDED.password_hash,
  role = EXCLUDED.role,
  full_name = EXCLUDED.full_name,
  is_verified = EXCLUDED.is_verified,
  is_active = EXCLUDED.is_active,
  updated_at = NOW();
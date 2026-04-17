-- Add missing geo-location columns expected by Appointment entity
ALTER TABLE appointments
    ADD COLUMN IF NOT EXISTS client_lat DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS client_lon DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS distance_meters INTEGER;

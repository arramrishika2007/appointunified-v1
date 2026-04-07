-- ============================================================================
-- ─── Test Users ──────────────────────────────────────────────────────────────
-- Password for all seed users: "password123"
-- Hash: bcrypt $2a$12$... (pre-computed for "password123")
INSERT INTO users (id, email, phone, password_hash, role, full_name, is_verified, is_active) VALUES
  ('a1000000-0000-0000-0000-000000000001', 'patient@test.com',   '+919876543001', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj2oEmbThvAi', 'PUBLIC',       'Priya Sharma',       TRUE, TRUE),
  ('a1000000-0000-0000-0000-000000000002', 'doctor@test.com',    '+919876543002', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj2oEmbThvAi', 'PROFESSIONAL', 'Dr. Rajesh Kumar',   TRUE, TRUE),
  ('a1000000-0000-0000-0000-000000000003', 'officer@test.com',   '+919876543003', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj2oEmbThvAi', 'PROFESSIONAL', 'Officer Meena Reddy', TRUE, TRUE),
  ('a1000000-0000-0000-0000-000000000004', 'plumber@test.com',   '+919876543004', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj2oEmbThvAi', 'PROFESSIONAL', 'Ramesh Yadav',        TRUE, TRUE),
  ('a1000000-0000-0000-0000-000000000005', 'admin@test.com',     '+919876543005', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj2oEmbThvAi', 'ADMIN',        'Admin User',          TRUE, TRUE),
  ('a1000000-0000-0000-0000-000000000006', 'superadmin@test.com', '+919876543006', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj2oEmbThvAi', 'SUPER_ADMIN',  'Super Admin User',    TRUE, TRUE)
ON CONFLICT (id) DO NOTHING;

-- ─── Healthcare Professional ──────────────────────────────────────────────────
INSERT INTO professionals (
  id, user_id, display_name, sector, specialty,
  license_number, verification_status, rating_avg, total_reviews, total_completed,
  bio, qualification, years_experience, consultation_fee,
  city, address, is_accepting_bookings, availability_mood
) VALUES (
  'b1000000-0000-0000-0000-000000000001',
  'a1000000-0000-0000-0000-000000000002',
  'Dr. Rajesh Kumar',
  'HEALTHCARE', 'Cardiologist',
  'NMC-2024-KAR-001', 'APPROVED', 4.8, 127, 892,
  'Senior Cardiologist with 15 years of experience at Apollo Hospitals. Specialising in interventional cardiology, heart failure management, and preventive cardiology.',
  'MBBS (AIIMS Delhi), MD Cardiology (PGIMER Chandigarh), Fellowship in Interventional Cardiology (UK)',
  15, 800.00,
  'Bangalore', 'Apollo Hospitals, Bannerghatta Road, Bangalore - 560076',
  TRUE, 'AVAILABLE'
) ON CONFLICT (id) DO NOTHING;

-- ─── Government Professional ──────────────────────────────────────────────────
INSERT INTO professionals (
  id, user_id, display_name, sector, specialty,
  license_number, verification_status, rating_avg, total_reviews, total_completed,
  bio, years_experience, consultation_fee,
  city, address, is_accepting_bookings, availability_mood, mood_note
) VALUES (
  'b1000000-0000-0000-0000-000000000002',
  'a1000000-0000-0000-0000-000000000003',
  'Officer Meena Reddy',
  'GOVERNMENT', 'RTO Officer — Driving Licences',
  'KA-RTO-2019-004', 'APPROVED', 4.2, 89, 340,
  'Senior RTO Officer handling DL renewals, vehicle registrations, and fitness certificates for the Bangalore East division.',
  8, 0.00,
  'Bangalore', 'RTO Bangalore East, Indiranagar, Bangalore - 560038',
  TRUE, 'RUNNING_LATE', 'Running approximately 20 minutes late today due to server maintenance'
) ON CONFLICT (id) DO NOTHING;

-- ─── Service Professional ─────────────────────────────────────────────────────
INSERT INTO professionals (
  id, user_id, display_name, sector, specialty,
  license_number, verification_status, rating_avg, total_reviews, total_completed,
  bio, years_experience, consultation_fee,
  city, address, is_accepting_bookings, availability_mood,
  service_area_radius_km
) VALUES (
  'b1000000-0000-0000-0000-000000000003',
  'a1000000-0000-0000-0000-000000000004',
  'Ramesh Yadav',
  'SERVICES', 'Master Plumber & Electrician',
  'KA-TRADE-PLUMB-2018', 'APPROVED', 4.6, 203, 1450,
  'Certified master plumber and electrician with 12 years of experience. Specialising in bathroom renovations, leak repairs, electrical rewiring, and AC servicing.',
  12, 0.00,
  'Bangalore', 'Marathahalli, Bangalore - 560037',
  TRUE, 'AVAILABLE',
  15.0
) ON CONFLICT (id) DO NOTHING;

-- ─── Availability Schedules ────────────────────────────────────────────────────
-- Dr. Rajesh: Mon-Sat 9 AM-5 PM, 30 min slots
INSERT INTO availability (professional_id, weekday, start_time, end_time, buffer_minutes, slot_duration_mins, is_active)
SELECT 'b1000000-0000-0000-0000-000000000001', d, '09:00', '17:00', 10, 30, TRUE
FROM generate_series(0, 5) AS d
ON CONFLICT (professional_id, weekday) DO NOTHING;

-- Officer Meena: Mon-Fri 10 AM-4 PM, 20 min slots
INSERT INTO availability (professional_id, weekday, start_time, end_time, buffer_minutes, slot_duration_mins, is_active)
SELECT 'b1000000-0000-0000-0000-000000000002', d, '10:00', '16:00', 5, 20, TRUE
FROM generate_series(0, 4) AS d
ON CONFLICT (professional_id, weekday) DO NOTHING;

-- Ramesh: Mon-Sun 8 AM-7 PM, 60 min slots (home service)
INSERT INTO availability (professional_id, weekday, start_time, end_time, buffer_minutes, slot_duration_mins, is_active)
SELECT 'b1000000-0000-0000-0000-000000000003', d, '08:00', '19:00', 30, 60, TRUE
FROM generate_series(0, 6) AS d
ON CONFLICT (professional_id, weekday) DO NOTHING;

-- ─── Services ──────────────────────────────────────────────────────────────────
INSERT INTO services (id, professional_id, sector, name, description, duration_minutes, price, is_active)
VALUES
  -- Healthcare services
  (gen_random_uuid(), 'b1000000-0000-0000-0000-000000000001', 'HEALTHCARE', 'General Cardiology Consultation', 'Initial consultation including ECG interpretation, history taking, and treatment plan', 30, 800.00, TRUE),
  (gen_random_uuid(), 'b1000000-0000-0000-0000-000000000001', 'HEALTHCARE', 'Follow-up Consultation', 'Review of ongoing treatment, medication adjustment, test result discussion', 20, 500.00, TRUE),
  (gen_random_uuid(), 'b1000000-0000-0000-0000-000000000001', 'HEALTHCARE', 'Stress Test Evaluation', 'Treadmill stress test with cardiologist monitoring and immediate report', 60, 2500.00, TRUE),
  -- Government services
  (gen_random_uuid(), 'b1000000-0000-0000-0000-000000000002', 'GOVERNMENT', 'DL Renewal', 'Driving licence renewal with verification and document processing', 20, 0.00, TRUE),
  (gen_random_uuid(), 'b1000000-0000-0000-0000-000000000002', 'GOVERNMENT', 'Vehicle Registration Transfer', 'Transfer of vehicle ownership with complete documentation', 20, 0.00, TRUE),
  -- Services
  (gen_random_uuid(), 'b1000000-0000-0000-0000-000000000003', 'SERVICES', 'Plumbing Inspection & Repair', 'Full home plumbing inspection, leak detection, and on-the-spot minor repairs', 60, 699.00, TRUE),
  (gen_random_uuid(), 'b1000000-0000-0000-0000-000000000003', 'SERVICES', 'Bathroom Renovation Consultation', 'Site visit for bathroom renovation planning, material advice, and quotation', 60, 0.00, TRUE),
  (gen_random_uuid(), 'b1000000-0000-0000-0000-000000000003', 'SERVICES', 'Electrical Safety Audit', 'Complete home electrical wiring inspection, switchboard check, and safety report', 90, 999.00, TRUE)
ON CONFLICT DO NOTHING;



-- flyway:executeInTransaction=false
ALTER TYPE payment_status ADD VALUE IF NOT EXISTS 'CONFIRMED';
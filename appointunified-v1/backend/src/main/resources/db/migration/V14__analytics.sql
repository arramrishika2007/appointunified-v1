-- V14__analytics.sql
-- Analytics Intelligence (V9) schema

CREATE TABLE analytics_snapshots (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  snapshot_date DATE NOT NULL,
  sector VARCHAR(50),  -- 'HEALTHCARE','GOVERNMENT','SERVICES' or null for platform-wide
  total_bookings INTEGER DEFAULT 0,
  completed_bookings INTEGER DEFAULT 0,
  cancelled_bookings INTEGER DEFAULT 0,
  no_shows INTEGER DEFAULT 0,
  total_revenue DECIMAL(12,2) DEFAULT 0.0,
  new_users INTEGER DEFAULT 0,
  new_professionals INTEGER DEFAULT 0,
  average_no_show_rate DECIMAL(5,2) DEFAULT 0.0,
  average_completion_rate DECIMAL(5,2) DEFAULT 0.0,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE(snapshot_date, sector)
);

CREATE TABLE report_exports (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  requested_by UUID REFERENCES users(id),
  export_type VARCHAR(50) NOT NULL,  -- 'BOOKINGS','NO_SHOW_REPORT','AUDIT_LOG','VERIFICATION_REPORT'
  file_name VARCHAR(255) NOT NULL,
  file_url TEXT,
  file_hash VARCHAR(64),
  status VARCHAR(20) DEFAULT 'GENERATING',  -- 'GENERATING','READY','FAILED','EXPIRED'
  file_size_bytes BIGINT,
  error_message TEXT,
  date_filter_start DATE,
  date_filter_end DATE,
  sector_filter VARCHAR(50),
  created_at TIMESTAMPTZ DEFAULT NOW(),
  expires_at TIMESTAMPTZ DEFAULT NOW() + INTERVAL '7 days'
);

CREATE TABLE ai_suggestions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  professional_id UUID REFERENCES professionals(id),
  suggestion_title VARCHAR(255) NOT NULL,
  suggestion_description TEXT NOT NULL,
  expected_impact VARCHAR(100),  -- 'HIGH','MEDIUM','LOW'
  suggestion_data JSONB,  -- stores any supporting data from Groq
  generated_at TIMESTAMPTZ DEFAULT NOW(),
  generated_by_model VARCHAR(100) DEFAULT 'llama3-70b',
  is_active BOOLEAN DEFAULT TRUE
);

-- Indexes for performance
CREATE INDEX idx_analytics_snapshots_date ON analytics_snapshots(snapshot_date DESC);
CREATE INDEX idx_analytics_snapshots_sector ON analytics_snapshots(sector);
CREATE INDEX idx_report_exports_requested_by ON report_exports(requested_by);
CREATE INDEX idx_report_exports_status ON report_exports(status);
CREATE INDEX idx_ai_suggestions_professional ON ai_suggestions(professional_id);
CREATE INDEX idx_ai_suggestions_created ON ai_suggestions(generated_at DESC);

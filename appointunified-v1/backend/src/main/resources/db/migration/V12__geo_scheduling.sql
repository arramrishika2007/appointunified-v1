-- V7 geo scheduling

CREATE TABLE IF NOT EXISTS route_cache (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    from_hash VARCHAR(64) NOT NULL,
    to_hash VARCHAR(64) NOT NULL,
    distance_km DECIMAL(8,2) NOT NULL,
    duration_minutes INTEGER NOT NULL,
    cached_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (from_hash, to_hash)
);

CREATE TABLE IF NOT EXISTS geo_zones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    professional_id UUID NOT NULL REFERENCES professionals(id) ON DELETE CASCADE,
    center_lat DECIMAL(9,6) NOT NULL,
    center_lng DECIMAL(9,6) NOT NULL,
    radius_km DECIMAL(6,2) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_geo_zones_professional_active
    ON geo_zones(professional_id, is_active);

CREATE INDEX IF NOT EXISTS idx_route_cache_lookup
    ON route_cache(from_hash, to_hash);

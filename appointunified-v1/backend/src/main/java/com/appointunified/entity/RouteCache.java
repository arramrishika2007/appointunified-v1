package com.appointunified.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "route_cache")
public class RouteCache {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "from_hash", nullable = false, length = 64)
    private String fromHash;

    @Column(name = "to_hash", nullable = false, length = 64)
    private String toHash;

    @Column(name = "distance_km", nullable = false, precision = 8, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "cached_at", nullable = false)
    private OffsetDateTime cachedAt = OffsetDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getFromHash() { return fromHash; }
    public void setFromHash(String fromHash) { this.fromHash = fromHash; }
    public String getToHash() { return toHash; }
    public void setToHash(String toHash) { this.toHash = toHash; }
    public BigDecimal getDistanceKm() { return distanceKm; }
    public void setDistanceKm(BigDecimal distanceKm) { this.distanceKm = distanceKm; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public OffsetDateTime getCachedAt() { return cachedAt; }
    public void setCachedAt(OffsetDateTime cachedAt) { this.cachedAt = cachedAt; }
}

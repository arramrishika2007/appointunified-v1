package com.appointunified.repository;

import com.appointunified.entity.RouteCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RouteCacheRepository extends JpaRepository<RouteCache, UUID> {
    Optional<RouteCache> findByFromHashAndToHash(String fromHash, String toHash);
    void deleteByCachedAtBefore(OffsetDateTime threshold);
}

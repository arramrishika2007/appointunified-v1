package com.appointunified.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "behavior_scores")
public class BehaviorScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal score = BigDecimal.ONE;

    @Column(name = "total_cancellations", nullable = false)
    private int totalCancellations = 0;

    @Column(name = "last_minute_cancellations", nullable = false)
    private int lastMinuteCancellations = 0;

    @Column(name = "no_shows", nullable = false)
    private int noShows = 0;

    @Column(name = "completions", nullable = false)
    private int completions = 0;

    @Column(name = "last_calculated_at", nullable = false)
    private OffsetDateTime lastCalculatedAt = OffsetDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public int getTotalCancellations() { return totalCancellations; }
    public void setTotalCancellations(int totalCancellations) { this.totalCancellations = totalCancellations; }
    public int getLastMinuteCancellations() { return lastMinuteCancellations; }
    public void setLastMinuteCancellations(int lastMinuteCancellations) { this.lastMinuteCancellations = lastMinuteCancellations; }
    public int getNoShows() { return noShows; }
    public void setNoShows(int noShows) { this.noShows = noShows; }
    public int getCompletions() { return completions; }
    public void setCompletions(int completions) { this.completions = completions; }
    public OffsetDateTime getLastCalculatedAt() { return lastCalculatedAt; }
    public void setLastCalculatedAt(OffsetDateTime lastCalculatedAt) { this.lastCalculatedAt = lastCalculatedAt; }
}

package com.appointunified.service;

import com.appointunified.dto.response.BehaviorResponse;
import com.appointunified.entity.Appointment;
import com.appointunified.entity.BehaviorScore;
import com.appointunified.entity.CancellationHistory;
import com.appointunified.entity.User;
import com.appointunified.exception.AppException;
import com.appointunified.repository.BehaviorScoreRepository;
import com.appointunified.repository.CancellationHistoryRepository;
import com.appointunified.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BehaviorScoringService {

    private static final BigDecimal CANCELLATION_PENALTY = new BigDecimal("0.15");
    private static final BigDecimal LATE_CANCELLATION_PENALTY = new BigDecimal("0.10");
    private static final BigDecimal NO_SHOW_PENALTY = new BigDecimal("0.25");
    private static final BigDecimal COMPLETION_BONUS = new BigDecimal("0.05");

    private final BehaviorScoreRepository behaviorScoreRepository;
    private final CancellationHistoryRepository cancellationHistoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public void registerCancellation(Appointment appointment, String reason) {
        User user = appointment.getClient();
        BehaviorScore score = getOrCreate(user);

        double hoursBefore = Duration.between(OffsetDateTime.now(), appointment.getStartTime()).toMinutes() / 60.0;
        boolean within24h = hoursBefore <= 24;
        boolean late = hoursBefore <= 6;

        score.setTotalCancellations(score.getTotalCancellations() + 1);
        if (late) {
            score.setLastMinuteCancellations(score.getLastMinuteCancellations() + 1);
        }

        BigDecimal penaltyApplied = BigDecimal.ZERO;
        if (within24h) {
            penaltyApplied = penaltyApplied.add(CANCELLATION_PENALTY);
        }
        if (late) {
            penaltyApplied = penaltyApplied.add(LATE_CANCELLATION_PENALTY);
        }

        CancellationHistory history = new CancellationHistory();
        history.setAppointment(appointment);
        history.setUser(user);
        history.setCancelledAt(OffsetDateTime.now());
        history.setHoursBeforeAppointment(BigDecimal.valueOf(hoursBefore).setScale(2, RoundingMode.HALF_UP));
        history.setReason(reason);
        history.setPenaltyApplied(penaltyApplied);
        cancellationHistoryRepository.save(history);

        recalculateAndPersist(score, user);
    }

    @Transactional
    public void registerNoShow(Appointment appointment) {
        User user = appointment.getClient();
        BehaviorScore score = getOrCreate(user);
        score.setNoShows(score.getNoShows() + 1);
        recalculateAndPersist(score, user);
    }

    @Transactional
    public void registerCompletion(Appointment appointment) {
        User user = appointment.getClient();
        BehaviorScore score = getOrCreate(user);
        score.setCompletions(score.getCompletions() + 1);
        recalculateAndPersist(score, user);
    }

    @Transactional(readOnly = true)
    public BehaviorResponse.RiskScore getRiskScore(UUID userId) {
        BehaviorScore score = behaviorScoreRepository.findByUserId(userId)
                .orElseGet(() -> {
                    BehaviorScore fallback = new BehaviorScore();
                    fallback.setUser(userRepository.findById(userId)
                            .orElseThrow(() -> AppException.notFound("User not found")));
                    fallback.setScore(BigDecimal.ONE);
                    fallback.setLastCalculatedAt(OffsetDateTime.now());
                    return fallback;
                });

        BehaviorResponse.RiskScore response = new BehaviorResponse.RiskScore();
        response.setUserId(userId);
        response.setScore(score.getScore());
        response.setRiskLevel(toRiskLevel(score.getScore()));
        return response;
    }

    @Transactional(readOnly = true)
    public BehaviorResponse.RiskSummary getRiskSummary(UUID userId) {
        BehaviorScore score = behaviorScoreRepository.findByUserId(userId)
                .orElseGet(() -> {
                    BehaviorScore fallback = new BehaviorScore();
                    fallback.setUser(userRepository.findById(userId)
                            .orElseThrow(() -> AppException.notFound("User not found")));
                    fallback.setScore(BigDecimal.ONE);
                    fallback.setLastCalculatedAt(OffsetDateTime.now());
                    return fallback;
                });

        BehaviorResponse.RiskSummary summary = new BehaviorResponse.RiskSummary();
        summary.setUserId(userId);
        summary.setScore(score.getScore());
        summary.setRiskLevel(toRiskLevel(score.getScore()));
        summary.setTotalCancellations(score.getTotalCancellations());
        summary.setLastMinuteCancellations(score.getLastMinuteCancellations());
        summary.setNoShows(score.getNoShows());
        summary.setCompletions(score.getCompletions());
        summary.setLastCalculatedAt(score.getLastCalculatedAt());
        return summary;
    }

    private BehaviorScore getOrCreate(User user) {
        return behaviorScoreRepository.findByUserId(user.getId()).orElseGet(() -> {
            BehaviorScore initial = new BehaviorScore();
            initial.setUser(user);
            initial.setScore(BigDecimal.ONE);
            initial.setLastCalculatedAt(OffsetDateTime.now());
            return behaviorScoreRepository.save(initial);
        });
    }

    private void recalculateAndPersist(BehaviorScore score, User user) {
        BigDecimal computed = BigDecimal.ONE
                .subtract(CANCELLATION_PENALTY.multiply(BigDecimal.valueOf(score.getTotalCancellations())))
                .subtract(LATE_CANCELLATION_PENALTY.multiply(BigDecimal.valueOf(score.getLastMinuteCancellations())))
                .subtract(NO_SHOW_PENALTY.multiply(BigDecimal.valueOf(score.getNoShows())))
                .add(COMPLETION_BONUS.multiply(BigDecimal.valueOf(score.getCompletions())));

        if (computed.compareTo(BigDecimal.ZERO) < 0) {
            computed = BigDecimal.ZERO;
        }
        if (computed.compareTo(BigDecimal.ONE) > 0) {
            computed = BigDecimal.ONE;
        }

        score.setScore(computed.setScale(2, RoundingMode.HALF_UP));
        score.setLastCalculatedAt(OffsetDateTime.now());
        behaviorScoreRepository.save(score);

        user.setRiskScore(score.getScore());
        userRepository.save(user);
    }

    private String toRiskLevel(BigDecimal score) {
        if (score.compareTo(new BigDecimal("0.40")) < 0) {
            return "HIGH";
        }
        if (score.compareTo(new BigDecimal("0.60")) < 0) {
            return "MEDIUM";
        }
        return "LOW";
    }
}

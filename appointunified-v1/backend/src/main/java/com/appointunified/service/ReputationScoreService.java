package com.appointunified.service;

import com.appointunified.entity.Professional;
import com.appointunified.entity.ReputationScore;
import com.appointunified.exception.AppException;
import com.appointunified.repository.AppointmentRepository;
import com.appointunified.repository.ProfessionalRepository;
import com.appointunified.repository.ReputationScoreRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ReputationScoreService {

    private final ReputationScoreRepository reputationScoreRepository;
    private final ProfessionalRepository professionalRepository;
    private final AppointmentRepository appointmentRepository;

    public ReputationScoreService(
            ReputationScoreRepository reputationScoreRepository,
            ProfessionalRepository professionalRepository,
            AppointmentRepository appointmentRepository) {
        this.reputationScoreRepository = reputationScoreRepository;
        this.professionalRepository = professionalRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional(readOnly = true)
    public ReputationScore getOrCreate(UUID professionalId) {
        return reputationScoreRepository.findByProfessionalId(professionalId)
                .orElseGet(() -> {
                    Professional professional = professionalRepository.findById(professionalId)
                            .orElseThrow(() -> AppException.notFound("Professional not found"));
                    ReputationScore score = new ReputationScore();
                    score.setProfessional(professional);
                    return reputationScoreRepository.save(score);
                });
    }

    @Transactional
    public ReputationScore recompute(UUID professionalId) {
        Professional professional = professionalRepository.findById(professionalId)
                .orElseThrow(() -> AppException.notFound("Professional not found"));

        ReputationScore score = reputationScoreRepository.findByProfessionalId(professionalId)
                .orElseGet(() -> {
                    ReputationScore s = new ReputationScore();
                    s.setProfessional(professional);
                    return s;
                });

        int completed = professional.getTotalCompleted() != null ? professional.getTotalCompleted() : 0;
        BigDecimal ratingAvg = professional.getRatingAvg() != null ? professional.getRatingAvg() : BigDecimal.ZERO;
        BigDecimal ratingComponent = ratingAvg.min(new BigDecimal("5.00"))
                .divide(new BigDecimal("5.00"), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("4.00"));

        BigDecimal completionComponent = completed >= 200
                ? new BigDecimal("2.50")
                : new BigDecimal(completed).divide(new BigDecimal("200"), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("2.50"));

        BigDecimal responseComponent = new BigDecimal("2.00");
        BigDecimal recencyComponent = new BigDecimal("1.50");

        BigDecimal overall = ratingComponent
                .add(completionComponent)
                .add(responseComponent)
                .add(recencyComponent)
                .setScale(2, RoundingMode.HALF_UP);

        score.setRatingComponent(ratingComponent.setScale(2, RoundingMode.HALF_UP));
        score.setCompletionComponent(completionComponent.setScale(2, RoundingMode.HALF_UP));
        score.setResponseComponent(responseComponent.setScale(2, RoundingMode.HALF_UP));
        score.setRecencyComponent(recencyComponent.setScale(2, RoundingMode.HALF_UP));
        score.setOverallScore(overall.min(new BigDecimal("10.00")));
        score.setTotalAppointments((int) appointmentRepository.countByProfessionalId(professionalId));
        score.setLastComputedAt(OffsetDateTime.now());

        return reputationScoreRepository.save(score);
    }

    @Transactional
    @Scheduled(cron = "0 30 2 * * *")
    public void recomputeAllNightly() {
        List<Professional> professionals = professionalRepository.findAll();
        for (Professional professional : professionals) {
            recompute(professional.getId());
        }
    }
}

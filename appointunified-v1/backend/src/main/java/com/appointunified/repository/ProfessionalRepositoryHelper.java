package com.appointunified.repository;

import com.appointunified.entity.VerificationDocument;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ProfessionalRepositoryHelper {

    private final EntityManager entityManager;

    public ProfessionalRepositoryHelper(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Page<VerificationDocument> pendingVerificationDocuments(Pageable pageable) {
        List<VerificationDocument> rows = entityManager.createQuery(
                        "SELECT d FROM VerificationDocument d WHERE d.status = 'PENDING' ORDER BY d.submittedAt ASC",
                        VerificationDocument.class)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        Long total = entityManager.createQuery(
                        "SELECT COUNT(d) FROM VerificationDocument d WHERE d.status = 'PENDING'",
                        Long.class)
                .getSingleResult();

        return new PageImpl<>(rows, pageable, total);
    }
}

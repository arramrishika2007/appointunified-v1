package com.appointunified.repository;

import com.appointunified.entity.AdminAction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

import java.util.UUID;

public interface AdminActionRepository extends JpaRepository<AdminAction, UUID> {

	@Query("""
		SELECT a
		FROM AdminAction a
		JOIN FETCH a.admin
		LEFT JOIN FETCH a.targetProfessional p
		LEFT JOIN FETCH p.user
		WHERE a.actionType IN :actionTypes
		ORDER BY a.createdAt DESC
		""")
	List<AdminAction> findDecisionActions(
			@Param("actionTypes") Collection<String> actionTypes,
			Pageable pageable);
}

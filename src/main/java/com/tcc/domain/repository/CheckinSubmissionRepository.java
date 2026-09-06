package com.tcc.domain.repository;

import com.tcc.domain.model.CheckinSubmission;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckinSubmissionRepository extends JpaRepository<CheckinSubmission, UUID> {
    Optional<CheckinSubmission> findByPatientIdAndIdempotencyKey(UUID patientId, String idempotencyKey);
}

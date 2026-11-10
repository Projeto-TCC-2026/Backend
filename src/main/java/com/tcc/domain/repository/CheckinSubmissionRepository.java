package com.tcc.domain.repository;

import com.tcc.domain.model.CheckinSubmission;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CheckinSubmissionRepository extends JpaRepository<CheckinSubmission, UUID> {
  Optional<CheckinSubmission> findByPatientIdAndIdempotencyKey(UUID patientId, String idempotencyKey);

  @Query("""
      SELECT YEAR(s.submittedAt), MONTH(s.submittedAt), DAY(s.submittedAt), COUNT(s)
      FROM CheckinSubmission s
      WHERE s.submittedAt >= :start
        AND s.submittedAt < :end
        AND (:doctorId IS NULL OR EXISTS (
              SELECT dp.id FROM DoctorPatient dp
              WHERE dp.patient.id = s.patient.id
                AND dp.doctor.id = :doctorId
        ))
        AND (:hospitalId IS NULL OR EXISTS (
              SELECT dp.id FROM DoctorPatient dp
              WHERE dp.patient.id = s.patient.id
                AND dp.doctor.hospital.id = :hospitalId
        ))
      GROUP BY YEAR(s.submittedAt), MONTH(s.submittedAt), DAY(s.submittedAt)
      """)
  List<Object[]> countDailySubmissions(@Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end,
      @Param("doctorId") UUID doctorId,
      @Param("hospitalId") UUID hospitalId);
}

package com.tcc.domain.repository;

import com.tcc.domain.model.Checkin;
import com.tcc.domain.model.CheckinSource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CheckinRepository extends JpaRepository<Checkin, UUID> {

    boolean existsByPatientProcedureIdAndSourceAndManualDate(
            UUID patientProcedureId, CheckinSource source, LocalDate manualDate);

    @Query("""
            SELECT DISTINCT c FROM Checkin c
            JOIN FETCH c.patientProcedure pp
            JOIN FETCH pp.patient
            JOIN FETCH pp.doctor
            JOIN FETCH pp.procedure
            LEFT JOIN FETCH c.fieldValues fv
            LEFT JOIN FETCH fv.field
            WHERE c.submittedAt >= :start
              AND c.submittedAt < :end
              AND (:procedureId IS NULL OR pp.procedure.id = :procedureId)
              AND (:patientId IS NULL OR pp.patient.id = :patientId)
              AND (:doctorId IS NULL OR pp.doctor.id = :doctorId)
              AND (:hospitalId IS NULL OR pp.doctor.hospital.id = :hospitalId)
            ORDER BY c.submittedAt ASC
            """)
    List<Checkin> findForReport(@Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end,
                                @Param("procedureId") UUID procedureId,
                                @Param("patientId") UUID patientId,
                                @Param("doctorId") UUID doctorId,
                                @Param("hospitalId") UUID hospitalId);
}

package com.tcc.domain.repository;

import com.tcc.domain.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {

    List<Alert> findByPatientId(UUID patientId);

    List<Alert> findByHealthReadingId(UUID healthReadingId);

    List<Alert> findBySeverity(String severity);

    List<Alert> findByStatus(String status);

    List<Alert> findByPatientIdAndStatus(UUID patientId, String status);

    List<Alert> findByPatientIdOrderByCreatedAtDesc(UUID patientId);

    @Query("""
            SELECT a FROM Alert a
            JOIN FETCH a.patient
            LEFT JOIN FETCH a.healthReading
            WHERE a.createdAt >= :start
              AND a.createdAt < :end
            ORDER BY a.createdAt ASC
            """)
    List<Alert> findForReport(@Param("start") LocalDateTime start,
                              @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(a) FROM Alert a " +
           "JOIN a.patient p " +
           "JOIN p.doctorPatients dp " +
           "WHERE dp.doctor.hospital.id = :hospitalId " +
           "AND a.status = 'PENDING'")
    Long countPendingAlertsByHospitalId(@Param("hospitalId") UUID hospitalId);

    @Query("SELECT COUNT(DISTINCT a.patient) FROM Alert a " +
           "JOIN a.patient p " +
           "JOIN p.doctorPatients dp " +
           "WHERE dp.doctor.id = :doctorId " +
           "AND a.status = 'PENDING'")
    Long countPatientsWithPendingAlertByDoctorId(@Param("doctorId") UUID doctorId);
}

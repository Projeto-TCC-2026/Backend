package com.tcc.domain.repository;

import com.tcc.domain.model.ProcedureExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProcedureExecutionRepository extends JpaRepository<ProcedureExecution, UUID> {
    
    List<ProcedureExecution> findByPatientProcedureId(UUID patientProcedureId);
    
    List<ProcedureExecution> findByPatientId(UUID patientId);
    
    List<ProcedureExecution> findByDoctorId(UUID doctorId);
    
    List<ProcedureExecution> findByProcedureId(UUID procedureId);
    
    List<ProcedureExecution> findByStatus(String status);
    
    List<ProcedureExecution> findByExecutionDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT pe FROM ProcedureExecution pe WHERE pe.patient.id = :patientId")
    Page<ProcedureExecution> findPagedByPatientId(@Param("patientId") UUID patientId, Pageable pageable);
    
    @Query("SELECT pe FROM ProcedureExecution pe WHERE pe.patient.id = :patientId AND pe.status = :status")
    Page<ProcedureExecution> findPagedByPatientIdAndStatus(@Param("patientId") UUID patientId, @Param("status") String status, Pageable pageable);
    
    @Query("SELECT pe FROM ProcedureExecution pe WHERE pe.doctor.id = :doctorId")
    Page<ProcedureExecution> findPagedByDoctorId(@Param("doctorId") UUID doctorId, Pageable pageable);
    
    long countByPatientId(UUID patientId);

    long countByPatientIdAndStatus(UUID patientId, String status);

    long countByDoctorId(UUID doctorId);
    
    boolean existsByPatientId(UUID patientId);
    
    boolean existsByPatientIdAndStatus(UUID patientId, String status);
    
    @Query("SELECT pe FROM ProcedureExecution pe WHERE pe.patient.id = :patientId AND pe.executionDate BETWEEN :startDate AND :endDate")
    List<ProcedureExecution> findByPatientIdAndDateRange(@Param("patientId") UUID patientId, 
                                                          @Param("startDate") LocalDateTime startDate, 
                                                          @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT pe FROM ProcedureExecution pe WHERE pe.patient.id = :patientId ORDER BY pe.executionDate DESC")
    List<ProcedureExecution> findLatestByPatientId(@Param("patientId") UUID patientId, Pageable pageable);
}

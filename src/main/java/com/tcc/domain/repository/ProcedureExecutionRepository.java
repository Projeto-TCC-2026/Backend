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

@Repository
public interface ProcedureExecutionRepository extends JpaRepository<ProcedureExecution, Long> {
    
    // Métodos de busca simples
    List<ProcedureExecution> findByPatientProcedureId(Long patientProcedureId);
    
    List<ProcedureExecution> findByPatientId(Long patientId);
    
    List<ProcedureExecution> findByDoctorId(Long doctorId);
    
    List<ProcedureExecution> findByProcedureId(Long procedureId);
    
    List<ProcedureExecution> findByStatus(String status);
    
    List<ProcedureExecution> findByExecutionDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // Métodos com paginação
    Page<ProcedureExecution> findByPatientId(Long patientId, Pageable pageable);
    
    Page<ProcedureExecution> findByPatientIdAndStatus(Long patientId, String status, Pageable pageable);
    
    Page<ProcedureExecution> findByDoctorId(Long doctorId, Pageable pageable);
    
    // Métodos de contagem
    long countByPatientId(Long patientId);
    
    long countByPatientIdAndStatus(Long patientId, String status);
    
    // Verificações de existência
    boolean existsByPatientId(Long patientId);
    
    boolean existsByPatientIdAndStatus(Long patientId, String status);
    
    // Consultas personalizadas
    @Query("SELECT pe FROM ProcedureExecution pe WHERE pe.patient.id = :patientId AND pe.executionDate BETWEEN :startDate AND :endDate")
    List<ProcedureExecution> findByPatientIdAndDateRange(@Param("patientId") Long patientId, 
                                                          @Param("startDate") LocalDateTime startDate, 
                                                          @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT pe FROM ProcedureExecution pe WHERE pe.patient.id = :patientId ORDER BY pe.executionDate DESC")
    List<ProcedureExecution> findLatestByPatientId(@Param("patientId") Long patientId, Pageable pageable);
}


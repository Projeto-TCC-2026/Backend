package com.tcc.domain.repository;

import com.tcc.domain.model.ProcedureExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProcedureExecutionRepository extends JpaRepository<ProcedureExecution, Long> {
    
    List<ProcedureExecution> findByPatientProcedureId(Long patientProcedureId);
    
    List<ProcedureExecution> findByPatientId(Long patientId);
    
    List<ProcedureExecution> findByDoctorId(Long doctorId);
    
    List<ProcedureExecution> findByProcedureId(Long procedureId);
    
    List<ProcedureExecution> findByStatus(String status);
    
    List<ProcedureExecution> findByExecutionDateBetween(LocalDateTime startDate, LocalDateTime endDate);
}

package com.tcc.domain.repository;

import com.tcc.domain.model.PatientProcedure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientProcedureRepository extends JpaRepository<PatientProcedure, Long> {
    
    List<PatientProcedure> findByPatientId(Long patientId);
    
    List<PatientProcedure> findByDoctorId(Long doctorId);
    
    List<PatientProcedure> findByProcedureId(Long procedureId);
    
    List<PatientProcedure> findByStatus(String status);
    
    List<PatientProcedure> findByPatientIdAndStatus(Long patientId, String status);
}

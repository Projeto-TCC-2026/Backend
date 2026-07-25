package com.tcc.domain.repository;

import com.tcc.domain.model.PatientProcedure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PatientProcedureRepository extends JpaRepository<PatientProcedure, UUID> {
    
    List<PatientProcedure> findByPatientId(UUID patientId);
    
    List<PatientProcedure> findByDoctorId(UUID doctorId);
    
    List<PatientProcedure> findByProcedureId(UUID procedureId);
    
    List<PatientProcedure> findByStatus(String status);
    
    List<PatientProcedure> findByPatientIdAndStatus(UUID patientId, String status);
}

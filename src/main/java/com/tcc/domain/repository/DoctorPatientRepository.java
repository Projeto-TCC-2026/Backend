package com.tcc.domain.repository;

import com.tcc.domain.model.DoctorPatient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorPatientRepository extends JpaRepository<DoctorPatient, Long> {
    
    List<DoctorPatient> findByDoctorId(Long doctorId);
    
    List<DoctorPatient> findByPatientId(Long patientId);
    
    Optional<DoctorPatient> findByDoctorIdAndPatientId(Long doctorId, Long patientId);
    
    boolean existsByDoctorIdAndPatientId(Long doctorId, Long patientId);
}

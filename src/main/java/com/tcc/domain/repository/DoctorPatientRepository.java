package com.tcc.domain.repository;

import com.tcc.domain.model.DoctorPatient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DoctorPatientRepository extends JpaRepository<DoctorPatient, UUID> {
    
    List<DoctorPatient> findByDoctorId(UUID doctorId);
    
    List<DoctorPatient> findByPatientId(UUID patientId);
    
    Optional<DoctorPatient> findByDoctorIdAndPatientId(UUID doctorId, UUID patientId);
    
    boolean existsByDoctorIdAndPatientId(UUID doctorId, UUID patientId);
}

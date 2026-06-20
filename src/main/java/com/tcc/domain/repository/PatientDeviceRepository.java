package com.tcc.domain.repository;

import com.tcc.domain.model.PatientDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientDeviceRepository extends JpaRepository<PatientDevice, Long> {
    
    List<PatientDevice> findByPatientId(Long patientId);
    
    List<PatientDevice> findByPatientIdAndActive(Long patientId, Boolean active);
    
    Optional<PatientDevice> findByDeviceIdentifier(String deviceIdentifier);
}

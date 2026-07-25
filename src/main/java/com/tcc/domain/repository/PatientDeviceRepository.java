package com.tcc.domain.repository;

import com.tcc.domain.model.PatientDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientDeviceRepository extends JpaRepository<PatientDevice, UUID> {
    
    List<PatientDevice> findByPatientId(UUID patientId);
    
    List<PatientDevice> findByPatientIdAndActive(UUID patientId, Boolean active);
    
    Optional<PatientDevice> findByDeviceIdentifier(String deviceIdentifier);
}

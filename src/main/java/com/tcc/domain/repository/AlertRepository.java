package com.tcc.domain.repository;

import com.tcc.domain.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {
    
    List<Alert> findByPatientId(UUID patientId);
    
    List<Alert> findByHealthReadingId(UUID healthReadingId);
    
    List<Alert> findBySeverity(String severity);
    
    List<Alert> findByStatus(String status);
    
    List<Alert> findByPatientIdAndStatus(UUID patientId, String status);
    
    List<Alert> findByPatientIdOrderByCreatedAtDesc(UUID patientId);
}

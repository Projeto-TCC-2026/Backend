package com.tcc.domain.repository;

import com.tcc.domain.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    
    List<Alert> findByPatientId(Long patientId);
    
    List<Alert> findByHealthReadingId(Long healthReadingId);
    
    List<Alert> findBySeverity(String severity);
    
    List<Alert> findByStatus(String status);
    
    List<Alert> findByPatientIdAndStatus(Long patientId, String status);
    
    List<Alert> findByPatientIdOrderByCreatedAtDesc(Long patientId);
}

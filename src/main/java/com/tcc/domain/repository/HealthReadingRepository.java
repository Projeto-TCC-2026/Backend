package com.tcc.domain.repository;

import com.tcc.domain.model.HealthReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface HealthReadingRepository extends JpaRepository<HealthReading, UUID> {
    
    List<HealthReading> findByPatientId(UUID patientId);
    
    List<HealthReading> findByPatientDeviceId(UUID patientDeviceId);
    
    List<HealthReading> findByReadingImportId(UUID readingImportId);
    
    List<HealthReading> findByReadingType(String readingType);
    
    List<HealthReading> findByPatientIdAndReadingType(UUID patientId, String readingType);
    
    List<HealthReading> findByPatientIdAndMeasuredAtBetween(UUID patientId, LocalDateTime startDate, LocalDateTime endDate);
    
    List<HealthReading> findByPatientIdOrderByMeasuredAtDesc(UUID patientId);
}

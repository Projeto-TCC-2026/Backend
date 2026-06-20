package com.tcc.domain.repository;

import com.tcc.domain.model.HealthReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HealthReadingRepository extends JpaRepository<HealthReading, Long> {
    
    List<HealthReading> findByPatientId(Long patientId);
    
    List<HealthReading> findByPatientDeviceId(Long patientDeviceId);
    
    List<HealthReading> findByReadingImportId(Long readingImportId);
    
    List<HealthReading> findByReadingType(String readingType);
    
    List<HealthReading> findByPatientIdAndReadingType(Long patientId, String readingType);
    
    List<HealthReading> findByPatientIdAndMeasuredAtBetween(Long patientId, LocalDateTime startDate, LocalDateTime endDate);
    
    List<HealthReading> findByPatientIdOrderByMeasuredAtDesc(Long patientId);
}

package com.tcc.domain.repository;

import com.tcc.domain.model.ReadingImport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReadingImportRepository extends JpaRepository<ReadingImport, Long> {
    
    List<ReadingImport> findByPatientId(Long patientId);
    
    List<ReadingImport> findByPatientDeviceId(Long patientDeviceId);
    
    List<ReadingImport> findByPatientIdOrderByImportedAtDesc(Long patientId);
}

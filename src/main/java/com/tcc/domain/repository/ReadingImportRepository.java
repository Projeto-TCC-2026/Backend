package com.tcc.domain.repository;

import com.tcc.domain.model.ReadingImport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReadingImportRepository extends JpaRepository<ReadingImport, UUID> {
    
    List<ReadingImport> findByPatientId(UUID patientId);
    
    List<ReadingImport> findByPatientDeviceId(UUID patientDeviceId);
    
    List<ReadingImport> findByPatientIdOrderByImportedAtDesc(UUID patientId);
}

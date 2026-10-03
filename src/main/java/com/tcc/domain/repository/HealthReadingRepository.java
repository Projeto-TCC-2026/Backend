package com.tcc.domain.repository;

import com.tcc.domain.model.HealthReading;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HealthReadingRepository extends JpaRepository<HealthReading, UUID> {
    
    List<HealthReading> findByPatientId(UUID patientId);
    
    List<HealthReading> findByPatientDeviceId(UUID patientDeviceId);
    
    List<HealthReading> findByReadingImportId(UUID readingImportId);
    
    List<HealthReading> findByReadingType(String readingType);
    
    List<HealthReading> findByPatientIdAndReadingType(UUID patientId, String readingType);
    
    List<HealthReading> findByPatientIdAndMeasuredAtBetween(UUID patientId, LocalDateTime startDate, LocalDateTime endDate);
    
    List<HealthReading> findByPatientIdOrderByMeasuredAtDesc(UUID patientId);

    /**
     * Chave de idempotência da leitura recebida pela integração: a mesma medição
     * reentregue pela fila não deve virar uma segunda linha. Devolve a leitura já
     * gravada para que a resposta aponte para ela.
     *
     * <p>Pode devolver no máximo uma linha: a restrição UNIQUE criada na V33 é
     * exatamente sobre estas três colunas.
     */
    Optional<HealthReading> findByPatientIdAndReadingTypeAndMeasuredAt(UUID patientId,
                                                                      String readingType,
                                                                      LocalDateTime measuredAt);
}

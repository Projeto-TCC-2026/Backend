package com.tcc.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tcc.domain.model.HealthReading;

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

    /**
     * "Leitura anterior" do mesmo paciente e tipo: a mais recente, não suspeita, com
     * {@code measuredAt} estritamente menor que o da leitura atual.
     *
     * <p>O filtro {@code suspect = false} é o que faz uma leitura impossível no meio
     * do caminho não quebrar a sequência de confirmação — ela é pulada como se não
     * existisse.
     *
     * <p>Ordena por {@code measuredAt} desc e pagina em 1; quem chama pega o
     * primeiro elemento, se houver.
     */
    @Query("""
            SELECT r FROM HealthReading r
            WHERE r.patient.id = :patientId
              AND r.readingType = :readingType
              AND r.suspect = false
              AND r.measuredAt < :before
            ORDER BY r.measuredAt DESC
            """)
    List<HealthReading> findPreviousTrustedReadings(@Param("patientId") UUID patientId,
                                                    @Param("readingType") String readingType,
                                                    @Param("before") LocalDateTime before,
                                                    Pageable pageable);
}

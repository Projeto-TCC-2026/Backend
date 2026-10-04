package com.tcc.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tcc.domain.model.Alert;

public interface AlertRepository extends JpaRepository<Alert, UUID> {

    List<Alert> findByPatientId(UUID patientId);

    List<Alert> findByHealthReadingId(UUID healthReadingId);

    List<Alert> findBySeverity(String severity);

    List<Alert> findByStatus(String status);

    List<Alert> findByPatientIdAndStatus(UUID patientId, String status);

    /**
     * Alerta mais recente do paciente naquele status para o tipo de leitura.
     *
     * <p>Usado em dois pontos do fluxo de confirmação: buscar o UNCONFIRMED que a
     * leitura atual pode confirmar, e buscar o PENDING cuja janela de 4h pode ainda
     * estar bloqueando novos avisos. Traz a leitura associada junto porque o passo
     * seguinte compara a leitura do alerta com a leitura anterior.
     *
     * <p>Ordena por {@code createdAt} desc e pagina em 1 em vez de usar
     * {@code findFirst}: o projeto não tem derived query com {@code First} hoje, e
     * o JPQL explícito permite o JOIN FETCH da leitura na mesma ida ao banco.
     */
    @Query("""
            SELECT a FROM Alert a
            JOIN FETCH a.healthReading r
            WHERE a.patient.id = :patientId
              AND a.status = :status
              AND r.readingType = :readingType
            ORDER BY a.createdAt DESC
            """)
    List<Alert> findLatestByPatientAndStatusAndReadingType(@Param("patientId") UUID patientId,
                                                           @Param("status") String status,
                                                           @Param("readingType") String readingType,
                                                           Pageable pageable);

    /**
     * Carrega o alerta junto com o paciente, para o listener de e-mail poder ler o
     * nome do paciente e a leitura fora de transação da requisição original.
     */
    @Query("""
            SELECT a FROM Alert a
            JOIN FETCH a.patient
            LEFT JOIN FETCH a.healthReading
            WHERE a.id = :alertId
            """)
    Optional<Alert> findByIdWithPatientAndReading(@Param("alertId") UUID alertId);

    List<Alert> findByPatientIdOrderByCreatedAtDesc(UUID patientId);

    Page<Alert> findByPatientIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            UUID patientId, LocalDateTime since, Pageable pageable);

    @Query("""
            SELECT a FROM Alert a
            JOIN FETCH a.patient
            LEFT JOIN FETCH a.healthReading
            WHERE a.createdAt >= :start
              AND a.createdAt < :end
            ORDER BY a.createdAt ASC
            """)
    List<Alert> findForReport(@Param("start") LocalDateTime start,
                              @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(a) FROM Alert a " +
           "JOIN a.patient p " +
           "JOIN p.doctorPatients dp " +
           "WHERE dp.doctor.hospital.id = :hospitalId " +
           "AND a.status = 'PENDING'")
    Long countPendingAlertsByHospitalId(@Param("hospitalId") UUID hospitalId);

    @Query("SELECT COUNT(DISTINCT a.patient) FROM Alert a " +
           "JOIN a.patient p " +
           "JOIN p.doctorPatients dp " +
           "WHERE dp.doctor.id = :doctorId " +
           "AND a.status = 'PENDING'")
    Long countPatientsWithPendingAlertByDoctorId(@Param("doctorId") UUID doctorId);
}

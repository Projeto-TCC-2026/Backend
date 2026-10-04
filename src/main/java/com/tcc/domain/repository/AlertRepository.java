package com.tcc.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tcc.domain.model.Alert;
import com.tcc.domain.model.PatientAlertAnswer;

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

    /**
     * Ids dos alertas AWAITING_PATIENT cujo prazo de resposta já venceu. É a
     * varredura do agendador que roda a cada minuto.
     *
     * <p>Devolve só o id: o agendador não precisa da entidade para decidir, e cada
     * alerta é carregado depois, já dentro da transação que tenta a troca de status.
     *
     * <p>Alerta AWAITING_PATIENT sem prazo gravado é ignorado em vez de tratado como
     * vencido. Isso não deve existir — o prazo é preenchido na criação — mas um
     * registro assim seria confirmado imediatamente, avisando o médico sem que a
     * paciente tivesse tido chance de responder.
     */
    @Query("""
            SELECT a.id FROM Alert a
            WHERE a.status = 'AWAITING_PATIENT'
              AND a.patientResponseDeadline IS NOT NULL
              AND a.patientResponseDeadline <= :now
            ORDER BY a.patientResponseDeadline ASC
            """)
    List<UUID> findIdsAwaitingPatientPastDeadline(@Param("now") LocalDateTime now, Pageable pageable);

    /**
     * Tira o alerta de AWAITING_PATIENT, de forma atômica.
     *
     * <p>Este é o ponto que garante vencedor único entre a resposta da paciente e o
     * agendador. A condição {@code status = 'AWAITING_PATIENT'} está no próprio
     * UPDATE, então o banco serializa os dois caminhos no lock da linha: o primeiro
     * troca o status e recebe 1, o segundo encontra a linha já fora de
     * AWAITING_PATIENT e recebe 0. Só quem recebe 1 publica o evento, então o médico
     * recebe no máximo um e-mail.
     *
     * <p>Uma leitura seguida de escrita em dois comandos não daria essa garantia: as
     * duas transações poderiam ler AWAITING_PATIENT antes de qualquer escrita e as
     * duas se considerariam vencedoras.
     *
     * <p>{@code flushAutomatically} descarrega as alterações pendentes antes do
     * UPDATE, e {@code clearAutomatically} limpa o contexto depois, para que
     * nenhuma entidade em memória permaneça com o status antigo.
     *
     * @param confirmedAt     horário da medição que confirmou, ou nulo quando o
     *                        alerta não está sendo confirmado (resposta "estou bem")
     * @param answer          resposta da paciente, ou nulo quando quem age é o
     *                        agendador
     * @param respondedAt     horário da resposta, ou nulo quando quem age é o
     *                        agendador
     * @return 1 quando este chamador fez a transição, 0 quando outro chegou antes
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Alert a
               SET a.status = :targetStatus,
                   a.confirmedAt = :confirmedAt,
                   a.patientResponse = :answer,
                   a.patientRespondedAt = :respondedAt
             WHERE a.id = :alertId
               AND a.status = 'AWAITING_PATIENT'
            """)
    int leaveAwaitingPatient(@Param("alertId") UUID alertId,
                             @Param("targetStatus") String targetStatus,
                             @Param("confirmedAt") LocalDateTime confirmedAt,
                             @Param("answer") PatientAlertAnswer answer,
                             @Param("respondedAt") LocalDateTime respondedAt);

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

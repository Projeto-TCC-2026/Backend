package com.tcc.application.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tcc.application.dto.request.AlertEvaluationRequest;
import com.tcc.application.dto.request.AlertRequest;
import com.tcc.application.dto.response.AlertEvaluationResponse;
import com.tcc.application.dto.response.AlertResponse;
import com.tcc.application.mapper.AlertMapper;
import com.tcc.domain.event.AlertConfirmedEvent;
import com.tcc.domain.event.AlertCreatedEvent;
import com.tcc.domain.model.Alert;
import com.tcc.domain.model.AlertStatus;
import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.HealthReading;
import com.tcc.domain.model.Patient;
import com.tcc.domain.model.ReadingThreshold;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.AlertRepository;
import com.tcc.domain.repository.DoctorPatientRepository;
import com.tcc.domain.repository.DoctorRepository;
import com.tcc.domain.repository.HealthReadingRepository;
import com.tcc.domain.repository.PatientRepository;
import com.tcc.domain.repository.ReadingThresholdRepository;
import com.tcc.domain.repository.UserRepository;
import com.tcc.exception.ErrorMessages;
import com.tcc.exception.ResourceNotFoundException;
import com.tcc.exception.UnauthorizedException;

@Service
public class AlertServiceImpl implements AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertServiceImpl.class);

    /**
     * Distância máxima entre a leitura que criou o alerta e a que o confirma. Acima
     * disso as duas medições não descrevem o mesmo episódio, e a segunda abre um
     * alerta novo em vez de confirmar o antigo.
     */
    private static final Duration CONFIRMATION_WINDOW = Duration.ofHours(2);

    /**
     * Tempo durante o qual um alerta já confirmado silencia novos avisos do mesmo
     * paciente e tipo. Contado a partir de {@code confirmed_at}.
     */
    private static final Duration RENOTIFY_WINDOW = Duration.ofHours(4);

    /** Só a leitura anterior e o alerta mais recente interessam. */
    private static final Pageable LATEST_ONE = PageRequest.of(0, 1);

    private final PatientRepository patientRepository;
    private final ReadingThresholdRepository readingThresholdRepository;
    private final AlertRepository alertRepository;
    private final HealthReadingRepository healthReadingRepository;
    private final AlertMapper alertMapper;
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorPatientRepository doctorPatientRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AlertServiceImpl(PatientRepository patientRepository,
                            ReadingThresholdRepository readingThresholdRepository,
                            AlertRepository alertRepository,
                            HealthReadingRepository healthReadingRepository,
                            AlertMapper alertMapper,
                            UserRepository userRepository,
                            DoctorRepository doctorRepository,
                            DoctorPatientRepository doctorPatientRepository,
                            ApplicationEventPublisher eventPublisher) {
        this.patientRepository = patientRepository;
        this.readingThresholdRepository = readingThresholdRepository;
        this.alertRepository = alertRepository;
        this.healthReadingRepository = healthReadingRepository;
        this.alertMapper = alertMapper;
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.doctorPatientRepository = doctorPatientRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Grava a leitura e decide o que fazer com ela.
     *
     * <p>Ordem fixa:
     * <ol>
     *   <li>idempotência — leitura já recebida não é regravada;</li>
     *   <li>paciente;</li>
     *   <li>faixa plausível — valor impossível é gravado como suspeito e para aqui;</li>
     *   <li>gravação da leitura;</li>
     *   <li>faixa normal;</li>
     *   <li>dentro da faixa: descarta um UNCONFIRMED aberto do tipo;</li>
     *   <li>fora da faixa: janela de 4h, confirmação, ou alerta novo.</li>
     * </ol>
     *
     * <p>A leitura é gravada independentemente do resultado da avaliação, porque o
     * histórico de medições não depende de haver faixa cadastrada nem de o valor ser
     * anormal. Mesmo a leitura implausível é gravada — só fica marcada como suspeita.
     */
    @Override
    @Transactional
    public AlertEvaluationResponse evaluateReading(AlertEvaluationRequest request) {
        LocalDateTime measuredAtUtc = toUtc(request.measuredAt());

        Optional<HealthReading> alreadyReceived = healthReadingRepository
                .findByPatientIdAndReadingTypeAndMeasuredAt(
                        request.patientId(), request.readingType(), measuredAtUtc);

        if (alreadyReceived.isPresent()) {
            log.info("Leitura repetida do paciente {} para o tipo {}. Nada gravado, nada avisado.",
                    request.patientId(), request.readingType());
            return AlertEvaluationResponse.duplicate(alreadyReceived.get().getId());
        }

        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.patientNotFoundById(request.patientId())));

        Optional<ReadingThreshold> threshold =
                readingThresholdRepository.findByReadingType(request.readingType());

        // Valor impossível de medir: defeito de sensor, não quadro clínico. Grava
        // marcada como suspeita e encerra — sem avaliação, sem alerta, sem aviso.
        // A resposta é de sucesso de propósito: 4xx faria a fila mandar a mensagem
        // para a DLQ e a leitura se perderia.
        if (threshold.isPresent() && threshold.get().isImplausible(request.value())) {
            ReadingThreshold range = threshold.get();
            HealthReading suspectReading =
                    saveReading(patient, request, measuredAtUtc, true);

            log.warn("Leitura implausivel do paciente {} no tipo {}. Gravada como suspeita.",
                    patient.getId(), range.getReadingType());

            return AlertEvaluationResponse.suspect(
                    suspectReading.getId(), buildImplausibleReason(range));
        }

        HealthReading reading = saveReading(patient, request, measuredAtUtc, false);

        if (threshold.isEmpty()) {
            log.warn("Nenhuma faixa cadastrada para o tipo de leitura {}. Avaliacao ignorada para o paciente {}.",
                    request.readingType(), patient.getId());
            return AlertEvaluationResponse.withoutAlert(reading.getId());
        }

        ReadingThreshold range = threshold.get();
        String reason = buildReason(request.value(), range);

        return reason == null
                ? handleNormalReading(patient, reading, range)
                : handleAbnormalReading(patient, reading, range, reason);
    }

    /**
     * Leitura dentro da faixa normal. Se havia um alerta aguardando confirmação para
     * este tipo, o desvio não se repetiu: o alerta vira NOT_CONFIRMED e ninguém é
     * avisado.
     */
    private AlertEvaluationResponse handleNormalReading(Patient patient, HealthReading reading,
                                                        ReadingThreshold range) {
        Optional<Alert> unconfirmed = findLatestAlert(
                patient.getId(), AlertStatus.UNCONFIRMED, range.getReadingType());

        if (unconfirmed.isEmpty()) {
            return AlertEvaluationResponse.withoutAlert(reading.getId());
        }

        Alert alert = unconfirmed.get();
        alert.setStatus(AlertStatus.NOT_CONFIRMED);
        alertRepository.save(alert);

        log.info("Alerta {} do paciente {} passou a {}: leitura seguinte voltou ao normal.",
                alert.getId(), patient.getId(), AlertStatus.NOT_CONFIRMED);

        return AlertEvaluationResponse.withUpdatedAlert(
                alert.getSeverity(), alert.getId(), null, reading.getId(),
                AlertStatus.NOT_CONFIRMED);
    }

    /**
     * Leitura fora da faixa normal. Três saídas possíveis, nesta ordem: silenciada
     * pela janela de 4h de um alerta já confirmado, confirmação de um UNCONFIRMED
     * recente, ou alerta novo.
     */
    private AlertEvaluationResponse handleAbnormalReading(Patient patient, HealthReading reading,
                                                          ReadingThreshold range, String reason) {
        if (isSilencedByRecentConfirmation(patient, range, reading)) {
            return AlertEvaluationResponse.withoutAlert(reading.getId());
        }

        Optional<Alert> unconfirmed = findLatestAlert(
                patient.getId(), AlertStatus.UNCONFIRMED, range.getReadingType());

        if (unconfirmed.isPresent()) {
            Optional<Alert> confirmed =
                    confirmPendingCandidate(patient, unconfirmed.get(), range, reading);

            if (confirmed.isPresent()) {
                Alert alert = confirmed.get();
                return AlertEvaluationResponse.withUpdatedAlert(
                        alert.getSeverity(), alert.getId(), reason, reading.getId(),
                        AlertStatus.PENDING);
            }

            discardUnconfirmed(patient, unconfirmed.get());
        }

        Alert savedAlert = alertRepository.save(buildAlert(patient, reading, range, reason));

        // Consumido em AFTER_COMMIT: se esta transação sofrer rollback, nenhuma
        // notificação é enviada para um alerta que não existe. Este evento avisa
        // apenas o paciente; o médico só é avisado na confirmação.
        eventPublisher.publishEvent(new AlertCreatedEvent(savedAlert));

        log.info("Alerta {} criado como {} para o paciente {} no tipo {}.",
                savedAlert.getId(), AlertStatus.UNCONFIRMED, patient.getId(), range.getReadingType());

        return AlertEvaluationResponse.withAlert(
                range.getSeverity(), savedAlert.getId(), reason, reading.getId(),
                AlertStatus.UNCONFIRMED);
    }

    /**
     * Verdadeiro quando existe alerta PENDING do mesmo paciente e tipo confirmado há
     * menos de 4h. Nesse caso a leitura é gravada e nada mais acontece: o médico já
     * foi avisado e não precisa de um segundo e-mail pelo mesmo episódio.
     *
     * <p>PENDING sem {@code confirmedAt} não silencia. Isso cobre os alertas criados
     * antes da V35, que nasciam PENDING direto e nunca tiveram confirmação
     * registrada — tratá-los como silenciadores bloquearia o fluxo indefinidamente,
     * já que a janela nunca venceria.
     */
    private boolean isSilencedByRecentConfirmation(Patient patient, ReadingThreshold range,
                                                   HealthReading reading) {
        Optional<Alert> pending = findLatestAlert(
                patient.getId(), AlertStatus.PENDING, range.getReadingType());

        if (pending.isEmpty()) {
            return false;
        }

        LocalDateTime confirmedAt = pending.get().getConfirmedAt();

        if (confirmedAt == null) {
            log.debug("Alerta {} esta {} sem confirmed_at. Nao silencia a leitura atual.",
                    pending.get().getId(), AlertStatus.PENDING);
            return false;
        }

        boolean withinWindow = Duration.between(confirmedAt, reading.getMeasuredAt())
                .compareTo(RENOTIFY_WINDOW) < 0;

        if (withinWindow) {
            log.info("Alerta {} do paciente {} confirmado ha menos de {}h. Leitura gravada sem novo aviso.",
                    pending.get().getId(), patient.getId(), RENOTIFY_WINDOW.toHours());
        }

        return withinWindow;
    }

    /**
     * Tenta confirmar o alerta UNCONFIRMED do tipo com a leitura atual.
     *
     * <p>Confirma somente se a leitura do alerta for exatamente a leitura anterior
     * (a mais recente não suspeita antes da atual) e a distância entre as duas
     * medições couber na janela de confirmação. Exigir que seja a leitura
     * imediatamente anterior é o que torna "duas leituras seguidas" literal: uma
     * leitura normal no meio quebra a sequência, e uma suspeita não, porque a
     * consulta a ignora.
     *
     * @param alert o alerta UNCONFIRMED candidato, já carregado pelo chamador
     * @return o alerta confirmado, ou vazio quando não houve confirmação
     */
    private Optional<Alert> confirmPendingCandidate(Patient patient, Alert alert,
                                                    ReadingThreshold range,
                                                    HealthReading reading) {
        HealthReading alertReading = alert.getHealthReading();
        Optional<HealthReading> previous = findPreviousTrustedReading(
                patient.getId(), range.getReadingType(), reading.getMeasuredAt());

        if (previous.isEmpty() || alertReading == null
                || !previous.get().getId().equals(alertReading.getId())) {
            log.info("Alerta {} nao foi confirmado: a leitura anterior do tipo {} nao e a do alerta.",
                    alert.getId(), range.getReadingType());
            return Optional.empty();
        }

        Duration gap = Duration.between(alertReading.getMeasuredAt(), reading.getMeasuredAt());

        if (gap.compareTo(CONFIRMATION_WINDOW) > 0) {
            log.info("Alerta {} nao foi confirmado: {} min entre as leituras excedem a janela de {}h.",
                    alert.getId(), gap.toMinutes(), CONFIRMATION_WINDOW.toHours());
            return Optional.empty();
        }

        alert.setStatus(AlertStatus.PENDING);
        alert.setConfirmedAt(reading.getMeasuredAt());
        Alert saved = alertRepository.save(alert);

        // Este é o evento que dispara o e-mail ao médico. Carrega as duas leituras
        // porque o e-mail mostra a progressão, não só o último valor.
        eventPublisher.publishEvent(new AlertConfirmedEvent(saved, alertReading, reading));

        log.info("Alerta {} do paciente {} confirmado por segunda leitura e passou a {}.",
                saved.getId(), patient.getId(), AlertStatus.PENDING);

        return Optional.of(saved);
    }

    /**
     * Fecha como NOT_CONFIRMED o UNCONFIRMED que a leitura atual não confirmou —
     * seja porque as duas medições não couberam na janela de 2h, seja porque a
     * sequência de leituras foi quebrada no meio.
     *
     * <p>Sem isso o alerta antigo ficaria UNCONFIRMED para sempre: a leitura atual
     * abre um alerta novo, que passa a ser o mais recente do tipo, e nenhuma leitura
     * seguinte volta a alcançar o antigo. NOT_CONFIRMED é o mesmo desfecho usado
     * quando a leitura seguinte volta ao normal: o desvio não se repetiu em
     * sequência e ninguém é avisado por ele.
     */
    private void discardUnconfirmed(Patient patient, Alert alert) {
        alert.setStatus(AlertStatus.NOT_CONFIRMED);
        alertRepository.save(alert);

        log.info("Alerta {} do paciente {} passou a {}: nao foi confirmado e um alerta novo o substitui.",
                alert.getId(), patient.getId(), AlertStatus.NOT_CONFIRMED);
    }

    private Optional<Alert> findLatestAlert(UUID patientId, String status, String readingType) {
        return alertRepository
                .findLatestByPatientAndStatusAndReadingType(patientId, status, readingType, LATEST_ONE)
                .stream()
                .findFirst();
    }

    private Optional<HealthReading> findPreviousTrustedReading(UUID patientId, String readingType,
                                                               LocalDateTime before) {
        return healthReadingRepository
                .findPreviousTrustedReadings(patientId, readingType, before, LATEST_ONE)
                .stream()
                .findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AlertResponse> listRecentForPatient(String email, Pageable pageable) {
        UUID userId = userRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.userNotFoundByEmail(email)))
                .getId();
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new UnauthorizedException(
                        "Paciente não encontrado para o usuário autenticado"));

        return alertRepository
                .findByPatientIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                        patient.getId(), LocalDateTime.now().minusDays(7), pageable)
                .map(alertMapper::toResponse);
    }

    /**
     * Marca o alerta como RESOLVED.
     *
     * <p>O escopo é aplicado na consulta de vínculo, não só pela role: o médico
     * alcança apenas alerta de paciente vinculado a ele.
     */
    @Override
    @Transactional
    public AlertResponse resolveAlert(String email, UUID alertId) {
        Doctor doctor = resolveDoctor(email);

        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException(alertNotFoundById(alertId)));

        if (!doctorPatientRepository.existsByDoctorIdAndPatientId(
                doctor.getId(), alert.getPatient().getId())) {
            throw new UnauthorizedException(ErrorMessages.patientNotLinkedToDoctor());
        }

        // Alerta já resolvido: operação idempotente. Devolve o estado atual sem
        // gravar de novo, para não sobrescrever o registro de quem resolveu antes.
        if (AlertStatus.RESOLVED.equals(alert.getStatus())) {
            log.info("Alerta {} ja estava {}. Nada alterado.", alertId, AlertStatus.RESOLVED);
            return alertMapper.toResponse(alert);
        }

        alert.setStatus(AlertStatus.RESOLVED);
        Alert saved = alertRepository.save(alert);

        log.info("Alerta {} marcado como {} pelo medico {}.",
                alertId, AlertStatus.RESOLVED, doctor.getId());

        return alertMapper.toResponse(saved);
    }

    /**
     * Converte o horário informado para UTC, que é como a coluna
     * {@code measured_at} guarda o valor.
     *
     * <p>É isso que faz {@code 14:32Z} e {@code 11:32-03:00} virarem a mesma
     * leitura: os dois descrevem o mesmo instante, e depois da normalização
     * produzem o mesmo {@code LocalDateTime}. A conversão não consulta o fuso da
     * JVM — o deslocamento vem do próprio valor recebido.
     */
    private LocalDateTime toUtc(OffsetDateTime measuredAt) {
        return measuredAt.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    /**
     * Persiste a leitura recebida, com {@code measuredAt} já normalizado em UTC. O
     * dispositivo fica nulo: o contrato da integração não informa dispositivo, e a
     * coluna passou a aceitar nulo na V34.
     *
     * <p>A verificação de idempotência acontece antes desta chamada, mas duas
     * requisições idênticas em paralelo passam as duas por lá. A restrição UNIQUE da
     * V33 barra a segunda, e o flush explícito faz a violação aparecer aqui, onde
     * ainda é possível traduzi-la em {@link AlertDuplicateReadingException} — leitura
     * repetida é sucesso para quem chama, não erro.
     */
    private HealthReading saveReading(Patient patient, AlertEvaluationRequest request,
                                     LocalDateTime measuredAtUtc, boolean suspect) {
        HealthReading reading = new HealthReading();
        reading.setPatient(patient);
        reading.setPatientDevice(null);
        reading.setReadingType(request.readingType());
        reading.setValue(String.valueOf(request.value()));
        reading.setUnit(request.unit());
        reading.setMeasuredAt(measuredAtUtc);
        reading.setSuspect(suspect);

        try {
            return healthReadingRepository.saveAndFlush(reading);
        } catch (DataIntegrityViolationException e) {
            log.info("Leitura concorrente do paciente {} no tipo {} barrada pela restricao de unicidade.",
                    patient.getId(), request.readingType());
            throw new AlertDuplicateReadingException(e);
        }
    }

    /**
     * Compara o valor com a faixa normal e descreve o desvio encontrado.
     * Retorna null quando o valor está dentro da faixa.
     *
     * <p>Os limites são inclusivos no normal: a comparação usa estritamente menor e
     * estritamente maior, então valor igual a um dos limites não gera alerta.
     */
    private String buildReason(Double value, ReadingThreshold range) {
        Double min = range.getNormalMin();
        Double max = range.getNormalMax();

        if (min != null && value < min) {
            return "Valor abaixo do mínimo normal de " + min + " para " + range.getReadingType();
        }
        if (max != null && value > max) {
            return "Valor acima do máximo normal de " + max + " para " + range.getReadingType();
        }
        return null;
    }

    /**
     * Descreve por que a leitura foi considerada impossível. Vai no campo
     * {@code reason} da resposta, para quem chama saber que a leitura foi descartada
     * por implausibilidade e não por estar dentro do normal.
     */
    private String buildImplausibleReason(ReadingThreshold range) {
        return "Valor fora da faixa plausível de " + range.getReadingType()
                + " (" + range.getPlausibleMin() + " a " + range.getPlausibleMax()
                + "). Leitura registrada como suspeita e não avaliada.";
    }

    /**
     * Monta o alerta reaproveitando o AlertMapper já existente, para não duplicar a
     * atribuição de campos da entidade. A leitura que originou o alerta vai no
     * relacionamento, então health_reading_id fica preenchido.
     *
     * <p>Nasce UNCONFIRMED: só vira PENDING quando uma segunda leitura seguida
     * confirmar o desvio.
     */
    private Alert buildAlert(Patient patient, HealthReading reading,
                             ReadingThreshold range, String reason) {
        AlertRequest alertRequest = new AlertRequest(
                patient.getId(),
                reading.getId(),
                range.getSeverity(),
                buildTitle(range),
                reason,
                AlertStatus.UNCONFIRMED
        );
        return alertMapper.toEntity(alertRequest, patient, reading);
    }

    private String buildTitle(ReadingThreshold range) {
        String label = range.getLabel();
        String title = "Leitura fora da faixa normal: " + range.getReadingType()
                + (label != null ? " (" + label + ")" : "");
        return title.length() > 255 ? title.substring(0, 255) : title;
    }

    /** Mesmo caminho usado pelos outros services: usuário ativo, depois médico. */
    private Doctor resolveDoctor(String email) {
        User user = userRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.userNotFoundByEmail(email)));

        return doctorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new UnauthorizedException(ErrorMessages.doctorProfileNotFound()));
    }

    private static String alertNotFoundById(UUID id) {
        return "Alerta não encontrado com ID: " + id;
    }
}

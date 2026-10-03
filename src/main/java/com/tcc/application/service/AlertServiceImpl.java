package com.tcc.application.service;

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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tcc.application.dto.request.AlertEvaluationRequest;
import com.tcc.application.dto.request.AlertRequest;
import com.tcc.application.dto.response.AlertEvaluationResponse;
import com.tcc.application.dto.response.AlertResponse;
import com.tcc.application.mapper.AlertMapper;
import com.tcc.domain.event.AlertCreatedEvent;
import com.tcc.domain.model.Alert;
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
     * Status inicial do alerta. O JPQL de contagem do dashboard filtra por esta
     * string literal, então qualquer outra grafia torna o alerta invisível nos
     * indicadores de "alertas pendentes".
     */
    private static final String STATUS_PENDING = "PENDING";

    /** Status final do alerta depois que o médico o trata. */
    private static final String STATUS_RESOLVED = "RESOLVED";

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
     * Grava a leitura e, quando ela está fora da faixa normal, cria o alerta.
     *
     * <p>Ordem fixa: idempotência, paciente, gravação da leitura, avaliação,
     * deduplicação, alerta. A leitura é gravada antes de qualquer decisão sobre
     * alerta, porque o histórico de medições não depende de haver faixa cadastrada
     * nem de o valor ser anormal.
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

        HealthReading reading = saveReading(patient, request, measuredAtUtc);

        Optional<ReadingThreshold> threshold =
                readingThresholdRepository.findByReadingType(request.readingType());

        if (threshold.isEmpty()) {
            log.warn("Nenhuma faixa cadastrada para o tipo de leitura {}. Avaliacao ignorada para o paciente {}.",
                    request.readingType(), patient.getId());
            return AlertEvaluationResponse.withoutAlert(reading.getId());
        }

        ReadingThreshold range = threshold.get();
        String reason = buildReason(request.value(), range);

        if (reason == null) {
            return AlertEvaluationResponse.withoutAlert(reading.getId());
        }

        if (alertRepository.existsByPatientIdAndStatusAndHealthReading_ReadingType(
                patient.getId(), STATUS_PENDING, range.getReadingType())) {
            log.info("Alerta ja em aberto para o paciente {} no tipo {}. Nenhum novo alerta criado.",
                    patient.getId(), range.getReadingType());
            return AlertEvaluationResponse.withoutAlert(reading.getId());
        }

        Alert savedAlert = alertRepository.save(buildAlert(patient, reading, range, reason));

        // Consumido em AFTER_COMMIT: se esta transação sofrer rollback, nenhuma
        // notificação é enviada para um alerta que não existe.
        eventPublisher.publishEvent(new AlertCreatedEvent(savedAlert));

        log.info("Alerta {} gerado para o paciente {} a partir do tipo de leitura {}.",
                savedAlert.getId(), patient.getId(), range.getReadingType());

        return AlertEvaluationResponse.withAlert(
                range.getSeverity(), savedAlert.getId(), reason, reading.getId());
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
        if (STATUS_RESOLVED.equals(alert.getStatus())) {
            log.info("Alerta {} ja estava {}. Nada alterado.", alertId, STATUS_RESOLVED);
            return alertMapper.toResponse(alert);
        }

        alert.setStatus(STATUS_RESOLVED);
        Alert saved = alertRepository.save(alert);

        log.info("Alerta {} marcado como {} pelo medico {}.", alertId, STATUS_RESOLVED, doctor.getId());

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
                                     LocalDateTime measuredAtUtc) {
        HealthReading reading = new HealthReading();
        reading.setPatient(patient);
        reading.setPatientDevice(null);
        reading.setReadingType(request.readingType());
        reading.setValue(String.valueOf(request.value()));
        reading.setUnit(request.unit());
        reading.setMeasuredAt(measuredAtUtc);

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
     * Monta o alerta reaproveitando o AlertMapper já existente, para não duplicar a
     * atribuição de campos da entidade. A leitura que originou o alerta vai no
     * relacionamento, então health_reading_id fica preenchido.
     */
    private Alert buildAlert(Patient patient, HealthReading reading,
                             ReadingThreshold range, String reason) {
        AlertRequest alertRequest = new AlertRequest(
                patient.getId(),
                reading.getId(),
                range.getSeverity(),
                buildTitle(range),
                reason,
                STATUS_PENDING
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

package com.tcc.application.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.tcc.application.dto.request.AlertEvaluationRequest;
import com.tcc.application.dto.response.AlertEvaluationResponse;
import com.tcc.application.dto.response.AlertResponse;
import com.tcc.application.mapper.AlertMapper;
import com.tcc.domain.event.AlertCreatedEvent;
import com.tcc.domain.model.Alert;
import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.HealthReading;
import com.tcc.domain.model.Hospital;
import com.tcc.domain.model.Patient;
import com.tcc.domain.model.ReadingThreshold;
import com.tcc.domain.model.Role;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.AlertRepository;
import com.tcc.domain.repository.DoctorPatientRepository;
import com.tcc.domain.repository.DoctorRepository;
import com.tcc.domain.repository.HealthReadingRepository;
import com.tcc.domain.repository.PatientRepository;
import com.tcc.domain.repository.ReadingThresholdRepository;
import com.tcc.domain.repository.UserRepository;
import com.tcc.exception.ResourceNotFoundException;
import com.tcc.exception.UnauthorizedException;

@ExtendWith(MockitoExtension.class)
class AlertServiceImplTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private ReadingThresholdRepository readingThresholdRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private HealthReadingRepository healthReadingRepository;

    @Mock
    private AlertMapper alertMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private DoctorPatientRepository doctorPatientRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AlertServiceImpl alertService;

    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID NONEXISTENT_PATIENT_ID = UUID.randomUUID();
    private static final UUID ALERT_ID = UUID.randomUUID();
    private static final UUID READING_ID = UUID.randomUUID();
    private static final UUID DOCTOR_ID = UUID.randomUUID();

    private static final String HEART_RATE = "HEART_RATE";
    private static final String SPO2 = "SPO2";

    /** Horário informado pela integração, em UTC. */
    private static final OffsetDateTime MEASURED_AT_UTC =
            OffsetDateTime.of(2026, 9, 29, 14, 30, 0, 0, ZoneOffset.UTC);

    /** O mesmo instante expresso em -03:00: 11:30 em Brasília é 14:30 em UTC. */
    private static final OffsetDateTime MEASURED_AT_SAME_INSTANT_OFFSET =
            OffsetDateTime.of(2026, 9, 29, 11, 30, 0, 0, ZoneOffset.ofHours(-3));

    /** O que precisa chegar ao repositório e à entidade, depois da normalização. */
    private static final LocalDateTime MEASURED_AT_STORED = LocalDateTime.of(2026, 9, 29, 14, 30);

    private Patient patient;

    @BeforeEach
    void setUp() {
        patient = new Patient();
        patient.setId(PATIENT_ID);
        patient.setFullName("Paciente de Teste");
    }

    private AlertEvaluationRequest requestOf(String readingType, Double value) {
        return requestOf(readingType, value, MEASURED_AT_UTC);
    }

    private AlertEvaluationRequest requestOf(String readingType, Double value,
                                             OffsetDateTime measuredAt) {
        return new AlertEvaluationRequest(PATIENT_ID, readingType, value, measuredAt, "bpm");
    }

    private ReadingThreshold thresholdOf(String readingType, Double min, Double max) {
        ReadingThreshold threshold = new ReadingThreshold(readingType, min, max, "CRITICAL");
        threshold.setId(UUID.randomUUID());
        return threshold;
    }

    /**
     * Leitura ainda não recebida: o caminho normal de gravação.
     *
     * <p>O stub casa com o horário já normalizado em UTC. Se o service deixar de
     * converter, a consulta chega com outro valor, o stub não casa e o teste falha.
     */
    private void stubReadingNotYetReceived(String readingType) {
        when(healthReadingRepository.findByPatientIdAndReadingTypeAndMeasuredAt(
                PATIENT_ID, readingType, MEASURED_AT_STORED)).thenReturn(Optional.empty());
        when(healthReadingRepository.saveAndFlush(any(HealthReading.class)))
                .thenAnswer(invocation -> {
                    HealthReading reading = invocation.getArgument(0);
                    reading.setId(READING_ID);
                    return reading;
                });
    }

    /** Simula a persistência atribuindo um id ao alerta salvo. */
    private void stubAlertPersistence() {
        when(alertMapper.toEntity(any(), any(), any())).thenAnswer(invocation -> {
            var alertRequest = (com.tcc.application.dto.request.AlertRequest) invocation.getArgument(0);
            Alert alert = new Alert();
            alert.setPatient(patient);
            alert.setHealthReading(invocation.getArgument(2));
            alert.setSeverity(alertRequest.severity());
            alert.setTitle(alertRequest.title());
            alert.setDescription(alertRequest.description());
            alert.setStatus(alertRequest.status());
            return alert;
        });
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> {
            Alert alert = invocation.getArgument(0);
            alert.setId(ALERT_ID);
            return alert;
        });
    }

    private void stubNoPendingAlert(String readingType) {
        when(alertRepository.existsByPatientIdAndStatusAndHealthReading_ReadingType(
                PATIENT_ID, "PENDING", readingType)).thenReturn(false);
    }

    @Nested
    @DisplayName("gravação da leitura")
    class ReadingPersistence {

        @Test
        @DisplayName("leitura normal deve ser gravada sem gerar alerta")
        void shouldPersistNormalReadingWithoutAlert() {
            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            stubReadingNotYetReceived(HEART_RATE);
            when(readingThresholdRepository.findByReadingType(HEART_RATE))
                    .thenReturn(Optional.of(thresholdOf(HEART_RATE, 50.0, 120.0)));

            AlertEvaluationResponse result = alertService.evaluateReading(requestOf(HEART_RATE, 80.0));

            ArgumentCaptor<HealthReading> captor = ArgumentCaptor.forClass(HealthReading.class);
            verify(healthReadingRepository).saveAndFlush(captor.capture());

            HealthReading saved = captor.getValue();
            assertThat(saved.getPatient()).isEqualTo(patient);
            assertThat(saved.getReadingType()).isEqualTo(HEART_RATE);
            assertThat(saved.getValue()).isEqualTo("80.0");
            assertThat(saved.getUnit()).isEqualTo("bpm");
            assertThat(saved.getMeasuredAt()).isEqualTo(MEASURED_AT_STORED);
            assertThat(saved.getPatientDevice()).isNull();

            assertThat(result.alertGenerated()).isFalse();
            assertThat(result.duplicateReading()).isFalse();
            assertThat(result.healthReadingId()).isEqualTo(READING_ID);
            verify(alertRepository, never()).save(any());
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("leitura sem faixa cadastrada deve ser gravada mesmo assim")
        void shouldPersistReadingWithoutConfiguredThreshold() {
            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            stubReadingNotYetReceived("GLUCOSE");
            when(readingThresholdRepository.findByReadingType("GLUCOSE")).thenReturn(Optional.empty());

            AlertEvaluationResponse result = alertService.evaluateReading(requestOf("GLUCOSE", 500.0));

            verify(healthReadingRepository).saveAndFlush(any(HealthReading.class));
            assertThat(result.alertGenerated()).isFalse();
            assertThat(result.healthReadingId()).isEqualTo(READING_ID);
            verify(alertRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("valor dentro da faixa normal")
    class WithinNormalRange {

        @Test
        @DisplayName("nao deve gerar alerta quando valor e exatamente igual ao minimo")
        void shouldNotGenerateAlertWhenValueEqualsMinimum() {
            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            stubReadingNotYetReceived(HEART_RATE);
            when(readingThresholdRepository.findByReadingType(HEART_RATE))
                    .thenReturn(Optional.of(thresholdOf(HEART_RATE, 50.0, 120.0)));

            AlertEvaluationResponse result = alertService.evaluateReading(requestOf(HEART_RATE, 50.0));

            assertThat(result.alertGenerated()).isFalse();
            verify(alertRepository, never()).save(any());
        }

        @Test
        @DisplayName("nao deve gerar alerta quando valor e exatamente igual ao maximo")
        void shouldNotGenerateAlertWhenValueEqualsMaximum() {
            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            stubReadingNotYetReceived(HEART_RATE);
            when(readingThresholdRepository.findByReadingType(HEART_RATE))
                    .thenReturn(Optional.of(thresholdOf(HEART_RATE, 50.0, 120.0)));

            AlertEvaluationResponse result = alertService.evaluateReading(requestOf(HEART_RATE, 120.0));

            assertThat(result.alertGenerated()).isFalse();
            verify(alertRepository, never()).save(any());
        }

        @Test
        @DisplayName("nao deve gerar alerta quando faixa nao tem maximo e o valor e alto")
        void shouldNotGenerateAlertWhenMaximumIsNullAndValueIsHigh() {
            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            stubReadingNotYetReceived(SPO2);
            when(readingThresholdRepository.findByReadingType(SPO2))
                    .thenReturn(Optional.of(thresholdOf(SPO2, 90.0, null)));

            AlertEvaluationResponse result = alertService.evaluateReading(requestOf(SPO2, 99.0));

            assertThat(result.alertGenerated()).isFalse();
            verify(alertRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve listar somente alertas do paciente autenticado dos últimos sete dias")
        void shouldListRecentAlertsForAuthenticatedPatient() {
            User user = new User("patient@tcc.com", "hash", Role.PATIENT);
            user.setId(UUID.randomUUID());
            Alert alert = new Alert();
            alert.setPatient(patient);
            AlertResponse response = new AlertResponse(ALERT_ID, null, null, "CRITICAL",
                    "Alerta", "Descrição", "PENDING", LocalDateTime.now());
            PageRequest pageable = PageRequest.of(0, 20);

            when(userRepository.findByEmailAndActiveTrue(user.getEmail())).thenReturn(Optional.of(user));
            when(patientRepository.findByUserId(user.getId())).thenReturn(Optional.of(patient));
            when(alertRepository.findByPatientIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                    org.mockito.ArgumentMatchers.eq(PATIENT_ID),
                    any(LocalDateTime.class),
                    org.mockito.ArgumentMatchers.eq(pageable))).thenReturn(new PageImpl<>(java.util.List.of(alert)));
            when(alertMapper.toResponse(alert)).thenReturn(response);

            var result = alertService.listRecentForPatient(user.getEmail(), pageable);

            assertThat(result.getContent()).containsExactly(response);
            verify(alertRepository).findByPatientIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                    org.mockito.ArgumentMatchers.eq(PATIENT_ID),
                    any(LocalDateTime.class),
                    org.mockito.ArgumentMatchers.eq(pageable));
        }
    }

    @Nested
    @DisplayName("valor fora da faixa normal")
    class OutsideNormalRange {

        @Test
        @DisplayName("deve gravar a leitura, criar alerta ligado a ela e publicar o evento")
        void shouldPersistReadingCreateLinkedAlertAndPublishEvent() {
            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            stubReadingNotYetReceived(HEART_RATE);
            when(readingThresholdRepository.findByReadingType(HEART_RATE))
                    .thenReturn(Optional.of(thresholdOf(HEART_RATE, 50.0, 120.0)));
            stubNoPendingAlert(HEART_RATE);
            stubAlertPersistence();

            AlertEvaluationResponse result = alertService.evaluateReading(requestOf(HEART_RATE, 130.0));

            ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
            verify(alertRepository).save(alertCaptor.capture());

            Alert saved = alertCaptor.getValue();
            assertThat(saved.getStatus()).isEqualTo("PENDING");
            assertThat(saved.getSeverity()).isEqualTo("CRITICAL");
            assertThat(saved.getPatient()).isEqualTo(patient);
            assertThat(saved.getHealthReading()).isNotNull();
            assertThat(saved.getHealthReading().getId()).isEqualTo(READING_ID);
            assertThat(saved.getTitle()).isNotBlank();

            ArgumentCaptor<AlertCreatedEvent> eventCaptor =
                    ArgumentCaptor.forClass(AlertCreatedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().alert().getId()).isEqualTo(ALERT_ID);

            assertThat(result.alertGenerated()).isTrue();
            assertThat(result.severity()).isEqualTo("CRITICAL");
            assertThat(result.alertId()).isEqualTo(ALERT_ID);
            assertThat(result.healthReadingId()).isEqualTo(READING_ID);
            assertThat(result.reason()).contains("acima");
        }

        @Test
        @DisplayName("deve gerar alerta quando valor esta abaixo do minimo")
        void shouldGenerateAlertWhenValueIsBelowMinimum() {
            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            stubReadingNotYetReceived(HEART_RATE);
            when(readingThresholdRepository.findByReadingType(HEART_RATE))
                    .thenReturn(Optional.of(thresholdOf(HEART_RATE, 50.0, 120.0)));
            stubNoPendingAlert(HEART_RATE);
            stubAlertPersistence();

            AlertEvaluationResponse result = alertService.evaluateReading(requestOf(HEART_RATE, 45.0));

            assertThat(result.alertGenerated()).isTrue();
            assertThat(result.reason()).contains("abaixo");
        }

        @Test
        @DisplayName("deve gerar alerta quando faixa sem maximo recebe valor abaixo do minimo")
        void shouldGenerateAlertWhenValueIsBelowMinimumOnOpenEndedRange() {
            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            stubReadingNotYetReceived(SPO2);
            when(readingThresholdRepository.findByReadingType(SPO2))
                    .thenReturn(Optional.of(thresholdOf(SPO2, 90.0, null)));
            stubNoPendingAlert(SPO2);
            stubAlertPersistence();

            AlertEvaluationResponse result = alertService.evaluateReading(requestOf(SPO2, 85.0));

            assertThat(result.alertGenerated()).isTrue();
            assertThat(result.severity()).isEqualTo("CRITICAL");
        }
    }

    @Nested
    @DisplayName("idempotência da leitura")
    class Idempotency {

        @Test
        @DisplayName("leitura repetida nao deve ser gravada, nao cria alerta e responde sucesso")
        void shouldIgnoreAlreadyReceivedReading() {
            HealthReading existing = new HealthReading();
            existing.setId(READING_ID);
            when(healthReadingRepository.findByPatientIdAndReadingTypeAndMeasuredAt(
                    PATIENT_ID, HEART_RATE, MEASURED_AT_STORED)).thenReturn(Optional.of(existing));

            AlertEvaluationResponse result = alertService.evaluateReading(requestOf(HEART_RATE, 200.0));

            assertThat(result.duplicateReading()).isTrue();
            assertThat(result.alertGenerated()).isFalse();
            assertThat(result.healthReadingId()).isEqualTo(READING_ID);

            verify(healthReadingRepository, never()).saveAndFlush(any());
            verify(alertRepository, never()).save(any());
            verifyNoInteractions(eventPublisher, patientRepository, readingThresholdRepository);
        }

        @Test
        @DisplayName("mesmo instante com fuso diferente (-03:00) deve ser tratado como repetido")
        void shouldTreatSameInstantInAnotherOffsetAsDuplicate() {
            HealthReading existing = new HealthReading();
            existing.setId(READING_ID);

            // Gravado a partir de 14:30Z; agora chega 11:30-03:00, o mesmo instante.
            when(healthReadingRepository.findByPatientIdAndReadingTypeAndMeasuredAt(
                    PATIENT_ID, HEART_RATE, MEASURED_AT_STORED)).thenReturn(Optional.of(existing));

            AlertEvaluationResponse result = alertService.evaluateReading(
                    requestOf(HEART_RATE, 200.0, MEASURED_AT_SAME_INSTANT_OFFSET));

            assertThat(result.duplicateReading()).isTrue();
            assertThat(result.healthReadingId()).isEqualTo(READING_ID);

            // Prova a normalização: a consulta usou o horário em UTC, não 11:30.
            verify(healthReadingRepository).findByPatientIdAndReadingTypeAndMeasuredAt(
                    PATIENT_ID, HEART_RATE, MEASURED_AT_STORED);
            verify(healthReadingRepository, never()).saveAndFlush(any());
            verify(alertRepository, never()).save(any());
        }

        @Test
        @DisplayName("os dois fusos do mesmo instante devem gravar o mesmo measuredAt")
        void shouldNormalizeBothOffsetsToTheSameStoredValue() {
            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            stubReadingNotYetReceived(HEART_RATE);
            when(readingThresholdRepository.findByReadingType(HEART_RATE))
                    .thenReturn(Optional.of(thresholdOf(HEART_RATE, 50.0, 120.0)));

            alertService.evaluateReading(requestOf(HEART_RATE, 80.0, MEASURED_AT_UTC));
            alertService.evaluateReading(
                    requestOf(HEART_RATE, 80.0, MEASURED_AT_SAME_INSTANT_OFFSET));

            ArgumentCaptor<HealthReading> captor = ArgumentCaptor.forClass(HealthReading.class);
            verify(healthReadingRepository, times(2)).saveAndFlush(captor.capture());

            assertThat(captor.getAllValues())
                    .extracting(HealthReading::getMeasuredAt)
                    .containsExactly(MEASURED_AT_STORED, MEASURED_AT_STORED);
        }

        @Test
        @DisplayName("violacao de unicidade em corrida deve virar AlertDuplicateReadingException")
        void shouldTranslateUniqueViolationIntoDuplicateException() {
            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            when(healthReadingRepository.findByPatientIdAndReadingTypeAndMeasuredAt(
                    PATIENT_ID, HEART_RATE, MEASURED_AT_STORED)).thenReturn(Optional.empty());
            when(healthReadingRepository.saveAndFlush(any(HealthReading.class)))
                    .thenThrow(new DataIntegrityViolationException("uq_health_readings..."));

            AlertEvaluationRequest request = requestOf(HEART_RATE, 200.0);

            assertThatThrownBy(() -> alertService.evaluateReading(request))
                    .isInstanceOf(AlertDuplicateReadingException.class);

            verify(alertRepository, never()).save(any());
            verifyNoInteractions(eventPublisher);
        }
    }

    @Nested
    @DisplayName("deduplicação de alerta")
    class AlertDeduplication {

        @Test
        @DisplayName("com alerta PENDING do mesmo tipo deve gravar a leitura e nao criar alerta")
        void shouldPersistReadingButSkipAlertWhenPendingAlreadyExists() {
            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            stubReadingNotYetReceived(HEART_RATE);
            when(readingThresholdRepository.findByReadingType(HEART_RATE))
                    .thenReturn(Optional.of(thresholdOf(HEART_RATE, 50.0, 120.0)));
            when(alertRepository.existsByPatientIdAndStatusAndHealthReading_ReadingType(
                    PATIENT_ID, "PENDING", HEART_RATE)).thenReturn(true);

            AlertEvaluationResponse result = alertService.evaluateReading(requestOf(HEART_RATE, 190.0));

            verify(healthReadingRepository).saveAndFlush(any(HealthReading.class));
            assertThat(result.alertGenerated()).isFalse();
            assertThat(result.duplicateReading()).isFalse();
            assertThat(result.healthReadingId()).isEqualTo(READING_ID);

            verify(alertRepository, never()).save(any());
            verifyNoInteractions(eventPublisher);
        }
    }

    @Nested
    @DisplayName("paciente inexistente")
    class MissingPatient {

        @Test
        @DisplayName("deve lancar excecao quando paciente nao encontrado")
        void shouldThrowWhenPatientNotFound() {
            when(healthReadingRepository.findByPatientIdAndReadingTypeAndMeasuredAt(
                    NONEXISTENT_PATIENT_ID, HEART_RATE, MEASURED_AT_STORED))
                    .thenReturn(Optional.empty());
            when(patientRepository.findById(NONEXISTENT_PATIENT_ID)).thenReturn(Optional.empty());

            AlertEvaluationRequest request = new AlertEvaluationRequest(
                    NONEXISTENT_PATIENT_ID, HEART_RATE, 80.0, MEASURED_AT_UTC, null);

            assertThatThrownBy(() -> alertService.evaluateReading(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Paciente");

            verify(healthReadingRepository, never()).saveAndFlush(any());
            verify(alertRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("resolução de alerta pelo médico")
    class Resolution {

        private static final String DOCTOR_EMAIL = "doctor@tcc.com";

        private Alert pendingAlert() {
            Alert alert = new Alert();
            alert.setId(ALERT_ID);
            alert.setPatient(patient);
            alert.setStatus("PENDING");
            return alert;
        }

        private void stubAuthenticatedDoctor() {
            User user = new User(DOCTOR_EMAIL, "hash", Role.DOCTOR);
            user.setId(UUID.randomUUID());

            Doctor doctor = new Doctor();
            doctor.setId(DOCTOR_ID);
            doctor.setUser(user);
            doctor.setHospital(new Hospital());

            when(userRepository.findByEmailAndActiveTrue(DOCTOR_EMAIL)).thenReturn(Optional.of(user));
            when(doctorRepository.findByUserId(user.getId())).thenReturn(Optional.of(doctor));
        }

        @Test
        @DisplayName("medico vinculado deve conseguir marcar o alerta como RESOLVED")
        void shouldResolveAlertForLinkedDoctor() {
            Alert alert = pendingAlert();
            stubAuthenticatedDoctor();
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(alert));
            when(doctorPatientRepository.existsByDoctorIdAndPatientId(DOCTOR_ID, PATIENT_ID))
                    .thenReturn(true);
            when(alertRepository.save(alert)).thenReturn(alert);
            when(alertMapper.toResponse(alert)).thenReturn(new AlertResponse(
                    ALERT_ID, null, READING_ID, "CRITICAL", "Alerta", "Motivo",
                    "RESOLVED", LocalDateTime.now()));

            AlertResponse result = alertService.resolveAlert(DOCTOR_EMAIL, ALERT_ID);

            assertThat(alert.getStatus()).isEqualTo("RESOLVED");
            assertThat(result.status()).isEqualTo("RESOLVED");
            verify(alertRepository).save(alert);
        }

        @Test
        @DisplayName("alerta ja RESOLVED nao deve ser gravado de novo e devolve o estado atual")
        void shouldNotSaveAgainWhenAlertIsAlreadyResolved() {
            Alert alert = pendingAlert();
            alert.setStatus("RESOLVED");

            stubAuthenticatedDoctor();
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(alert));
            when(doctorPatientRepository.existsByDoctorIdAndPatientId(DOCTOR_ID, PATIENT_ID))
                    .thenReturn(true);
            when(alertMapper.toResponse(alert)).thenReturn(new AlertResponse(
                    ALERT_ID, null, READING_ID, "CRITICAL", "Alerta", "Motivo",
                    "RESOLVED", LocalDateTime.now()));

            AlertResponse result = alertService.resolveAlert(DOCTOR_EMAIL, ALERT_ID);

            assertThat(result.status()).isEqualTo("RESOLVED");
            verify(alertRepository, never()).save(any());
        }

        @Test
        @DisplayName("medico nao vinculado nao deve conseguir resolver e o status nao muda")
        void shouldRejectResolutionForUnlinkedDoctor() {
            Alert alert = pendingAlert();
            stubAuthenticatedDoctor();
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(alert));
            when(doctorPatientRepository.existsByDoctorIdAndPatientId(DOCTOR_ID, PATIENT_ID))
                    .thenReturn(false);

            assertThatThrownBy(() -> alertService.resolveAlert(DOCTOR_EMAIL, ALERT_ID))
                    .isInstanceOf(UnauthorizedException.class);

            assertThat(alert.getStatus()).isEqualTo("PENDING");
            verify(alertRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar excecao quando o alerta nao existe")
        void shouldThrowWhenAlertNotFound() {
            stubAuthenticatedDoctor();
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> alertService.resolveAlert(DOCTOR_EMAIL, ALERT_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Alerta");

            verify(alertRepository, never()).save(any());
        }
    }
}

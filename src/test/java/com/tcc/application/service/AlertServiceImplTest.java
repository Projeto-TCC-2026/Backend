package com.tcc.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
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
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.data.domain.Pageable;

import com.tcc.application.dto.request.AlertEvaluationRequest;
import com.tcc.application.dto.response.AlertEvaluationResponse;
import com.tcc.application.dto.response.AlertResponse;
import com.tcc.application.dto.response.PatientAlertAnswerResponse;
import com.tcc.application.mapper.AlertMapper;
import com.tcc.domain.event.AlertConfirmationReason;
import com.tcc.domain.event.AlertConfirmedEvent;
import com.tcc.domain.event.AlertCreatedEvent;
import com.tcc.domain.model.Alert;
import com.tcc.domain.model.AlertStatus;
import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.HealthReading;
import com.tcc.domain.model.Hospital;
import com.tcc.domain.model.Patient;
import com.tcc.domain.model.PatientAlertAnswer;
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

/**
 * Fluxo de leitura → alerta.
 *
 * <p>Todos os horários são construídos com deslocamento explícito e as asserções
 * comparam {@code LocalDateTime} já em UTC, então nenhum teste depende do fuso da
 * máquina.
 */
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

    /**
     * Relógio fixo, não o do sistema: o prazo de resposta da paciente e a varredura
     * do agendador são calculados a partir dele, então nenhuma asserção de horário
     * depende do instante em que a suíte roda.
     *
     * <p>Não é {@code @Mock}: é o objeto real, com instante congelado, porque o que
     * interessa é o valor que ele devolve e não a interação com ele.
     */
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 29, 15, 0);

    private final Clock clock = Clock.fixed(
            NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private AlertServiceImpl alertService;

    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID NONEXISTENT_PATIENT_ID = UUID.randomUUID();
    private static final UUID ALERT_ID = UUID.randomUUID();
    private static final UUID READING_ID = UUID.randomUUID();
    private static final UUID PREVIOUS_READING_ID = UUID.randomUUID();
    private static final UUID DOCTOR_ID = UUID.randomUUID();

    private static final String HEART_RATE = "HEART_RATE";
    private static final String SPO2 = "SPO2";
    private static final String TEMPERATURE = "TEMPERATURE";

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

        // Construído à mão em vez de @InjectMocks: o Clock é objeto real, com
        // instante congelado, e não um mock que o Mockito saberia injetar.
        alertService = new AlertServiceImpl(
                patientRepository, readingThresholdRepository, alertRepository,
                healthReadingRepository, alertMapper, userRepository, doctorRepository,
                doctorPatientRepository, eventPublisher, clock);
    }

    // --- Fábricas ---

    private AlertEvaluationRequest requestOf(String readingType, Double value) {
        return requestOf(readingType, value, MEASURED_AT_UTC);
    }

    private AlertEvaluationRequest requestOf(String readingType, Double value,
                                             OffsetDateTime measuredAt) {
        return new AlertEvaluationRequest(PATIENT_ID, readingType, value, measuredAt, "bpm");
    }

    /** Faixa normal sem faixa plausível: o filtro de implausibilidade fica desligado. */
    private ReadingThreshold thresholdOf(String readingType, Double min, Double max) {
        ReadingThreshold threshold = new ReadingThreshold(readingType, min, max, "CRITICAL");
        threshold.setId(UUID.randomUUID());
        return threshold;
    }

    /** Faixa normal mais faixa plausível, como a V35 deixa os tipos cadastrados. */
    private ReadingThreshold thresholdWithPlausible(String readingType, Double min, Double max,
                                                    Double plausibleMin, Double plausibleMax) {
        ReadingThreshold threshold = thresholdOf(readingType, min, max);
        threshold.setPlausibleMin(plausibleMin);
        threshold.setPlausibleMax(plausibleMax);
        return threshold;
    }

    /** Faixa de HEART_RATE como a V36 a deixa: normal, plausível e grave. */
    private ReadingThreshold heartRateThreshold() {
        ReadingThreshold threshold =
                thresholdWithPlausible(HEART_RATE, 50.0, 120.0, 25.0, 250.0);
        threshold.setSevereMin(40.0);
        threshold.setSevereMax(131.0);
        return threshold;
    }

    /** SPO2 como a V36 a deixa: grave só pelo lado de baixo. */
    private ReadingThreshold spo2Threshold() {
        ReadingThreshold threshold = thresholdWithPlausible(SPO2, 90.0, null, 50.0, 100.0);
        threshold.setSevereMin(84.0);
        threshold.setSevereMax(null);
        return threshold;
    }

    private HealthReading readingOf(UUID id, String readingType, String value,
                                    LocalDateTime measuredAt, boolean suspect) {
        HealthReading reading = new HealthReading();
        reading.setId(id);
        reading.setPatient(patient);
        reading.setReadingType(readingType);
        reading.setValue(value);
        reading.setMeasuredAt(measuredAt);
        reading.setSuspect(suspect);
        return reading;
    }

    private Alert alertOf(UUID id, String status, HealthReading reading, LocalDateTime confirmedAt) {
        Alert alert = new Alert();
        alert.setId(id);
        alert.setPatient(patient);
        alert.setHealthReading(reading);
        alert.setSeverity("CRITICAL");
        alert.setStatus(status);
        alert.setConfirmedAt(confirmedAt);
        return alert;
    }

    // --- Stubs ---

    private void stubReadingNotYetReceived(String readingType) {
        stubReadingNotYetReceived(readingType, MEASURED_AT_STORED);
    }

    /**
     * Leitura ainda não recebida: o caminho normal de gravação.
     *
     * <p>O stub casa com o horário já normalizado em UTC. Se o service deixar de
     * converter, a consulta chega com outro valor, o stub não casa e o teste falha.
     */
    private void stubReadingNotYetReceived(String readingType, LocalDateTime storedAt) {
        when(healthReadingRepository.findByPatientIdAndReadingTypeAndMeasuredAt(
                PATIENT_ID, readingType, storedAt)).thenReturn(Optional.empty());
        when(healthReadingRepository.saveAndFlush(any(HealthReading.class)))
                .thenAnswer(invocation -> {
                    HealthReading reading = invocation.getArgument(0);
                    reading.setId(READING_ID);
                    return reading;
                });
    }

    /**
     * Stub de gravação de alerta, sem o mapper.
     *
     * <p>Usado nos caminhos que apenas atualizam um alerta existente (confirmação e
     * NOT_CONFIRMED). O mapper fica de fora de propósito: com a strictness do
     * Mockito, stubá-lo aqui falharia o teste por stub não usado — o que é
     * justamente a prova de que nenhum alerta novo foi construído.
     */
    private void stubAlertSave() {
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> {
            Alert alert = invocation.getArgument(0);
            if (alert.getId() == null) {
                alert.setId(ALERT_ID);
            }
            return alert;
        });
    }

    /** Stub completo: mapper mais gravação, para os caminhos que criam alerta novo. */
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
        stubAlertSave();
    }

    private void stubNoAlert(String status, String readingType) {
        when(alertRepository.findLatestByPatientAndStatusAndReadingType(
                eq(PATIENT_ID), eq(status), eq(readingType), any(Pageable.class)))
                .thenReturn(List.of());
    }

    private void stubLatestAlert(String status, String readingType, Alert alert) {
        when(alertRepository.findLatestByPatientAndStatusAndReadingType(
                eq(PATIENT_ID), eq(status), eq(readingType), any(Pageable.class)))
                .thenReturn(List.of(alert));
    }

    private void stubPreviousReading(String readingType, LocalDateTime before, HealthReading reading) {
        when(healthReadingRepository.findPreviousTrustedReadings(
                eq(PATIENT_ID), eq(readingType), eq(before), any(Pageable.class)))
                .thenReturn(reading == null ? List.of() : List.of(reading));
    }

    @Nested
    @DisplayName("leitura implausível")
    class ImplausibleReading {

        @Test
        @DisplayName("deve gravar como suspeita, sem avaliar, sem alerta e sem avisar")
        void shouldPersistAsSuspectWithoutEvaluating() {
            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            stubReadingNotYetReceived(HEART_RATE);
            when(readingThresholdRepository.findByReadingType(HEART_RATE))
                    .thenReturn(Optional.of(thresholdWithPlausible(HEART_RATE, 50.0, 120.0, 25.0, 250.0)));

            AlertEvaluationResponse result = alertService.evaluateReading(requestOf(HEART_RATE, 300.0));

            ArgumentCaptor<HealthReading> captor = ArgumentCaptor.forClass(HealthReading.class);
            verify(healthReadingRepository).saveAndFlush(captor.capture());

            assertThat(captor.getValue().isSuspect()).isTrue();
            assertThat(captor.getValue().getValue()).isEqualTo("300.0");

            assertThat(result.suspectReading()).isTrue();
            assertThat(result.alertGenerated()).isFalse();
            assertThat(result.alertStatus()).isNull();
            assertThat(result.healthReadingId()).isEqualTo(READING_ID);
            assertThat(result.reason()).contains("plausível");

            verify(alertRepository, never()).save(any());
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("valor abaixo do minimo plausivel tambem e suspeito")
        void shouldFlagValueBelowPlausibleMinimum() {
            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            stubReadingNotYetReceived(HEART_RATE);
            when(readingThresholdRepository.findByReadingType(HEART_RATE))
                    .thenReturn(Optional.of(thresholdWithPlausible(HEART_RATE, 50.0, 120.0, 25.0, 250.0)));

            AlertEvaluationResponse result = alertService.evaluateReading(requestOf(HEART_RATE, 10.0));

            assertThat(result.suspectReading()).isTrue();
            verify(alertRepository, never()).save(any());
        }

        @Test
        @DisplayName("limite plausivel e inclusivo: valor igual ao maximo e avaliado normalmente")
        void shouldAcceptValueEqualToPlausibleMaximum() {
            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            stubReadingNotYetReceived(HEART_RATE);
            when(readingThresholdRepository.findByReadingType(HEART_RATE))
                    .thenReturn(Optional.of(thresholdWithPlausible(HEART_RATE, 50.0, 120.0, 25.0, 250.0)));
            stubNoAlert(AlertStatus.PENDING, HEART_RATE);
            stubNoAlert(AlertStatus.UNCONFIRMED, HEART_RATE);
            stubAlertPersistence();

            AlertEvaluationResponse result = alertService.evaluateReading(requestOf(HEART_RATE, 250.0));

            assertThat(result.suspectReading()).isFalse();
            assertThat(result.alertStatus()).isEqualTo(AlertStatus.UNCONFIRMED);
        }

        @Test
        @DisplayName("tipo sem faixa plausivel cadastrada nao sofre o filtro")
        void shouldSkipFilterWhenPlausibleRangeIsNotConfigured() {
            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            stubReadingNotYetReceived(SPO2);
            when(readingThresholdRepository.findByReadingType(SPO2))
                    .thenReturn(Optional.of(thresholdOf(SPO2, 90.0, null)));
            stubNoAlert(AlertStatus.PENDING, SPO2);
            stubNoAlert(AlertStatus.UNCONFIRMED, SPO2);
            stubAlertPersistence();

            AlertEvaluationResponse result = alertService.evaluateReading(requestOf(SPO2, 5.0));

            assertThat(result.suspectReading()).isFalse();
            assertThat(result.alertStatus()).isEqualTo(AlertStatus.UNCONFIRMED);
        }
    }

    @Nested
    @DisplayName("primeira leitura fora da faixa")
    class FirstAbnormalReading {

        @Test
        @DisplayName("deve criar alerta UNCONFIRMED e publicar apenas o evento de criacao")
        void shouldCreateUnconfirmedAlertAndPublishCreatedEvent() {
            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            stubReadingNotYetReceived(HEART_RATE);
            when(readingThresholdRepository.findByReadingType(HEART_RATE))
                    .thenReturn(Optional.of(thresholdOf(HEART_RATE, 50.0, 120.0)));
            stubNoAlert(AlertStatus.PENDING, HEART_RATE);
            stubNoAlert(AlertStatus.UNCONFIRMED, HEART_RATE);
            stubAlertPersistence();

            AlertEvaluationResponse result = alertService.evaluateReading(requestOf(HEART_RATE, 150.0));

            ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
            verify(alertRepository).save(alertCaptor.capture());

            Alert saved = alertCaptor.getValue();
            assertThat(saved.getStatus()).isEqualTo(AlertStatus.UNCONFIRMED);
            assertThat(saved.getConfirmedAt()).isNull();
            assertThat(saved.getHealthReading().getId()).isEqualTo(READING_ID);
            assertThat(saved.getSeverity()).isEqualTo("CRITICAL");
            assertThat(saved.getTitle()).isNotBlank();
            assertThat(saved.getDescription()).contains("acima");

            // Só o evento de criação: o médico ainda não é avisado.
            verify(eventPublisher).publishEvent(any(AlertCreatedEvent.class));
            verify(eventPublisher, never()).publishEvent(any(AlertConfirmedEvent.class));

            assertThat(result.alertGenerated()).isTrue();
            assertThat(result.alertStatus()).isEqualTo(AlertStatus.UNCONFIRMED);
            assertThat(result.alertId()).isEqualTo(ALERT_ID);
            assertThat(result.reason()).contains("acima");
        }

        @Test
        @DisplayName("valor abaixo do minimo normal tambem cria alerta UNCONFIRMED")
        void shouldCreateUnconfirmedAlertWhenValueIsBelowMinimum() {
            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            stubReadingNotYetReceived(HEART_RATE);
            when(readingThresholdRepository.findByReadingType(HEART_RATE))
                    .thenReturn(Optional.of(thresholdOf(HEART_RATE, 50.0, 120.0)));
            stubNoAlert(AlertStatus.PENDING, HEART_RATE);
            stubNoAlert(AlertStatus.UNCONFIRMED, HEART_RATE);
            stubAlertPersistence();

            AlertEvaluationResponse result = alertService.evaluateReading(requestOf(HEART_RATE, 45.0));

            ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
            verify(alertRepository).save(alertCaptor.capture());

            Alert saved = alertCaptor.getValue();
            assertThat(saved.getStatus()).isEqualTo(AlertStatus.UNCONFIRMED);
            assertThat(saved.getDescription()).contains("abaixo");

            assertThat(result.alertGenerated()).isTrue();
            assertThat(result.alertStatus()).isEqualTo(AlertStatus.UNCONFIRMED);
            assertThat(result.reason()).contains("abaixo");
            verify(eventPublisher).publishEvent(any(AlertCreatedEvent.class));
        }
    }

    @Nested
    @DisplayName("leitura com valor grave")
    class SevereReading {

        /** Prazo esperado: relógio fixo do teste mais 10 minutos. */
        private static final LocalDateTime EXPECTED_DEADLINE = NOW.plusMinutes(10);

        private void stubSevereHeartRatePath() {
            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            stubReadingNotYetReceived(HEART_RATE);
            when(readingThresholdRepository.findByReadingType(HEART_RATE))
                    .thenReturn(Optional.of(heartRateThreshold()));
            stubNoAlert(AlertStatus.PENDING, HEART_RATE);
        }

        @Test
        @DisplayName("deve criar AWAITING_PATIENT com prazo de 10 min, sem avisar o medico")
        void shouldCreateAwaitingPatientWithTenMinuteDeadline() {
            stubSevereHeartRatePath();
            stubNoAlert(AlertStatus.AWAITING_PATIENT, HEART_RATE);
            stubNoAlert(AlertStatus.UNCONFIRMED, HEART_RATE);
            stubAlertPersistence();

            AlertEvaluationResponse result = alertService.evaluateReading(requestOf(HEART_RATE, 35.0));

            ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
            verify(alertRepository).save(captor.capture());

            Alert saved = captor.getValue();
            assertThat(saved.getStatus()).isEqualTo(AlertStatus.AWAITING_PATIENT);
            // Prazo contado do Clock injetado, não do measuredAt: leitura atrasada
            // pela fila daria um prazo já vencido.
            assertThat(saved.getPatientResponseDeadline()).isEqualTo(EXPECTED_DEADLINE);
            assertThat(saved.getConfirmedAt()).isNull();
            assertThat(saved.getPatientResponse()).isNull();
            assertThat(saved.getHealthReading().getId()).isEqualTo(READING_ID);

            // Só o evento de criação, que é o push ao paciente. Nenhum e-mail.
            verify(eventPublisher).publishEvent(any(AlertCreatedEvent.class));
            verify(eventPublisher, never()).publishEvent(any(AlertConfirmedEvent.class));

            assertThat(result.alertGenerated()).isTrue();
            assertThat(result.alertStatus()).isEqualTo(AlertStatus.AWAITING_PATIENT);
            assertThat(result.alertId()).isEqualTo(ALERT_ID);
        }

        @Test
        @DisplayName("com AWAITING_PATIENT aberto do mesmo tipo deve apenas gravar a leitura")
        void shouldOnlyPersistWhenAwaitingPatientIsAlreadyOpen() {
            HealthReading severe = readingOf(PREVIOUS_READING_ID, HEART_RATE, "35.0",
                    MEASURED_AT_STORED.minusMinutes(3), false);
            Alert awaiting = alertOf(ALERT_ID, AlertStatus.AWAITING_PATIENT, severe, null);
            awaiting.setPatientResponseDeadline(NOW.plusMinutes(7));

            stubSevereHeartRatePath();
            // A tentativa de confirmação vem primeiro, então o UNCONFIRMED é
            // consultado antes do AWAITING_PATIENT. Aqui não há nenhum.
            stubNoAlert(AlertStatus.UNCONFIRMED, HEART_RATE);
            stubLatestAlert(AlertStatus.AWAITING_PATIENT, HEART_RATE, awaiting);

            AlertEvaluationResponse result = alertService.evaluateReading(requestOf(HEART_RATE, 33.0));

            assertThat(result.alertGenerated()).isFalse();
            assertThat(result.alertStatus()).isNull();
            assertThat(result.healthReadingId()).isEqualTo(READING_ID);

            // A pergunta está de pé: nada é criado e nenhum segundo push sai.
            assertThat(awaiting.getStatus()).isEqualTo(AlertStatus.AWAITING_PATIENT);
            verify(alertRepository, never()).save(any());
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("dentro da janela de 4h de um PENDING deve apenas gravar a leitura")
        void shouldOnlyPersistWhenInsideFourHourWindowOfPendingAlert() {
            HealthReading first = readingOf(PREVIOUS_READING_ID, HEART_RATE, "38.0",
                    MEASURED_AT_STORED.minusHours(3), false);
            Alert pending = alertOf(ALERT_ID, AlertStatus.PENDING, first,
                    MEASURED_AT_STORED.minusHours(3));

            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            stubReadingNotYetReceived(HEART_RATE);
            when(readingThresholdRepository.findByReadingType(HEART_RATE))
                    .thenReturn(Optional.of(heartRateThreshold()));
            stubLatestAlert(AlertStatus.PENDING, HEART_RATE, pending);

            AlertEvaluationResponse result = alertService.evaluateReading(requestOf(HEART_RATE, 32.0));

            assertThat(result.alertGenerated()).isFalse();
            assertThat(result.alertStatus()).isNull();
            assertThat(result.healthReadingId()).isEqualTo(READING_ID);

            // A janela de 4h vence até a leitura grave: o médico acabou de ser avisado.
            verify(alertRepository, never()).save(any());
            verifyNoInteractions(eventPublisher);
        }

        /**
         * UNCONFIRMED velho demais para ser confirmado: a leitura grave abre a
         * pergunta e o antigo é fechado, para não ficar pendurado — a pergunta
         * substitui a espera pela segunda leitura, então ninguém mais o confirmaria.
         *
         * <p>A distância de 3h entre as duas medições é explícita de propósito: é ela
         * que impede a confirmação, que agora é tentada antes da checagem de
         * gravidade. Com um intervalo menor este caso viraria uma confirmação.
         */
        @Test
        @DisplayName("UNCONFIRMED com mais de 2h nao confirma: vira NOT_CONFIRMED e nasce a pergunta")
        void shouldCloseOpenUnconfirmedWhenSevereAlertIsCreated() {
            UUID oldAlertId = UUID.randomUUID();
            LocalDateTime longAgo = MEASURED_AT_STORED.minusHours(3);
            HealthReading previous = readingOf(PREVIOUS_READING_ID, HEART_RATE, "125.0",
                    longAgo, false);
            Alert unconfirmed = alertOf(oldAlertId, AlertStatus.UNCONFIRMED, previous, null);

            stubSevereHeartRatePath();
            stubLatestAlert(AlertStatus.UNCONFIRMED, HEART_RATE, unconfirmed);
            stubPreviousReading(HEART_RATE, MEASURED_AT_STORED, previous);
            stubNoAlert(AlertStatus.AWAITING_PATIENT, HEART_RATE);
            stubAlertPersistence();

            AlertEvaluationResponse result = alertService.evaluateReading(requestOf(HEART_RATE, 140.0));

            ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
            verify(alertRepository, times(2)).save(captor.capture());

            assertThat(captor.getAllValues().get(0).getId()).isEqualTo(oldAlertId);
            assertThat(captor.getAllValues().get(0).getStatus()).isEqualTo(AlertStatus.NOT_CONFIRMED);
            assertThat(captor.getAllValues().get(1).getStatus()).isEqualTo(AlertStatus.AWAITING_PATIENT);

            assertThat(result.alertStatus()).isEqualTo(AlertStatus.AWAITING_PATIENT);

            // Nenhuma confirmação: o médico não é avisado por esta leitura.
            verify(eventPublisher).publishEvent(any(AlertCreatedEvent.class));
            verify(eventPublisher, never()).publishEvent(any(AlertConfirmedEvent.class));
        }
    }

    /**
     * A tentativa de confirmação vem ANTES da checagem de gravidade.
     *
     * <p>Sem essa ordem, a paciente que responde "estou bem" a cada leitura grave
     * nunca chegaria ao médico: a resposta devolve o alerta a UNCONFIRMED, e a
     * leitura grave seguinte abriria outra pergunta descartando esse UNCONFIRMED —
     * indefinidamente. Confirmando primeiro, a segunda leitura seguida fora da faixa
     * avisa o médico, grave ou não.
     */
    @Nested
    @DisplayName("leitura grave confirma UNCONFIRMED antes de perguntar de novo")
    class SevereReadingConfirmsFirst {

        private void stubSevereHeartRatePath() {
            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            stubReadingNotYetReceived(HEART_RATE);
            when(readingThresholdRepository.findByReadingType(HEART_RATE))
                    .thenReturn(Optional.of(heartRateThreshold()));
            stubNoAlert(AlertStatus.PENDING, HEART_RATE);
        }

        /**
         * O cenário que o fluxo antigo deixava passar: alerta grave respondido com
         * "estou bem" (logo, UNCONFIRMED com resposta registrada) e uma nova leitura
         * grave em seguida. Agora confirma em vez de perguntar de novo.
         */
        @Test
        @DisplayName("depois de 'estou bem', leitura GRAVE em ate 2h deve confirmar e avisar o medico")
        void shouldConfirmWhenSevereReadingFollowsPatientSaidOk() {
            HealthReading severeBefore = readingOf(PREVIOUS_READING_ID, HEART_RATE, "35.0",
                    MEASURED_AT_STORED.minusMinutes(25), false);
            Alert afterOk = alertOf(ALERT_ID, AlertStatus.UNCONFIRMED, severeBefore, null);
            afterOk.setPatientResponse(PatientAlertAnswer.OK);
            afterOk.setPatientRespondedAt(MEASURED_AT_STORED.minusMinutes(24));

            stubSevereHeartRatePath();
            stubLatestAlert(AlertStatus.UNCONFIRMED, HEART_RATE, afterOk);
            stubPreviousReading(HEART_RATE, MEASURED_AT_STORED, severeBefore);
            stubAlertSave();

            // 33 é grave (<= severe_min 40). Mesmo assim confirma, em vez de perguntar.
            AlertEvaluationResponse result = alertService.evaluateReading(requestOf(HEART_RATE, 33.0));

            assertThat(afterOk.getStatus()).isEqualTo(AlertStatus.PENDING);
            assertThat(afterOk.getConfirmedAt()).isEqualTo(MEASURED_AT_STORED);
            assertThat(result.alertStatus()).isEqualTo(AlertStatus.PENDING);
            // Nenhum alerta novo: o existente foi promovido.
            assertThat(result.alertGenerated()).isFalse();
            assertThat(result.alertId()).isEqualTo(ALERT_ID);

            ArgumentCaptor<AlertConfirmedEvent> captor =
                    ArgumentCaptor.forClass(AlertConfirmedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().reason())
                    .isEqualTo(AlertConfirmationReason.DUAS_LEITURAS);

            // Nenhuma pergunta nova: o prazo de resposta não é tocado.
            assertThat(afterOk.getPatientResponseDeadline()).isNull();
            verify(eventPublisher, never()).publishEvent(any(AlertCreatedEvent.class));
            verify(alertMapper, never()).toEntity(any(), any(), any());
        }

        /**
         * A confirmação não exige que as duas leituras sejam do mesmo tipo de
         * gravidade: a primeira foi um desvio comum, a segunda é grave, e as duas
         * seguidas fora da faixa bastam.
         */
        @Test
        @DisplayName("UNCONFIRMED comum seguido de leitura GRAVE em ate 2h deve confirmar")
        void shouldConfirmWhenSevereReadingFollowsCommonUnconfirmed() {
            HealthReading commonBefore = readingOf(PREVIOUS_READING_ID, HEART_RATE, "125.0",
                    MEASURED_AT_STORED.minusMinutes(40), false);
            Alert unconfirmed = alertOf(ALERT_ID, AlertStatus.UNCONFIRMED, commonBefore, null);

            stubSevereHeartRatePath();
            stubLatestAlert(AlertStatus.UNCONFIRMED, HEART_RATE, unconfirmed);
            stubPreviousReading(HEART_RATE, MEASURED_AT_STORED, commonBefore);
            stubAlertSave();

            AlertEvaluationResponse result = alertService.evaluateReading(requestOf(HEART_RATE, 35.0));

            assertThat(unconfirmed.getStatus()).isEqualTo(AlertStatus.PENDING);
            assertThat(result.alertStatus()).isEqualTo(AlertStatus.PENDING);
            assertThat(result.alertGenerated()).isFalse();

            ArgumentCaptor<AlertConfirmedEvent> captor =
                    ArgumentCaptor.forClass(AlertConfirmedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());

            AlertConfirmedEvent event = captor.getValue();
            assertThat(event.reason()).isEqualTo(AlertConfirmationReason.DUAS_LEITURAS);
            // As duas leituras viajam no evento: o e-mail mostra a progressão.
            assertThat(event.firstReading().getId()).isEqualTo(PREVIOUS_READING_ID);
            assertThat(event.confirmingReading().getId()).isEqualTo(READING_ID);

            // Nenhuma pergunta criada.
            verify(eventPublisher, never()).publishEvent(any(AlertCreatedEvent.class));
            verify(alertMapper, never()).toEntity(any(), any(), any());
        }

        /**
         * Confirmando, o fluxo para: não chega a consultar AWAITING_PATIENT nem a
         * criar pergunta. É o que impede o alerta de ser confirmado e questionado na
         * mesma leitura.
         */
        @Test
        @DisplayName("ao confirmar nao deve nem consultar AWAITING_PATIENT")
        void shouldNotEvenLookForAwaitingPatientWhenConfirming() {
            HealthReading severeBefore = readingOf(PREVIOUS_READING_ID, HEART_RATE, "35.0",
                    MEASURED_AT_STORED.minusMinutes(10), false);
            Alert unconfirmed = alertOf(ALERT_ID, AlertStatus.UNCONFIRMED, severeBefore, null);

            stubSevereHeartRatePath();
            stubLatestAlert(AlertStatus.UNCONFIRMED, HEART_RATE, unconfirmed);
            stubPreviousReading(HEART_RATE, MEASURED_AT_STORED, severeBefore);
            stubAlertSave();

            alertService.evaluateReading(requestOf(HEART_RATE, 36.0));

            verify(alertRepository, never()).findLatestByPatientAndStatusAndReadingType(
                    eq(PATIENT_ID), eq(AlertStatus.AWAITING_PATIENT), eq(HEART_RATE),
                    any(Pageable.class));
        }
    }

    /**
     * Limites graves são inclusivos POR DENTRO, ao contrário da faixa normal e da
     * plausível: o limite pertence ao que é grave. É o que separa 40 de 41 e 131
     * de 130.
     */
    @Nested
    @DisplayName("limites inclusivos da faixa grave")
    class SevereBoundaries {

        private String statusFor(String readingType, ReadingThreshold threshold, Double value,
                                 boolean expectSevere) {
            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            stubReadingNotYetReceived(readingType);
            when(readingThresholdRepository.findByReadingType(readingType))
                    .thenReturn(Optional.of(threshold));
            stubNoAlert(AlertStatus.PENDING, readingType);

            if (expectSevere) {
                stubNoAlert(AlertStatus.AWAITING_PATIENT, readingType);
            }

            stubNoAlert(AlertStatus.UNCONFIRMED, readingType);
            stubAlertPersistence();

            return alertService.evaluateReading(requestOf(readingType, value)).alertStatus();
        }

        @Test
        @DisplayName("HEART_RATE 40 e grave (igual ao severe_min)")
        void heartRateFortyIsSevere() {
            assertThat(statusFor(HEART_RATE, heartRateThreshold(), 40.0, true))
                    .isEqualTo(AlertStatus.AWAITING_PATIENT);
        }

        @Test
        @DisplayName("HEART_RATE 41 nao e grave: segue o fluxo comum")
        void heartRateFortyOneIsNotSevere() {
            assertThat(statusFor(HEART_RATE, heartRateThreshold(), 41.0, false))
                    .isEqualTo(AlertStatus.UNCONFIRMED);
        }

        @Test
        @DisplayName("HEART_RATE 131 e grave (igual ao severe_max)")
        void heartRateOneHundredThirtyOneIsSevere() {
            assertThat(statusFor(HEART_RATE, heartRateThreshold(), 131.0, true))
                    .isEqualTo(AlertStatus.AWAITING_PATIENT);
        }

        @Test
        @DisplayName("HEART_RATE 130 nao e grave: segue o fluxo comum")
        void heartRateOneHundredThirtyIsNotSevere() {
            assertThat(statusFor(HEART_RATE, heartRateThreshold(), 130.0, false))
                    .isEqualTo(AlertStatus.UNCONFIRMED);
        }

        @Test
        @DisplayName("SPO2 84 e grave (igual ao severe_min)")
        void spo2EightyFourIsSevere() {
            assertThat(statusFor(SPO2, spo2Threshold(), 84.0, true))
                    .isEqualTo(AlertStatus.AWAITING_PATIENT);
        }

        @Test
        @DisplayName("SPO2 85 nao e grave: segue o fluxo comum")
        void spo2EightyFiveIsNotSevere() {
            assertThat(statusFor(SPO2, spo2Threshold(), 85.0, false))
                    .isEqualTo(AlertStatus.UNCONFIRMED);
        }

        @Test
        @DisplayName("TEMPERATURE 35.0 e grave (igual ao severe_min)")
        void temperatureThirtyFiveIsSevere() {
            ReadingThreshold threshold =
                    thresholdWithPlausible(TEMPERATURE, 35.5, 38.0, 30.0, 45.0);
            threshold.setSevereMin(35.0);

            assertThat(statusFor(TEMPERATURE, threshold, 35.0, true))
                    .isEqualTo(AlertStatus.AWAITING_PATIENT);
        }

        /**
         * SPO2 e TEMPERATURE não têm severe_max na V36: valor alto é desvio, mas não
         * justifica interromper a paciente com uma pergunta.
         */
        @Test
        @DisplayName("TEMPERATURE alta nao e grave: sem severe_max, o lado de cima e ignorado")
        void temperatureHighIsNotSevereWithoutSevereMax() {
            ReadingThreshold threshold =
                    thresholdWithPlausible(TEMPERATURE, 35.5, 38.0, 30.0, 45.0);
            threshold.setSevereMin(35.0);
            threshold.setSevereMax(null);

            assertThat(statusFor(TEMPERATURE, threshold, 41.0, false))
                    .isEqualTo(AlertStatus.UNCONFIRMED);
        }

        @Test
        @DisplayName("tipo sem faixa grave cadastrada segue so o fluxo comum")
        void typeWithoutSevereRangeFollowsCommonFlowOnly() {
            // thresholdOf não preenche severe_min nem severe_max.
            assertThat(statusFor(HEART_RATE, thresholdOf(HEART_RATE, 50.0, 120.0), 20.0, false))
                    .isEqualTo(AlertStatus.UNCONFIRMED);
        }
    }

    @Nested
    @DisplayName("confirmação por segunda leitura")
    class Confirmation {

        private static final LocalDateTime FIRST_AT = LocalDateTime.of(2026, 9, 29, 13, 0);

        @Test
        @DisplayName("segunda leitura fora dentro de 2h deve confirmar, gravar confirmed_at e publicar evento")
        void shouldConfirmWhenSecondReadingIsWithinTwoHours() {
            HealthReading first = readingOf(PREVIOUS_READING_ID, HEART_RATE, "150.0", FIRST_AT, false);
            Alert unconfirmed = alertOf(ALERT_ID, AlertStatus.UNCONFIRMED, first, null);

            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            stubReadingNotYetReceived(HEART_RATE);
            when(readingThresholdRepository.findByReadingType(HEART_RATE))
                    .thenReturn(Optional.of(thresholdOf(HEART_RATE, 50.0, 120.0)));
            stubNoAlert(AlertStatus.PENDING, HEART_RATE);
            stubLatestAlert(AlertStatus.UNCONFIRMED, HEART_RATE, unconfirmed);
            stubPreviousReading(HEART_RATE, MEASURED_AT_STORED, first);
            stubAlertSave();

            AlertEvaluationResponse result = alertService.evaluateReading(requestOf(HEART_RATE, 155.0));

            assertThat(unconfirmed.getStatus()).isEqualTo(AlertStatus.PENDING);
            assertThat(unconfirmed.getConfirmedAt()).isEqualTo(MEASURED_AT_STORED);

            ArgumentCaptor<AlertConfirmedEvent> captor =
                    ArgumentCaptor.forClass(AlertConfirmedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());

            AlertConfirmedEvent event = captor.getValue();
            assertThat(event.firstReading().getId()).isEqualTo(PREVIOUS_READING_ID);
            assertThat(event.confirmingReading().getId()).isEqualTo(READING_ID);

            // Nenhum alerta novo foi criado: o existente foi promovido.
            assertThat(result.alertGenerated()).isFalse();
            assertThat(result.alertStatus()).isEqualTo(AlertStatus.PENDING);
            assertThat(result.alertId()).isEqualTo(ALERT_ID);
            verify(alertMapper, never()).toEntity(any(), any(), any());
        }

        @Test
        @DisplayName("segunda leitura fora com mais de 2h nao confirma e cria novo UNCONFIRMED")
        void shouldNotConfirmWhenGapExceedsTwoHours() {
            LocalDateTime longAgo = MEASURED_AT_STORED.minusHours(3);
            HealthReading first = readingOf(PREVIOUS_READING_ID, HEART_RATE, "150.0", longAgo, false);
            Alert unconfirmed = alertOf(ALERT_ID, AlertStatus.UNCONFIRMED, first, null);

            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            stubReadingNotYetReceived(HEART_RATE);
            when(readingThresholdRepository.findByReadingType(HEART_RATE))
                    .thenReturn(Optional.of(thresholdOf(HEART_RATE, 50.0, 120.0)));
            stubNoAlert(AlertStatus.PENDING, HEART_RATE);
            stubLatestAlert(AlertStatus.UNCONFIRMED, HEART_RATE, unconfirmed);
            stubPreviousReading(HEART_RATE, MEASURED_AT_STORED, first);
            stubAlertPersistence();

            AlertEvaluationResponse result = alertService.evaluateReading(requestOf(HEART_RATE, 155.0));

            // O antigo não fica UNCONFIRMED para sempre: sem confirmação, ele é fechado.
            assertThat(unconfirmed.getStatus()).isEqualTo(AlertStatus.NOT_CONFIRMED);
            assertThat(unconfirmed.getConfirmedAt()).isNull();

            verify(eventPublisher).publishEvent(any(AlertCreatedEvent.class));
            verify(eventPublisher, never()).publishEvent(any(AlertConfirmedEvent.class));

            assertThat(result.alertGenerated()).isTrue();
            assertThat(result.alertStatus()).isEqualTo(AlertStatus.UNCONFIRMED);
        }

        /**
         * O alerta antigo não pode ficar UNCONFIRMED indefinidamente: a leitura atual
         * cria um alerta novo, que passa a ser o mais recente do tipo, e nenhuma
         * leitura seguinte voltaria a alcançar o antigo.
         */
        @Test
        @DisplayName("UNCONFIRMED nao confirmado vira NOT_CONFIRMED quando um alerta novo nasce")
        void shouldCloseOldUnconfirmedWhenNewAlertIsCreated() {
            UUID oldAlertId = UUID.randomUUID();
            LocalDateTime longAgo = MEASURED_AT_STORED.minusHours(3);
            HealthReading first = readingOf(PREVIOUS_READING_ID, HEART_RATE, "150.0", longAgo, false);
            Alert oldUnconfirmed = alertOf(oldAlertId, AlertStatus.UNCONFIRMED, first, null);

            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            stubReadingNotYetReceived(HEART_RATE);
            when(readingThresholdRepository.findByReadingType(HEART_RATE))
                    .thenReturn(Optional.of(thresholdOf(HEART_RATE, 50.0, 120.0)));
            stubNoAlert(AlertStatus.PENDING, HEART_RATE);
            stubLatestAlert(AlertStatus.UNCONFIRMED, HEART_RATE, oldUnconfirmed);
            stubPreviousReading(HEART_RATE, MEASURED_AT_STORED, first);
            stubAlertPersistence();

            AlertEvaluationResponse result = alertService.evaluateReading(requestOf(HEART_RATE, 155.0));

            ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
            verify(alertRepository, times(2)).save(captor.capture());

            // Ordem importa: o antigo é fechado antes de o novo ser gravado.
            Alert closed = captor.getAllValues().get(0);
            Alert created = captor.getAllValues().get(1);

            assertThat(closed.getId()).isEqualTo(oldAlertId);
            assertThat(closed.getStatus()).isEqualTo(AlertStatus.NOT_CONFIRMED);

            assertThat(created.getId()).isEqualTo(ALERT_ID);
            assertThat(created.getStatus()).isEqualTo(AlertStatus.UNCONFIRMED);
            assertThat(created.getHealthReading().getId()).isEqualTo(READING_ID);

            assertThat(result.alertGenerated()).isTrue();
            assertThat(result.alertStatus()).isEqualTo(AlertStatus.UNCONFIRMED);
            assertThat(result.alertId()).isEqualTo(ALERT_ID);

            // O antigo é fechado em silêncio: só o alerta novo avisa o paciente.
            verify(eventPublisher).publishEvent(any(AlertCreatedEvent.class));
            verify(eventPublisher, never()).publishEvent(any(AlertConfirmedEvent.class));
        }

        /**
         * A leitura suspeita é invisível para a busca da leitura anterior — o filtro
         * {@code suspect = false} está na própria query. O teste representa isso
         * devolvendo a primeira leitura como anterior, que é o que o banco faria,
         * e prova que a confirmação acontece mesmo tendo havido uma suspeita no meio.
         */
        @Test
        @DisplayName("leitura suspeita entre as duas nao quebra a sequencia")
        void shouldConfirmEvenWithSuspectReadingInBetween() {
            HealthReading first = readingOf(PREVIOUS_READING_ID, HEART_RATE, "150.0", FIRST_AT, false);
            Alert unconfirmed = alertOf(ALERT_ID, AlertStatus.UNCONFIRMED, first, null);

            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            stubReadingNotYetReceived(HEART_RATE);
            when(readingThresholdRepository.findByReadingType(HEART_RATE))
                    .thenReturn(Optional.of(thresholdOf(HEART_RATE, 50.0, 120.0)));
            stubNoAlert(AlertStatus.PENDING, HEART_RATE);
            stubLatestAlert(AlertStatus.UNCONFIRMED, HEART_RATE, unconfirmed);
            stubPreviousReading(HEART_RATE, MEASURED_AT_STORED, first);
            stubAlertSave();

            alertService.evaluateReading(requestOf(HEART_RATE, 155.0));

            assertThat(unconfirmed.getStatus()).isEqualTo(AlertStatus.PENDING);
            verify(eventPublisher).publishEvent(any(AlertConfirmedEvent.class));

            // A consulta exclui as suspeitas: é ela que garante o "não quebra".
            verify(healthReadingRepository).findPreviousTrustedReadings(
                    eq(PATIENT_ID), eq(HEART_RATE), eq(MEASURED_AT_STORED), any(Pageable.class));
        }

        @Test
        @DisplayName("nao confirma quando a leitura anterior nao e a do alerta")
        void shouldNotConfirmWhenPreviousReadingIsNotTheAlertReading() {
            HealthReading alertReading =
                    readingOf(PREVIOUS_READING_ID, HEART_RATE, "150.0", FIRST_AT, false);
            HealthReading otherReading = readingOf(
                    UUID.randomUUID(), HEART_RATE, "80.0", FIRST_AT.plusMinutes(30), false);
            Alert unconfirmed = alertOf(ALERT_ID, AlertStatus.UNCONFIRMED, alertReading, null);

            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            stubReadingNotYetReceived(HEART_RATE);
            when(readingThresholdRepository.findByReadingType(HEART_RATE))
                    .thenReturn(Optional.of(thresholdOf(HEART_RATE, 50.0, 120.0)));
            stubNoAlert(AlertStatus.PENDING, HEART_RATE);
            stubLatestAlert(AlertStatus.UNCONFIRMED, HEART_RATE, unconfirmed);
            stubPreviousReading(HEART_RATE, MEASURED_AT_STORED, otherReading);
            stubAlertPersistence();

            AlertEvaluationResponse result = alertService.evaluateReading(requestOf(HEART_RATE, 155.0));

            // Sequência quebrada: o antigo é fechado e a leitura atual abre outro.
            assertThat(unconfirmed.getStatus()).isEqualTo(AlertStatus.NOT_CONFIRMED);
            assertThat(result.alertStatus()).isEqualTo(AlertStatus.UNCONFIRMED);
            assertThat(result.alertGenerated()).isTrue();
            verify(eventPublisher, never()).publishEvent(any(AlertConfirmedEvent.class));
        }
    }

    @Nested
    @DisplayName("leitura normal depois de UNCONFIRMED")
    class NormalAfterUnconfirmed {

        @Test
        @DisplayName("deve marcar o alerta como NOT_CONFIRMED sem avisar ninguem")
        void shouldMarkAlertAsNotConfirmed() {
            HealthReading first = readingOf(PREVIOUS_READING_ID, HEART_RATE, "150.0",
                    MEASURED_AT_STORED.minusMinutes(30), false);
            Alert unconfirmed = alertOf(ALERT_ID, AlertStatus.UNCONFIRMED, first, null);

            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            stubReadingNotYetReceived(HEART_RATE);
            when(readingThresholdRepository.findByReadingType(HEART_RATE))
                    .thenReturn(Optional.of(thresholdOf(HEART_RATE, 50.0, 120.0)));
            stubLatestAlert(AlertStatus.UNCONFIRMED, HEART_RATE, unconfirmed);
            stubAlertSave();

            AlertEvaluationResponse result = alertService.evaluateReading(requestOf(HEART_RATE, 80.0));

            assertThat(unconfirmed.getStatus()).isEqualTo(AlertStatus.NOT_CONFIRMED);
            assertThat(result.alertStatus()).isEqualTo(AlertStatus.NOT_CONFIRMED);
            assertThat(result.alertGenerated()).isFalse();
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("sem alerta UNCONFIRMED a leitura normal apenas e gravada")
        void shouldOnlyPersistWhenThereIsNoUnconfirmedAlert() {
            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            stubReadingNotYetReceived(HEART_RATE);
            when(readingThresholdRepository.findByReadingType(HEART_RATE))
                    .thenReturn(Optional.of(thresholdOf(HEART_RATE, 50.0, 120.0)));
            stubNoAlert(AlertStatus.UNCONFIRMED, HEART_RATE);

            AlertEvaluationResponse result = alertService.evaluateReading(requestOf(HEART_RATE, 80.0));

            assertThat(result.alertStatus()).isNull();
            assertThat(result.alertGenerated()).isFalse();
            assertThat(result.healthReadingId()).isEqualTo(READING_ID);
            verify(alertRepository, never()).save(any());
            verifyNoInteractions(eventPublisher);
        }
    }

    @Nested
    @DisplayName("janela de 4h do alerta confirmado")
    class RenotifyWindow {

        @Test
        @DisplayName("PENDING confirmado ha menos de 4h nao cria nem atualiza nada")
        void shouldStaySilentWhenConfirmedLessThanFourHoursAgo() {
            HealthReading first = readingOf(PREVIOUS_READING_ID, HEART_RATE, "150.0",
                    MEASURED_AT_STORED.minusHours(3), false);
            Alert pending = alertOf(ALERT_ID, AlertStatus.PENDING, first,
                    MEASURED_AT_STORED.minusHours(3));

            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            stubReadingNotYetReceived(HEART_RATE);
            when(readingThresholdRepository.findByReadingType(HEART_RATE))
                    .thenReturn(Optional.of(thresholdOf(HEART_RATE, 50.0, 120.0)));
            stubLatestAlert(AlertStatus.PENDING, HEART_RATE, pending);

            AlertEvaluationResponse result = alertService.evaluateReading(requestOf(HEART_RATE, 155.0));

            assertThat(result.alertGenerated()).isFalse();
            assertThat(result.alertStatus()).isNull();
            assertThat(result.healthReadingId()).isEqualTo(READING_ID);

            verify(alertRepository, never()).save(any());
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("PENDING confirmado ha mais de 4h permite novo UNCONFIRMED")
        void shouldCreateNewUnconfirmedWhenConfirmedMoreThanFourHoursAgo() {
            HealthReading first = readingOf(PREVIOUS_READING_ID, HEART_RATE, "150.0",
                    MEASURED_AT_STORED.minusHours(5), false);
            Alert pending = alertOf(ALERT_ID, AlertStatus.PENDING, first,
                    MEASURED_AT_STORED.minusHours(5));

            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            stubReadingNotYetReceived(HEART_RATE);
            when(readingThresholdRepository.findByReadingType(HEART_RATE))
                    .thenReturn(Optional.of(thresholdOf(HEART_RATE, 50.0, 120.0)));
            stubLatestAlert(AlertStatus.PENDING, HEART_RATE, pending);
            stubNoAlert(AlertStatus.UNCONFIRMED, HEART_RATE);
            stubAlertPersistence();

            AlertEvaluationResponse result = alertService.evaluateReading(requestOf(HEART_RATE, 155.0));

            assertThat(result.alertGenerated()).isTrue();
            assertThat(result.alertStatus()).isEqualTo(AlertStatus.UNCONFIRMED);
            verify(eventPublisher).publishEvent(any(AlertCreatedEvent.class));
        }

        /**
         * Alerta criado antes da V35 nasceu PENDING e nunca teve confirmed_at. Se ele
         * silenciasse a leitura atual, a janela nunca venceria e o paciente ficaria
         * sem alerta para sempre naquele tipo.
         */
        @Test
        @DisplayName("PENDING sem confirmed_at nao silencia a leitura atual")
        void shouldNotSilenceWhenPendingHasNoConfirmedAt() {
            HealthReading first = readingOf(PREVIOUS_READING_ID, HEART_RATE, "150.0",
                    MEASURED_AT_STORED.minusHours(1), false);
            Alert legacyPending = alertOf(ALERT_ID, AlertStatus.PENDING, first, null);

            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            stubReadingNotYetReceived(HEART_RATE);
            when(readingThresholdRepository.findByReadingType(HEART_RATE))
                    .thenReturn(Optional.of(thresholdOf(HEART_RATE, 50.0, 120.0)));
            stubLatestAlert(AlertStatus.PENDING, HEART_RATE, legacyPending);
            stubNoAlert(AlertStatus.UNCONFIRMED, HEART_RATE);
            stubAlertPersistence();

            AlertEvaluationResponse result = alertService.evaluateReading(requestOf(HEART_RATE, 155.0));

            assertThat(result.alertStatus()).isEqualTo(AlertStatus.UNCONFIRMED);
        }
    }

    @Nested
    @DisplayName("gravação e normalização da leitura")
    class ReadingPersistence {

        @Test
        @DisplayName("leitura normal deve ser gravada com suspect false")
        void shouldPersistNormalReadingAsNotSuspect() {
            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            stubReadingNotYetReceived(HEART_RATE);
            when(readingThresholdRepository.findByReadingType(HEART_RATE))
                    .thenReturn(Optional.of(thresholdOf(HEART_RATE, 50.0, 120.0)));
            stubNoAlert(AlertStatus.UNCONFIRMED, HEART_RATE);

            alertService.evaluateReading(requestOf(HEART_RATE, 80.0));

            ArgumentCaptor<HealthReading> captor = ArgumentCaptor.forClass(HealthReading.class);
            verify(healthReadingRepository).saveAndFlush(captor.capture());

            HealthReading saved = captor.getValue();
            assertThat(saved.isSuspect()).isFalse();
            assertThat(saved.getMeasuredAt()).isEqualTo(MEASURED_AT_STORED);
            assertThat(saved.getPatientDevice()).isNull();
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
            assertThat(result.suspectReading()).isFalse();
            assertThat(result.healthReadingId()).isEqualTo(READING_ID);
            verify(alertRepository, never()).save(any());
        }

        @Test
        @DisplayName("os dois fusos do mesmo instante devem gravar o mesmo measuredAt")
        void shouldNormalizeBothOffsetsToTheSameStoredValue() {
            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            stubReadingNotYetReceived(HEART_RATE);
            when(readingThresholdRepository.findByReadingType(HEART_RATE))
                    .thenReturn(Optional.of(thresholdOf(HEART_RATE, 50.0, 120.0)));
            stubNoAlert(AlertStatus.UNCONFIRMED, HEART_RATE);

            alertService.evaluateReading(requestOf(HEART_RATE, 80.0, MEASURED_AT_UTC));
            alertService.evaluateReading(
                    requestOf(HEART_RATE, 80.0, MEASURED_AT_SAME_INSTANT_OFFSET));

            ArgumentCaptor<HealthReading> captor = ArgumentCaptor.forClass(HealthReading.class);
            verify(healthReadingRepository, times(2)).saveAndFlush(captor.capture());

            assertThat(captor.getAllValues())
                    .extracting(HealthReading::getMeasuredAt)
                    .containsExactly(MEASURED_AT_STORED, MEASURED_AT_STORED);
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
            stubNoAlert(AlertStatus.UNCONFIRMED, HEART_RATE);

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
            stubNoAlert(AlertStatus.UNCONFIRMED, HEART_RATE);

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
            stubNoAlert(AlertStatus.UNCONFIRMED, SPO2);

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
                    "Alerta", "Descrição", AlertStatus.PENDING, LocalDateTime.now());
            PageRequest pageable = PageRequest.of(0, 20);

            when(userRepository.findByEmailAndActiveTrue(user.getEmail())).thenReturn(Optional.of(user));
            when(patientRepository.findByUserId(user.getId())).thenReturn(Optional.of(patient));
            when(alertRepository.findByPatientIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                    org.mockito.ArgumentMatchers.eq(PATIENT_ID),
                    any(LocalDateTime.class),
                    org.mockito.ArgumentMatchers.eq(pageable))).thenReturn(new PageImpl<>(List.of(alert)));
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
            assertThat(result.alertStatus()).isNull();
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

            when(healthReadingRepository.findByPatientIdAndReadingTypeAndMeasuredAt(
                    PATIENT_ID, HEART_RATE, MEASURED_AT_STORED)).thenReturn(Optional.of(existing));

            AlertEvaluationResponse result = alertService.evaluateReading(
                    requestOf(HEART_RATE, 200.0, MEASURED_AT_SAME_INSTANT_OFFSET));

            assertThat(result.duplicateReading()).isTrue();
            assertThat(result.healthReadingId()).isEqualTo(READING_ID);
            verify(healthReadingRepository).findByPatientIdAndReadingTypeAndMeasuredAt(
                    PATIENT_ID, HEART_RATE, MEASURED_AT_STORED);
            verify(healthReadingRepository, never()).saveAndFlush(any());
            verify(alertRepository, never()).save(any());
        }

        @Test
        @DisplayName("violacao de unicidade em corrida deve virar AlertDuplicateReadingException")
        void shouldTranslateUniqueViolationIntoDuplicateException() {
            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            when(healthReadingRepository.findByPatientIdAndReadingTypeAndMeasuredAt(
                    PATIENT_ID, HEART_RATE, MEASURED_AT_STORED)).thenReturn(Optional.empty());
            when(readingThresholdRepository.findByReadingType(HEART_RATE))
                    .thenReturn(Optional.of(thresholdOf(HEART_RATE, 50.0, 120.0)));
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
    @DisplayName("resposta do paciente ao alerta grave")
    class PatientResponse {

        private static final String PATIENT_EMAIL = "patient@tcc.com";
        private static final LocalDateTime SEVERE_MEASURED_AT = LocalDateTime.of(2026, 9, 29, 14, 50);

        private HealthReading severeReading;

        private Alert awaitingAlert() {
            severeReading = readingOf(READING_ID, HEART_RATE, "35.0", SEVERE_MEASURED_AT, false);
            Alert alert = alertOf(ALERT_ID, AlertStatus.AWAITING_PATIENT, severeReading, null);
            alert.setPatientResponseDeadline(NOW.plusMinutes(4));
            return alert;
        }

        private void stubAuthenticatedPatient() {
            User user = new User(PATIENT_EMAIL, "hash", Role.PATIENT);
            user.setId(UUID.randomUUID());
            patient.setUser(user);

            when(userRepository.findByEmailAndActiveTrue(PATIENT_EMAIL)).thenReturn(Optional.of(user));
            when(patientRepository.findByUserId(user.getId())).thenReturn(Optional.of(patient));
        }

        /** O UPDATE condicional encontrou a linha ainda em AWAITING_PATIENT. */
        private void stubTransitionWon(String targetStatus) {
            when(alertRepository.leaveAwaitingPatient(
                    eq(ALERT_ID), eq(targetStatus), any(), any(), any())).thenReturn(1);
        }

        @Test
        @DisplayName("NOT_OK deve levar a PENDING e publicar confirmacao com motivo PACIENTE_NAO_ESTA_BEM")
        void shouldConfirmAndNotifyDoctorWhenPatientIsNotOk() {
            Alert alert = awaitingAlert();
            stubAuthenticatedPatient();
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(alert));
            stubTransitionWon(AlertStatus.PENDING);

            PatientAlertAnswerResponse result = alertService.registerPatientResponse(
                    PATIENT_EMAIL, ALERT_ID, PatientAlertAnswer.NOT_OK);

            // confirmed_at é o horário da MEDIÇÃO grave, não o da resposta: é a
            // referência clínica e é de onde a janela de 4h é contada.
            verify(alertRepository).leaveAwaitingPatient(
                    ALERT_ID, AlertStatus.PENDING, SEVERE_MEASURED_AT,
                    PatientAlertAnswer.NOT_OK, NOW);

            ArgumentCaptor<AlertConfirmedEvent> captor =
                    ArgumentCaptor.forClass(AlertConfirmedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());

            AlertConfirmedEvent event = captor.getValue();
            assertThat(event.reason()).isEqualTo(AlertConfirmationReason.PACIENTE_NAO_ESTA_BEM);
            assertThat(event.firstReading().getId()).isEqualTo(READING_ID);
            // Nenhuma segunda medição participou da decisão.
            assertThat(event.confirmingReading()).isNull();

            assertThat(result.alertStatus()).isEqualTo(AlertStatus.PENDING);
            assertThat(result.answer()).isEqualTo(PatientAlertAnswer.NOT_OK);
            assertThat(result.respondedAt()).isEqualTo(NOW);
            assertThat(result.doctorNotified()).isTrue();
        }

        @Test
        @DisplayName("OK deve levar a UNCONFIRMED sem avisar o medico")
        void shouldReturnToCommonFlowWhenPatientIsOk() {
            Alert alert = awaitingAlert();
            stubAuthenticatedPatient();
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(alert));
            stubTransitionWon(AlertStatus.UNCONFIRMED);

            PatientAlertAnswerResponse result = alertService.registerPatientResponse(
                    PATIENT_EMAIL, ALERT_ID, PatientAlertAnswer.OK);

            // confirmed_at fica nulo: o alerta volta a não confirmado, e um valor ali
            // faria a janela de 4h silenciar as leituras seguintes.
            verify(alertRepository).leaveAwaitingPatient(
                    ALERT_ID, AlertStatus.UNCONFIRMED, null, PatientAlertAnswer.OK, NOW);

            // Nenhum e-mail: o evento de confirmação não é publicado.
            verifyNoInteractions(eventPublisher);

            assertThat(result.alertStatus()).isEqualTo(AlertStatus.UNCONFIRMED);
            assertThat(result.doctorNotified()).isFalse();
        }

        /**
         * Depois do OK o alerta é um UNCONFIRMED comum, então a próxima leitura fora
         * da faixa em até 2h o confirma pelo fluxo de duas leituras — grave ou não.
         */
        @Test
        @DisplayName("depois do OK a proxima leitura fora da faixa confirma pelo fluxo comum")
        void shouldConfirmByCommonFlowAfterPatientSaidOk() {
            HealthReading afterOk = readingOf(PREVIOUS_READING_ID, HEART_RATE, "35.0",
                    MEASURED_AT_STORED.minusMinutes(20), false);
            Alert unconfirmed = alertOf(ALERT_ID, AlertStatus.UNCONFIRMED, afterOk, null);
            unconfirmed.setPatientResponse(PatientAlertAnswer.OK);
            unconfirmed.setPatientRespondedAt(MEASURED_AT_STORED.minusMinutes(19));

            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            stubReadingNotYetReceived(HEART_RATE);
            when(readingThresholdRepository.findByReadingType(HEART_RATE))
                    .thenReturn(Optional.of(heartRateThreshold()));
            stubNoAlert(AlertStatus.PENDING, HEART_RATE);
            stubLatestAlert(AlertStatus.UNCONFIRMED, HEART_RATE, unconfirmed);
            stubPreviousReading(HEART_RATE, MEASURED_AT_STORED, afterOk);
            stubAlertSave();

            // 125 está fora da faixa normal (50–120) e NÃO é grave (severe_max 131),
            // então entra pelo fluxo comum e confirma o UNCONFIRMED.
            AlertEvaluationResponse result = alertService.evaluateReading(requestOf(HEART_RATE, 125.0));

            assertThat(unconfirmed.getStatus()).isEqualTo(AlertStatus.PENDING);
            assertThat(unconfirmed.getConfirmedAt()).isEqualTo(MEASURED_AT_STORED);
            assertThat(result.alertStatus()).isEqualTo(AlertStatus.PENDING);

            ArgumentCaptor<AlertConfirmedEvent> captor =
                    ArgumentCaptor.forClass(AlertConfirmedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().reason())
                    .isEqualTo(AlertConfirmationReason.DUAS_LEITURAS);
        }

        @Test
        @DisplayName("resposta em alerta de outro paciente deve ser negada")
        void shouldRejectResponseToAnotherPatientsAlert() {
            Patient otherPatient = new Patient();
            otherPatient.setId(UUID.randomUUID());

            Alert alert = awaitingAlert();
            alert.setPatient(otherPatient);

            stubAuthenticatedPatient();
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(alert));

            assertThatThrownBy(() -> alertService.registerPatientResponse(
                    PATIENT_EMAIL, ALERT_ID, PatientAlertAnswer.NOT_OK))
                    .isInstanceOf(UnauthorizedException.class);

            // Nada é gravado e nenhum médico é avisado.
            verify(alertRepository, never()).leaveAwaitingPatient(any(), any(), any(), any(), any());
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("resposta depois do prazo deve ser recusada com a mensagem de janela fechada")
        void shouldRejectResponseAfterDeadline() {
            // O agendador já levou o alerta a PENDING: o status não é mais
            // AWAITING_PATIENT e a resposta não é aceita.
            Alert alert = awaitingAlert();
            alert.setStatus(AlertStatus.PENDING);
            alert.setConfirmedAt(SEVERE_MEASURED_AT);
            alert.setPatientResponseDeadline(NOW.minusMinutes(2));

            stubAuthenticatedPatient();
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(alert));

            assertThatThrownBy(() -> alertService.registerPatientResponse(
                    PATIENT_EMAIL, ALERT_ID, PatientAlertAnswer.NOT_OK))
                    .isInstanceOf(AlertResponseWindowClosedException.class)
                    .hasMessageContaining("prazo")
                    .hasMessageContaining("médico foi avisado");

            verify(alertRepository, never()).leaveAwaitingPatient(any(), any(), any(), any(), any());
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("segunda resposta ao mesmo alerta deve dizer que a resposta ja foi registrada")
        void shouldRejectSecondResponseToTheSameAlert() {
            Alert alert = awaitingAlert();
            alert.setStatus(AlertStatus.UNCONFIRMED);
            alert.setPatientResponse(PatientAlertAnswer.OK);
            alert.setPatientRespondedAt(NOW.minusMinutes(1));

            stubAuthenticatedPatient();
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(alert));

            assertThatThrownBy(() -> alertService.registerPatientResponse(
                    PATIENT_EMAIL, ALERT_ID, PatientAlertAnswer.NOT_OK))
                    .isInstanceOf(AlertResponseWindowClosedException.class)
                    .hasMessageContaining("já foi registrada");

            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("alerta inexistente deve lancar ResourceNotFoundException")
        void shouldThrowWhenAlertNotFound() {
            stubAuthenticatedPatient();
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> alertService.registerPatientResponse(
                    PATIENT_EMAIL, ALERT_ID, PatientAlertAnswer.OK))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Alerta");
        }
    }

    @Nested
    @DisplayName("prazo de resposta vencido (agendador)")
    class ResponseDeadlineSweep {

        private static final LocalDateTime SEVERE_MEASURED_AT = LocalDateTime.of(2026, 9, 29, 14, 45);

        private HealthReading severeReading;

        private Alert expiredAlert() {
            severeReading = readingOf(READING_ID, HEART_RATE, "35.0", SEVERE_MEASURED_AT, false);
            Alert alert = alertOf(ALERT_ID, AlertStatus.AWAITING_PATIENT, severeReading, null);
            alert.setPatientResponseDeadline(NOW.minusMinutes(1));
            return alert;
        }

        @Test
        @DisplayName("deve consultar os vencidos usando o instante do Clock injetado")
        void shouldQueryExpiredUsingInjectedClock() {
            when(alertRepository.findIdsAwaitingPatientPastDeadline(
                    eq(NOW), any(Pageable.class))).thenReturn(List.of(ALERT_ID));

            assertThat(alertService.findAlertIdsAwaitingPatientPastDeadline())
                    .containsExactly(ALERT_ID);

            verify(alertRepository).findIdsAwaitingPatientPastDeadline(
                    eq(NOW), any(Pageable.class));
        }

        @Test
        @DisplayName("prazo vencido deve virar PENDING e publicar confirmacao com motivo SEM_RESPOSTA")
        void shouldConfirmWithNoAnswerReason() {
            Alert alert = expiredAlert();
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(alert));
            when(alertRepository.leaveAwaitingPatient(
                    ALERT_ID, AlertStatus.PENDING, SEVERE_MEASURED_AT, null, null)).thenReturn(1);

            boolean confirmed = alertService.confirmAlertWithoutPatientResponse(ALERT_ID);

            assertThat(confirmed).isTrue();

            // Sem resposta: patient_response e patient_responded_at ficam nulos.
            verify(alertRepository).leaveAwaitingPatient(
                    ALERT_ID, AlertStatus.PENDING, SEVERE_MEASURED_AT, null, null);

            ArgumentCaptor<AlertConfirmedEvent> captor =
                    ArgumentCaptor.forClass(AlertConfirmedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().reason())
                    .isEqualTo(AlertConfirmationReason.SEM_RESPOSTA);
            assertThat(captor.getValue().firstReading().getId()).isEqualTo(READING_ID);
        }

        /**
         * Prazo não vencido nem aparece na consulta do agendador. Mas se o alerta já
         * tiver saído de AWAITING_PATIENT entre a consulta e a troca, nada acontece.
         */
        @Test
        @DisplayName("alerta que ja saiu de AWAITING_PATIENT nao deve ser tocado")
        void shouldNotTouchAlertThatLeftAwaitingPatient() {
            Alert alert = expiredAlert();
            alert.setStatus(AlertStatus.UNCONFIRMED);
            alert.setPatientResponse(PatientAlertAnswer.OK);

            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(alert));

            assertThat(alertService.confirmAlertWithoutPatientResponse(ALERT_ID)).isFalse();

            verify(alertRepository, never()).leaveAwaitingPatient(any(), any(), any(), any(), any());
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("alerta inexistente deve ser ignorado sem excecao")
        void shouldIgnoreMissingAlert() {
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.empty());

            assertThat(alertService.confirmAlertWithoutPatientResponse(ALERT_ID)).isFalse();

            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("sem alerta vencido a consulta devolve lista vazia")
        void shouldReturnEmptyWhenNothingExpired() {
            when(alertRepository.findIdsAwaitingPatientPastDeadline(
                    eq(NOW), any(Pageable.class))).thenReturn(List.of());

            assertThat(alertService.findAlertIdsAwaitingPatientPastDeadline()).isEmpty();

            verifyNoInteractions(eventPublisher);
        }
    }

    /**
     * Resposta da paciente e agendador agindo sobre o mesmo alerta.
     *
     * <p>A exclusão mútua está no UPDATE condicional
     * {@code leaveAwaitingPatient}, que o banco serializa no lock da linha: o
     * primeiro recebe 1, o segundo recebe 0. Só quem recebe 1 publica o evento de
     * confirmação, então o médico recebe no máximo um e-mail.
     *
     * <p>Os testes representam a corrida pelo retorno do UPDATE, que é exatamente o
     * sinal que o service usa para decidir.
     */
    @Nested
    @DisplayName("corrida entre a resposta e o agendador")
    class ResponseRace {

        private static final String PATIENT_EMAIL = "patient@tcc.com";
        private static final LocalDateTime SEVERE_MEASURED_AT = LocalDateTime.of(2026, 9, 29, 14, 55);

        private Alert awaiting() {
            HealthReading severe = readingOf(READING_ID, HEART_RATE, "35.0", SEVERE_MEASURED_AT, false);
            Alert alert = alertOf(ALERT_ID, AlertStatus.AWAITING_PATIENT, severe, null);
            alert.setPatientResponseDeadline(NOW);
            return alert;
        }

        private void stubAuthenticatedPatient() {
            User user = new User(PATIENT_EMAIL, "hash", Role.PATIENT);
            user.setId(UUID.randomUUID());
            patient.setUser(user);

            when(userRepository.findByEmailAndActiveTrue(PATIENT_EMAIL)).thenReturn(Optional.of(user));
            when(patientRepository.findByUserId(user.getId())).thenReturn(Optional.of(patient));
        }

        @Test
        @DisplayName("agendador vence: a resposta recebe 409 e nenhum segundo evento sai")
        void schedulerWinsAndResponseIsRejected() {
            Alert alert = awaiting();
            stubAuthenticatedPatient();
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(alert));

            // UPDATE devolve 0: outra transação já tirou a linha de AWAITING_PATIENT.
            when(alertRepository.leaveAwaitingPatient(
                    eq(ALERT_ID), eq(AlertStatus.PENDING), any(), any(), any())).thenReturn(0);

            assertThatThrownBy(() -> alertService.registerPatientResponse(
                    PATIENT_EMAIL, ALERT_ID, PatientAlertAnswer.NOT_OK))
                    .isInstanceOf(AlertResponseWindowClosedException.class);

            // Quem perdeu a corrida não publica: o médico não recebe um 2º e-mail.
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("resposta vence: o agendador nao publica nada")
        void responseWinsAndSchedulerPublishesNothing() {
            Alert alert = awaiting();
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(alert));
            when(alertRepository.leaveAwaitingPatient(
                    ALERT_ID, AlertStatus.PENDING, SEVERE_MEASURED_AT, null, null)).thenReturn(0);

            assertThat(alertService.confirmAlertWithoutPatientResponse(ALERT_ID)).isFalse();

            verifyNoInteractions(eventPublisher);
        }

        /**
         * Os dois caminhos juntos, com o UPDATE devolvendo 1 para o primeiro e 0 para
         * o segundo — que é o comportamento do UPDATE condicional no banco. Exatamente
         * uma confirmação é publicada.
         */
        @Test
        @DisplayName("resposta e agendador juntos devem produzir no maximo uma confirmacao")
        void atMostOneConfirmationWhenBothAct() {
            Alert alert = awaiting();
            stubAuthenticatedPatient();
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(alert));

            // Primeira chamada vence, segunda perde: é o lock da linha decidindo.
            when(alertRepository.leaveAwaitingPatient(
                    eq(ALERT_ID), eq(AlertStatus.PENDING), any(), any(), any()))
                    .thenReturn(1, 0);

            PatientAlertAnswerResponse answered = alertService.registerPatientResponse(
                    PATIENT_EMAIL, ALERT_ID, PatientAlertAnswer.NOT_OK);
            boolean sweptAfter = alertService.confirmAlertWithoutPatientResponse(ALERT_ID);

            assertThat(answered.doctorNotified()).isTrue();
            assertThat(sweptAfter).isFalse();

            // Uma única confirmação publicada, apesar de os dois caminhos terem agido.
            verify(eventPublisher, times(1)).publishEvent(any(AlertConfirmedEvent.class));
        }
    }

    @Nested
    @DisplayName("resolução de alerta pelo médico")
    class Resolution {

        private static final String DOCTOR_EMAIL = "doctor@tcc.com";

        private Alert pendingAlert() {
            return alertOf(ALERT_ID, AlertStatus.PENDING, null, MEASURED_AT_STORED);
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
                    AlertStatus.RESOLVED, LocalDateTime.now()));

            AlertResponse result = alertService.resolveAlert(DOCTOR_EMAIL, ALERT_ID);

            assertThat(alert.getStatus()).isEqualTo(AlertStatus.RESOLVED);
            assertThat(result.status()).isEqualTo(AlertStatus.RESOLVED);
            verify(alertRepository).save(alert);
        }

        @Test
        @DisplayName("alerta ja RESOLVED nao deve ser gravado de novo e devolve o estado atual")
        void shouldNotSaveAgainWhenAlertIsAlreadyResolved() {
            Alert alert = pendingAlert();
            alert.setStatus(AlertStatus.RESOLVED);

            stubAuthenticatedDoctor();
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(alert));
            when(doctorPatientRepository.existsByDoctorIdAndPatientId(DOCTOR_ID, PATIENT_ID))
                    .thenReturn(true);
            when(alertMapper.toResponse(alert)).thenReturn(new AlertResponse(
                    ALERT_ID, null, READING_ID, "CRITICAL", "Alerta", "Motivo",
                    AlertStatus.RESOLVED, LocalDateTime.now()));

            AlertResponse result = alertService.resolveAlert(DOCTOR_EMAIL, ALERT_ID);

            assertThat(result.status()).isEqualTo(AlertStatus.RESOLVED);
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

            assertThat(alert.getStatus()).isEqualTo(AlertStatus.PENDING);
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

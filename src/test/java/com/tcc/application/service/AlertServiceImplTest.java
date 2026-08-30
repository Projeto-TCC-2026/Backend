package com.tcc.application.service;

import java.time.LocalDateTime;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tcc.application.dto.request.AlertEvaluationRequest;
import com.tcc.application.dto.response.AlertEvaluationResponse;
import com.tcc.application.mapper.AlertMapper;
import com.tcc.domain.model.Alert;
import com.tcc.domain.model.Patient;
import com.tcc.domain.model.ReadingThreshold;
import com.tcc.domain.repository.AlertRepository;
import com.tcc.domain.repository.PatientRepository;
import com.tcc.domain.repository.ReadingThresholdRepository;
import com.tcc.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class AlertServiceImplTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private ReadingThresholdRepository readingThresholdRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private AlertMapper alertMapper;

    @InjectMocks
    private AlertServiceImpl alertService;

    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID NONEXISTENT_PATIENT_ID = UUID.randomUUID();
    private static final UUID ALERT_ID = UUID.randomUUID();

    private static final String HEART_RATE = "HEART_RATE";
    private static final String SPO2 = "SPO2";

    private Patient patient;

    @BeforeEach
    void setUp() {
        patient = new Patient();
        patient.setId(PATIENT_ID);
    }

    private AlertEvaluationRequest requestOf(String readingType, Double value) {
        return new AlertEvaluationRequest(PATIENT_ID, readingType, value, LocalDateTime.now());
    }

    private ReadingThreshold thresholdOf(String readingType, Double min, Double max) {
        ReadingThreshold threshold = new ReadingThreshold(readingType, min, max, "CRITICAL");
        threshold.setId(UUID.randomUUID());
        return threshold;
    }

    /** Simula a persistência atribuindo um id ao alerta salvo. */
    private void stubAlertPersistence() {
        when(alertMapper.toEntity(any(), any(), any())).thenAnswer(invocation -> {
            var alertRequest = (com.tcc.application.dto.request.AlertRequest) invocation.getArgument(0);
            Alert alert = new Alert();
            alert.setPatient(patient);
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

    @Nested
    @DisplayName("valor dentro da faixa normal")
    class WithinNormalRange {

        @Test
        @DisplayName("nao deve gerar alerta quando valor esta dentro da faixa")
        void shouldNotGenerateAlertWhenValueIsWithinRange() {
            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            when(readingThresholdRepository.findByReadingType(HEART_RATE))
                    .thenReturn(Optional.of(thresholdOf(HEART_RATE, 50.0, 120.0)));

            AlertEvaluationResponse result = alertService.evaluateReading(requestOf(HEART_RATE, 80.0));

            assertThat(result.alertGenerated()).isFalse();
            assertThat(result.severity()).isNull();
            assertThat(result.alertId()).isNull();
            assertThat(result.reason()).isNull();
            verify(alertRepository, never()).save(any());
        }

        @Test
        @DisplayName("nao deve gerar alerta quando valor e exatamente igual ao minimo")
        void shouldNotGenerateAlertWhenValueEqualsMinimum() {
            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
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
            when(readingThresholdRepository.findByReadingType(SPO2))
                    .thenReturn(Optional.of(thresholdOf(SPO2, 90.0, null)));

            AlertEvaluationResponse result = alertService.evaluateReading(requestOf(SPO2, 99.0));

            assertThat(result.alertGenerated()).isFalse();
            verify(alertRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("valor fora da faixa normal")
    class OutsideNormalRange {

        @Test
        @DisplayName("deve gerar alerta CRITICAL quando valor esta abaixo do minimo")
        void shouldGenerateCriticalAlertWhenValueIsBelowMinimum() {
            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            when(readingThresholdRepository.findByReadingType(HEART_RATE))
                    .thenReturn(Optional.of(thresholdOf(HEART_RATE, 50.0, 120.0)));
            stubAlertPersistence();

            AlertEvaluationResponse result = alertService.evaluateReading(requestOf(HEART_RATE, 45.0));

            assertThat(result.alertGenerated()).isTrue();
            assertThat(result.severity()).isEqualTo("CRITICAL");
            assertThat(result.alertId()).isEqualTo(ALERT_ID);
            assertThat(result.reason()).contains("abaixo");
            verify(alertRepository).save(any(Alert.class));
        }

        @Test
        @DisplayName("deve gerar alerta CRITICAL quando valor esta acima do maximo")
        void shouldGenerateCriticalAlertWhenValueIsAboveMaximum() {
            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            when(readingThresholdRepository.findByReadingType(HEART_RATE))
                    .thenReturn(Optional.of(thresholdOf(HEART_RATE, 50.0, 120.0)));
            stubAlertPersistence();

            AlertEvaluationResponse result = alertService.evaluateReading(requestOf(HEART_RATE, 130.0));

            assertThat(result.alertGenerated()).isTrue();
            assertThat(result.severity()).isEqualTo("CRITICAL");
            assertThat(result.reason()).contains("acima");
            verify(alertRepository).save(any(Alert.class));
        }

        @Test
        @DisplayName("deve gerar alerta quando faixa sem maximo recebe valor abaixo do minimo")
        void shouldGenerateAlertWhenValueIsBelowMinimumOnOpenEndedRange() {
            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            when(readingThresholdRepository.findByReadingType(SPO2))
                    .thenReturn(Optional.of(thresholdOf(SPO2, 90.0, null)));
            stubAlertPersistence();

            AlertEvaluationResponse result = alertService.evaluateReading(requestOf(SPO2, 85.0));

            assertThat(result.alertGenerated()).isTrue();
            assertThat(result.severity()).isEqualTo("CRITICAL");
        }

        @Test
        @DisplayName("alerta criado deve ter status exatamente PENDING")
        void shouldCreateAlertWithStatusExactlyPending() {
            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            when(readingThresholdRepository.findByReadingType(HEART_RATE))
                    .thenReturn(Optional.of(thresholdOf(HEART_RATE, 50.0, 120.0)));
            stubAlertPersistence();

            alertService.evaluateReading(requestOf(HEART_RATE, 200.0));

            ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
            verify(alertRepository).save(captor.capture());

            Alert saved = captor.getValue();
            assertThat(saved.getStatus()).isEqualTo("PENDING");
            assertThat(saved.getSeverity()).isEqualTo("CRITICAL");
            assertThat(saved.getPatient()).isEqualTo(patient);
            assertThat(saved.getHealthReading()).isNull();
            assertThat(saved.getTitle()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("casos sem faixa ou sem paciente")
    class MissingData {

        @Test
        @DisplayName("nao deve gerar alerta nem lancar excecao quando tipo de leitura nao tem faixa")
        void shouldNotGenerateAlertWhenThresholdIsNotConfigured() {
            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
            when(readingThresholdRepository.findByReadingType("GLUCOSE"))
                    .thenReturn(Optional.empty());

            AlertEvaluationResponse result = alertService.evaluateReading(requestOf("GLUCOSE", 500.0));

            assertThat(result.alertGenerated()).isFalse();
            assertThat(result.severity()).isNull();
            assertThat(result.alertId()).isNull();
            verify(alertRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar excecao quando paciente nao encontrado")
        void shouldThrowWhenPatientNotFound() {
            when(patientRepository.findById(NONEXISTENT_PATIENT_ID)).thenReturn(Optional.empty());

            AlertEvaluationRequest request = new AlertEvaluationRequest(
                    NONEXISTENT_PATIENT_ID, HEART_RATE, 80.0, LocalDateTime.now());

            assertThatThrownBy(() -> alertService.evaluateReading(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Paciente");

            verify(alertRepository, never()).save(any());
        }
    }
}

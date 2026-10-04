package com.tcc.application.mapper;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tcc.application.dto.response.DoctorAlertResponse;
import com.tcc.domain.event.AlertConfirmationReason;
import com.tcc.domain.model.Alert;
import com.tcc.domain.model.AlertStatus;
import com.tcc.domain.model.HealthReading;
import com.tcc.domain.model.Patient;
import com.tcc.domain.model.PatientAlertAnswer;

/**
 * Conversão do alerta para a visão do médico.
 *
 * <p>O foco é o horário: as colunas guardam UTC em {@code TIMESTAMP} sem fuso, e o
 * DTO precisa sair com o fuso explícito, sem deslocar o valor. Um teste que só
 * comparasse campos de texto não pegaria uma conversão que mudasse o instante.
 */
@ExtendWith(MockitoExtension.class)
class AlertMapperTest {

    @Mock
    private PatientMapper patientMapper;

    @InjectMocks
    private AlertMapper alertMapper;

    private static final UUID ALERT_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID READING_ID = UUID.randomUUID();

    /** Como a coluna guarda: 14:40 já em UTC, sem fuso no tipo. */
    private static final LocalDateTime MEASURED_AT_STORED = LocalDateTime.of(2026, 8, 29, 14, 40);

    private Patient patient() {
        Patient patient = new Patient();
        patient.setId(PATIENT_ID);
        patient.setFullName("Paciente de Teste");
        return patient;
    }

    private HealthReading reading() {
        HealthReading reading = new HealthReading();
        reading.setId(READING_ID);
        reading.setReadingType("HEART_RATE");
        reading.setValue("38.0");
        reading.setUnit("bpm");
        reading.setMeasuredAt(MEASURED_AT_STORED);
        reading.setSuspect(false);
        return reading;
    }

    private Alert alertOf(String status, HealthReading reading) {
        Alert alert = new Alert();
        alert.setId(ALERT_ID);
        alert.setPatient(patient());
        alert.setHealthReading(reading);
        alert.setSeverity("CRITICAL");
        alert.setTitle("Leitura fora da faixa normal: HEART_RATE");
        alert.setStatus(status);
        return alert;
    }

    @Nested
    @DisplayName("toDoctorResponse")
    class ToDoctorResponse {

        @Test
        @DisplayName("deve achatar paciente e leitura e trazer o motivo da confirmacao")
        void shouldFlattenPatientAndReading() {
            Alert alert = alertOf(AlertStatus.PENDING, reading());
            alert.setConfirmedAt(MEASURED_AT_STORED);
            alert.setConfirmationReason(AlertConfirmationReason.DUAS_LEITURAS);

            DoctorAlertResponse result = alertMapper.toDoctorResponse(alert);

            assertThat(result.id()).isEqualTo(ALERT_ID);
            assertThat(result.patientId()).isEqualTo(PATIENT_ID);
            assertThat(result.patientName()).isEqualTo("Paciente de Teste");
            assertThat(result.readingType()).isEqualTo("HEART_RATE");
            assertThat(result.readingValue()).isEqualTo("38.0");
            assertThat(result.unit()).isEqualTo("bpm");
            assertThat(result.severity()).isEqualTo("CRITICAL");
            assertThat(result.status()).isEqualTo(AlertStatus.PENDING);
            assertThat(result.confirmationReason())
                    .isEqualTo(AlertConfirmationReason.DUAS_LEITURAS);
        }

        /**
         * O valor da coluna já está em UTC, então rotular o fuso não pode mudar o
         * instante. Um {@code atZone} com o fuso da JVM passaria no "14:40" e erraria
         * o instante — daí a asserção ser sobre o offset e sobre o instante.
         */
        @Test
        @DisplayName("measuredAt e confirmedAt devem sair em UTC, sem deslocar o valor")
        void shouldExposeTimestampsAsUtc() {
            Alert alert = alertOf(AlertStatus.PENDING, reading());
            alert.setConfirmedAt(MEASURED_AT_STORED.plusMinutes(5));

            DoctorAlertResponse result = alertMapper.toDoctorResponse(alert);

            assertThat(result.measuredAt())
                    .isEqualTo(OffsetDateTime.of(2026, 8, 29, 14, 40, 0, 0, ZoneOffset.UTC));
            assertThat(result.measuredAt().getOffset()).isEqualTo(ZoneOffset.UTC);
            assertThat(result.measuredAt().toLocalDateTime()).isEqualTo(MEASURED_AT_STORED);

            assertThat(result.confirmedAt())
                    .isEqualTo(OffsetDateTime.of(2026, 8, 29, 14, 45, 0, 0, ZoneOffset.UTC));
        }

        @Test
        @DisplayName("alerta AWAITING_PATIENT vem sem confirmedAt e sem motivo")
        void shouldLeaveConfirmationFieldsNullWhenAwaitingPatient() {
            Alert alert = alertOf(AlertStatus.AWAITING_PATIENT, reading());

            DoctorAlertResponse result = alertMapper.toDoctorResponse(alert);

            assertThat(result.confirmedAt()).isNull();
            assertThat(result.confirmationReason()).isNull();
            assertThat(result.patientResponse()).isNull();
        }

        /** Alerta confirmado antes da V37: a coluna existe, mas está nula. */
        @Test
        @DisplayName("alerta antigo confirmado sem motivo gravado vem com motivo nulo")
        void shouldTolerateConfirmedAlertWithoutReason() {
            Alert alert = alertOf(AlertStatus.PENDING, reading());
            alert.setConfirmedAt(MEASURED_AT_STORED);

            DoctorAlertResponse result = alertMapper.toDoctorResponse(alert);

            assertThat(result.confirmedAt()).isNotNull();
            assertThat(result.confirmationReason()).isNull();
        }

        @Test
        @DisplayName("deve expor a resposta da paciente quando houver")
        void shouldExposePatientResponse() {
            Alert alert = alertOf(AlertStatus.PENDING, reading());
            alert.setPatientResponse(PatientAlertAnswer.NOT_OK);
            alert.setConfirmationReason(AlertConfirmationReason.PACIENTE_NAO_ESTA_BEM);

            DoctorAlertResponse result = alertMapper.toDoctorResponse(alert);

            assertThat(result.patientResponse()).isEqualTo(PatientAlertAnswer.NOT_OK);
            assertThat(result.confirmationReason())
                    .isEqualTo(AlertConfirmationReason.PACIENTE_NAO_ESTA_BEM);
        }

        /** Alerta sem leitura associada é possível: a coluna aceita nulo. */
        @Test
        @DisplayName("alerta sem leitura deve zerar os quatro campos derivados dela")
        void shouldNullReadingFieldsWhenThereIsNoReading() {
            Alert alert = alertOf(AlertStatus.PENDING, null);

            DoctorAlertResponse result = alertMapper.toDoctorResponse(alert);

            assertThat(result.readingType()).isNull();
            assertThat(result.readingValue()).isNull();
            assertThat(result.unit()).isNull();
            assertThat(result.measuredAt()).isNull();
            // O resto do alerta continua preenchido.
            assertThat(result.title()).isNotNull();
        }

        @Test
        @DisplayName("alerta nulo deve devolver nulo")
        void shouldReturnNullForNullAlert() {
            assertThat(alertMapper.toDoctorResponse(null)).isNull();
        }
    }
}

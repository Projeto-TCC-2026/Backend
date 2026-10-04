package com.tcc.application.listener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tcc.application.port.out.AlertEmailSender;
import com.tcc.domain.event.AlertConfirmedEvent;
import com.tcc.domain.model.Alert;
import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.HealthReading;
import com.tcc.domain.model.Hospital;
import com.tcc.domain.model.Notification;
import com.tcc.domain.model.Patient;
import com.tcc.domain.model.Role;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.AlertRepository;
import com.tcc.domain.repository.DoctorPatientRepository;
import com.tcc.domain.repository.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class AlertEmailNotificationListenerTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private DoctorPatientRepository doctorPatientRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private AlertEmailSender alertEmailSender;

    @InjectMocks
    private AlertEmailNotificationListener listener;

    private static final UUID ALERT_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final String DOCTOR_A_EMAIL = "doctor.a@tcc.com";
    private static final String DOCTOR_B_EMAIL = "doctor.b@tcc.com";
    /**
     * Horários gravados, em UTC — é a convenção da coluna {@code measured_at}.
     * 14:32 UTC equivale a 11:32 em America/Sao_Paulo (UTC-3), e é esse o horário
     * que o e-mail precisa mostrar, em qualquer fuso de servidor.
     */
    private static final LocalDateTime FIRST_MEASURED_AT_UTC = LocalDateTime.of(2026, 9, 29, 13, 15);
    private static final LocalDateTime SECOND_MEASURED_AT_UTC = LocalDateTime.of(2026, 9, 29, 14, 32);

    private static final String FIRST_EXPECTED_IN_EMAIL = "29/09/2026 às 10:15";
    private static final String SECOND_EXPECTED_IN_EMAIL = "29/09/2026 às 11:32";

    private Patient patient;
    private HealthReading firstReading;
    private HealthReading confirmingReading;
    private Alert alert;

    @BeforeEach
    void setUp() {
        patient = new Patient();
        patient.setId(PATIENT_ID);
        patient.setFullName("Maria da Silva");

        firstReading = readingOf("150.0", FIRST_MEASURED_AT_UTC);
        confirmingReading = readingOf("155.0", SECOND_MEASURED_AT_UTC);

        alert = new Alert();
        alert.setId(ALERT_ID);
        alert.setPatient(patient);
        alert.setHealthReading(firstReading);
        alert.setSeverity("CRITICAL");
        alert.setDescription("Valor acima do máximo normal de 120.0 para HEART_RATE");
        alert.setStatus("PENDING");
        alert.setConfirmedAt(SECOND_MEASURED_AT_UTC);
    }

    private HealthReading readingOf(String value, LocalDateTime measuredAt) {
        HealthReading reading = new HealthReading();
        reading.setId(UUID.randomUUID());
        reading.setPatient(patient);
        reading.setReadingType("HEART_RATE");
        reading.setValue(value);
        reading.setUnit("bpm");
        reading.setMeasuredAt(measuredAt);
        return reading;
    }

    private AlertConfirmedEvent eventOf() {
        return new AlertConfirmedEvent(alert, firstReading, confirmingReading);
    }

    private Doctor doctorOf(String email) {
        User user = new User(email, "hash", Role.DOCTOR);
        user.setId(UUID.randomUUID());

        Doctor doctor = new Doctor();
        doctor.setId(UUID.randomUUID());
        doctor.setUser(user);
        doctor.setHospital(new Hospital());
        doctor.setFullName("Dr. Teste");
        return doctor;
    }

    private void stubAlertFound() {
        when(alertRepository.findByIdWithPatientAndReading(ALERT_ID)).thenReturn(Optional.of(alert));
    }

    @Nested
    @DisplayName("envio aos médicos vinculados")
    class Sending {

        @Test
        @DisplayName("deve enviar para cada medico vinculado e gravar notification SENT")
        void shouldSendToEveryLinkedDoctorAndRecordSent() {
            Doctor doctorA = doctorOf(DOCTOR_A_EMAIL);
            Doctor doctorB = doctorOf(DOCTOR_B_EMAIL);

            stubAlertFound();
            when(doctorPatientRepository.findActiveDoctorsByPatientId(PATIENT_ID))
                    .thenReturn(List.of(doctorA, doctorB));
            when(alertEmailSender.sendAlertEmail(anyString(), anyString(), anyString(), eq(ALERT_ID)))
                    .thenReturn(true);

            listener.onAlertConfirmed(eventOf());

            ArgumentCaptor<String> recipientCaptor = ArgumentCaptor.forClass(String.class);
            verify(alertEmailSender, times(2))
                    .sendAlertEmail(recipientCaptor.capture(), anyString(), anyString(), eq(ALERT_ID));
            assertThat(recipientCaptor.getAllValues())
                    .containsExactly(DOCTOR_A_EMAIL, DOCTOR_B_EMAIL);

            ArgumentCaptor<Notification> notificationCaptor =
                    ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository, times(2)).save(notificationCaptor.capture());

            assertThat(notificationCaptor.getAllValues())
                    .allSatisfy(notification -> {
                        assertThat(notification.getStatus()).isEqualTo("SENT");
                        assertThat(notification.getAlert()).isEqualTo(alert);
                        assertThat(notification.getMessage()).isNotBlank();
                    });
            assertThat(notificationCaptor.getAllValues())
                    .extracting(Notification::getDoctor)
                    .containsExactly(doctorA, doctorB);
        }

        @Test
        @DisplayName("corpo deve ter paciente, tipo, valor, severidade e horario da medicao")
        void shouldIncludeMeasurementDetailsInBody() {
            stubAlertFound();
            when(doctorPatientRepository.findActiveDoctorsByPatientId(PATIENT_ID))
                    .thenReturn(List.of(doctorOf(DOCTOR_A_EMAIL)));
            when(alertEmailSender.sendAlertEmail(anyString(), anyString(), anyString(), eq(ALERT_ID)))
                    .thenReturn(true);

            listener.onAlertConfirmed(eventOf());

            ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
            verify(alertEmailSender).sendAlertEmail(
                    anyString(), subjectCaptor.capture(), bodyCaptor.capture(), eq(ALERT_ID));

            assertThat(subjectCaptor.getValue()).contains("Maria da Silva").contains("CRITICAL");

            String body = bodyCaptor.getValue();
            assertThat(body)
                    .contains("Maria da Silva")
                    .contains("HEART_RATE")
                    .contains("CRITICAL")
                    // As DUAS leituras, com valor e horário de cada uma: é a
                    // progressão que justifica o aviso ao médico.
                    .contains("1ª leitura: 150.0 bpm")
                    .contains("2ª leitura: 155.0 bpm")
                    // 13:15 e 14:32 UTC gravados -> 10:15 e 11:32 exibidos. Não
                    // depende do fuso da máquina: os dois fusos da conversão são
                    // explícitos no código.
                    .contains(FIRST_EXPECTED_IN_EMAIL)
                    .contains(SECOND_EXPECTED_IN_EMAIL)
                    .doesNotContain("14:32")
                    .doesNotContain("13:15")
                    .contains("Brasília")
                    .contains("confirmado por duas leituras");
        }

        @Test
        @DisplayName("deve mostrar as duas leituras na ordem: primeira antes da confirmadora")
        void shouldShowReadingsInChronologicalOrder() {
            stubAlertFound();
            when(doctorPatientRepository.findActiveDoctorsByPatientId(PATIENT_ID))
                    .thenReturn(List.of(doctorOf(DOCTOR_A_EMAIL)));
            when(alertEmailSender.sendAlertEmail(anyString(), anyString(), anyString(), eq(ALERT_ID)))
                    .thenReturn(true);

            listener.onAlertConfirmed(eventOf());

            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
            verify(alertEmailSender).sendAlertEmail(
                    anyString(), anyString(), bodyCaptor.capture(), eq(ALERT_ID));

            String body = bodyCaptor.getValue();

            assertThat(body.indexOf(FIRST_EXPECTED_IN_EMAIL))
                    .as("1ª leitura deve aparecer antes da 2ª")
                    .isLessThan(body.indexOf(SECOND_EXPECTED_IN_EMAIL));
        }

        /**
         * Trava a independência de fuso: o teste força o fuso default da JVM para
         * dois extremos e exige o mesmo horário no e-mail nos dois casos. Se alguém
         * reintroduzir {@code ZoneId.systemDefault()} na conversão, este teste falha.
         *
         * <p>O fuso default é restaurado no fim, inclusive em caso de falha, para não
         * contaminar os outros testes da suíte.
         */
        @Test
        @DisplayName("horario do e-mail nao deve depender do fuso da maquina")
        void shouldFormatMeasuredAtIndependentlyOfJvmZone() {
            TimeZone original = TimeZone.getDefault();

            try {
                for (String zone : List.of("UTC", "America/Sao_Paulo", "Asia/Tokyo")) {
                    TimeZone.setDefault(TimeZone.getTimeZone(zone));

                    assertThat(bodySentWithFreshMocks())
                            .as("corpo do e-mail com a JVM em %s", zone)
                            .contains(FIRST_EXPECTED_IN_EMAIL)
                            .contains(SECOND_EXPECTED_IN_EMAIL);
                }
            } finally {
                TimeZone.setDefault(original);
            }
        }

        /**
         * Executa o listener com mocks próprios, para que a contagem de invocações de
         * um ciclo não interfira no seguinte dentro do laço acima.
         */
        private String bodySentWithFreshMocks() {
            AlertRepository alertRepo = org.mockito.Mockito.mock(AlertRepository.class);
            DoctorPatientRepository doctorPatientRepo =
                    org.mockito.Mockito.mock(DoctorPatientRepository.class);
            NotificationRepository notificationRepo =
                    org.mockito.Mockito.mock(NotificationRepository.class);
            AlertEmailSender sender = org.mockito.Mockito.mock(AlertEmailSender.class);

            when(alertRepo.findByIdWithPatientAndReading(ALERT_ID)).thenReturn(Optional.of(alert));
            when(doctorPatientRepo.findActiveDoctorsByPatientId(PATIENT_ID))
                    .thenReturn(List.of(doctorOf(DOCTOR_A_EMAIL)));
            when(sender.sendAlertEmail(anyString(), anyString(), anyString(), eq(ALERT_ID)))
                    .thenReturn(true);

            new AlertEmailNotificationListener(
                    alertRepo, doctorPatientRepo, notificationRepo, sender)
                    .onAlertConfirmed(eventOf());

            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendAlertEmail(
                    anyString(), anyString(), bodyCaptor.capture(), eq(ALERT_ID));

            return bodyCaptor.getValue();
        }

        @Test
        @DisplayName("nao deve enviar nada quando o paciente nao tem medico com vinculo ativo")
        void shouldNotSendWhenNoLinkedDoctor() {
            stubAlertFound();
            when(doctorPatientRepository.findActiveDoctorsByPatientId(PATIENT_ID))
                    .thenReturn(List.of());

            listener.onAlertConfirmed(eventOf());

            verifyNoInteractions(alertEmailSender, notificationRepository);
        }

        @Test
        @DisplayName("nao deve enviar nada quando o alerta nao e mais encontrado")
        void shouldNotSendWhenAlertNoLongerExists() {
            when(alertRepository.findByIdWithPatientAndReading(ALERT_ID)).thenReturn(Optional.empty());

            listener.onAlertConfirmed(eventOf());

            verifyNoInteractions(alertEmailSender, notificationRepository);
            verify(doctorPatientRepository, never()).findActiveDoctorsByPatientId(any());
        }
    }

    @Nested
    @DisplayName("falha no envio")
    class SendFailure {

        @Test
        @DisplayName("falha no SES deve gravar notification FAILED sem lancar excecao")
        void shouldRecordFailedWhenSenderReturnsFalse() {
            stubAlertFound();
            when(doctorPatientRepository.findActiveDoctorsByPatientId(PATIENT_ID))
                    .thenReturn(List.of(doctorOf(DOCTOR_A_EMAIL)));
            when(alertEmailSender.sendAlertEmail(anyString(), anyString(), anyString(), eq(ALERT_ID)))
                    .thenReturn(false);

            assertThatCode(() -> listener.onAlertConfirmed(eventOf())).doesNotThrowAnyException();

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo("FAILED");
        }

        @Test
        @DisplayName("excecao lancada pelo sender nao deve propagar")
        void shouldSwallowSenderException() {
            stubAlertFound();
            when(doctorPatientRepository.findActiveDoctorsByPatientId(PATIENT_ID))
                    .thenReturn(List.of(doctorOf(DOCTOR_A_EMAIL)));
            when(alertEmailSender.sendAlertEmail(anyString(), anyString(), anyString(), eq(ALERT_ID)))
                    .thenThrow(new RuntimeException("SES fora do ar"));

            assertThatCode(() -> listener.onAlertConfirmed(eventOf())).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("excecao na consulta ao repositorio nao deve propagar")
        void shouldSwallowRepositoryException() {
            when(alertRepository.findByIdWithPatientAndReading(ALERT_ID))
                    .thenThrow(new RuntimeException("banco indisponivel"));

            assertThatCode(() -> listener.onAlertConfirmed(eventOf())).doesNotThrowAnyException();

            verifyNoInteractions(alertEmailSender, notificationRepository);
        }
    }

    @Nested
    @DisplayName("dados ausentes no alerta")
    class MissingData {

        /**
         * Defesa contra evento mal formado. Não deve acontecer no fluxo normal — a
         * confirmação só é publicada com as duas leituras — mas o e-mail não pode
         * sair com campo vazio nem quebrar por NullPointerException.
         */
        @Test
        @DisplayName("evento sem as leituras deve gerar corpo sem campo vazio")
        void shouldHandleEventWithoutReadings() {
            alert.setHealthReading(null);
            firstReading = null;
            confirmingReading = null;

            stubAlertFound();
            when(doctorPatientRepository.findActiveDoctorsByPatientId(PATIENT_ID))
                    .thenReturn(List.of(doctorOf(DOCTOR_A_EMAIL)));
            when(alertEmailSender.sendAlertEmail(anyString(), anyString(), anyString(), eq(ALERT_ID)))
                    .thenReturn(true);

            listener.onAlertConfirmed(eventOf());

            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
            verify(alertEmailSender).sendAlertEmail(
                    anyString(), anyString(), bodyCaptor.capture(), eq(ALERT_ID));

            assertThat(bodyCaptor.getValue()).contains("não informado");
        }

        @Test
        @DisplayName("medico sem usuario deve gerar notification FAILED")
        void shouldRecordFailedWhenDoctorHasNoUser() {
            Doctor doctorWithoutUser = doctorOf(DOCTOR_A_EMAIL);
            doctorWithoutUser.setUser(null);

            stubAlertFound();
            when(doctorPatientRepository.findActiveDoctorsByPatientId(PATIENT_ID))
                    .thenReturn(List.of(doctorWithoutUser));
            when(alertEmailSender.sendAlertEmail(eq(null), anyString(), anyString(), eq(ALERT_ID)))
                    .thenReturn(false);

            listener.onAlertConfirmed(eventOf());

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo("FAILED");
        }
    }
}

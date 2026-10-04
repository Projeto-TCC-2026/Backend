package com.tcc.application.listener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tcc.application.port.out.PushNotificationPublisher;
import com.tcc.domain.event.AlertCreatedEvent;
import com.tcc.domain.model.Alert;
import com.tcc.domain.model.AlertStatus;
import com.tcc.domain.model.DeviceToken;
import com.tcc.domain.model.HealthReading;
import com.tcc.domain.model.Patient;
import com.tcc.domain.model.Role;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.AlertRepository;
import com.tcc.domain.repository.DeviceTokenRepository;

@ExtendWith(MockitoExtension.class)
class AlertPushNotificationListenerTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private PushNotificationPublisher pushNotificationPublisher;

    @InjectMocks
    private AlertPushNotificationListener listener;

    private static final UUID ALERT_ID = UUID.randomUUID();
    private static final String TOKEN_A = "fcm-token-a";
    private static final String TOKEN_B = "fcm-token-b";

    private User user;
    private Patient patient;
    private HealthReading reading;
    private Alert alert;

    @BeforeEach
    void setUp() {
        user = new User("patient@tcc.com", "hash", Role.PATIENT);
        user.setId(UUID.randomUUID());

        patient = new Patient();
        patient.setId(UUID.randomUUID());
        patient.setUser(user);

        reading = new HealthReading();
        reading.setId(UUID.randomUUID());
        reading.setPatient(patient);
        reading.setReadingType("HEART_RATE");
        reading.setValue("155.0");
        reading.setMeasuredAt(LocalDateTime.of(2026, 9, 29, 14, 32));

        alert = new Alert();
        alert.setId(ALERT_ID);
        alert.setPatient(patient);
        alert.setHealthReading(reading);
        alert.setSeverity("CRITICAL");
        alert.setDescription("Valor acima do máximo normal de 120.0 para HEART_RATE");
        // O push sai na criação do alerta, quando ele ainda está UNCONFIRMED.
        alert.setStatus(AlertStatus.UNCONFIRMED);
    }

    private AlertCreatedEvent eventOf(Alert alert) {
        return new AlertCreatedEvent(alert);
    }

    private DeviceToken deviceTokenOf(String token) {
        DeviceToken deviceToken = new DeviceToken(user, token, "ANDROID", "device-001");
        deviceToken.setId(UUID.randomUUID());
        return deviceToken;
    }

    @Nested
    @DisplayName("envio da notificacao")
    class Sending {

        @Test
        @DisplayName("deve enviar para todos os tokens do usuario do paciente")
        void shouldSendToEveryTokenOfThePatientUser() {
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(alert));
            when(deviceTokenRepository.findByUserId(user.getId()))
                    .thenReturn(List.of(deviceTokenOf(TOKEN_A), deviceTokenOf(TOKEN_B)));
            when(pushNotificationPublisher.publishAlertCreated(anyList(), anyString(), anyString(), eq(ALERT_ID)))
                    .thenReturn(List.of());

            listener.onAlertCreated(eventOf(alert));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
            verify(pushNotificationPublisher)
                    .publishAlertCreated(captor.capture(), anyString(), anyString(), eq(ALERT_ID));

            assertThat(captor.getValue()).containsExactly(TOKEN_A, TOKEN_B);
        }

        @Test
        @DisplayName("deve derivar o titulo da severidade e o corpo do tipo da leitura")
        void shouldDeriveTitleFromSeverityAndBodyFromReadingType() {
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(alert));
            when(deviceTokenRepository.findByUserId(user.getId()))
                    .thenReturn(List.of(deviceTokenOf(TOKEN_A)));
            when(pushNotificationPublisher.publishAlertCreated(anyList(), anyString(), anyString(), eq(ALERT_ID)))
                    .thenReturn(List.of());

            listener.onAlertCreated(eventOf(alert));

            ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
            verify(pushNotificationPublisher).publishAlertCreated(
                    anyList(), titleCaptor.capture(), bodyCaptor.capture(), eq(ALERT_ID));

            assertThat(titleCaptor.getValue()).isEqualTo("Alerta crítico de saúde");
            assertThat(bodyCaptor.getValue())
                    .isEqualTo("Sua medição de frequência cardíaca saiu do normal. "
                            + "Se não estiver bem, use a opção 'Não estou bem' no app.");

            // Alerta UNCONFIRMED continua no push comum: sem categoria e sem o type
            // SEVERE_CHECK, que fariam o app mostrar os botões de resposta.
            verify(pushNotificationPublisher, never()).publishAlertCreated(
                    anyList(), anyString(), anyString(), any(UUID.class), anyMap(), anyString());
        }

        /**
         * O corpo não pode repassar o limite numérico violado, que é a descrição do
         * alerta: é informação clínica crua para o paciente numa notificação.
         */
        @Test
        @DisplayName("corpo nao deve conter o limite numerico da descricao do alerta")
        void shouldNotLeakThresholdDetailIntoPushBody() {
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(alert));
            when(deviceTokenRepository.findByUserId(user.getId()))
                    .thenReturn(List.of(deviceTokenOf(TOKEN_A)));
            when(pushNotificationPublisher.publishAlertCreated(anyList(), anyString(), anyString(), eq(ALERT_ID)))
                    .thenReturn(List.of());

            listener.onAlertCreated(eventOf(alert));

            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
            verify(pushNotificationPublisher).publishAlertCreated(
                    anyList(), anyString(), bodyCaptor.capture(), eq(ALERT_ID));

            assertThat(bodyCaptor.getValue())
                    .doesNotContain("120.0")
                    .doesNotContain("155.0");
        }

        @Test
        @DisplayName("deve traduzir SPO2 e TEMPERATURE no corpo do push")
        void shouldTranslateOtherReadingTypes() {
            reading.setReadingType("SPO2");
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(alert));
            when(deviceTokenRepository.findByUserId(user.getId()))
                    .thenReturn(List.of(deviceTokenOf(TOKEN_A)));
            when(pushNotificationPublisher.publishAlertCreated(anyList(), anyString(), anyString(), eq(ALERT_ID)))
                    .thenReturn(List.of());

            listener.onAlertCreated(eventOf(alert));

            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
            verify(pushNotificationPublisher).publishAlertCreated(
                    anyList(), anyString(), bodyCaptor.capture(), eq(ALERT_ID));

            assertThat(bodyCaptor.getValue()).contains("saturação de oxigênio");
        }

        @Test
        @DisplayName("deve usar titulo generico quando a severidade e desconhecida")
        void shouldUseGenericTitleForUnknownSeverity() {
            alert.setSeverity("SEVERIDADE_NOVA");
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(alert));
            when(deviceTokenRepository.findByUserId(user.getId()))
                    .thenReturn(List.of(deviceTokenOf(TOKEN_A)));
            when(pushNotificationPublisher.publishAlertCreated(anyList(), anyString(), anyString(), eq(ALERT_ID)))
                    .thenReturn(List.of());

            listener.onAlertCreated(eventOf(alert));

            ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
            verify(pushNotificationPublisher).publishAlertCreated(
                    anyList(), titleCaptor.capture(), anyString(), eq(ALERT_ID));

            assertThat(titleCaptor.getValue()).isEqualTo("Alerta de saúde");
        }

        @Test
        @DisplayName("deve usar rotulo generico quando o alerta nao tem leitura associada")
        void shouldUseGenericLabelWhenAlertHasNoReading() {
            alert.setHealthReading(null);
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(alert));
            when(deviceTokenRepository.findByUserId(user.getId()))
                    .thenReturn(List.of(deviceTokenOf(TOKEN_A)));
            when(pushNotificationPublisher.publishAlertCreated(anyList(), anyString(), anyString(), eq(ALERT_ID)))
                    .thenReturn(List.of());

            listener.onAlertCreated(eventOf(alert));

            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
            verify(pushNotificationPublisher).publishAlertCreated(
                    anyList(), anyString(), bodyCaptor.capture(), eq(ALERT_ID));

            assertThat(bodyCaptor.getValue())
                    .isNotBlank()
                    .contains("sinal vital")
                    .contains("Não estou bem");
        }
    }

    @Nested
    @DisplayName("push da pergunta ao paciente (AWAITING_PATIENT)")
    class SevereCheck {

        @BeforeEach
        void awaitingPatient() {
            alert.setStatus(AlertStatus.AWAITING_PATIENT);
            alert.setPatientResponseDeadline(LocalDateTime.of(2026, 9, 29, 14, 42));
        }

        /** Captura a chamada da sobrecarga com data extra e categoria. */
        private void sendAndStub() {
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(alert));
            when(deviceTokenRepository.findByUserId(user.getId()))
                    .thenReturn(List.of(deviceTokenOf(TOKEN_A)));
            when(pushNotificationPublisher.publishAlertCreated(
                    anyList(), anyString(), anyString(), eq(ALERT_ID), anyMap(), anyString()))
                    .thenReturn(List.of());

            listener.onAlertCreated(eventOf(alert));
        }

        @Test
        @DisplayName("texto deve perguntar se esta bem, sem o valor medido")
        void shouldAskIfPatientIsWellWithoutTheMeasuredValue() {
            sendAndStub();

            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
            verify(pushNotificationPublisher).publishAlertCreated(
                    anyList(), anyString(), bodyCaptor.capture(), eq(ALERT_ID), anyMap(), anyString());

            assertThat(bodyCaptor.getValue())
                    .isEqualTo("Sua medição de frequência cardíaca está muito fora do normal. "
                            + "Você está bem?")
                    // Nenhum número: nem o valor medido, nem o limite da faixa.
                    .doesNotContain("155")
                    .doesNotContain("120")
                    .doesNotContain("131");
        }

        @Test
        @DisplayName("data payload deve levar type SEVERE_CHECK e a categoria severe-check")
        void shouldSendSevereCheckTypeAndCategory() {
            sendAndStub();

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, String>> dataCaptor = ArgumentCaptor.forClass(Map.class);
            ArgumentCaptor<String> categoryCaptor = ArgumentCaptor.forClass(String.class);
            verify(pushNotificationPublisher).publishAlertCreated(
                    anyList(), anyString(), anyString(), eq(ALERT_ID),
                    dataCaptor.capture(), categoryCaptor.capture());

            assertThat(dataCaptor.getValue()).containsEntry("type", "SEVERE_CHECK");
            assertThat(categoryCaptor.getValue()).isEqualTo("severe-check");
        }

        /**
         * O alertId não entra pelo mapa de extras: ele é acrescentado pelo publisher,
         * e chega ao app pelo mesmo caminho do push comum.
         */
        @Test
        @DisplayName("alertId deve ser passado ao publisher, que o poe no data")
        void shouldPassAlertIdToThePublisher() {
            sendAndStub();

            verify(pushNotificationPublisher).publishAlertCreated(
                    anyList(), anyString(), anyString(), eq(ALERT_ID), anyMap(), anyString());
        }

        @Test
        @DisplayName("nao deve usar o texto do push comum")
        void shouldNotUseTheCommonPushText() {
            sendAndStub();

            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
            verify(pushNotificationPublisher).publishAlertCreated(
                    anyList(), anyString(), bodyCaptor.capture(), eq(ALERT_ID), anyMap(), anyString());

            assertThat(bodyCaptor.getValue()).doesNotContain("Não estou bem");
        }

        @Test
        @DisplayName("deve remover token nao registrado tambem no push da pergunta")
        void shouldRemoveUnregisteredTokenInSevereCheckToo() {
            DeviceToken tokenA = deviceTokenOf(TOKEN_A);

            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(alert));
            when(deviceTokenRepository.findByUserId(user.getId())).thenReturn(List.of(tokenA));
            when(pushNotificationPublisher.publishAlertCreated(
                    anyList(), anyString(), anyString(), eq(ALERT_ID), anyMap(), anyString()))
                    .thenReturn(List.of(TOKEN_A));

            listener.onAlertCreated(eventOf(alert));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<DeviceToken>> captor = ArgumentCaptor.forClass(List.class);
            verify(deviceTokenRepository).deleteAll(captor.capture());

            assertThat(captor.getValue()).containsExactly(tokenA);
        }
    }

    @Nested
    @DisplayName("ausencia de dispositivo")
    class NoDevice {

        @Test
        @DisplayName("nao deve enviar nada quando o usuario nao tem token registrado")
        void shouldNotSendWhenUserHasNoToken() {
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(alert));
            when(deviceTokenRepository.findByUserId(user.getId())).thenReturn(List.of());

            listener.onAlertCreated(eventOf(alert));

            verifyNoInteractions(pushNotificationPublisher);
            verify(deviceTokenRepository, never()).deleteAll(anyList());
        }

        @Test
        @DisplayName("nao deve enviar nada quando o alerta nao e mais encontrado")
        void shouldNotSendWhenAlertNoLongerExists() {
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.empty());

            listener.onAlertCreated(eventOf(alert));

            verifyNoInteractions(pushNotificationPublisher);
            verify(deviceTokenRepository, never()).findByUserId(any());
        }
    }

    @Nested
    @DisplayName("limpeza de token invalido")
    class TokenCleanup {

        @Test
        @DisplayName("deve remover apenas o token reportado como nao registrado")
        void shouldRemoveOnlyTheUnregisteredToken() {
            DeviceToken tokenA = deviceTokenOf(TOKEN_A);
            DeviceToken tokenB = deviceTokenOf(TOKEN_B);

            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(alert));
            when(deviceTokenRepository.findByUserId(user.getId())).thenReturn(List.of(tokenA, tokenB));
            when(pushNotificationPublisher.publishAlertCreated(anyList(), anyString(), anyString(), eq(ALERT_ID)))
                    .thenReturn(List.of(TOKEN_B));

            listener.onAlertCreated(eventOf(alert));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<DeviceToken>> captor = ArgumentCaptor.forClass(List.class);
            verify(deviceTokenRepository).deleteAll(captor.capture());

            assertThat(captor.getValue()).containsExactly(tokenB);
        }

        @Test
        @DisplayName("nao deve remover token quando o provedor nao reporta nenhum invalido")
        void shouldNotRemoveTokenWhenNoneIsReported() {
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(alert));
            when(deviceTokenRepository.findByUserId(user.getId()))
                    .thenReturn(List.of(deviceTokenOf(TOKEN_A)));
            when(pushNotificationPublisher.publishAlertCreated(anyList(), anyString(), anyString(), eq(ALERT_ID)))
                    .thenReturn(List.of());

            listener.onAlertCreated(eventOf(alert));

            verify(deviceTokenRepository, never()).deleteAll(anyList());
        }
    }

    @Nested
    @DisplayName("isolamento de falha")
    class FailureIsolation {

        @Test
        @DisplayName("nao deve propagar excecao lancada pelo publisher")
        void shouldSwallowPublisherException() {
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(alert));
            when(deviceTokenRepository.findByUserId(user.getId()))
                    .thenReturn(List.of(deviceTokenOf(TOKEN_A)));
            when(pushNotificationPublisher.publishAlertCreated(anyList(), anyString(), anyString(), eq(ALERT_ID)))
                    .thenThrow(new RuntimeException("provedor fora do ar"));

            assertThatCode(() -> listener.onAlertCreated(eventOf(alert))).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("nao deve propagar excecao lancada na consulta ao repositorio")
        void shouldSwallowRepositoryException() {
            when(alertRepository.findById(ALERT_ID)).thenThrow(new RuntimeException("banco indisponivel"));

            assertThatCode(() -> listener.onAlertCreated(eventOf(alert))).doesNotThrowAnyException();

            verifyNoInteractions(pushNotificationPublisher);
        }
    }
}

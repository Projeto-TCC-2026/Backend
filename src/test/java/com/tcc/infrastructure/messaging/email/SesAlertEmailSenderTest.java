package com.tcc.infrastructure.messaging.email;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;

@ExtendWith(MockitoExtension.class)
class SesAlertEmailSenderTest {

    @Mock
    private SesV2Client sesV2Client;

    private static final UUID ALERT_ID = UUID.randomUUID();
    private static final String SENDER = "alertas@tcc.local";
    private static final String RECIPIENT = "doctor@tcc.local";
    private static final String SUBJECT = "Alerta de saúde: Maria da Silva [CRITICAL]";
    private static final String BODY = "Horário da medição: 29/09/2026 às 14:30";

    private SesAlertEmailSender senderWith(String senderAddress) {
        return new SesAlertEmailSender(sesV2Client, senderAddress);
    }

    @Nested
    @DisplayName("envio bem-sucedido")
    class Success {

        @Test
        @DisplayName("deve montar a requisicao com remetente, destinatario, assunto e corpo em texto")
        void shouldBuildRequestWithAllFields() {
            when(sesV2Client.sendEmail(any(SendEmailRequest.class)))
                    .thenReturn(SendEmailResponse.builder().messageId("id-123").build());

            boolean result = senderWith(SENDER).sendAlertEmail(RECIPIENT, SUBJECT, BODY, ALERT_ID);

            assertThat(result).isTrue();

            ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
            verify(sesV2Client).sendEmail(captor.capture());

            SendEmailRequest request = captor.getValue();
            assertThat(request.fromEmailAddress()).isEqualTo(SENDER);
            assertThat(request.destination().toAddresses()).containsExactly(RECIPIENT);
            assertThat(request.content().simple().subject().data()).isEqualTo(SUBJECT);
            assertThat(request.content().simple().body().text().data()).isEqualTo(BODY);
            assertThat(request.content().simple().body().html()).isNull();
            assertThat(request.content().simple().body().text().charset()).isEqualTo("UTF-8");
        }
    }

    @Nested
    @DisplayName("falha e configuração ausente")
    class Failure {

        @Test
        @DisplayName("falha do provedor deve devolver false sem lancar excecao")
        void shouldReturnFalseWhenProviderFails() {
            when(sesV2Client.sendEmail(any(SendEmailRequest.class)))
                    .thenThrow(new RuntimeException("SES fora do ar"));

            SesAlertEmailSender sender = senderWith(SENDER);

            assertThatCode(() -> sender.sendAlertEmail(RECIPIENT, SUBJECT, BODY, ALERT_ID))
                    .doesNotThrowAnyException();
            assertThat(sender.sendAlertEmail(RECIPIENT, SUBJECT, BODY, ALERT_ID)).isFalse();
        }

        @Test
        @DisplayName("destinatario em branco deve devolver false sem chamar o provedor")
        void shouldReturnFalseForBlankRecipient() {
            boolean result = senderWith(SENDER).sendAlertEmail("  ", SUBJECT, BODY, ALERT_ID);

            assertThat(result).isFalse();
            verify(sesV2Client, never()).sendEmail(any(SendEmailRequest.class));
        }

        @Test
        @DisplayName("remetente nao configurado deve devolver false sem chamar o provedor")
        void shouldReturnFalseWhenSenderIsNotConfigured() {
            boolean result = senderWith("").sendAlertEmail(RECIPIENT, SUBJECT, BODY, ALERT_ID);

            assertThat(result).isFalse();
            verify(sesV2Client, never()).sendEmail(any(SendEmailRequest.class));
        }
    }
}

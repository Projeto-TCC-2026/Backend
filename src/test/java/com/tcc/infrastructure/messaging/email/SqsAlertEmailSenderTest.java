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
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class SqsAlertEmailSenderTest {

    @Mock
    private SqsClient sqsClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final UUID ALERT_ID = UUID.randomUUID();
    private static final String QUEUE_URL =
            "https://sqs.us-east-1.amazonaws.com/919446725618/alerta-medico-email-requests";
    private static final String RECIPIENT = "doctor@tcc.local";
    private static final String SUBJECT = "Alerta de saúde: Maria da Silva [CRITICAL]";
    private static final String BODY = "Horário da medição: 29/09/2026 às 14:30\nPressão: 180/110 mmHg";

    private SqsAlertEmailSender senderWith(String queueUrl) {
        return new SqsAlertEmailSender(sqsClient, objectMapper, queueUrl);
    }

    @Nested
    @DisplayName("envio bem-sucedido")
    class Success {

        @Test
        @DisplayName("deve publicar na fila configurada o JSON com to, subject e body")
        void shouldSendJsonWithTheThreeFieldsToConfiguredQueue() throws Exception {
            when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                    .thenReturn(SendMessageResponse.builder().messageId("msg-123").build());

            boolean result = senderWith(QUEUE_URL).sendAlertEmail(RECIPIENT, SUBJECT, BODY, ALERT_ID);

            assertThat(result).isTrue();

            ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
            verify(sqsClient).sendMessage(captor.capture());

            SendMessageRequest request = captor.getValue();
            assertThat(request.queueUrl()).isEqualTo(QUEUE_URL);

            JsonNode json = objectMapper.readTree(request.messageBody());
            assertThat(json.isObject()).isTrue();
            assertThat(json.size()).isEqualTo(3);
            assertThat(json.get("to").asText()).isEqualTo(RECIPIENT);
            assertThat(json.get("subject").asText()).isEqualTo(SUBJECT);
            assertThat(json.get("body").asText()).isEqualTo(BODY);
        }

        @Test
        @DisplayName("log deve ter o id do alerta e nunca destinatario, assunto ou corpo")
        void shouldNotLogSensitiveContentOnSuccess(CapturedOutput output) {
            when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                    .thenReturn(SendMessageResponse.builder().messageId("msg-123").build());

            senderWith(QUEUE_URL).sendAlertEmail(RECIPIENT, SUBJECT, BODY, ALERT_ID);

            assertThat(output.getAll()).contains(ALERT_ID.toString());
            assertNoSensitiveContent(output);
        }
    }

    @Nested
    @DisplayName("falha e configuração ausente")
    class Failure {

        @Test
        @DisplayName("falha do SQS deve devolver false sem lancar excecao")
        void shouldReturnFalseWhenSqsFails() {
            when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                    .thenThrow(SqsException.builder().message("SQS fora do ar").build());

            SqsAlertEmailSender sender = senderWith(QUEUE_URL);

            assertThatCode(() -> sender.sendAlertEmail(RECIPIENT, SUBJECT, BODY, ALERT_ID))
                    .doesNotThrowAnyException();
            assertThat(sender.sendAlertEmail(RECIPIENT, SUBJECT, BODY, ALERT_ID)).isFalse();
        }

        @Test
        @DisplayName("falha do SQS nao deve registrar destinatario, assunto ou corpo")
        void shouldNotLogSensitiveContentOnFailure(CapturedOutput output) {
            // Mensagem da exceção com o destinatário: garante que ela não vai para o log.
            when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                    .thenThrow(SqsException.builder().message("erro " + RECIPIENT).build());

            senderWith(QUEUE_URL).sendAlertEmail(RECIPIENT, SUBJECT, BODY, ALERT_ID);

            assertThat(output.getAll()).contains(ALERT_ID.toString());
            assertNoSensitiveContent(output);
        }

        @Test
        @DisplayName("destinatario em branco deve devolver false sem chamar o SQS")
        void shouldReturnFalseForBlankRecipient() {
            boolean result = senderWith(QUEUE_URL).sendAlertEmail("  ", SUBJECT, BODY, ALERT_ID);

            assertThat(result).isFalse();
            verify(sqsClient, never()).sendMessage(any(SendMessageRequest.class));
        }

        @Test
        @DisplayName("fila nao configurada deve devolver false sem chamar o SQS")
        void shouldReturnFalseWhenQueueUrlIsNotConfigured() {
            boolean result = senderWith("").sendAlertEmail(RECIPIENT, SUBJECT, BODY, ALERT_ID);

            assertThat(result).isFalse();
            verify(sqsClient, never()).sendMessage(any(SendMessageRequest.class));
        }
    }

    private static void assertNoSensitiveContent(CapturedOutput output) {
        assertThat(output.getAll())
                .doesNotContain(RECIPIENT)
                .doesNotContain(SUBJECT)
                .doesNotContain("Maria da Silva")
                .doesNotContain("180/110");
    }
}

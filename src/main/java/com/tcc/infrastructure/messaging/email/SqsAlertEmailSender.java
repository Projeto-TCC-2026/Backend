package com.tcc.infrastructure.messaging.email;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.application.port.out.AlertEmailSender;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

/**
 * Envio por fila SQS: publica {@code {to, subject, body}} na fila
 * {@code alerta-medico-email-requests}, consumida pela Lambda que entrega o e-mail
 * em texto simples. O remetente é fixo na Lambda, por isso não vai na mensagem.
 *
 * <p>Usado no lugar do {@link SesAlertEmailSender} porque o SES em sandbox só
 * entrega para endereços verificados. Mesmo formato dos publishers SQS do projeto:
 * {@code SqsClient} e {@code ObjectMapper} injetados, URL da fila por propriedade.
 *
 * <p>O conteúdo tem dado de saúde: destinatário, assunto, corpo e o JSON montado
 * nunca aparecem em log. Só o id do alerta e o resultado.
 */
@Service
@ConditionalOnProperty(name = "app.alert-email.enabled", havingValue = "true")
@ConditionalOnProperty(name = "app.alert-email.provider", havingValue = "sqs")
public class SqsAlertEmailSender implements AlertEmailSender {

    private static final Logger log = LoggerFactory.getLogger(SqsAlertEmailSender.class);

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final String queueUrl;

    public SqsAlertEmailSender(SqsClient sqsClient,
                               ObjectMapper objectMapper,
                               @Value("${app.alert-email.sqs-queue-url:}") String queueUrl) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.queueUrl = queueUrl;
    }

    /**
     * {@code true} quando a fila aceitou a mensagem. A entrega do e-mail em si
     * acontece depois, na Lambda; daqui só se sabe se a mensagem foi enfileirada.
     *
     * <p>Nenhuma exceção escapa, como no SES: falha vira FAILED em
     * {@code notifications}. Do erro só se registra a classe, porque a mensagem de
     * uma exceção de serialização ou do SDK pode ecoar parte do conteúdo.
     */
    @Override
    public boolean sendAlertEmail(String recipientEmail, String subject, String body, UUID alertId) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("Destinatario ausente no aviso do alerta {}. Envio ignorado.", alertId);
            return false;
        }

        if (queueUrl == null || queueUrl.isBlank()) {
            log.error("Fila de e-mail de alerta nao configurada. Aviso do alerta {} nao enviado.", alertId);
            return false;
        }

        try {
            String payload = objectMapper.writeValueAsString(
                    new AlertEmailRequest(recipientEmail, subject, body));

            SendMessageResponse response = sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(payload)
                    .build());

            log.info("Aviso do alerta {} enfileirado para envio por e-mail. messageId={}",
                    alertId, response == null ? null : response.messageId());
            return true;

        } catch (Exception e) {
            log.error("Falha ao enfileirar e-mail do alerta {}. exception={}",
                    alertId, e.getClass().getSimpleName());
            return false;
        }
    }

    /** Contrato da fila: exatamente estes três campos. */
    record AlertEmailRequest(String to, String subject, String body) {
    }
}

package com.tcc.infrastructure.messaging.sqs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.application.port.out.PasswordResetPublisher;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Service
@ConditionalOnProperty(name = "app.password-reset.sqs-enabled", havingValue = "true")
public class AwsSqsPasswordResetPublisher
        implements PasswordResetPublisher {

    private static final Logger log = LoggerFactory.getLogger(
            AwsSqsPasswordResetPublisher.class);

    private final ObjectMapper objectMapper;
    private final SqsClient sqsClient;
    private final String queueUrl;

    public AwsSqsPasswordResetPublisher(
            ObjectMapper objectMapper,
            SqsClient sqsClient,
            @Value("${app.password-reset.sqs-queue-url}") String queueUrl) {
        this.objectMapper = objectMapper;
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
    }

    @Override
    public void publishResetRequested(
            String email,
            String token,
            String frontendBaseUrl) {

        if (queueUrl == null || queueUrl.isBlank()) {
            throw new IllegalStateException(
                    "Queue URL para password reset não configurada.");
        }

        PasswordResetMessage message = new PasswordResetMessage(
                "PASSWORD_RESET_REQUESTED",
                email,
                token,
                frontendBaseUrl);

        try {

            String payload = objectMapper.writeValueAsString(message);

            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(payload)
                    .build();

            sqsClient.sendMessage(request);

            log.info(
                    "Mensagem de recuperação de senha publicada na SQS.");

        } catch (JsonProcessingException e) {

            log.error(
                    "Erro ao serializar mensagem de recuperação de senha.",
                    e);

            throw new IllegalStateException(
                    "Falha ao serializar mensagem de recuperação de senha.",
                    e);

        } catch (Exception e) {

            log.error(
                    "Erro ao publicar mensagem na SQS. QueueUrl={}",
                    queueUrl,
                    e);

            throw new IllegalStateException(
                    "Falha ao publicar mensagem na SQS.",
                    e);
        }
    }
}
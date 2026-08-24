package com.tcc.infrastructure.messaging.sqs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.application.port.out.AccountActivationPublisher;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Service
@ConditionalOnProperty(name = "app.account-activation.sqs-enabled", havingValue = "true")
public class AwsSqsAccountActivationPublisher implements AccountActivationPublisher {

    private static final Logger log = LoggerFactory.getLogger(AwsSqsAccountActivationPublisher.class);

    private final ObjectMapper objectMapper;
    private final SqsClient sqsClient;
    private final String queueUrl;

    public AwsSqsAccountActivationPublisher(
            ObjectMapper objectMapper,
            SqsClient sqsClient,
            @Value("${app.account-activation.sqs-queue-url}") String queueUrl) {
        this.objectMapper = objectMapper;
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
    }

    @Override
    public void publishAccountCreated(String email, String fullName, String token, String frontendBaseUrl) {

        if (queueUrl == null || queueUrl.isBlank()) {
            throw new IllegalStateException("Queue URL para ativação de conta não configurada.");
        }

        AccountActivationMessage message = new AccountActivationMessage(
                "ACCOUNT_CREATED",
                email,
                fullName,
                token,
                frontendBaseUrl);

        try {
            String payload = objectMapper.writeValueAsString(message);

            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(payload)
                    .build();

            sqsClient.sendMessage(request);

            log.info("Mensagem de boas-vindas (ativação de conta) publicada na SQS.");

        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar mensagem de ativação de conta.", e);
            throw new IllegalStateException("Falha ao serializar mensagem de ativação de conta.", e);

        } catch (Exception e) {
            log.error("Erro ao publicar mensagem de ativação de conta na SQS. QueueUrl={}", queueUrl, e);
            throw new IllegalStateException("Falha ao publicar mensagem na SQS.", e);
        }
    }
}

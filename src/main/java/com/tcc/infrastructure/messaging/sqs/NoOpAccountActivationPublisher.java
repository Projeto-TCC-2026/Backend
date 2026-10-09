package com.tcc.infrastructure.messaging.sqs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.tcc.application.port.out.AccountActivationPublisher;

@Service
@ConditionalOnProperty(name = "app.account-activation.sqs-enabled", havingValue = "false", matchIfMissing = true)
public class NoOpAccountActivationPublisher implements AccountActivationPublisher {

    private static final Logger log = LoggerFactory.getLogger(NoOpAccountActivationPublisher.class);

    @Override
    public void publishAccountCreated(String email, String fullName, String token, String frontendBaseUrl) {
        log.error("SQS de ativação desabilitado; e-mail de boas-vindas não foi publicado.");
        throw new IllegalStateException(
                "Envio do e-mail de ativação está desabilitado. Configure a fila SQS de boas-vindas.");
    }
}

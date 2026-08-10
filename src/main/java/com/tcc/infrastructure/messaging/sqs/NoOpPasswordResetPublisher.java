package com.tcc.infrastructure.messaging.sqs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.tcc.application.port.out.PasswordResetPublisher;

@Service
@ConditionalOnProperty(name = "app.password-reset.sqs-enabled", havingValue = "false", matchIfMissing = true)
public class NoOpPasswordResetPublisher
        implements PasswordResetPublisher {

    private static final Logger log = LoggerFactory.getLogger(
            NoOpPasswordResetPublisher.class);

    @Override
    public void publishResetRequested(
            String email,
            String token,
            String frontendBaseUrl) {

        log.warn(
                "SQS desabilitado. Solicitação de recuperação de senha não foi publicada.");
    }
}
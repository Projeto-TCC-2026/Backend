package com.tcc.infrastructure.messaging.email;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.tcc.application.port.out.AlertEmailSender;

/**
 * Implementação ativa quando {@code app.alert-email.enabled=false} (o padrão).
 * Mesmo arranjo do {@code NoOpPushNotificationPublisher}: o ambiente de
 * desenvolvimento sobe sem depender da AWS.
 */
@Service
@ConditionalOnProperty(name = "app.alert-email.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpAlertEmailSender implements AlertEmailSender {

    private static final Logger log = LoggerFactory.getLogger(NoOpAlertEmailSender.class);

    /**
     * Devolve false: nada foi entregue, então a notificação correspondente é
     * gravada como FAILED. Responder true aqui faria o banco afirmar que um
     * médico foi avisado quando nenhum e-mail saiu.
     */
    @Override
    public boolean sendAlertEmail(String recipientEmail, String subject, String body, UUID alertId) {
        log.warn("E-mail de alerta desabilitado. Aviso do alerta {} nao foi enviado.", alertId);
        return false;
    }
}

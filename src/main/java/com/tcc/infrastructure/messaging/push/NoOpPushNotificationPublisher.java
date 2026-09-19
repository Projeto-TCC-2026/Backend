package com.tcc.infrastructure.messaging.push;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.tcc.application.port.out.PushNotificationPublisher;

@Service
@ConditionalOnProperty(name = "app.push.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpPushNotificationPublisher implements PushNotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(NoOpPushNotificationPublisher.class);

    /**
     * Sempre devolve lista vazia: sem chamada ao provedor não existe ticket de erro,
     * então nenhum token pode ser considerado inválido.
     */
    @Override
    public List<String> publishAlertCreated(List<String> pushTokens, String title, String body, UUID alertId) {
        log.warn("Push desabilitado. Notificacao do alerta {} nao foi enviada para {} dispositivo(s).",
                alertId, pushTokens == null ? 0 : pushTokens.size());
        return List.of();
    }
}

package com.tcc.application.port.out;

import java.util.List;
import java.util.UUID;

public interface PushNotificationPublisher {

    /**
     * Envia a notificação de alerta para os tokens informados.
     *
     * @param pushTokens tokens de destino; a implementação não os registra em log
     * @param title      título exibido na notificação
     * @param body       texto exibido na notificação
     * @param alertId    vai no campo {@code data} da mensagem, para o app abrir o
     *                   alerta correto ao receber o toque na notificação
     * @return tokens que o provedor recusou por não estarem mais registrados em
     *         nenhum dispositivo, e que portanto devem ser removidos. Lista vazia
     *         quando não há token a descartar, inclusive em caso de falha de envio.
     */
    List<String> publishAlertCreated(List<String> pushTokens, String title, String body, UUID alertId);
}

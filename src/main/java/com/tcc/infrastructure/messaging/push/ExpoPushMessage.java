package com.tcc.infrastructure.messaging.push;

import java.util.Map;

/**
 * Mensagem individual aceita pelo Expo Push Service.
 *
 * @param to    token de push do dispositivo de destino
 * @param title título da notificação
 * @param body  texto da notificação
 * @param data  carga livre entregue ao app; aqui transporta o alertId
 */
public record ExpoPushMessage(
        String to,
        String title,
        String body,
        Map<String, String> data
) {
}

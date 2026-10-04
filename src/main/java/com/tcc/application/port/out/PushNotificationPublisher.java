package com.tcc.application.port.out;

import java.util.List;
import java.util.Map;
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

    /**
     * Mesma coisa, com carga e categoria extras na mensagem.
     *
     * <p>Usado pelo push da pergunta à paciente (alerta AWAITING_PATIENT), que
     * precisa de dois acréscimos em relação ao push comum: um par
     * {@code "type": "SEVERE_CHECK"} no {@code data}, para o app saber que deve
     * mostrar os botões de resposta, e uma categoria de notificação, para o sistema
     * operacional oferecer as ações direto na notificação.
     *
     * @param extraData  pares adicionais do campo {@code data}; o {@code alertId}
     *                   continua sendo acrescentado pela implementação
     * @param categoryId identificador da categoria de notificação, ou nulo para
     *                   omitir o campo
     * @return os mesmos tokens a descartar de {@link #publishAlertCreated}
     */
    List<String> publishAlertCreated(List<String> pushTokens, String title, String body, UUID alertId,
                                     Map<String, String> extraData, String categoryId);
}

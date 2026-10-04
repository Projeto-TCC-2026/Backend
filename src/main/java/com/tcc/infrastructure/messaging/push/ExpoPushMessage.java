package com.tcc.infrastructure.messaging.push;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Mensagem individual aceita pelo Expo Push Service.
 *
 * <p>Os nomes dos campos são os do "message request format" do Expo, em que só
 * {@code to} é obrigatório.
 *
 * @param to         token de push do dispositivo de destino
 * @param title      título da notificação
 * @param body       texto da notificação
 * @param data       carga livre entregue ao app; aqui transporta o alertId e, no
 *                   push da pergunta à paciente, também o type SEVERE_CHECK
 * @param categoryId identificador da categoria de notificação, que o Expo
 *                   documenta como campo de texto válido em Android e iOS. É o que
 *                   permite ao sistema mostrar as ações da categoria direto na
 *                   notificação. Nulo é omitido do JSON: mandar
 *                   {@code "categoryId": null} não é o mesmo que não mandar o
 *                   campo, e o push comum não tem categoria
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExpoPushMessage(
        String to,
        String title,
        String body,
        Map<String, String> data,
        String categoryId
) {

    /** Mensagem sem categoria, como no push comum de alerta. */
    public ExpoPushMessage(String to, String title, String body, Map<String, String> data) {
        this(to, title, body, data, null);
    }
}

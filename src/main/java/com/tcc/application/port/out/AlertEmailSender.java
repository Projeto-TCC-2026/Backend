package com.tcc.application.port.out;

import java.util.UUID;

/**
 * Envio do aviso de alerta por e-mail ao médico responsável pelo paciente.
 *
 * <p>Mesma forma do {@link PushNotificationPublisher}: a implementação real é
 * trocada por uma NoOp através de propriedade, e nenhuma implementação propaga
 * exceção. O resultado booleano é o que decide o status gravado em
 * {@code notifications} (SENT ou FAILED).
 */
public interface AlertEmailSender {

    /**
     * Envia um e-mail de texto simples.
     *
     * @param recipientEmail destinatário; a implementação não o registra em log
     * @param subject        assunto, em português
     * @param body           corpo em texto simples, em português
     * @param alertId        só para correlação em log; não vai no conteúdo
     * @return {@code true} quando o provedor aceitou a mensagem. {@code false} em
     *         qualquer falha. Nunca lança exceção.
     */
    boolean sendAlertEmail(String recipientEmail, String subject, String body, UUID alertId);
}

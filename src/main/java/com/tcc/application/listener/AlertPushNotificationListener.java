package com.tcc.application.listener;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.tcc.application.port.out.PushNotificationPublisher;
import com.tcc.domain.event.AlertCreatedEvent;
import com.tcc.domain.model.Alert;
import com.tcc.domain.model.AlertStatus;
import com.tcc.domain.model.DeviceToken;
import com.tcc.domain.repository.AlertRepository;
import com.tcc.domain.repository.DeviceTokenRepository;

@Component
public class AlertPushNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(AlertPushNotificationListener.class);

    private static final Map<String, String> TITLE_BY_SEVERITY = Map.of(
            "CRITICAL", "Alerta crítico de saúde",
            "HIGH", "Alerta de saúde importante",
            "MEDIUM", "Atenção à sua leitura",
            "LOW", "Aviso sobre sua leitura");

    private static final String DEFAULT_TITLE = "Alerta de saúde";

    /** Usado quando o alerta não tem leitura associada e o tipo é desconhecido. */
    private static final String FALLBACK_READING_LABEL = "sinal vital";

    /**
     * Texto do push ao paciente.
     *
     * <p>O alerta ainda está UNCONFIRMED neste momento: uma única leitura saiu da
     * faixa e nada foi confirmado. Por isso a mensagem não afirma que há um problema
     * — ela informa o desvio e oferece o caminho de ação no app, sem alarmar.
     */
    private static final String BODY_TEMPLATE =
            "Sua medição de %s saiu do normal. Se não estiver bem, "
                    + "use a opção 'Não estou bem' no app.";

    /**
     * Texto do push de alerta AWAITING_PATIENT, disparado por uma leitura GRAVE.
     *
     * <p>Aqui o app não espera que a paciente procure a opção no menu: a notificação
     * faz a pergunta direto, porque a resposta dela é o que decide se o médico é
     * avisado nos próximos 10 minutos.
     *
     * <p>Sem o valor medido no texto, pelo mesmo motivo do push comum: número de
     * sinal vital na tela de bloqueio é informação clínica crua, e aqui o número é
     * justamente o mais assustador. A pergunta basta.
     */
    private static final String SEVERE_BODY_TEMPLATE =
            "Sua medição de %s está muito fora do normal. Você está bem?";

    /** Título do push da pergunta, sem o vocabulário de severidade do push comum. */
    private static final String SEVERE_TITLE = "Precisamos saber como você está";

    /** Chave e valor que fazem o app mostrar os botões de resposta. */
    private static final String DATA_KEY_TYPE = "type";
    private static final String DATA_TYPE_SEVERE_CHECK = "SEVERE_CHECK";

    /**
     * Categoria de notificação da pergunta. O Expo documenta {@code categoryId} como
     * campo de texto da mensagem, válido em Android e iOS, e é ele que permite ao
     * sistema oferecer as ações da categoria direto na notificação. O app precisa ter
     * registrado uma categoria com este identificador; se não tiver, a notificação
     * chega sem as ações e o {@code data} ainda leva o type, então o fluxo continua
     * funcionando pela tela do alerta.
     */
    private static final String SEVERE_CHECK_CATEGORY_ID = "severe-check";

    private final AlertRepository alertRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final PushNotificationPublisher pushNotificationPublisher;

    public AlertPushNotificationListener(AlertRepository alertRepository,
                                         DeviceTokenRepository deviceTokenRepository,
                                         PushNotificationPublisher pushNotificationPublisher) {
        this.alertRepository = alertRepository;
        this.deviceTokenRepository = deviceTokenRepository;
        this.pushNotificationPublisher = pushNotificationPublisher;
    }

    /**
     * Dispara o push depois que a transação do alerta foi confirmada.
     *
     * <p>AFTER_COMMIT garante que nunca se notifica um alerta que acabou sofrendo
     * rollback. Como consequência, este método roda fora da transação original e
     * abre uma nova (REQUIRES_NEW) para poder navegar os relacionamentos LAZY do
     * alerta, que já vêm desanexados no evento.
     *
     * <p>Todo o corpo é envolvido em try/catch porque exceção lançada aqui não tem
     * para onde ir: o alerta já está gravado e a resposta do endpoint já foi
     * decidida. Falha de push é registrada e descartada.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAlertCreated(AlertCreatedEvent event) {
        UUID alertId = event.alert().getId();

        try {
            notifyPatient(alertId);
        } catch (Exception e) {
            log.error("Falha ao notificar o alerta {}. exception={}",
                    alertId, e.getClass().getSimpleName());
        }
    }

    private void notifyPatient(UUID alertId) {
        Alert alert = alertRepository.findById(alertId).orElse(null);

        if (alert == null) {
            log.warn("Alerta {} não encontrado ao preparar a notificacao.", alertId);
            return;
        }

        UUID userId = alert.getPatient().getUser().getId();
        List<DeviceToken> deviceTokens = deviceTokenRepository.findByUserId(userId);

        if (deviceTokens.isEmpty()) {
            log.debug("Usuario {} nao possui dispositivo registrado. Push do alerta {} ignorado.",
                    userId, alertId);
            return;
        }

        List<String> tokens = deviceTokens.stream().map(DeviceToken::getToken).toList();

        List<String> unregistered = isAwaitingPatient(alert)
                ? publishSevereCheck(tokens, alert, alertId)
                : pushNotificationPublisher.publishAlertCreated(
                        tokens, buildTitle(alert), buildBody(alert), alertId);

        removeUnregistered(deviceTokens, unregistered, userId);
    }

    /**
     * Alerta criado por leitura grave, que pergunta à paciente se ela está bem.
     *
     * <p>O status é a única diferença entre os dois pushes, e ele chega aqui pelo
     * alerta recarregado do banco — não pelo evento, cujo alerta vem desanexado.
     */
    private boolean isAwaitingPatient(Alert alert) {
        return AlertStatus.AWAITING_PATIENT.equals(alert.getStatus());
    }

    /**
     * Push da pergunta. Além do texto próprio, leva dois acréscimos que o push comum
     * não tem: {@code type = SEVERE_CHECK} no {@code data}, para o app mostrar os
     * botões de resposta, e a categoria de notificação, para o sistema oferecer as
     * ações direto na notificação. O {@code alertId} continua no {@code data}, posto
     * lá pelo publisher, e é com ele que o app sabe qual alerta responder.
     */
    private List<String> publishSevereCheck(List<String> tokens, Alert alert, UUID alertId) {
        return pushNotificationPublisher.publishAlertCreated(
                tokens,
                SEVERE_TITLE,
                SEVERE_BODY_TEMPLATE.formatted(readingLabel(alert)),
                alertId,
                Map.of(DATA_KEY_TYPE, DATA_TYPE_SEVERE_CHECK),
                SEVERE_CHECK_CATEGORY_ID);
    }

    /**
     * Remove os tokens que o provedor reportou como não registrados. Usa as
     * entidades já carregadas, casando pelo valor do token, para não precisar de uma
     * segunda consulta.
     */
    private void removeUnregistered(List<DeviceToken> deviceTokens, List<String> unregistered, UUID userId) {
        if (unregistered == null || unregistered.isEmpty()) {
            return;
        }

        List<DeviceToken> toRemove = deviceTokens.stream()
                .filter(deviceToken -> unregistered.contains(deviceToken.getToken()))
                .toList();

        if (toRemove.isEmpty()) {
            return;
        }

        deviceTokenRepository.deleteAll(toRemove);
        log.info("Removido(s) {} token(s) nao registrado(s) do usuario {}.", toRemove.size(), userId);
    }

    private String buildTitle(Alert alert) {
        String severity = alert.getSeverity();

        if (severity == null) {
            return DEFAULT_TITLE;
        }

        return TITLE_BY_SEVERITY.getOrDefault(severity.toUpperCase(), DEFAULT_TITLE);
    }

    /**
     * Monta o corpo a partir do tipo da leitura que originou o alerta.
     *
     * <p>Não usa mais a descrição do alerta: ela carrega o limite numérico violado
     * ("Valor acima do máximo normal de 120.0"), que é informação clínica crua para
     * mostrar ao paciente numa notificação. O tipo da leitura basta para ele
     * entender o que foi medido.
     */
    private String buildBody(Alert alert) {
        return BODY_TEMPLATE.formatted(readingLabel(alert));
    }

    /**
     * Rótulo do tipo de leitura em português. Tipo não mapeado cai no próprio nome
     * técnico, em minúsculas, que ainda é mais informativo que um texto genérico.
     */
    private String readingLabel(Alert alert) {
        if (alert.getHealthReading() == null || alert.getHealthReading().getReadingType() == null) {
            return FALLBACK_READING_LABEL;
        }

        String readingType = alert.getHealthReading().getReadingType();

        return switch (readingType.toUpperCase()) {
            case "HEART_RATE" -> "frequência cardíaca";
            case "SPO2" -> "saturação de oxigênio";
            case "TEMPERATURE" -> "temperatura";
            default -> readingType.toLowerCase().replace('_', ' ');
        };
    }
}

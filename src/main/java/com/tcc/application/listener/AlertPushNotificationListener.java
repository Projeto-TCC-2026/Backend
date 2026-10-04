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

        List<String> unregistered = pushNotificationPublisher.publishAlertCreated(
                tokens, buildTitle(alert), buildBody(alert), alertId);

        removeUnregistered(deviceTokens, unregistered, userId);
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

package com.tcc.infrastructure.messaging.push;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.tcc.application.port.out.PushNotificationPublisher;

@Service
@ConditionalOnProperty(name = "app.push.enabled", havingValue = "true")
public class ExpoPushNotificationPublisher implements PushNotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(ExpoPushNotificationPublisher.class);

    /**
     * Código devolvido pelo Expo quando o token não corresponde mais a nenhuma
     * instalação ativa. É o único erro que justifica descartar o token; os demais
     * são transitórios ou de configuração.
     */
    private static final String DEVICE_NOT_REGISTERED = "DeviceNotRegistered";

    private static final String DATA_KEY_ALERT_ID = "alertId";

    private final RestClient expoPushRestClient;
    private final String endpoint;

    public ExpoPushNotificationPublisher(
            RestClient expoPushRestClient,
            @Value("${app.push.expo-endpoint}") String endpoint) {
        this.expoPushRestClient = expoPushRestClient;
        this.endpoint = endpoint;
    }

    /**
     * Envia um lote com uma mensagem por token. Nenhuma exceção escapa: o retorno é
     * lista vazia quando o envio falha, porque não há como saber, sem resposta, se
     * algum token está inválido. Descartar token nesse cenário silenciaria o
     * paciente por causa de uma falha de rede.
     */
    @Override
    public List<String> publishAlertCreated(List<String> pushTokens, String title, String body, UUID alertId) {
        if (pushTokens == null || pushTokens.isEmpty()) {
            return List.of();
        }

        List<ExpoPushMessage> messages = pushTokens.stream()
                .map(token -> new ExpoPushMessage(token, title, body,
                        Map.of(DATA_KEY_ALERT_ID, alertId.toString())))
                .toList();

        try {
            ExpoPushResponse response = expoPushRestClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(messages)
                    .retrieve()
                    .body(ExpoPushResponse.class);

            return collectUnregisteredTokens(pushTokens, response, alertId);

        } catch (Exception e) {
            log.error("Falha ao enviar push do alerta {}. exception={}",
                    alertId, e.getClass().getSimpleName());
            return List.of();
        }
    }

    /**
     * Casa ticket com token pela posição, que é o contrato do Expo. Se a resposta
     * vier com quantidade diferente da enviada, o pareamento não é confiável e
     * nenhum token é descartado.
     */
    private List<String> collectUnregisteredTokens(List<String> pushTokens,
                                                   ExpoPushResponse response,
                                                   UUID alertId) {
        if (response == null || response.data() == null) {
            log.warn("Resposta do Expo sem tickets para o alerta {}.", alertId);
            return List.of();
        }

        List<ExpoPushResponse.Ticket> tickets = response.data();

        if (tickets.size() != pushTokens.size()) {
            log.warn("Expo devolveu {} tickets para {} mensagens do alerta {}. "
                            + "Nenhum token sera descartado.",
                    tickets.size(), pushTokens.size(), alertId);
            return List.of();
        }

        List<String> unregistered = new ArrayList<>();

        for (int i = 0; i < tickets.size(); i++) {
            ExpoPushResponse.Ticket ticket = tickets.get(i);

            if (!ticket.isError()) {
                continue;
            }

            if (DEVICE_NOT_REGISTERED.equals(ticket.errorCode())) {
                unregistered.add(pushTokens.get(i));
                log.info("Dispositivo nao registrado no envio do alerta {}. Token sera removido.", alertId);
            } else {
                log.warn("Expo recusou uma mensagem do alerta {}. errorCode={}",
                        alertId, ticket.errorCode());
            }
        }

        return unregistered;
    }
}

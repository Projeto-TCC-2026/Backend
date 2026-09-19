package com.tcc.infrastructure.messaging.push;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import org.springframework.web.client.RestClient;

class ExpoPushNotificationPublisherTest {

    private static final String ENDPOINT = "https://exp.host/--/api/v2/push/send";
    private static final String TOKEN_A = "ExponentPushToken[aaa]";
    private static final String TOKEN_B = "ExponentPushToken[bbb]";
    private static final UUID ALERT_ID = UUID.randomUUID();
    private static final String TITLE = "Alerta crítico de saúde";
    private static final String BODY = "Valor acima do máximo normal";

    private MockRestServiceServer server;
    private ExpoPushNotificationPublisher publisher;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        publisher = new ExpoPushNotificationPublisher(builder.build(), ENDPOINT);
    }

    private void respondWith(String json) {
        server.expect(requestTo(ENDPOINT))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }

    @Nested
    @DisplayName("corpo da requisicao")
    class RequestBody {

        @Test
        @DisplayName("deve enviar uma mensagem por token com o alertId no campo data")
        void shouldSendOneMessagePerTokenCarryingAlertId() {
            server.expect(requestTo(ENDPOINT))
                    .andExpect(method(org.springframework.http.HttpMethod.POST))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].to").value(TOKEN_A))
                    .andExpect(jsonPath("$[0].title").value(TITLE))
                    .andExpect(jsonPath("$[0].body").value(BODY))
                    .andExpect(jsonPath("$[0].data.alertId").value(ALERT_ID.toString()))
                    .andExpect(jsonPath("$[1].to").value(TOKEN_B))
                    .andRespond(withSuccess("{\"data\":[{\"status\":\"ok\"},{\"status\":\"ok\"}]}",
                            MediaType.APPLICATION_JSON));

            publisher.publishAlertCreated(List.of(TOKEN_A, TOKEN_B), TITLE, BODY, ALERT_ID);

            server.verify();
        }

        @Test
        @DisplayName("nao deve chamar o provedor quando a lista de tokens esta vazia")
        void shouldNotCallProviderWhenTokenListIsEmpty() {
            List<String> result = publisher.publishAlertCreated(List.of(), TITLE, BODY, ALERT_ID);

            assertThat(result).isEmpty();
            server.verify();
        }

        @Test
        @DisplayName("nao deve chamar o provedor quando a lista de tokens e nula")
        void shouldNotCallProviderWhenTokenListIsNull() {
            List<String> result = publisher.publishAlertCreated(null, TITLE, BODY, ALERT_ID);

            assertThat(result).isEmpty();
            server.verify();
        }
    }

    @Nested
    @DisplayName("tickets de resposta")
    class Tickets {

        @Test
        @DisplayName("deve devolver lista vazia quando todos os tickets vem ok")
        void shouldReturnEmptyWhenEveryTicketIsOk() {
            respondWith("{\"data\":[{\"status\":\"ok\"}]}");

            List<String> result = publisher.publishAlertCreated(List.of(TOKEN_A), TITLE, BODY, ALERT_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("deve devolver o token cujo ticket veio com DeviceNotRegistered")
        void shouldReturnTokenReportedAsDeviceNotRegistered() {
            respondWith("{\"data\":[{\"status\":\"ok\"},"
                    + "{\"status\":\"error\",\"message\":\"nao registrado\","
                    + "\"details\":{\"error\":\"DeviceNotRegistered\"}}]}");

            List<String> result = publisher.publishAlertCreated(
                    List.of(TOKEN_A, TOKEN_B), TITLE, BODY, ALERT_ID);

            assertThat(result).containsExactly(TOKEN_B);
        }

        @Test
        @DisplayName("nao deve devolver token quando o erro e de outro tipo")
        void shouldNotReturnTokenForOtherErrorCodes() {
            respondWith("{\"data\":[{\"status\":\"error\",\"message\":\"limite excedido\","
                    + "\"details\":{\"error\":\"MessageRateExceeded\"}}]}");

            List<String> result = publisher.publishAlertCreated(List.of(TOKEN_A), TITLE, BODY, ALERT_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("nao deve devolver token quando o ticket de erro nao traz details")
        void shouldNotReturnTokenWhenErrorTicketHasNoDetails() {
            respondWith("{\"data\":[{\"status\":\"error\",\"message\":\"falha generica\"}]}");

            List<String> result = publisher.publishAlertCreated(List.of(TOKEN_A), TITLE, BODY, ALERT_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("nao deve descartar token quando a quantidade de tickets difere da enviada")
        void shouldNotDiscardTokenWhenTicketCountDiffers() {
            respondWith("{\"data\":[{\"status\":\"error\","
                    + "\"details\":{\"error\":\"DeviceNotRegistered\"}}]}");

            List<String> result = publisher.publishAlertCreated(
                    List.of(TOKEN_A, TOKEN_B), TITLE, BODY, ALERT_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("deve devolver lista vazia quando a resposta nao tem tickets")
        void shouldReturnEmptyWhenResponseHasNoTickets() {
            respondWith("{}");

            List<String> result = publisher.publishAlertCreated(List.of(TOKEN_A), TITLE, BODY, ALERT_ID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("falha no envio")
    class SendFailure {

        @Test
        @DisplayName("deve devolver lista vazia sem lancar excecao quando o provedor responde 500")
        void shouldSwallowServerError() {
            server.expect(requestTo(ENDPOINT)).andRespond(withServerError());

            List<String> result = publisher.publishAlertCreated(List.of(TOKEN_A), TITLE, BODY, ALERT_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("deve devolver lista vazia sem lancar excecao quando o provedor responde 400")
        void shouldSwallowClientError() {
            server.expect(requestTo(ENDPOINT))
                    .andRespond(withStatus(HttpStatus.BAD_REQUEST));

            List<String> result = publisher.publishAlertCreated(List.of(TOKEN_A), TITLE, BODY, ALERT_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("nao deve lancar excecao quando o corpo da resposta e invalido")
        void shouldSwallowMalformedBody() {
            respondWith("nao é json");

            assertThatCode(() -> publisher.publishAlertCreated(List.of(TOKEN_A), TITLE, BODY, ALERT_ID))
                    .doesNotThrowAnyException();
        }
    }
}

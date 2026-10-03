package com.tcc.exception;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

/**
 * Corpo ilegível tem de virar 400, não 500.
 *
 * <p>Antes deste handler o caso caía no {@code handleGeneric} e virava 500, o que
 * faria a fila de integração reenviar indefinidamente um payload que nunca seria
 * aceito — por exemplo {@code measuredAt} sem fuso.
 */
@DisplayName("GlobalExceptionHandler: corpo ilegível")
class GlobalExceptionHandlerUnreadableBodyTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private HttpMessageNotReadableException exceptionOf(String message) {
        return new HttpMessageNotReadableException(message, new RuntimeException(message), null);
    }

    @Test
    @DisplayName("deve responder 400 no formato padrão do projeto")
    void shouldReturnBadRequestInProjectFormat() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleUnreadableBody(exceptionOf("JSON parse error"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo(400);
        assertThat(body.get("error")).isEqualTo(HttpStatus.BAD_REQUEST.getReasonPhrase());
        assertThat(body).containsKeys("timestamp", "status", "error", "message");
    }

    @Test
    @DisplayName("mensagem deve estar em português e orientar sobre o fuso")
    void shouldReturnPortugueseMessageMentioningTimezone() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleUnreadableBody(exceptionOf("JSON parse error"));

        String message = String.valueOf(response.getBody().get("message"));

        assertThat(message)
                .contains("Corpo da requisição inválido")
                .contains("fuso")
                .contains("2026-07-01T14:32:00Z");
    }

    /**
     * O texto da exceção do Jackson cita o conteúdo do campo que falhou, e o corpo
     * da requisição carrega dado de saúde. A resposta não pode repassar isso.
     */
    @Test
    @DisplayName("não deve repassar o detalhe da exceção do Jackson na resposta")
    void shouldNotLeakJacksonDetailIntoResponse() {
        String sensitive = "Cannot deserialize value \"PACIENTE-SIGILOSO-123\"";

        ResponseEntity<Map<String, Object>> response =
                handler.handleUnreadableBody(exceptionOf(sensitive));

        assertThat(String.valueOf(response.getBody().get("message")))
                .doesNotContain("PACIENTE-SIGILOSO-123");
    }
}

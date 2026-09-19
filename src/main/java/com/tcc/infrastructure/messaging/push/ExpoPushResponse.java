package com.tcc.infrastructure.messaging.push;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Resposta do Expo Push Service.
 *
 * <p>O campo {@code data} traz um ticket por mensagem enviada, na mesma ordem da
 * requisição. É essa correspondência posicional que permite descobrir qual token
 * foi recusado, já que o ticket não repete o token.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExpoPushResponse(List<Ticket> data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Ticket(String status, String message, Details details) {

        public boolean isError() {
            return "error".equalsIgnoreCase(status);
        }

        public String errorCode() {
            return details == null ? null : details.error();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Details(String error) {
    }
}

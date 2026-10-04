package com.tcc.presentation.controller;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.tcc.application.dto.request.AlertEvaluationRequest;
import com.tcc.application.dto.response.AlertEvaluationResponse;
import com.tcc.application.dto.response.ApiResponse;
import com.tcc.application.service.AlertDuplicateReadingException;
import com.tcc.application.service.AlertService;
import com.tcc.domain.model.AlertStatus;
import com.tcc.exception.ResourceNotFoundException;

/**
 * Códigos HTTP do endpoint de integração.
 *
 * <p>Quem chama é uma fila com retentativa, então o status decide se a mensagem
 * volta. Leitura repetida tem de ser 2xx; dado inválido e paciente inexistente,
 * 4xx (os dois vêm do GlobalExceptionHandler, provado pelo tipo da exceção que
 * sobe daqui); erro inesperado, 5xx.
 */
@ExtendWith(MockitoExtension.class)
class AlertControllerTest {

    @Mock
    private AlertService alertService;

    @InjectMocks
    private AlertController alertController;

    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID READING_ID = UUID.randomUUID();
    private static final UUID ALERT_ID = UUID.randomUUID();

    private AlertEvaluationRequest request() {
        return new AlertEvaluationRequest(
                PATIENT_ID, "HEART_RATE", 155.0, OffsetDateTime.of(2026, 9, 29, 14, 30, 0, 0, ZoneOffset.UTC), "bpm");
    }

    @Nested
    @DisplayName("leitura nova")
    class NewReading {

        @Test
        @DisplayName("leitura gravada com alerta UNCONFIRMED deve responder 201")
        void shouldReturnCreatedWhenAlertIsGenerated() {
            when(alertService.evaluateReading(any())).thenReturn(
                    AlertEvaluationResponse.withAlert("CRITICAL", ALERT_ID, "Valor acima",
                            READING_ID, AlertStatus.UNCONFIRMED));

            ResponseEntity<ApiResponse<AlertEvaluationResponse>> response =
                    alertController.evaluateReading(request());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getData().alertGenerated()).isTrue();
            assertThat(response.getBody().getData().healthReadingId()).isEqualTo(READING_ID);
            assertThat(response.getBody().getData().alertStatus()).isEqualTo(AlertStatus.UNCONFIRMED);
            assertThat(response.getBody().getData().suspectReading()).isFalse();
        }

        @Test
        @DisplayName("leitura gravada sem alerta deve responder 201")
        void shouldReturnCreatedWhenNoAlertIsGenerated() {
            when(alertService.evaluateReading(any()))
                    .thenReturn(AlertEvaluationResponse.withoutAlert(READING_ID));

            ResponseEntity<ApiResponse<AlertEvaluationResponse>> response =
                    alertController.evaluateReading(request());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().getData().alertGenerated()).isFalse();
            assertThat(response.getBody().getData().duplicateReading()).isFalse();
            assertThat(response.getBody().getData().alertStatus()).isNull();
        }

        @Test
        @DisplayName("alerta confirmado nesta chamada deve responder 201 com PENDING e alertGenerated false")
        void shouldReturnCreatedWhenAlertIsConfirmed() {
            when(alertService.evaluateReading(any())).thenReturn(
                    AlertEvaluationResponse.withUpdatedAlert("CRITICAL", ALERT_ID, "Valor acima",
                            READING_ID, AlertStatus.PENDING));

            ResponseEntity<ApiResponse<AlertEvaluationResponse>> response =
                    alertController.evaluateReading(request());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            // Nenhum alerta novo foi criado: o existente foi promovido.
            assertThat(response.getBody().getData().alertGenerated()).isFalse();
            assertThat(response.getBody().getData().alertStatus()).isEqualTo(AlertStatus.PENDING);
            assertThat(response.getBody().getData().alertId()).isEqualTo(ALERT_ID);
        }
    }

    @Nested
    @DisplayName("leitura implausível")
    class SuspectReading {

        /**
         * 201 e não 4xx de propósito: um 4xx faria a fila mandar a mensagem para a
         * DLQ e a leitura se perderia, quando o certo é registrá-la como suspeita.
         */
        @Test
        @DisplayName("leitura suspeita deve responder 201 com suspectReading true")
        void shouldReturnCreatedForSuspectReading() {
            when(alertService.evaluateReading(any())).thenReturn(
                    AlertEvaluationResponse.suspect(READING_ID, "Valor fora da faixa plausível"));

            ResponseEntity<ApiResponse<AlertEvaluationResponse>> response =
                    alertController.evaluateReading(request());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData().suspectReading()).isTrue();
            assertThat(response.getBody().getData().alertGenerated()).isFalse();
            assertThat(response.getBody().getData().alertStatus()).isNull();
            assertThat(response.getBody().getData().healthReadingId()).isEqualTo(READING_ID);
        }
    }

    @Nested
    @DisplayName("leitura repetida")
    class DuplicateReading {

        @Test
        @DisplayName("repetida detectada por consulta deve responder 200")
        void shouldReturnOkForDuplicateDetectedByQuery() {
            when(alertService.evaluateReading(any()))
                    .thenReturn(AlertEvaluationResponse.duplicate(READING_ID));

            ResponseEntity<ApiResponse<AlertEvaluationResponse>> response =
                    alertController.evaluateReading(request());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData().duplicateReading()).isTrue();
            assertThat(response.getBody().getData().alertGenerated()).isFalse();
        }

        @Test
        @DisplayName("repetida barrada pela unicidade do banco tambem deve responder 200")
        void shouldReturnOkWhenUniqueConstraintRejectsTheReading() {
            when(alertService.evaluateReading(any()))
                    .thenThrow(new AlertDuplicateReadingException(new RuntimeException("uq...")));

            ResponseEntity<ApiResponse<AlertEvaluationResponse>> response =
                    alertController.evaluateReading(request());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData().duplicateReading()).isTrue();
        }
    }

    @Nested
    @DisplayName("erros")
    class Errors {

        @Test
        @DisplayName("paciente inexistente deve subir ResourceNotFoundException, que o handler traduz em 404")
        void shouldPropagateNotFound() {
            when(alertService.evaluateReading(any()))
                    .thenThrow(new ResourceNotFoundException("Paciente não encontrado com ID: " + PATIENT_ID));

            AlertEvaluationRequest request = request();

            assertThatThrownBy(() -> alertController.evaluateReading(request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("erro inesperado deve subir, para o handler traduzir em 500")
        void shouldPropagateUnexpectedError() {
            when(alertService.evaluateReading(any()))
                    .thenThrow(new IllegalStateException("falha inesperada"));

            AlertEvaluationRequest request = request();

            assertThatThrownBy(() -> alertController.evaluateReading(request))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}

package com.tcc.presentation.controller;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import com.tcc.application.dto.request.PatientAlertResponseRequest;
import com.tcc.application.dto.response.ApiResponse;
import com.tcc.application.dto.response.PatientAlertAnswerResponse;
import com.tcc.application.service.AlertResponseWindowClosedException;
import com.tcc.application.service.AlertService;
import com.tcc.domain.model.AlertStatus;
import com.tcc.domain.model.PatientAlertAnswer;
import com.tcc.exception.ResourceNotFoundException;
import com.tcc.exception.UnauthorizedException;

/**
 * Códigos HTTP da resposta da paciente ao alerta de leitura grave.
 *
 * <p>O caso central é o 409: ele não existe no {@code GlobalExceptionHandler} e é
 * montado aqui, então precisa estar coberto no controller. Os demais códigos vêm
 * do handler, provados pelo tipo da exceção que sobe daqui.
 */
@ExtendWith(MockitoExtension.class)
class MobileAlertControllerTest {

    @Mock
    private AlertService alertService;

    @InjectMocks
    private MobileAlertController controller;

    private static final UUID ALERT_ID = UUID.randomUUID();
    private static final String PATIENT_EMAIL = "patient@tcc.com";

    private Authentication authentication;

    @BeforeEach
    void setUp() {
        UserDetails principal = User.withUsername(PATIENT_EMAIL)
                .password("hash")
                .authorities("ROLE_PATIENT")
                .build();

        authentication = new UsernamePasswordAuthenticationToken(
                principal, "hash", principal.getAuthorities());
    }

    private PatientAlertResponseRequest requestOf(PatientAlertAnswer answer) {
        return new PatientAlertResponseRequest(answer);
    }

    @Nested
    @DisplayName("resposta registrada")
    class Accepted {

        @Test
        @DisplayName("NOT_OK deve responder 200 com PENDING e doctorNotified true")
        void shouldReturnOkForNotOk() {
            LocalDateTime respondedAt = LocalDateTime.of(2026, 9, 29, 15, 0);

            when(alertService.registerPatientResponse(
                    eq(PATIENT_EMAIL), eq(ALERT_ID), eq(PatientAlertAnswer.NOT_OK)))
                    .thenReturn(new PatientAlertAnswerResponse(
                            ALERT_ID, PatientAlertAnswer.NOT_OK, respondedAt,
                            AlertStatus.PENDING, true));

            ResponseEntity<?> response = controller.respondToAlert(
                    authentication, ALERT_ID, requestOf(PatientAlertAnswer.NOT_OK));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            @SuppressWarnings("unchecked")
            ApiResponse<PatientAlertAnswerResponse> body =
                    (ApiResponse<PatientAlertAnswerResponse>) response.getBody();

            assertThat(body).isNotNull();
            assertThat(body.isSuccess()).isTrue();
            assertThat(body.getData().alertStatus()).isEqualTo(AlertStatus.PENDING);
            assertThat(body.getData().doctorNotified()).isTrue();
            assertThat(body.getData().respondedAt()).isEqualTo(respondedAt);
        }

        @Test
        @DisplayName("OK deve responder 200 com UNCONFIRMED e doctorNotified false")
        void shouldReturnOkForOk() {
            when(alertService.registerPatientResponse(
                    eq(PATIENT_EMAIL), eq(ALERT_ID), eq(PatientAlertAnswer.OK)))
                    .thenReturn(new PatientAlertAnswerResponse(
                            ALERT_ID, PatientAlertAnswer.OK, LocalDateTime.of(2026, 9, 29, 15, 0),
                            AlertStatus.UNCONFIRMED, false));

            ResponseEntity<?> response = controller.respondToAlert(
                    authentication, ALERT_ID, requestOf(PatientAlertAnswer.OK));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            @SuppressWarnings("unchecked")
            ApiResponse<PatientAlertAnswerResponse> body =
                    (ApiResponse<PatientAlertAnswerResponse>) response.getBody();

            assertThat(body.getData().alertStatus()).isEqualTo(AlertStatus.UNCONFIRMED);
            assertThat(body.getData().doctorNotified()).isFalse();
        }
    }

    @Nested
    @DisplayName("janela de resposta fechada")
    class WindowClosed {

        @SuppressWarnings("unchecked")
        private Map<String, Object> bodyOf(ResponseEntity<?> response) {
            return (Map<String, Object>) response.getBody();
        }

        @Test
        @DisplayName("prazo vencido deve responder 409 com mensagem em portugues")
        void shouldReturnConflictWhenDeadlineExpired() {
            when(alertService.registerPatientResponse(
                    eq(PATIENT_EMAIL), eq(ALERT_ID), eq(PatientAlertAnswer.NOT_OK)))
                    .thenThrow(AlertResponseWindowClosedException.deadlineExpired());

            ResponseEntity<?> response = controller.respondToAlert(
                    authentication, ALERT_ID, requestOf(PatientAlertAnswer.NOT_OK));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(bodyOf(response))
                    .containsEntry("status", HttpStatus.CONFLICT.value());
            assertThat(bodyOf(response).get("message").toString())
                    .contains("prazo")
                    .contains("médico foi avisado");
        }

        @Test
        @DisplayName("resposta ja registrada deve responder 409 dizendo isso")
        void shouldReturnConflictWhenAlreadyAnswered() {
            when(alertService.registerPatientResponse(
                    eq(PATIENT_EMAIL), eq(ALERT_ID), eq(PatientAlertAnswer.OK)))
                    .thenThrow(AlertResponseWindowClosedException.alreadyAnswered());

            ResponseEntity<?> response = controller.respondToAlert(
                    authentication, ALERT_ID, requestOf(PatientAlertAnswer.OK));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(bodyOf(response).get("message").toString())
                    .contains("já foi registrada");
        }
    }

    @Nested
    @DisplayName("erros traduzidos pelo handler global")
    class PropagatedErrors {

        @Test
        @DisplayName("alerta de outro paciente deve subir UnauthorizedException")
        void shouldPropagateUnauthorized() {
            when(alertService.registerPatientResponse(
                    eq(PATIENT_EMAIL), eq(ALERT_ID), eq(PatientAlertAnswer.NOT_OK)))
                    .thenThrow(new UnauthorizedException("Alerta não pertence ao paciente autenticado"));

            PatientAlertResponseRequest request = requestOf(PatientAlertAnswer.NOT_OK);

            assertThatThrownBy(() -> controller.respondToAlert(authentication, ALERT_ID, request))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("alerta inexistente deve subir ResourceNotFoundException")
        void shouldPropagateNotFound() {
            when(alertService.registerPatientResponse(
                    eq(PATIENT_EMAIL), eq(ALERT_ID), eq(PatientAlertAnswer.OK)))
                    .thenThrow(new ResourceNotFoundException("Alerta não encontrado com ID: " + ALERT_ID));

            PatientAlertResponseRequest request = requestOf(PatientAlertAnswer.OK);

            assertThatThrownBy(() -> controller.respondToAlert(authentication, ALERT_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}

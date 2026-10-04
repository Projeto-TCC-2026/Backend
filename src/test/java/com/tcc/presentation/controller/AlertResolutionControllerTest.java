package com.tcc.presentation.controller;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import com.tcc.application.dto.response.ApiResponse;
import com.tcc.application.dto.response.DoctorAlertResponse;
import com.tcc.application.service.AlertService;
import com.tcc.domain.event.AlertConfirmationReason;
import com.tcc.domain.model.AlertStatus;
import com.tcc.exception.BusinessException;

/**
 * Contrato HTTP dos endpoints de alerta do médico.
 *
 * <p>Verifica o que o controller controla: o e-mail do médico extraído do token e
 * não de parâmetro, o envelope {@code ApiResponse}, e o repasse do filtro e da
 * paginação ao service. As regras de escopo e de status estão no service, e são
 * testadas lá.
 */
@ExtendWith(MockitoExtension.class)
class AlertResolutionControllerTest {

    @Mock
    private AlertService alertService;

    @InjectMocks
    private AlertResolutionController controller;

    private static final String DOCTOR_EMAIL = "doctor@tcc.com";
    private static final UUID ALERT_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final Pageable FIRST_PAGE = PageRequest.of(0, 20);

    private Authentication authentication;

    @BeforeEach
    void setUp() {
        UserDetails principal = User.withUsername(DOCTOR_EMAIL)
                .password("hash")
                .authorities("ROLE_DOCTOR")
                .build();

        authentication = new UsernamePasswordAuthenticationToken(
                principal, "hash", principal.getAuthorities());
    }

    private DoctorAlertResponse alertOf(String status, AlertConfirmationReason reason) {
        OffsetDateTime measuredAt = OffsetDateTime.of(
                2026, 8, 29, 14, 40, 0, 0, ZoneOffset.UTC);

        return new DoctorAlertResponse(
                ALERT_ID, PATIENT_ID, "Paciente de Teste", "HEART_RATE", "38.0", "bpm",
                measuredAt, "CRITICAL", "Leitura fora da faixa normal: HEART_RATE",
                status, reason == null ? null : measuredAt, null, reason);
    }

    @SuppressWarnings("unchecked")
    private ApiResponse<Page<DoctorAlertResponse>> bodyOf(
            ResponseEntity<ApiResponse<Page<DoctorAlertResponse>>> response) {
        return response.getBody();
    }

    @Nested
    @DisplayName("listagem de alertas")
    class Listing {

        @Test
        @DisplayName("deve responder 200 com a pagina embrulhada em ApiResponse")
        void shouldReturnWrappedPage() {
            DoctorAlertResponse alert =
                    alertOf(AlertStatus.PENDING, AlertConfirmationReason.DUAS_LEITURAS);

            when(alertService.listForDoctor(eq(DOCTOR_EMAIL), isNull(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(alert), FIRST_PAGE, 1));

            ResponseEntity<ApiResponse<Page<DoctorAlertResponse>>> response =
                    controller.listAlerts(authentication, null, FIRST_PAGE);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(bodyOf(response).isSuccess()).isTrue();
            assertThat(bodyOf(response).getData().getContent()).containsExactly(alert);
            assertThat(bodyOf(response).getData().getTotalElements()).isEqualTo(1);
        }

        /**
         * A identidade do médico vem do token. Não há parâmetro de médico no
         * endpoint, e é isso que impede um médico de listar alertas de outro.
         */
        @Test
        @DisplayName("deve derivar o medico do token, nao de parametro")
        void shouldDeriveDoctorFromToken() {
            when(alertService.listForDoctor(eq(DOCTOR_EMAIL), isNull(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            controller.listAlerts(authentication, null, FIRST_PAGE);

            verify(alertService).listForDoctor(eq(DOCTOR_EMAIL), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("deve repassar o filtro de status e a paginacao ao service")
        void shouldForwardStatusFilterAndPageable() {
            Pageable secondPage = PageRequest.of(1, 5);

            when(alertService.listForDoctor(DOCTOR_EMAIL, AlertStatus.RESOLVED, secondPage))
                    .thenReturn(new PageImpl<>(List.of()));

            controller.listAlerts(authentication, AlertStatus.RESOLVED, secondPage);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(alertService).listForDoctor(
                    eq(DOCTOR_EMAIL), eq(AlertStatus.RESOLVED), captor.capture());

            assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
            assertThat(captor.getValue().getPageSize()).isEqualTo(5);
        }

        @Test
        @DisplayName("alerta AWAITING_PATIENT vem sem motivo de confirmacao")
        void shouldExposeNullReasonForAwaitingPatient() {
            DoctorAlertResponse awaiting = alertOf(AlertStatus.AWAITING_PATIENT, null);

            when(alertService.listForDoctor(eq(DOCTOR_EMAIL), isNull(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(awaiting)));

            ResponseEntity<ApiResponse<Page<DoctorAlertResponse>>> response =
                    controller.listAlerts(authentication, null, FIRST_PAGE);

            DoctorAlertResponse item = bodyOf(response).getData().getContent().get(0);

            assertThat(item.status()).isEqualTo(AlertStatus.AWAITING_PATIENT);
            assertThat(item.confirmationReason()).isNull();
            assertThat(item.confirmedAt()).isNull();
        }

        /**
         * Status inválido sobe como {@link BusinessException}, que o
         * {@code GlobalExceptionHandler} traduz em 400. O controller não monta
         * resposta de erro.
         */
        @Test
        @DisplayName("status invalido deve subir BusinessException, que o handler traduz em 400")
        void shouldPropagateBusinessExceptionForInvalidStatus() {
            when(alertService.listForDoctor(eq(DOCTOR_EMAIL), eq("ARQUIVADO"), any(Pageable.class)))
                    .thenThrow(new BusinessException("Status inválido. Valores aceitos: "
                            + "PENDING, AWAITING_PATIENT, RESOLVED"));

            assertThatThrownBy(() -> controller.listAlerts(authentication, "ARQUIVADO", FIRST_PAGE))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Status inválido");
        }
    }
}

package com.tcc.infrastructure.scheduler;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tcc.application.service.AlertService;

/**
 * O agendador não decide nada: ele pede a lista ao service e chama o service uma
 * vez por alerta. Nenhum horário é calculado aqui, então nada depende do relógio
 * real — o instante da varredura vem do Clock injetado no service.
 */
@ExtendWith(MockitoExtension.class)
class PatientResponseDeadlineSchedulerTest {

    @Mock
    private AlertService alertService;

    @InjectMocks
    private PatientResponseDeadlineScheduler scheduler;

    private static final UUID ALERT_A = UUID.randomUUID();
    private static final UUID ALERT_B = UUID.randomUUID();

    @Nested
    @DisplayName("varredura dos prazos vencidos")
    class Sweep {

        @Test
        @DisplayName("deve confirmar cada alerta vencido devolvido pela consulta")
        void shouldConfirmEveryExpiredAlert() {
            when(alertService.findAlertIdsAwaitingPatientPastDeadline())
                    .thenReturn(List.of(ALERT_A, ALERT_B));
            when(alertService.confirmAlertWithoutPatientResponse(any(UUID.class))).thenReturn(true);

            scheduler.confirmAlertsWithoutPatientResponse();

            verify(alertService).confirmAlertWithoutPatientResponse(ALERT_A);
            verify(alertService).confirmAlertWithoutPatientResponse(ALERT_B);
        }

        /**
         * Prazo não vencido não aparece na consulta, então o agendador não toca em
         * nada: é a query que aplica o critério de vencimento.
         */
        @Test
        @DisplayName("sem alerta vencido nao deve chamar a confirmacao")
        void shouldDoNothingWhenNothingIsExpired() {
            when(alertService.findAlertIdsAwaitingPatientPastDeadline()).thenReturn(List.of());

            scheduler.confirmAlertsWithoutPatientResponse();

            verify(alertService, never()).confirmAlertWithoutPatientResponse(any());
        }

        @Test
        @DisplayName("alerta que perdeu a corrida com a resposta nao impede os demais")
        void shouldKeepGoingWhenOneAlertLosesTheRace() {
            when(alertService.findAlertIdsAwaitingPatientPastDeadline())
                    .thenReturn(List.of(ALERT_A, ALERT_B));
            when(alertService.confirmAlertWithoutPatientResponse(ALERT_A)).thenReturn(false);
            when(alertService.confirmAlertWithoutPatientResponse(ALERT_B)).thenReturn(true);

            scheduler.confirmAlertsWithoutPatientResponse();

            verify(alertService).confirmAlertWithoutPatientResponse(ALERT_B);
        }
    }

    @Nested
    @DisplayName("isolamento de falha")
    class FailureIsolation {

        /**
         * Exceção não pode escapar: o agendador do Spring interromperia as execuções
         * seguintes de uma tarefa que lança, e o fluxo precisa continuar rodando no
         * minuto seguinte.
         */
        @Test
        @DisplayName("falha na consulta nao deve propagar")
        void shouldSwallowQueryFailure() {
            when(alertService.findAlertIdsAwaitingPatientPastDeadline())
                    .thenThrow(new RuntimeException("banco indisponivel"));

            assertThatCode(() -> scheduler.confirmAlertsWithoutPatientResponse())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("falha em um alerta nao deve propagar nem impedir o proximo")
        void shouldSwallowPerAlertFailureAndContinue() {
            when(alertService.findAlertIdsAwaitingPatientPastDeadline())
                    .thenReturn(List.of(ALERT_A, ALERT_B));
            when(alertService.confirmAlertWithoutPatientResponse(ALERT_A))
                    .thenThrow(new RuntimeException("falha isolada"));
            when(alertService.confirmAlertWithoutPatientResponse(ALERT_B)).thenReturn(true);

            assertThatCode(() -> scheduler.confirmAlertsWithoutPatientResponse())
                    .doesNotThrowAnyException();

            verify(alertService).confirmAlertWithoutPatientResponse(ALERT_B);
        }

        @Test
        @DisplayName("deve continuar utilizavel depois de um ciclo com falha")
        void shouldStayUsableAfterAFailedCycle() {
            when(alertService.findAlertIdsAwaitingPatientPastDeadline())
                    .thenThrow(new RuntimeException("falha transitoria"))
                    .thenReturn(List.of(ALERT_A));
            when(alertService.confirmAlertWithoutPatientResponse(ALERT_A)).thenReturn(true);

            scheduler.confirmAlertsWithoutPatientResponse();
            scheduler.confirmAlertsWithoutPatientResponse();

            verify(alertService).confirmAlertWithoutPatientResponse(ALERT_A);
            assertThat(true).isTrue();
        }
    }
}

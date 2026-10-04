package com.tcc.infrastructure.scheduler;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tcc.application.service.AlertService;

/**
 * Varre os alertas AWAITING_PATIENT cujo prazo de resposta venceu e os leva a
 * PENDING, avisando o médico.
 *
 * <p>Silêncio da paciente não é "está tudo bem": passados os 10 minutos sem
 * resposta a uma leitura grave, o médico precisa saber.
 *
 * <p>O agendador não decide nada e não abre transação: ele pede a lista ao service
 * e chama o service uma vez por alerta. Cada chamada atravessa o proxy do Spring e
 * abre transação própria, então a falha em um alerta não desfaz os outros do ciclo
 * e cada troca de status disputa sozinha com uma possível resposta da paciente.
 */
@Component
public class PatientResponseDeadlineScheduler {

    private static final Logger log = LoggerFactory.getLogger(
            PatientResponseDeadlineScheduler.class);

    private final AlertService alertService;

    public PatientResponseDeadlineScheduler(AlertService alertService) {
        this.alertService = alertService;
    }

    /**
     * Roda a cada minuto. O intervalo é um minuto porque o prazo é de dez: uma
     * varredura mais esparsa atrasaria o aviso ao médico por uma fração relevante do
     * próprio prazo.
     *
     * <p>{@code fixedDelay} em vez de {@code fixedRate}: o intervalo conta do fim da
     * execução anterior, então um ciclo lento não acumula execuções concorrentes
     * disputando as mesmas linhas.
     *
     * <p>Exceção não escapa: o agendador do Spring interromperia as execuções
     * seguintes de uma tarefa que lança, e o fluxo precisa continuar rodando no
     * minuto seguinte.
     */
    @Scheduled(
            fixedDelayString = "${app.alerts.patient-response-sweep-interval-ms:60000}",
            initialDelayString = "${app.alerts.patient-response-sweep-initial-delay-ms:60000}")
    public void confirmAlertsWithoutPatientResponse() {
        try {
            List<UUID> expired = alertService.findAlertIdsAwaitingPatientPastDeadline();

            if (expired.isEmpty()) {
                return;
            }

            int confirmed = 0;

            for (UUID alertId : expired) {
                confirmed += confirmOne(alertId) ? 1 : 0;
            }

            log.info("Varredura de prazo de resposta: {} de {} alerta(s) confirmado(s).",
                    confirmed, expired.size());

        } catch (Exception e) {
            log.error("Erro na varredura de prazo de resposta do paciente. exception={}",
                    e.getClass().getSimpleName());
        }
    }

    /**
     * Isola a falha de um alerta: um erro ao confirmar o terceiro não impede o quarto
     * de ser tratado no mesmo ciclo.
     */
    private boolean confirmOne(UUID alertId) {
        try {
            return alertService.confirmAlertWithoutPatientResponse(alertId);

        } catch (Exception e) {
            log.error("Erro ao confirmar o alerta {} por prazo vencido. exception={}",
                    alertId, e.getClass().getSimpleName());
            return false;
        }
    }
}

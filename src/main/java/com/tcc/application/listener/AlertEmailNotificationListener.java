package com.tcc.application.listener;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.tcc.application.port.out.AlertEmailSender;
import com.tcc.domain.event.AlertCreatedEvent;
import com.tcc.domain.model.Alert;
import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.HealthReading;
import com.tcc.domain.model.Notification;
import com.tcc.domain.repository.AlertRepository;
import com.tcc.domain.repository.DoctorPatientRepository;
import com.tcc.domain.repository.NotificationRepository;

/**
 * Avisa por e-mail os médicos com vínculo ativo com o paciente do alerta, e grava
 * uma linha em {@code notifications} por destinatário.
 *
 * <p>Listener separado do {@code AlertPushNotificationListener} de propósito: os
 * dois escutam o mesmo evento e falham de forma independente. Erro no e-mail não
 * afeta o alerta já gravado nem o push ao paciente, e vice-versa.
 */
@Component
public class AlertEmailNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(AlertEmailNotificationListener.class);

    /**
     * A coluna {@code measured_at} é TIMESTAMP sem fuso e guarda o horário em UTC,
     * normalizado por {@code AlertServiceImpl} na entrada. Para o médico o horário é
     * exibido em America/Sao_Paulo.
     */
    private static final ZoneOffset STORAGE_OFFSET = ZoneOffset.UTC;

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("America/Sao_Paulo");

    private static final DateTimeFormatter MEASURED_AT_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private static final String SUBJECT_PREFIX = "Alerta de saúde: ";
    private static final String NOT_INFORMED = "não informado";

    /** Status gravados em notifications conforme o resultado do envio. */
    private static final String STATUS_SENT = "SENT";
    private static final String STATUS_FAILED = "FAILED";

    private final AlertRepository alertRepository;
    private final DoctorPatientRepository doctorPatientRepository;
    private final NotificationRepository notificationRepository;
    private final AlertEmailSender alertEmailSender;

    public AlertEmailNotificationListener(AlertRepository alertRepository,
                                          DoctorPatientRepository doctorPatientRepository,
                                          NotificationRepository notificationRepository,
                                          AlertEmailSender alertEmailSender) {
        this.alertRepository = alertRepository;
        this.doctorPatientRepository = doctorPatientRepository;
        this.notificationRepository = notificationRepository;
        this.alertEmailSender = alertEmailSender;
    }

    /**
     * Roda depois do commit do alerta, em transação própria (REQUIRES_NEW) para
     * poder navegar os relacionamentos LAZY e gravar as notificações.
     *
     * <p>Todo o corpo é envolvido em try/catch: a resposta do endpoint já foi
     * decidida e o alerta já está gravado, então exceção aqui não tem para onde ir.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAlertCreated(AlertCreatedEvent event) {
        UUID alertId = event.alert().getId();

        try {
            notifyDoctors(alertId);
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail do alerta {}. exception={}",
                    alertId, e.getClass().getSimpleName());
        }
    }

    private void notifyDoctors(UUID alertId) {
        Alert alert = alertRepository.findByIdWithPatientAndReading(alertId).orElse(null);

        if (alert == null) {
            log.warn("Alerta {} nao encontrado ao preparar o e-mail.", alertId);
            return;
        }

        UUID patientId = alert.getPatient().getId();
        List<Doctor> doctors = doctorPatientRepository.findActiveDoctorsByPatientId(patientId);

        if (doctors.isEmpty()) {
            log.debug("Paciente {} nao tem medico com vinculo ativo. E-mail do alerta {} ignorado.",
                    patientId, alertId);
            return;
        }

        String subject = buildSubject(alert);
        String body = buildBody(alert);

        for (Doctor doctor : doctors) {
            notifyDoctor(alert, doctor, subject, body);
        }
    }

    /**
     * Envia e registra o resultado. A gravação da notificação acontece nos dois
     * caminhos: SENT quando o provedor aceitou, FAILED quando não — inclusive quando
     * o médico não tem e-mail utilizável. Sem a linha FAILED não haveria rastro de
     * que o aviso foi tentado e não chegou.
     */
    private void notifyDoctor(Alert alert, Doctor doctor, String subject, String body) {
        String recipient = doctor.getUser() == null ? null : doctor.getUser().getEmail();

        boolean sent = alertEmailSender.sendAlertEmail(recipient, subject, body, alert.getId());

        Notification notification = new Notification(
                alert, doctor, body, sent ? STATUS_SENT : STATUS_FAILED);
        notificationRepository.save(notification);

        if (sent) {
            log.info("Notificacao do alerta {} registrada como {} para o medico {}.",
                    alert.getId(), STATUS_SENT, doctor.getId());
        } else {
            log.warn("Notificacao do alerta {} registrada como {} para o medico {}.",
                    alert.getId(), STATUS_FAILED, doctor.getId());
        }
    }

    private String buildSubject(Alert alert) {
        String severity = alert.getSeverity() == null ? "" : " [" + alert.getSeverity() + "]";
        String subject = SUBJECT_PREFIX + alert.getPatient().getFullName() + severity;
        return subject.length() > 255 ? subject.substring(0, 255) : subject;
    }

    /**
     * Texto simples, em português, com o que o médico precisa para decidir: quem,
     * o que foi medido, quanto deu, a gravidade e quando a medição aconteceu.
     *
     * <p>O horário da medição é o ponto central: a leitura pode chegar atrasada pela
     * fila, então o horário de criação do alerta não serve como referência clínica.
     */
    private String buildBody(Alert alert) {
        HealthReading reading = alert.getHealthReading();

        return """
                Um alerta foi gerado para o paciente sob seus cuidados.

                Paciente: %s
                Tipo da leitura: %s
                Valor medido: %s
                Severidade: %s
                Horário da medição: %s (horário de Brasília)

                Motivo: %s

                Acesse o portal para ver o histórico completo do paciente.
                """.formatted(
                alert.getPatient().getFullName(),
                reading == null ? NOT_INFORMED : reading.getReadingType(),
                formatValue(reading),
                alert.getSeverity() == null ? NOT_INFORMED : alert.getSeverity(),
                formatMeasuredAt(reading),
                alert.getDescription() == null || alert.getDescription().isBlank()
                        ? NOT_INFORMED : alert.getDescription());
    }

    private String formatValue(HealthReading reading) {
        if (reading == null || reading.getValue() == null) {
            return NOT_INFORMED;
        }

        String unit = reading.getUnit();
        return unit == null || unit.isBlank() ? reading.getValue() : reading.getValue() + " " + unit;
    }

    /**
     * Converte o horário gravado de UTC para America/Sao_Paulo antes de formatar.
     *
     * <p>Os dois fusos são explícitos e nenhum deles vem da JVM: o resultado é o
     * mesmo com o servidor em UTC ou em horário de Brasília. Usar o fuso da máquina
     * aqui faria o mesmo alerta mostrar horários diferentes em ambientes diferentes.
     */
    private String formatMeasuredAt(HealthReading reading) {
        if (reading == null || reading.getMeasuredAt() == null) {
            return NOT_INFORMED;
        }

        LocalDateTime forDoctor = reading.getMeasuredAt()
                .atOffset(STORAGE_OFFSET)
                .atZoneSameInstant(DISPLAY_ZONE)
                .toLocalDateTime();

        return MEASURED_AT_FORMAT.format(forDoctor);
    }
}

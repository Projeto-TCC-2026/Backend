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
import com.tcc.domain.event.AlertConfirmationReason;
import com.tcc.domain.event.AlertConfirmedEvent;
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
 * <p>Escuta {@link AlertConfirmedEvent}, não a criação do alerta: o médico só é
 * avisado depois que uma segunda leitura seguida confirma o desvio. Na criação
 * (alerta UNCONFIRMED) apenas o paciente recebe push, pelo
 * {@code AlertPushNotificationListener}.
 *
 * <p>Os dois listeners são separados de propósito e falham de forma independente.
 * Erro no e-mail não afeta o alerta já gravado nem o push ao paciente, e vice-versa.
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

    /**
     * Motivo da confirmação, em português, como o médico lê no e-mail. É a frase que
     * distingue a evidência que chegou até ele.
     */
    private static final String REASON_PATIENT_NOT_OK = "A paciente respondeu que não está bem.";
    private static final String REASON_NO_ANSWER = "A paciente não respondeu em 10 minutos.";
    private static final String REASON_TWO_READINGS = "Duas leituras seguidas confirmaram o desvio.";
    private static final String REASON_UNKNOWN = "Motivo da confirmação não informado.";

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
    public void onAlertConfirmed(AlertConfirmedEvent event) {
        UUID alertId = event.alert().getId();

        try {
            notifyDoctors(alertId, event.firstReading(), event.confirmingReading(), event.reason());
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail do alerta {}. exception={}",
                    alertId, e.getClass().getSimpleName());
        }
    }

    private void notifyDoctors(UUID alertId, HealthReading firstReading,
                               HealthReading confirmingReading,
                               AlertConfirmationReason reason) {
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
        String body = buildBody(alert, firstReading, confirmingReading, reason);

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
     * Texto simples, em português, com o que o médico precisa para decidir: quem, o
     * que foi medido, as DUAS leituras que confirmaram o desvio, a gravidade e
     * quando cada medição aconteceu.
     *
     * <p>As duas leituras aparecem porque o alerta só chega ao médico depois de
     * confirmado: mostrar apenas o último valor esconderia a progressão, que é o que
     * distingue um desvio sustentado de uma medição isolada.
     *
     * <p>O horário da medição é o ponto central: a leitura pode chegar atrasada pela
     * fila, então o horário de criação do alerta não serve como referência clínica.
     */
    private String buildBody(Alert alert, HealthReading firstReading,
                             HealthReading confirmingReading,
                             AlertConfirmationReason reason) {
        return reason == AlertConfirmationReason.DUAS_LEITURAS
                ? buildTwoReadingsBody(alert, firstReading, confirmingReading)
                : buildSevereReadingBody(alert, firstReading, reason);
    }

    private String buildTwoReadingsBody(Alert alert, HealthReading firstReading,
                                        HealthReading confirmingReading) {
        return """
                Um alerta foi confirmado por duas leituras seguidas para o paciente sob seus cuidados.

                Paciente: %s
                Tipo da leitura: %s
                Severidade: %s

                1ª leitura: %s em %s
                2ª leitura: %s em %s
                (horários de Brasília)

                Motivo: %s

                Acesse o portal para ver o histórico completo do paciente.
                """.formatted(
                alert.getPatient().getFullName(),
                readingType(alert, firstReading, confirmingReading),
                alert.getSeverity() == null ? NOT_INFORMED : alert.getSeverity(),
                formatValue(firstReading),
                formatMeasuredAt(firstReading),
                formatValue(confirmingReading),
                formatMeasuredAt(confirmingReading),
                alert.getDescription() == null || alert.getDescription().isBlank()
                        ? NOT_INFORMED : alert.getDescription());
    }

    /**
     * Corpo dos dois motivos de leitura grave. Mostra uma leitura só, a grave, porque
     * foi ela sozinha que levou o aviso ao médico — não houve segunda medição.
     *
     * <p>O motivo aparece por extenso e em destaque: a diferença entre "ela disse que
     * não está bem" e "ela não respondeu" muda a urgência, e o médico precisa ver
     * qual das duas chegou até ele.
     */
    private String buildSevereReadingBody(Alert alert, HealthReading severeReading,
                                          AlertConfirmationReason reason) {
        return """
                Uma medição com valor grave foi registrada para o paciente sob seus cuidados.

                Paciente: %s
                Tipo da leitura: %s
                Severidade: %s

                Leitura: %s em %s
                (horário de Brasília)

                %s

                Motivo da leitura: %s

                Acesse o portal para ver o histórico completo do paciente.
                """.formatted(
                alert.getPatient().getFullName(),
                readingType(alert, severeReading, null),
                alert.getSeverity() == null ? NOT_INFORMED : alert.getSeverity(),
                formatValue(severeReading),
                formatMeasuredAt(severeReading),
                describeReason(reason),
                alert.getDescription() == null || alert.getDescription().isBlank()
                        ? NOT_INFORMED : alert.getDescription());
    }

    /**
     * Motivo em português. {@code DUAS_LEITURAS} não aparece aqui porque tem corpo
     * próprio; se chegar, cai no texto neutro em vez de quebrar o e-mail.
     */
    private String describeReason(AlertConfirmationReason reason) {
        if (reason == null) {
            return REASON_UNKNOWN;
        }

        return switch (reason) {
            case PACIENTE_NAO_ESTA_BEM -> REASON_PATIENT_NOT_OK;
            case SEM_RESPOSTA -> REASON_NO_ANSWER;
            case DUAS_LEITURAS -> REASON_TWO_READINGS;
        };
    }

    /**
     * Tipo da leitura, procurado nas três fontes disponíveis. As duas leituras do
     * evento são sempre do mesmo tipo — a confirmação exige isso — então qualquer uma
     * serve; a do alerta entra como terceira opção.
     */
    private String readingType(Alert alert, HealthReading firstReading,
                               HealthReading confirmingReading) {
        if (firstReading != null && firstReading.getReadingType() != null) {
            return firstReading.getReadingType();
        }
        if (confirmingReading != null && confirmingReading.getReadingType() != null) {
            return confirmingReading.getReadingType();
        }
        if (alert.getHealthReading() != null && alert.getHealthReading().getReadingType() != null) {
            return alert.getHealthReading().getReadingType();
        }
        return NOT_INFORMED;
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

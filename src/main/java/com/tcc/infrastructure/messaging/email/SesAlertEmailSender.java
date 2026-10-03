package com.tcc.infrastructure.messaging.email;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.tcc.application.port.out.AlertEmailSender;

import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;

/**
 * Envio pelo Amazon SES (API v2), seguindo o mesmo formato dos clientes SQS e S3
 * já usados no projeto: cliente do AWS SDK v2 injetado por construtor, região e
 * demais parâmetros vindos de propriedade.
 */
@Service
@ConditionalOnProperty(name = "app.alert-email.enabled", havingValue = "true")
public class SesAlertEmailSender implements AlertEmailSender {

    private static final Logger log = LoggerFactory.getLogger(SesAlertEmailSender.class);

    /** O conteúdo tem acento: sem charset explícito o texto chega corrompido. */
    private static final String CHARSET = StandardCharsets.UTF_8.name();

    private final SesV2Client sesV2Client;
    private final String senderAddress;

    public SesAlertEmailSender(SesV2Client sesV2Client,
                               @Value("${app.alert-email.sender}") String senderAddress) {
        this.sesV2Client = sesV2Client;
        this.senderAddress = senderAddress;
    }

    /**
     * Nenhuma exceção escapa: quem chama é um listener que roda depois do commit do
     * alerta, e o retorno booleano é o que decide o status da notificação. Falha de
     * SES vira FAILED em {@code notifications}, não erro na requisição original.
     */
    @Override
    public boolean sendAlertEmail(String recipientEmail, String subject, String body, UUID alertId) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("Destinatario ausente no aviso do alerta {}. Envio ignorado.", alertId);
            return false;
        }

        if (senderAddress == null || senderAddress.isBlank()) {
            log.error("Remetente de e-mail nao configurado. Aviso do alerta {} nao enviado.", alertId);
            return false;
        }

        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .fromEmailAddress(senderAddress)
                    .destination(Destination.builder().toAddresses(recipientEmail).build())
                    .content(EmailContent.builder()
                            .simple(Message.builder()
                                    .subject(Content.builder().charset(CHARSET).data(subject).build())
                                    .body(Body.builder()
                                            .text(Content.builder().charset(CHARSET).data(body).build())
                                            .build())
                                    .build())
                            .build())
                    .build();

            sesV2Client.sendEmail(request);

            log.info("Aviso do alerta {} enviado por e-mail.", alertId);
            return true;

        } catch (Exception e) {
            log.error("Falha ao enviar e-mail do alerta {}. exception={}",
                    alertId, e.getClass().getSimpleName());
            return false;
        }
    }
}

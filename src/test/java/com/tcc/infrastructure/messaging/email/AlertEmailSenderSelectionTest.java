package com.tcc.infrastructure.messaging.email;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.tcc.application.port.out.AlertEmailSender;
import com.tcc.infrastructure.config.AwsSqsConfig;
import com.tcc.infrastructure.config.JacksonConfig;

import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * Qual {@link AlertEmailSender} entra para cada combinação de
 * {@code app.alert-email.enabled} e {@code app.alert-email.provider}, e se o
 * {@code SesV2Client} só é criado com provider=ses.
 *
 * <p>Os clientes do SDK são construídos de verdade, mas sem chamada de API: o
 * builder só resolve credencial e endpoint na primeira requisição.
 */
class AlertEmailSenderSelectionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(
                    AwsSqsConfig.class,
                    JacksonConfig.class,
                    NoOpAlertEmailSender.class,
                    SesAlertEmailSender.class,
                    SqsAlertEmailSender.class)
            .withPropertyValues(
                    "app.password-reset.aws-region=us-east-1",
                    "app.alert-email.aws-region=us-east-1",
                    "app.alert-email.sender=alertas@tcc.local",
                    "app.alert-email.sqs-queue-url="
                            + "https://sqs.us-east-1.amazonaws.com/919446725618/alerta-medico-email-requests");

    @Test
    @DisplayName("enabled ausente: NoOp, sem cliente SES")
    void enabledMissingSelectsNoOp() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(AlertEmailSender.class);
            assertThat(context).hasSingleBean(NoOpAlertEmailSender.class);
            assertThat(context).doesNotHaveBean(SesV2Client.class);
        });
    }

    @Test
    @DisplayName("enabled=false: NoOp, com qualquer provider")
    void enabledFalseSelectsNoOp() {
        for (String provider : new String[] {"ses", "sqs"}) {
            runner.withPropertyValues("app.alert-email.enabled=false", "app.alert-email.provider=" + provider)
                    .run(context -> {
                        assertThat(context).hasSingleBean(AlertEmailSender.class);
                        assertThat(context).hasSingleBean(NoOpAlertEmailSender.class);
                        assertThat(context).doesNotHaveBean(SesV2Client.class);
                    });
        }
    }

    @Test
    @DisplayName("enabled=true sem provider: SES (padrão)")
    void enabledTrueWithoutProviderSelectsSes() {
        runner.withPropertyValues("app.alert-email.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(AlertEmailSender.class);
                    assertThat(context).hasSingleBean(SesAlertEmailSender.class);
                    assertThat(context).hasSingleBean(SesV2Client.class);
                });
    }

    @Test
    @DisplayName("enabled=true + provider=ses: SES e cliente SES criado")
    void enabledTrueProviderSesSelectsSes() {
        runner.withPropertyValues("app.alert-email.enabled=true", "app.alert-email.provider=ses")
                .run(context -> {
                    assertThat(context).hasSingleBean(AlertEmailSender.class);
                    assertThat(context).hasSingleBean(SesAlertEmailSender.class);
                    assertThat(context).hasSingleBean(SesV2Client.class);
                });
    }

    @Test
    @DisplayName("enabled=true + provider=sqs: SQS com o SqsClient existente, sem cliente SES")
    void enabledTrueProviderSqsSelectsSqs() {
        runner.withPropertyValues("app.alert-email.enabled=true", "app.alert-email.provider=sqs")
                .run(context -> {
                    assertThat(context).hasSingleBean(AlertEmailSender.class);
                    assertThat(context).hasSingleBean(SqsAlertEmailSender.class);
                    assertThat(context).doesNotHaveBean(SesV2Client.class);
                    // Continua existindo um único SqsClient: o mesmo dos outros publishers.
                    assertThat(context).hasSingleBean(SqsClient.class);
                    assertThat(context).hasBean("sqsClient");
                });
    }
}

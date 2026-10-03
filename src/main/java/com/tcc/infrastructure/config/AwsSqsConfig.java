package com.tcc.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class AwsSqsConfig {

    @Bean
    public SqsClient sqsClient(
            @Value("${app.password-reset.aws-region:us-east-1}") String awsRegion) {
        return SqsClient.builder()
                .region(Region.of(awsRegion))
                .build();
    }

    /**
     * Cliente do SES usado pelo aviso de alerta ao médico.
     *
     * <p>Diferente do {@code SqsClient} acima, este bean é condicional: ele só
     * existe quando {@code app.alert-email.enabled=true}. Com o e-mail desligado
     * (o padrão) entra o {@code NoOpAlertEmailSender}, que não tem cliente AWS, e
     * nada precisa ser criado.
     */
    @Bean
    @ConditionalOnProperty(name = "app.alert-email.enabled", havingValue = "true")
    public SesV2Client sesV2Client(
            @Value("${app.alert-email.aws-region:us-east-1}") String awsRegion) {
        return SesV2Client.builder()
                .region(Region.of(awsRegion))
                .build();
    }
}

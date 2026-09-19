package com.tcc.infrastructure.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    /**
     * Client dedicado ao Expo Push Service.
     *
     * <p>Os timeouts são obrigatórios: sem eles uma indisponibilidade do provedor
     * prenderia a thread do listener por tempo indefinido. O envio de push roda
     * depois do commit do alerta, então travar aqui não corrompe dado, mas consome
     * thread do pool à toa.
     */
    @Bean
    public RestClient expoPushRestClient(
            @Value("${app.push.connect-timeout-ms:3000}") long connectTimeoutMs,
            @Value("${app.push.read-timeout-ms:5000}") long readTimeoutMs) {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        return RestClient.builder().requestFactory(requestFactory).build();
    }
}

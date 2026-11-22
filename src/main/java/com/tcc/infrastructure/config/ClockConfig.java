package com.tcc.infrastructure.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Relógio injetável.
 *
 * <p>Existe para que o prazo de resposta da paciente e a varredura do agendador
 * não dependam do relógio real em teste: o teste injeta um {@link Clock#fixed} e
 * controla o instante.
 *
 * <p>O relógio usa o fuso configurado pela aplicação, com
 * {@code America/Sao_Paulo} como padrão. Ele pode ser substituído por um relógio
 * fixo nos testes.
 */
@Configuration
public class ClockConfig {

    @Value("${app.time-zone:America/Sao_Paulo}")
    private String timeZone;

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of(timeZone));
    }
}

package com.tcc.infrastructure.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Relógio injetável.
 *
 * <p>Existe para que o prazo de resposta da paciente e a varredura do agendador
 * não dependam do relógio real em teste: o teste injeta um {@link Clock#fixed} e
 * controla o instante.
 *
 * <p>É {@link Clock#systemDefaultZone()} de propósito, e não {@code systemUTC()}:
 * as colunas de data/hora do projeto são TIMESTAMP sem fuso e os campos gerados
 * pela aplicação ({@code createdAt} nos {@code @PrePersist}) usam
 * {@code LocalDateTime.now()}, que é o fuso da JVM. Um relógio em UTC aqui faria
 * {@code patient_response_deadline} nascer deslocado de {@code created_at} do
 * mesmo alerta.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}

package com.tcc.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita o agendamento de tarefas. O projeto não tinha scheduling
 * habilitado antes desta configuração.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
